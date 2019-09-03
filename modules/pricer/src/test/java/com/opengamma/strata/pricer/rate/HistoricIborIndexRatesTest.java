/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.rate;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
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
import com.opengamma.strata.basics.index.IborIndexObservation;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.MarketDataNotFoundException;
import com.opengamma.strata.market.curve.CurveName;
import com.opengamma.strata.market.sensitivity.PointSensitivityBuilder;

/**
 * Tests {@link HistoricIborIndexRates}.
 */
public class HistoricIborIndexRatesTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DATE_VAL = date(2015, 6, 4);
  private static final LocalDate DATE_BEFORE = date(2015, 6, 3);
  private static final LocalDate DATE_AFTER = date(2015, 7, 30);

  private static final IborIndexObservation GBP_LIBOR_3M_VAL = IborIndexObservation.of(GBP_LIBOR_3M, DATE_VAL, REF_DATA);
  private static final IborIndexObservation GBP_LIBOR_3M_BEFORE = IborIndexObservation.of(GBP_LIBOR_3M, DATE_BEFORE, REF_DATA);
  private static final IborIndexObservation GBP_LIBOR_3M_AFTER = IborIndexObservation.of(GBP_LIBOR_3M, DATE_AFTER, REF_DATA);

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
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    assertThat(test.getIndex()).isEqualTo(GBP_LIBOR_3M);
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
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    assertThat(test.rate(GBP_LIBOR_3M_BEFORE)).isEqualTo(RATE_BEFORE);
  }

  @Test
  public void test_rate_beforeValuation_noFixing_emptySeries() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES_EMPTY);
    assertThatIllegalArgumentException().isThrownBy(() -> test.rate(GBP_LIBOR_3M_BEFORE));
  }

  @Test
  public void test_rate_beforeValuation_noFixing_notEmptySeries() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES_MINIMAL);
    assertThatIllegalArgumentException().isThrownBy(() -> test.rate(GBP_LIBOR_3M_BEFORE));
  }

  @Test
  public void test_rate_onValuation_fixing() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    assertThat(test.rate(GBP_LIBOR_3M_VAL)).isEqualTo(RATE_VAL);
  }

  @Test
  public void test_rate_onValuation_noFixing() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES_EMPTY);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.rate(GBP_LIBOR_3M_VAL));
  }

  @Test
  public void test_rate_afterValuation() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.rate(GBP_LIBOR_3M_AFTER));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_ratePointSensitivity_fixing() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    assertThat(test.ratePointSensitivity(GBP_LIBOR_3M_BEFORE)).isEqualTo(PointSensitivityBuilder.none());
    assertThat(test.ratePointSensitivity(GBP_LIBOR_3M_VAL)).isEqualTo(PointSensitivityBuilder.none());
  }

  @Test
  public void test_ratePointSensitivity_onValuation_noFixing() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES_EMPTY);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.ratePointSensitivity(GBP_LIBOR_3M_AFTER));
  }

  @Test
  public void test_ratePointSensitivity_afterValuation() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.ratePointSensitivity(GBP_LIBOR_3M_AFTER));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_rateIgnoringFixings() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.rateIgnoringFixings(GBP_LIBOR_3M_BEFORE));
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.rateIgnoringFixings(GBP_LIBOR_3M_VAL));
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.rateIgnoringFixings(GBP_LIBOR_3M_AFTER));
  }

  @Test
  public void test_rateIgnoringFixingsPointSensitivity() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.rateIgnoringFixingsPointSensitivity(GBP_LIBOR_3M_BEFORE));
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.rateIgnoringFixingsPointSensitivity(GBP_LIBOR_3M_VAL));
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.rateIgnoringFixingsPointSensitivity(GBP_LIBOR_3M_AFTER));
  }

  //-------------------------------------------------------------------------
  // proper end-to-end tests are elsewhere
  @Test
  public void test_parameterSensitivity() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    IborRateSensitivity point = IborRateSensitivity.of(GBP_LIBOR_3M_AFTER, GBP, 1d);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.parameterSensitivity(point));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_createParameterSensitivity() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    DoubleArray sensitivities = DoubleArray.of(0.12, 0.15);
    assertThatExceptionOfType(MarketDataNotFoundException.class).isThrownBy(() -> test.createParameterSensitivity(USD, sensitivities));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    coverImmutableBean(test);
    HistoricIborIndexRates test2 = HistoricIborIndexRates.of(USD_LIBOR_3M, DATE_AFTER, SERIES_EMPTY);
    coverBeanEquals(test, test2);
  }
  
}
