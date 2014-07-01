/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.collect.timeseries;

import static com.opengamma.collect.timeseries.LocalDateDoubleTimeSeries.EMPTY_SERIES;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.beans.BeanBuilder;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.collect.TestHelper;

/**
 * Test LocalDateDoubleTimeSeries.
 */
@Test
public class LocalDateDoubleTimeSeriesTest {

  private static final LocalDate DATE_2012_06_29 = LocalDate.of(2012, 6, 29);
  private static final LocalDate DATE_2012_06_30 = LocalDate.of(2012, 6, 30);
  private static final LocalDate DATE_2012_07_01 = LocalDate.of(2012, 7, 1);
  private static final double TOLERANCE = 0.00001d;

  //-------------------------------------------------------------------------
  public void test_emptySeries() {
    LocalDateDoubleTimeSeries test = EMPTY_SERIES;
    assertEquals(test.isEmpty(), true);
    assertEquals(test.size(), 0);
    assertEquals(test.containsDate(DATE_2012_06_29), false);
    assertEquals(test.containsDate(DATE_2012_06_30), false);
    assertEquals(test.containsDate(DATE_2012_07_01), false);
    assertEquals(test.get(DATE_2012_06_29), OptionalDouble.empty());
    assertEquals(test.get(DATE_2012_06_30), OptionalDouble.empty());
    assertEquals(test.get(DATE_2012_07_01), OptionalDouble.empty());
    assertEquals(test, LocalDateDoubleTimeSeries.of(new LocalDate[0], new double[0]));
  }

