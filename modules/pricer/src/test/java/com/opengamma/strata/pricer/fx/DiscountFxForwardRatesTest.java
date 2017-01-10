/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.MultiCurrencyAmount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.basics.index.FxIndices;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.DiscountFactors;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;
import com.opengamma.strata.pricer.impl.swap.DiscountingRatePaymentPeriodPricer;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.swap.FxReset;
import com.opengamma.strata.product.swap.RateAccrualPeriod;
import com.opengamma.strata.product.swap.RatePaymentPeriod;

/**
 * Test {@link DiscountFxForwardRates}.
 */
@Test
public class DiscountFxForwardRatesTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DATE_VAL = date(2015, 6, 4);
  private static final LocalDate DATE_REF = date(2015, 7, 30);
  private static final FxRate FX_RATE = FxRate.of(GBP, USD, 1.5d);
  private static final CurrencyPair CURRENCY_PAIR = CurrencyPair.of(GBP, USD);

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveMetadata METADATA1 = Curves.zeroRates("TestCurve", ACT_365F);
  private static final CurveMetadata METADATA2 = Curves.zeroRates("TestCurveUSD", ACT_365F);
  private static final InterpolatedNodalCurve CURVE1 =
      InterpolatedNodalCurve.of(METADATA1, DoubleArray.of(0, 10), DoubleArray.of(0.01, 0.02), INTERPOLATOR);
  private static final InterpolatedNodalCurve CURVE2 =
      InterpolatedNodalCurve.of(METADATA2, DoubleArray.of(0, 10), DoubleArray.of(0.015, 0.025), INTERPOLATOR);
  private static final ZeroRateDiscountFactors DFCURVE_GBP = ZeroRateDiscountFactors.of(GBP, DATE_VAL, CURVE1);
  private static final ZeroRateDiscountFactors DFCURVE_GBP2 = ZeroRateDiscountFactors.of(GBP, DATE_VAL, CURVE2);
  private static final ZeroRateDiscountFactors DFCURVE_USD = ZeroRateDiscountFactors.of(USD, DATE_VAL, CURVE2);
  private static final ZeroRateDiscountFactors DFCURVE_USD2 = ZeroRateDiscountFactors.of(USD, DATE_VAL, CURVE1);
  private static final RatesProvider PROVIDER = ImmutableRatesProvider.builder(DATE_VAL)
      .discountCurve(GBP, CURVE1).discountCurve(USD, CURVE2)
      .fxRateProvider(FX_RATE).build();
  private static final DiscountingRatePaymentPeriodPricer PERIOD_PRICER = DiscountingRatePaymentPeriodPricer.DEFAULT;
  private static final double TOLERANCE = 1.0E-6;
  
  //-------------------------------------------------------------------------
  public void test_of() {
    DiscountFxForwardRates test = DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getBaseCurrencyDiscountFactors(), DFCURVE_GBP);
    assertEquals(test.getCounterCurrencyDiscountFactors(), DFCURVE_USD);
    assertEquals(test.getFxRateProvider(), FX_RATE);
    assertEquals(test.findData(CURVE1.getName()), Optional.of(CURVE1));
    assertEquals(test.findData(CURVE2.getName()), Optional.of(CURVE2));
    assertEquals(test.findData(CurveName.of("Rubbish")), Optional.empty());

    int baseSize = DFCURVE_USD.getParameterCount();
    assertEquals(test.getParameterCount(), DFCURVE_GBP.getParameterCount() + baseSize);
    assertEquals(test.getParameter(0), DFCURVE_GBP.getParameter(0));
    assertEquals(test.getParameter(baseSize), DFCURVE_USD.getParameter(0));
    assertEquals(test.getParameterMetadata(0), DFCURVE_GBP.getParameterMetadata(0));
    assertEquals(test.getParameterMetadata(baseSize), DFCURVE_USD.getParameterMetadata(0));
    assertEquals(test.withParameter(0, 1d).getBaseCurrencyDiscountFactors(), DFCURVE_GBP.withParameter(0, 1d));
    assertEquals(test.withParameter(0, 1d).getCounterCurrencyDiscountFactors(), DFCURVE_USD);
    assertEquals(test.withParameter(baseSize, 1d).getBaseCurrencyDiscountFactors(), DFCURVE_GBP);
    assertEquals(test.withParameter(baseSize, 1d).getCounterCurrencyDiscountFactors(), DFCURVE_USD.withParameter(0, 1d));
    assertEquals(
        test.withPerturbation((i, v, m) -> v + 1d).getBaseCurrencyDiscountFactors(),
        DFCURVE_GBP.withPerturbation((i, v, m) -> v + 1d));
    assertEquals(
        test.withPerturbation((i, v, m) -> v + 1d).getCounterCurrencyDiscountFactors(),
        DFCURVE_USD.withPerturbation((i, v, m) -> v + 1d));
  }

  public void test_of_nonMatchingCurrency() {
    assertThrowsIllegalArg(() -> DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_GBP));
    assertThrowsIllegalArg(() -> DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_USD, DFCURVE_USD));
  }

  public void test_of_nonMatchingValuationDates() {
    DiscountFactors curve2 = ZeroRateDiscountFactors.of(USD, DATE_REF, CURVE2);
    assertThrowsIllegalArg(() -> DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, curve2));
  }

  public void test_builder() {
    assertThrowsIllegalArg(() -> DiscountFxForwardRates.meta().builder()
        .setString(DiscountFxForwardRates.meta().currencyPair(), "GBP/USD").build());
    assertThrowsIllegalArg(() -> DiscountFxForwardRates.meta().builder()
        .setString(DiscountFxForwardRates.meta().currencyPair().name(), "GBP/USD").build());
  }

  //-------------------------------------------------------------------------
  public void test_withDiscountFactors() {
    DiscountFxForwardRates test = DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    test = test.withDiscountFactors(DFCURVE_GBP2, DFCURVE_USD2);
    assertEquals(test.getCurrencyPair(), CURRENCY_PAIR);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getBaseCurrencyDiscountFactors(), DFCURVE_GBP2);
    assertEquals(test.getCounterCurrencyDiscountFactors(), DFCURVE_USD2);
    assertEquals(test.getFxRateProvider(), FX_RATE);
  }

  //-------------------------------------------------------------------------
  public void test_rate() {
    DiscountFxForwardRates test = DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    double dfCcyBaseAtMaturity = DFCURVE_GBP.discountFactor(DATE_REF);
    double dfCcyCounterAtMaturity = DFCURVE_USD.discountFactor(DATE_REF);
    double expected = FX_RATE.fxRate(GBP, USD) * (dfCcyBaseAtMaturity / dfCcyCounterAtMaturity);
    assertEquals(test.rate(GBP, DATE_REF), expected, 1e-12);
    assertEquals(test.rate(USD, DATE_REF), 1d / expected, 1e-12);
  }

  public void test_rate_nonMatchingCurrency() {
    DiscountFxForwardRates test = DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertThrowsIllegalArg(() -> test.rate(EUR, DATE_VAL));
  }

  //-------------------------------------------------------------------------
  public void test_ratePointSensitivity() {
    DiscountFxForwardRates test = DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertEquals(test.ratePointSensitivity(GBP, DATE_REF),
        FxForwardSensitivity.of(CURRENCY_PAIR, GBP, DATE_REF, 1d));
    assertEquals(test.ratePointSensitivity(USD, DATE_REF),
        FxForwardSensitivity.of(CURRENCY_PAIR, USD, DATE_REF, 1d));
  }

  public void test_ratePointSensitivity_nonMatchingCurrency() {
    DiscountFxForwardRates test = DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertThrowsIllegalArg(() -> test.ratePointSensitivity(EUR, DATE_VAL));
  }

  //-------------------------------------------------------------------------
  public void test_rateFxSpotSensitivity() {
    DiscountFxForwardRates test = DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    double dfCcyBaseAtMaturity = DFCURVE_GBP.discountFactor(DATE_REF);
    double dfCcyCounterAtMaturity = DFCURVE_USD.discountFactor(DATE_REF);
    double expected = dfCcyBaseAtMaturity / dfCcyCounterAtMaturity;
    assertEquals(test.rateFxSpotSensitivity(GBP, DATE_REF), expected, 1e-12);
    assertEquals(test.rateFxSpotSensitivity(USD, DATE_REF), 1d / expected, 1e-12);
  }

  public void test_rateFxSpotSensitivity_nonMatchingCurrency() {
    DiscountFxForwardRates test = DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertThrowsIllegalArg(() -> test.rateFxSpotSensitivity(EUR, DATE_VAL));
  }

  //-------------------------------------------------------------------------
  //proper end-to-end tests are elsewhere
  public void test_parameterSensitivity() {
    DiscountFxForwardRates test = DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    FxForwardSensitivity point = FxForwardSensitivity.of(CURRENCY_PAIR, GBP, DATE_VAL, 1d);
    assertEquals(test.parameterSensitivity(point).size(), 2);
    FxForwardSensitivity point2 = FxForwardSensitivity.of(CURRENCY_PAIR, USD, DATE_VAL, 1d);
    assertEquals(test.parameterSensitivity(point2).size(), 2);
  }

  //-------------------------------------------------------------------------
  public void currency_exposure_GBP() {
    LocalDate startDate = LocalDate.of(2016, 8, 2);
    LocalDate fixingDate = LocalDate.of(2016, 11, 2);
    LocalDate endDate = LocalDate.of(2016, 11, 4);
    double yearFraction = 0.25;
    double rate = 0.10;
    RateAccrualPeriod accrual = RateAccrualPeriod.builder().startDate(startDate)
        .endDate(endDate).yearFraction(yearFraction).rateComputation(FixedRateComputation.of(rate)).build();
    double notional = 1000000;
    RatePaymentPeriod fixedFx = RatePaymentPeriod.builder()
        .accrualPeriods(accrual)
        .fxReset(FxReset.of(FxIndexObservation.of(FxIndices.GBP_USD_WM, fixingDate, REF_DATA), GBP))
        .notional(notional)
        .paymentDate(endDate)
        .dayCount(DayCounts.ONE_ONE)
        .currency(USD).build(); // 1_000_000 GBP paid in USD at maturity
    PointSensitivityBuilder pts = PERIOD_PRICER.presentValueSensitivity(fixedFx, PROVIDER);
    MultiCurrencyAmount ceComputed = PERIOD_PRICER.currencyExposure(fixedFx, PROVIDER);
    double dfGbp = PROVIDER.discountFactor(GBP, endDate);
    double ceGbpExpected = notional * yearFraction * rate * dfGbp;
    assertEquals(ceComputed.getAmount(GBP).getAmount(), ceGbpExpected, 1.0E-6);
    MultiCurrencyAmount ceWithoutPvComputed = PROVIDER.currencyExposure(pts.build().convertedTo(GBP, PROVIDER));
    CurrencyAmount pvComputed = CurrencyAmount.of(USD, PERIOD_PRICER.presentValue(fixedFx, PROVIDER));
    MultiCurrencyAmount ceComputed2 = ceWithoutPvComputed.plus(pvComputed);
    assertEquals(ceComputed2.getAmount(GBP).getAmount(), ceGbpExpected, TOLERANCE);
    assertEquals(ceComputed2.getAmount(USD).getAmount(), 0.0, TOLERANCE);
  }
  
  public void currency_exposure_USD() {
    LocalDate startDate = LocalDate.of(2016, 8, 2);
    LocalDate fixingDate = LocalDate.of(2016, 11, 2);
    LocalDate endDate = LocalDate.of(2016, 11, 4);
    double yearFraction = 0.25;
    double rate = 0.10;
    RateAccrualPeriod accrual = RateAccrualPeriod.builder().startDate(startDate)
        .endDate(endDate).yearFraction(yearFraction).rateComputation(FixedRateComputation.of(rate)).build();
    double notional = 1000000;
    RatePaymentPeriod fixedFx = RatePaymentPeriod.builder()
        .accrualPeriods(accrual)
        .fxReset(FxReset.of(FxIndexObservation.of(FxIndices.GBP_USD_WM, fixingDate, REF_DATA), USD))
        .notional(notional)
        .paymentDate(endDate)
        .dayCount(DayCounts.ONE_ONE)
        .currency(GBP).build(); // 1_000_000 USD paid in GBP at maturity
    PointSensitivityBuilder pts = PERIOD_PRICER.presentValueSensitivity(fixedFx, PROVIDER);
    MultiCurrencyAmount ceComputed = PERIOD_PRICER.currencyExposure(fixedFx, PROVIDER);
    double dfUsd = PROVIDER.discountFactor(USD, endDate);
    double ceUsdExpected = notional * yearFraction * rate * dfUsd;
    assertEquals(ceComputed.getAmount(USD).getAmount(), ceUsdExpected, 1.0E-6);
    MultiCurrencyAmount ceWithoutPvComputed = PROVIDER.currencyExposure(pts.build().convertedTo(USD, PROVIDER));
    CurrencyAmount pvComputed = CurrencyAmount.of(GBP, PERIOD_PRICER.presentValue(fixedFx, PROVIDER));
    MultiCurrencyAmount ceComputed2 = ceWithoutPvComputed.plus(pvComputed);
    assertEquals(ceComputed2.getAmount(USD).getAmount(), ceUsdExpected, TOLERANCE);
    assertEquals(ceComputed2.getAmount(GBP).getAmount(), 0.0, TOLERANCE);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DiscountFxForwardRates test1 = DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    coverImmutableBean(test1);
    DiscountFxForwardRates test2 =
        DiscountFxForwardRates.of(CURRENCY_PAIR, FX_RATE.inverse(), DFCURVE_GBP2, DFCURVE_USD2);
    coverBeanEquals(test1, test2);
    DiscountFxForwardRates test3 = DiscountFxForwardRates.of(CurrencyPair.of(USD, EUR), FxRate.of(EUR, USD, 1.2d),
        DFCURVE_USD, ZeroRateDiscountFactors.of(EUR, DATE_VAL, CURVE2));
    coverBeanEquals(test1, test3);
  }

}
