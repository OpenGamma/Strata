/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.value;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.FxIndices.EUR_GBP_ECB;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.interpolator.CurveInterpolator;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.key.FxIndexRatesKey;
import com.opengamma.strata.market.sensitivity.FxIndexSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Test {@link DiscountFxIndexRates}.
 */
@Test
public class DiscountFxIndexRatesTest {

  private static final LocalDate DATE_VAL = date(2015, 6, 4);
  private static final LocalDate DATE_BEFORE = date(2015, 6, 3);
  private static final LocalDate DATE_AFTER = date(2015, 7, 30);
  private static final CurrencyPair PAIR_GBP_USD = CurrencyPair.of(GBP, USD);
  private static final CurrencyPair PAIR_USD_GBP = CurrencyPair.of(USD, GBP);
  private static final CurrencyPair PAIR_EUR_GBP = CurrencyPair.of(EUR, GBP);
  private static final FxRate FX_RATE = FxRate.of(GBP, USD, 1.5d);
  private static final FxRate FX_RATE_EUR_GBP = FxRate.of(EUR, GBP, 0.7d);

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveMetadata METADATA1 = Curves.zeroRates("TestCurve", ACT_365F);
  private static final CurveMetadata METADATA2 = Curves.zeroRates("TestCurveUSD", ACT_365F);
  private static final InterpolatedNodalCurve CURVE1 =
      InterpolatedNodalCurve.of(METADATA1, DoubleArray.of(0, 10), DoubleArray.of(0.01, 0.02), INTERPOLATOR);
  private static final InterpolatedNodalCurve CURVE2 =
      InterpolatedNodalCurve.of(METADATA2, DoubleArray.of(0, 10), DoubleArray.of(0.015, 0.025), INTERPOLATOR);
  private static final ZeroRateDiscountFactors DFCURVE_GBP = ZeroRateDiscountFactors.of(GBP, DATE_VAL, CURVE1);
  private static final ZeroRateDiscountFactors DFCURVE_USD = ZeroRateDiscountFactors.of(USD, DATE_VAL, CURVE2);
  private static final ZeroRateDiscountFactors DFCURVE_EUR = ZeroRateDiscountFactors.of(EUR, DATE_VAL, CURVE2);

  private static final double RATE_BEFORE = 0.013d;
  private static final double RATE_VAL = 0.014d;
  private static final LocalDateDoubleTimeSeries SERIES = LocalDateDoubleTimeSeries.builder()
      .put(DATE_BEFORE, RATE_BEFORE)
      .put(DATE_VAL, RATE_VAL)
      .build();
  private static final LocalDateDoubleTimeSeries SERIES_MINIMAL = LocalDateDoubleTimeSeries.of(DATE_VAL, RATE_VAL);
  private static final LocalDateDoubleTimeSeries SERIES_EMPTY = LocalDateDoubleTimeSeries.empty();

