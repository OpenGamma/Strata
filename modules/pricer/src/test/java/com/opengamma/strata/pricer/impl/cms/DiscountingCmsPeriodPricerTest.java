/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.cms;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.product.swap.SwapIndices.EUR_EURIBOR_1100_5Y;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateSensitivity;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.pricer.swaption.SwaptionSabrRateVolatilityDataSet;
import com.opengamma.strata.product.cms.CmsPeriod;
import com.opengamma.strata.product.common.BuySell;
import com.opengamma.strata.product.swap.ResolvedSwap;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.type.FixedIborSwapConvention;

/**
 * Test {@link DiscountingCmsPeriodPricer}.
 */
@Test
public class DiscountingCmsPeriodPricerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate VALUATION = LocalDate.of(2010, 8, 18);
  // Coupon CMS
  private static final LocalDate FIXING = LocalDate.of(2020, 4, 24);
  private static final LocalDate START = LocalDate.of(2020, 4, 28);
  private static final LocalDate END = LocalDate.of(2021, 4, 28);
  private static final LocalDate AFTER_FIXING = LocalDate.of(2020, 8, 11);
  private static final LocalDate PAYMENT = LocalDate.of(2021, 4, 28);
  private static final LocalDate AFTER_PAYMENT = LocalDate.of(2021, 4, 29);
  private static final double ACC_FACTOR = ACT_360.relativeYearFraction(START, END);
  private static final double NOTIONAL = 100_000_000; // 100m
  private static final double STRIKE = 0.04;
  private static final double STRIKE_NEGATIVE = -0.01;
  private static final CmsPeriod COUPON = createCmsCoupon(true);
  private static final CmsPeriod CAPLET = createCmsCaplet(true, STRIKE);
  private static final CmsPeriod FLOORLET = createCmsFloorlet(true, STRIKE);
  private static final CmsPeriod COUPON_SELL = createCmsCoupon(false);
  private static final CmsPeriod CAPLET_NEGATIVE = createCmsCaplet(true, STRIKE_NEGATIVE);
  private static final CmsPeriod FLOORLET_NEGATIVE = createCmsFloorlet(true, STRIKE_NEGATIVE);
  // Providers
  private static final double OBS_INDEX = 0.0135;
  private static final LocalDateDoubleTimeSeries TIME_SERIES = LocalDateDoubleTimeSeries.of(FIXING, OBS_INDEX);
  private static final ImmutableRatesProvider RATES_PROVIDER =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(VALUATION);
  // providers - on fixing date, no time series
  private static final ImmutableRatesProvider RATES_PROVIDER_ON_FIX =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(FIXING);
  private static final ImmutableRatesProvider RATES_PROVIDER_ON_FIX_TS =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(FIXING, TIME_SERIES);
  // providers - after fixing date, no time series
  private static final ImmutableRatesProvider RATES_PROVIDER_NO_TS =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(AFTER_FIXING);
  // providers - between fixing date and payment date
  private static final ImmutableRatesProvider RATES_PROVIDER_AFTER_FIX =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(AFTER_FIXING, TIME_SERIES);
  // providers - on payment date
  private static final ImmutableRatesProvider RATES_PROVIDER_ON_PAY =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(PAYMENT, TIME_SERIES);
  // providers - ended
  private static final ImmutableRatesProvider RATES_PROVIDER_AFTER_PAY =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderEur(AFTER_PAYMENT, TIME_SERIES);
  // Pricer
  private static final DiscountingCmsPeriodPricer PRICER_CMS = DiscountingCmsPeriodPricer.DEFAULT;
  private static final DiscountingSwapProductPricer PRICER_SWAP = DiscountingSwapProductPricer.DEFAULT;

  private static final double TOLERANCE_PV = 1.0E-2;
  private static final double TOLERANCE_DELTA = 1.0E+2;

  // Present Value
  public void presentValue_beforeFixing_coupon() {
    CurrencyAmount pv = PRICER_CMS.presentValue(COUPON, RATES_PROVIDER);
    double df = RATES_PROVIDER.discountFactor(EUR, END);
    double forward = PRICER_SWAP.parRate(COUPON.getUnderlyingSwap(), RATES_PROVIDER);
    assertEquals(pv.getAmount(), forward * df * NOTIONAL * ACC_FACTOR, TOLERANCE_PV);    
  }

  public void presentValue_beforeFixing_capfloor() {
    assertThrowsIllegalArg(() -> PRICER_CMS.presentValue(CAPLET, RATES_PROVIDER));
    assertThrowsIllegalArg(() -> PRICER_CMS.presentValue(FLOORLET, RATES_PROVIDER));
  }

  public void presentValue_buySell() {
    CurrencyAmount pvBuy = PRICER_CMS.presentValue(COUPON, RATES_PROVIDER);
    CurrencyAmount pvSell = PRICER_CMS.presentValue(COUPON_SELL, RATES_PROVIDER);
    assertEquals(pvBuy.getAmount(), -pvSell.getAmount(), TOLERANCE_PV);
  }

  public void presentValue_onFix_nots() {
    CurrencyAmount pv = PRICER_CMS.presentValue(COUPON, RATES_PROVIDER_ON_FIX);
    double factor = RATES_PROVIDER_ON_FIX.discountFactor(EUR, PAYMENT) * NOTIONAL * COUPON.getYearFraction();
    double forward = PRICER_SWAP.parRate(COUPON.getUnderlyingSwap(), RATES_PROVIDER_ON_FIX);
    assertEquals(pv.getAmount(), forward * factor, TOLERANCE_PV);
  }

  public void presentValue_onFix_ts() {
    CurrencyAmount pv = PRICER_CMS.presentValue(COUPON, RATES_PROVIDER_ON_FIX_TS);
    CurrencyAmount pvCapletOtm = PRICER_CMS.presentValue(CAPLET, RATES_PROVIDER_ON_FIX_TS);
    CurrencyAmount pvCapletItm = PRICER_CMS.presentValue(CAPLET_NEGATIVE, RATES_PROVIDER_ON_FIX_TS);
    CurrencyAmount pvFloorletItm = PRICER_CMS.presentValue(FLOORLET, RATES_PROVIDER_ON_FIX_TS);
    CurrencyAmount pvFloorletOtm = PRICER_CMS.presentValue(FLOORLET_NEGATIVE, RATES_PROVIDER_ON_FIX_TS);
    double factor = RATES_PROVIDER_ON_FIX_TS.discountFactor(EUR, PAYMENT) * NOTIONAL * COUPON.getYearFraction();
    assertEquals(pv.getAmount(), OBS_INDEX * factor, TOLERANCE_PV);
    assertEquals(pvCapletOtm.getAmount(), 0d, TOLERANCE_PV);
    assertEquals(pvCapletItm.getAmount(), (OBS_INDEX - STRIKE_NEGATIVE) * factor, TOLERANCE_PV);
    assertEquals(pvFloorletItm.getAmount(), (STRIKE - OBS_INDEX) * factor, TOLERANCE_PV);
    assertEquals(pvFloorletOtm.getAmount(), 0d, TOLERANCE_PV);
  }

  public void presentValue_afterFix() {
    CurrencyAmount pv = PRICER_CMS.presentValue(COUPON, RATES_PROVIDER_AFTER_FIX);
    CurrencyAmount pvCapletOtm = PRICER_CMS.presentValue(CAPLET, RATES_PROVIDER_AFTER_FIX);
    CurrencyAmount pvCapletItm = PRICER_CMS.presentValue(CAPLET_NEGATIVE, RATES_PROVIDER_AFTER_FIX);
    CurrencyAmount pvFloorletItm = PRICER_CMS.presentValue(FLOORLET, RATES_PROVIDER_AFTER_FIX);
    CurrencyAmount pvFloorletOtm = PRICER_CMS.presentValue(FLOORLET_NEGATIVE, RATES_PROVIDER_AFTER_FIX);
    double factor = RATES_PROVIDER_AFTER_FIX.discountFactor(EUR, PAYMENT) * NOTIONAL * COUPON.getYearFraction();
    assertEquals(pv.getAmount(), OBS_INDEX * factor, TOLERANCE_PV);
    assertEquals(pvCapletOtm.getAmount(), 0d, TOLERANCE_PV);
    assertEquals(pvCapletItm.getAmount(), (OBS_INDEX - STRIKE_NEGATIVE) * factor, TOLERANCE_PV);
    assertEquals(pvFloorletItm.getAmount(), (STRIKE - OBS_INDEX) * factor, TOLERANCE_PV);
    assertEquals(pvFloorletOtm.getAmount(), 0d, TOLERANCE_PV);
  }

  public void presentValue_onPayment() {
    CurrencyAmount pv = PRICER_CMS.presentValue(COUPON, RATES_PROVIDER_ON_PAY);
    CurrencyAmount pvCapletOtm = PRICER_CMS.presentValue(CAPLET, RATES_PROVIDER_ON_PAY);
    CurrencyAmount pvCapletItm = PRICER_CMS.presentValue(CAPLET_NEGATIVE, RATES_PROVIDER_ON_PAY);
    CurrencyAmount pvFloorletItm = PRICER_CMS.presentValue(FLOORLET, RATES_PROVIDER_ON_PAY);
    CurrencyAmount pvFloorletOtm = PRICER_CMS.presentValue(FLOORLET_NEGATIVE, RATES_PROVIDER_ON_PAY);
    double factor = NOTIONAL * COUPON.getYearFraction();
    assertEquals(pv.getAmount(), OBS_INDEX * factor, TOLERANCE_PV);
    assertEquals(pvCapletOtm.getAmount(), 0d, TOLERANCE_PV);
    assertEquals(pvCapletItm.getAmount(), (OBS_INDEX - STRIKE_NEGATIVE) * factor, TOLERANCE_PV);
    assertEquals(pvFloorletItm.getAmount(), (STRIKE - OBS_INDEX) * factor, TOLERANCE_PV);
    assertEquals(pvFloorletOtm.getAmount(), 0d, TOLERANCE_PV);
  }

  public void presentValue_afterPayment() {
    CurrencyAmount pv = PRICER_CMS.presentValue(COUPON, RATES_PROVIDER_AFTER_PAY);
    CurrencyAmount pvCaplet = PRICER_CMS.presentValue(CAPLET, RATES_PROVIDER_AFTER_PAY);
    CurrencyAmount pvFloorlet = PRICER_CMS.presentValue(FLOORLET, RATES_PROVIDER_AFTER_PAY);
    assertEquals(pv, CurrencyAmount.zero(EUR));
    assertEquals(pvCaplet, CurrencyAmount.zero(EUR));
    assertEquals(pvFloorlet, CurrencyAmount.zero(EUR));
  }

  public void presentValue_afterFix_noTimeSeries() {
    assertThrowsIllegalArg(() -> PRICER_CMS.presentValue(COUPON, RATES_PROVIDER_NO_TS));
    assertThrowsIllegalArg(() -> PRICER_CMS.presentValue(CAPLET, RATES_PROVIDER_NO_TS));
    assertThrowsIllegalArg(() -> PRICER_CMS.presentValue(FLOORLET, RATES_PROVIDER_NO_TS));
  }

  public void forward_rate() {
    double fwdComputed = PRICER_CMS.forwardRate(COUPON, RATES_PROVIDER);
    double fwdExpected = PRICER_SWAP.parRate(COUPON.getUnderlyingSwap(), RATES_PROVIDER);
    assertEquals(fwdComputed, fwdExpected, TOLERANCE_DELTA);
  }  

  public void forward_rate_after_fixing() {
    assertThrowsIllegalArg(() -> PRICER_CMS.forwardRate(COUPON, RATES_PROVIDER_AFTER_FIX));
  }  

  // Present Value Curve Sensitivity
  public void presentValueSensitivity_beforeFixing_coupon() {
    PointSensitivities pv = PRICER_CMS.presentValueSensitivity(COUPON, RATES_PROVIDER).build();
    double df = RATES_PROVIDER.discountFactor(EUR, PAYMENT);
    ZeroRateSensitivity dfdr = RATES_PROVIDER.discountFactors(EUR).zeroRatePointSensitivity(PAYMENT);
    double forward = PRICER_SWAP.parRate(COUPON.getUnderlyingSwap(), RATES_PROVIDER);
    PointSensitivities forwarddr = PRICER_SWAP.parRateSensitivity(COUPON.getUnderlyingSwap(), RATES_PROVIDER).build();
    PointSensitivities expected = forwarddr.multipliedBy(df).combinedWith(dfdr.multipliedBy(forward).build())
        .multipliedBy(NOTIONAL * ACC_FACTOR);
    assertTrue(pv.equalWithTolerance(expected, TOLERANCE_DELTA));    
  }

  public void presentValueSensitivity_beforeFixing_capfloor() {
    assertThrowsIllegalArg(() -> PRICER_CMS.presentValueSensitivity(CAPLET, RATES_PROVIDER));
    assertThrowsIllegalArg(() -> PRICER_CMS.presentValueSensitivity(FLOORLET, RATES_PROVIDER));
  }

  public void presentValueSensitivity_buySell() {
    PointSensitivityBuilder pvBuy = PRICER_CMS.presentValueSensitivity(COUPON, RATES_PROVIDER);
    PointSensitivityBuilder pvSell = PRICER_CMS.presentValueSensitivity(COUPON_SELL, RATES_PROVIDER);
    CurrencyParameterSensitivities ps = 
        RATES_PROVIDER.parameterSensitivity(pvBuy.combinedWith(pvSell).build());
    assertTrue(ps.equalWithTolerance(CurrencyParameterSensitivities.empty(), TOLERANCE_DELTA));
  }

  public void presentValueSensitivity_onFix_nots() {
    PointSensitivities pv = PRICER_CMS.presentValueSensitivity(COUPON, RATES_PROVIDER_ON_FIX).build();
    double df = RATES_PROVIDER_ON_FIX.discountFactor(EUR, PAYMENT);
    ZeroRateSensitivity dfdr = RATES_PROVIDER_ON_FIX.discountFactors(EUR).zeroRatePointSensitivity(PAYMENT);
    double forward = PRICER_SWAP.parRate(COUPON.getUnderlyingSwap(), RATES_PROVIDER_ON_FIX);
    PointSensitivities forwarddr = PRICER_SWAP.parRateSensitivity(COUPON.getUnderlyingSwap(), RATES_PROVIDER_ON_FIX).build();
    PointSensitivities expected = forwarddr.multipliedBy(df).combinedWith(dfdr.multipliedBy(forward).build())
        .multipliedBy(NOTIONAL * ACC_FACTOR);
    assertTrue(pv.equalWithTolerance(expected, TOLERANCE_DELTA));    
  }

  public void presentValueSensitivity_onFix_ts() {
    PointSensitivities ptsCpn = PRICER_CMS.presentValueSensitivity(COUPON, RATES_PROVIDER_ON_FIX_TS).build();
    PointSensitivities ptsCapletOtm = PRICER_CMS.presentValueSensitivity(CAPLET, RATES_PROVIDER_ON_FIX_TS).build();
    PointSensitivities ptsCapletItm = PRICER_CMS.presentValueSensitivity(CAPLET_NEGATIVE, RATES_PROVIDER_ON_FIX_TS).build();
    PointSensitivities ptsFloorletItm = PRICER_CMS.presentValueSensitivity(FLOORLET, RATES_PROVIDER_ON_FIX_TS).build();
    PointSensitivities ptsFloorletOtm = PRICER_CMS.presentValueSensitivity(FLOORLET_NEGATIVE, RATES_PROVIDER_ON_FIX_TS).build();
    double factor = NOTIONAL * COUPON.getYearFraction();
    ZeroRateSensitivity pts = RATES_PROVIDER_ON_FIX_TS.discountFactors(EUR).zeroRatePointSensitivity(PAYMENT);
    assertTrue(ptsCpn.equalWithTolerance(pts.build().multipliedBy(factor * OBS_INDEX), TOLERANCE_DELTA));
    assertTrue(ptsCapletOtm.equalWithTolerance(pts.build().multipliedBy(0d), TOLERANCE_DELTA));
    assertTrue(ptsCapletItm.equalWithTolerance(pts.build().multipliedBy(factor * (OBS_INDEX - STRIKE_NEGATIVE)), TOLERANCE_DELTA));
    assertTrue(ptsFloorletItm.equalWithTolerance(pts.build().multipliedBy(factor * (STRIKE - OBS_INDEX)), TOLERANCE_DELTA));
    assertTrue(ptsFloorletOtm.equalWithTolerance(pts.build().multipliedBy(0d), TOLERANCE_DELTA));
  }

  public void presentValueSensitivity_afterFix() {
    PointSensitivities ptsCpn = PRICER_CMS.presentValueSensitivity(COUPON, RATES_PROVIDER_AFTER_FIX).build();
    PointSensitivities ptsCapletOtm = PRICER_CMS.presentValueSensitivity(CAPLET, RATES_PROVIDER_AFTER_FIX).build();
    PointSensitivities ptsCapletItm = PRICER_CMS.presentValueSensitivity(CAPLET_NEGATIVE, RATES_PROVIDER_AFTER_FIX).build();
    PointSensitivities ptsFloorletItm = PRICER_CMS.presentValueSensitivity(FLOORLET, RATES_PROVIDER_AFTER_FIX).build();
    PointSensitivities ptsFloorletOtm = PRICER_CMS.presentValueSensitivity(FLOORLET_NEGATIVE, RATES_PROVIDER_AFTER_FIX).build();
    double factor = NOTIONAL * COUPON.getYearFraction();
    ZeroRateSensitivity pts = RATES_PROVIDER_AFTER_FIX.discountFactors(EUR).zeroRatePointSensitivity(PAYMENT);
    assertTrue(ptsCpn.equalWithTolerance(pts.build().multipliedBy(factor * OBS_INDEX), TOLERANCE_DELTA));
    assertTrue(ptsCapletOtm.equalWithTolerance(pts.build().multipliedBy(0d), TOLERANCE_DELTA));
    assertTrue(ptsCapletItm.equalWithTolerance(pts.build().multipliedBy(factor * (OBS_INDEX - STRIKE_NEGATIVE)), TOLERANCE_DELTA));
    assertTrue(ptsFloorletItm.equalWithTolerance(pts.build().multipliedBy(factor * (STRIKE - OBS_INDEX)), TOLERANCE_DELTA));
    assertTrue(ptsFloorletOtm.equalWithTolerance(pts.build().multipliedBy(0d), TOLERANCE_DELTA));
  }

  public void presentValueSensitivity_onPayment() {
    PointSensitivities ptsCpn = PRICER_CMS.presentValueSensitivity(COUPON, RATES_PROVIDER_ON_PAY).build();
    PointSensitivities ptsCapletOtm = PRICER_CMS.presentValueSensitivity(CAPLET, RATES_PROVIDER_ON_PAY).build();
    PointSensitivities ptsCapletItm = PRICER_CMS.presentValueSensitivity(CAPLET_NEGATIVE, RATES_PROVIDER_ON_PAY).build();
    PointSensitivities ptsFloorletItm = PRICER_CMS.presentValueSensitivity(FLOORLET, RATES_PROVIDER_ON_PAY).build();
    PointSensitivities ptsFloorletOtm = PRICER_CMS.presentValueSensitivity(FLOORLET_NEGATIVE, RATES_PROVIDER_ON_PAY).build();
    assertTrue(RATES_PROVIDER_ON_PAY.parameterSensitivity(ptsCpn)
        .equalWithTolerance(CurrencyParameterSensitivities.empty(), TOLERANCE_DELTA));
    assertTrue(RATES_PROVIDER_ON_PAY.parameterSensitivity(ptsCapletOtm)
        .equalWithTolerance(CurrencyParameterSensitivities.empty(), TOLERANCE_DELTA));
    assertTrue(RATES_PROVIDER_ON_PAY.parameterSensitivity(ptsCapletItm)
        .equalWithTolerance(CurrencyParameterSensitivities.empty(), TOLERANCE_DELTA));
    assertTrue(RATES_PROVIDER_ON_PAY.parameterSensitivity(ptsFloorletItm)
        .equalWithTolerance(CurrencyParameterSensitivities.empty(), TOLERANCE_DELTA));
    assertTrue(RATES_PROVIDER_ON_PAY.parameterSensitivity(ptsFloorletOtm)
        .equalWithTolerance(CurrencyParameterSensitivities.empty(), TOLERANCE_DELTA));
  }

  public void presentValueSensitivity_afterPayment() {
    PointSensitivityBuilder pv = PRICER_CMS.presentValueSensitivity(COUPON, RATES_PROVIDER_AFTER_PAY);
    PointSensitivityBuilder pvCaplet = PRICER_CMS.presentValueSensitivity(CAPLET, RATES_PROVIDER_AFTER_PAY);
    PointSensitivityBuilder pvFloorlet = PRICER_CMS.presentValueSensitivity(FLOORLET, RATES_PROVIDER_AFTER_PAY);
    assertEquals(pv, PointSensitivityBuilder.none());
    assertEquals(pvCaplet, PointSensitivityBuilder.none());
    assertEquals(pvFloorlet, PointSensitivityBuilder.none());
  }

  public void presentValueSensitivity_afterFix_noTimeSeries() {
    assertThrowsIllegalArg(() -> PRICER_CMS.presentValueSensitivity(COUPON, RATES_PROVIDER_NO_TS));
    assertThrowsIllegalArg(() -> PRICER_CMS.presentValueSensitivity(CAPLET, RATES_PROVIDER_NO_TS));
    assertThrowsIllegalArg(() -> PRICER_CMS.presentValueSensitivity(FLOORLET, RATES_PROVIDER_NO_TS));
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

  // creates and resolves the underlying swap
  private static ResolvedSwap createUnderlyingSwap(LocalDate fixingDate) {
    FixedIborSwapConvention conv = EUR_EURIBOR_1100_5Y.getTemplate().getConvention();
    LocalDate effectiveDate = conv.calculateSpotDateFromTradeDate(fixingDate, REF_DATA);
    LocalDate maturityDate = effectiveDate.plus(EUR_EURIBOR_1100_5Y.getTemplate().getTenor());
    Swap swap = conv.toTrade(fixingDate, effectiveDate, maturityDate, BuySell.BUY, 1d, 1d).getProduct();
    return swap.resolve(REF_DATA);
  }

}
