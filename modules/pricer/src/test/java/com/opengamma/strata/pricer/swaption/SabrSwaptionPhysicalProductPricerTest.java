/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swaption;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.assertThrowsWithCause;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.BuySell;
import com.opengamma.strata.basics.LongShort;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.Tenor;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivities;
import com.opengamma.strata.market.curve.CurveCurrencyParameterSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.market.sensitivity.SwaptionSabrSensitivity;
import com.opengamma.strata.market.sensitivity.SwaptionSensitivity;
import com.opengamma.strata.market.surface.ConstantNodalSurface;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivities;
import com.opengamma.strata.market.surface.SurfaceCurrencyParameterSensitivity;
import com.opengamma.strata.market.surface.SurfaceMetadata;
import com.opengamma.strata.market.surface.SurfaceParameterMetadata;
import com.opengamma.strata.market.surface.meta.SwaptionSurfaceExpiryTenorNodeMetadata;
import com.opengamma.strata.market.view.SwaptionVolatilities;
import com.opengamma.strata.pricer.impl.option.BlackFormulaRepository;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.sensitivity.RatesFiniteDifferenceSensitivityCalculator;
import com.opengamma.strata.pricer.swap.DiscountingSwapProductPricer;
import com.opengamma.strata.product.swap.Swap;
import com.opengamma.strata.product.swap.SwapLegType;
import com.opengamma.strata.product.swaption.CashSettlement;
import com.opengamma.strata.product.swaption.CashSettlementMethod;
import com.opengamma.strata.product.swaption.PhysicalSettlement;
import com.opengamma.strata.product.swaption.Swaption;
import com.opengamma.strata.product.swaption.SwaptionSettlement;

/**
 * Test {@link SabrSwaptionPhysicalProductPricer}.
 */
@Test
public class SabrSwaptionPhysicalProductPricerTest {

  private static final LocalDate VAL_DATE = LocalDate.of(2014, 1, 22);
  // swaptions
  private static final double NOTIONAL = 100000000; //100m
  private static final double RATE = 0.0350;
  private static final int TENOR_YEAR = 7;
  private static final Tenor TENOR = Tenor.ofYears(TENOR_YEAR);
  private static final ZonedDateTime MATURITY_DATE = LocalDate.of(2016, 1, 22).atStartOfDay(ZoneOffset.UTC); // 2Y
  private static final Swap SWAP_PAY = SwaptionSabrRateVolatilityDataSet.SWAP_CONVENTION_USD.createTrade(
      MATURITY_DATE.toLocalDate(), TENOR, BuySell.BUY, NOTIONAL, RATE).getProduct();
  private static final Swap SWAP_REC = SwaptionSabrRateVolatilityDataSet.SWAP_CONVENTION_USD.createTrade(
      MATURITY_DATE.toLocalDate(), TENOR, BuySell.SELL, NOTIONAL, RATE).getProduct();
  private static final SwaptionSettlement PHYSICAL_SETTLE = PhysicalSettlement.DEFAULT;
  private static final SwaptionSettlement CASH_SETTLE = CashSettlement.builder()
      .cashSettlementMethod(CashSettlementMethod.PAR_YIELD)
      .settlementDate(SWAP_REC.getStartDate())
      .build();
  private static final Swaption SWAPTION_PAY_LONG = Swaption.builder()
      .expiryDate(AdjustableDate.of(MATURITY_DATE.toLocalDate()))
      .expiryTime(MATURITY_DATE.toLocalTime())
      .expiryZone(MATURITY_DATE.getZone())
      .longShort(LongShort.LONG)
      .swaptionSettlement(PHYSICAL_SETTLE)
      .underlying(SWAP_PAY)
      .build();
  private static final Swaption SWAPTION_PAY_SHORT = Swaption.builder()
      .expiryDate(AdjustableDate.of(MATURITY_DATE.toLocalDate()))
      .expiryTime(MATURITY_DATE.toLocalTime())
      .expiryZone(MATURITY_DATE.getZone())
      .longShort(LongShort.SHORT)
      .swaptionSettlement(PHYSICAL_SETTLE)
      .underlying(SWAP_PAY)
      .build();
  private static final Swaption SWAPTION_REC_LONG = Swaption.builder()
      .expiryDate(AdjustableDate.of(MATURITY_DATE.toLocalDate()))
      .expiryTime(MATURITY_DATE.toLocalTime())
      .expiryZone(MATURITY_DATE.getZone())
      .longShort(LongShort.LONG)
      .swaptionSettlement(PHYSICAL_SETTLE)
      .underlying(SWAP_REC)
      .build();
  private static final Swaption SWAPTION_REC_SHORT = Swaption.builder()
      .expiryDate(AdjustableDate.of(MATURITY_DATE.toLocalDate()))
      .expiryTime(MATURITY_DATE.toLocalTime())
      .expiryZone(MATURITY_DATE.getZone())
      .longShort(LongShort.SHORT)
      .swaptionSettlement(PHYSICAL_SETTLE)
      .underlying(SWAP_REC)
      .build();
  private static final Swaption SWAPTION_CASH = Swaption.builder()
      .expiryDate(AdjustableDate.of(MATURITY_DATE.toLocalDate()))
      .expiryTime(MATURITY_DATE.toLocalTime())
      .expiryZone(MATURITY_DATE.getZone())
      .longShort(LongShort.LONG)
      .swaptionSettlement(CASH_SETTLE)
      .underlying(SWAP_REC)
      .build();
  // providers
  private static final ImmutableRatesProvider RATE_PROVIDER =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderUsd(VAL_DATE);
  private static final ImmutableRatesProvider RATE_PROVIDER_AT_MATURITY =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderUsd(MATURITY_DATE.toLocalDate());
  private static final ImmutableRatesProvider RATE_PROVIDER_AFTER_MATURITY =
      SwaptionSabrRateVolatilityDataSet.getRatesProviderUsd(MATURITY_DATE.toLocalDate().plusDays(1));
  private static final SabrParametersSwaptionVolatilities VOL_PROVIDER =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesUsd(VAL_DATE, true);
  private static final SabrParametersSwaptionVolatilities VOL_PROVIDER_AT_MATURITY =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesUsd(MATURITY_DATE.toLocalDate(), true);
  private static final SabrParametersSwaptionVolatilities VOL_PROVIDER_AFTER_MATURITY =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesUsd(MATURITY_DATE.toLocalDate().plusDays(1), true);
  private static final SabrParametersSwaptionVolatilities VOL_PROVIDER_REGRESSION =
      SwaptionSabrRateVolatilityDataSet.getVolatilitiesUsd(VAL_DATE, false);
  // test parameters and calculator
  private static final double TOL = 1.0e-13;
  private static final double TOLERANCE_DELTA = 1.0E-2;
  private static final double FD_EPS = 1.0e-6;
  private static final double REGRESSION_TOL = 1.0e-4; // due to tenor computation difference
  private static final SabrSwaptionPhysicalProductPricer SWAPTION_PRICER = SabrSwaptionPhysicalProductPricer.DEFAULT;
  private static final DiscountingSwapProductPricer SWAP_PRICER = DiscountingSwapProductPricer.DEFAULT;
  private static final RatesFiniteDifferenceSensitivityCalculator FD_CAL =
      new RatesFiniteDifferenceSensitivityCalculator(FD_EPS);

