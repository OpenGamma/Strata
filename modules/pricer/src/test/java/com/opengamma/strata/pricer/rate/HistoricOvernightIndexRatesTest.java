/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.OvernightIndexObservation;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Tests {@link HistoricOvernightIndexRates}.
 */
public class HistoricOvernightIndexRatesTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DATE_VAL = date(2015, 6, 4);
  private static final LocalDate DATE_BEFORE = date(2015, 6, 3);
  private static final LocalDate DATE_AFTER = date(2015, 7, 30);

  private static final OvernightIndexObservation GBP_SONIA_VAL =
      OvernightIndexObservation.of(GBP_SONIA, DATE_VAL, REF_DATA);
  private static final OvernightIndexObservation GBP_SONIA_BEFORE =
      OvernightIndexObservation.of(GBP_SONIA, DATE_BEFORE, REF_DATA);
  private static final OvernightIndexObservation GBP_SONIA_AFTER =
      OvernightIndexObservation.of(GBP_SONIA, DATE_AFTER, REF_DATA);

  private static final double RATE_BEFORE = 0.013d;
  private static final double RATE_VAL = 0.014d;
  private static final LocalDateDoubleTimeSeries SERIES = LocalDateDoubleTimeSeries.builder()
      .put(DATE_BEFORE, RATE_BEFORE)
      .put(DATE_VAL, RATE_VAL)
      .build();
  private static final LocalDateDoubleTimeSeries SERIES_MINIMAL = LocalDateDoubleTimeSeries.of(DATE_VAL, RATE_VAL);
  private static final LocalDateDoubleTimeSeries SERIES_EMPTY = LocalDateDoubleTimeSeries.empty();

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    assertThat(test.getIndex()).isEqualTo(GBP_SONIA);
    assertThat(test.getValuationDate()).isEqualTo(DATE_VAL);
    assertThat(test.getFixings()).isEqualTo(SERIES);
    assertThat(test.getParameterCount()).isEqualTo(0);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.getParameter(0));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.getParameterMetadata(0));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.withParameter(0, 1d));
    assertThat(test.withPerturbation((i, v, m) -> v + 1d)).isSameAs(test);
    assertThat(test.findData(CurveName.of("Rubbish"))).isEqualTo(Optional.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_rate_beforeValuation_fixing() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    assertThat(test.rate(GBP_SONIA_BEFORE)).isEqualTo(RATE_BEFORE);
  }

  @Test
  public void test_rate_beforeValuation_noFixing_emptySeries() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES_EMPTY);
    assertThatIllegalArgumentException().isThrownBy(() -> test.rate(GBP_SONIA_BEFORE));
  }

  @Test
  public void test_rate_beforeValuation_noFixing_notEmptySeries() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES_MINIMAL);
    assertThatIllegalArgumentException().isThrownBy(() -> test.rate(GBP_SONIA_BEFORE));
  }

  @Test
  public void test_rate_onValuation_fixing() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    assertThat(test.rate(GBP_SONIA_VAL)).isEqualTo(RATE_VAL);
  }

  @Test
  public void test_rate_onValuation_noFixing() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES_EMPTY);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.rate(GBP_SONIA_VAL));
  }

  @Test
  public void test_rate_afterValuation() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.rate(GBP_SONIA_AFTER));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_ratePointSensitivity_fixing() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    assertThat(test.ratePointSensitivity(GBP_SONIA_BEFORE)).isEqualTo(PointSensitivityBuilder.none());
    assertThat(test.ratePointSensitivity(GBP_SONIA_VAL)).isEqualTo(PointSensitivityBuilder.none());
  }

  @Test
  public void test_ratePointSensitivity_onValuation_noFixing() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES_EMPTY);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.ratePointSensitivity(GBP_SONIA_AFTER));
  }

  @Test
  public void test_ratePointSensitivity_afterValuation() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.ratePointSensitivity(GBP_SONIA_AFTER));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_rateIgnoringFixings() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.rateIgnoringFixings(GBP_SONIA_BEFORE));
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.rateIgnoringFixings(GBP_SONIA_VAL));
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.rateIgnoringFixings(GBP_SONIA_AFTER));
  }

  @Test
  public void test_rateIgnoringFixingsPointSensitivity() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.rateIgnoringFixingsPointSensitivity(GBP_SONIA_BEFORE));
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.rateIgnoringFixingsPointSensitivity(GBP_SONIA_VAL));
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.rateIgnoringFixingsPointSensitivity(GBP_SONIA_AFTER));
  }

  //-------------------------------------------------------------------------
  // proper end-to-end tests are elsewhere
  @Test
  public void test_parameterSensitivity() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    OvernightRateSensitivity point = OvernightRateSensitivity.of(GBP_SONIA_AFTER, GBP, 1d);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.parameterSensitivity(point));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createParameterSensitivity() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    DoubleArray sensitivities = DoubleArray.of(0.12, 0.15);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.createParameterSensitivity(USD, sensitivities));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    coverImmutableBean(test);
    HistoricOvernightIndexRates test2 = HistoricOvernightIndexRates.of(USD_FED_FUND, DATE_AFTER, SERIES_EMPTY);
    coverBeanEquals(test, test2);
  }
  
}
