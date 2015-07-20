/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.marketdata.timeseries;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.market.id.IndexRateId;

/**
 * Test {@link FixingSeriesCsvLoader}.
 */
@Test
public class FixingSeriesCsvLoaderTest {

  private static final String FIXING_SERIES_1 = "classpath:test-marketdata-complete/historical-fixings/fixings-1.csv";
  private static final String FIXING_SERIES_2 = "classpath:test-marketdata-complete/historical-fixings/fixings-2.csv";
  private static final String FIXING_SERIES_1_AND_2 = "classpath:test-marketdata-additional/historical-fixings/fixings-1-and-2.csv";

  public void test_single_series_single_file() {
    Map<ObservableId, LocalDateDoubleTimeSeries> ts = FixingSeriesCsvLoader.loadFixingSeries(
        ImmutableList.of(ResourceLocator.of(FIXING_SERIES_1)));

    assertEquals(ts.size(), 1);

    ObservableId libor3mId = IndexRateId.of(IborIndices.USD_LIBOR_3M);
    assertTrue(ts.containsKey(libor3mId));

    LocalDateDoubleTimeSeries libor3mSeries = ts.get(libor3mId);
    assertLibor3mSeries(libor3mSeries);
  }

  @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "Multiple entries with same key: .*")
  public void test_single_series_multiple_files() {
    FixingSeriesCsvLoader.loadFixingSeries(
        ImmutableList.of(ResourceLocator.of(FIXING_SERIES_1), ResourceLocator.of(FIXING_SERIES_1)));
  }

  public void test_multiple_series_single_file() {
    Map<ObservableId, LocalDateDoubleTimeSeries> ts = FixingSeriesCsvLoader.loadFixingSeries(
        ImmutableList.of(ResourceLocator.of(FIXING_SERIES_1_AND_2)));

    assertLibor3m6mSeries(ts);
  }

  public void test_multiple_series_multiple_files() {
    Map<ObservableId, LocalDateDoubleTimeSeries> ts = FixingSeriesCsvLoader.loadFixingSeries(
        ImmutableList.of(ResourceLocator.of(FIXING_SERIES_1), ResourceLocator.of(FIXING_SERIES_2)));

    assertLibor3m6mSeries(ts);
  }

  //-------------------------------------------------------------------------
  private void assertLibor3m6mSeries(Map<ObservableId, LocalDateDoubleTimeSeries> ts) {
    assertEquals(ts.size(), 2);

    ObservableId libor3mId = IndexRateId.of(IborIndices.USD_LIBOR_3M);
    ObservableId libor6mId = IndexRateId.of(IborIndices.USD_LIBOR_6M);
    assertTrue(ts.containsKey(libor3mId));
    assertTrue(ts.containsKey(libor6mId));

    LocalDateDoubleTimeSeries libor3mSeries = ts.get(libor3mId);
    assertLibor3mSeries(libor3mSeries);

    LocalDateDoubleTimeSeries libor6mSeries = ts.get(libor6mId);
    assertLibor6mSeries(libor6mSeries);
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

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(FixingSeriesCsvLoader.class);
  }

}
