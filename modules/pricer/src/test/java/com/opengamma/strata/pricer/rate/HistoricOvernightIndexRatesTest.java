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
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertThrows;

import java.time.LocalDate;
import java.util.Optional;

import org.testng.annotations.Test;

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
@Test
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
  public void test_of() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    assertEquals(test.getIndex(), GBP_SONIA);
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
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    assertEquals(test.rate(GBP_SONIA_BEFORE), RATE_BEFORE);
  }

  public void test_rate_beforeValuation_noFixing_emptySeries() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES_EMPTY);
    assertThrows(IllegalArgumentException.class, () -> test.rate(GBP_SONIA_BEFORE));
  }

  public void test_rate_beforeValuation_noFixing_notEmptySeries() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES_MINIMAL);
    assertThrows(IllegalArgumentException.class, () -> test.rate(GBP_SONIA_BEFORE));
  }

  public void test_rate_onValuation_fixing() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    assertEquals(test.rate(GBP_SONIA_VAL), RATE_VAL);
  }

  public void test_rate_onValuation_noFixing() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES_EMPTY);
    assertThrows(MarketDataNotFoundException.class, () -> test.rate(GBP_SONIA_VAL));
  }

  public void test_rate_afterValuation() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    assertThrows(MarketDataNotFoundException.class, () -> test.rate(GBP_SONIA_AFTER));
  }

  //-------------------------------------------------------------------------
  public void test_ratePointSensitivity_fixing() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    assertEquals(test.ratePointSensitivity(GBP_SONIA_BEFORE), PointSensitivityBuilder.none());
    assertEquals(test.ratePointSensitivity(GBP_SONIA_VAL), PointSensitivityBuilder.none());
  }

  public void test_ratePointSensitivity_onValuation_noFixing() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES_EMPTY);
    assertThrows(MarketDataNotFoundException.class, () -> test.ratePointSensitivity(GBP_SONIA_AFTER));
  }

  public void test_ratePointSensitivity_afterValuation() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    assertThrows(MarketDataNotFoundException.class, () -> test.ratePointSensitivity(GBP_SONIA_AFTER));
  }

  //-------------------------------------------------------------------------
  public void test_rateIgnoringFixings() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    assertThrows(MarketDataNotFoundException.class, () -> test.rateIgnoringFixings(GBP_SONIA_BEFORE));
    assertThrows(MarketDataNotFoundException.class, () -> test.rateIgnoringFixings(GBP_SONIA_VAL));
    assertThrows(MarketDataNotFoundException.class, () -> test.rateIgnoringFixings(GBP_SONIA_AFTER));
  }

  public void test_rateIgnoringFixingsPointSensitivity() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    assertThrows(MarketDataNotFoundException.class,
        () -> test.rateIgnoringFixingsPointSensitivity(GBP_SONIA_BEFORE));
    assertThrows(MarketDataNotFoundException.class,
        () -> test.rateIgnoringFixingsPointSensitivity(GBP_SONIA_VAL));
    assertThrows(MarketDataNotFoundException.class,
        () -> test.rateIgnoringFixingsPointSensitivity(GBP_SONIA_AFTER));
  }

  //-------------------------------------------------------------------------
  // proper end-to-end tests are elsewhere
  public void test_parameterSensitivity() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    OvernightRateSensitivity point = OvernightRateSensitivity.of(GBP_SONIA_AFTER, GBP, 1d);
    assertThrows(MarketDataNotFoundException.class, () -> test.parameterSensitivity(point));
  }

  //-------------------------------------------------------------------------
  public void test_createParameterSensitivity() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    DoubleArray sensitivities = DoubleArray.of(0.12, 0.15);
    assertThrows(MarketDataNotFoundException.class, () -> test.createParameterSensitivity(USD, sensitivities));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    HistoricOvernightIndexRates test = HistoricOvernightIndexRates.of(GBP_SONIA, DATE_VAL, SERIES);
    coverImmutableBean(test);
    HistoricOvernightIndexRates test2 = HistoricOvernightIndexRates.of(USD_FED_FUND, DATE_AFTER, SERIES_EMPTY);
    coverBeanEquals(test, test2);
  }
  
}