  //-------------------------------------------------------------------------
  public void test_of_singleton() {
    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.of(DATE_2012_06_30, 2d);
    assertEquals(test.isEmpty(), false);
    assertEquals(test.size(), 1);
    assertEquals(test.containsDate(DATE_2012_06_29), false);
    assertEquals(test.containsDate(DATE_2012_06_30), true);
    assertEquals(test.containsDate(DATE_2012_07_01), false);
    assertEquals(test.get(DATE_2012_06_29), OptionalDouble.empty());
    assertEquals(test.get(DATE_2012_06_30), OptionalDouble.of(2d));
    assertEquals(test.get(DATE_2012_07_01), OptionalDouble.empty());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_singleton_nullDateDisallowed() {
    LocalDateDoubleTimeSeries.of(null, 1d);
  }

  //-------------------------------------------------------------------------
  public void test_of_arrayArray() {
    LocalDate[] dates = {DATE_2012_06_30, DATE_2012_07_01};
    double[] values = {2d, 3d};
    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.of(dates, values);
    assertEquals(test.isEmpty(), false);
    assertEquals(test.size(), 2);
    assertEquals(test.containsDate(DATE_2012_06_29), false);
    assertEquals(test.containsDate(DATE_2012_06_30), true);
    assertEquals(test.containsDate(DATE_2012_07_01), true);
    assertEquals(test.get(DATE_2012_06_29), OptionalDouble.empty());
    assertEquals(test.get(DATE_2012_06_30), OptionalDouble.of(2d));
    assertEquals(test.get(DATE_2012_07_01), OptionalDouble.of(3d));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_arrayArray_dateArrayNull() {
    LocalDateDoubleTimeSeries.of(null, new double[] {1d, 2d});
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_arrayArray_valueArrayNull() {
    LocalDateDoubleTimeSeries.of(new LocalDate[] {LocalDate.MIN}, (double[]) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_arrayArray_dateArrayWithNull() {
    LocalDateDoubleTimeSeries.of(new LocalDate[] {null}, new double[]{1d});
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_arrayArray_arraysOfDifferentSize() {
    LocalDateDoubleTimeSeries.of(new LocalDate[] {LocalDate.MIN, LocalDate.MAX}, new double[] {1d});
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_arrayArray_datesUnordered() {
    LocalDate[] dates = {date(2012, 1, 1), date(2014, 1, 1), date(2013, 1, 1)};
    LocalDateDoubleTimeSeries.of(dates, new double[] {1d, 1d, 1d});
  }

  //-------------------------------------------------------------------------
  public void test_of_collectionCollection() {
    Collection<LocalDate> dates = Arrays.asList(DATE_2012_06_30, DATE_2012_07_01);
    Collection<Double> values = Arrays.asList(2d, 3d);
    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.of(dates, values);
    assertEquals(test.isEmpty(), false);
    assertEquals(test.size(), 2);
    assertEquals(test.containsDate(DATE_2012_06_29), false);
    assertEquals(test.containsDate(DATE_2012_06_30), true);
    assertEquals(test.containsDate(DATE_2012_07_01), true);
    assertEquals(test.get(DATE_2012_06_29), OptionalDouble.empty());
    assertEquals(test.get(DATE_2012_06_30), OptionalDouble.of(2d));
    assertEquals(test.get(DATE_2012_07_01), OptionalDouble.of(3d));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_dateCollectionNull() {
    Collection<Double> values = Arrays.asList(2d, 3d);
    LocalDateDoubleTimeSeries.of((Collection<LocalDate>) null, values);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_valueCollectionNull() {
    Collection<LocalDate> dates = Arrays.asList(DATE_2012_06_30, DATE_2012_07_01);
    LocalDateDoubleTimeSeries.of(dates, (Collection<Double>) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_dateCollectionWithNull() {
    Collection<LocalDate> dates = Arrays.asList(DATE_2012_06_30, null);
    Collection<Double> values = Arrays.asList(2d, 3d);
    LocalDateDoubleTimeSeries.of(dates, values);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_valueCollectionWithNull() {
    Collection<LocalDate> dates = Arrays.asList(DATE_2012_06_30, DATE_2012_07_01);
    Collection<Double> values = Arrays.asList(2d, null);
    LocalDateDoubleTimeSeries.of(dates, values);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_collectionsOfDifferentSize() {
    Collection<LocalDate> dates = Arrays.asList(DATE_2012_06_30);
    Collection<Double> values = Arrays.asList(2d, 3d);
    LocalDateDoubleTimeSeries.of(dates, values);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collectionCollection_datesUnordered() {
    Collection<LocalDate> dates = Arrays.asList(DATE_2012_07_01, DATE_2012_06_30);
    Collection<Double> values = Arrays.asList(2d, 1d);
    LocalDateDoubleTimeSeries.of(dates, values);
  }

  //-------------------------------------------------------------------------
  public void test_of_map() {
    Map<LocalDate, Double> map = new HashMap<>();
    map.put(DATE_2012_06_30, 2d);
    map.put(DATE_2012_07_01, 3d);
    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.of(map);
    assertEquals(test.isEmpty(), false);
    assertEquals(test.size(), 2);
    assertEquals(test.containsDate(DATE_2012_06_29), false);
    assertEquals(test.containsDate(DATE_2012_06_30), true);
    assertEquals(test.containsDate(DATE_2012_07_01), true);
    assertEquals(test.get(DATE_2012_06_29), OptionalDouble.empty());
    assertEquals(test.get(DATE_2012_06_30), OptionalDouble.of(2d));
    assertEquals(test.get(DATE_2012_07_01), OptionalDouble.of(3d));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_map_null() {
    LocalDateDoubleTimeSeries.of((Map<LocalDate, Double>) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_map_dateNull() {
    Map<LocalDate, Double> map = new HashMap<>();
    map.put(DATE_2012_06_30, 2d);
    map.put(null, 3d);
    LocalDateDoubleTimeSeries.of(map);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_map_valueNull() {
    Map<LocalDate, Double> map = new HashMap<>();
    map.put(DATE_2012_06_30, 2d);
    map.put(DATE_2012_07_01, null);
    LocalDateDoubleTimeSeries.of(map);
  }

  //-------------------------------------------------------------------------
  public void test_of_collection() {
    Collection<LocalDateDoublePoint> points = Arrays.asList(
        LocalDateDoublePoint.of(DATE_2012_06_30, 2d),
        LocalDateDoublePoint.of(DATE_2012_07_01, 3d));
    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.of(points);
    assertEquals(test.isEmpty(), false);
    assertEquals(test.size(), 2);
    assertEquals(test.containsDate(DATE_2012_06_29), false);
    assertEquals(test.containsDate(DATE_2012_06_30), true);
    assertEquals(test.containsDate(DATE_2012_07_01), true);
    assertEquals(test.get(DATE_2012_06_29), OptionalDouble.empty());
    assertEquals(test.get(DATE_2012_06_30), OptionalDouble.of(2d));
    assertEquals(test.get(DATE_2012_07_01), OptionalDouble.of(3d));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collection_collectionNull() {
    LocalDateDoubleTimeSeries.of((Collection<LocalDateDoublePoint>) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_of_collection_collectionWithNull() {
    Collection<LocalDateDoublePoint> points = Arrays.asList(
        LocalDateDoublePoint.of(DATE_2012_06_30, 2d), null);
    LocalDateDoubleTimeSeries.of(points);
  }

  //-------------------------------------------------------------------------
  public void test_immutableViaArrayFactory() {
    LocalDate[] dates = {DATE_2012_06_29, DATE_2012_06_30, DATE_2012_07_01};
    double[] values = {6, 5, 4};
    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.of(dates, values);
    dates[0] = DATE_2012_07_01;
    values[0] = -1;
    assertEquals(test.getAtIndex(0), LocalDateDoublePoint.of(DATE_2012_06_29, 6d));
    assertEquals(test.getAtIndex(1), LocalDateDoublePoint.of(DATE_2012_06_30, 5d));
    assertEquals(test.getAtIndex(2), LocalDateDoublePoint.of(DATE_2012_07_01, 4d));
  }

  public void test_immutableViaBeanBuilder() {
    LocalDate[] dates = {DATE_2012_06_29, DATE_2012_06_30, DATE_2012_07_01};
    double[] values = {6, 5, 4};
    BeanBuilder<? extends LocalDateDoubleTimeSeries> builder = LocalDateDoubleTimeSeries.meta().builder();
    builder.set("dates", dates);
    builder.set("values", values);
    LocalDateDoubleTimeSeries test = builder.build();
    dates[0] = DATE_2012_07_01;
    values[0] = -1;
    assertEquals(test.getAtIndex(0), LocalDateDoublePoint.of(DATE_2012_06_29, 6d));
    assertEquals(test.getAtIndex(1), LocalDateDoublePoint.of(DATE_2012_06_30, 5d));
    assertEquals(test.getAtIndex(2), LocalDateDoublePoint.of(DATE_2012_07_01, 4d));
  }

  public void test_immutableDatesViaBeanGet() {
    LocalDate[] dates = {DATE_2012_06_29, DATE_2012_06_30, DATE_2012_07_01};
    double[] values = {6, 5, 4};
    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.of(dates, values);
    LocalDate[] array = (LocalDate[]) test.property("dates").get();
    array[0] = DATE_2012_07_01;
    assertEquals(test.getAtIndex(0), LocalDateDoublePoint.of(DATE_2012_06_29, 6d));
    assertEquals(test.getAtIndex(1), LocalDateDoublePoint.of(DATE_2012_06_30, 5d));
    assertEquals(test.getAtIndex(2), LocalDateDoublePoint.of(DATE_2012_07_01, 4d));
  }

  public void test_immutableValuesViaBeanGet() {
    LocalDate[] dates = {DATE_2012_06_29, DATE_2012_06_30, DATE_2012_07_01};
    double[] values = {6, 5, 4};
    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.of(dates, values);
    double[] array = (double[]) test.property("values").get();
    array[0] = -1;
    assertEquals(test.getAtIndex(0), LocalDateDoublePoint.of(DATE_2012_06_29, 6d));
    assertEquals(test.getAtIndex(1), LocalDateDoublePoint.of(DATE_2012_06_30, 5d));
    assertEquals(test.getAtIndex(2), LocalDateDoublePoint.of(DATE_2012_07_01, 4d));
  }

  //-------------------------------------------------------------------------
  public void test_getAtIndex() {
    LocalDate[] dates = {DATE_2012_06_29, DATE_2012_06_30, DATE_2012_07_01};
    double[] values = {6, 5, 4};
    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.of(dates, values);
    assertEquals(test.getAtIndex(0), LocalDateDoublePoint.of(DATE_2012_06_29, 6d));
    assertEquals(test.getAtIndex(1), LocalDateDoublePoint.of(DATE_2012_06_30, 5d));
    assertEquals(test.getAtIndex(2), LocalDateDoublePoint.of(DATE_2012_07_01, 4d));
    TestHelper.assertThrows(() -> test.getAtIndex(-1), IndexOutOfBoundsException.class);
    TestHelper.assertThrows(() -> test.getAtIndex(3), IndexOutOfBoundsException.class);
  }

  //-------------------------------------------------------------------------
  public void test_earliestLatest() {
    LocalDate[] dates = {DATE_2012_06_29, DATE_2012_06_30, DATE_2012_07_01};
    double[] values = {6, 5, 4};
    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.of(dates, values);
    assertEquals(test.getEarliestDate(), DATE_2012_06_29);
    assertEquals(test.getEarliestValue(), 6d, TOLERANCE);
    assertEquals(test.getLatestDate(), DATE_2012_07_01);
    assertEquals(test.getLatestValue(), 4d, TOLERANCE);
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
        {date(2011, 1, 1), date(2011, 1, 1), new int[] {}},
        // no overlap
        {date(2006, 1, 1), date(2009, 1, 1), new int[] {}},
        // single point
        {date(2011, 1, 1), date(2011, 1, 2), new int[] {1}},
        // include when start matches base, exclude when end matches base
        {date(2011, 1, 1), date(2013, 1, 1), new int[] {1, 2}},
        // include when start matches base
        {date(2011, 1, 1), date(2013, 1, 2), new int[] {1, 2, 3}},
        // neither start nor end match
        {date(2010, 12, 31), date(2013, 1, 2), new int[] {1, 2, 3}},
        // start date just after a base date
        {date(2011, 1, 2), date(2013, 1, 2), new int[] {2, 3}},
    };
  }

  @Test(dataProvider = "subSeries")
  public void test_subSeries(LocalDate start, LocalDate end, int[] expected) {
    LocalDate[] dates = {date(2010, 1, 1), date(2011, 1, 1), date(2012, 1, 1), date(2013, 1, 1), date(2014, 1, 1)};
    double[] values = {10, 11, 12, 13, 14};
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.of(dates, values);
    LocalDateDoubleTimeSeries test = base.subSeries(start, end);
    assertEquals(test.size(), expected.length);
    for (int i = 0; i < dates.length; i++) {
      if (Arrays.binarySearch(expected, i) >= 0) {
        assertEquals(test.get(dates[i]), OptionalDouble.of(values[i]));
      } else {
        assertEquals(test.get(dates[i]), OptionalDouble.empty());
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
    LocalDate[] dates = {date(2010, 1, 1), date(2011, 1, 1), date(2012, 1, 1), date(2013, 1, 1), date(2014, 1, 1)};
    double[] values = {10, 11, 12, 13, 14};
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.of(dates, values);
    base.subSeries(date(2011, 1, 2), date(2011, 1, 1));
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
    LocalDate[] dates = {date(2010, 1, 1), date(2011, 1, 1), date(2012, 1, 1), date(2013, 1, 1), date(2014, 1, 1)};
    double[] values = {10, 11, 12, 13, 14};
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.of(dates, values);
    LocalDateDoubleTimeSeries test = base.headSeries(count);
    assertEquals(test.size(), expected.length);
    for (int i = 0; i < dates.length; i++) {
      if (Arrays.binarySearch(expected, i) >= 0) {
        assertEquals(test.get(dates[i]), OptionalDouble.of(values[i]));
      } else {
        assertEquals(test.get(dates[i]), OptionalDouble.empty());
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
    LocalDate[] dates = {date(2010, 1, 1), date(2011, 1, 1), date(2012, 1, 1), date(2013, 1, 1), date(2014, 1, 1)};
    double[] values = {10, 11, 12, 13, 14};
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.of(dates, values);
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
    LocalDate[] dates = {date(2010, 1, 1), date(2011, 1, 1), date(2012, 1, 1), date(2013, 1, 1), date(2014, 1, 1)};
    double[] values = {10, 11, 12, 13, 14};
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.of(dates, values);
    LocalDateDoubleTimeSeries test = base.tailSeries(count);
    assertEquals(test.size(), expected.length);
    for (int i = 0; i < dates.length; i++) {
      if (Arrays.binarySearch(expected, i) >= 0) {
        assertEquals(test.get(dates[i]), OptionalDouble.of(values[i]));
      } else {
        assertEquals(test.get(dates[i]), OptionalDouble.empty());
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
    LocalDate[] dates = {date(2010, 1, 1), date(2011, 1, 1), date(2012, 1, 1), date(2013, 1, 1), date(2014, 1, 1)};
    double[] values = {10, 11, 12, 13, 14};
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.of(dates, values);
    base.tailSeries(-1);
  }

  //-------------------------------------------------------------------------
  public void test_stream() {
    LocalDate[] dates = {DATE_2012_06_29, DATE_2012_06_30, DATE_2012_07_01};
    double[] values = {10, 11, 12};
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.of(dates, values);
    Object[] test = base.stream().toArray();
    assertEquals(test[0], LocalDateDoublePoint.of(DATE_2012_06_29, 10));
    assertEquals(test[1], LocalDateDoublePoint.of(DATE_2012_06_30, 11));
    assertEquals(test[2], LocalDateDoublePoint.of(DATE_2012_07_01, 12));
  }

  public void test_stream_withCollector() {
    LocalDate[] dates = {DATE_2012_06_29, DATE_2012_06_30, DATE_2012_07_01};
    double[] values = {10, 11, 12};
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.of(dates, values);
    LocalDateDoubleTimeSeries test = base.stream()
        .map(point -> point.withValue(1.5d))
        .collect(LocalDateDoubleTimeSeries.collector());
    assertEquals(test.size(), 3);
    assertEquals(test.get(DATE_2012_06_29), OptionalDouble.of(1.5));
    assertEquals(test.get(DATE_2012_06_30), OptionalDouble.of(1.5));
    assertEquals(test.get(DATE_2012_07_01), OptionalDouble.of(1.5));
  }

  //-------------------------------------------------------------------------
  public void test_dateStream() {
    LocalDate[] dates = {DATE_2012_06_29, DATE_2012_06_30, DATE_2012_07_01};
    double[] values = {10, 11, 12};
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.of(dates, values);
    LocalDate[] test = base.dateStream().toArray(LocalDate[]::new);
    assertEquals(test[0], DATE_2012_06_29);
    assertEquals(test[1], DATE_2012_06_30);
    assertEquals(test[2], DATE_2012_07_01);
  }

  public void test_valueStream() {
    LocalDate[] dates = {DATE_2012_06_29, DATE_2012_06_30, DATE_2012_07_01};
    double[] values = {10, 11, 12};
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.of(dates, values);
    double[] test = base.valueStream().toArray();
    assertEquals(test[0], 10, TOLERANCE);
    assertEquals(test[1], 11, TOLERANCE);
    assertEquals(test[2], 12, TOLERANCE);
  }

  //-------------------------------------------------------------------------
  public void test_forEach() {
    LocalDate[] dates = {date(2010, 1, 1), date(2011, 1, 1), date(2012, 1, 1), date(2013, 1, 1), date(2014, 1, 1)};
    double[] values = {10, 11, 12, 13, 14};
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.of(dates, values);

    AtomicInteger counter = new AtomicInteger();
    base.forEach((date, value) -> counter.addAndGet((int) value));
    assertEquals(counter.get(), 10 + 11 + 12 + 13 + 14);
  }

  //-------------------------------------------------------------------------
  public void test_combineWith_intersectionWithNoMatchingElements() {
    LocalDate[] dates1 = {date(2010, 1, 1), date(2011, 1, 1), date(2012, 1, 1), date(2013, 1, 1), date(2014, 1, 1)};
    double[] values = {10, 11, 12, 13, 14};
    LocalDateDoubleTimeSeries series1 = LocalDateDoubleTimeSeries.of(dates1, values);
    LocalDate[] dates2 = {date(2010, 6, 1), date(2011, 6, 1), date(2012, 6, 1), date(2013, 6, 1), date(2014, 6, 1)};
    LocalDateDoubleTimeSeries series2 = LocalDateDoubleTimeSeries.of(dates2, values);

    LocalDateDoubleTimeSeries test = series1.combineWith(series2, (l, r) -> l + r);
    assertEquals(test, EMPTY_SERIES);
  }

  public void test_combineWith_intersectionWithSomeMatchingElements() {
    LocalDate[] dates1 = {date(2010, 1, 1), date(2011, 1, 1), date(2012, 1, 1), date(2013, 1, 1), date(2014, 1, 1)};
    double[] values1 = {10, 11, 12, 13, 14};
    LocalDateDoubleTimeSeries series1 = LocalDateDoubleTimeSeries.of(dates1, values1);
    LocalDate[] dates2 = {date(2010, 1, 1), date(2011, 6, 1), date(2012, 1, 1), date(2013, 6, 1), date(2014, 1, 1)};
    double[] values2 = {1.0, 1.1, 1.2, 1.3, 1.4};
    LocalDateDoubleTimeSeries series2 = LocalDateDoubleTimeSeries.of(dates2, values2);

    LocalDateDoubleTimeSeries test = series1.combineWith(series2, (l, r) -> l + r);
    assertEquals(test.size(), 3);
    assertEquals(test.getEarliestDate(), date(2010, 1, 1));
    assertEquals(test.getLatestDate(), date(2014, 1, 1));
    assertEquals(test.get(date(2010, 1, 1)), OptionalDouble.of(11.0));
    assertEquals(test.get(date(2012, 1, 1)), OptionalDouble.of(13.2));
    assertEquals(test.get(date(2014, 1, 1)), OptionalDouble.of(15.4));
  }

  public void test_combineWith_intersectionWithSomeMatchingElements2() {
    LocalDate[] dates1 = {date(2010, 1, 1), date(2011, 1, 1), date(2012, 1, 1), date(2014, 1, 1), date(2015, 6, 1)};
    double[] values1 = {10, 11, 12, 13, 14};
    LocalDateDoubleTimeSeries series1 = LocalDateDoubleTimeSeries.of(dates1, values1);
    LocalDate[] dates2 = {date(2010, 1, 1), date(2011, 6, 1), date(2012, 1, 1), date(2013, 1, 1), date(2014, 1, 1)};
    double[] values2 = {1.0, 1.1, 1.2, 1.3, 1.4};
    LocalDateDoubleTimeSeries series2 = LocalDateDoubleTimeSeries.of(dates2, values2);

    LocalDateDoubleTimeSeries test = series1.combineWith(series2, (l, r) -> l + r);
    assertEquals(test.size(), 3);
    assertEquals(test.getEarliestDate(), date(2010, 1, 1));
    assertEquals(test.getLatestDate(), date(2014, 1, 1));
    assertEquals(test.get(date(2010, 1, 1)), OptionalDouble.of(11.0));
    assertEquals(test.get(date(2012, 1, 1)), OptionalDouble.of(13.2));
    assertEquals(test.get(date(2014, 1, 1)), OptionalDouble.of(14.4));
  }

  public void test_combineWith_intersectionWithAllMatchingElements() {
    LocalDate[] dates1 = {date(2010, 1, 1), date(2011, 1, 1), date(2012, 1, 1), date(2013, 1, 1), date(2014, 1, 1)};
    double[] values1 = {10, 11, 12, 13, 14};
    LocalDateDoubleTimeSeries series1 = LocalDateDoubleTimeSeries.of(dates1, values1);
    LocalDate[] dates2 = {date(2010, 1, 1), date(2011, 1, 1), date(2012, 1, 1), date(2013, 1, 1), date(2014, 1, 1)};
    double[] values2 = {1.0, 1.1, 1.2, 1.3, 1.4};
    LocalDateDoubleTimeSeries series2 = LocalDateDoubleTimeSeries.of(dates2, values2);

    LocalDateDoubleTimeSeries combined = series1.combineWith(series2, (l, r) -> l + r);
    assertEquals(combined.size(), 5);
    assertEquals(combined.getEarliestDate(), date(2010, 1, 1));
    assertEquals(combined.getLatestDate(), date(2014, 1, 1));
    assertEquals(combined.get(date(2010, 1, 1)), OptionalDouble.of(11.0));
    assertEquals(combined.get(date(2011, 1, 1)), OptionalDouble.of(12.1));
    assertEquals(combined.get(date(2012, 1, 1)), OptionalDouble.of(13.2));
    assertEquals(combined.get(date(2013, 1, 1)), OptionalDouble.of(14.3));
    assertEquals(combined.get(date(2014, 1, 1)), OptionalDouble.of(15.4));
  }

  //-------------------------------------------------------------------------
  public void test_mapValues_addConstantToSeries() {
    LocalDate[] dates = {date(2010, 1, 1), date(2011, 1, 1), date(2012, 1, 1), date(2013, 1, 1), date(2014, 1, 1)};
    double[] values = {10, 11, 12, 13, 14};
    LocalDateDoubleTimeSeries series = LocalDateDoubleTimeSeries.of(dates, values);

    LocalDateDoubleTimeSeries test = series.mapValues(d -> d + 5);
    double[] expectedValues = {15, 16, 17, 18, 19};
    assertEquals(test, LocalDateDoubleTimeSeries.of(dates, expectedValues));
  }

  public void test_mapValues_multiplySeries() {
    LocalDate[] dates = {date(2010, 1, 1), date(2011, 1, 1), date(2012, 1, 1), date(2013, 1, 1), date(2014, 1, 1)};
    double[] values = {10, 11, 12, 13, 14};
    LocalDateDoubleTimeSeries series = LocalDateDoubleTimeSeries.of(dates, values);

    LocalDateDoubleTimeSeries test = series.mapValues(d -> d * 5);
    double[] expectedValues = {50, 55, 60, 65, 70};
    assertEquals(test, LocalDateDoubleTimeSeries.of(dates, expectedValues));
  }

  public void test_mapValues_invertSeries() {
    LocalDate[] dates = {date(2010, 1, 1), date(2011, 1, 1), date(2012, 1, 1), date(2013, 1, 1), date(2014, 1, 1)};
    double[] values = {1, 2, 4, 5, 8};
    LocalDateDoubleTimeSeries series = LocalDateDoubleTimeSeries.of(dates, values);

    LocalDateDoubleTimeSeries test = series.mapValues(d -> 1 / d);
    double[] expectedValues = {1, 0.5, 0.25, 0.2, 0.125};
    assertEquals(test, LocalDateDoubleTimeSeries.of(dates, expectedValues));
  }

  //-------------------------------------------------------------------------
  public void test_filter_byDate() {
    LocalDate[] dates = {date(2010, 1, 1), date(2011, 6, 1), date(2012, 1, 1), date(2013, 6, 1), date(2014, 1, 1)};
    LocalDateDoubleTimeSeries series = LocalDateDoubleTimeSeries.of(dates, new double[]{10, 11, 12, 13, 14});

    LocalDateDoubleTimeSeries test = series.filter((ld, v) -> ld.getMonthValue() != 6);
    assertEquals(test.size(), 3);
    assertEquals(test.getEarliestDate(), date(2010, 1, 1));
    assertEquals(test.getLatestDate(), date(2014, 1, 1));
    assertEquals(test.get(date(2010, 1, 1)), OptionalDouble.of(10d));
    assertEquals(test.get(date(2012, 1, 1)), OptionalDouble.of(12d));
    assertEquals(test.get(date(2014, 1, 1)), OptionalDouble.of(14d));
  }

  public void test_filter_byValue() {
    LocalDate[] dates = {date(2010, 1, 1), date(2011, 6, 1), date(2012, 1, 1), date(2013, 6, 1), date(2014, 1, 1)};
    LocalDateDoubleTimeSeries series = LocalDateDoubleTimeSeries.of(dates, new double[]{10, 11, 12, 13, 14});

    LocalDateDoubleTimeSeries test = series.filter((ld, v) -> v % 2 == 1);
    assertEquals(test.size(), 2);
    assertEquals(test.getEarliestDate(), date(2011, 6, 1));
    assertEquals(test.getLatestDate(), date(2013, 6, 1));
    assertEquals(test.get(date(2011, 6, 1)), OptionalDouble.of(11d));
    assertEquals(test.get(date(2013, 6, 1)), OptionalDouble.of(13d));
  }

  public void test_filter_byDateAndValue() {
    LocalDate[] dates = {date(2010, 1, 1), date(2011, 6, 1), date(2012, 1, 1), date(2013, 6, 1), date(2014, 1, 1)};
    LocalDateDoubleTimeSeries series = LocalDateDoubleTimeSeries.of(dates, new double[]{10, 11, 12, 13, 14});

    LocalDateDoubleTimeSeries test = series.filter((ld, v) -> ld.getYear() >= 2012 && v % 2 == 0);
    assertEquals(test.size(), 2);
    assertEquals(test.getEarliestDate(), date(2012, 1, 1));
    assertEquals(test.getLatestDate(), date(2014, 1, 1));
    assertEquals(test.get(date(2012, 1, 1)), OptionalDouble.of(12d));
    assertEquals(test.get(date(2014, 1, 1)), OptionalDouble.of(14d));
  }

  //-------------------------------------------------------------------------
  public void test_toMap() {
    LocalDate[] dates = {DATE_2012_06_29, DATE_2012_06_30, DATE_2012_07_01};
    double[] values = {10, 11, 12};
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.of(dates, values);
    ImmutableMap<LocalDate, Double> test = base.toMap();
    assertEquals(test.size(), 3);
    assertEquals(test.get(DATE_2012_06_29), Double.valueOf(10));
    assertEquals(test.get(DATE_2012_06_30), Double.valueOf(11));
    assertEquals(test.get(DATE_2012_07_01), Double.valueOf(12));
  }

  //-------------------------------------------------------------------------
  public void test_equals_similarSeriesAreEqual() {
    LocalDateDoubleTimeSeries series1 = LocalDateDoubleTimeSeries.of(date(2014, 1, 1), 1d);
    LocalDateDoubleTimeSeries series2 = LocalDateDoubleTimeSeries.of(
        new LocalDate[] {date(2014, 1, 1)}, new double[] {1d});
    assertEquals(series1.size(), 1);
    assertEquals(series1, series2);
    assertEquals(series1, series1);
    assertEquals(series1.hashCode(), series1.hashCode());
  }

  public void test_equals_notEqual() {
    LocalDateDoubleTimeSeries series1 = LocalDateDoubleTimeSeries.of(date(2014, 1, 1), 1d);
    LocalDateDoubleTimeSeries series2 = LocalDateDoubleTimeSeries.of(date(2014, 1, 2), 1d);
    LocalDateDoubleTimeSeries series3 = LocalDateDoubleTimeSeries.of(date(2014, 1, 1), 3d);
    assertNotEquals(series1, series2);
    assertNotEquals(series1, series3);
  }

  public void test_equals_bad() {
    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.of(date(2014, 1, 1), 1d);
    assertEquals(test.equals(""), false);
    assertEquals(test.equals(null), false);
  }

  //-------------------------------------------------------------------------
  public void test_coverage() {
    TestHelper.coverImmutableBean(LocalDateDoubleTimeSeries.of(date(2014, 1, 1), 1d));
  }

  //-------------------------------------------------------------------------
  private static LocalDate date(int year, int month, int day) {
    return LocalDate.of(year, month, day);
  }

}
