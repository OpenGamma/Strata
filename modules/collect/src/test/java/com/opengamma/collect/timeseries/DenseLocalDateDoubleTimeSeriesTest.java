/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.timeseries;

import static com.opengamma.collect.timeseries.DenseLocalDateDoubleTimeSeries.DenseTimeSeriesCalculation.INCLUDE_WEEKENDS;
import static com.opengamma.collect.timeseries.DenseLocalDateDoubleTimeSeries.DenseTimeSeriesCalculation.SKIP_WEEKENDS;
import static com.opengamma.collect.timeseries.DenseLocalDateDoubleTimeSeries.EMPTY_SERIES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.beans.BeanBuilder;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Doubles;
import com.opengamma.collect.Guavate;
import com.opengamma.collect.TestHelper;
import com.opengamma.collect.timeseries.DenseLocalDateDoubleTimeSeries;
import com.opengamma.collect.timeseries.LocalDateDoublePoint;

@Test
public class DenseLocalDateDoubleTimeSeriesTest {

  private static final double TOLERANCE = 1e-5;

  private static final LocalDate DATE_2015_06_01 = date(2015, 6, 1);
  private static final LocalDate DATE_2014_06_01 = date(2014, 6, 1);
  private static final LocalDate DATE_2012_06_01 = date(2012, 6, 1);
  private static final LocalDate DATE_2010_06_01 = date(2010, 6, 1);
  private static final LocalDate DATE_2011_06_01 = date(2011, 6, 1);
  private static final LocalDate DATE_2013_06_01 = date(2013, 6, 1);
  private static final LocalDate DATE_2010_01_01 = date(2010, 1, 1);
  private static final LocalDate DATE_2010_01_02 = date(2010, 1, 2);
  private static final LocalDate DATE_2010_01_03 = date(2010, 1, 3);
  // Avoid weekends for these days
  private static final LocalDate DATE_2011_01_01 = date(2011, 1, 1);
  private static final LocalDate DATE_2012_01_01 = date(2012, 1, 1);
  private static final LocalDate DATE_2013_01_01 = date(2013, 1, 1);
  private static final LocalDate DATE_2014_01_01 = date(2014, 1, 1);
  private static final ImmutableList<LocalDate> DATES_2010_14 = dates(
      DATE_2010_01_01, DATE_2011_01_01, DATE_2012_01_01, DATE_2013_01_01, DATE_2014_01_01);
  private static final ImmutableList<Double> VALUES_10_14 = values(10, 11, 12, 13, 14);
  private static final ImmutableList<LocalDate> DATES_2010_12 = dates(
      DATE_2010_01_01, DATE_2011_01_01, DATE_2012_01_01);
  private static final ImmutableList<Double> VALUES_10_12 = values(10, 11, 12);

