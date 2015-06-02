/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.value;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.analytics.math.interpolation.Interpolator1DFactory;
import com.opengamma.strata.basics.interpolator.CurveInterpolator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.sensitivity.IborRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Test {@link DiscountIborIndexRates}.
 */
@Test
public class DiscountIborIndexRatesTest {

  private static final LocalDate DATE_VAL = date(2015, 6, 4);
  private static final LocalDate DATE_BEFORE = date(2015, 6, 3);
  private static final LocalDate DATE_AFTER = date(2015, 7, 30);

  private static final CurveInterpolator INTERPOLATOR = Interpolator1DFactory.LINEAR_INSTANCE;
  private static final CurveName NAME = CurveName.of("TestCurve");
  private static final InterpolatedNodalCurve CURVE =
      InterpolatedNodalCurve.of(NAME, new double[] {0, 10}, new double[] {0.01, 0.02}, INTERPOLATOR);
  private static final ZeroRateDiscountFactors DFCURVE =
      ZeroRateDiscountFactors.of(GBP, DATE_VAL, ACT_365F, CURVE);
  private static final ZeroRateDiscountFactors DFCURVE2 =
      ZeroRateDiscountFactors.of(GBP, DATE_VAL, ACT_360, CURVE);

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
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getTimeSeries(), SERIES_EMPTY);
    assertEquals(test.getDiscountFactors(), DFCURVE);
    assertEquals(test.getCurveName(), NAME);
    assertEquals(test.getParameterCount(), 2);
  }

  public void test_of_withFixings() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, SERIES, DFCURVE);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getTimeSeries(), SERIES);
    assertEquals(test.getDiscountFactors(), DFCURVE);
    assertEquals(test.getCurveName(), NAME);
    assertEquals(test.getParameterCount(), 2);
  }

  //-------------------------------------------------------------------------
  public void test_withDiscountFactors() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, SERIES, DFCURVE);
    test = test.withDiscountFactors(DFCURVE2);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getTimeSeries(), SERIES);
    assertEquals(test.getDiscountFactors(), DFCURVE2);
    assertEquals(test.getCurveName(), NAME);
    assertEquals(test.getParameterCount(), 2);
  }

  //-------------------------------------------------------------------------
  public void test_rate_beforeValuation_fixing() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, SERIES, DFCURVE);
    assertEquals(test.rate(DATE_BEFORE), RATE_BEFORE);
  }

  public void test_rate_beforeValuation_noFixing_emptySeries() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, SERIES_EMPTY, DFCURVE);
    assertThrowsIllegalArg(() -> test.rate(DATE_BEFORE));
  }

  public void test_rate_beforeValuation_noFixing_notEmptySeries() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, SERIES_MINIMAL, DFCURVE);
    assertThrowsIllegalArg(() -> test.rate(DATE_BEFORE));
  }

  public void test_rate_onValuation_fixing() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, SERIES, DFCURVE);
    assertEquals(test.rate(DATE_VAL), RATE_VAL);
  }

  public void test_rate_onValuation_noFixing() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, SERIES_EMPTY, DFCURVE);
    LocalDate startDate = GBP_LIBOR_3M.calculateEffectiveFromFixing(DATE_VAL);
    LocalDate endDate = GBP_LIBOR_3M.calculateMaturityFromEffective(startDate);
    double accrualFactor = GBP_LIBOR_3M.getDayCount().yearFraction(startDate, endDate);
    double expected = (DFCURVE.discountFactor(startDate) / DFCURVE.discountFactor(endDate) - 1) / accrualFactor;
    assertEquals(test.rate(DATE_VAL), expected, 1e-8);
  }

  public void test_rate_afterValuation() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, SERIES, DFCURVE);
    LocalDate startDate = GBP_LIBOR_3M.calculateEffectiveFromFixing(DATE_AFTER);
    LocalDate endDate = GBP_LIBOR_3M.calculateMaturityFromEffective(startDate);
    double accrualFactor = GBP_LIBOR_3M.getDayCount().yearFraction(startDate, endDate);
    double expected = (DFCURVE.discountFactor(startDate) / DFCURVE.discountFactor(endDate) - 1) / accrualFactor;
    assertEquals(test.rate(DATE_AFTER), expected, 1e-8);
  }

  //-------------------------------------------------------------------------
  public void test_pointSensitivity_fixing() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, SERIES, DFCURVE);
    assertEquals(test.pointSensitivity(DATE_BEFORE), PointSensitivityBuilder.none());
    assertEquals(test.pointSensitivity(DATE_VAL), PointSensitivityBuilder.none());
  }

  public void test_pointSensitivity_onValuation_noFixing() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, SERIES_EMPTY, DFCURVE);
    IborRateSensitivity expected = IborRateSensitivity.of(GBP_LIBOR_3M, DATE_VAL, 1d);
    assertEquals(test.pointSensitivity(DATE_VAL), expected);
  }

  public void test_pointSensitivity_afterValuation() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, SERIES, DFCURVE);
    IborRateSensitivity expected = IborRateSensitivity.of(GBP_LIBOR_3M, DATE_AFTER, 1d);
    assertEquals(test.pointSensitivity(DATE_AFTER), expected);
  }

  //-------------------------------------------------------------------------
  public void test_parameterSensitivity() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, SERIES, DFCURVE);
    double relativeTime = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    double[] expected = CURVE.yValueParameterSensitivity(relativeTime);
    assertEquals(test.unitParameterSensitivity(DATE_AFTER), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, SERIES, DFCURVE);
    coverImmutableBean(test);
    DiscountIborIndexRates test2 = DiscountIborIndexRates.of(USD_LIBOR_3M, SERIES_EMPTY, DFCURVE2);
    coverBeanEquals(test, test2);
  }

}
