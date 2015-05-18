/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.FxIndices.WM_GBP_USD;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;
import com.opengamma.analytics.math.curve.InterpolatedDoublesCurve;
import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Test {@link DiscountFxIndexCurve}.
 */
@Test
public class DiscountFxIndexCurveTest {

  private static final LocalDate DATE_VAL = date(2015, 6, 4);
  private static final LocalDate DATE_BEFORE = date(2015, 6, 3);
  private static final LocalDate DATE_AFTER = date(2015, 7, 30);
  private static final FxRate FX_RATE = FxRate.of(GBP, USD, 1.5d);

  private static final CurveName NAME1 = CurveName.of("TestCurve");
  private static final CurveName NAME2 = CurveName.of("TestCurveUSD");
  private static final YieldCurve CURVE1 = YieldCurve.from(
      InterpolatedDoublesCurve.fromSorted(
          new double[] {0, 10},
          new double[] {0.01, 0.02},
          Interpolator1DFactory.LINEAR_INSTANCE,
          NAME1.toString()));
  private static final YieldCurve CURVE2 = YieldCurve.from(
      InterpolatedDoublesCurve.fromSorted(
          new double[] {0, 10},
          new double[] {0.015, 0.025},
          Interpolator1DFactory.LINEAR_INSTANCE,
          NAME2.toString()));
  private static final ZeroRateDiscountFactorCurve DFCURVE1 =
      ZeroRateDiscountFactorCurve.of(GBP, DATE_VAL, ACT_365F, CURVE1);
  private static final ZeroRateDiscountFactorCurve DFCURVE2 =
      ZeroRateDiscountFactorCurve.of(USD, DATE_VAL, ACT_360, CURVE2);

  private static final double RATE_BEFORE = 0.013d;
  private static final double RATE_VAL = 0.014d;
  private static final LocalDateDoubleTimeSeries SERIES = LocalDateDoubleTimeSeries.builder()
      .put(DATE_BEFORE, RATE_BEFORE)
      .put(DATE_VAL, RATE_VAL)
      .build();
  private static final LocalDateDoubleTimeSeries SERIES_EMPTY = LocalDateDoubleTimeSeries.empty();

  //-------------------------------------------------------------------------
  public void test_of() {
    DiscountFxIndexCurve test = DiscountFxIndexCurve.of(WM_GBP_USD, SERIES, FX_RATE, DFCURVE1, DFCURVE2);
    assertEquals(test.getIndex(), WM_GBP_USD);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getTimeSeries(), SERIES);
    assertEquals(test.getBaseCurrencyDiscountFactors(), DFCURVE1);
    assertEquals(test.getCounterCurrencyDiscountFactors(), DFCURVE2);
  }

  public void test_of_nonMatchingValuationDates() {
    DiscountFactorCurve curve2 = ZeroRateDiscountFactorCurve.of(USD, DATE_AFTER, ACT_360, CURVE2);
    assertThrowsIllegalArg(() -> DiscountFxIndexCurve.of(WM_GBP_USD, SERIES, FX_RATE, DFCURVE1, curve2));
  }

  //-------------------------------------------------------------------------
  public void test_rate_beforeValuation_fixing() {
    DiscountFxIndexCurve test = DiscountFxIndexCurve.of(WM_GBP_USD, SERIES, FX_RATE, DFCURVE1, DFCURVE2);
    assertEquals(test.rate(GBP, DATE_BEFORE), RATE_BEFORE);
    assertEquals(test.rate(USD, DATE_BEFORE), 1d / RATE_BEFORE);
  }

  public void test_rate_beforeValuation_noFixing() {
    DiscountFxIndexCurve test = DiscountFxIndexCurve.of(WM_GBP_USD, SERIES_EMPTY, FX_RATE, DFCURVE1, DFCURVE2);
    assertThrowsIllegalArg(() -> test.rate(GBP, DATE_BEFORE));
  }

  public void test_rate_onValuation_fixing() {
    DiscountFxIndexCurve test = DiscountFxIndexCurve.of(WM_GBP_USD, SERIES, FX_RATE, DFCURVE1, DFCURVE2);
    assertEquals(test.rate(GBP, DATE_VAL), RATE_VAL);
    assertEquals(test.rate(USD, DATE_VAL), 1d / RATE_VAL);
  }

  public void test_rate_onValuation_noFixing() {
    DiscountFxIndexCurve test = DiscountFxIndexCurve.of(WM_GBP_USD, SERIES_EMPTY, FX_RATE, DFCURVE1, DFCURVE2);
    LocalDate maturityDate = WM_GBP_USD.calculateMaturityFromFixing(DATE_VAL);
    double dfCcyBaseAtMaturity = DFCURVE1.discountFactor(maturityDate);
    double dfCcyCounterAtMaturity = DFCURVE2.discountFactor(maturityDate);
    double expected = FX_RATE.fxRate(GBP, USD) * (dfCcyBaseAtMaturity / dfCcyCounterAtMaturity);
    assertEquals(test.rate(GBP, DATE_VAL), expected, 1e-8);
    assertEquals(test.rate(USD, DATE_VAL), 1d / expected, 1e-8);
  }

  public void test_rate_afterValuation() {
    DiscountFxIndexCurve test = DiscountFxIndexCurve.of(WM_GBP_USD, SERIES, FX_RATE, DFCURVE1, DFCURVE2);
    LocalDate maturityDate = WM_GBP_USD.calculateMaturityFromFixing(DATE_AFTER);
    double dfCcyBaseAtMaturity = DFCURVE1.discountFactor(maturityDate);
    double dfCcyCounterAtMaturity = DFCURVE2.discountFactor(maturityDate);
    double expected = FX_RATE.fxRate(GBP, USD) * (dfCcyBaseAtMaturity / dfCcyCounterAtMaturity);
    assertEquals(test.rate(GBP, DATE_AFTER), expected, 1e-8);
    assertEquals(test.rate(USD, DATE_AFTER), 1d / expected, 1e-8);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DiscountFxIndexCurve test = DiscountFxIndexCurve.of(WM_GBP_USD, SERIES, FX_RATE, DFCURVE1, DFCURVE2);
    coverImmutableBean(test);
  }

}