  //-------------------------------------------------------------------------
  public void validate_physical_settlement() {
    assertThrowsIllegalArg(() -> SWAPTION_PRICER.presentValue(SWAPTION_CASH, RATE_PROVIDER, VOL_PROVIDER));
  }

  public void no_pv_gamma() {
    assertThrowsWithCause(() -> SWAPTION_PRICER.presentValueGamma(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER),
        UnsupportedOperationException.class);
  }

  public void no_pv_theta() {
    assertThrowsWithCause(() -> SWAPTION_PRICER.presentValueTheta(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER),
        UnsupportedOperationException.class);
  }

  //-------------------------------------------------------------------------
  public void test_presentValue() {
    CurrencyAmount computedRec = SWAPTION_PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount computedPay = SWAPTION_PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    double forward = SWAP_PRICER.parRate(SWAP_REC, RATE_PROVIDER);
    double pvbp = SWAP_PRICER.getLegPricer().pvbp(SWAP_REC.getLegs(SwapLegType.FIXED).get(0), RATE_PROVIDER);
    double volatility = VOL_PROVIDER.volatility(SWAPTION_REC_LONG.getExpiryDateTime(),
        TENOR_YEAR, RATE, forward);
    double maturity = VOL_PROVIDER.relativeTime(SWAPTION_REC_LONG.getExpiryDateTime());
    double expectedRec = pvbp * BlackFormulaRepository.price(forward + SwaptionSabrRateVolatilityDataSet.SHIFT,
        RATE + SwaptionSabrRateVolatilityDataSet.SHIFT, maturity, volatility, false);
    double expectedPay = -pvbp * BlackFormulaRepository.price(forward + SwaptionSabrRateVolatilityDataSet.SHIFT,
        RATE + SwaptionSabrRateVolatilityDataSet.SHIFT, maturity, volatility, true);
    assertEquals(computedRec.getCurrency(), USD);
    assertEquals(computedRec.getAmount(), expectedRec, NOTIONAL * TOL);
    assertEquals(computedPay.getCurrency(), USD);
    assertEquals(computedPay.getAmount(), expectedPay, NOTIONAL * TOL);
  }

