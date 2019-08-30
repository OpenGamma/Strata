/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.IborIndexObservation;
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
 * Test {@link DiscountIborIndexRates}.
 */
public class DiscountIborIndexRatesTest {

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
  private static final CurveMetadata METADATA = Curves.zeroRates(NAME, CURVE_DAY_COUNT);
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
  
  private static final double TOLERANCE_RATE = 1.0E-8;

  //-------------------------------------------------------------------------
  @Test
  public void test_of_withoutFixings() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE);
    assertThat(test.getIndex()).isEqualTo(GBP_LIBOR_3M);
    assertThat(test.getValuationDate()).isEqualTo(DATE_VAL);
    assertThat(test.getFixings()).isEqualTo(SERIES_EMPTY);
    assertThat(test.getDiscountFactors()).isEqualTo(DFCURVE);
    assertThat(test.getDiscountFactors()).isEqualTo(DFCURVE);
    assertThat(test.getParameterCount()).isEqualTo(DFCURVE.getParameterCount());
    assertThat(test.getParameter(0)).isEqualTo(DFCURVE.getParameter(0));
    assertThat(test.getParameterMetadata(0)).isEqualTo(DFCURVE.getParameterMetadata(0));
    assertThat(test.withParameter(0, 1d).getDiscountFactors()).isEqualTo(DFCURVE.withParameter(0, 1d));
    assertThat(test.withPerturbation((i, v, m) -> v + 1d).getDiscountFactors()).isEqualTo(DFCURVE.withPerturbation((i, v, m) -> v + 1d));
    assertThat(test.findData(CURVE.getName())).isEqualTo(Optional.of(CURVE));
    assertThat(test.findData(CurveName.of("Rubbish"))).isEqualTo(Optional.empty());
    // check IborIndexRates
    IborIndexRates test2 = IborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, CURVE);
    assertThat(test).isEqualTo(test2);
  }

  @Test
  public void test_of_withFixings() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    assertThat(test.getIndex()).isEqualTo(GBP_LIBOR_3M);
    assertThat(test.getValuationDate()).isEqualTo(DATE_VAL);
    assertThat(test.getFixings()).isEqualTo(SERIES);
    assertThat(test.getDiscountFactors()).isEqualTo(DFCURVE);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_withDiscountFactors() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    test = test.withDiscountFactors(DFCURVE2);
    assertThat(test.getIndex()).isEqualTo(GBP_LIBOR_3M);
    assertThat(test.getValuationDate()).isEqualTo(DATE_VAL);
    assertThat(test.getFixings()).isEqualTo(SERIES);
    assertThat(test.getDiscountFactors()).isEqualTo(DFCURVE2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_rate_beforeValuation_fixing() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    assertThat(test.rate(GBP_LIBOR_3M_BEFORE)).isEqualTo(RATE_BEFORE);
  }

  @Test
  public void test_rate_beforeValuation_noFixing_emptySeries() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES_EMPTY);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.rate(GBP_LIBOR_3M_BEFORE));
  }

  @Test
  public void test_rate_beforeValuation_noFixing_notEmptySeries() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES_MINIMAL);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.rate(GBP_LIBOR_3M_BEFORE));
  }

  @Test
  public void test_rate_onValuation_fixing() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    assertThat(test.rate(GBP_LIBOR_3M_VAL)).isEqualTo(RATE_VAL);
  }

  @Test
  public void test_rateIgnoringFixings_onValuation_fixing() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    LocalDate startDate = GBP_LIBOR_3M_VAL.getEffectiveDate();
    LocalDate endDate = GBP_LIBOR_3M_VAL.getMaturityDate();
    double accrualFactor = GBP_LIBOR_3M_VAL.getYearFraction();
    double expected = (DFCURVE.discountFactor(startDate) / DFCURVE.discountFactor(endDate) - 1) / accrualFactor;
    assertThat(test.rateIgnoringFixings(GBP_LIBOR_3M_VAL)).isCloseTo(expected, offset(TOLERANCE_RATE));
  }

  @Test
  public void test_rate_onValuation_noFixing() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES_EMPTY);
    LocalDate startDate = GBP_LIBOR_3M_VAL.getEffectiveDate();
    LocalDate endDate = GBP_LIBOR_3M_VAL.getMaturityDate();
    double accrualFactor = GBP_LIBOR_3M_VAL.getYearFraction();
    double expected = (DFCURVE.discountFactor(startDate) / DFCURVE.discountFactor(endDate) - 1) / accrualFactor;
    assertThat(test.rate(GBP_LIBOR_3M_VAL)).isCloseTo(expected, offset(TOLERANCE_RATE));
    assertThat(test.rateIgnoringFixings(GBP_LIBOR_3M_VAL)).isCloseTo(expected, offset(TOLERANCE_RATE));
  }

  @Test
  public void test_rate_afterValuation() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    LocalDate startDate = GBP_LIBOR_3M_AFTER.getEffectiveDate();
    LocalDate endDate = GBP_LIBOR_3M_AFTER.getMaturityDate();
    double accrualFactor = GBP_LIBOR_3M_AFTER.getYearFraction();
    double expected = (DFCURVE.discountFactor(startDate) / DFCURVE.discountFactor(endDate) - 1) / accrualFactor;
    assertThat(test.rate(GBP_LIBOR_3M_AFTER)).isCloseTo(expected, offset(TOLERANCE_RATE));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_ratePointSensitivity_fixing() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    assertThat(test.ratePointSensitivity(GBP_LIBOR_3M_BEFORE)).isEqualTo(PointSensitivityBuilder.none());
    assertThat(test.ratePointSensitivity(GBP_LIBOR_3M_VAL)).isEqualTo(PointSensitivityBuilder.none());
  }
  
  @Test
  public void test_rateIgnoringFixingsPointSensitivity_onValuation() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    IborRateSensitivity expected = IborRateSensitivity.of(GBP_LIBOR_3M_VAL, 1d);
    assertThat(test.rateIgnoringFixingsPointSensitivity(GBP_LIBOR_3M_VAL)).isEqualTo(expected);
  }

  @Test
  public void test_ratePointSensitivity_onValuation_noFixing() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES_EMPTY);
    IborRateSensitivity expected = IborRateSensitivity.of(GBP_LIBOR_3M_VAL, 1d);
    assertThat(test.ratePointSensitivity(GBP_LIBOR_3M_VAL)).isEqualTo(expected);
    assertThat(test.rateIgnoringFixingsPointSensitivity(GBP_LIBOR_3M_VAL)).isEqualTo(expected);
  }

  @Test
  public void test_ratePointSensitivity_afterValuation() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    IborRateSensitivity expected = IborRateSensitivity.of(GBP_LIBOR_3M_AFTER, 1d);
    assertThat(test.ratePointSensitivity(GBP_LIBOR_3M_AFTER)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  // proper end-to-end tests are elsewhere
  @Test
  public void test_parameterSensitivity() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    IborRateSensitivity point = IborRateSensitivity.of(GBP_LIBOR_3M_AFTER, GBP, 1d);
    assertThat(test.parameterSensitivity(point).size()).isEqualTo(1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createParameterSensitivity() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    DoubleArray sensitivities = DoubleArray.of(0.12, 0.15);
    CurrencyParameterSensitivities sens = test.createParameterSensitivity(USD, sensitivities);
    assertThat(sens.getSensitivities().get(0)).isEqualTo(CURVE.createParameterSensitivity(USD, sensitivities));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    DiscountIborIndexRates test = DiscountIborIndexRates.of(GBP_LIBOR_3M, DFCURVE, SERIES);
    coverImmutableBean(test);
    DiscountIborIndexRates test2 = DiscountIborIndexRates.of(USD_LIBOR_3M, DFCURVE2, SERIES_EMPTY);
    coverBeanEquals(test, test2);
  }

}
