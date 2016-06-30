/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.ValueType;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.DefaultCurveMetadata;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Tests {@link SimpleIborIndexRates}.
 */
@Test
public class SimpleIborIndexRatesTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DATE_VAL = date(2015, 6, 4);
  private static final LocalDate DATE_BEFORE = date(2015, 6, 3);
  private static final LocalDate DATE_AFTER = date(2015, 7, 30);

  private static final IborIndexObservation GBP_LIBOR_3M_VAL = IborIndexObservation.of(GBP_LIBOR_3M, DATE_VAL, REF_DATA);
  private static final IborIndexObservation GBP_LIBOR_3M_BEFORE = IborIndexObservation.of(GBP_LIBOR_3M, DATE_BEFORE, REF_DATA);
  private static final IborIndexObservation GBP_LIBOR_3M_AFTER = IborIndexObservation.of(GBP_LIBOR_3M, DATE_AFTER, REF_DATA);

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final DayCount CURVE_DAY_COUNT = ACT_ACT_ISDA;
  private static final CurveName NAME = CurveName.of("TestCurve");
  private static final CurveMetadata METADATA = DefaultCurveMetadata.builder()
      .xValueType(ValueType.YEAR_FRACTION)
      .yValueType(ValueType.FORWARD_RATE)
      .curveName(NAME)
      .dayCount(CURVE_DAY_COUNT)
      .build();

  private static final InterpolatedNodalCurve CURVE =
      InterpolatedNodalCurve.of(METADATA, DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
  private static final InterpolatedNodalCurve CURVE2 =
      InterpolatedNodalCurve.of(METADATA, DoubleArray.of(0, 10), DoubleArray.of(2, 3), INTERPOLATOR);

  private static final double RATE_BEFORE = 0.013d;
  private static final double RATE_VAL = 0.014d;
  private static final LocalDateDoubleTimeSeries SERIES = LocalDateDoubleTimeSeries.builder()
      .put(DATE_BEFORE, RATE_BEFORE)
      .put(DATE_VAL, RATE_VAL)
      .build();
  private static final LocalDateDoubleTimeSeries SERIES_MINIMAL = LocalDateDoubleTimeSeries.of(DATE_VAL, RATE_VAL);
  private static final LocalDateDoubleTimeSeries SERIES_EMPTY = LocalDateDoubleTimeSeries.empty();

  private static final double TOLERANCE_RATE = 1.0E-8;

  //-------------------------------------------------------------------------
  public void test_of_withoutFixings() {
    SimpleIborIndexRates test = SimpleIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, CURVE);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getFixings(), SERIES_EMPTY);
    assertEquals(test.getCurve(), CURVE);
    assertEquals(test.getParameterCount(), CURVE.getParameterCount());
    assertEquals(test.getParameter(0), CURVE.getParameter(0));
    assertEquals(test.getParameterMetadata(0), CURVE.getParameterMetadata(0));
    assertEquals(test.withParameter(0, 1d).getCurve(), CURVE.withParameter(0, 1d));
    assertEquals(test.withPerturbation((i, v, m) -> v + 1d).getCurve(), CURVE.withPerturbation((i, v, m) -> v + 1d));
    assertEquals(test.findData(CURVE.getName()), Optional.of(CURVE));
    assertEquals(test.findData(CurveName.of("Rubbish")), Optional.empty());
    // check IborIndexRates
    IborIndexRates test2 = IborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, CURVE);
    assertEquals(test, test2);
  }

  public void test_of_withFixings() {
    SimpleIborIndexRates test = SimpleIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, CURVE, SERIES);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getFixings(), SERIES);
    assertEquals(test.getCurve(), CURVE);
  }

  public void test_of_badCurve() {
    CurveMetadata noDayCountMetadata = DefaultCurveMetadata.builder()
        .curveName(NAME)
        .xValueType(ValueType.YEAR_FRACTION)
        .yValueType(ValueType.FORWARD_RATE)
        .build();
    InterpolatedNodalCurve notDayCount = InterpolatedNodalCurve.of(
        noDayCountMetadata, DoubleArray.of(0, 10), DoubleArray.of(1, 2), INTERPOLATOR);
    assertThrowsIllegalArg(() -> SimpleIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, notDayCount));
  }

  //-------------------------------------------------------------------------
  public void test_withDiscountFactors() {
    SimpleIborIndexRates test = SimpleIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, CURVE, SERIES);
    test = test.withCurve(CURVE2);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getFixings(), SERIES);
    assertEquals(test.getCurve(), CURVE2);
  }

  //-------------------------------------------------------------------------
  public void test_rate_beforeValuation_fixing() {
    SimpleIborIndexRates test = SimpleIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, CURVE, SERIES);
    assertEquals(test.rate(GBP_LIBOR_3M_BEFORE), RATE_BEFORE);
  }

  public void test_rate_beforeValuation_noFixing_emptySeries() {
    SimpleIborIndexRates test = SimpleIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, CURVE, SERIES_EMPTY);
    assertThrowsIllegalArg(() -> test.rate(GBP_LIBOR_3M_BEFORE));
  }

  public void test_rate_beforeValuation_noFixing_notEmptySeries() {
    SimpleIborIndexRates test = SimpleIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, CURVE, SERIES_MINIMAL);
    assertThrowsIllegalArg(() -> test.rate(GBP_LIBOR_3M_BEFORE));
  }

  public void test_rate_onValuation_fixing() {
    SimpleIborIndexRates test = SimpleIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, CURVE, SERIES);
    assertEquals(test.rate(GBP_LIBOR_3M_VAL), RATE_VAL);
  }

  public void test_rateIgnoringFixings_onValuation_fixing() {
    SimpleIborIndexRates test = SimpleIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, CURVE, SERIES);
    double time = CURVE_DAY_COUNT.yearFraction(DATE_VAL, GBP_LIBOR_3M_VAL.getMaturityDate());
    double expected = CURVE.yValue(time);
    assertEquals(test.rateIgnoringFixings(GBP_LIBOR_3M_VAL), expected, TOLERANCE_RATE);
  }

  public void test_rate_onValuation_noFixing() {
    SimpleIborIndexRates test = SimpleIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, CURVE, SERIES_EMPTY);
    double time = CURVE_DAY_COUNT.yearFraction(DATE_VAL, GBP_LIBOR_3M_VAL.getMaturityDate());
    double expected = CURVE.yValue(time);
    assertEquals(test.rate(GBP_LIBOR_3M_VAL), expected, TOLERANCE_RATE);
    assertEquals(test.rateIgnoringFixings(GBP_LIBOR_3M_VAL), expected, TOLERANCE_RATE);
  }

  public void test_rate_afterValuation() {
    SimpleIborIndexRates test = SimpleIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, CURVE, SERIES);
    double time = CURVE_DAY_COUNT.yearFraction(DATE_VAL, GBP_LIBOR_3M_AFTER.getMaturityDate());
    double expected = CURVE.yValue(time);
    assertEquals(test.rate(GBP_LIBOR_3M_AFTER), expected, TOLERANCE_RATE);
  }

  //-------------------------------------------------------------------------
  public void test_ratePointSensitivity_fixing() {
    SimpleIborIndexRates test = SimpleIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, CURVE, SERIES);
    assertEquals(test.ratePointSensitivity(GBP_LIBOR_3M_BEFORE), PointSensitivityBuilder.none());
    assertEquals(test.ratePointSensitivity(GBP_LIBOR_3M_VAL), PointSensitivityBuilder.none());
  }
  
  public void test_rateIgnoringFixingsPointSensitivity_onValuation() {
    SimpleIborIndexRates test = SimpleIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, CURVE, SERIES);
    IborRateSensitivity expected = IborRateSensitivity.of(GBP_LIBOR_3M_VAL, 1d);
    assertEquals(test.rateIgnoringFixingsPointSensitivity(GBP_LIBOR_3M_VAL), expected);
  }

  public void test_ratePointSensitivity_onValuation_noFixing() {
    SimpleIborIndexRates test = SimpleIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, CURVE, SERIES_EMPTY);
    IborRateSensitivity expected = IborRateSensitivity.of(GBP_LIBOR_3M_VAL, 1d);
    assertEquals(test.ratePointSensitivity(GBP_LIBOR_3M_VAL), expected);
    assertEquals(test.rateIgnoringFixingsPointSensitivity(GBP_LIBOR_3M_VAL), expected);
  }

  public void test_ratePointSensitivity_afterValuation() {
    SimpleIborIndexRates test = SimpleIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, CURVE, SERIES);
    IborRateSensitivity expected = IborRateSensitivity.of(GBP_LIBOR_3M_AFTER, 1d);
    assertEquals(test.ratePointSensitivity(GBP_LIBOR_3M_AFTER), expected);
  }

  //-------------------------------------------------------------------------
  // proper end-to-end tests are elsewhere
  public void test_parameterSensitivity() {
    SimpleIborIndexRates test = SimpleIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, CURVE, SERIES);
    IborRateSensitivity point = IborRateSensitivity.of(GBP_LIBOR_3M_AFTER, GBP, 1d);
    assertEquals(test.parameterSensitivity(point).size(), 1);
  }

  //-------------------------------------------------------------------------
  public void test_createParameterSensitivity() {
    SimpleIborIndexRates test = SimpleIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, CURVE, SERIES);
    DoubleArray sensitivities = DoubleArray.of(0.12, 0.15);
    CurrencyParameterSensitivities sens = test.createParameterSensitivity(USD, sensitivities);
    assertEquals(sens.getSensitivities().get(0), CURVE.createParameterSensitivity(USD, sensitivities));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SimpleIborIndexRates test = SimpleIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, CURVE, SERIES);
    coverImmutableBean(test);
    SimpleIborIndexRates test2 = SimpleIborIndexRates.of(USD_LIBOR_3M, DATE_AFTER, CURVE2, SERIES_EMPTY);
    coverBeanEquals(test, test2);
  }
  
}
