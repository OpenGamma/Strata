/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.loader.csv;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.PriceIndices;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.data.ObservableId;
import com.opengamma.strata.market.observable.IndexQuoteId;

/**
 * Test {@link FixingSeriesCsvLoader}.
 */
@Test
public class FixingSeriesCsvLoaderTest {

  private static final IndexQuoteId ID_USD_LIBOR_3M = IndexQuoteId.of(IborIndices.USD_LIBOR_3M);
  private static final IndexQuoteId ID_USD_LIBOR_6M = IndexQuoteId.of(IborIndices.USD_LIBOR_6M);
  private static final IndexQuoteId ID_GB_RPI = IndexQuoteId.of(PriceIndices.GB_RPI);

  private static final ResourceLocator FIXING_SERIES_1 =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/fixings-1.csv");
  private static final ResourceLocator FIXING_SERIES_2 =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/fixings-2.csv");
  private static final ResourceLocator FIXING_SERIES_1_AND_2 =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/fixings-1-and-2.csv");
  private static final ResourceLocator FIXING_SERIES_INVALID_DATE =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/fixings-invalid-date.csv");
  private static final ResourceLocator FIXING_SERIES_PRICE1 =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/fixings-price1.csv");
  private static final ResourceLocator FIXING_SERIES_PRICE2 =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/fixings-price2.csv");
  private static final ResourceLocator FIXING_SERIES_PRICE_INVALID =
      ResourceLocator.of("classpath:com/opengamma/strata/loader/csv/fixings-price-invalid.csv");

  //-------------------------------------------------------------------------
  public void test_single_series_single_file() {
    Map<ObservableId, LocalDateDoubleTimeSeries> ts = FixingSeriesCsvLoader.load(
        ImmutableList.of(FIXING_SERIES_1));

    assertEquals(ts.size(), 1);
    assertTrue(ts.containsKey(ID_USD_LIBOR_3M));
    assertLibor3mSeries(ts.get(ID_USD_LIBOR_3M));
  }

  public void test_multiple_series_single_file() {
    Map<ObservableId, LocalDateDoubleTimeSeries> ts = FixingSeriesCsvLoader.load(
        ImmutableList.of(FIXING_SERIES_1_AND_2));
    assertLibor3m6mSeries(ts);
  }

  public void test_multiple_series_multiple_files() {
    Map<ObservableId, LocalDateDoubleTimeSeries> ts = FixingSeriesCsvLoader.load(
        FIXING_SERIES_1, FIXING_SERIES_2);
    assertLibor3m6mSeries(ts);
  }

  public void test_priceIndex1() {
    Map<ObservableId, LocalDateDoubleTimeSeries> ts = FixingSeriesCsvLoader.load(FIXING_SERIES_PRICE1);
    assertEquals(ts.size(), 1);
    assertTrue(ts.containsKey(ID_GB_RPI));
    assertPriceIndexSeries(ts.get(ID_GB_RPI));
  }

  public void test_priceIndex2() {
    Map<ObservableId, LocalDateDoubleTimeSeries> ts = FixingSeriesCsvLoader.load(FIXING_SERIES_PRICE2);
    assertEquals(ts.size(), 1);
    assertTrue(ts.containsKey(ID_GB_RPI));
    assertPriceIndexSeries(ts.get(ID_GB_RPI));
  }

  public void test_priceIndex_invalidDate() {
    assertThatIllegalArgumentException().isThrownBy(() -> FixingSeriesCsvLoader.load(FIXING_SERIES_PRICE_INVALID));
  }

  public void test_single_series_multiple_files() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixingSeriesCsvLoader.load(FIXING_SERIES_1, FIXING_SERIES_1))
        .withMessageStartingWith("Multiple entries with same key: ");
  }

  public void test_invalidDate() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixingSeriesCsvLoader.load(FIXING_SERIES_INVALID_DATE))
        .withMessageStartingWith("Error processing resource as CSV file: ");
  }

  //-------------------------------------------------------------------------
  private void assertLibor3m6mSeries(Map<ObservableId, LocalDateDoubleTimeSeries> ts) {
    assertEquals(ts.size(), 2);
    assertTrue(ts.containsKey(ID_USD_LIBOR_3M));
    assertTrue(ts.containsKey(ID_USD_LIBOR_6M));
    assertLibor3mSeries(ts.get(ID_USD_LIBOR_3M));
    assertLibor6mSeries(ts.get(ID_USD_LIBOR_6M));
  }

  private void assertLibor3mSeries(LocalDateDoubleTimeSeries actualSeries) {
    assertEquals(actualSeries.size(), 3);
    LocalDateDoubleTimeSeries expectedSeries = LocalDateDoubleTimeSeries.builder()
        .put(LocalDate.of(1971, 1, 4), 0.065)
        .put(LocalDate.of(1971, 1, 5), 0.0638)
        .put(LocalDate.of(1971, 1, 6), 0.0638)
        .build();
    assertEquals(actualSeries, expectedSeries);
  }

  private void assertLibor6mSeries(LocalDateDoubleTimeSeries actualSeries) {
    assertEquals(actualSeries.size(), 3);
    LocalDateDoubleTimeSeries expectedSeries = LocalDateDoubleTimeSeries.builder()
        .put(LocalDate.of(1971, 1, 4), 0.0681)
        .put(LocalDate.of(1971, 1, 5), 0.0675)
        .put(LocalDate.of(1971, 1, 6), 0.0669)
        .build();
    assertEquals(actualSeries, expectedSeries);
  }

  private void assertPriceIndexSeries(LocalDateDoubleTimeSeries actualSeries) {
    assertEquals(actualSeries.size(), 3);
    LocalDateDoubleTimeSeries expectedSeries = LocalDateDoubleTimeSeries.builder()
        .put(LocalDate.of(2017, 1, 31), 200)
        .put(LocalDate.of(2017, 2, 28), 300)
        .put(LocalDate.of(2017, 3, 31), 390)
        .build();
    assertEquals(actualSeries, expectedSeries);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(FixingSeriesCsvLoader.class);
  }

}