  //-------------------------------------------------------------------------
  public void test_emptySeries() {
    DenseLocalDateDoubleTimeSeries test = DenseLocalDateDoubleTimeSeries.EMPTY_SERIES;
    assertEquals(test.isEmpty(), true);
    assertEquals(test.size(), 0);
    assertEquals(test.containsDate(DATE_2010_01_01), false);
    assertEquals(test.containsDate(DATE_2011_01_01), false);
    assertEquals(test.containsDate(DATE_2012_01_01), false);
    assertEquals(test.get(DATE_2010_01_01), OptionalDouble.empty());
    assertEquals(test.get(DATE_2011_01_01), OptionalDouble.empty());
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.empty());
    assertEquals(test, DenseLocalDateDoubleTimeSeries.of(dates(), values()));
    assertEquals(test.dates().size(), 0);
    assertEquals(test.values().size(), 0);
  }

  //-------------------------------------------------------------------------
  public void test_of_singleton() {
    DenseLocalDateDoubleTimeSeries test = DenseLocalDateDoubleTimeSeries.of(DATE_2011_01_01, 2d, INCLUDE_WEEKENDS);
    assertEquals(test.isEmpty(), false);
    assertEquals(test.size(), 1);

    // Check start is not weekend

    assertEquals(test.containsDate(DATE_2010_01_01), false);
    assertEquals(test.containsDate(DATE_2011_01_01), true);
    assertEquals(test.containsDate(DATE_2012_01_01), false);
    assertEquals(test.get(DATE_2010_01_01), OptionalDouble.empty());
    assertEquals(test.get(DATE_2011_01_01), OptionalDouble.of(2d));
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.empty());
    assertEquals(test.dates(), ImmutableList.of(DATE_2011_01_01));
    assertEquals(test.values(), ImmutableList.of(2d));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_singleton_nullDateDisallowed() {
    DenseLocalDateDoubleTimeSeries.of(null, 1d);
  }

  //-------------------------------------------------------------------------
  public void test_of_collectionCollection() {
    Collection<LocalDate> dates = dates(DATE_2011_01_01, DATE_2012_01_01);
    Collection<Double> values = values(2d, 3d);
    DenseLocalDateDoubleTimeSeries test = DenseLocalDateDoubleTimeSeries.of(dates, values, INCLUDE_WEEKENDS);
    assertEquals(test.isEmpty(), false);
    assertEquals(test.size(), 2);
    assertEquals(test.containsDate(DATE_2010_01_01), false);
    assertEquals(test.containsDate(DATE_2011_01_01), true);
    assertEquals(test.containsDate(DATE_2012_01_01), true);
    assertEquals(test.get(DATE_2010_01_01), OptionalDouble.empty());
    assertEquals(test.get(DATE_2011_01_01), OptionalDouble.of(2d));
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.of(3d));
    assertEquals(test.dates(), ImmutableList.of(DATE_2011_01_01, DATE_2012_01_01));
    assertEquals(test.values(), ImmutableList.of(2d, 3d));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_dateCollectionNull() {
    Collection<Double> values = values(2d, 3d);
    DenseLocalDateDoubleTimeSeries.of(null, values);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_valueCollectionNull() {
    Collection<LocalDate> dates = dates(DATE_2011_01_01, DATE_2012_01_01);
    DenseLocalDateDoubleTimeSeries.of(dates, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_dateCollectionWithNull() {
    Collection<LocalDate> dates = Arrays.asList(DATE_2011_01_01, null);
    Collection<Double> values = values(2d, 3d);
    DenseLocalDateDoubleTimeSeries.of(dates, values);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_valueCollectionWithNull() {
    Collection<LocalDate> dates = dates(DATE_2011_01_01, DATE_2012_01_01);
    Collection<Double> values = Arrays.asList(2d, null);
    DenseLocalDateDoubleTimeSeries.of(dates, values);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_collectionsOfDifferentSize() {
    Collection<LocalDate> dates = dates(DATE_2011_01_01);
    Collection<Double> values = values(2d, 3d);
    DenseLocalDateDoubleTimeSeries.of(dates, values);
  }

  public void test_of_collectionCollection_datesUnordered() {
    Collection<LocalDate> dates = dates(DATE_2012_01_01, DATE_2011_01_01);
    Collection<Double> values = values(2d, 1d);
    DenseLocalDateDoubleTimeSeries series = DenseLocalDateDoubleTimeSeries.of(dates, values, INCLUDE_WEEKENDS);
    assertEquals(series.get(DATE_2011_01_01), OptionalDouble.of(1d));
    assertEquals(series.get(DATE_2012_01_01), OptionalDouble.of(2d));
  }

  //-------------------------------------------------------------------------
  public void test_of_map() {
    Map<LocalDate, Double> map = new HashMap<>();
    map.put(DATE_2011_01_01, 2d);
    map.put(DATE_2012_01_01, 3d);
    DenseLocalDateDoubleTimeSeries test = DenseLocalDateDoubleTimeSeries.of(map, INCLUDE_WEEKENDS);
    assertEquals(test.isEmpty(), false);
    assertEquals(test.size(), 2);
    assertEquals(test.containsDate(DATE_2010_01_01), false);
    assertEquals(test.containsDate(DATE_2011_01_01), true);
    assertEquals(test.containsDate(DATE_2012_01_01), true);
    assertEquals(test.get(DATE_2010_01_01), OptionalDouble.empty());
    assertEquals(test.get(DATE_2011_01_01), OptionalDouble.of(2d));
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.of(3d));
    assertEquals(test.dates(), ImmutableList.of(DATE_2011_01_01, DATE_2012_01_01));
    assertEquals(test.values(), ImmutableList.of(2d, 3d));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_map_null() {
    DenseLocalDateDoubleTimeSeries.of((Map<LocalDate, Double>) null);
  }

  public void test_of_map_empty() {
    DenseLocalDateDoubleTimeSeries series = DenseLocalDateDoubleTimeSeries.of(ImmutableMap.of());
    assertEquals(series, EMPTY_SERIES);
  }


  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_map_dateNull() {
    Map<LocalDate, Double> map = new HashMap<>();
    map.put(DATE_2011_01_01, 2d);
    map.put(null, 3d);
    DenseLocalDateDoubleTimeSeries.of(map);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_map_valueNull() {
    Map<LocalDate, Double> map = new HashMap<>();
    map.put(DATE_2011_01_01, 2d);
    map.put(DATE_2012_01_01, null);
    DenseLocalDateDoubleTimeSeries.of(map);
  }

  //-------------------------------------------------------------------------
  public void test_of_collection() {
    Collection<LocalDateDoublePoint> points = Arrays.asList(
        LocalDateDoublePoint.of(DATE_2011_01_01, 2d),
        LocalDateDoublePoint.of(DATE_2012_01_01, 3d));
    DenseLocalDateDoubleTimeSeries test = DenseLocalDateDoubleTimeSeries.of(points, INCLUDE_WEEKENDS);
    assertEquals(test.isEmpty(), false);
    assertEquals(test.size(), 2);
    assertEquals(test.containsDate(DATE_2010_01_01), false);
    assertEquals(test.containsDate(DATE_2011_01_01), true);
    assertEquals(test.containsDate(DATE_2012_01_01), true);
    assertEquals(test.get(DATE_2010_01_01), OptionalDouble.empty());
    assertEquals(test.get(DATE_2011_01_01), OptionalDouble.of(2d));
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.of(3d));
    assertEquals(test.dates(), ImmutableList.of(DATE_2011_01_01, DATE_2012_01_01));
    assertEquals(test.values(), ImmutableList.of(2d, 3d));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collection_collectionNull() {
    DenseLocalDateDoubleTimeSeries.of((Collection<LocalDateDoublePoint>) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collection_collectionWithNull() {
    Collection<LocalDateDoublePoint> points = Arrays.asList(
        LocalDateDoublePoint.of(DATE_2011_01_01, 2d), null);
    DenseLocalDateDoubleTimeSeries.of(points);
  }

  public void test_of_collection_empty() {
    Collection<LocalDateDoublePoint> points = ImmutableList.of();
    assertEquals(DenseLocalDateDoubleTimeSeries.of(points), EMPTY_SERIES);
  }

  //-------------------------------------------------------------------------
  public void test_immutableViaBeanBuilder() {
    LocalDate startDate = DATE_2010_01_01;
    double[] values = {6, 5, 4};
    BeanBuilder<? extends DenseLocalDateDoubleTimeSeries> builder = DenseLocalDateDoubleTimeSeries.meta().builder();
    builder.set("startDate", startDate);
    builder.set("points", values);
    builder.set("dateCalculation", INCLUDE_WEEKENDS);
    DenseLocalDateDoubleTimeSeries test = builder.build();
    values[0] = -1;

    LocalDateDoublePoint[] points = test.stream().toArray(LocalDateDoublePoint[]::new);
    assertEquals(points[0], LocalDateDoublePoint.of(DATE_2010_01_01, 6d));
    assertEquals(points[1], LocalDateDoublePoint.of(DATE_2010_01_02, 5d));
    assertEquals(points[2], LocalDateDoublePoint.of(DATE_2010_01_03, 4d));
  }

  public void test_immutableValuesViaBeanGet() {
    DenseLocalDateDoubleTimeSeries test =
        DenseLocalDateDoubleTimeSeries.of(DATES_2010_12, VALUES_10_12, INCLUDE_WEEKENDS);
    double[] array = (double[]) test.property("points").get();
    array[0] = -1;
    LocalDateDoublePoint[] points = test.stream().toArray(LocalDateDoublePoint[]::new);
    assertEquals(points[0], LocalDateDoublePoint.of(DATE_2010_01_01, 10d));
    assertEquals(points[1], LocalDateDoublePoint.of(DATE_2011_01_01, 11d));
    assertEquals(points[2], LocalDateDoublePoint.of(DATE_2012_01_01, 12d));
  }

  //-------------------------------------------------------------------------
  public void test_earliestLatest() {
    DenseLocalDateDoubleTimeSeries test = DenseLocalDateDoubleTimeSeries.of(DATES_2010_12, VALUES_10_12, INCLUDE_WEEKENDS);
    assertEquals(test.getEarliestDate(), DATE_2010_01_01);
    assertEquals(test.getEarliestValue(), 10d, TOLERANCE);
    assertEquals(test.getLatestDate(), DATE_2012_01_01);
    assertEquals(test.getLatestValue(), 12d, TOLERANCE);
  }

  public void test_earliestLatest_whenEmpty() {
    DenseLocalDateDoubleTimeSeries test = EMPTY_SERIES;
    TestHelper.assertThrows(test::getEarliestDate, NoSuchElementException.class);
    TestHelper.assertThrows(test::getEarliestValue, NoSuchElementException.class);
    TestHelper.assertThrows(test::getLatestDate, NoSuchElementException.class);
    TestHelper.assertThrows(test::getLatestValue, NoSuchElementException.class);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "subSeries")
  Object[][] data_subSeries() {
    return new Object[][] {
        // start = end -> empty
        {DATE_2011_01_01, DATE_2011_01_01, new int[] {}},
        // no overlap
        {date(2006, 1, 1), date(2009, 1, 1), new int[] {}},
        // single point
        {DATE_2011_01_01, date(2011, 1, 2), new int[] {1}},
        // include when start matches base, exclude when end matches base
        {DATE_2011_01_01, DATE_2013_01_01, new int[] {1, 2}},
        // include when start matches base
        {DATE_2011_01_01, date(2013, 1, 2), new int[] {1, 2, 3}},
        // neither start nor end match
        {date(2010, 12, 31), date(2013, 1, 2), new int[] {1, 2, 3}},
        // start date just after a base date
        {date(2011, 1, 2), date(2013, 1, 2), new int[] {2, 3}},
    };
  }

  @Test(dataProvider = "subSeries")
  public void test_subSeries(LocalDate start, LocalDate end, int[] expected) {
    DenseLocalDateDoubleTimeSeries base =
        DenseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14, INCLUDE_WEEKENDS);
    DenseLocalDateDoubleTimeSeries test = base.subSeries(start, end);
    assertEquals(test.size(), expected.length);
    for (int i = 0; i < DATES_2010_14.size(); i++) {
      if (Arrays.binarySearch(expected, i) >= 0) {
        assertEquals(test.get(DATES_2010_14.get(i)), OptionalDouble.of(VALUES_10_14.get(i)));
      } else {
        assertEquals(test.get(DATES_2010_14.get(i)), OptionalDouble.empty());
      }
    }
  }

  @Test(dataProvider = "subSeries")
  public void test_subSeries_emptySeries(LocalDate start, LocalDate end, int[] expected) {
    DenseLocalDateDoubleTimeSeries test = EMPTY_SERIES.subSeries(start, end);
    assertEquals(test.size(), 0);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_subSeries_startAfterEnd() {
    DenseLocalDateDoubleTimeSeries base =
        DenseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14, INCLUDE_WEEKENDS);
    base.subSeries(date(2011, 1, 2), DATE_2011_01_01);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "headSeries")
  Object[][] data_headSeries() {
    return new Object[][] {
        {0, new int[] {}},
        {1, new int[] {0}},
        {2, new int[] {0, 1}},
        {3, new int[] {0, 1, 2}},
        {4, new int[] {0, 1, 2, 3}},
        {5, new int[] {0, 1, 2, 3, 4}},
        {6, new int[] {0, 1, 2, 3, 4}},
    };
  }

  @Test(dataProvider = "headSeries")
  public void test_headSeries(int count, int[] expected) {

    DenseLocalDateDoubleTimeSeries base =
        DenseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14, INCLUDE_WEEKENDS);
    DenseLocalDateDoubleTimeSeries test = base.headSeries(count);
    assertEquals(test.size(), expected.length);
    for (int i = 0; i < DATES_2010_14.size(); i++) {
      if (Arrays.binarySearch(expected, i) >= 0) {
        assertEquals(test.get(DATES_2010_14.get(i)), OptionalDouble.of(VALUES_10_14.get(i)));
      } else {
        assertEquals(test.get(DATES_2010_14.get(i)), OptionalDouble.empty());
      }
    }
  }

  @Test(dataProvider = "headSeries")
  public void test_headSeries_emptySeries(int count, int[] expected) {
    DenseLocalDateDoubleTimeSeries test = EMPTY_SERIES.headSeries(count);
    assertEquals(test.size(), 0);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_headSeries_negative() {
    DenseLocalDateDoubleTimeSeries base = DenseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14);
    base.headSeries(-1);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "tailSeries")
  Object[][] data_tailSeries() {
    return new Object[][] {
        {0, new int[] {}},
        {1, new int[] {4}},
        {2, new int[] {3, 4}},
        {3, new int[] {2, 3, 4}},
        {4, new int[] {1, 2, 3, 4}},
        {5, new int[] {0, 1, 2, 3, 4}},
        {6, new int[] {0, 1, 2, 3, 4}},
    };
  }

  @Test(dataProvider = "tailSeries")
  public void test_tailSeries(int count, int[] expected) {
    DenseLocalDateDoubleTimeSeries base =
        DenseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14, INCLUDE_WEEKENDS);
    DenseLocalDateDoubleTimeSeries test = base.tailSeries(count);
    assertEquals(test.size(), expected.length);
    for (int i = 0; i < DATES_2010_14.size(); i++) {
      if (Arrays.binarySearch(expected, i) >= 0) {
        assertEquals(test.get(DATES_2010_14.get(i)), OptionalDouble.of(VALUES_10_14.get(i)));
      } else {
        assertEquals(test.get(DATES_2010_14.get(i)), OptionalDouble.empty());
      }
    }
  }

  @Test(dataProvider = "tailSeries")
  public void test_tailSeries_emptySeries(int count, int[] expected) {
    DenseLocalDateDoubleTimeSeries test = EMPTY_SERIES.tailSeries(count);
    assertEquals(test.size(), 0);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_tailSeries_negative() {
    DenseLocalDateDoubleTimeSeries base = DenseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14);
    base.tailSeries(-1);
  }

  //-------------------------------------------------------------------------
  public void test_stream() {
    DenseLocalDateDoubleTimeSeries base =
        DenseLocalDateDoubleTimeSeries.of(DATES_2010_12, VALUES_10_12, INCLUDE_WEEKENDS);
    Object[] test = base.stream().toArray();
    assertEquals(test[0], LocalDateDoublePoint.of(DATE_2010_01_01, 10));
    assertEquals(test[1], LocalDateDoublePoint.of(DATE_2011_01_01, 11));
    assertEquals(test[2], LocalDateDoublePoint.of(DATE_2012_01_01, 12));
  }

  public void test_stream_withCollector() {
    DenseLocalDateDoubleTimeSeries base =
        DenseLocalDateDoubleTimeSeries.of(DATES_2010_12, VALUES_10_12, INCLUDE_WEEKENDS);
    LocalDateDoubleTimeSeries test = base.stream()
        .map(point -> point.withValue(1.5d))
        .collect(LocalDateDoubleTimeSeries.collector());
    assertEquals(test.size(), 3);
    assertEquals(test.get(DATE_2010_01_01), OptionalDouble.of(1.5));
    assertEquals(test.get(DATE_2011_01_01), OptionalDouble.of(1.5));
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.of(1.5));
  }

  //-------------------------------------------------------------------------
  public void test_dateStream() {
    DenseLocalDateDoubleTimeSeries base = DenseLocalDateDoubleTimeSeries.of(DATES_2010_12, VALUES_10_12, INCLUDE_WEEKENDS);
    LocalDate[] test = base.dateStream().toArray(LocalDate[]::new);
    assertEquals(test[0], DATE_2010_01_01);
    assertEquals(test[1], DATE_2011_01_01);
    assertEquals(test[2], DATE_2012_01_01);
  }

  public void test_valueStream() {
    DenseLocalDateDoubleTimeSeries base =
        DenseLocalDateDoubleTimeSeries.of(DATES_2010_12, VALUES_10_12, INCLUDE_WEEKENDS);
    double[] test = base.valueStream().toArray();
    assertEquals(test[0], 10, TOLERANCE);
    assertEquals(test[1], 11, TOLERANCE);
    assertEquals(test[2], 12, TOLERANCE);
  }

  //-------------------------------------------------------------------------
  public void test_forEach() {
    DenseLocalDateDoubleTimeSeries base = DenseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14, INCLUDE_WEEKENDS);
    AtomicInteger counter = new AtomicInteger();
    base.forEach((date, value) -> counter.addAndGet((int) value));
    assertEquals(counter.get(), 10 + 11 + 12 + 13 + 14);
  }

  //-------------------------------------------------------------------------
  public void test_combineWith_intersectionWithNoMatchingElements() {
    DenseLocalDateDoubleTimeSeries series1 =
        DenseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14, INCLUDE_WEEKENDS);

    List<LocalDate> dates2 = dates(DATE_2010_06_01, DATE_2011_06_01, DATE_2012_06_01, DATE_2013_06_01, DATE_2014_06_01);
    DenseLocalDateDoubleTimeSeries series2 =
        DenseLocalDateDoubleTimeSeries.of(dates2, VALUES_10_14, INCLUDE_WEEKENDS);

    LocalDateDoubleTimeSeries test = series1.combineWith(series2, (l, r) -> l + r);
    assertEquals(test, SparseLocalDateDoubleTimeSeries.EMPTY_SERIES);
  }

  public void test_combineWith_intersectionWithSomeMatchingElements() {
    DenseLocalDateDoubleTimeSeries series1 =
        DenseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14, INCLUDE_WEEKENDS);

    List<LocalDate> dates2 = dates(DATE_2010_01_01, DATE_2011_06_01, DATE_2012_01_01, DATE_2013_06_01, DATE_2014_01_01);
    List<Double> values2 = values(1.0, 1.1, 1.2, 1.3, 1.4);
    DenseLocalDateDoubleTimeSeries series2 =
        DenseLocalDateDoubleTimeSeries.of(dates2, values2, INCLUDE_WEEKENDS);

    LocalDateDoubleTimeSeries test = series1.combineWith(series2, (l, r) -> l + r);
    assertEquals(test.size(), 3);
    assertEquals(test.get(DATE_2010_01_01), OptionalDouble.of(11.0));
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.of(13.2));
    assertEquals(test.get(DATE_2014_01_01), OptionalDouble.of(15.4));
  }

  public void test_combineWith_intersectionWithSomeMatchingElements2() {
    List<LocalDate> dates1 = dates(DATE_2010_01_01, DATE_2011_01_01, DATE_2012_01_01, DATE_2014_01_01, DATE_2015_06_01);
    List<Double> values1 = values(10, 11, 12, 13, 14);
    DenseLocalDateDoubleTimeSeries series1 = DenseLocalDateDoubleTimeSeries.of(dates1, values1, INCLUDE_WEEKENDS);

    List<LocalDate> dates2 = dates(DATE_2010_01_01, DATE_2011_06_01, DATE_2012_01_01, DATE_2013_01_01, DATE_2014_01_01);
    List<Double> values2 = values(1.0, 1.1, 1.2, 1.3, 1.4);
    DenseLocalDateDoubleTimeSeries series2 = DenseLocalDateDoubleTimeSeries.of(dates2, values2, INCLUDE_WEEKENDS);

    LocalDateDoubleTimeSeries test = series1.combineWith(series2, (l, r) -> l + r);
    assertEquals(test.size(), 3);
    assertEquals(test.get(DATE_2010_01_01), OptionalDouble.of(11.0));
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.of(13.2));
    assertEquals(test.get(DATE_2014_01_01), OptionalDouble.of(14.4));
  }

  public void test_combineWith_intersectionWithAllMatchingElements() {
    List<LocalDate> dates1 = DATES_2010_14;
    List<Double> values1 = values(10, 11, 12, 13, 14);
    DenseLocalDateDoubleTimeSeries series1 =
        DenseLocalDateDoubleTimeSeries.of(dates1, values1, INCLUDE_WEEKENDS);
    List<LocalDate> dates2 = DATES_2010_14;
    List<Double> values2 = values(1.0, 1.1, 1.2, 1.3, 1.4);
    DenseLocalDateDoubleTimeSeries series2 =
        DenseLocalDateDoubleTimeSeries.of(dates2, values2, INCLUDE_WEEKENDS);

    LocalDateDoubleTimeSeries combined = series1.combineWith(series2, (l, r) -> l + r);
    assertEquals(combined.size(), 5);
    assertEquals(combined.getEarliestDate(), DATE_2010_01_01);
    assertEquals(combined.getLatestDate(), DATE_2014_01_01);
    assertEquals(combined.get(DATE_2010_01_01), OptionalDouble.of(11.0));
    assertEquals(combined.get(DATE_2011_01_01), OptionalDouble.of(12.1));
    assertEquals(combined.get(DATE_2012_01_01), OptionalDouble.of(13.2));
    assertEquals(combined.get(DATE_2013_01_01), OptionalDouble.of(14.3));
    assertEquals(combined.get(DATE_2014_01_01), OptionalDouble.of(15.4));
  }

  //-------------------------------------------------------------------------
  public void test_mapValues_addConstantToSeries() {
    DenseLocalDateDoubleTimeSeries base = DenseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14, INCLUDE_WEEKENDS);
    DenseLocalDateDoubleTimeSeries test = base.mapValues(d -> d + 5);
    List<Double> expectedValues = values(15, 16, 17, 18, 19);
    assertEquals(test, DenseLocalDateDoubleTimeSeries.of(DATES_2010_14, expectedValues, INCLUDE_WEEKENDS));
  }

  public void test_mapValues_multiplySeries() {
    DenseLocalDateDoubleTimeSeries base =
        DenseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14, INCLUDE_WEEKENDS);

    DenseLocalDateDoubleTimeSeries test = base.mapValues(d -> d * 5);
    List<Double> expectedValues = values(50, 55, 60, 65, 70);
    assertEquals(test, DenseLocalDateDoubleTimeSeries.of(DATES_2010_14, expectedValues, INCLUDE_WEEKENDS));
  }

  public void test_mapValues_invertSeries() {
    List<Double> values = values(1, 2, 4, 5, 8);
    DenseLocalDateDoubleTimeSeries base =
        DenseLocalDateDoubleTimeSeries.of(DATES_2010_14, values, INCLUDE_WEEKENDS);
    DenseLocalDateDoubleTimeSeries test = base.mapValues(d -> 1 / d);
    List<Double> expectedValues = values(1, 0.5, 0.25, 0.2, 0.125);
    assertEquals(test, DenseLocalDateDoubleTimeSeries.of(DATES_2010_14, expectedValues, INCLUDE_WEEKENDS));
  }

  //-------------------------------------------------------------------------
  public void test_filter_byDate() {
    List<LocalDate> dates = dates(DATE_2010_01_01, DATE_2011_06_01, DATE_2012_01_01, DATE_2013_06_01, DATE_2014_01_01);
    DenseLocalDateDoubleTimeSeries base = DenseLocalDateDoubleTimeSeries.of(dates, VALUES_10_14, INCLUDE_WEEKENDS);
    DenseLocalDateDoubleTimeSeries test = base.filter((ld, v) -> ld.getMonthValue() != 6);
    assertEquals(test.size(), 3);
    assertEquals(test.get(DATE_2010_01_01), OptionalDouble.of(10d));
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.of(12d));
    assertEquals(test.get(DATE_2014_01_01), OptionalDouble.of(14d));
  }

  public void test_filter_byValue() {
    DenseLocalDateDoubleTimeSeries base = DenseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14, INCLUDE_WEEKENDS);
    DenseLocalDateDoubleTimeSeries test = base.filter((ld, v) -> v % 2 == 1);
    assertEquals(test.size(), 2);
    assertEquals(test.get(DATE_2011_01_01), OptionalDouble.of(11d));
    assertEquals(test.get(DATE_2013_01_01), OptionalDouble.of(13d));
  }

  public void test_filter_byDateAndValue() {
    List<LocalDate> dates = dates(DATE_2010_01_01, DATE_2011_06_01, DATE_2012_01_01, DATE_2013_06_01, DATE_2014_01_01);
    DenseLocalDateDoubleTimeSeries series = DenseLocalDateDoubleTimeSeries.of(dates, VALUES_10_14, INCLUDE_WEEKENDS);

    DenseLocalDateDoubleTimeSeries test = series.filter((ld, v) -> ld.getYear() >= 2012 && v % 2 == 0);
    assertEquals(test.size(), 2);
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.of(12d));
    assertEquals(test.get(DATE_2014_01_01), OptionalDouble.of(14d));
  }

  //-------------------------------------------------------------------------
  public void test_toMap() {
    DenseLocalDateDoubleTimeSeries base =
        DenseLocalDateDoubleTimeSeries.of(DATES_2010_12, VALUES_10_12, INCLUDE_WEEKENDS);
    ImmutableMap<LocalDate, Double> test = base.toMap();
    assertEquals(test.size(), 3);
    assertEquals(test.get(DATE_2010_01_01), 10d);
    assertEquals(test.get(DATE_2011_01_01), 11d);
    assertEquals(test.get(DATE_2012_01_01), 12d);
  }

  //-------------------------------------------------------------------------
  public void test_equals_similarSeriesAreEqual() {
    DenseLocalDateDoubleTimeSeries series1 = DenseLocalDateDoubleTimeSeries.of(DATE_2014_01_01, 1d);
    DenseLocalDateDoubleTimeSeries series2 = DenseLocalDateDoubleTimeSeries.of(dates(DATE_2014_01_01), values(1d));
    assertEquals(series1.size(), 1);
    assertEquals(series1, series2);
    assertEquals(series1, series1);
    assertEquals(series1.hashCode(), series1.hashCode());
  }

  public void test_equals_notEqual() {
    DenseLocalDateDoubleTimeSeries series1 = DenseLocalDateDoubleTimeSeries.of(DATE_2014_01_01, 1d);
    DenseLocalDateDoubleTimeSeries series2 = DenseLocalDateDoubleTimeSeries.of(DATE_2013_06_01, 1d);
    DenseLocalDateDoubleTimeSeries series3 = DenseLocalDateDoubleTimeSeries.of(DATE_2014_01_01, 3d);
    assertNotEquals(series1, series2);
    assertNotEquals(series1, series3);
  }

  public void test_equals_bad() {
    DenseLocalDateDoubleTimeSeries test = DenseLocalDateDoubleTimeSeries.of(DATE_2014_01_01, 1d);
    assertEquals(test.equals(""), false);
    assertEquals(test.equals(null), false);
  }

  public void checkOffsetsIncludeWeekends() {

    Map<LocalDate, Double> map = ImmutableMap.<LocalDate, Double>builder()
        .put(dt(2014, 12, 26), 14d)
            // Weekend
        .put(dt(2014, 12, 29), 13d)
        .put(dt(2014, 12, 30), 12d)
        .put(dt(2014, 12, 31), 11d)
            // 1st is bank hol so no data
        .put(dt(2015, 1, 2), 11d)
            // Weekend, so no data
        .put(dt(2015, 1, 5), 12d)
        .put(dt(2015, 1, 6), 13d)
        .put(dt(2015, 1, 7), 14d)
        .build();

    DenseLocalDateDoubleTimeSeries ts = DenseLocalDateDoubleTimeSeries.of(map, INCLUDE_WEEKENDS);
    assertThat(ts.get(dt(2014, 12, 26))).isEqualTo(OptionalDouble.of(14d));
    assertThat(ts.get(dt(2014, 12, 27))).isEqualTo(OptionalDouble.empty());
    assertThat(ts.get(dt(2014, 12, 28))).isEqualTo(OptionalDouble.empty());
    assertThat(ts.get(dt(2014, 12, 29))).isEqualTo(OptionalDouble.of(13d));
    assertThat(ts.get(dt(2014, 12, 30))).isEqualTo(OptionalDouble.of(12d));
    assertThat(ts.get(dt(2014, 12, 31))).isEqualTo(OptionalDouble.of(11d));
    assertThat(ts.get(dt(2015, 1, 1))).isEqualTo(OptionalDouble.empty());
    assertThat(ts.get(dt(2015, 1, 2))).isEqualTo(OptionalDouble.of(11d));
    assertThat(ts.get(dt(2015, 1, 3))).isEqualTo(OptionalDouble.empty());
    assertThat(ts.get(dt(2015, 1, 4))).isEqualTo(OptionalDouble.empty());
    assertThat(ts.get(dt(2015, 1, 5))).isEqualTo(OptionalDouble.of(12d));
    assertThat(ts.get(dt(2015, 1, 6))).isEqualTo(OptionalDouble.of(13d));
    assertThat(ts.get(dt(2015, 1, 7))).isEqualTo(OptionalDouble.of(14d));
  }

  //-------------------------------------------------------------------------
  public void test_coverage() {
    TestHelper.coverImmutableBean(DenseLocalDateDoubleTimeSeries.of(DATE_2014_01_01, 1d));
  }

  //-------------------------------------------------------------------------
  private static LocalDate date(int year, int month, int day) {
    return LocalDate.of(year, month, day);
  }

  private static ImmutableList<LocalDate> dates(LocalDate... dates) {
    return ImmutableList.copyOf(dates);
  }

  
  private static ImmutableList<Double> values(double... values) {
    return ImmutableList.copyOf(Doubles.asList(values));
  }

  public void checkOffsetsSkipWeekends() {

    Map<LocalDate, Double> map = ImmutableMap.<LocalDate, Double>builder()
        .put(dt(2014, 12, 26), 14d)
            // Weekend
        .put(dt(2014, 12, 29), 13d)
        .put(dt(2014, 12, 30), 12d)
        .put(dt(2014, 12, 31), 11d)
        .put(dt(2015, 1, 1), Double.NaN)
        .put(dt(2015, 1, 2), 11d)
            // Weekend, so no data
        .put(dt(2015, 1, 5), 12d)
        .put(dt(2015, 1, 6), 13d)
        .put(dt(2015, 1, 7), 14d)
        .build();

    DenseLocalDateDoubleTimeSeries ts = DenseLocalDateDoubleTimeSeries.of(map, SKIP_WEEKENDS);
    assertThat(ts.get(dt(2014, 12, 26))).isEqualTo(OptionalDouble.of(14d));
    assertThat(ts.get(dt(2014, 12, 29))).isEqualTo(OptionalDouble.of(13d));
    assertThat(ts.get(dt(2014, 12, 30))).isEqualTo(OptionalDouble.of(12d));
    assertThat(ts.get(dt(2014, 12, 31))).isEqualTo(OptionalDouble.of(11d));
    assertThat(ts.get(dt(2015, 1, 1))).isEqualTo(OptionalDouble.empty());
    assertThat(ts.get(dt(2015, 1, 2))).isEqualTo(OptionalDouble.of(11d));
    assertThat(ts.get(dt(2015, 1, 3))).isEqualTo(OptionalDouble.empty());
    assertThat(ts.get(dt(2015, 1, 4))).isEqualTo(OptionalDouble.empty());
    assertThat(ts.get(dt(2015, 1, 5))).isEqualTo(OptionalDouble.of(12d));
    assertThat(ts.get(dt(2015, 1, 6))).isEqualTo(OptionalDouble.of(13d));
    assertThat(ts.get(dt(2015, 1, 7))).isEqualTo(OptionalDouble.of(14d));
  }

  public void underOneWeekNoWeekend() {

    Map<LocalDate, Double> map = ImmutableMap.<LocalDate, Double>builder()
        .put(dt(2015, 1, 5), 12d) // Monday
        .put(dt(2015, 1, 6), 13d)
        .put(dt(2015, 1, 7), 14d)
        .put(dt(2015, 1, 8), 15d)
        .put(dt(2015, 1, 9), 16d) // Friday
        .build();

    DenseLocalDateDoubleTimeSeries ts = DenseLocalDateDoubleTimeSeries.of(map, SKIP_WEEKENDS);
    assertThat(ts.get(dt(2015, 1, 5))).isEqualTo(OptionalDouble.of(12d));
    assertThat(ts.get(dt(2015, 1, 9))).isEqualTo(OptionalDouble.of(16d));
  }

  public void underOneWeekWithWeekend() {

    Map<LocalDate, Double> map = ImmutableMap.<LocalDate, Double>builder()
        .put(dt(2015, 1, 1), 10d) // Thursday
        .put(dt(2015, 1, 2), 11d) // Friday
        .put(dt(2015, 1, 5), 12d)
        .put(dt(2015, 1, 6), 13d)
        .put(dt(2015, 1, 7), 14d)
        .put(dt(2015, 1, 8), 15d) // Thursday
        .put(dt(2015, 1, 9), 16d) // Friday
        .build();

    DenseLocalDateDoubleTimeSeries ts = DenseLocalDateDoubleTimeSeries.of(map, SKIP_WEEKENDS);
    assertThat(ts.get(dt(2015, 1, 1))).isEqualTo(OptionalDouble.of(10d));
    assertThat(ts.get(dt(2015, 1, 2))).isEqualTo(OptionalDouble.of(11d));
    assertThat(ts.get(dt(2015, 1, 5))).isEqualTo(OptionalDouble.of(12d));
    assertThat(ts.get(dt(2015, 1, 6))).isEqualTo(OptionalDouble.of(13d));
    assertThat(ts.get(dt(2015, 1, 7))).isEqualTo(OptionalDouble.of(14d));
    assertThat(ts.get(dt(2015, 1, 8))).isEqualTo(OptionalDouble.of(15d));
    assertThat(ts.get(dt(2015, 1, 9))).isEqualTo(OptionalDouble.of(16d));
  }

  public void roundTrip() {
    Map<LocalDate, Double> in = ImmutableMap.<LocalDate, Double>builder()
        .put(dt(2015, 1, 1), 10d) // Thursday
        .put(dt(2015, 1, 2), 11d) // Friday
        .put(dt(2015, 1, 5), 12d)
        .put(dt(2015, 1, 6), 13d)
        .put(dt(2015, 1, 7), 14d)
        .put(dt(2015, 1, 8), 15d) // Thursday
        .put(dt(2015, 1, 9), 16d) // Friday
        .build();

    DenseLocalDateDoubleTimeSeries ts = DenseLocalDateDoubleTimeSeries.of(in, SKIP_WEEKENDS);

    Map<LocalDate, Double> out = ts.stream()
        .collect(Guavate.toImmutableMap(LocalDateDoublePoint::getDate, LocalDateDoublePoint::getValue));
    assertThat(out).isEqualTo(in);
  }

  private LocalDate dt(int yr, int mth, int day) {
    return LocalDate.of(yr, mth, day);
  }
}