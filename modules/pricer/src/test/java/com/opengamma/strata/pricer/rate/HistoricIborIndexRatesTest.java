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
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertThrows;

import java.time.LocalDate;
import java.util.Optional;

import org.testng.annotations.Test;

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
@Test
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
  public void test_of() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getValuationDate(), DATE_VAL);
    assertEquals(test.getFixings(), SERIES);
    assertEquals(test.getParameterCount(), 0);
    assertThrows(IndexOutOfBoundsException.class, () -> test.getParameter(0));
    assertThrows(IndexOutOfBoundsException.class, () -> test.getParameterMetadata(0));
    assertThrows(IndexOutOfBoundsException.class, () -> test.withParameter(0, 1d));
    assertSame(test.withPerturbation((i, v, m) -> v + 1d), test);
    assertEquals(test.findData(CurveName.of("Rubbish")), Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void test_rate_beforeValuation_fixing() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    assertEquals(test.rate(GBP_LIBOR_3M_BEFORE), RATE_BEFORE);
  }

  public void test_rate_beforeValuation_noFixing_emptySeries() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES_EMPTY);
    assertThrows(IllegalArgumentException.class, () -> test.rate(GBP_LIBOR_3M_BEFORE));
  }

  public void test_rate_beforeValuation_noFixing_notEmptySeries() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES_MINIMAL);
    assertThrows(IllegalArgumentException.class, () -> test.rate(GBP_LIBOR_3M_BEFORE));
  }

  public void test_rate_onValuation_fixing() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    assertEquals(test.rate(GBP_LIBOR_3M_VAL), RATE_VAL);
  }

  public void test_rate_onValuation_noFixing() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES_EMPTY);
    assertThrows(MarketDataNotFoundException.class, () -> test.rate(GBP_LIBOR_3M_VAL));
  }

  public void test_rate_afterValuation() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    assertThrows(MarketDataNotFoundException.class, () -> test.rate(GBP_LIBOR_3M_AFTER));
  }

  //-------------------------------------------------------------------------
  public void test_ratePointSensitivity_fixing() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    assertEquals(test.ratePointSensitivity(GBP_LIBOR_3M_BEFORE), PointSensitivityBuilder.none());
    assertEquals(test.ratePointSensitivity(GBP_LIBOR_3M_VAL), PointSensitivityBuilder.none());
  }

  public void test_ratePointSensitivity_onValuation_noFixing() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES_EMPTY);
    assertThrows(MarketDataNotFoundException.class, () -> test.ratePointSensitivity(GBP_LIBOR_3M_AFTER));
  }

  public void test_ratePointSensitivity_afterValuation() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    assertThrows(MarketDataNotFoundException.class, () -> test.ratePointSensitivity(GBP_LIBOR_3M_AFTER));
  }

  //-------------------------------------------------------------------------
  public void test_rateIgnoringFixings() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    assertThrows(MarketDataNotFoundException.class, () -> test.rateIgnoringFixings(GBP_LIBOR_3M_BEFORE));
    assertThrows(MarketDataNotFoundException.class, () -> test.rateIgnoringFixings(GBP_LIBOR_3M_VAL));
    assertThrows(MarketDataNotFoundException.class, () -> test.rateIgnoringFixings(GBP_LIBOR_3M_AFTER));
  }

  public void test_rateIgnoringFixingsPointSensitivity() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    assertThrows(MarketDataNotFoundException.class,
        () -> test.rateIgnoringFixingsPointSensitivity(GBP_LIBOR_3M_BEFORE));
    assertThrows(MarketDataNotFoundException.class,
        () -> test.rateIgnoringFixingsPointSensitivity(GBP_LIBOR_3M_VAL));
    assertThrows(MarketDataNotFoundException.class,
        () -> test.rateIgnoringFixingsPointSensitivity(GBP_LIBOR_3M_AFTER));
  }

  //-------------------------------------------------------------------------
  // proper end-to-end tests are elsewhere
  public void test_parameterSensitivity() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    IborRateSensitivity point = IborRateSensitivity.of(GBP_LIBOR_3M_AFTER, GBP, 1d);
    assertThrows(MarketDataNotFoundException.class, () -> test.parameterSensitivity(point));
  }

  //-------------------------------------------------------------------------
  public void test_createParameterSensitivity() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    DoubleArray sensitivities = DoubleArray.of(0.12, 0.15);
    assertThrows(MarketDataNotFoundException.class, () -> test.createParameterSensitivity(USD, sensitivities));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    HistoricIborIndexRates test = HistoricIborIndexRates.of(GBP_LIBOR_3M, DATE_VAL, SERIES);
    coverImmutableBean(test);
    HistoricIborIndexRates test2 = HistoricIborIndexRates.of(USD_LIBOR_3M, DATE_AFTER, SERIES_EMPTY);
    coverBeanEquals(test, test2);
  }
  
}
