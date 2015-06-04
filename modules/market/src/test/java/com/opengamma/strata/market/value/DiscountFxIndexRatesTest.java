/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.value;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.FxIndices.WM_GBP_USD;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.FxIndex;
import com.opengamma.strata.basics.index.ImmutableFxIndex;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
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
  private static final FxRate FX_RATE = FxRate.of(GBP, USD, 1.5d);
  private static final FxIndex USD_GBP = ImmutableFxIndex.builder()
      .name("TestUSDGBP")
      .currencyPair(CurrencyPair.of(USD, GBP))
      .fixingCalendar(WM_GBP_USD.getFixingCalendar())
      .maturityDateOffset(DaysAdjustment.ofCalendarDays(1))
      .build();

  private static final CurveInterpolator INTERPOLATOR = Interpolator1DFactory.LINEAR_INSTANCE;
  private static final CurveName NAME1 = CurveName.of("TestCurve");
  private static final CurveName NAME2 = CurveName.of("TestCurveUSD");
  private static final InterpolatedNodalCurve CURVE1 =
      InterpolatedNodalCurve.of(NAME1, new double[] {0, 10}, new double[] {0.01, 0.02}, INTERPOLATOR);
  private static final InterpolatedNodalCurve CURVE2 =
      InterpolatedNodalCurve.of(NAME2, new double[] {0, 10}, new double[] {0.015, 0.025}, INTERPOLATOR);
  private static final ZeroRateDiscountFactors DFCURVE_GBP =
      ZeroRateDiscountFactors.of(GBP, DATE_VAL, ACT_365F, CURVE1);
  private static final ZeroRateDiscountFactors DFCURVE_GBP2 =
      ZeroRateDiscountFactors.of(GBP, DATE_VAL, ACT_365F, CURVE2);
  private static final ZeroRateDiscountFactors DFCURVE_USD =
      ZeroRateDiscountFactors.of(USD, DATE_VAL, ACT_360, CURVE2);
  private static final ZeroRateDiscountFactors DFCURVE_USD2 =
      ZeroRateDiscountFactors.of(USD, DATE_VAL, ACT_360, CURVE1);

  private static final double RATE_BEFORE = 0.013d;
  private static final double RATE_VAL = 0.014d;
  private static final LocalDateDoubleTimeSeries SERIES = LocalDateDoubleTimeSeries.builder()
      .put(DATE_BEFORE, RATE_BEFORE)
      .put(DATE_VAL, RATE_VAL)
      .build();
  private static final LocalDateDoubleTimeSeries SERIES_MINIMAL = LocalDateDoubleTimeSeries.of(DATE_VAL, RATE_VAL);
  private static final LocalDateDoubleTimeSeries SERIES_EMPTY = LocalDateDoubleTimeSeries.empty();

  //-------------------------------------------------------------------------
  public void test_of_withoutFixings() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(WM_GBP_USD, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertEquals(test.getIndex(), WM_GBP_USD);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getTimeSeries(), SERIES_EMPTY);
    assertEquals(test.getBaseCurrencyDiscountFactors(), DFCURVE_GBP);
    assertEquals(test.getCounterCurrencyDiscountFactors(), DFCURVE_USD);
  }

  public void test_of_withFixings() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(WM_GBP_USD, SERIES, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertEquals(test.getIndex(), WM_GBP_USD);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getTimeSeries(), SERIES);
    assertEquals(test.getBaseCurrencyDiscountFactors(), DFCURVE_GBP);
    assertEquals(test.getCounterCurrencyDiscountFactors(), DFCURVE_USD);
  }

  public void test_of_nonMatchingCurrency() {
    assertThrowsIllegalArg(() -> DiscountFxIndexRates.of(WM_GBP_USD, SERIES, FX_RATE, DFCURVE_GBP, DFCURVE_GBP));
    assertThrowsIllegalArg(() -> DiscountFxIndexRates.of(WM_GBP_USD, SERIES, FX_RATE, DFCURVE_USD, DFCURVE_USD));
  }

  public void test_of_nonMatchingValuationDates() {
    DiscountFactors curve2 = ZeroRateDiscountFactors.of(USD, DATE_AFTER, ACT_360, CURVE2);
    assertThrowsIllegalArg(() -> DiscountFxIndexRates.of(WM_GBP_USD, SERIES, FX_RATE, DFCURVE_GBP, curve2));
  }

  //-------------------------------------------------------------------------
  public void test_withDiscountFactors() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(WM_GBP_USD, SERIES, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    test = test.withDiscountFactors(DFCURVE_GBP2, DFCURVE_USD2);
    assertEquals(test.getIndex(), WM_GBP_USD);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getTimeSeries(), SERIES);
    assertEquals(test.getBaseCurrencyDiscountFactors(), DFCURVE_GBP2);
    assertEquals(test.getCounterCurrencyDiscountFactors(), DFCURVE_USD2);
  }

  //-------------------------------------------------------------------------
  public void test_rate_beforeValuation_fixing() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(WM_GBP_USD, SERIES, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertEquals(test.rate(GBP, DATE_BEFORE), RATE_BEFORE);
    assertEquals(test.rate(USD, DATE_BEFORE), 1d / RATE_BEFORE);
  }

  public void test_rate_beforeValuation_noFixing_emptySeries() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(WM_GBP_USD, SERIES_EMPTY, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertThrowsIllegalArg(() -> test.rate(GBP, DATE_BEFORE));
  }

  public void test_rate_beforeValuation_noFixing_notEmptySeries() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(WM_GBP_USD, SERIES_MINIMAL, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertThrowsIllegalArg(() -> test.rate(GBP, DATE_BEFORE));
  }

  public void test_rate_onValuation_fixing() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(WM_GBP_USD, SERIES, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertEquals(test.rate(GBP, DATE_VAL), RATE_VAL);
    assertEquals(test.rate(USD, DATE_VAL), 1d / RATE_VAL);
  }

  public void test_rate_onValuation_noFixing() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(WM_GBP_USD, SERIES_EMPTY, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    LocalDate maturityDate = WM_GBP_USD.calculateMaturityFromFixing(DATE_VAL);
    double dfCcyBaseAtMaturity = DFCURVE_GBP.discountFactor(maturityDate);
    double dfCcyCounterAtMaturity = DFCURVE_USD.discountFactor(maturityDate);
    double expected = FX_RATE.fxRate(GBP, USD) * (dfCcyBaseAtMaturity / dfCcyCounterAtMaturity);
    assertEquals(test.rate(GBP, DATE_VAL), expected, 1e-8);
    assertEquals(test.rate(USD, DATE_VAL), 1d / expected, 1e-8);
  }

  public void test_rate_afterValuation() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(WM_GBP_USD, SERIES, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    LocalDate maturityDate = WM_GBP_USD.calculateMaturityFromFixing(DATE_AFTER);
    double dfCcyBaseAtMaturity = DFCURVE_GBP.discountFactor(maturityDate);
    double dfCcyCounterAtMaturity = DFCURVE_USD.discountFactor(maturityDate);
    double expected = FX_RATE.fxRate(GBP, USD) * (dfCcyBaseAtMaturity / dfCcyCounterAtMaturity);
    assertEquals(test.rate(GBP, DATE_AFTER), expected, 1e-8);
    assertEquals(test.rate(USD, DATE_AFTER), 1d / expected, 1e-8);
  }

  public void test_rate_nonMatchingCurrency() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(WM_GBP_USD, SERIES, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertThrowsIllegalArg(() -> test.rate(EUR, DATE_VAL));
  }

  //-------------------------------------------------------------------------
  public void test_ratePointSensitivity_fixing() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(WM_GBP_USD, SERIES, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertEquals(test.ratePointSensitivity(GBP, DATE_BEFORE), PointSensitivityBuilder.none());
    assertEquals(test.ratePointSensitivity(GBP, DATE_VAL), PointSensitivityBuilder.none());
  }

  public void test_ratePointSensitivity_onValuation_noFixing() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(WM_GBP_USD, SERIES_EMPTY, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertEquals(test.ratePointSensitivity(GBP, DATE_VAL), FxIndexSensitivity.of(WM_GBP_USD, GBP, DATE_VAL, 1d));
    assertEquals(test.ratePointSensitivity(USD, DATE_VAL), FxIndexSensitivity.of(WM_GBP_USD, USD, DATE_VAL, 1d));
  }

  public void test_ratePointSensitivity_afterValuation() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(WM_GBP_USD, SERIES, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertEquals(test.ratePointSensitivity(GBP, DATE_AFTER), FxIndexSensitivity.of(WM_GBP_USD, GBP, DATE_AFTER, 1d));
    assertEquals(test.ratePointSensitivity(USD, DATE_AFTER), FxIndexSensitivity.of(WM_GBP_USD, USD, DATE_AFTER, 1d));
  }

  public void test_ratePointSensitivity_nonMatchingCurrency() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(WM_GBP_USD, SERIES, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    assertThrowsIllegalArg(() -> test.ratePointSensitivity(EUR, DATE_VAL));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DiscountFxIndexRates test = DiscountFxIndexRates.of(WM_GBP_USD, SERIES, FX_RATE, DFCURVE_GBP, DFCURVE_USD);
    coverImmutableBean(test);
    DiscountFxIndexRates test2 =
        DiscountFxIndexRates.of(WM_GBP_USD, SERIES_EMPTY, FX_RATE.inverse(), DFCURVE_GBP2, DFCURVE_USD2);
    coverBeanEquals(test, test2);
    DiscountFxIndexRates test3 = DiscountFxIndexRates.of(USD_GBP, SERIES_EMPTY, FX_RATE.inverse(), DFCURVE_USD, DFCURVE_GBP);
    coverBeanEquals(test, test3);
  }

}