  public void test_presentValue_atMaturity() {
    CurrencyAmount computedRec =
        SWAPTION_PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    CurrencyAmount computedPay =
        SWAPTION_PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    double swapPV = SWAP_PRICER.presentValue(SWAP_REC, RATE_PROVIDER_AT_MATURITY).getAmount(USD).getAmount();
    assertEquals(computedRec.getAmount(), swapPV, NOTIONAL * TOL);
    assertEquals(computedPay.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValue_afterExpiry() {
    CurrencyAmount computedRec =
        SWAPTION_PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY);
    CurrencyAmount computedPay =
        SWAPTION_PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY);
    assertEquals(computedRec.getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(computedPay.getAmount(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValue_parity() {
    CurrencyAmount pvRecLong = SWAPTION_PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvRecShort = SWAPTION_PRICER.presentValue(SWAPTION_REC_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvPayLong = SWAPTION_PRICER.presentValue(SWAPTION_PAY_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount pvPayShort = SWAPTION_PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvRecLong.getAmount(), -pvRecShort.getAmount(), NOTIONAL * TOL);
    assertEquals(pvPayLong.getAmount(), -pvPayShort.getAmount(), NOTIONAL * TOL);
    double swapPV = SWAP_PRICER.presentValue(SWAP_PAY, RATE_PROVIDER).getAmount(USD).getAmount();
    assertEquals(pvPayLong.getAmount() - pvRecLong.getAmount(), swapPV, NOTIONAL * TOL);
    assertEquals(pvPayShort.getAmount() - pvRecShort.getAmount(), -swapPV, NOTIONAL * TOL);
  }

  public void test_presentValue_parity_atMaturity() {
    CurrencyAmount pvRecLong =
        SWAPTION_PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    CurrencyAmount pvRecShort =
        SWAPTION_PRICER.presentValue(SWAPTION_REC_SHORT, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    CurrencyAmount pvPayLong =
        SWAPTION_PRICER.presentValue(SWAPTION_PAY_LONG, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    CurrencyAmount pvPayShort =
        SWAPTION_PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    assertEquals(pvRecLong.getAmount(), -pvRecShort.getAmount(), NOTIONAL * TOL);
    assertEquals(pvPayLong.getAmount(), -pvPayShort.getAmount(), NOTIONAL * TOL);
    double swapPV = SWAP_PRICER.presentValue(SWAP_PAY, RATE_PROVIDER_AT_MATURITY).getAmount(USD).getAmount();
    assertEquals(pvPayLong.getAmount() - pvRecLong.getAmount(), swapPV, NOTIONAL * TOL);
    assertEquals(pvPayShort.getAmount() - pvRecShort.getAmount(), -swapPV, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_currencyExposure() {
    MultiCurrencyAmount computedRec = SWAPTION_PRICER.currencyExposure(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount computedPay = SWAPTION_PRICER.currencyExposure(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    PointSensitivityBuilder pointRec =
        SWAPTION_PRICER.presentValueSensitivity(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount expectedRec = RATE_PROVIDER.currencyExposure(pointRec.build())
        .plus(SWAPTION_PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER));
    assertEquals(computedRec.size(), 1);
    assertEquals(computedRec.getAmount(USD).getAmount(), expectedRec.getAmount(USD).getAmount(), NOTIONAL * TOL);
    PointSensitivityBuilder pointPay =
        SWAPTION_PRICER.presentValueSensitivity(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    MultiCurrencyAmount expectedPay = RATE_PROVIDER.currencyExposure(pointPay.build())
        .plus(SWAPTION_PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER));
    assertEquals(computedPay.size(), 1);
    assertEquals(computedPay.getAmount(USD).getAmount(), expectedPay.getAmount(USD).getAmount(), NOTIONAL * TOL);
  }

  public void test_currencyExposure_atMaturity() {
    MultiCurrencyAmount computedRec = SWAPTION_PRICER.currencyExposure(
        SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    MultiCurrencyAmount computedPay = SWAPTION_PRICER.currencyExposure(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    PointSensitivityBuilder pointRec =
        SWAPTION_PRICER.presentValueSensitivity(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    MultiCurrencyAmount expectedRec = RATE_PROVIDER.currencyExposure(pointRec.build())
        .plus(SWAPTION_PRICER.presentValue(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY));
    assertEquals(computedRec.size(), 1);
    assertEquals(computedRec.getAmount(USD).getAmount(), expectedRec.getAmount(USD).getAmount(), NOTIONAL * TOL);
    PointSensitivityBuilder pointPay =
        SWAPTION_PRICER.presentValueSensitivity(SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    MultiCurrencyAmount expectedPay = RATE_PROVIDER.currencyExposure(pointPay.build())
        .plus(SWAPTION_PRICER.presentValue(SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY));
    assertEquals(computedPay.size(), 1);
    assertEquals(computedPay.getAmount(USD).getAmount(), expectedPay.getAmount(USD).getAmount(), NOTIONAL * TOL);
  }

  public void test_currencyExposure_afterMaturity() {
    MultiCurrencyAmount computedRec = SWAPTION_PRICER.currencyExposure(
        SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY);
    MultiCurrencyAmount computedPay = SWAPTION_PRICER.currencyExposure(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY);
    assertEquals(computedRec.size(), 1);
    assertEquals(computedRec.getAmount(USD).getAmount(), 0d, NOTIONAL * TOL);
    assertEquals(computedPay.size(), 1);
    assertEquals(computedPay.getAmount(USD).getAmount(), 0d, NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void test_impliedVolatility() {
    double computedRec = SWAPTION_PRICER.impliedVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    double computedPay = SWAPTION_PRICER.impliedVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    double forward = SWAP_PRICER.parRate(SWAP_REC, RATE_PROVIDER);
    double expected = VOL_PROVIDER.volatility(MATURITY_DATE, TENOR_YEAR, RATE, forward);
    assertEquals(computedRec, expected, TOL);
    assertEquals(computedPay, expected, TOL);
  }

  public void test_impliedVolatility_atMaturity() {
    double computedRec =
        SWAPTION_PRICER.impliedVolatility(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    double computedPay =
        SWAPTION_PRICER.impliedVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    double forward = SWAP_PRICER.parRate(SWAP_REC, RATE_PROVIDER_AT_MATURITY);
    double expected = VOL_PROVIDER_AT_MATURITY.volatility(MATURITY_DATE, TENOR_YEAR, RATE, forward);
    assertEquals(computedRec, expected, TOL);
    assertEquals(computedPay, expected, TOL);
  }

  public void test_impliedVolatility_afterMaturity() {
    assertThrowsIllegalArg(() -> SWAPTION_PRICER.impliedVolatility(
        SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY));
    assertThrowsIllegalArg(() -> SWAPTION_PRICER.impliedVolatility(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueDelta_parity() {
    double pvbp = SWAP_PRICER.getLegPricer()
        .pvbp(SWAPTION_REC_LONG.getUnderlying().getLegs(SwapLegType.FIXED).get(0), RATE_PROVIDER);
    CurrencyAmount deltaRec = SWAPTION_PRICER.presentValueDelta(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurrencyAmount deltaPay = SWAPTION_PRICER.presentValueDelta(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(deltaRec.getAmount() + deltaPay.getAmount(), -pvbp, TOLERANCE_DELTA);
  }

  public void test_presentValueDelta_afterMaturity() {
    CurrencyAmount deltaRec =
        SWAPTION_PRICER.presentValueDelta(SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY);
    assertEquals(deltaRec.getAmount(), 0, TOLERANCE_DELTA);
    CurrencyAmount deltaPay =
        SWAPTION_PRICER.presentValueDelta(SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY);
    assertEquals(deltaPay.getAmount(), 0, TOLERANCE_DELTA);
  }

  public void test_presentValueDelta_atMaturity() {
    double forward = SWAP_PRICER.parRate(SWAP_REC, RATE_PROVIDER_AT_MATURITY);
    double pvbp = SWAP_PRICER.getLegPricer()
        .pvbp(SWAPTION_REC_LONG.getUnderlying().getLegs(SwapLegType.FIXED).get(0), RATE_PROVIDER_AT_MATURITY);
    CurrencyAmount deltaRec =
        SWAPTION_PRICER.presentValueDelta(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    assertEquals(deltaRec.getAmount(), RATE > forward ? -pvbp : 0, TOLERANCE_DELTA);
    CurrencyAmount deltaPay =
        SWAPTION_PRICER.presentValueDelta(SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    assertEquals(deltaPay.getAmount(), RATE > forward ? 0 : pvbp, TOLERANCE_DELTA);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivity() {
    PointSensitivityBuilder pointRec =
        SWAPTION_PRICER.presentValueSensitivity(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities computedRec = RATE_PROVIDER.curveParameterSensitivity(pointRec.build());
    CurveCurrencyParameterSensitivities expectedRec =
        FD_CAL.sensitivity(RATE_PROVIDER, (p) -> SWAPTION_PRICER.presentValue(SWAPTION_REC_LONG, (p), VOL_PROVIDER));
    assertTrue(computedRec.equalWithTolerance(expectedRec, NOTIONAL * FD_EPS * 100d));
    PointSensitivityBuilder pointPay =
        SWAPTION_PRICER.presentValueSensitivity(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    CurveCurrencyParameterSensitivities computedPay = RATE_PROVIDER.curveParameterSensitivity(pointPay.build());
    CurveCurrencyParameterSensitivities expectedPay =
        FD_CAL.sensitivity(RATE_PROVIDER, (p) -> SWAPTION_PRICER.presentValue(SWAPTION_PAY_SHORT, (p), VOL_PROVIDER));
    assertTrue(computedPay.equalWithTolerance(expectedPay, NOTIONAL * FD_EPS * 100d));
  }

  public void test_presentValueSensitivity_stickyStrike() {
    SwaptionVolatilities volSabr = SwaptionSabrRateVolatilityDataSet.getVolatilitiesUsd(VAL_DATE, false);
    double impliedVol = SWAPTION_PRICER.impliedVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, volSabr);
    SwaptionVolatilities volCst = BlackSwaptionExpiryTenorVolatilities.of(
        ConstantNodalSurface.of("CST", impliedVol), VOL_PROVIDER.getConvention(),
        VOL_PROVIDER.getValuationDateTime(), VOL_PROVIDER.getDayCount());
    // To obtain a constant volatility surface which create a sticky strike sensitivity
    PointSensitivityBuilder pointRec =
        SWAPTION_PRICER.presentValueSensitivityStickyStrike(SWAPTION_REC_LONG, RATE_PROVIDER, volSabr);
    CurveCurrencyParameterSensitivities computedRec = RATE_PROVIDER.curveParameterSensitivity(pointRec.build());
    CurveCurrencyParameterSensitivities expectedRec =
        FD_CAL.sensitivity(RATE_PROVIDER, (p) -> SWAPTION_PRICER.presentValue(SWAPTION_REC_LONG, (p), volCst));
    assertTrue(computedRec.equalWithTolerance(expectedRec, NOTIONAL * FD_EPS * 100d));

    PointSensitivityBuilder pointPay =
        SWAPTION_PRICER.presentValueSensitivityStickyStrike(SWAPTION_PAY_SHORT, RATE_PROVIDER, volSabr);
    CurveCurrencyParameterSensitivities computedPay = RATE_PROVIDER.curveParameterSensitivity(pointPay.build());
    CurveCurrencyParameterSensitivities expectedPay =
        FD_CAL.sensitivity(RATE_PROVIDER, (p) -> SWAPTION_PRICER.presentValue(SWAPTION_PAY_SHORT, (p), volCst));
    assertTrue(computedPay.equalWithTolerance(expectedPay, NOTIONAL * FD_EPS * 100d));
  }

  public void test_presentValueSensitivity_atMaturity() {
    PointSensitivityBuilder pointRec =
        SWAPTION_PRICER.presentValueSensitivity(SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    CurveCurrencyParameterSensitivities computedRec =
        RATE_PROVIDER_AT_MATURITY.curveParameterSensitivity(pointRec.build());
    CurveCurrencyParameterSensitivities expectedRec = FD_CAL.sensitivity(
        RATE_PROVIDER_AT_MATURITY, (p) -> SWAPTION_PRICER.presentValue(SWAPTION_REC_LONG, (p), VOL_PROVIDER_AT_MATURITY));
    assertTrue(computedRec.equalWithTolerance(expectedRec, NOTIONAL * FD_EPS * 100d));
    PointSensitivities pointPay = SWAPTION_PRICER.presentValueSensitivity(SWAPTION_PAY_SHORT,
        RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY).build();
    for (PointSensitivity sensi : pointPay.getSensitivities()) {
      assertEquals(Math.abs(sensi.getSensitivity()), 0d);
    }
  }

  public void test_presentValueSensitivity_afterMaturity() {
    PointSensitivities pointRec = SWAPTION_PRICER.presentValueSensitivity(
        SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY).build();
    for (PointSensitivity sensi : pointRec.getSensitivities()) {
      assertEquals(Math.abs(sensi.getSensitivity()), 0d);
    }
    PointSensitivities pointPay = SWAPTION_PRICER.presentValueSensitivity(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY).build();
    for (PointSensitivity sensi : pointPay.getSensitivities()) {
      assertEquals(Math.abs(sensi.getSensitivity()), 0d);
    }
  }

  public void test_presentValueSensitivity_parity() {
    CurveCurrencyParameterSensitivities pvSensiRecLong = RATE_PROVIDER.curveParameterSensitivity(
        SWAPTION_PRICER.presentValueSensitivity(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER).build());
    CurveCurrencyParameterSensitivities pvSensiRecShort = RATE_PROVIDER.curveParameterSensitivity(
        SWAPTION_PRICER.presentValueSensitivity(SWAPTION_REC_SHORT, RATE_PROVIDER, VOL_PROVIDER).build());
    CurveCurrencyParameterSensitivities pvSensiPayLong = RATE_PROVIDER.curveParameterSensitivity(
        SWAPTION_PRICER.presentValueSensitivity(SWAPTION_PAY_LONG, RATE_PROVIDER, VOL_PROVIDER).build());
    CurveCurrencyParameterSensitivities pvSensiPayShort = RATE_PROVIDER.curveParameterSensitivity(
        SWAPTION_PRICER.presentValueSensitivity(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER).build());
    assertTrue(pvSensiRecLong.equalWithTolerance(pvSensiRecShort.multipliedBy(-1d), NOTIONAL * TOL));
    assertTrue(pvSensiPayLong.equalWithTolerance(pvSensiPayShort.multipliedBy(-1d), NOTIONAL * TOL));

    CurveCurrencyParameterSensitivities pvSensiSwap = RATE_PROVIDER.curveParameterSensitivity(
        SWAP_PRICER.presentValueSensitivity(SWAP_PAY, RATE_PROVIDER).build());
    assertTrue(pvSensiSwap.equalWithTolerance(pvSensiPayLong.combinedWith(pvSensiRecLong.multipliedBy(-1d)),
        NOTIONAL * TOL));
    assertTrue(pvSensiSwap.equalWithTolerance(pvSensiRecShort.combinedWith(pvSensiPayShort.multipliedBy(-1d)),
        NOTIONAL * TOL));
  }

  //-------------------------------------------------------------------------
  public void test_presentValueVega_parity() {
    SwaptionSensitivity vegaRec = SWAPTION_PRICER
        .presentValueSensitivityVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    SwaptionSensitivity vegaPay = SWAPTION_PRICER
        .presentValueSensitivityVolatility(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(vegaRec.getSensitivity(), -vegaPay.getSensitivity(), TOLERANCE_DELTA);
  }

  public void test_presentValueVega_atMaturity() {
    SwaptionSensitivity vegaRec = SWAPTION_PRICER.presentValueSensitivityVolatility(
        SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    assertEquals(vegaRec.getSensitivity(), 0, TOLERANCE_DELTA);
    SwaptionSensitivity vegaPay = SWAPTION_PRICER.presentValueSensitivityVolatility(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    assertEquals(vegaPay.getSensitivity(), 0, TOLERANCE_DELTA);
  }

  public void test_presentValueVega_afterMaturity() {
    SwaptionSensitivity vegaRec = SWAPTION_PRICER.presentValueSensitivityVolatility(
        SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY);
    assertEquals(vegaRec.getSensitivity(), 0, TOLERANCE_DELTA);
    SwaptionSensitivity vegaPay = SWAPTION_PRICER.presentValueSensitivityVolatility(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY);
    assertEquals(vegaPay.getSensitivity(), 0, TOLERANCE_DELTA);
  }

  public void test_presentValueVega_surface() {
    SwaptionSensitivity vegaRec = SWAPTION_PRICER
        .presentValueSensitivityVolatility(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    assertThrowsWithCause(() -> VOL_PROVIDER.surfaceCurrencyParameterSensitivity(vegaRec),
        UnsupportedOperationException.class);
  }

  //-------------------------------------------------------------------------
  public void test_presentValueSensitivitySabrParameter() {
    SwaptionSabrSensitivity sensiRec =
        SWAPTION_PRICER.presentValueSensitivitySabrParameter(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    SwaptionSabrSensitivity sensiPay =
        SWAPTION_PRICER.presentValueSensitivitySabrParameter(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    double forward = SWAP_PRICER.parRate(SWAP_REC, RATE_PROVIDER);
    double pvbp = SWAP_PRICER.getLegPricer().pvbp(SWAP_REC.getLegs(SwapLegType.FIXED).get(0), RATE_PROVIDER);
    double volatility = VOL_PROVIDER.volatility(SWAPTION_REC_LONG.getExpiryDateTime(),
        TENOR_YEAR, RATE, forward);
    double maturity = VOL_PROVIDER.relativeTime(SWAPTION_REC_LONG.getExpiryDateTime());
    double[] volSensi = VOL_PROVIDER.getParameters()
        .volatilityAdjoint(maturity, TENOR_YEAR, RATE, forward).getDerivatives().toArray();
    double vegaRec = pvbp * BlackFormulaRepository.vega(forward + SwaptionSabrRateVolatilityDataSet.SHIFT,
        RATE + SwaptionSabrRateVolatilityDataSet.SHIFT, maturity, volatility);
    double vegaPay = -pvbp * BlackFormulaRepository.vega(forward + SwaptionSabrRateVolatilityDataSet.SHIFT,
        RATE + SwaptionSabrRateVolatilityDataSet.SHIFT, maturity, volatility);
    assertEquals(sensiRec.getCurrency(), USD);
    assertEquals(sensiRec.getAlphaSensitivity(), vegaRec * volSensi[2], NOTIONAL * TOL);
    assertEquals(sensiRec.getBetaSensitivity(), vegaRec * volSensi[3], NOTIONAL * TOL);
    assertEquals(sensiRec.getRhoSensitivity(), vegaRec * volSensi[4], NOTIONAL * TOL);
    assertEquals(sensiRec.getNuSensitivity(), vegaRec * volSensi[5], NOTIONAL * TOL);
    assertEquals(sensiRec.getConvention(), SwaptionSabrRateVolatilityDataSet.SWAP_CONVENTION_USD);
    assertEquals(sensiRec.getExpiry(), SWAPTION_REC_LONG.getExpiryDateTime());
    assertEquals(sensiRec.getTenor(), (double) TENOR_YEAR);
    assertEquals(sensiPay.getCurrency(), USD);
    assertEquals(sensiPay.getAlphaSensitivity(), vegaPay * volSensi[2], NOTIONAL * TOL);
    assertEquals(sensiPay.getBetaSensitivity(), vegaPay * volSensi[3], NOTIONAL * TOL);
    assertEquals(sensiPay.getRhoSensitivity(), vegaPay * volSensi[4], NOTIONAL * TOL);
    assertEquals(sensiPay.getNuSensitivity(), vegaPay * volSensi[5], NOTIONAL * TOL);
    assertEquals(sensiRec.getConvention(), SwaptionSabrRateVolatilityDataSet.SWAP_CONVENTION_USD);
    assertEquals(sensiPay.getExpiry(), SWAPTION_REC_LONG.getExpiryDateTime());
    assertEquals(sensiPay.getTenor(), (double) TENOR_YEAR);
  }

  public void test_presentValueSensitivitySabrParameter_atMaturity() {
    SwaptionSabrSensitivity sensiRec = SWAPTION_PRICER.presentValueSensitivitySabrParameter(
        SWAPTION_REC_LONG, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    assertEquals(sensiRec.getAlphaSensitivity(), 0d, NOTIONAL * TOL);
    assertEquals(sensiRec.getBetaSensitivity(), 0d, NOTIONAL * TOL);
    assertEquals(sensiRec.getRhoSensitivity(), 0d, NOTIONAL * TOL);
    assertEquals(sensiRec.getNuSensitivity(), 0d, NOTIONAL * TOL);
    SwaptionSabrSensitivity sensiPay = SWAPTION_PRICER.presentValueSensitivitySabrParameter(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AT_MATURITY, VOL_PROVIDER_AT_MATURITY);
    assertEquals(sensiPay.getAlphaSensitivity(), 0d, NOTIONAL * TOL);
    assertEquals(sensiRec.getBetaSensitivity(), 0d, NOTIONAL * TOL);
    assertEquals(sensiRec.getRhoSensitivity(), 0d, NOTIONAL * TOL);
    assertEquals(sensiRec.getNuSensitivity(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValueSensitivitySabrParameter_afterMaturity() {
    SwaptionSabrSensitivity sensiRec = SWAPTION_PRICER.presentValueSensitivitySabrParameter(
        SWAPTION_REC_LONG, RATE_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY);
    assertEquals(sensiRec.getAlphaSensitivity(), 0d, NOTIONAL * TOL);
    assertEquals(sensiRec.getBetaSensitivity(), 0d, NOTIONAL * TOL);
    assertEquals(sensiRec.getRhoSensitivity(), 0d, NOTIONAL * TOL);
    assertEquals(sensiRec.getNuSensitivity(), 0d, NOTIONAL * TOL);
    SwaptionSabrSensitivity sensiPay = SWAPTION_PRICER.presentValueSensitivitySabrParameter(
        SWAPTION_PAY_SHORT, RATE_PROVIDER_AFTER_MATURITY, VOL_PROVIDER_AFTER_MATURITY);
    assertEquals(sensiPay.getAlphaSensitivity(), 0d, NOTIONAL * TOL);
    assertEquals(sensiRec.getBetaSensitivity(), 0d, NOTIONAL * TOL);
    assertEquals(sensiRec.getRhoSensitivity(), 0d, NOTIONAL * TOL);
    assertEquals(sensiRec.getNuSensitivity(), 0d, NOTIONAL * TOL);
  }

  public void test_presentValueSensitivitySabrParameter_parity() {
    SwaptionSabrSensitivity pvSensiRecLong =
        SWAPTION_PRICER.presentValueSensitivitySabrParameter(SWAPTION_REC_LONG, RATE_PROVIDER, VOL_PROVIDER);
    SwaptionSabrSensitivity pvSensiRecShort =
        SWAPTION_PRICER.presentValueSensitivitySabrParameter(SWAPTION_REC_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    SwaptionSabrSensitivity pvSensiPayLong =
        SWAPTION_PRICER.presentValueSensitivitySabrParameter(SWAPTION_PAY_LONG, RATE_PROVIDER, VOL_PROVIDER);
    SwaptionSabrSensitivity pvSensiPayShort =
        SWAPTION_PRICER.presentValueSensitivitySabrParameter(SWAPTION_PAY_SHORT, RATE_PROVIDER, VOL_PROVIDER);
    assertEquals(pvSensiRecLong.getAlphaSensitivity(), -pvSensiRecShort.getAlphaSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiPayLong.getAlphaSensitivity(), -pvSensiPayShort.getAlphaSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiRecLong.getAlphaSensitivity(), pvSensiPayLong.getAlphaSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiPayShort.getAlphaSensitivity(), pvSensiPayShort.getAlphaSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiRecLong.getBetaSensitivity(), -pvSensiRecShort.getBetaSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiPayLong.getBetaSensitivity(), -pvSensiPayShort.getBetaSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiRecLong.getBetaSensitivity(), pvSensiPayLong.getBetaSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiPayShort.getBetaSensitivity(), pvSensiPayShort.getBetaSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiRecLong.getRhoSensitivity(), -pvSensiRecShort.getRhoSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiPayLong.getRhoSensitivity(), -pvSensiPayShort.getRhoSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiRecLong.getRhoSensitivity(), pvSensiPayLong.getRhoSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiPayShort.getRhoSensitivity(), pvSensiPayShort.getRhoSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiRecLong.getNuSensitivity(), -pvSensiRecShort.getNuSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiPayLong.getNuSensitivity(), -pvSensiPayShort.getNuSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiRecLong.getNuSensitivity(), pvSensiPayLong.getNuSensitivity(), NOTIONAL * TOL);
    assertEquals(pvSensiPayShort.getNuSensitivity(), pvSensiPayShort.getNuSensitivity(), NOTIONAL * TOL);
  }

  //-------------------------------------------------------------------------
  public void regressionPv() {
    CurrencyAmount pvComputed = SWAPTION_PRICER.presentValue(SWAPTION_PAY_LONG, RATE_PROVIDER, VOL_PROVIDER_REGRESSION);
    assertEquals(pvComputed.getAmount(), 3156216.489577751, REGRESSION_TOL * NOTIONAL);
  }

  public void regressionPvCurveSensi() {
    PointSensitivityBuilder point =
        SWAPTION_PRICER.presentValueSensitivity(SWAPTION_PAY_LONG, RATE_PROVIDER, VOL_PROVIDER_REGRESSION);
    CurveCurrencyParameterSensitivities sensiComputed = RATE_PROVIDER.curveParameterSensitivity(point.build());
    final double[] deltaDsc = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 109037.92080563342, 637123.4570377409,
        -931862.187003511, -2556192.7520530378, -4233440.216336116, -5686205.439275854, -6160338.898970505,
        -3709275.494841247, 0.0};
    final double[] deltaFwd = {0.0, 0.0, 0.0, 0.0, -1.0223186788452002E8, 2506923.9169937484, 4980364.73045286,
        1.254633556119663E7, 1.528160539036628E8, 2.5824191204559547E8, 0.0, 0.0, 0.0, 0.0, 0.0};
    CurveCurrencyParameterSensitivities sensiExpected = CurveCurrencyParameterSensitivities.of(
        CurveCurrencyParameterSensitivity.of(SwaptionSabrRateVolatilityDataSet.META_DSC_USD, USD, DoubleArray.copyOf(deltaDsc)),
        CurveCurrencyParameterSensitivity.of(SwaptionSabrRateVolatilityDataSet.META_FWD_USD, USD, DoubleArray.copyOf(deltaFwd)));
    assertTrue(sensiComputed.equalWithTolerance(sensiExpected, NOTIONAL * REGRESSION_TOL));
  }

  public void regressionPvSurfaceSensi() {
    SwaptionSabrSensitivity pointComputed =
        SWAPTION_PRICER.presentValueSensitivitySabrParameter(SWAPTION_PAY_LONG, RATE_PROVIDER, VOL_PROVIDER_REGRESSION);
    assertEquals(pointComputed.getAlphaSensitivity(), 6.5786313367554754E7, REGRESSION_TOL * NOTIONAL);
    assertEquals(pointComputed.getBetaSensitivity(), -1.2044275797229866E7, REGRESSION_TOL * NOTIONAL);
    assertEquals(pointComputed.getRhoSensitivity(), 266223.51118849067, REGRESSION_TOL * NOTIONAL);
    assertEquals(pointComputed.getNuSensitivity(), 400285.55052713456, REGRESSION_TOL * NOTIONAL);
    SurfaceCurrencyParameterSensitivities sensiComputed =
        VOL_PROVIDER_REGRESSION.surfaceCurrencyParameterSensitivity(pointComputed);
    double[][] alphaExp = new double[][] { {10.0, 10.0, 0.0}, {5.0, 5.0, 0.0}, {1.0, 1.0, 0.0}, {2.0, 1.0, 0.0},
        {2.0, 10.0, 2.631285063205345E7}, {10.0, 1.0, 0.0}, {1.0, 10.0, 4136.961894403858}, {0.5, 5.0, 0.0},
        {0.0, 1.0, 0.0}, {0.0, 10.0, 0.0}, {0.5, 10.0, 0.0}, {0.5, 1.0, 0.0}, {10.0, 5.0, 0.0}, {5.0, 10.0, 0.0},
        {5.0, 1.0, 0.0}, {1.0, 5.0, 6204.475194599179}, {2.0, 5.0, 3.94631212984123E7}, {0.0, 5.0, 0.0}};
    double[][] betaExp = new double[][] { {10.0, 10.0, -0.0}, {5.0, 5.0, -0.0}, {1.0, 1.0, -0.0}, {2.0, 1.0, -0.0},
        {2.0, 10.0, -4817403.70908317}, {10.0, 1.0, -0.0}, {1.0, 10.0, -757.402375482629}, {0.5, 5.0, -0.0},
        {0.0, 1.0, -0.0}, {0.0, 10.0, -0.0}, {0.5, 10.0, -0.0}, {0.5, 1.0, -0.0}, {10.0, 5.0, -0.0},
        {5.0, 10.0, -0.0}, {5.0, 1.0, -0.0}, {1.0, 5.0, -1135.926404680998}, {2.0, 5.0, -7224978.759366533},
        {0.0, 5.0, -0.0}};
    double[][] rhoExp = new double[][] { {10.0, 10.0, 0.0}, {5.0, 5.0, 0.0}, {1.0, 1.0, 0.0}, {2.0, 1.0, 0.0},
        {2.0, 10.0, 106482.62725265314}, {10.0, 1.0, 0.0}, {1.0, 10.0, 16.741423326578513}, {0.5, 5.0, 0.0},
        {0.0, 1.0, 0.0}, {0.0, 10.0, 0.0}, {0.5, 10.0, 0.0}, {0.5, 1.0, 0.0}, {10.0, 5.0, 0.0}, {5.0, 10.0, 0.0},
        {5.0, 1.0, 0.0}, {1.0, 5.0, 25.10821912392996}, {2.0, 5.0, 159699.03429338703}, {0.0, 5.0, 0.0}};
    double[][] nuExp = new double[][] { {10.0, 10.0, 0.0}, {5.0, 5.0, 0.0}, {1.0, 1.0, 0.0}, {2.0, 1.0, 0.0},
        {2.0, 10.0, 160104.03018547}, {10.0, 1.0, 0.0}, {1.0, 10.0, 25.171893432592533}, {0.5, 5.0, 0.0},
        {0.0, 1.0, 0.0}, {0.0, 10.0, 0.0}, {0.5, 10.0, 0.0}, {0.5, 1.0, 0.0}, {10.0, 5.0, 0.0}, {5.0, 10.0, 0.0},
        {5.0, 1.0, 0.0}, {1.0, 5.0, 37.751952372314484}, {2.0, 5.0, 240118.59649585965}, {0.0, 5.0, 0.0}};
    double[][][] exps = new double[][][] {alphaExp, betaExp, rhoExp, nuExp};
    SurfaceMetadata[] metadata = new SurfaceMetadata[] {SwaptionSabrRateVolatilityDataSet.META_ALPHA,
        SwaptionSabrRateVolatilityDataSet.META_BETA_USD, SwaptionSabrRateVolatilityDataSet.META_RHO,
        SwaptionSabrRateVolatilityDataSet.META_NU};
    SurfaceCurrencyParameterSensitivities sensiExpected = SurfaceCurrencyParameterSensitivities.empty();
    for (int i = 0; i < exps.length; ++i) {
      int size = exps[i].length;
      List<SurfaceParameterMetadata> paramMetadata = new ArrayList<SurfaceParameterMetadata>(size);
      List<Double> sensi = new ArrayList<Double>(size);
      for (int j = 0; j < size; ++j) {
        paramMetadata.add(SwaptionSurfaceExpiryTenorNodeMetadata.of(exps[i][j][0], exps[i][j][1]));
        sensi.add(exps[i][j][2]);
      }
      SurfaceMetadata surfaceMetadata = metadata[i].withParameterMetadata(paramMetadata);
      sensiExpected = sensiExpected.combinedWith(
          SurfaceCurrencyParameterSensitivity.of(surfaceMetadata, USD, DoubleArray.copyOf(sensi)));
    }
    testSurfaceParameterSensitivities(sensiComputed, sensiExpected, REGRESSION_TOL * NOTIONAL);
  }

  //-------------------------------------------------------------------------
  private void testSurfaceParameterSensitivities(
      SurfaceCurrencyParameterSensitivities computed,
      SurfaceCurrencyParameterSensitivities expected,
      double tol) {
    List<SurfaceCurrencyParameterSensitivity> listComputed = new ArrayList<>(computed.getSensitivities());
    List<SurfaceCurrencyParameterSensitivity> listExpected = new ArrayList<>(expected.getSensitivities());
    for (SurfaceCurrencyParameterSensitivity sensExpected : listExpected) {
      int index = Math.abs(Collections.binarySearch(listComputed, sensExpected,
          SurfaceCurrencyParameterSensitivity::compareExcludingSensitivity));
      SurfaceCurrencyParameterSensitivity sensComputed = listComputed.get(index);
      int nSens = sensExpected.getParameterCount();
      assertEquals(sensComputed.getParameterCount(), nSens);
      for (int i = 0; i < nSens; ++i) {
        SwaptionSurfaceExpiryTenorNodeMetadata metaExpected =
            (SwaptionSurfaceExpiryTenorNodeMetadata) sensExpected.getMetadata().getParameterMetadata().get().get(i);
        boolean test = false;
        for (int j = 0; j < nSens; ++j) {
          SwaptionSurfaceExpiryTenorNodeMetadata metaComputed =
              (SwaptionSurfaceExpiryTenorNodeMetadata) sensComputed.getMetadata().getParameterMetadata().get().get(j);
          if (metaExpected.getYearFraction() == metaComputed.getYearFraction() &&
              metaExpected.getTenor() == metaComputed.getTenor()) {
            assertEquals(sensComputed.getSensitivity().toArray()[j], sensExpected.getSensitivity().toArray()[i], tol);
            test = true;
          }
        }
        assertTrue(test);
      }
      listComputed.remove(index);
    }
  }

}
