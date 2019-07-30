/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.index.OvernightIndices.EUR_EONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
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

  private static final OvernightIndexObservation EUR_EONIA_VAL =
      OvernightIndexObservation.of(EUR_EONIA, DATE_VAL, REF_DATA);
  private static final OvernightIndexObservation EUR_EONIA_BEFORE =
      OvernightIndexObservation.of(EUR_EONIA, DATE_BEFORE, REF_DATA);
  private static final OvernightIndexObservation USD_FEDFUND_BEFORE =
      OvernightIndexObservation.of(USD_FED_FUND, DATE_BEFORE, REF_DATA);
  private static final OvernightIndexObservation EUR_EONIA_AFTER =
      OvernightIndexObservation.of(EUR_EONIA, DATE_AFTER, REF_DATA);
  private static final OvernightIndexObservation EUR_EONIA_AFTER_END =
      OvernightIndexObservation.of(EUR_EONIA, DATE_AFTER_END, REF_DATA);

  private static final CurveInterpolator INTERPOLATOR = CurveInterpolators.LINEAR;
  private static final CurveName NAME = CurveName.of("TestCurve");
  private static final CurveMetadata METADATA = Curves.zeroRates(NAME, ACT_365F);
  private static final InterpolatedNodalCurve CURVE =
      InterpolatedNodalCurve.of(METADATA, DoubleArray.of(0, 10), DoubleArray.of(0.01, 0.02), INTERPOLATOR);
  private static final InterpolatedNodalCurve CURVE2 =
      InterpolatedNodalCurve.of(METADATA, DoubleArray.of(0, 10), DoubleArray.of(0.01, 0.03), INTERPOLATOR);
  private static final ZeroRateDiscountFactors DFCURVE = ZeroRateDiscountFactors.of(EUR, DATE_VAL, CURVE);
  private static final ZeroRateDiscountFactors DFCURVE2 = ZeroRateDiscountFactors.of(EUR, DATE_VAL, CURVE2);

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
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE);
    assertEquals(test.getIndex(), EUR_EONIA);
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
    OvernightIndexRates test2 = OvernightIndexRates.of(EUR_EONIA, DATE_VAL, CURVE);
    assertEquals(test, test2);
  }

  public void test_of_withFixings() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE, SERIES);
    assertEquals(test.getIndex(), EUR_EONIA);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getFixings(), SERIES);
    assertEquals(test.getDiscountFactors(), DFCURVE);
  }

  //-------------------------------------------------------------------------
  public void test_withDiscountFactors() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE, SERIES);
    test = test.withDiscountFactors(DFCURVE2);
    assertEquals(test.getIndex(), EUR_EONIA);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getFixings(), SERIES);
    assertEquals(test.getDiscountFactors(), DFCURVE2);
  }

  //-------------------------------------------------------------------------
  public void test_rate_beforeValuation_fixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE, SERIES);
    assertEquals(test.rate(EUR_EONIA_BEFORE), RATE_BEFORE);
  }

  public void test_rate_beforeValuation_noFixing_emptySeries() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE, SERIES_EMPTY);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.rate(EUR_EONIA_BEFORE));
  }

  public void test_rate_beforeValuation_noFixing_notEmptySeries() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE, SERIES_MINIMAL);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.rate(EUR_EONIA_BEFORE));
  }

  public void test_rate_onValuation_fixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE, SERIES);
    assertEquals(test.rate(EUR_EONIA_VAL), RATE_VAL);
  }

  public void test_rateIgnoringFixings_onValuation_fixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE, SERIES);
    LocalDate startDate = EUR_EONIA_VAL.getEffectiveDate();
    LocalDate endDate = EUR_EONIA_VAL.getMaturityDate();
    double accrualFactor = EUR_EONIA_VAL.getYearFraction();
    double expected = (DFCURVE.discountFactor(startDate) / DFCURVE.discountFactor(endDate) - 1) / accrualFactor;
    assertEquals(test.rateIgnoringFixings(EUR_EONIA_VAL), expected, 1e-8);
  }

  public void test_rate_onPublication_noFixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE, SERIES_EMPTY);
    LocalDate startDate = EUR_EONIA_VAL.getEffectiveDate();
    LocalDate endDate = EUR_EONIA_VAL.getMaturityDate();
    double accrualFactor = EUR_EONIA.getDayCount().yearFraction(startDate, endDate);
    double expected = (DFCURVE.discountFactor(startDate) / DFCURVE.discountFactor(endDate) - 1) / accrualFactor;
    assertEquals(test.rate(EUR_EONIA_VAL), expected, 1e-4);
  }

  public void test_rate_afterPublication() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE, SERIES);
    LocalDate startDate = EUR_EONIA_AFTER.getEffectiveDate();
    LocalDate endDate = EUR_EONIA_AFTER.getMaturityDate();
    double accrualFactor = EUR_EONIA.getDayCount().yearFraction(startDate, endDate);
    double expected = (DFCURVE.discountFactor(startDate) / DFCURVE.discountFactor(endDate) - 1) / accrualFactor;
    assertEquals(test.rate(EUR_EONIA_AFTER), expected, 1e-8);
  }

  //-------------------------------------------------------------------------
  public void test_ratePointSensitivity_fixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE, SERIES);
    assertEquals(test.ratePointSensitivity(EUR_EONIA_BEFORE), PointSensitivityBuilder.none());
    assertEquals(test.ratePointSensitivity(EUR_EONIA_VAL), PointSensitivityBuilder.none());
  }

  public void test_rateIgnoringFixingsPointSensitivity_onValuation() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE, SERIES);
    OvernightRateSensitivity expected = OvernightRateSensitivity.of(EUR_EONIA_VAL, 1d);
    assertEquals(test.rateIgnoringFixingsPointSensitivity(EUR_EONIA_VAL), expected);
  }

  public void test_ratePointSensitivity_onPublication_noFixing() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE, SERIES_EMPTY);
    OvernightRateSensitivity expected = OvernightRateSensitivity.of(EUR_EONIA_VAL, 1d);
    assertEquals(test.ratePointSensitivity(EUR_EONIA_VAL), expected);
  }

  public void test_ratePointSensitivity_afterPublication() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE, SERIES);
    OvernightRateSensitivity expected = OvernightRateSensitivity.of(EUR_EONIA_AFTER, 1d);
    assertEquals(test.ratePointSensitivity(EUR_EONIA_AFTER), expected);
  }

  //-------------------------------------------------------------------------
  public void test_periodRate() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE, SERIES);
    double accrualFactor = EUR_EONIA.getDayCount().yearFraction(DATE_AFTER, DATE_AFTER_END);
    double expected = (DFCURVE.discountFactor(DATE_AFTER) / DFCURVE.discountFactor(DATE_AFTER_END) - 1) / accrualFactor;
    assertEquals(test.periodRate(EUR_EONIA_AFTER, DATE_AFTER_END), expected, 1e-8);
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
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE, SERIES);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.periodRate(EUR_EONIA_AFTER_END, DATE_AFTER));
  }

  //-------------------------------------------------------------------------
  public void test_periodRatePointSensitivity() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE, SERIES);
    OvernightRateSensitivity expected = OvernightRateSensitivity.ofPeriod(EUR_EONIA_AFTER, DATE_AFTER_END, EUR, 1d);
    assertEquals(test.periodRatePointSensitivity(EUR_EONIA_AFTER, DATE_AFTER_END), expected);
  }
  
  public void test_periodRatePointSensitivity_onholidaybeforepublication() {
    LocalDate lastFixingDate = LocalDate.of(2017, 6, 30);
    LocalDate gbdBeforeValDate = LocalDate.of(2017, 7, 3);
    LocalDate gbdAfterValDate = LocalDate.of(2017, 7, 5);
    double fixingValue = 0.0010;
    InterpolatedNodalCurve curve =
        InterpolatedNodalCurve.of(METADATA, DoubleArray.of(-1.0d, 10.0d), DoubleArray.of(0.01, 0.02), INTERPOLATOR);
    ZeroRateDiscountFactors df = ZeroRateDiscountFactors.of(USD, LocalDate.of(2017, 7, 4), curve);
    LocalDateDoubleTimeSeries series = LocalDateDoubleTimeSeries.builder()
        .put(lastFixingDate, fixingValue)
        .build();
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(USD_FED_FUND, df, series);
    OvernightIndexObservation obs = OvernightIndexObservation.of(USD_FED_FUND, gbdBeforeValDate, REF_DATA);
    OvernightRateSensitivity expected = OvernightRateSensitivity.ofPeriod(obs, gbdAfterValDate, USD, 1d);
    assertEquals(test.periodRatePointSensitivity(obs, gbdAfterValDate), expected);
  }

  public void test_periodRatePointSensitivity_badDates() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE, SERIES);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.periodRatePointSensitivity(EUR_EONIA_AFTER_END, DATE_AFTER));
  }

  //-------------------------------------------------------------------------
  // proper end-to-end tests are elsewhere
  public void test_parameterSensitivity() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE, SERIES);
    OvernightRateSensitivity point = OvernightRateSensitivity.ofPeriod(EUR_EONIA_AFTER, DATE_AFTER_END, EUR, 1d);
    assertEquals(test.parameterSensitivity(point).size(), 1);
  }
  
  //-------------------------------------------------------------------------
  public void test_createParameterSensitivity() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE, SERIES);
    DoubleArray sensitivities = DoubleArray.of(0.12, 0.15);
    CurrencyParameterSensitivities sens = test.createParameterSensitivity(USD, sensitivities);
    assertEquals(sens.getSensitivities().get(0), CURVE.createParameterSensitivity(USD, sensitivities));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    DiscountOvernightIndexRates test = DiscountOvernightIndexRates.of(EUR_EONIA, DFCURVE, SERIES);
    coverImmutableBean(test);
    DiscountOvernightIndexRates test2 = DiscountOvernightIndexRates.of(USD_FED_FUND, DFCURVE2, SERIES_EMPTY);
    coverBeanEquals(test, test2);
  }

}
