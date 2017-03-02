/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.OvernightIndexObservation;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.curve.CurveMetadata;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.curve.Curves;
import com.opengamma.strata.market.curve.InterpolatedNodalCurve;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolator;
import com.opengamma.strata.market.curve.interpolator.CurveInterpolators;
import com.opengamma.strata.market.param.CurrencyParameterSensitivities;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;
import com.opengamma.strata.pricer.ZeroRateDiscountFactors;

/**
 * Test {@link DiscountOvernightIndexRates}.
 */
@Test
public class DiscountOvernightIndexRatesTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DATE_VAL = date(2015, 6, 3);
  private static final LocalDate DATE_BEFORE = date(2015, 6, 2);
  private static final LocalDate DATE_AFTER = date(2015, 7, 30);
  private static final LocalDate DATE_AFTER_END = date(2015, 7, 31);

  private static final OvernightIndexObservation GBP_SONIA_VAL =
      OvernightIndexObservation.of(GBP_SONIA, DATE_VAL, REF_DATA);
  private static final OvernightIndexObservation GBP_SONIA_BEFORE =
      OvernightIndexObservation.of(GBP_SONIA, DATE_BEFORE, REF_DATA);
  private static final OvernightIndexObservation USD_FEDFUND_BEFORE =
      OvernightIndexObservation.of(USD_FED_FUND, DATE_BEFORE, REF_DATA);
  private static final OvernightIndexObservation GBP_SONIA_AFTER =
      OvernightIndexObservation.of(GBP_SONIA, DATE_AFTER, REF_DATA);
  private static final OvernightIndexObservation GBP_SONIA_AFTER_END =
      OvernightIndexObservation.of(GBP_SONIA, DATE_AFTER_END, REF_DATA);

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
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getFixings(), SERIES_EMPTY);
    assertEquals(test.getDiscountFactors(), DFCURVE);
    assertEquals(test.getParameterCount(), DFCURVE.getParameterCount());
    assertEquals(test.getParameter(0), DFCURVE.getParameter(0));
    assertEquals(test.getParameterMetadata(0), DFCURVE.getParameterMetadata(0));
    assertEquals(test.withParameter(0, 1d).getDiscountFactors(), DFCURVE.withParameter(0, 1d));
    assertEquals(test.withPerturbation((i, v, m) -> v + 1d).getDiscountFactors(), DFCURVE.withPerturbation((i, v, m) -> v + 1d));
    assertEquals(test.findData(CURVE.getName()), Optional.of(CURVE));
    assertEquals(test.findData(CurveName.of("Rubbish")), Optional.empty());
    // check IborIndexRates
    OvernightIndexRates test2 = OvernightIndexRates.of(GBP_SONIA, DATE_VAL, CURVE);
    assertEquals(test, test2);
  }

  public void test_of_withFixings() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE, SERIES);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getFixings(), SERIES);
    assertEquals(test.getDiscountFactors(), DFCURVE);
  }

  //-------------------------------------------------------------------------
  public void test_withDiscountFactors() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE, SERIES);
    test = test.withDiscountFactors(DFCURVE2);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getFixings(), SERIES);
    assertEquals(test.getDiscountFactors(), DFCURVE2);
  }

  //-------------------------------------------------------------------------
  public void test_rate_beforeValuation_fixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE, SERIES);
    assertEquals(test.rate(GBP_SONIA_BEFORE), RATE_BEFORE);
  }

  public void test_rate_beforeValuation_noFixing_emptySeries() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE, SERIES_EMPTY);
    assertThrowsIllegalArg(() -> test.rate(GBP_SONIA_BEFORE));
  }

  public void test_rate_beforeValuation_noFixing_notEmptySeries() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE, SERIES_MINIMAL);
    assertThrowsIllegalArg(() -> test.rate(GBP_SONIA_BEFORE));
  }

  public void test_rate_onValuation_fixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE, SERIES);
    assertEquals(test.rate(GBP_SONIA_VAL), RATE_VAL);
  }

  public void test_rateIgnoringFixings_onValuation_fixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE, SERIES);
    LocalDate startDate = GBP_SONIA_VAL.getEffectiveDate();
    LocalDate endDate = GBP_SONIA_VAL.getMaturityDate();
    double accrualFactor = GBP_SONIA_VAL.getYearFraction();
    double expected = (DFCURVE.discountFactor(startDate) / DFCURVE.discountFactor(endDate) - 1) / accrualFactor;
    assertEquals(test.rateIgnoringFixings(GBP_SONIA_VAL), expected, 1e-8);
  }

  public void test_rate_onPublication_noFixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE, SERIES_EMPTY);
    LocalDate startDate = GBP_SONIA_VAL.getEffectiveDate();
    LocalDate endDate = GBP_SONIA_VAL.getMaturityDate();
    double accrualFactor = GBP_SONIA.getDayCount().yearFraction(startDate, endDate);
    double expected = (DFCURVE.discountFactor(startDate) / DFCURVE.discountFactor(endDate) - 1) / accrualFactor;
    assertEquals(test.rate(GBP_SONIA_VAL), expected, 1e-4);
  }

  public void test_rate_afterPublication() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE, SERIES);
    LocalDate startDate = GBP_SONIA_AFTER.getEffectiveDate();
    LocalDate endDate = GBP_SONIA_AFTER.getMaturityDate();
    double accrualFactor = GBP_SONIA.getDayCount().yearFraction(startDate, endDate);
    double expected = (DFCURVE.discountFactor(startDate) / DFCURVE.discountFactor(endDate) - 1) / accrualFactor;
    assertEquals(test.rate(GBP_SONIA_AFTER), expected, 1e-8);
  }

  //-------------------------------------------------------------------------
  public void test_ratePointSensitivity_fixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE, SERIES);
    assertEquals(test.ratePointSensitivity(GBP_SONIA_BEFORE), PointSensitivityBuilder.none());
    assertEquals(test.ratePointSensitivity(GBP_SONIA_VAL), PointSensitivityBuilder.none());
  }

  public void test_rateIgnoringFixingsPointSensitivity_onValuation() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE, SERIES);
    OvernightRateSensitivity expected = OvernightRateSensitivity.of(GBP_SONIA_VAL, 1d);
    assertEquals(test.rateIgnoringFixingsPointSensitivity(GBP_SONIA_VAL), expected);
  }

  public void test_ratePointSensitivity_onPublication_noFixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE, SERIES_EMPTY);
    OvernightRateSensitivity expected = OvernightRateSensitivity.of(GBP_SONIA_VAL, 1d);
    assertEquals(test.ratePointSensitivity(GBP_SONIA_VAL), expected);
  }

  public void test_ratePointSensitivity_afterPublication() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE, SERIES);
    OvernightRateSensitivity expected = OvernightRateSensitivity.of(GBP_SONIA_AFTER, 1d);
    assertEquals(test.ratePointSensitivity(GBP_SONIA_AFTER), expected);
  }

  //-------------------------------------------------------------------------
  public void test_periodRate() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE, SERIES);
    double accrualFactor = GBP_SONIA.getDayCount().yearFraction(DATE_AFTER, DATE_AFTER_END);
    double expected = (DFCURVE.discountFactor(DATE_AFTER) / DFCURVE.discountFactor(DATE_AFTER_END) - 1) / accrualFactor;
    assertEquals(test.periodRate(GBP_SONIA_AFTER, DATE_AFTER_END), expected, 1e-8);
  }
  
  // This type of "forward" for the day before is required when the publication offset is 1.
  // The fixing for the previous day will still be unknown at the beginning of the day and need to be computed from the curve.
  public void test_periodRate_publication_1() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(USD_FED_FUND, DFCURVE, SERIES);
    double accrualFactor = USD_FED_FUND.getDayCount().yearFraction(DATE_BEFORE, DATE_VAL);
    double expected = (DFCURVE.discountFactor(DATE_BEFORE) / DFCURVE.discountFactor(DATE_VAL) - 1) / accrualFactor;
    assertEquals(test.periodRate(USD_FEDFUND_BEFORE, DATE_VAL), expected, 1e-8);
  }

  public void test_periodRate_badDates() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE, SERIES);
    assertThrowsIllegalArg(() -> test.periodRate(GBP_SONIA_AFTER_END, DATE_AFTER));
  }

  //-------------------------------------------------------------------------
  public void test_periodRatePointSensitivity() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE, SERIES);
    OvernightRateSensitivity expected = OvernightRateSensitivity.ofPeriod(GBP_SONIA_AFTER, DATE_AFTER_END, GBP, 1d);
    assertEquals(test.periodRatePointSensitivity(GBP_SONIA_AFTER, DATE_AFTER_END), expected);
  }

  public void test_periodRatePointSensitivity_badDates() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE, SERIES);
    assertThrowsIllegalArg(() -> test.periodRatePointSensitivity(GBP_SONIA_BEFORE, DATE_VAL));
    assertThrowsIllegalArg(() -> test.periodRatePointSensitivity(GBP_SONIA_AFTER_END, DATE_AFTER));
  }

  //-------------------------------------------------------------------------
  // proper end-to-end tests are elsewhere
  public void test_parameterSensitivity() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE, SERIES);
    OvernightRateSensitivity point = OvernightRateSensitivity.ofPeriod(GBP_SONIA_AFTER, DATE_AFTER_END, GBP, 1d);
    assertEquals(test.parameterSensitivity(point).size(), 1);
  }

  //-------------------------------------------------------------------------
  public void test_createParameterSensitivity() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE, SERIES);
    DoubleArray sensitivities = DoubleArray.of(0.12, 0.15);
    CurrencyParameterSensitivities sens = test.createParameterSensitivity(USD, sensitivities);
    assertEquals(sens.getSensitivities().get(0), CURVE.createParameterSensitivity(USD, sensitivities));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(GBP_SONIA, DFCURVE, SERIES);
    coverImmutableBean(test);
    DiscountOvernightIndexRates test2 = DiscountOvernightIndexRates.of(USD_FED_FUND, DFCURVE2, SERIES_EMPTY);
    coverBeanEquals(test, test2);
  }

}