  private static final FxForwardRates FWD_RATES = DiscountFxForwardRates.of(PAIR_GBP_USD, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
  private static final FxForwardRates FWD_RATES_USD_GBP =
      DiscountFxForwardRates.of(PAIR_USD_GBP, FX_RATE.inverse(), DFCURVE_USD, DFCURVE_GBP);
  private static final FxForwardRates FWD_RATES_EUR_GBP =
      DiscountFxForwardRates.of(PAIR_EUR_GBP, FX_RATE_EUR_GBP, DFCURVE_EUR, DFCURVE_GBP);

  //-------------------------------------------------------------------------
  public void test_of_withoutFixings() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(GBP_USD_WM, FWD_RATES);
    assertEquals(test.getKey(), FxIndexRatesKey.of(GBP_USD_WM));
    assertEquals(test.getIndex(), GBP_USD_WM);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getTimeSeries(), SERIES_EMPTY);
    assertEquals(test.getFxForwardRates(), FWD_RATES);
  }

  public void test_of_withFixings() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(GBP_USD_WM, SERIES, FWD_RATES);
    assertEquals(test.getKey(), FxIndexRatesKey.of(GBP_USD_WM));
    assertEquals(test.getIndex(), GBP_USD_WM);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getTimeSeries(), SERIES);
    assertEquals(test.getFxForwardRates(), FWD_RATES);
  }

  public void test_of_nonMatchingCurrency() {
    assertThrowsIllegalArg(() -> DiscountFxIndexRates.of(GBP_USD_WM, SERIES, FWD_RATES_USD_GBP));
    assertThrowsIllegalArg(() -> DiscountFxIndexRates.of(GBP_USD_WM, SERIES, FWD_RATES_EUR_GBP));
  }

  //-------------------------------------------------------------------------
  public void test_rate_beforeValuation_fixing() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(GBP_USD_WM, SERIES, FWD_RATES);
    assertEquals(test.rate(GBP, DATE_BEFORE), RATE_BEFORE);
    assertEquals(test.rate(USD, DATE_BEFORE), 1d / RATE_BEFORE);
  }

  public void test_rate_beforeValuation_noFixing_emptySeries() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(GBP_USD_WM, SERIES_EMPTY, FWD_RATES);
    assertThrowsIllegalArg(() -> test.rate(GBP, DATE_BEFORE));
  }

  public void test_rate_beforeValuation_noFixing_notEmptySeries() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(GBP_USD_WM, SERIES_MINIMAL, FWD_RATES);
    assertThrowsIllegalArg(() -> test.rate(GBP, DATE_BEFORE));
  }

  public void test_rate_onValuation_fixing() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(GBP_USD_WM, SERIES, FWD_RATES);
    assertEquals(test.rate(GBP, DATE_VAL), RATE_VAL);
    assertEquals(test.rate(USD, DATE_VAL), 1d / RATE_VAL);
  }

  public void test_rate_onValuation_noFixing() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(GBP_USD_WM, SERIES_EMPTY, FWD_RATES);
    LocalDate maturityDate = GBP_USD_WM.calculateMaturityFromFixing(DATE_VAL);
    double dfCcyBaseAtMaturity = DFCURVE_GBP.discountFactor(maturityDate);
    double dfCcyCounterAtMaturity = DFCURVE_USD.discountFactor(maturityDate);
    double expected = FX_RATE.fxRate(GBP, USD) * (dfCcyBaseAtMaturity / dfCcyCounterAtMaturity);
    assertEquals(test.rate(GBP, DATE_VAL), expected, 1e-8);
    assertEquals(test.rate(USD, DATE_VAL), 1d / expected, 1e-8);
  }

  public void test_rate_afterValuation() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(GBP_USD_WM, SERIES, FWD_RATES);
    LocalDate maturityDate = GBP_USD_WM.calculateMaturityFromFixing(DATE_AFTER);
    double dfCcyBaseAtMaturity = DFCURVE_GBP.discountFactor(maturityDate);
    double dfCcyCounterAtMaturity = DFCURVE_USD.discountFactor(maturityDate);
    double expected = FX_RATE.fxRate(GBP, USD) * (dfCcyBaseAtMaturity / dfCcyCounterAtMaturity);
    assertEquals(test.rate(GBP, DATE_AFTER), expected, 1e-8);
    assertEquals(test.rate(USD, DATE_AFTER), 1d / expected, 1e-8);
  }

  public void test_rate_nonMatchingCurrency() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(GBP_USD_WM, SERIES, FWD_RATES);
    assertThrowsIllegalArg(() -> test.rate(EUR, DATE_VAL));
  }

  //-------------------------------------------------------------------------
  public void test_ratePointSensitivity_fixing() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(GBP_USD_WM, SERIES, FWD_RATES);
    assertEquals(test.ratePointSensitivity(GBP, DATE_BEFORE), PointSensitivityBuilder.none());
    assertEquals(test.ratePointSensitivity(GBP, DATE_VAL), PointSensitivityBuilder.none());
  }

  public void test_ratePointSensitivity_onValuation_noFixing() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(GBP_USD_WM, SERIES_EMPTY, FWD_RATES);
    assertEquals(test.ratePointSensitivity(GBP, DATE_VAL), FxIndexSensitivity.of(GBP_USD_WM, GBP, DATE_VAL, 1d));
    assertEquals(test.ratePointSensitivity(USD, DATE_VAL), FxIndexSensitivity.of(GBP_USD_WM, USD, DATE_VAL, 1d));
  }

  public void test_ratePointSensitivity_afterValuation() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(GBP_USD_WM, SERIES, FWD_RATES);
    assertEquals(test.ratePointSensitivity(GBP, DATE_AFTER), FxIndexSensitivity.of(GBP_USD_WM, GBP, DATE_AFTER, 1d));
    assertEquals(test.ratePointSensitivity(USD, DATE_AFTER), FxIndexSensitivity.of(GBP_USD_WM, USD, DATE_AFTER, 1d));
  }

  public void test_ratePointSensitivity_nonMatchingCurrency() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(GBP_USD_WM, SERIES, FWD_RATES);
    assertThrowsIllegalArg(() -> test.ratePointSensitivity(EUR, DATE_VAL));
  }

  //-------------------------------------------------------------------------
  //proper end-to-end tests are elsewhere
  public void test_curveParameterSensitivity() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(GBP_USD_WM, SERIES, FWD_RATES);
    FxIndexSensitivity point = FxIndexSensitivity.of(GBP_USD_WM, GBP, DATE_VAL, 1d);
    assertEquals(test.curveParameterSensitivity(point).size(), 2);
    FxIndexSensitivity point2 = FxIndexSensitivity.of(GBP_USD_WM, USD, DATE_VAL, 1d);
    assertEquals(test.curveParameterSensitivity(point2).size(), 2);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(GBP_USD_WM, SERIES, FWD_RATES);
    coverImmutableBean(test);
    DiscountFxIndexRates test2 = DiscountFxIndexRates.of(EUR_GBP_ECB, SERIES_MINIMAL, FWD_RATES_EUR_GBP);
    coverBeanEquals(test, test2);
  }

}
