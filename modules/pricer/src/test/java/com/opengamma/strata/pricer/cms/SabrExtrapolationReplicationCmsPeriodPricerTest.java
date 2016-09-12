/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.cms;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.product.swap.SwapIndices.EUR_EURIBOR_1100_5Y;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.param.CurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.surface.InterpolatedNodalSurface;
import com.opengamma.strata.math.impl.integration.RungeKuttaIntegrator1D;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.impl.option.SabrExtrapolationRightFunction;
import com.opengamma.strata.pricer.impl.volatility.smile.SabrFormulaData;
import com.opengamma.strata.pricer.model.SabrInterestRateParameters;
import com.opengamma.strata.pricer.model.SabrVolatilityFormula;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.pricer.swaption.SabrParametersSwaptionVolatilities;
import com.opengamma.strata.pricer.swaption.SwaptionSabrRateVolatilityDataSet;
import com.opengamma.strata.pricer.swaption.SwaptionVolatilitiesName;
import com.opengamma.strata.product.cms.CmsPeriod;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.common.PutCall;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.ResolvedSwapLeg;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapIndex;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;

/**
 * Test {@link SabrExtrapolationReplicationCmsPeriodPricer}.
 */
@Test
public class SabrExtrapolationReplicationCmsPeriodPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VALUATION = LocalDate.of(2010, 8, 18);
  private static final LocalDate FIXING = LocalDate.of(2020, 4, 24);
  private static final LocalDate START = LocalDate.of(2020, 4, 28);
  private static final LocalDate END = LocalDate.of(2021, 4, 28);
  private static final LocalDate AFTER_FIXING = LocalDate.of(2020, 8, 11);
  private static final LocalDate PAYMENT = LocalDate.of(2021, 4, 28);
  private static final LocalDate AFTER_PAYMENT = LocalDate.of(2021, 4, 29);

  // providers
  private static final ImmutableRatesProvider RATES_PROVIDER =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(VALUATION);
  private static final SabrParametersSwaptionVolatilities VOLATILITIES =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(VALUATION, false);
  private static final SabrParametersSwaptionVolatilities VOLATILITIES_SHIFT =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(VALUATION, true);
  private static final double SHIFT = VOLATILITIES_SHIFT.getParameters().getShiftSurface().getParameter(0); // constant surface
  private static final double OBS_INDEX = 0.0135;
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.of(FIXING, OBS_INDEX);
  // providers - on fixing date, no time series
  private static final ImmutableRatesProvider RATES_PROVIDER_ON_FIX =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(FIXING);
  private static final SabrParametersSwaptionVolatilities VOLATILITIES_ON_FIX =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(FIXING, true);
  // providers - after fixing date, no time series
  private static final ImmutableRatesProvider RATES_PROVIDER_NO_TS =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(AFTER_FIXING);
  private static final SabrParametersSwaptionVolatilities VOLATILITIES_NO_TS =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(AFTER_FIXING, true);
  // providers - between fixing date and payment date
  private static final ImmutableRatesProvider RATES_PROVIDER_AFTER_FIX =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(AFTER_FIXING, TIME_SERIES);
  private static final SabrParametersSwaptionVolatilities VOLATILITIES_AFTER_FIX =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(AFTER_FIXING, true);
  // providers - on payment date
  private static final ImmutableRatesProvider RATES_PROVIDER_ON_PAY =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(PAYMENT, TIME_SERIES);
  private static final SabrParametersSwaptionVolatilities VOLATILITIES_ON_PAY =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(PAYMENT, true);
  // providers - ended
  private static final ImmutableRatesProvider RATES_PROVIDER_AFTER_PAY =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(AFTER_PAYMENT, TIME_SERIES);
  private static final SabrParametersSwaptionVolatilities VOLATILITIES_AFTER_PAY =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesEur(AFTER_PAYMENT, true);

  private static final double ACC_FACTOR = ACT_360.relativeYearFraction(START, END);
  private static final double NOTIONAL = 10000000; // 10m
  private static final double STRIKE = 0.04;
  private static final double STRIKE_NEGATIVE = -0.01;
  // CMS - buy
  private static final CmsPeriod COUPON = createCmsCoupon(true);
  private static final CmsPeriod CAPLET = createCmsCaplet(true, STRIKE);
  private static final CmsPeriod FLOORLET = createCmsFloorlet(true, STRIKE);
  // CMS - sell
  private static final CmsPeriod COUPON_SELL = createCmsCoupon(false);
  private static final CmsPeriod CAPLET_SELL = createCmsCaplet(false, STRIKE);
  private static final CmsPeriod FLOORLET_SELL = createCmsFloorlet(false, STRIKE);
  // CMS - zero strikes
  private static final CmsPeriod CAPLET_ZERO = createCmsCaplet(true, 0d);
  private static final CmsPeriod FLOORLET_ZERO = createCmsFloorlet(true, 0d);
  // CMS - negative strikes, to become positive after shift
  private static final CmsPeriod CAPLET_NEGATIVE = createCmsCaplet(true, STRIKE_NEGATIVE);
  private static final CmsPeriod FLOORLET_NEGATIVE = createCmsFloorlet(true, STRIKE_NEGATIVE);
  // CMS - negative strikes, to become zero after shift
  private static final CmsPeriod CAPLET_SHIFT = createCmsCaplet(true, -SHIFT);
  private static final CmsPeriod FLOORLET_SHIFT = createCmsFloorlet(true, -SHIFT);

  private static final double CUT_OFF_STRIKE = 0.10;
  private static final double MU = 2.50;
  private static final double EPS = 1.0e-5;
  private static final double TOL = 1.0e-12;
  private static final SabrExtrapolationReplicationCmsPeriodPricer PRICER =
      SabrExtrapolationReplicationCmsPeriodPricer.of(CUT_OFF_STRIKE, MU);
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(EPS);  
  private static final DiscountingSwapProductPricer PRICER_SWAP =
      DiscountingSwapProductPricer.DEFAULT;

  public void test_presentValue_zero() {
    CurrencyAmount pv = PRICER.presentValue(COUPON, RATES_PROVIDER, VOLATILITIES);
    CurrencyAmount pvCaplet = PRICER.presentValue(CAPLET_ZERO, RATES_PROVIDER, VOLATILITIES);
    CurrencyAmount pvFloorlet = PRICER.presentValue(FLOORLET_ZERO, RATES_PROVIDER, VOLATILITIES);
    assertEquals(pv.getAmount(), pvCaplet.getAmount(), NOTIONAL * TOL);
    assertEquals(pvFloorlet.getAmount(), 0d, 2.0d * NOTIONAL * TOL);
    CurrencyAmount pvShift = PRICER.presentValue(COUPON, RATES_PROVIDER, VOLATILITIES_SHIFT);
    CurrencyAmount pvCapletShift = PRICER.presentValue(CAPLET_SHIFT, RATES_PROVIDER, VOLATILITIES_SHIFT);
    CurrencyAmount pvFloorletShift = PRICER.presentValue(FLOORLET_SHIFT, RATES_PROVIDER, VOLATILITIES_SHIFT);
    double dfPayment = RATES_PROVIDER.discountFactor(EUR, PAYMENT);
    assertEquals(pvShift.getAmount(), pvCapletShift.getAmount() - SHIFT * dfPayment * NOTIONAL * ACC_FACTOR, NOTIONAL * TOL);
    assertEquals(pvFloorletShift.getAmount(), 0d, 2.0d * NOTIONAL * TOL);
  }

  public void test_presentValue_buySell() {
    CurrencyAmount pvBuy = PRICER.presentValue(COUPON, RATES_PROVIDER, VOLATILITIES);
    CurrencyAmount pvCapletBuy = PRICER.presentValue(CAPLET, RATES_PROVIDER, VOLATILITIES);
    CurrencyAmount pvFloorletBuy = PRICER.presentValue(FLOORLET, RATES_PROVIDER, VOLATILITIES);
    CurrencyAmount pvSell = PRICER.presentValue(COUPON_SELL, RATES_PROVIDER, VOLATILITIES);
    CurrencyAmount pvCapletSell = PRICER.presentValue(CAPLET_SELL, RATES_PROVIDER, VOLATILITIES);
    CurrencyAmount pvFloorletSell = PRICER.presentValue(FLOORLET_SELL, RATES_PROVIDER, VOLATILITIES);
    assertEquals(pvBuy.getAmount(), -pvSell.getAmount(), NOTIONAL * TOL);
    assertEquals(pvCapletBuy.getAmount(), -pvCapletSell.getAmount(), NOTIONAL * TOL);
    assertEquals(pvFloorletBuy.getAmount(), -pvFloorletSell.getAmount(), NOTIONAL * TOL);
  }

  public void test_presentValue_afterFix() {
    CurrencyAmount pv = PRICER.presentValue(COUPON, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX);
    CurrencyAmount pvCapletOtm = PRICER.presentValue(CAPLET, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX);
    CurrencyAmount pvCapletItm = PRICER.presentValue(CAPLET_NEGATIVE, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX);
    CurrencyAmount pvFloorletItm = PRICER.presentValue(FLOORLET, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX);
    CurrencyAmount pvFloorletOtm = PRICER.presentValue(FLOORLET_NEGATIVE, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX);
    double factor = RATES_PROVIDER_AFTER_FIX.discountFactor(EUR, PAYMENT) * NOTIONAL * COUPON.getYearFraction();
    assertEquals(pv.getAmount(), OBS_INDEX * factor, NOTIONAL * TOL);
    assertEquals(pvCapletOtm.getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(pvCapletItm.getAmount(), (OBS_INDEX - STRIKE_NEGATIVE) * factor, NOTIONAL * TOL);
    assertEquals(pvFloorletItm.getAmount(), (STRIKE - OBS_INDEX) * factor, NOTIONAL * TOL);
    assertEquals(pvFloorletOtm.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValue_onPayment() {
    CurrencyAmount pv = PRICER.presentValue(COUPON, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY);
    CurrencyAmount pvCapletOtm = PRICER.presentValue(CAPLET, RATES_PROVIDER_ON_PAY, VOLATILITIES_AFTER_FIX);
    CurrencyAmount pvCapletItm = PRICER.presentValue(CAPLET_NEGATIVE, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY);
    CurrencyAmount pvFloorletItm = PRICER.presentValue(FLOORLET, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY);
    CurrencyAmount pvFloorletOtm = PRICER.presentValue(FLOORLET_NEGATIVE, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY);
    double factor = NOTIONAL * COUPON.getYearFraction();
    assertEquals(pv.getAmount(), OBS_INDEX * factor, NOTIONAL * TOL);
    assertEquals(pvCapletOtm.getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(pvCapletItm.getAmount(), (OBS_INDEX - STRIKE_NEGATIVE) * factor, NOTIONAL * TOL);
    assertEquals(pvFloorletItm.getAmount(), (STRIKE - OBS_INDEX) * factor, NOTIONAL * TOL);
    assertEquals(pvFloorletOtm.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValue_afterPayment() {
    CurrencyAmount pv = PRICER.presentValue(COUPON, RATES_PROVIDER_AFTER_PAY, VOLATILITIES_AFTER_PAY);
    CurrencyAmount pvCaplet = PRICER.presentValue(CAPLET, RATES_PROVIDER_AFTER_PAY, VOLATILITIES_AFTER_PAY);
    CurrencyAmount pvFloorlet = PRICER.presentValue(FLOORLET, RATES_PROVIDER_AFTER_PAY, VOLATILITIES_AFTER_PAY);
    assertEquals(pv, CurrencyAmount.zero(EUR));
    assertEquals(pvCaplet, CurrencyAmount.zero(EUR));
    assertEquals(pvFloorlet, CurrencyAmount.zero(EUR));
  }

  public void test_presentValue_afterFix_noTimeSeries() {
    assertThrowsIllegalArg(() -> PRICER.presentValue(COUPON, RATES_PROVIDER_NO_TS, VOLATILITIES_NO_TS));
    assertThrowsIllegalArg(() -> PRICER.presentValue(CAPLET, RATES_PROVIDER_NO_TS, VOLATILITIES_NO_TS));
    assertThrowsIllegalArg(() -> PRICER.presentValue(FLOORLET, RATES_PROVIDER_NO_TS, VOLATILITIES_NO_TS));
  }
  
  public void test_presentValue_cap_floor_parity() { 
    // Cap/Floor parity is not perfect as the cash swaption standard formula is not arbitrage free.
    CurrencyAmount pvCap = PRICER.presentValue(CAPLET, RATES_PROVIDER, VOLATILITIES_SHIFT);
    CurrencyAmount pvFloor = PRICER.presentValue(FLOORLET, RATES_PROVIDER, VOLATILITIES_SHIFT);
    CurrencyAmount pvCpn = PRICER.presentValue(COUPON, RATES_PROVIDER, VOLATILITIES_SHIFT);
    double pvStrike = STRIKE * NOTIONAL * ACC_FACTOR * RATES_PROVIDER.discountFactor(EUR, PAYMENT);
    assertEquals(pvCap.getAmount() - pvFloor.getAmount(), pvCpn.getAmount() - pvStrike, 1.0E+3);    
    CurrencyAmount pvCap1 = PRICER.presentValue(CAPLET_NEGATIVE, RATES_PROVIDER, VOLATILITIES_SHIFT);
    CurrencyAmount pvFloor1 = PRICER.presentValue(FLOORLET_NEGATIVE, RATES_PROVIDER, VOLATILITIES_SHIFT);
    CurrencyAmount pvCpn1 = PRICER.presentValue(COUPON, RATES_PROVIDER, VOLATILITIES_SHIFT);
    double pvStrike1 = STRIKE_NEGATIVE * NOTIONAL * ACC_FACTOR * RATES_PROVIDER.discountFactor(EUR, PAYMENT);
    assertEquals(pvCap1.getAmount() - pvFloor1.getAmount(), pvCpn1.getAmount() - pvStrike1, 1.0E+3);
    CurrencyAmount pvCap2 = PRICER.presentValue(CAPLET, RATES_PROVIDER, VOLATILITIES);
    CurrencyAmount pvFloor2 = PRICER.presentValue(FLOORLET, RATES_PROVIDER, VOLATILITIES);
    CurrencyAmount pvCpn2 = PRICER.presentValue(COUPON, RATES_PROVIDER, VOLATILITIES);
    double pvStrike2 = STRIKE * NOTIONAL * ACC_FACTOR * RATES_PROVIDER.discountFactor(EUR, PAYMENT);
    assertEquals(pvCap2.getAmount() - pvFloor2.getAmount(), pvCpn2.getAmount() - pvStrike2, 1.0E+3);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    PointSensitivityBuilder pvPointCoupon = PRICER.presentValueSensitivityRates(COUPON_SELL, RATES_PROVIDER, VOLATILITIES);
    CurrencyParameterSensitivities computedCoupon = RATES_PROVIDER
        .parameterSensitivity(pvPointCoupon.build());
    CurrencyParameterSensitivities expectedCoupon = FD_CAL.sensitivity(
        RATES_PROVIDER, p -> PRICER.presentValue(COUPON_SELL, p, VOLATILITIES));
    assertTrue(computedCoupon.equalWithTolerance(expectedCoupon, EPS * NOTIONAL * 50d));
    PointSensitivityBuilder pvCapPoint = PRICER.presentValueSensitivityRates(CAPLET_SELL, RATES_PROVIDER, VOLATILITIES);
    CurrencyParameterSensitivities computedCap = RATES_PROVIDER.parameterSensitivity(pvCapPoint.build());
    CurrencyParameterSensitivities expectedCap = FD_CAL.sensitivity(
        RATES_PROVIDER, p -> PRICER.presentValue(CAPLET_SELL, p, VOLATILITIES));
    assertTrue(computedCap.equalWithTolerance(expectedCap, EPS * NOTIONAL * 50d));
    PointSensitivityBuilder pvFloorPoint = PRICER.presentValueSensitivityRates(FLOORLET_SELL, RATES_PROVIDER, VOLATILITIES);
    CurrencyParameterSensitivities computedFloor = RATES_PROVIDER.parameterSensitivity(pvFloorPoint.build());
    CurrencyParameterSensitivities expectedFloor = FD_CAL.sensitivity(
        RATES_PROVIDER, p -> PRICER.presentValue(FLOORLET_SELL, p, VOLATILITIES));
    assertTrue(computedFloor.equalWithTolerance(expectedFloor, EPS * NOTIONAL * 10d));
  }

  public void test_presentValueSensitivity_shift() {
//    CurrencyAmount tmp = PRICER.presentValue(COUPON, RATES_PROVIDER, VOLATILITIES_SHIFT);
    PointSensitivityBuilder pvPointCoupon = PRICER.presentValueSensitivityRates(COUPON, RATES_PROVIDER, VOLATILITIES_SHIFT);
    CurrencyParameterSensitivities computedCoupon = RATES_PROVIDER
        .parameterSensitivity(pvPointCoupon.build());
    CurrencyParameterSensitivities expectedCoupon = FD_CAL.sensitivity(
        RATES_PROVIDER, p -> PRICER.presentValue(COUPON, p, VOLATILITIES_SHIFT));
    assertTrue(computedCoupon.equalWithTolerance(expectedCoupon, EPS * NOTIONAL * 50d));
    PointSensitivityBuilder pvCapPoint = PRICER.presentValueSensitivityRates(CAPLET_NEGATIVE, RATES_PROVIDER, VOLATILITIES_SHIFT);
    CurrencyParameterSensitivities computedCap = RATES_PROVIDER.parameterSensitivity(pvCapPoint.build());
    CurrencyParameterSensitivities expectedCap = FD_CAL.sensitivity(
        RATES_PROVIDER, p -> PRICER.presentValue(CAPLET_NEGATIVE, p, VOLATILITIES_SHIFT));
    assertTrue(computedCap.equalWithTolerance(expectedCap, EPS * NOTIONAL * 50d));
    PointSensitivityBuilder pvFloorPoint = PRICER.presentValueSensitivityRates(FLOORLET_NEGATIVE, RATES_PROVIDER, VOLATILITIES_SHIFT);
    CurrencyParameterSensitivities computedFloor = RATES_PROVIDER.parameterSensitivity(pvFloorPoint.build());
    CurrencyParameterSensitivities expectedFloor = FD_CAL.sensitivity(
        RATES_PROVIDER, p -> PRICER.presentValue(FLOORLET_NEGATIVE, p, VOLATILITIES_SHIFT));
    assertTrue(computedFloor.equalWithTolerance(expectedFloor, EPS * NOTIONAL * 10d));
  }

  public void test_presentValueSensitivity_onFix() {
    PointSensitivityBuilder pvPointCoupon =
        PRICER.presentValueSensitivityRates(COUPON_SELL, RATES_PROVIDER_ON_FIX, VOLATILITIES_ON_FIX);
    CurrencyParameterSensitivities computedCoupon =
        RATES_PROVIDER_ON_FIX.parameterSensitivity(pvPointCoupon.build());
    CurrencyParameterSensitivities expectedCoupon =
        FD_CAL.sensitivity(RATES_PROVIDER_ON_FIX, p -> PRICER.presentValue(COUPON_SELL, p, VOLATILITIES_ON_FIX));
    assertTrue(computedCoupon.equalWithTolerance(expectedCoupon, EPS * NOTIONAL * 50d));
    PointSensitivityBuilder pvCapPoint =
        PRICER.presentValueSensitivityRates(CAPLET_SELL, RATES_PROVIDER_ON_FIX, VOLATILITIES_ON_FIX);
    CurrencyParameterSensitivities computedCap =
        RATES_PROVIDER_ON_FIX.parameterSensitivity(pvCapPoint.build());
    CurrencyParameterSensitivities expectedCap =
        FD_CAL.sensitivity(RATES_PROVIDER_ON_FIX, p -> PRICER.presentValue(CAPLET_SELL, p, VOLATILITIES_ON_FIX));
    assertTrue(computedCap.equalWithTolerance(expectedCap, EPS * NOTIONAL * 80d));
    PointSensitivityBuilder pvFloorPoint =
        PRICER.presentValueSensitivityRates(FLOORLET_SELL, RATES_PROVIDER_ON_FIX, VOLATILITIES_ON_FIX);
    CurrencyParameterSensitivities computedFloor =
        RATES_PROVIDER_ON_FIX.parameterSensitivity(pvFloorPoint.build());
    CurrencyParameterSensitivities expectedFloor =
        FD_CAL.sensitivity(RATES_PROVIDER_ON_FIX, p -> PRICER.presentValue(FLOORLET_SELL, p, VOLATILITIES_ON_FIX));
    assertTrue(computedFloor.equalWithTolerance(expectedFloor, EPS * NOTIONAL * 50d));
  }

  public void test_presentValueSensitivity_afterFix() {
    PointSensitivityBuilder pvPointCoupon =
        PRICER.presentValueSensitivityRates(COUPON_SELL, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX);
    CurrencyParameterSensitivities computedCoupon =
        RATES_PROVIDER_AFTER_FIX.parameterSensitivity(pvPointCoupon.build());
    CurrencyParameterSensitivities expectedCoupon =
        FD_CAL.sensitivity(RATES_PROVIDER_AFTER_FIX, p -> PRICER.presentValue(COUPON_SELL, p, VOLATILITIES_AFTER_FIX));
    assertTrue(computedCoupon.equalWithTolerance(expectedCoupon, EPS * NOTIONAL));
    PointSensitivityBuilder pvCapPoint =
        PRICER.presentValueSensitivityRates(CAPLET_SELL, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX);
    CurrencyParameterSensitivities computedCap =
        RATES_PROVIDER_AFTER_FIX.parameterSensitivity(pvCapPoint.build());
    CurrencyParameterSensitivities expectedCap =
        FD_CAL.sensitivity(RATES_PROVIDER_AFTER_FIX, p -> PRICER.presentValue(CAPLET_SELL, p, VOLATILITIES_AFTER_FIX));
    assertTrue(computedCap.equalWithTolerance(expectedCap, EPS * NOTIONAL));
    PointSensitivityBuilder pvFloorPoint =
        PRICER.presentValueSensitivityRates(FLOORLET_SELL, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX);
    CurrencyParameterSensitivities computedFloor =
        RATES_PROVIDER_AFTER_FIX.parameterSensitivity(pvFloorPoint.build());
    CurrencyParameterSensitivities expectedFloor =
        FD_CAL.sensitivity(RATES_PROVIDER_AFTER_FIX, p -> PRICER.presentValue(FLOORLET_SELL, p, VOLATILITIES_AFTER_FIX));
    assertTrue(computedFloor.equalWithTolerance(expectedFloor, EPS * NOTIONAL));
  }

  public void test_presentValueSensitivity_onPayment() {
    PointSensitivityBuilder pvSensi = PRICER
        .presentValueSensitivityRates(COUPON, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY);
    PointSensitivityBuilder pvSensiCapletOtm =
        PRICER.presentValueSensitivityRates(CAPLET, RATES_PROVIDER_ON_PAY, VOLATILITIES_AFTER_FIX);
    PointSensitivityBuilder pvSensiCapletItm =
        PRICER.presentValueSensitivityRates(CAPLET_NEGATIVE, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY);
    PointSensitivityBuilder pvSensiFloorletItm =
        PRICER.presentValueSensitivityRates(FLOORLET, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY);
    PointSensitivityBuilder pvSensiFloorletOtm =
        PRICER.presentValueSensitivityRates(FLOORLET_NEGATIVE, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY);
    double paymentTime = RATES_PROVIDER_ON_PAY.discountFactors(EUR).relativeYearFraction(PAYMENT);
    PointSensitivityBuilder expected = ZeroRateSensitivity.of(EUR, paymentTime, -0d);
    assertEquals(pvSensi, expected);
    assertEquals(pvSensiCapletOtm, expected);
    assertEquals(pvSensiCapletItm, expected);
    assertEquals(pvSensiFloorletItm, expected);
    assertEquals(pvSensiFloorletOtm, expected);
  }

  public void test_presentValueSensitivity_afterFix_noTimeSeries() {
    assertThrowsIllegalArg(() -> PRICER.presentValueSensitivityRates(COUPON, RATES_PROVIDER_NO_TS, VOLATILITIES_NO_TS));
    assertThrowsIllegalArg(() -> PRICER.presentValueSensitivityRates(CAPLET, RATES_PROVIDER_NO_TS, VOLATILITIES_NO_TS));
    assertThrowsIllegalArg(() -> PRICER.presentValueSensitivityRates(FLOORLET, RATES_PROVIDER_NO_TS, VOLATILITIES_NO_TS));
  }

  public void test_presentValueSensitivity_afterPayment() {
    PointSensitivityBuilder pt = PRICER.presentValueSensitivityRates(COUPON, RATES_PROVIDER_AFTER_PAY, VOLATILITIES_AFTER_PAY);
    PointSensitivityBuilder ptCap = 
        PRICER.presentValueSensitivityRates(CAPLET, RATES_PROVIDER_AFTER_PAY, VOLATILITIES_AFTER_PAY);
    PointSensitivityBuilder ptFloor =
        PRICER.presentValueSensitivityRates(FLOORLET, RATES_PROVIDER_AFTER_PAY, VOLATILITIES_AFTER_PAY);
    assertEquals(pt, PointSensitivityBuilder.none());
    assertEquals(ptCap, PointSensitivityBuilder.none());
    assertEquals(ptFloor, PointSensitivityBuilder.none());
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivitySabrParameter() {
    testPresentValueSensitivitySabrParameter(
        COUPON_SELL, CAPLET_SELL, FLOORLET_SELL, RATES_PROVIDER, VOLATILITIES);
    testPresentValueSensitivitySabrParameter(
        COUPON, CAPLET_NEGATIVE, FLOORLET_NEGATIVE, RATES_PROVIDER, VOLATILITIES_SHIFT);
    testPresentValueSensitivitySabrParameter(
        COUPON_SELL, CAPLET_SELL, FLOORLET_SELL, RATES_PROVIDER_ON_FIX, VOLATILITIES_ON_FIX);
  }

  public void test_presentValueSensitivitySabrParameter_afterFix() {
    PointSensitivityBuilder pvCouponPoint =
        PRICER.presentValueSensitivityModelParamsSabr(COUPON_SELL, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX);
    PointSensitivityBuilder pvCapPoint =
        PRICER.presentValueSensitivityModelParamsSabr(CAPLET_SELL, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX);
    PointSensitivityBuilder pvFloorPoint =
        PRICER.presentValueSensitivityModelParamsSabr(FLOORLET_SELL, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX);
    assertEquals(pvCouponPoint, PointSensitivityBuilder.none());
    assertEquals(pvCapPoint, PointSensitivityBuilder.none());
    assertEquals(pvFloorPoint, PointSensitivityBuilder.none());
  }

  public void test_presentValueSensitivitySabrParameter_onPayment() {
    PointSensitivityBuilder pvSensi = PRICER
        .presentValueSensitivityModelParamsSabr(COUPON, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY);
    PointSensitivityBuilder pvSensiCapletOtm =
        PRICER.presentValueSensitivityModelParamsSabr(CAPLET, RATES_PROVIDER_ON_PAY, VOLATILITIES_AFTER_FIX);
    PointSensitivityBuilder pvSensiCapletItm =
        PRICER.presentValueSensitivityModelParamsSabr(CAPLET_NEGATIVE, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY);
    PointSensitivityBuilder pvSensiFloorletItm =
        PRICER.presentValueSensitivityModelParamsSabr(FLOORLET, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY);
    PointSensitivityBuilder pvSensiFloorletOtm =
        PRICER.presentValueSensitivityModelParamsSabr(FLOORLET_NEGATIVE, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY);
    assertEquals(pvSensi, PointSensitivityBuilder.none());
    assertEquals(pvSensiCapletOtm, PointSensitivityBuilder.none());
    assertEquals(pvSensiCapletItm, PointSensitivityBuilder.none());
    assertEquals(pvSensiFloorletItm, PointSensitivityBuilder.none());
    assertEquals(pvSensiFloorletOtm, PointSensitivityBuilder.none());
  }

  public void test_presentValueSensitivitySabrParameter_afterFix_noTimeSeries() {
    assertThrowsIllegalArg(() -> PRICER.presentValueSensitivityModelParamsSabr(COUPON, RATES_PROVIDER_NO_TS,
        VOLATILITIES_NO_TS));
    assertThrowsIllegalArg(() -> PRICER.presentValueSensitivityModelParamsSabr(CAPLET, RATES_PROVIDER_NO_TS,
        VOLATILITIES_NO_TS));
    assertThrowsIllegalArg(() -> PRICER.presentValueSensitivityModelParamsSabr(FLOORLET, RATES_PROVIDER_NO_TS,
        VOLATILITIES_NO_TS));
  }

  public void test_presentValueSensitivitySabrParameter_afterPayment() {
    PointSensitivityBuilder sensi =
        PRICER.presentValueSensitivityModelParamsSabr(COUPON, RATES_PROVIDER_AFTER_PAY, VOLATILITIES_AFTER_PAY);
    PointSensitivityBuilder sensiCap =
        PRICER.presentValueSensitivityModelParamsSabr(CAPLET, RATES_PROVIDER_AFTER_PAY, VOLATILITIES_AFTER_PAY);
    PointSensitivityBuilder sensiFloor =
        PRICER.presentValueSensitivityModelParamsSabr(FLOORLET, RATES_PROVIDER_AFTER_PAY, VOLATILITIES_AFTER_PAY);
    assertEquals(sensi, PointSensitivityBuilder.none());
    assertEquals(sensiCap, PointSensitivityBuilder.none());
    assertEquals(sensiFloor, PointSensitivityBuilder.none());
  }
  
  public void test_adjusted_forward_rate() {
    CmsPeriod coupon1 = COUPON.toBuilder().notional(1.0).yearFraction(1.0).build();
    CurrencyAmount pvBuy = PRICER.presentValue(coupon1, RATES_PROVIDER, VOLATILITIES);
    double df = RATES_PROVIDER.discountFactor(EUR, PAYMENT);
    double adjustedForwardRateExpected = pvBuy.getAmount() / df;
    double adjustedForwardRateComputed = PRICER.adjustedForwardRate(COUPON, RATES_PROVIDER, VOLATILITIES);
    assertEquals(adjustedForwardRateComputed, adjustedForwardRateExpected, TOL);
  }
  
  public void test_adjustment_forward_rate() {
    double adjustedForwardRateComputed = PRICER.adjustedForwardRate(COUPON, RATES_PROVIDER, VOLATILITIES);
    double forward = PRICER_SWAP.parRate(COUPON.getUnderlyingSwap(), RATES_PROVIDER);
    double adjustmentComputed = PRICER.adjustmentToForwardRate(COUPON, RATES_PROVIDER, VOLATILITIES);
    assertEquals(adjustmentComputed, adjustedForwardRateComputed - forward, TOL);
  }
  

  public void test_adjusted_forward_rate_cap_floor() {
    double adjustedForwardRateCoupon = PRICER.adjustedForwardRate(COUPON, RATES_PROVIDER, VOLATILITIES);
    double adjustedForwardRateFloor = PRICER.adjustedForwardRate(FLOORLET, RATES_PROVIDER, VOLATILITIES);
    assertEquals(adjustedForwardRateCoupon, adjustedForwardRateFloor, TOL);
    double adjustedForwardRateCap = PRICER.adjustedForwardRate(CAPLET, RATES_PROVIDER, VOLATILITIES);
    assertEquals(adjustedForwardRateCoupon, adjustedForwardRateCap, TOL);
  }
  
  public void test_adjusted_forward_rate_afterFix() {
    double adjustedForward = PRICER.adjustedForwardRate(COUPON, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX);
    assertEquals(adjustedForward, OBS_INDEX , TOL);    
  }

  public void test_adjusted_rate_error() {
    assertThrowsIllegalArg(() -> PRICER.adjustmentToForwardRate(COUPON, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX));
  }

  //-------------------------------------------------------------------------
  private static final CmsPeriod CAPLET_UP = createCmsCaplet(true, STRIKE + EPS);
  private static final CmsPeriod CAPLET_DW = createCmsCaplet(true, STRIKE - EPS);
  private static final CmsPeriod FLOORLET_UP = createCmsFloorlet(true, STRIKE + EPS);
  private static final CmsPeriod FLOORLET_DW = createCmsFloorlet(true, STRIKE - EPS);

  public void test_presentValueSensitivityStrike() {
    double computedCaplet = PRICER.presentValueSensitivityStrike(CAPLET, RATES_PROVIDER, VOLATILITIES);
    double expectedCaplet = 0.5 * (PRICER.presentValue(CAPLET_UP, RATES_PROVIDER, VOLATILITIES).getAmount()
        - PRICER.presentValue(CAPLET_DW, RATES_PROVIDER, VOLATILITIES).getAmount()) / EPS;
    assertEquals(computedCaplet, expectedCaplet, NOTIONAL * EPS);
    double computedFloorlet = PRICER.presentValueSensitivityStrike(FLOORLET, RATES_PROVIDER, VOLATILITIES);
    double expectedFloorlet = 0.5 * (PRICER.presentValue(FLOORLET_UP, RATES_PROVIDER, VOLATILITIES).getAmount()
        - PRICER.presentValue(FLOORLET_DW, RATES_PROVIDER, VOLATILITIES).getAmount()) / EPS;
    assertEquals(computedFloorlet, expectedFloorlet, NOTIONAL * EPS);
  }

  public void test_presentValueSensitivityStrike_shift() {
    double computedCaplet = PRICER.presentValueSensitivityStrike(CAPLET_NEGATIVE, RATES_PROVIDER, VOLATILITIES_SHIFT);
    CmsPeriod capletUp = createCmsCaplet(true, STRIKE_NEGATIVE + EPS);
    CmsPeriod capletDw = createCmsCaplet(true, STRIKE_NEGATIVE - EPS);
    double expectedCaplet = 0.5 * (PRICER.presentValue(capletUp, RATES_PROVIDER, VOLATILITIES_SHIFT).getAmount()
        - PRICER.presentValue(capletDw, RATES_PROVIDER, VOLATILITIES_SHIFT).getAmount()) / EPS;
    assertEquals(computedCaplet, expectedCaplet, NOTIONAL * EPS);
    double computedFloorlet = PRICER.presentValueSensitivityStrike(FLOORLET_NEGATIVE, RATES_PROVIDER, VOLATILITIES_SHIFT);
    CmsPeriod floorletUp = createCmsFloorlet(true, STRIKE_NEGATIVE + EPS);
    CmsPeriod floorletDw = createCmsFloorlet(true, STRIKE_NEGATIVE - EPS);
    double expectedFloorlet = 0.5 * (PRICER.presentValue(floorletUp, RATES_PROVIDER, VOLATILITIES_SHIFT).getAmount()
        - PRICER.presentValue(floorletDw, RATES_PROVIDER, VOLATILITIES_SHIFT).getAmount()) / EPS;
    assertEquals(computedFloorlet, expectedFloorlet, NOTIONAL * EPS);
  }

  public void test_presentValueSensitivityStrike_onFix() {
    double computedCaplet = PRICER.presentValueSensitivityStrike(CAPLET, RATES_PROVIDER_ON_FIX, VOLATILITIES_ON_FIX);
    double expectedCaplet = 0.5 *
        (PRICER.presentValue(CAPLET_UP, RATES_PROVIDER_ON_FIX, VOLATILITIES_ON_FIX).getAmount()
        - PRICER.presentValue(CAPLET_DW, RATES_PROVIDER_ON_FIX, VOLATILITIES_ON_FIX).getAmount()) / EPS;
    assertEquals(computedCaplet, expectedCaplet, NOTIONAL * EPS);
    double computedFloorlet = PRICER
        .presentValueSensitivityStrike(FLOORLET, RATES_PROVIDER_ON_FIX, VOLATILITIES_ON_FIX);
    double expectedFloorlet = 0.5 *
        (PRICER.presentValue(FLOORLET_UP, RATES_PROVIDER_ON_FIX, VOLATILITIES_ON_FIX).getAmount()
        - PRICER.presentValue(FLOORLET_DW, RATES_PROVIDER_ON_FIX, VOLATILITIES_ON_FIX).getAmount()) / EPS;
    assertEquals(computedFloorlet, expectedFloorlet, NOTIONAL * EPS * 10d);
  }

  public void test_presentValueSensitivityStrike_afterFix() {
    double cmpCapletOtm =
        PRICER.presentValueSensitivityStrike(CAPLET, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX);
    double cmpCapletItm =
        PRICER.presentValueSensitivityStrike(CAPLET_NEGATIVE, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX);
    double cmpFloorletItm =
        PRICER.presentValueSensitivityStrike(FLOORLET, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX);
    double cmpFloorletOtm =
        PRICER.presentValueSensitivityStrike(FLOORLET_NEGATIVE, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX);
    double expCapletOtm = (PRICER.presentValue(CAPLET_UP, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX).getAmount()
        - PRICER.presentValue(CAPLET_DW, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX).getAmount()) * 0.5 / EPS;
    double expCapletItm = (PRICER.presentValue(createCmsCaplet(true, STRIKE_NEGATIVE + EPS), RATES_PROVIDER_AFTER_FIX,
        VOLATILITIES_AFTER_FIX).getAmount() - PRICER.presentValue(createCmsCaplet(true, STRIKE_NEGATIVE - EPS),
        RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX).getAmount()) * 0.5 / EPS;
    double expFloorletItm = (PRICER.presentValue(FLOORLET_UP, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX).getAmount()
        - PRICER.presentValue(FLOORLET_DW, RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX).getAmount()) * 0.5 / EPS;
    double expFloorletOtm = (PRICER.presentValue(createCmsFloorlet(true, STRIKE_NEGATIVE + EPS), RATES_PROVIDER_AFTER_FIX,
        VOLATILITIES_AFTER_FIX).getAmount() - PRICER.presentValue(createCmsFloorlet(true, STRIKE_NEGATIVE - EPS),
        RATES_PROVIDER_AFTER_FIX, VOLATILITIES_AFTER_FIX).getAmount()) * 0.5 / EPS;
    assertEquals(cmpCapletOtm, expCapletOtm, NOTIONAL * EPS);
    assertEquals(cmpCapletItm, expCapletItm, NOTIONAL * EPS);
    assertEquals(cmpFloorletOtm, expFloorletOtm, NOTIONAL * EPS);
    assertEquals(cmpFloorletItm, expFloorletItm, NOTIONAL * EPS);
  }

  public void test_presentValueSensitivityStrike_onPayment() {
    double computedCaplet = PRICER.presentValueSensitivityStrike(CAPLET, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY);
    double expectedCaplet = (PRICER.presentValue(CAPLET_UP, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY).getAmount()
        - PRICER.presentValue(CAPLET_DW, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY).getAmount()) * 0.5 / EPS;
    assertEquals(computedCaplet, expectedCaplet, NOTIONAL * EPS);
    double computedFloorlet = PRICER
        .presentValueSensitivityStrike(FLOORLET, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY);
    double expectedFloorlet = (PRICER.presentValue(FLOORLET_UP, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY).getAmount()
        - PRICER.presentValue(FLOORLET_DW, RATES_PROVIDER_ON_PAY, VOLATILITIES_ON_PAY).getAmount()) * 0.5 / EPS;
    assertEquals(computedFloorlet, expectedFloorlet, NOTIONAL * EPS);
  }

  public void test_presentValueSensitivityStrike_afterFix_noTimeSeries() {
    assertThrowsIllegalArg(() -> PRICER.presentValueSensitivityStrike(COUPON, RATES_PROVIDER_NO_TS, VOLATILITIES_NO_TS));
    assertThrowsIllegalArg(() -> PRICER.presentValueSensitivityStrike(CAPLET, RATES_PROVIDER_NO_TS, VOLATILITIES_NO_TS));
    assertThrowsIllegalArg(() -> PRICER.presentValueSensitivityStrike(FLOORLET, RATES_PROVIDER_NO_TS, VOLATILITIES_NO_TS));
  }

  public void test_presentValueSensitivityStrike_afterPayment() {
    double sensiCap = PRICER.presentValueSensitivityStrike(CAPLET, RATES_PROVIDER_AFTER_PAY, VOLATILITIES_AFTER_PAY);
    double sensiFloor = PRICER.presentValueSensitivityStrike(FLOORLET, RATES_PROVIDER_AFTER_PAY, VOLATILITIES_AFTER_PAY);
    assertEquals(sensiCap, 0d);
    assertEquals(sensiFloor, 0d);
  }

  public void test_presentValueSensitivityStrike_coupon() {
    assertThrowsIllegalArg(() -> PRICER.presentValueSensitivityStrike(COUPON, RATES_PROVIDER, VOLATILITIES));
  }

  //-------------------------------------------------------------------------
  private void testPresentValueSensitivitySabrParameter(CmsPeriod coupon, CmsPeriod caplet, CmsPeriod foorlet,
      RatesProvider ratesProvider, SabrParametersSwaptionVolatilities volatilities) {
    PointSensitivities pvPointCoupon =
        PRICER.presentValueSensitivityModelParamsSabr(coupon, ratesProvider, volatilities).build();
    CurrencyParameterSensitivities computedCoupon =
        volatilities.parameterSensitivity(pvPointCoupon);
    PointSensitivities pvCapPoint =
        PRICER.presentValueSensitivityModelParamsSabr(caplet, ratesProvider, volatilities).build();
    CurrencyParameterSensitivities computedCap =
        volatilities.parameterSensitivity(pvCapPoint);
    PointSensitivities pvFloorPoint =
        PRICER.presentValueSensitivityModelParamsSabr(foorlet, ratesProvider, volatilities).build();
    CurrencyParameterSensitivities computedFloor =
        volatilities.parameterSensitivity(pvFloorPoint);

    SabrInterestRateParameters sabr = volatilities.getParameters();
    // alpha surface
    InterpolatedNodalSurface surfaceAlpha = (InterpolatedNodalSurface) sabr.getAlphaSurface();
    CurrencyParameterSensitivity sensiCouponAlpha = computedCoupon.getSensitivity(surfaceAlpha.getName(), EUR);
    int nParamsAlpha = surfaceAlpha.getParameterCount();
    for (int i = 0; i < nParamsAlpha; ++i) {
      InterpolatedNodalSurface[] bumpedSurfaces = bumpSurface(surfaceAlpha, i);
      SabrInterestRateParameters sabrUp = SabrInterestRateParameters.of(bumpedSurfaces[0], sabr.getBetaSurface(),
          sabr.getRhoSurface(), sabr.getNuSurface(), sabr.getShiftSurface(), SabrVolatilityFormula.hagan());
      SabrInterestRateParameters sabrDw = SabrInterestRateParameters.of(bumpedSurfaces[1], sabr.getBetaSurface(),
          sabr.getRhoSurface(), sabr.getNuSurface(), sabr.getShiftSurface(), SabrVolatilityFormula.hagan());
      testSensitivityValue(
          coupon, caplet, foorlet, ratesProvider, i,
          sensiCouponAlpha.getSensitivity(),
          computedCap.getSensitivity(surfaceAlpha.getName(), EUR).getSensitivity(),
          computedFloor.getSensitivity(surfaceAlpha.getName(), EUR).getSensitivity(),
          replaceSabrParameters(sabrUp, volatilities),
          replaceSabrParameters(sabrDw, volatilities));
    }
    // beta surface
    InterpolatedNodalSurface surfaceBeta = (InterpolatedNodalSurface) sabr.getBetaSurface();
    CurrencyParameterSensitivity sensiCouponBeta = computedCoupon.getSensitivity(surfaceBeta.getName(), EUR);
    int nParamsBeta = surfaceBeta.getParameterCount();
    for (int i = 0; i < nParamsBeta; ++i) {
      InterpolatedNodalSurface[] bumpedSurfaces = bumpSurface(surfaceBeta, i);
      SabrInterestRateParameters sabrUp = SabrInterestRateParameters.of(sabr.getAlphaSurface(), bumpedSurfaces[0],
          sabr.getRhoSurface(), sabr.getNuSurface(), sabr.getShiftSurface(), SabrVolatilityFormula.hagan());
      SabrInterestRateParameters sabrDw = SabrInterestRateParameters.of(sabr.getAlphaSurface(), bumpedSurfaces[1],
          sabr.getRhoSurface(), sabr.getNuSurface(), sabr.getShiftSurface(), SabrVolatilityFormula.hagan());
      testSensitivityValue(
          coupon, caplet, foorlet, ratesProvider, i,
          sensiCouponBeta.getSensitivity(),
          computedCap.getSensitivity(surfaceBeta.getName(), EUR).getSensitivity(),
          computedFloor.getSensitivity(surfaceBeta.getName(), EUR).getSensitivity(),
          replaceSabrParameters(sabrUp, volatilities),
          replaceSabrParameters(sabrDw, volatilities));
    }
    // rho surface
    InterpolatedNodalSurface surfaceRho = (InterpolatedNodalSurface) sabr.getRhoSurface();
    CurrencyParameterSensitivity sensiCouponRho = computedCoupon.getSensitivity(surfaceRho.getName(), EUR);
    int nParamsRho = surfaceRho.getParameterCount();
    for (int i = 0; i < nParamsRho; ++i) {
      InterpolatedNodalSurface[] bumpedSurfaces = bumpSurface(surfaceRho, i);
      SabrInterestRateParameters sabrUp = SabrInterestRateParameters.of(sabr.getAlphaSurface(), sabr.getBetaSurface(),
          bumpedSurfaces[0], sabr.getNuSurface(), sabr.getShiftSurface(), SabrVolatilityFormula.hagan());
      SabrInterestRateParameters sabrDw = SabrInterestRateParameters.of(sabr.getAlphaSurface(), sabr.getBetaSurface(),
          bumpedSurfaces[1], sabr.getNuSurface(), sabr.getShiftSurface(), SabrVolatilityFormula.hagan());
      testSensitivityValue(
          coupon, caplet, foorlet, ratesProvider, i,
          sensiCouponRho.getSensitivity(),
          computedCap.getSensitivity(surfaceRho.getName(), EUR).getSensitivity(),
          computedFloor.getSensitivity(surfaceRho.getName(), EUR).getSensitivity(),
          replaceSabrParameters(sabrUp, volatilities),
          replaceSabrParameters(sabrDw, volatilities));
    }
    // nu surface
    InterpolatedNodalSurface surfaceNu = (InterpolatedNodalSurface) sabr.getNuSurface();
    CurrencyParameterSensitivity sensiCouponNu = computedCoupon.getSensitivity(surfaceNu.getName(), EUR);
    int nParamsNu = surfaceNu.getParameterCount();
    for (int i = 0; i < nParamsNu; ++i) {
      InterpolatedNodalSurface[] bumpedSurfaces = bumpSurface(surfaceNu, i);
      SabrInterestRateParameters sabrUp = SabrInterestRateParameters.of(sabr.getAlphaSurface(), sabr.getBetaSurface(),
          sabr.getRhoSurface(), bumpedSurfaces[0], sabr.getShiftSurface(), SabrVolatilityFormula.hagan());
      SabrInterestRateParameters sabrDw = SabrInterestRateParameters.of(sabr.getAlphaSurface(), sabr.getBetaSurface(),
          sabr.getRhoSurface(), bumpedSurfaces[1], sabr.getShiftSurface(), SabrVolatilityFormula.hagan());
      testSensitivityValue(
          coupon, caplet, foorlet, ratesProvider, i,
          sensiCouponNu.getSensitivity(),
          computedCap.getSensitivity(surfaceNu.getName(), EUR).getSensitivity(),
          computedFloor.getSensitivity(surfaceNu.getName(), EUR).getSensitivity(),
          replaceSabrParameters(sabrUp, volatilities),
          replaceSabrParameters(sabrDw, volatilities));
    }
  }

  private InterpolatedNodalSurface[] bumpSurface(InterpolatedNodalSurface surface, int position) {
    DoubleArray zValues = surface.getZValues();
    InterpolatedNodalSurface surfaceUp = surface.withZValues(zValues.with(position, zValues.get(position) + EPS));
    InterpolatedNodalSurface surfaceDw = surface.withZValues(zValues.with(position, zValues.get(position) - EPS));
    return new InterpolatedNodalSurface[] {surfaceUp, surfaceDw };
  }

  private SabrParametersSwaptionVolatilities replaceSabrParameters(
      SabrInterestRateParameters sabrParams,
      SabrParametersSwaptionVolatilities orgVols) {
    return SabrParametersSwaptionVolatilities.of(
        SwaptionVolatilitiesName.of("Test-SABR"), orgVols.getConvention(), orgVols.getValuationDateTime(), sabrParams);
  }

  private void testSensitivityValue(
      CmsPeriod coupon, CmsPeriod caplet, CmsPeriod floorlet, RatesProvider ratesProvider, int index,
      DoubleArray computedCouponSensi, DoubleArray computedCapSensi, DoubleArray computedFloorSensi,
      SabrParametersSwaptionVolatilities volsUp, SabrParametersSwaptionVolatilities volsDw) {
    double expectedCoupon = 0.5 * (PRICER.presentValue(coupon, ratesProvider, volsUp).getAmount()
        - PRICER.presentValue(coupon, ratesProvider, volsDw).getAmount()) / EPS;
    double expectedCap = 0.5 * (PRICER.presentValue(caplet, ratesProvider, volsUp).getAmount()
        - PRICER.presentValue(caplet, ratesProvider, volsDw).getAmount()) / EPS;
    double expectedFloor = 0.5 * (PRICER.presentValue(floorlet, ratesProvider, volsUp).getAmount()
        - PRICER.presentValue(floorlet, ratesProvider, volsDw).getAmount()) / EPS;
    assertEquals(computedCouponSensi.get(index), expectedCoupon, EPS * NOTIONAL * 10d);
    assertEquals(computedCapSensi.get(index), expectedCap, EPS * NOTIONAL * 10d);
    assertEquals(computedFloorSensi.get(index), expectedFloor, EPS * NOTIONAL * 10d);
  }

  private static CmsPeriod createCmsCoupon(boolean isBuy) {
    double notional = isBuy ? NOTIONAL : -NOTIONAL;
    return CmsPeriod.builder()
        .dayCount(ACT_360)
        .currency(EUR)
        .index(EUR_EURIBOR_1100_5Y)
        .startDate(START)
        .endDate(END)
        .fixingDate(FIXING)
        .notional(notional)
        .paymentDate(PAYMENT)
        .yearFraction(ACC_FACTOR)
        .underlyingSwap(createUnderlyingSwap(FIXING))
        .build();
  }

  private static CmsPeriod createCmsCaplet(boolean isBuy, double strike) {
    double notional = isBuy ? NOTIONAL : -NOTIONAL;
    return CmsPeriod.builder()
        .dayCount(ACT_360)
        .currency(EUR)
        .index(EUR_EURIBOR_1100_5Y)
        .startDate(START)
        .endDate(END)
        .fixingDate(FIXING)
        .notional(notional)
        .paymentDate(PAYMENT)
        .yearFraction(ACC_FACTOR)
        .caplet(strike)
        .underlyingSwap(createUnderlyingSwap(FIXING))
        .build();
  }

  private static CmsPeriod createCmsFloorlet(boolean isBuy, double strike) {
    double notional = isBuy ? NOTIONAL : -NOTIONAL;
    return CmsPeriod.builder()
        .dayCount(ACT_360)
        .currency(EUR)
        .index(EUR_EURIBOR_1100_5Y)
        .startDate(START)
        .endDate(END)
        .fixingDate(FIXING)
        .notional(notional)
        .paymentDate(PAYMENT)
        .yearFraction(ACC_FACTOR)
        .floorlet(strike)
        .underlyingSwap(createUnderlyingSwap(FIXING))
        .build();
  }

  // creates and resolves the underlying swap
  private static ResolvedSwap createUnderlyingSwap(LocalDate fixingDate) {
    FixedIborSwapConvention conv = EUR_EURIBOR_1100_5Y.getTemplate().getConvention();
    LocalDate effectiveDate = conv.calculateSpotDateFromTradeDate(fixingDate, REF_DATA);
    LocalDate maturityDate = effectiveDate.plus(EUR_EURIBOR_1100_5Y.getTemplate().getTenor());
    Swap swap = conv.toTrade(fixingDate, effectiveDate, maturityDate, BuySell.BUY, 1d, 1d).getProduct();
    return swap.resolve(REF_DATA);
  }

  //-------------------------------------------------------------------------
  private static final double TOLERANCE_K_P = 1.0E-8;
  private static final double TOLERANCE_K_PP = 1.0E-4;
  private static final double TOLERANCE_PV = 1.0E+0;

  /* Check that the internal function used in the integrant (h, G and k in the documentation) are correctly implemented. */
  public void integrant_internal() {
    SwapIndex index = CAPLET.getIndex();
    LocalDate effectiveDate = CAPLET.getUnderlyingSwap().getStartDate();
    ResolvedSwap expanded = CAPLET.getUnderlyingSwap();
    double tenor = VOLATILITIES_SHIFT.tenor(effectiveDate, CAPLET.getUnderlyingSwap().getEndDate());
    double theta = VOLATILITIES_SHIFT.relativeTime(
        CAPLET.getFixingDate().atTime(index.getFixingTime()).atZone(index.getFixingZone()));
    double delta = index.getTemplate().getConvention().getFixedLeg()
        .getDayCount().relativeYearFraction(effectiveDate, PAYMENT);
    double S0 = PRICER_SWAP.parRate(COUPON.getUnderlyingSwap(), RATES_PROVIDER);
    CmsIntegrantProvider integrant = new CmsIntegrantProvider(CAPLET, expanded, STRIKE, tenor, theta,
        S0, -delta, VOLATILITIES_SHIFT, CUT_OFF_STRIKE, MU);
    // Integrant internal
    double h = integrant.h(STRIKE);
    double hExpected = Math.pow(1 + STRIKE, -delta);
    assertEquals(h, hExpected, TOLERANCE_K_P);
    double g = integrant.g(STRIKE);
    double gExpected = (1.0 - 1.0 / Math.pow(1 + STRIKE, tenor)) / STRIKE;
    assertEquals(g, gExpected, TOLERANCE_K_P);
    double kExpected = integrant.h(STRIKE) / integrant.g(STRIKE);
    double k = integrant.k(STRIKE);
    assertEquals(k, kExpected, TOLERANCE_K_P);
    double shiftFd = 1.0E-5;
    double kP = integrant.h(STRIKE + shiftFd) / integrant.g(STRIKE + shiftFd);
    double kM = integrant.h(STRIKE - shiftFd) / integrant.g(STRIKE - shiftFd);
    double[] kpkpp = integrant.kpkpp(STRIKE);
    assertEquals(kpkpp[0], (kP - kM) / (2 * shiftFd), TOLERANCE_K_P);
    assertEquals(kpkpp[1], (kP + kM - 2 * k) / (shiftFd * shiftFd), TOLERANCE_K_PP);
  }

  /* Check the present value v.  */
  public void test_presentValue_replication_cap() {
    SwapIndex index = CAPLET.getIndex();
    LocalDate effectiveDate = CAPLET.getUnderlyingSwap().getStartDate();
    ResolvedSwap expanded = CAPLET.getUnderlyingSwap();
    double tenor = VOLATILITIES.tenor(effectiveDate, CAPLET.getUnderlyingSwap().getEndDate());
    double theta = VOLATILITIES.relativeTime(
        CAPLET.getFixingDate().atTime(index.getFixingTime()).atZone(index.getFixingZone()));
    double delta = index.getTemplate().getConvention().getFixedLeg()
        .getDayCount().relativeYearFraction(effectiveDate, PAYMENT);
    double ptp = RATES_PROVIDER.discountFactor(EUR, PAYMENT);
    double S0 = PRICER_SWAP.parRate(COUPON.getUnderlyingSwap(), RATES_PROVIDER);
    CmsIntegrantProvider integrant = new CmsIntegrantProvider(CAPLET, expanded, STRIKE, tenor, theta,
        S0, -delta, VOLATILITIES_SHIFT, CUT_OFF_STRIKE, MU);
    // Strike part
    double h_1S0 = 1.0 / integrant.h(S0);
    double gS0 = integrant.g(S0);
    double kK = integrant.k(STRIKE);
    double bsS0 = integrant.bs(STRIKE);
    double strikePart = ptp * h_1S0 * gS0 * kK * bsS0;
    // Integral part
    RungeKuttaIntegrator1D integrator = new RungeKuttaIntegrator1D(1.0E-7, 1.0E-10, 10);
    double integralPart = ptp * integrator.integrate(integrant.integrant(), STRIKE, 100.0);
    double pvExpected = (strikePart + integralPart) * NOTIONAL * ACC_FACTOR;
    CurrencyAmount pvComputed = PRICER.presentValue(CAPLET, RATES_PROVIDER, VOLATILITIES_SHIFT);
    assertEquals(pvComputed.getAmount(),  pvExpected, TOLERANCE_PV);    
  }

  //---------------------------------------------------------------------
  public void test_explainPresentValue() {
    ExplainMapBuilder builder = ExplainMap.builder();
    PRICER.explainPresentValue(FLOORLET, RATES_PROVIDER, VOLATILITIES, builder);
    ExplainMap explain = builder.build();
    //Test a CMS Floorlet Period.
    assertEquals(explain.get(ExplainKey.ENTRY_TYPE).get(), "CmsFloorletPeriod");
    assertEquals(explain.get(ExplainKey.STRIKE_VALUE).get(), 0.04d);
    assertEquals(explain.get(ExplainKey.NOTIONAL).get().getAmount(), 10000000d);
    assertEquals(explain.get(ExplainKey.PAYMENT_DATE).get(), LocalDate.of(2021, 04, 28));
    assertEquals(explain.get(ExplainKey.DISCOUNT_FACTOR).get(), 0.8518053333230845d);
    assertEquals(explain.get(ExplainKey.START_DATE).get(), LocalDate.of(2020, 04, 28));
    assertEquals(explain.get(ExplainKey.END_DATE).get(), LocalDate.of(2021, 04, 28));
    assertEquals(explain.get(ExplainKey.FIXING_DATE).get(), LocalDate.of(2020, 04, 24));
    assertEquals(explain.get(ExplainKey.ACCRUAL_YEAR_FRACTION).get(), 1.0138888888888888d);
    double forwardSwapRate = PRICER_SWAP.parRate(FLOORLET.getUnderlyingSwap(), RATES_PROVIDER);
    assertEquals(explain.get(ExplainKey.FORWARD_RATE).get(), forwardSwapRate);
    CurrencyAmount pv = PRICER.presentValue(FLOORLET, RATES_PROVIDER, VOLATILITIES);
    assertEquals(explain.get(ExplainKey.PRESENT_VALUE).get(), pv);
    double adjustedForwardRate = PRICER.adjustedForwardRate(FLOORLET, RATES_PROVIDER, VOLATILITIES);
    assertEquals(explain.get(ExplainKey.CONVEXITY_ADJUSTED_RATE).get(), adjustedForwardRate);
    
  }
  
  //-------------------------------------------------------------------------
  /** Simplified integrant for testing; only cap; underlying with annual payments */
  private class CmsIntegrantProvider {
    private final int nbFixedPeriod;
    private final double eta;
    private final double strike;
    private final double shift;
    private final double factor;
    private final SabrExtrapolationRightFunction sabrExtrapolation;

    public CmsIntegrantProvider(
        CmsPeriod cmsPeriod,
        ResolvedSwap swap,
        double strike,
        double tenor,
        double timeToExpiry,
        double forward,
        double eta,
        SabrParametersSwaptionVolatilities swaptionVolatilities,
        double cutOffStrike,
        double mu) {

      ResolvedSwapLeg fixedLeg = swap.getLegs(SwapLegType.FIXED).get(0);
      this.nbFixedPeriod = fixedLeg.getPaymentPeriods().size();
      this.eta = eta;
      SabrInterestRateParameters params = swaptionVolatilities.getParameters();
      SabrFormulaData sabrPoint = SabrFormulaData.of(params.alpha(timeToExpiry, tenor),
          params.beta(timeToExpiry, tenor), params.rho(timeToExpiry, tenor), params.nu(timeToExpiry, tenor));
      this.shift = params.shift(timeToExpiry, tenor);
      this.sabrExtrapolation = SabrExtrapolationRightFunction
          .of(forward + shift, timeToExpiry, sabrPoint, cutOffStrike + shift, mu);
      this.strike = strike;
      this.factor = g(forward) / h(forward);
    }

    /**
     * Obtains the integrant used in price replication.
     * 
     * @return the integrant
     */
    Function<Double, Double> integrant(){
      return new Function<Double, Double>() {
        @Override
        public Double apply(Double x) {
          double[] kD = kpkpp(x);
          // Implementation note: kD[0] contains the first derivative of k; kD[1] the second derivative of k.
          return factor * (kD[1] * (x - strike) + 2d * kD[0]) * bs(x);
        }
      };
    }

    /**
     * The approximation of the discount factor as function of the swap rate.
     * 
     * @param x  the swap rate.
     * @return the discount factor.
     */
    double h(double x) {
      return Math.pow(1d + x, eta);
    }

    /**
     * The cash annuity.
     * 
     * @param x  the swap rate.
     * @return the annuity.
     */
    double g(double x) {
        double periodFactor = 1d + x;
        double nPeriodDiscount = Math.pow(periodFactor, -nbFixedPeriod);
        return (1d - nPeriodDiscount) / x;
    }

    /**
     * The factor used in the strike part and in the integration of the replication.
     * 
     * @param x  the swap rate.
     * @return the factor.
     */
    double k(double x) {
      double g;
      double h;
        double periodFactor = 1d + x;
        double nPeriodDiscount = Math.pow(periodFactor, -nbFixedPeriod);
        g = (1d - nPeriodDiscount) / x;
        h = Math.pow(1.0 + x, eta);
      return h / g;
    }

    /**
     * The first and second derivative of the function k.
     * <p>
     * The first element is the first derivative and the second element is second derivative.
     * 
     * @param x  the swap rate.
     * @return the derivatives
     */
    protected double[] kpkpp(double x) {
      double periodFactor = 1d + x;
      double nPeriodDiscount = Math.pow(periodFactor, -nbFixedPeriod);
      /*The value of the annuity and its first and second derivative. */
      double g, gp, gpp;
        g = (1d - nPeriodDiscount) / x;
        gp = -g / x + nbFixedPeriod * nPeriodDiscount / (x  * periodFactor);
        gpp = 2d / (x * x) * g - 2d * nbFixedPeriod * nPeriodDiscount / (x * x * periodFactor)
            - (nbFixedPeriod + 1d) * nbFixedPeriod * nPeriodDiscount
            / (x * periodFactor * periodFactor);
      double h = Math.pow(1d + x, eta);
      double hp = eta * h / periodFactor;
      double hpp = (eta - 1d) * hp / periodFactor;
      double kp = hp / g - h * gp / (g * g);
      double kpp = hpp / g - 2d * hp * gp / (g * g) - h * (gpp / (g * g) - 2d * (gp * gp) / (g * g * g));
      return new double[] {kp, kpp };
    }

    /**
     * The Black price with numeraire 1 as function of the strike.
     * 
     * @param strike  the strike.
     * @return the Black price.
     */
    double bs(double strike) {
      return sabrExtrapolation.price(strike + shift, PutCall.CALL);
    }
  }

}
