/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.value;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.Perturbation;
import com.opengamma.strata.market.curve.Curve;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.CurveUnitParameterSensitivities;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.interpolator.CurveInterpolator;
import com.opengamma.strata.market.interpolator.CurveInterpolators;
import com.opengamma.strata.market.key.OvernightIndexRatesKey;
import com.opengamma.strata.market.sensitivity.OvernightRateSensitivity;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Test {@link DiscountOvernightIndexRates}.
 */
@Test
public class DiscountOvernightIndexRatesTest {

  private static final LocalDate DATE_VAL = date(2015, 6, 3);
  private static final LocalDate DATE_BEFORE = date(2015, 6, 2);
  private static final LocalDate DATE_AFTER = date(2015, 7, 30);
  private static final LocalDate DATE_AFTER_END = date(2015, 7, 31);

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveName NAME = CurveName.of("TestCurve");
  private static final CurveMetadata METADATA = Curves.zeroRates(NAME, ACT_365F);
  private static final InterpolatedNodalCurve CURVE =
      InterpolatedNodalCurve.of(METADATA, DoubleArray.of(0, 10), DoubleArray.of(0.01, 0.02), INTERPOLATOR);
  private static final InterpolatedNodalCurve CURVE2 =
      InterpolatedNodalCurve.of(METADATA, DoubleArray.of(0, 10), DoubleArray.of(0.01, 0.03), INTERPOLATOR);
  private static final ZeroRateDiscountFactors DFCURVE = ZeroRateDiscountFactors.of(GBP, DATE_VAL, CURVE);
  private static final ZeroRateDiscountFactors DFCURVE2 = ZeroRateDiscountFactors.of(GBP, DATE_VAL, CURVE2);

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
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE);
    assertEquals(test.getKey(), OvernightIndexRatesKey.of(GBP_SONIA));
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getTimeSeries(), SERIES_EMPTY);
    assertEquals(test.getDiscountFactors(), DFCURVE);
    assertEquals(test.getCurveName(), NAME);
    assertEquals(test.getParameterCount(), 2);
  }

  public void test_of_withFixings() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES, DFCURVE);
    assertEquals(test.getKey(), OvernightIndexRatesKey.of(GBP_SONIA));
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getTimeSeries(), SERIES);
    assertEquals(test.getDiscountFactors(), DFCURVE);
    assertEquals(test.getCurveName(), NAME);
    assertEquals(test.getParameterCount(), 2);
  }

  //-------------------------------------------------------------------------
  public void test_applyPerturbation() {
    Perturbation<Curve> perturbation = curve -> CURVE2;
    DiscountOvernightIndexRates base = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES, DFCURVE);
    DiscountOvernightIndexRates test = base.applyPerturbation(perturbation);
    test = test.withDiscountFactors(DFCURVE2);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getTimeSeries(), SERIES);
    assertEquals(test.getDiscountFactors(), DFCURVE2);
    assertEquals(test.getCurveName(), NAME);
    assertEquals(test.getParameterCount(), 2);
  }

  public void test_withDiscountFactors() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES, DFCURVE);
    test = test.withDiscountFactors(DFCURVE2);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getTimeSeries(), SERIES);
    assertEquals(test.getDiscountFactors(), DFCURVE2);
    assertEquals(test.getCurveName(), NAME);
    assertEquals(test.getParameterCount(), 2);
  }

  //-------------------------------------------------------------------------
  public void test_rate_beforeValuation_fixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES, DFCURVE);
    assertEquals(test.rate(DATE_BEFORE), RATE_BEFORE);
  }

  public void test_rate_beforeValuation_noFixing_emptySeries() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES_EMPTY, DFCURVE);
    assertThrowsIllegalArg(() -> test.rate(DATE_BEFORE));
  }

  public void test_rate_beforeValuation_noFixing_notEmptySeries() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES_MINIMAL, DFCURVE);
    assertThrowsIllegalArg(() -> test.rate(DATE_BEFORE));
  }

  public void test_rate_onValuation_fixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES, DFCURVE);
    assertEquals(test.rate(DATE_VAL), RATE_VAL);
  }

  public void test_rate_onPublication_noFixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES_EMPTY, DFCURVE);
    LocalDate startDate = GBP_SONIA.calculateEffectiveFromFixing(DATE_VAL);
    LocalDate endDate = GBP_SONIA.calculateMaturityFromEffective(startDate);
    double accrualFactor = GBP_SONIA.getDayCount().yearFraction(startDate, endDate);
    double expected = (DFCURVE.discountFactor(startDate) / DFCURVE.discountFactor(endDate) - 1) / accrualFactor;
    assertEquals(test.rate(DATE_VAL), expected, 1e-4);
  }

  public void test_rate_afterPublication() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES, DFCURVE);
    LocalDate startDate = GBP_SONIA.calculateEffectiveFromFixing(DATE_AFTER);
    LocalDate endDate = GBP_SONIA.calculateMaturityFromEffective(startDate);
    double accrualFactor = GBP_SONIA.getDayCount().yearFraction(startDate, endDate);
    double expected = (DFCURVE.discountFactor(startDate) / DFCURVE.discountFactor(endDate) - 1) / accrualFactor;
    assertEquals(test.rate(DATE_AFTER), expected, 1e-8);
  }

  //-------------------------------------------------------------------------
  public void test_ratePointSensitivity_fixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES, DFCURVE);
    assertEquals(test.ratePointSensitivity(DATE_BEFORE), PointSensitivityBuilder.none());
    assertEquals(test.ratePointSensitivity(DATE_VAL), PointSensitivityBuilder.none());
  }

  public void test_ratePointSensitivity_onPublication_noFixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES_EMPTY, DFCURVE);
    OvernightRateSensitivity expected = OvernightRateSensitivity.of(GBP_SONIA, DATE_VAL, 1d);
    assertEquals(test.ratePointSensitivity(DATE_VAL), expected);
  }

  public void test_ratePointSensitivity_afterPublication() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES, DFCURVE);
    OvernightRateSensitivity expected = OvernightRateSensitivity.of(GBP_SONIA, DATE_AFTER, 1d);
    assertEquals(test.ratePointSensitivity(DATE_AFTER), expected);
  }

  //-------------------------------------------------------------------------
  public void test_periodRate() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES, DFCURVE);
    double accrualFactor = GBP_SONIA.getDayCount().yearFraction(DATE_AFTER, DATE_AFTER_END);
    double expected = (DFCURVE.discountFactor(DATE_AFTER) / DFCURVE.discountFactor(DATE_AFTER_END) - 1) / accrualFactor;
    assertEquals(test.periodRate(DATE_AFTER, DATE_AFTER_END), expected, 1e-8);
  }

  public void test_periodRate_badDates() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES, DFCURVE);
    assertThrowsIllegalArg(() -> test.periodRate(DATE_BEFORE, DATE_VAL));
    assertThrowsIllegalArg(() -> test.periodRate(DATE_AFTER_END, DATE_AFTER));
  }

  //-------------------------------------------------------------------------
  public void test_periodRatePointSensitivity() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES, DFCURVE);
    OvernightRateSensitivity expected = OvernightRateSensitivity.of(GBP_SONIA, DATE_AFTER, DATE_AFTER_END, GBP, 1d);
    assertEquals(test.periodRatePointSensitivity(DATE_AFTER, DATE_AFTER_END), expected);
  }

  public void test_periodRatePointSensitivity_badDates() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES, DFCURVE);
    assertThrowsIllegalArg(() -> test.periodRatePointSensitivity(DATE_BEFORE, DATE_VAL));
    assertThrowsIllegalArg(() -> test.periodRatePointSensitivity(DATE_AFTER_END, DATE_AFTER));
  }

  //-------------------------------------------------------------------------
  public void test_unitParameterSensitivity_beforePublication_fixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES, DFCURVE);
    assertEquals(test.unitParameterSensitivity(DATE_BEFORE), CurveUnitParameterSensitivities.empty());
  }

  public void test_unitParameterSensitivity_beforePublication_noFixing_emptySeries() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES_EMPTY, DFCURVE);
    assertEquals(test.unitParameterSensitivity(DATE_BEFORE), CurveUnitParameterSensitivities.empty());
  }

  public void test_unitParameterSensitivity_beforePublication_noFixing_notEmptySeries() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES_MINIMAL, DFCURVE);
    assertEquals(test.unitParameterSensitivity(DATE_BEFORE), CurveUnitParameterSensitivities.empty());
  }

  public void test_unitParameterSensitivity_onPublication_fixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES, DFCURVE);
    assertEquals(test.unitParameterSensitivity(DATE_VAL), CurveUnitParameterSensitivities.empty());
  }

  public void test_unitParameterSensitivity_onPublication_noFixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES_EMPTY, DFCURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_VAL);
    CurveUnitParameterSensitivities expected = CurveUnitParameterSensitivities.of(
        CURVE.yValueParameterSensitivity(relativeYearFraction));
    assertEquals(test.unitParameterSensitivity(DATE_VAL), expected);
  }

  public void test_unitParameterSensitivity_afterPublication_noFixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES, DFCURVE);
    double relativeYearFraction = ACT_365F.relativeYearFraction(DATE_VAL, DATE_AFTER);
    CurveUnitParameterSensitivities expected = CurveUnitParameterSensitivities.of(
        CURVE.yValueParameterSensitivity(relativeYearFraction));
    assertEquals(test.unitParameterSensitivity(DATE_AFTER), expected);
  }

  //-------------------------------------------------------------------------
  // proper end-to-end tests are elsewhere
  public void test_curveParameterSensitivity() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES, DFCURVE);
    OvernightRateSensitivity point = OvernightRateSensitivity.of(GBP_SONIA, DATE_AFTER, DATE_AFTER_END, GBP, 1d);
    assertEquals(test.curveParameterSensitivity(point).size(), 1);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, SERIES, DFCURVE);
    coverImmutableBean(test);
    DiscountOvernightIndexRates test2 = DiscountOvernightIndexRates.of(USD_FED_FUND, SERIES_EMPTY, DFCURVE2);
    coverBeanEquals(test, test2);
  }

}
