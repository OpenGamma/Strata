/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.collect.timeseries;

import static com.opengamma.collect.timeseries.SparseLocalDateDoubleTimeSeries.EMPTY_SERIES;
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
import com.opengamma.collect.TestHelper;

/**
 * Test LocalDateDoubleTimeSeries.
 */
@Test
public class SparseLocalDateDoubleTimeSeriesTest {

  private static final LocalDate DATE_2015_06_01 = date(2015, 6, 1);
  private static final LocalDate DATE_2014_06_01 = date(2014, 6, 1);
  private static final LocalDate DATE_2012_06_01 = date(2012, 6, 1);
  private static final LocalDate DATE_2010_06_01 = date(2010, 6, 1);
  private static final LocalDate DATE_2011_06_01 = date(2011, 6, 1);
  private static final LocalDate DATE_2013_06_01 = date(2013, 6, 1);
  private static final LocalDate DATE_2010_01_01 = date(2010, 1, 1);
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
  private static final double TOLERANCE = 0.00001d;

  //-------------------------------------------------------------------------
  public void test_emptySeries() {
    LocalDateDoubleTimeSeries test = EMPTY_SERIES;
    assertEquals(test.isEmpty(), true);
    assertEquals(test.size(), 0);
    assertEquals(test.containsDate(DATE_2010_01_01), false);
    assertEquals(test.containsDate(DATE_2011_01_01), false);
    assertEquals(test.containsDate(DATE_2012_01_01), false);
    assertEquals(test.get(DATE_2010_01_01), OptionalDouble.empty());
    assertEquals(test.get(DATE_2011_01_01), OptionalDouble.empty());
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.empty());
    assertEquals(test, SparseLocalDateDoubleTimeSeries.of(dates(), values()));
    assertEquals(test.dates().size(), 0);
    assertEquals(test.values().size(), 0);
  }

  //-------------------------------------------------------------------------
  public void test_of_singleton() {
    LocalDateDoubleTimeSeries test = SparseLocalDateDoubleTimeSeries.of(DATE_2011_01_01, 2d);
    assertEquals(test.isEmpty(), false);
    assertEquals(test.size(), 1);
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
    SparseLocalDateDoubleTimeSeries.of(null, 1d);
  }

  //-------------------------------------------------------------------------
  public void test_of_collectionCollection() {
    Collection<LocalDate> dates = dates(DATE_2011_01_01, DATE_2012_01_01);
    Collection<Double> values = values(2d, 3d);
    LocalDateDoubleTimeSeries test = SparseLocalDateDoubleTimeSeries.of(dates, values);
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
    SparseLocalDateDoubleTimeSeries.of((Collection<LocalDate>) null, values);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_valueCollectionNull() {
    Collection<LocalDate> dates = dates(DATE_2011_01_01, DATE_2012_01_01);
    SparseLocalDateDoubleTimeSeries.of(dates, (Collection<Double>) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_dateCollectionWithNull() {
    Collection<LocalDate> dates = Arrays.asList(DATE_2011_01_01, null);
    Collection<Double> values = values(2d, 3d);
    SparseLocalDateDoubleTimeSeries.of(dates, values);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_valueCollectionWithNull() {
    Collection<LocalDate> dates = dates(DATE_2011_01_01, DATE_2012_01_01);
    Collection<Double> values = Arrays.asList(2d, null);
    SparseLocalDateDoubleTimeSeries.of(dates, values);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_collectionsOfDifferentSize() {
    Collection<LocalDate> dates = dates(DATE_2011_01_01);
    Collection<Double> values = values(2d, 3d);
    SparseLocalDateDoubleTimeSeries.of(dates, values);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_datesUnordered() {
    Collection<LocalDate> dates = dates(DATE_2012_01_01, DATE_2011_01_01);
    Collection<Double> values = values(2d, 1d);
    SparseLocalDateDoubleTimeSeries.of(dates, values);
  }

  //-------------------------------------------------------------------------
  public void test_of_map() {
    Map<LocalDate, Double> map = new HashMap<>();
    map.put(DATE_2011_01_01, 2d);
    map.put(DATE_2012_01_01, 3d);
    LocalDateDoubleTimeSeries test = SparseLocalDateDoubleTimeSeries.of(map);
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
    SparseLocalDateDoubleTimeSeries.of((Map<LocalDate, Double>) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_map_dateNull() {
    Map<LocalDate, Double> map = new HashMap<>();
    map.put(DATE_2011_01_01, 2d);
    map.put(null, 3d);
    SparseLocalDateDoubleTimeSeries.of(map);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_map_valueNull() {
    Map<LocalDate, Double> map = new HashMap<>();
    map.put(DATE_2011_01_01, 2d);
    map.put(DATE_2012_01_01, null);
    SparseLocalDateDoubleTimeSeries.of(map);
  }

  //-------------------------------------------------------------------------
  public void test_of_collection() {
    Collection<LocalDateDoublePoint> points = Arrays.asList(
        LocalDateDoublePoint.of(DATE_2011_01_01, 2d),
        LocalDateDoublePoint.of(DATE_2012_01_01, 3d));
    LocalDateDoubleTimeSeries test = SparseLocalDateDoubleTimeSeries.of(points);
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
    SparseLocalDateDoubleTimeSeries.of((Collection<LocalDateDoublePoint>) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collection_collectionWithNull() {
    Collection<LocalDateDoublePoint> points = Arrays.asList(
        LocalDateDoublePoint.of(DATE_2011_01_01, 2d), null);
    SparseLocalDateDoubleTimeSeries.of(points);
  }

  //-------------------------------------------------------------------------
  public void test_immutableViaBeanBuilder() {
    LocalDate[] dates = {DATE_2010_01_01, DATE_2011_01_01, DATE_2012_01_01};
    double[] values = {6, 5, 4};
    BeanBuilder<? extends LocalDateDoubleTimeSeries> builder = SparseLocalDateDoubleTimeSeries.meta().builder();
    builder.set("dates", dates);
    builder.set("values", values);
    LocalDateDoubleTimeSeries test = builder.build();
    dates[0] = DATE_2012_01_01;
    values[0] = -1;
    LocalDateDoublePoint[] points = test.stream().toArray(LocalDateDoublePoint[]::new);
    assertEquals(points[0], LocalDateDoublePoint.of(DATE_2010_01_01, 6d));
    assertEquals(points[1], LocalDateDoublePoint.of(DATE_2011_01_01, 5d));
    assertEquals(points[2], LocalDateDoublePoint.of(DATE_2012_01_01, 4d));
  }

  public void test_immutableDatesViaBeanGet() {
    SparseLocalDateDoubleTimeSeries test = SparseLocalDateDoubleTimeSeries.of(DATES_2010_12, VALUES_10_12);
    LocalDate[] array = (LocalDate[]) test.property("dates").get();
    array[0] = DATE_2012_01_01;
    LocalDateDoublePoint[] points = test.stream().toArray(LocalDateDoublePoint[]::new);
    assertEquals(points[0], LocalDateDoublePoint.of(DATE_2010_01_01, 10d));
    assertEquals(points[1], LocalDateDoublePoint.of(DATE_2011_01_01, 11d));
    assertEquals(points[2], LocalDateDoublePoint.of(DATE_2012_01_01, 12d));
  }

  public void test_immutableValuesViaBeanGet() {
    SparseLocalDateDoubleTimeSeries test = SparseLocalDateDoubleTimeSeries.of(DATES_2010_12, VALUES_10_12);
    double[] array = (double[]) test.property("values").get();
    array[0] = -1;
    LocalDateDoublePoint[] points = test.stream().toArray(LocalDateDoublePoint[]::new);
    assertEquals(points[0], LocalDateDoublePoint.of(DATE_2010_01_01, 10d));
    assertEquals(points[1], LocalDateDoublePoint.of(DATE_2011_01_01, 11d));
    assertEquals(points[2], LocalDateDoublePoint.of(DATE_2012_01_01, 12d));
  }

  //-------------------------------------------------------------------------
  public void test_earliestLatest() {
    LocalDateDoubleTimeSeries test = SparseLocalDateDoubleTimeSeries.of(DATES_2010_12, VALUES_10_12);
    assertEquals(test.getEarliestDate(), DATE_2010_01_01);
    assertEquals(test.getEarliestValue(), 10d, TOLERANCE);
    assertEquals(test.getLatestDate(), DATE_2012_01_01);
    assertEquals(test.getLatestValue(), 12d, TOLERANCE);
  }

  public void test_earliestLatest_whenEmpty() {
    LocalDateDoubleTimeSeries test = EMPTY_SERIES;
    TestHelper.assertThrows(() -> test.getEarliestDate(), NoSuchElementException.class);
    TestHelper.assertThrows(() -> test.getEarliestValue(), NoSuchElementException.class);
    TestHelper.assertThrows(() -> test.getLatestDate(), NoSuchElementException.class);
    TestHelper.assertThrows(() -> test.getLatestValue(), NoSuchElementException.class);
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
    LocalDateDoubleTimeSeries base = SparseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14);
    LocalDateDoubleTimeSeries test = base.subSeries(start, end);
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
    LocalDateDoubleTimeSeries test = EMPTY_SERIES.subSeries(start, end);
    assertEquals(test.size(), 0);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_subSeries_startAfterEnd() {
    LocalDateDoubleTimeSeries base = SparseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14);
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
    LocalDateDoubleTimeSeries base = SparseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14);
    LocalDateDoubleTimeSeries test = base.headSeries(count);
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
    LocalDateDoubleTimeSeries test = EMPTY_SERIES.headSeries(count);
    assertEquals(test.size(), 0);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_headSeries_negative() {
    LocalDateDoubleTimeSeries base = SparseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14);
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
    LocalDateDoubleTimeSeries base = SparseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14);
    LocalDateDoubleTimeSeries test = base.tailSeries(count);
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
    LocalDateDoubleTimeSeries test = EMPTY_SERIES.tailSeries(count);
    assertEquals(test.size(), 0);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_tailSeries_negative() {
    LocalDateDoubleTimeSeries base = SparseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14);
    base.tailSeries(-1);
  }

  //-------------------------------------------------------------------------
  public void test_stream() {
    LocalDateDoubleTimeSeries base = SparseLocalDateDoubleTimeSeries.of(DATES_2010_12, VALUES_10_12);
    Object[] test = base.stream().toArray();
    assertEquals(test[0], LocalDateDoublePoint.of(DATE_2010_01_01, 10));
    assertEquals(test[1], LocalDateDoublePoint.of(DATE_2011_01_01, 11));
    assertEquals(test[2], LocalDateDoublePoint.of(DATE_2012_01_01, 12));
  }

  public void test_stream_withCollector() {
    LocalDateDoubleTimeSeries base = SparseLocalDateDoubleTimeSeries.of(DATES_2010_12, VALUES_10_12);
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
    LocalDateDoubleTimeSeries base = SparseLocalDateDoubleTimeSeries.of(DATES_2010_12, VALUES_10_12);
    LocalDate[] test = base.dateStream().toArray(LocalDate[]::new);
    assertEquals(test[0], DATE_2010_01_01);
    assertEquals(test[1], DATE_2011_01_01);
    assertEquals(test[2], DATE_2012_01_01);
  }

  public void test_valueStream() {
    LocalDateDoubleTimeSeries base = SparseLocalDateDoubleTimeSeries.of(DATES_2010_12, VALUES_10_12);
    double[] test = base.valueStream().toArray();
    assertEquals(test[0], 10, TOLERANCE);
    assertEquals(test[1], 11, TOLERANCE);
    assertEquals(test[2], 12, TOLERANCE);
  }

  //-------------------------------------------------------------------------
  public void test_forEach() {
    LocalDateDoubleTimeSeries base = SparseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14);
    AtomicInteger counter = new AtomicInteger();
    base.forEach((date, value) -> counter.addAndGet((int) value));
    assertEquals(counter.get(), 10 + 11 + 12 + 13 + 14);
  }

  //-------------------------------------------------------------------------
  public void test_combineWith_intersectionWithNoMatchingElements() {
    LocalDateDoubleTimeSeries series1 = SparseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14);
    List<LocalDate> dates2 = dates(DATE_2010_06_01, DATE_2011_06_01, DATE_2012_06_01, DATE_2013_06_01, DATE_2014_06_01);
    SparseLocalDateDoubleTimeSeries series2 = SparseLocalDateDoubleTimeSeries.of(dates2, VALUES_10_14);

    LocalDateDoubleTimeSeries test = series1.combineWith(series2, (l, r) -> l + r);
    assertEquals(test, EMPTY_SERIES);
  }

  public void test_combineWith_intersectionWithSomeMatchingElements() {
    LocalDateDoubleTimeSeries series1 = SparseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14);
    List<LocalDate> dates2 = dates(DATE_2010_01_01, DATE_2011_06_01, DATE_2012_01_01, DATE_2013_06_01, DATE_2014_01_01);
    List<Double> values2 = values(1.0, 1.1, 1.2, 1.3, 1.4);
    SparseLocalDateDoubleTimeSeries series2 = SparseLocalDateDoubleTimeSeries.of(dates2, values2);

    LocalDateDoubleTimeSeries test = series1.combineWith(series2, (l, r) -> l + r);
    assertEquals(test.size(), 3);
    assertEquals(test.get(DATE_2010_01_01), OptionalDouble.of(11.0));
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.of(13.2));
    assertEquals(test.get(DATE_2014_01_01), OptionalDouble.of(15.4));
  }

  public void test_combineWith_intersectionWithSomeMatchingElements2() {
    List<LocalDate> dates1 = dates(DATE_2010_01_01, DATE_2011_01_01, DATE_2012_01_01, DATE_2014_01_01, DATE_2015_06_01);
    List<Double> values1 = values(10, 11, 12, 13, 14);
    LocalDateDoubleTimeSeries series1 = SparseLocalDateDoubleTimeSeries.of(dates1, values1);
    List<LocalDate> dates2 = dates(DATE_2010_01_01, DATE_2011_06_01, DATE_2012_01_01, DATE_2013_01_01, DATE_2014_01_01);
    List<Double> values2 = values(1.0, 1.1, 1.2, 1.3, 1.4);
    SparseLocalDateDoubleTimeSeries series2 = SparseLocalDateDoubleTimeSeries.of(dates2, values2);

    LocalDateDoubleTimeSeries test = series1.combineWith(series2, (l, r) -> l + r);
    assertEquals(test.size(), 3);
    assertEquals(test.get(DATE_2010_01_01), OptionalDouble.of(11.0));
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.of(13.2));
    assertEquals(test.get(DATE_2014_01_01), OptionalDouble.of(14.4));
  }

  public void test_combineWith_intersectionWithAllMatchingElements() {
    List<LocalDate> dates1 = DATES_2010_14;
    List<Double> values1 = values(10, 11, 12, 13, 14);
    LocalDateDoubleTimeSeries series1 = SparseLocalDateDoubleTimeSeries.of(dates1, values1);
    List<LocalDate> dates2 = DATES_2010_14;
    List<Double> values2 = values(1.0, 1.1, 1.2, 1.3, 1.4);
    SparseLocalDateDoubleTimeSeries series2 = SparseLocalDateDoubleTimeSeries.of(dates2, values2);

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
    LocalDateDoubleTimeSeries base = SparseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14);
    LocalDateDoubleTimeSeries test = base.mapValues(d -> d + 5);
    List<Double> expectedValues = values(15, 16, 17, 18, 19);
    assertEquals(test, SparseLocalDateDoubleTimeSeries.of(DATES_2010_14, expectedValues));
  }

  public void test_mapValues_multiplySeries() {
    LocalDateDoubleTimeSeries base = SparseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14);

    LocalDateDoubleTimeSeries test = base.mapValues(d -> d * 5);
    List<Double> expectedValues = values(50, 55, 60, 65, 70);
    assertEquals(test, SparseLocalDateDoubleTimeSeries.of(DATES_2010_14, expectedValues));
  }

  public void test_mapValues_invertSeries() {
    List<Double> values = values(1, 2, 4, 5, 8);
    LocalDateDoubleTimeSeries base = SparseLocalDateDoubleTimeSeries.of(DATES_2010_14, values);
    LocalDateDoubleTimeSeries test = base.mapValues(d -> 1 / d);
    List<Double> expectedValues = values(1, 0.5, 0.25, 0.2, 0.125);
    assertEquals(test, SparseLocalDateDoubleTimeSeries.of(DATES_2010_14, expectedValues));
  }

  //-------------------------------------------------------------------------
  public void test_filter_byDate() {
    List<LocalDate> dates = dates(DATE_2010_01_01, DATE_2011_06_01, DATE_2012_01_01, DATE_2013_06_01, DATE_2014_01_01);
    LocalDateDoubleTimeSeries base = SparseLocalDateDoubleTimeSeries.of(dates, VALUES_10_14);
    LocalDateDoubleTimeSeries test = base.filter((ld, v) -> ld.getMonthValue() != 6);
    assertEquals(test.size(), 3);
    assertEquals(test.get(DATE_2010_01_01), OptionalDouble.of(10d));
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.of(12d));
    assertEquals(test.get(DATE_2014_01_01), OptionalDouble.of(14d));
  }

  public void test_filter_byValue() {
    LocalDateDoubleTimeSeries base = SparseLocalDateDoubleTimeSeries.of(DATES_2010_14, VALUES_10_14);
    LocalDateDoubleTimeSeries test = base.filter((ld, v) -> v % 2 == 1);
    assertEquals(test.size(), 2);
    assertEquals(test.get(DATE_2011_01_01), OptionalDouble.of(11d));
    assertEquals(test.get(DATE_2013_01_01), OptionalDouble.of(13d));
  }

  public void test_filter_byDateAndValue() {
    List<LocalDate> dates = dates(DATE_2010_01_01, DATE_2011_06_01, DATE_2012_01_01, DATE_2013_06_01, DATE_2014_01_01);
    LocalDateDoubleTimeSeries series = SparseLocalDateDoubleTimeSeries.of(dates, VALUES_10_14);

    LocalDateDoubleTimeSeries test = series.filter((ld, v) -> ld.getYear() >= 2012 && v % 2 == 0);
    assertEquals(test.size(), 2);
    assertEquals(test.get(DATE_2012_01_01), OptionalDouble.of(12d));
    assertEquals(test.get(DATE_2014_01_01), OptionalDouble.of(14d));
  }

  //-------------------------------------------------------------------------
  public void test_toMap() {
    SparseLocalDateDoubleTimeSeries base = SparseLocalDateDoubleTimeSeries.of(DATES_2010_12, VALUES_10_12);
    ImmutableMap<LocalDate, Double> test = base.toMap();
    assertEquals(test.size(), 3);
    assertEquals(test.get(DATE_2010_01_01), Double.valueOf(10));
    assertEquals(test.get(DATE_2011_01_01), Double.valueOf(11));
    assertEquals(test.get(DATE_2012_01_01), Double.valueOf(12));
  }

  //-------------------------------------------------------------------------
  public void test_equals_similarSeriesAreEqual() {
    SparseLocalDateDoubleTimeSeries series1 = SparseLocalDateDoubleTimeSeries.of(DATE_2014_01_01, 1d);
    LocalDateDoubleTimeSeries series2 = SparseLocalDateDoubleTimeSeries.of(dates(DATE_2014_01_01), values(1d));
    assertEquals(series1.size(), 1);
    assertEquals(series1, series2);
    assertEquals(series1, series1);
    assertEquals(series1.hashCode(), series1.hashCode());
  }

  public void test_equals_notEqual() {
    LocalDateDoubleTimeSeries series1 = SparseLocalDateDoubleTimeSeries.of(DATE_2014_01_01, 1d);
    LocalDateDoubleTimeSeries series2 = SparseLocalDateDoubleTimeSeries.of(DATE_2013_06_01, 1d);
    LocalDateDoubleTimeSeries series3 = SparseLocalDateDoubleTimeSeries.of(DATE_2014_01_01, 3d);
    assertNotEquals(series1, series2);
    assertNotEquals(series1, series3);
  }

  public void test_equals_bad() {
    SparseLocalDateDoubleTimeSeries test = SparseLocalDateDoubleTimeSeries.of(DATE_2014_01_01, 1d);
    assertEquals(test.equals(""), false);
    assertEquals(test.equals(null), false);
  }

  //-------------------------------------------------------------------------
  public void test_coverage() {
    TestHelper.coverImmutableBean(SparseLocalDateDoubleTimeSeries.of(DATE_2014_01_01, 1d));
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

}
