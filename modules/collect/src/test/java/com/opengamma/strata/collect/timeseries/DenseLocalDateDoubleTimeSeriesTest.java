/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.timeseries;

import static com.opengamma.strata.collect.timeseries.DenseLocalDateDoubleTimeSeries.DenseTimeSeriesCalculation.INCLUDE_WEEKENDS;
import static com.opengamma.strata.collect.timeseries.DenseLocalDateDoubleTimeSeries.DenseTimeSeriesCalculation.SKIP_WEEKENDS;
import static com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.assertj.core.data.Offset;
import org.joda.beans.Bean;
import org.joda.beans.BeanBuilder;
import org.joda.beans.ImmutableBean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Doubles;
import com.opengamma.strata.collect.Guavate;
import com.opengamma.strata.collect.TestHelper;
import com.opengamma.strata.collect.tuple.Pair;

/**
 * Test {@link DenseLocalDateDoubleTimeSeries}.
 */
public class DenseLocalDateDoubleTimeSeriesTest {

  private static final Offset<Double> TOLERANCE = within(1e-5);
  private static final Object ANOTHER_TYPE = "";

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

  private static final LocalDate DATE_2014_01_01 = date(2014, 1, 1);  // Thu
  private static final LocalDate DATE_2015_01_02 = date(2015, 1, 2);  // Fri
  private static final LocalDate DATE_2015_01_03 = date(2015, 1, 3);  // Sat
  private static final LocalDate DATE_2015_01_04 = date(2015, 1, 4);  // Sun
  private static final LocalDate DATE_2015_01_05 = date(2015, 1, 5);  // Mon
  private static final LocalDate DATE_2015_01_06 = date(2015, 1, 6);  // Tue
  private static final LocalDate DATE_2015_01_07 = date(2015, 1, 7);  // Wed
  private static final LocalDate DATE_2015_01_08 = date(2015, 1, 8);  // Thu
  private static final LocalDate DATE_2015_01_09 = date(2015, 1, 9);  // Fri
  private static final LocalDate DATE_2015_01_11 = date(2015, 1, 11);  // Sun

  private static final LocalDate DATE_2015_01_12 = date(2015, 1, 12);

  private static final ImmutableList<LocalDate> DATES_2015_1_WEEK = dates(
      DATE_2015_01_05, DATE_2015_01_06, DATE_2015_01_07, DATE_2015_01_08, DATE_2015_01_09);

  private static final ImmutableList<Double> VALUES_1_WEEK = values(10, 11, 12, 13, 14);
  private static final ImmutableList<LocalDate> DATES_2010_12 = dates(
      DATE_2010_01_01, DATE_2011_01_01, DATE_2012_01_01);
  private static final ImmutableList<Double> VALUES_10_12 = values(10, 11, 12);
  private static final ImmutableList<Double> VALUES_1_3 = values(1, 2, 3);
  private static final ImmutableList<Double> VALUES_4_7 = values(4, 5, 6, 7);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_singleton() {
    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.of(DATE_2011_01_01, 2d);
    assertThat(test.isEmpty()).isEqualTo(false);
    assertThat(test.size()).isEqualTo(1);

    // Check start is not weekend

    assertThat(test.containsDate(DATE_2010_01_01)).isEqualTo(false);
    assertThat(test.containsDate(DATE_2011_01_01)).isEqualTo(true);
    assertThat(test.containsDate(DATE_2012_01_01)).isEqualTo(false);
    assertThat(test.get(DATE_2010_01_01)).isEqualTo(OptionalDouble.empty());
    assertThat(test.get(DATE_2011_01_01)).isEqualTo(OptionalDouble.of(2d));
    assertThat(test.get(DATE_2012_01_01)).isEqualTo(OptionalDouble.empty());
    assertThat(test.dates().toArray()).isEqualTo(new Object[] {DATE_2011_01_01});
    assertThat(test.values().toArray()).isEqualTo(new double[] {2d});
  }

  @Test
  public void test_of_singleton_nullDateDisallowed() {
    assertThatIllegalArgumentException().isThrownBy(() -> LocalDateDoubleTimeSeries.of(null, 1d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_collectionCollection() {
    Collection<LocalDate> dates = dates(DATE_2011_01_01, DATE_2012_01_01);
    Collection<Double> values = values(2d, 3d);

    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.builder().putAll(dates, values).build();
    assertThat(test.isEmpty()).isEqualTo(false);
    assertThat(test.size()).isEqualTo(2);
    assertThat(test.containsDate(DATE_2010_01_01)).isEqualTo(false);
    assertThat(test.containsDate(DATE_2011_01_01)).isEqualTo(true);
    assertThat(test.containsDate(DATE_2012_01_01)).isEqualTo(true);
    assertThat(test.get(DATE_2010_01_01)).isEqualTo(OptionalDouble.empty());
    assertThat(test.get(DATE_2011_01_01)).isEqualTo(OptionalDouble.of(2d));
    assertThat(test.get(DATE_2012_01_01)).isEqualTo(OptionalDouble.of(3d));
    assertThat(test.dates().toArray()).isEqualTo(new Object[] {DATE_2011_01_01, DATE_2012_01_01});
    assertThat(test.values().toArray()).isEqualTo(new double[] {2d, 3d});
  }

  @Test
  public void test_of_collectionCollection_dateCollectionNull() {
    Collection<Double> values = values(2d, 3d);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> LocalDateDoubleTimeSeries.builder().putAll(null, values).build());
  }

  @Test
  public void test_of_collectionCollection_valueCollectionNull() {
    Collection<LocalDate> dates = dates(DATE_2011_01_01, DATE_2012_01_01);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> LocalDateDoubleTimeSeries.builder().putAll(dates, (double[]) null).build());
  }

  @Test
  public void test_of_collectionCollection_dateCollectionWithNull() {
    Collection<LocalDate> dates = Arrays.asList(DATE_2011_01_01, null);
    Collection<Double> values = values(2d, 3d);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> LocalDateDoubleTimeSeries.builder().putAll(dates, values).build());
  }

  @Test
  public void test_of_collectionCollection_valueCollectionWithNull() {
    Collection<LocalDate> dates = dates(DATE_2011_01_01, DATE_2012_01_01);
    Collection<Double> values = Arrays.asList(2d, null);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> LocalDateDoubleTimeSeries.builder().putAll(dates, values).build());
  }

  @Test
  public void test_of_collectionCollection_collectionsOfDifferentSize() {
    Collection<LocalDate> dates = dates(DATE_2011_01_01);
    Collection<Double> values = values(2d, 3d);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> LocalDateDoubleTimeSeries.builder().putAll(dates, values).build());
  }

  @Test
  public void test_of_collectionCollection_datesUnordered() {
    Collection<LocalDate> dates = dates(DATE_2012_01_01, DATE_2011_01_01);
    Collection<Double> values = values(2d, 1d);

    LocalDateDoubleTimeSeries series = LocalDateDoubleTimeSeries.builder().putAll(dates, values).build();
    assertThat(series.get(DATE_2011_01_01)).isEqualTo(OptionalDouble.of(1d));
    assertThat(series.get(DATE_2012_01_01)).isEqualTo(OptionalDouble.of(2d));
  }

  @Test
  public void test_NaN_is_not_allowed() {
    assertThatIllegalArgumentException().isThrownBy(() -> LocalDateDoubleTimeSeries.of(DATE_2015_01_02, Double.NaN));
    assertThatIllegalArgumentException().isThrownBy(() -> LocalDateDoubleTimeSeries.builder().put(DATE_2015_01_02, Double.NaN));
    assertThatIllegalArgumentException().isThrownBy(() -> LocalDateDoubleTimeSeries.builder().putAll(
        ImmutableMap.of(DATE_2015_01_02, Double.NaN)));
    assertThatIllegalArgumentException().isThrownBy(() -> LocalDateDoubleTimeSeries.builder().put(
        LocalDateDoublePoint.of(DATE_2015_01_02, Double.NaN)));
    assertThatIllegalArgumentException().isThrownBy(() -> LocalDateDoubleTimeSeries.builder().putAll(
        ImmutableList.of(DATE_2015_01_02), ImmutableList.of(Double.NaN)));
    assertThatIllegalArgumentException().isThrownBy(() -> LocalDateDoubleTimeSeries.builder().putAll(
        ImmutableList.of(LocalDateDoublePoint.of(DATE_2015_01_02, Double.NaN))));

    LocalDateDoubleTimeSeries s1 = LocalDateDoubleTimeSeries.of(DATE_2015_01_02, 1d);
    LocalDateDoubleTimeSeries s2 = LocalDateDoubleTimeSeries.of(DATE_2015_01_02, 2d);

    assertThatIllegalArgumentException().isThrownBy(() -> s1.intersection(s2, (d1, d2) -> Double.NaN));

    assertThatIllegalArgumentException().isThrownBy(() -> s1.mapValues(d -> Double.NaN));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_map() {
    Map<LocalDate, Double> map = new HashMap<>();
    map.put(DATE_2011_01_01, 2d);
    map.put(DATE_2012_01_01, 3d);

    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.builder().putAll(map).build();
    assertThat(test.isEmpty()).isEqualTo(false);
    assertThat(test.size()).isEqualTo(2);
    assertThat(test.containsDate(DATE_2010_01_01)).isEqualTo(false);
    assertThat(test.containsDate(DATE_2011_01_01)).isEqualTo(true);
    assertThat(test.containsDate(DATE_2012_01_01)).isEqualTo(true);
    assertThat(test.get(DATE_2010_01_01)).isEqualTo(OptionalDouble.empty());
    assertThat(test.get(DATE_2011_01_01)).isEqualTo(OptionalDouble.of(2d));
    assertThat(test.get(DATE_2012_01_01)).isEqualTo(OptionalDouble.of(3d));
    assertThat(test.dates().toArray()).isEqualTo(new Object[] {DATE_2011_01_01, DATE_2012_01_01});
    assertThat(test.values().toArray()).isEqualTo(new double[] {2d, 3d});
  }

  @Test
  public void test_of_map_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LocalDateDoubleTimeSeries.builder().putAll((Map<LocalDate, Double>) null).build());
  }

  @Test
  public void test_of_map_empty() {
    LocalDateDoubleTimeSeries series = LocalDateDoubleTimeSeries.builder().putAll(ImmutableMap.of()).build();
    assertThat(series).isEqualTo(empty());
  }

  @Test
  public void test_of_map_dateNull() {
    Map<LocalDate, Double> map = new HashMap<>();
    map.put(DATE_2011_01_01, 2d);
    map.put(null, 3d);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> LocalDateDoubleTimeSeries.builder().putAll(map).build());
  }

  @Test
  public void test_of_map_valueNull() {
    Map<LocalDate, Double> map = new HashMap<>();
    map.put(DATE_2011_01_01, 2d);
    map.put(DATE_2012_01_01, null);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> LocalDateDoubleTimeSeries.builder().putAll(map).build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_collection() {
    Collection<LocalDateDoublePoint> points = Arrays.asList(
        LocalDateDoublePoint.of(DATE_2011_01_01, 2d),
        LocalDateDoublePoint.of(DATE_2012_01_01, 3d));

    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.builder().putAll(points.stream()).build();
    assertThat(test.isEmpty()).isEqualTo(false);
    assertThat(test.size()).isEqualTo(2);
    assertThat(test.containsDate(DATE_2010_01_01)).isEqualTo(false);
    assertThat(test.containsDate(DATE_2011_01_01)).isEqualTo(true);
    assertThat(test.containsDate(DATE_2012_01_01)).isEqualTo(true);
    assertThat(test.get(DATE_2010_01_01)).isEqualTo(OptionalDouble.empty());
    assertThat(test.get(DATE_2011_01_01)).isEqualTo(OptionalDouble.of(2d));
    assertThat(test.get(DATE_2012_01_01)).isEqualTo(OptionalDouble.of(3d));
    assertThat(test.dates().toArray()).isEqualTo(new Object[] {DATE_2011_01_01, DATE_2012_01_01});
    assertThat(test.values().toArray()).isEqualTo(new double[] {2d, 3d});
  }

  @Test
  public void test_of_collection_collectionNull() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LocalDateDoubleTimeSeries.builder().putAll((List<LocalDateDoublePoint>) null).build());
  }

  @Test
  public void test_of_collection_collectionWithNull() {
    Collection<LocalDateDoublePoint> points = Arrays.asList(
        LocalDateDoublePoint.of(DATE_2011_01_01, 2d), null);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> LocalDateDoubleTimeSeries.builder().putAll(points.stream()).build());
  }

  @Test
  public void test_of_collection_empty() {
    Collection<LocalDateDoublePoint> points = ImmutableList.of();

    assertThat(LocalDateDoubleTimeSeries.builder().putAll(points.stream()).build()).isEqualTo(empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_immutableViaBeanBuilder() {
    LocalDate startDate = DATE_2010_01_01;
    double[] values = {6, 5, 4};
    BeanBuilder<? extends DenseLocalDateDoubleTimeSeries> builder = DenseLocalDateDoubleTimeSeries.meta().builder();
    builder.set("startDate", startDate);
    builder.set("points", values);
    builder.set("dateCalculation", INCLUDE_WEEKENDS);
    DenseLocalDateDoubleTimeSeries test = builder.build();
    values[0] = -1;

    assertThat(test.stream())
        .containsExactly(
            LocalDateDoublePoint.of(DATE_2010_01_01, 6d),
            LocalDateDoublePoint.of(DATE_2010_01_02, 5d),
            LocalDateDoublePoint.of(DATE_2010_01_03, 4d));
  }

  @Test
  public void test_immutableValuesViaBeanGet() {

    LocalDateDoubleTimeSeries test =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();
    double[] array = (double[]) ((Bean) test).property("points").get();
    array[0] = -1;

    assertThat(test.stream())
        .containsSequence(
            LocalDateDoublePoint.of(DATE_2015_01_05, 10d),
            LocalDateDoublePoint.of(DATE_2015_01_06, 11d),
            LocalDateDoublePoint.of(DATE_2015_01_07, 12d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_earliestLatest() {

    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.builder().putAll(DATES_2010_12, VALUES_10_12).build();
    assertThat(test.getEarliestDate()).isEqualTo(DATE_2010_01_01);
    assertThat(test.getEarliestValue()).isEqualTo(10d, TOLERANCE);
    assertThat(test.getLatestDate()).isEqualTo(DATE_2012_01_01);
    assertThat(test.getLatestValue()).isEqualTo(12d, TOLERANCE);
  }

  @Test
  public void test_earliestLatest_whenEmpty() {
    LocalDateDoubleTimeSeries test = empty();
    assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(test::getEarliestDate);
    assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(test::getEarliestValue);
    assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(test::getLatestDate);
    assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(test::getLatestValue);
  }

  @Test
  public void test_earliest_with_subseries() {

    LocalDateDoubleTimeSeries series = LocalDateDoubleTimeSeries.builder()
        .put(DATE_2015_01_03, 3d) // Saturday, so include weekends
        .put(DATE_2015_01_05, 5d)
        .put(DATE_2015_01_06, 6d)
        .put(DATE_2015_01_07, 7d)
        .put(DATE_2015_01_08, 8d)
        .put(DATE_2015_01_09, 9d)
        .put(DATE_2015_01_11, 11d)
        .build();

    LocalDateDoubleTimeSeries subSeries = series.subSeries(DATE_2015_01_04, DATE_2015_01_11);
    assertThat(subSeries.getEarliestDate()).isEqualTo(DATE_2015_01_05);
    assertThat(subSeries.getEarliestValue()).isEqualTo(5d);
    assertThat(subSeries.getLatestDate()).isEqualTo(DATE_2015_01_09);
    assertThat(subSeries.getLatestValue()).isEqualTo(9d);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_subSeries() {
    return new Object[][] {
        // start = end -> empty
        {DATE_2011_01_01, DATE_2011_01_01, new int[] {}},
        // no overlap
        {date(2006, 1, 1), date(2009, 1, 1), new int[] {}},
        // single point
        {DATE_2015_01_06, DATE_2015_01_07, new int[] {1}},
        // include when start matches base, exclude when end matches base
        {DATE_2015_01_06, DATE_2015_01_08, new int[] {1, 2}},
        // include when start matches base
        {DATE_2015_01_05, DATE_2015_01_09, new int[] {0, 1, 2, 3}},
        // neither start nor end match
        {date(2014, 12, 31), date(2015, 2, 1), new int[] {0, 1, 2, 3, 4}},
    };
  }

  @ParameterizedTest
  @MethodSource("data_subSeries")
  public void test_subSeries(LocalDate start, LocalDate end, int[] expected) {

    LocalDateDoubleTimeSeries base =
        LocalDateDoubleTimeSeries.builder()
            .putAll(DATES_2015_1_WEEK, VALUES_1_WEEK)
            .build();

    LocalDateDoubleTimeSeries test = base.subSeries(start, end);

    assertThat(test.size()).isEqualTo(expected.length);
    for (int i = 0; i < DATES_2015_1_WEEK.size(); i++) {
      if (Arrays.binarySearch(expected, i) >= 0) {
        assertThat(test.get(DATES_2015_1_WEEK.get(i))).isEqualTo(OptionalDouble.of(VALUES_1_WEEK.get(i)));
      } else {
        assertThat(test.get(DATES_2015_1_WEEK.get(i))).isEqualTo(OptionalDouble.empty());
      }
    }
  }

  @ParameterizedTest
  @MethodSource("data_subSeries")
  public void test_subSeries_emptySeries(LocalDate start, LocalDate end, int[] expected) {
    LocalDateDoubleTimeSeries test = empty().subSeries(start, end);
    assertThat(test.size()).isEqualTo(0);
  }

  @Test
  public void test_subSeries_startAfterEnd() {

    LocalDateDoubleTimeSeries base =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();
    assertThatIllegalArgumentException().isThrownBy(() -> base.subSeries(date(2011, 1, 2), DATE_2011_01_01));
  }

  @Test
  public void test_subSeries_picks_valid_dates() {

    LocalDateDoubleTimeSeries series = LocalDateDoubleTimeSeries.builder()
        .put(DATE_2015_01_02, 10)  // Friday
        .put(DATE_2015_01_05, 11)  // Mon
        .put(DATE_2015_01_06, 12)
        .put(DATE_2015_01_07, 13)
        .put(DATE_2015_01_08, 14)
        .put(DATE_2015_01_09, 15)  // Fri
        .put(DATE_2015_01_12, 16)  // Mon
        .build();

    // Pick using weekend dates
    LocalDateDoubleTimeSeries subSeries = series.subSeries(DATE_2015_01_04, date(2015, 1, 10));

    assertThat(subSeries.size()).isEqualTo(5);
    assertThat(subSeries.get(DATE_2015_01_02)).isEqualTo(OptionalDouble.empty());
    assertThat(subSeries.get(DATE_2015_01_04)).isEqualTo(OptionalDouble.empty());
    assertThat(subSeries.get(DATE_2015_01_05)).isEqualTo(OptionalDouble.of(11));
    assertThat(subSeries.get(DATE_2015_01_06)).isEqualTo(OptionalDouble.of(12));
    assertThat(subSeries.get(DATE_2015_01_07)).isEqualTo(OptionalDouble.of(13));
    assertThat(subSeries.get(DATE_2015_01_08)).isEqualTo(OptionalDouble.of(14));
    assertThat(subSeries.get(DATE_2015_01_09)).isEqualTo(OptionalDouble.of(15));
    assertThat(subSeries.get(DATE_2015_01_12)).isEqualTo(OptionalDouble.empty());
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_headSeries() {
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

  @ParameterizedTest
  @MethodSource("data_headSeries")
  public void test_headSeries(int count, int[] expected) {

    LocalDateDoubleTimeSeries base =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();
    LocalDateDoubleTimeSeries test = base.headSeries(count);
    assertThat(test.size()).isEqualTo(expected.length);
    for (int i = 0; i < DATES_2015_1_WEEK.size(); i++) {
      if (Arrays.binarySearch(expected, i) >= 0) {
        assertThat(test.get(DATES_2015_1_WEEK.get(i))).isEqualTo(OptionalDouble.of(VALUES_1_WEEK.get(i)));
      } else {
        assertThat(test.get(DATES_2015_1_WEEK.get(i))).isEqualTo(OptionalDouble.empty());
      }
    }
  }

  @ParameterizedTest
  @MethodSource("data_headSeries")
  public void test_headSeries_emptySeries(int count, int[] expected) {
    LocalDateDoubleTimeSeries test = empty().headSeries(count);
    assertThat(test.size()).isEqualTo(0);
  }

  @Test
  public void test_headSeries_negative() {
    LocalDateDoubleTimeSeries base =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();
    assertThatIllegalArgumentException().isThrownBy(() -> base.headSeries(-1));
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_tailSeries() {
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

  @ParameterizedTest
  @MethodSource("data_tailSeries")
  public void test_tailSeries(int count, int[] expected) {
    LocalDateDoubleTimeSeries base =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();
    LocalDateDoubleTimeSeries test = base.tailSeries(count);
    assertThat(test.size()).isEqualTo(expected.length);
    for (int i = 0; i < DATES_2015_1_WEEK.size(); i++) {
      if (Arrays.binarySearch(expected, i) >= 0) {
        assertThat(test.get(DATES_2015_1_WEEK.get(i))).isEqualTo(OptionalDouble.of(VALUES_1_WEEK.get(i)));
      } else {
        assertThat(test.get(DATES_2015_1_WEEK.get(i))).isEqualTo(OptionalDouble.empty());
      }
    }
  }

  @ParameterizedTest
  @MethodSource("data_tailSeries")
  public void test_tailSeries_emptySeries(int count, int[] expected) {
    LocalDateDoubleTimeSeries test = empty().tailSeries(count);
    assertThat(test.size()).isEqualTo(0);
  }

  @Test
  public void test_tailSeries_negative() {
    LocalDateDoubleTimeSeries base =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();
    assertThatIllegalArgumentException().isThrownBy(() -> base.tailSeries(-1));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_stream() {
    LocalDateDoubleTimeSeries base =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2010_12, VALUES_10_12).build();
    assertThat(base.stream())
        .containsExactly(
            LocalDateDoublePoint.of(DATE_2010_01_01, 10d),
            LocalDateDoublePoint.of(DATE_2011_01_01, 11d),
            LocalDateDoublePoint.of(DATE_2012_01_01, 12d));
  }

  @Test
  public void test_stream_withCollector() {
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.builder().putAll(DATES_2010_12, VALUES_10_12).build();
    LocalDateDoubleTimeSeries test = base.stream()
        .map(point -> point.withValue(1.5d))
        .collect(LocalDateDoubleTimeSeries.collector());
    assertThat(test.size()).isEqualTo(3);
    assertThat(test.get(DATE_2010_01_01)).isEqualTo(OptionalDouble.of(1.5));
    assertThat(test.get(DATE_2011_01_01)).isEqualTo(OptionalDouble.of(1.5));
    assertThat(test.get(DATE_2012_01_01)).isEqualTo(OptionalDouble.of(1.5));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_dateStream() {
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.builder().putAll(DATES_2010_12, VALUES_10_12).build();
    assertThat(base.dates())
        .containsExactly(DATE_2010_01_01, DATE_2011_01_01, DATE_2012_01_01);
  }

  @Test
  public void test_valueStream() {
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.builder().putAll(DATES_2010_12, VALUES_10_12).build();
    assertThat(base.values())
        .containsExactly(10d, 11d, 12d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forEach() {
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();
    AtomicInteger counter = new AtomicInteger();
    base.forEach((date, value) -> counter.addAndGet((int) value));
    assertThat(counter.get()).isEqualTo(10 + 11 + 12 + 13 + 14);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_intersection_withNoMatchingElements() {

    LocalDateDoubleTimeSeries series1 =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();

    List<LocalDate> dates2 = dates(DATE_2010_06_01, DATE_2011_06_01, DATE_2012_06_01, DATE_2013_06_01, DATE_2014_06_01);

    LocalDateDoubleTimeSeries series2 =
        LocalDateDoubleTimeSeries.builder().putAll(dates2, VALUES_1_WEEK).build();

    LocalDateDoubleTimeSeries test = series1.intersection(series2, Double::sum);
    assertThat(test).isEqualTo(LocalDateDoubleTimeSeries.empty());
  }

  @Test
  public void test_intersection_withSomeMatchingElements() {

    LocalDateDoubleTimeSeries series1 =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();

    Map<LocalDate, Double> updates = ImmutableMap.of(
        DATE_2015_01_02, 1.0,
        DATE_2015_01_05, 1.1,
        DATE_2015_01_08, 1.2,
        DATE_2015_01_09, 1.3,
        DATE_2015_01_12, 1.4);

    LocalDateDoubleTimeSeries series2 =
        LocalDateDoubleTimeSeries.builder()
            .putAll(updates)
            .build();

    LocalDateDoubleTimeSeries test = series1.intersection(series2, Double::sum);
    assertThat(test.size()).isEqualTo(3);
    assertThat(test.get(DATE_2015_01_05)).isEqualTo(OptionalDouble.of(11.1));
    assertThat(test.get(DATE_2015_01_08)).isEqualTo(OptionalDouble.of(14.2));
    assertThat(test.get(DATE_2015_01_09)).isEqualTo(OptionalDouble.of(15.3));
  }

  @Test
  public void test_intersection_withSomeMatchingElements2() {
    List<LocalDate> dates1 = dates(DATE_2010_01_01, DATE_2011_01_01, DATE_2012_01_01, DATE_2014_01_01, DATE_2015_06_01);
    List<Double> values1 = values(10, 11, 12, 13, 14);

    LocalDateDoubleTimeSeries series1 = LocalDateDoubleTimeSeries.builder().putAll(dates1, values1).build();

    List<LocalDate> dates2 = dates(DATE_2010_01_01, DATE_2011_06_01, DATE_2012_01_01, DATE_2013_01_01, DATE_2014_01_01);
    List<Double> values2 = values(1.0, 1.1, 1.2, 1.3, 1.4);

    LocalDateDoubleTimeSeries series2 = LocalDateDoubleTimeSeries.builder().putAll(dates2, values2).build();

    LocalDateDoubleTimeSeries test = series1.intersection(series2, Double::sum);
    assertThat(test.size()).isEqualTo(3);
    assertThat(test.get(DATE_2010_01_01)).isEqualTo(OptionalDouble.of(11.0));
    assertThat(test.get(DATE_2012_01_01)).isEqualTo(OptionalDouble.of(13.2));
    assertThat(test.get(DATE_2014_01_01)).isEqualTo(OptionalDouble.of(14.4));
  }

  @Test
  public void test_intersection_withAllMatchingElements() {
    List<LocalDate> dates1 = DATES_2015_1_WEEK;
    List<Double> values1 = values(10, 11, 12, 13, 14);

    LocalDateDoubleTimeSeries series1 =
        LocalDateDoubleTimeSeries.builder().putAll(dates1, values1).build();
    List<LocalDate> dates2 = DATES_2015_1_WEEK;
    List<Double> values2 = values(1.0, 1.1, 1.2, 1.3, 1.4);

    LocalDateDoubleTimeSeries series2 =
        LocalDateDoubleTimeSeries.builder().putAll(dates2, values2).build();

    LocalDateDoubleTimeSeries combined = series1.intersection(series2, Double::sum);
    assertThat(combined.size()).isEqualTo(5);
    assertThat(combined.getEarliestDate()).isEqualTo(DATE_2015_01_05);
    assertThat(combined.getLatestDate()).isEqualTo(DATE_2015_01_09);
    assertThat(combined.get(DATE_2015_01_05)).isEqualTo(OptionalDouble.of(11.0));
    assertThat(combined.get(DATE_2015_01_06)).isEqualTo(OptionalDouble.of(12.1));
    assertThat(combined.get(DATE_2015_01_07)).isEqualTo(OptionalDouble.of(13.2));
    assertThat(combined.get(DATE_2015_01_08)).isEqualTo(OptionalDouble.of(14.3));
    assertThat(combined.get(DATE_2015_01_09)).isEqualTo(OptionalDouble.of(15.4));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_union_withMatchingElements() {
    List<LocalDate> dates1 = dates(DATE_2015_01_03, DATE_2015_01_05, DATE_2015_01_06);
    List<LocalDate> dates2 = dates(DATE_2015_01_02, DATE_2015_01_03, DATE_2015_01_05, DATE_2015_01_08);
    LocalDateDoubleTimeSeries series1 =
        LocalDateDoubleTimeSeries.builder().putAll(dates1, VALUES_10_12).build();
    LocalDateDoubleTimeSeries series2 =
        LocalDateDoubleTimeSeries.builder().putAll(dates2, VALUES_4_7).build();

    LocalDateDoubleTimeSeries test = series1.union(series2, Double::sum);
    assertThat(test.size()).isEqualTo(5);
    assertThat(test.getEarliestDate()).isEqualTo(DATE_2015_01_02);
    assertThat(test.getLatestDate()).isEqualTo(DATE_2015_01_08);
    assertThat(test.get(DATE_2015_01_02)).isEqualTo(OptionalDouble.of(4d));
    assertThat(test.get(DATE_2015_01_03)).isEqualTo(OptionalDouble.of(10d + 5d));
    assertThat(test.get(DATE_2015_01_05)).isEqualTo(OptionalDouble.of(11d + 6d));
    assertThat(test.get(DATE_2015_01_06)).isEqualTo(OptionalDouble.of(12d));
    assertThat(test.get(DATE_2015_01_08)).isEqualTo(OptionalDouble.of(7d));
  }

  @Test
  public void test_union_withNoMatchingElements() {
    List<LocalDate> dates1 = dates(DATE_2015_01_03, DATE_2015_01_05, DATE_2015_01_06);
    List<LocalDate> dates2 = dates(DATE_2015_01_02, DATE_2015_01_04, DATE_2015_01_08);
    LocalDateDoubleTimeSeries series1 =
        LocalDateDoubleTimeSeries.builder().putAll(dates1, VALUES_10_12).build();
    LocalDateDoubleTimeSeries series2 =
        LocalDateDoubleTimeSeries.builder().putAll(dates2, VALUES_1_3).build();

    LocalDateDoubleTimeSeries test = series1.union(series2, Double::sum);
    assertThat(test.size()).isEqualTo(6);
    assertThat(test.getEarliestDate()).isEqualTo(DATE_2015_01_02);
    assertThat(test.getLatestDate()).isEqualTo(DATE_2015_01_08);
    assertThat(test.get(DATE_2015_01_02)).isEqualTo(OptionalDouble.of(1d));
    assertThat(test.get(DATE_2015_01_03)).isEqualTo(OptionalDouble.of(10d));
    assertThat(test.get(DATE_2015_01_04)).isEqualTo(OptionalDouble.of(2d));
    assertThat(test.get(DATE_2015_01_05)).isEqualTo(OptionalDouble.of(11d));
    assertThat(test.get(DATE_2015_01_06)).isEqualTo(OptionalDouble.of(12d));
    assertThat(test.get(DATE_2015_01_08)).isEqualTo(OptionalDouble.of(3d));
  }

  @Test
  public void test_union_weekends() {
    List<LocalDate> dates1 = dates(DATE_2015_01_03, DATE_2015_01_05, DATE_2015_01_11);  // start/end on weekend
    List<LocalDate> dates2 = dates(DATE_2015_01_02, DATE_2015_01_05, DATE_2015_01_12);  // no weekend
    LocalDateDoubleTimeSeries series1 =
        LocalDateDoubleTimeSeries.builder().putAll(dates1, VALUES_10_12).build();
    LocalDateDoubleTimeSeries series2 =
        LocalDateDoubleTimeSeries.builder().putAll(dates2, VALUES_1_3).build();

    LocalDateDoubleTimeSeries test = series1.union(series2, Double::sum);
    LocalDateDoubleTimeSeries test2 = series2.union(series1, Double::sum);
    assertThat(test.size()).isEqualTo(5);
    assertThat(test).isEqualTo(test2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_mapValues_addConstantToSeries() {

    LocalDateDoubleTimeSeries base =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();
    LocalDateDoubleTimeSeries test = base.mapValues(d -> d + 5);
    List<Double> expectedValues = values(15, 16, 17, 18, 19);

    assertThat(test).isEqualTo(LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, expectedValues).build());
  }

  @Test
  public void test_mapValues_multiplySeries() {

    LocalDateDoubleTimeSeries base =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();

    LocalDateDoubleTimeSeries test = base.mapValues(d -> d * 5);
    List<Double> expectedValues = values(50, 55, 60, 65, 70);

    assertThat(test).isEqualTo(LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, expectedValues).build());
  }

  @Test
  public void test_mapValues_invertSeries() {
    List<Double> values = values(1, 2, 4, 5, 8);

    LocalDateDoubleTimeSeries base =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, values).build();
    LocalDateDoubleTimeSeries test = base.mapValues(d -> 1 / d);
    List<Double> expectedValues = values(1, 0.5, 0.25, 0.2, 0.125);

    assertThat(test).isEqualTo(LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, expectedValues).build());
  }

  @Test
  public void test_mapDates() {
    List<Double> values = values(1, 2, 4, 5, 8);
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, values).build();
    LocalDateDoubleTimeSeries test = base.mapDates(date -> date.plusYears(1));
    ImmutableList<LocalDate> expectedDates =
        ImmutableList.of(date(2016, 1, 5), date(2016, 1, 6), date(2016, 1, 7), date(2016, 1, 8), date(2016, 1, 9));
    LocalDateDoubleTimeSeries expected = LocalDateDoubleTimeSeries.builder().putAll(expectedDates, values).build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_mapDates_notAscending() {
    List<Double> values = values(1, 2, 4, 5, 8);
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, values).build();
    assertThatIllegalArgumentException().isThrownBy(() -> base.mapDates(date -> date(2016, 1, 6)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_filter_byDate() {
    List<LocalDate> dates = dates(DATE_2010_01_01, DATE_2011_06_01, DATE_2012_01_01, DATE_2013_06_01, DATE_2014_01_01);

    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.builder().putAll(dates, VALUES_1_WEEK).build();
    LocalDateDoubleTimeSeries test = base.filter((ld, v) -> ld.getMonthValue() != 6);
    assertThat(test.size()).isEqualTo(3);
    assertThat(test.get(DATE_2010_01_01)).isEqualTo(OptionalDouble.of(10d));
    assertThat(test.get(DATE_2012_01_01)).isEqualTo(OptionalDouble.of(12d));
    assertThat(test.get(DATE_2014_01_01)).isEqualTo(OptionalDouble.of(14d));
  }

  @Test
  public void test_filter_byValue() {

    LocalDateDoubleTimeSeries base =
        LocalDateDoubleTimeSeries.builder().putAll(DATES_2015_1_WEEK, VALUES_1_WEEK).build();
    LocalDateDoubleTimeSeries test = base.filter((ld, v) -> v % 2 == 1);
    assertThat(test.size()).isEqualTo(2);
    assertThat(test.get(DATE_2015_01_06)).isEqualTo(OptionalDouble.of(11d));
    assertThat(test.get(DATE_2015_01_08)).isEqualTo(OptionalDouble.of(13d));
  }

  @Test
  public void test_filter_byDateAndValue() {
    List<LocalDate> dates = dates(DATE_2010_01_01, DATE_2011_06_01, DATE_2012_01_01, DATE_2013_06_01, DATE_2014_01_01);

    LocalDateDoubleTimeSeries series = LocalDateDoubleTimeSeries.builder().putAll(dates, VALUES_1_WEEK).build();

    LocalDateDoubleTimeSeries test = series.filter((ld, v) -> ld.getYear() >= 2012 && v % 2 == 0);
    assertThat(test.size()).isEqualTo(2);
    assertThat(test.get(DATE_2012_01_01)).isEqualTo(OptionalDouble.of(12d));
    assertThat(test.get(DATE_2014_01_01)).isEqualTo(OptionalDouble.of(14d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equals_similarSeriesAreEqual() {
    LocalDateDoubleTimeSeries series1 = LocalDateDoubleTimeSeries.of(DATE_2014_01_01, 1d);

    LocalDateDoubleTimeSeries series2 =
        LocalDateDoubleTimeSeries.builder().putAll(dates(DATE_2014_01_01), values(1d)).build();
    assertThat(series1.size()).isEqualTo(1);
    assertThat(series1).isEqualTo(series2);
    assertThat(series1).isEqualTo(series1);
    assertThat(series1.hashCode()).isEqualTo(series1.hashCode());
  }

  @Test
  public void test_equals_notEqual() {
    LocalDateDoubleTimeSeries series1 = LocalDateDoubleTimeSeries.of(DATE_2014_01_01, 1d);
    LocalDateDoubleTimeSeries series2 = LocalDateDoubleTimeSeries.of(DATE_2013_06_01, 1d);
    LocalDateDoubleTimeSeries series3 = LocalDateDoubleTimeSeries.of(DATE_2014_01_01, 3d);
    assertThat(series1).isNotEqualTo(series2);
    assertThat(series1).isNotEqualTo(series3);
  }

  @Test
  public void test_equals_bad() {
    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.of(DATE_2014_01_01, 1d);
    assertThat(test.equals(ANOTHER_TYPE)).isEqualTo(false);
    assertThat(test.equals(null)).isEqualTo(false);
  }

  @Test
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

    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.builder().putAll(map).build();
    assertThat(ts.get(dt(2014, 12, 26))).hasValue(14d);
    assertThat(ts.get(dt(2014, 12, 27))).isEmpty();
    assertThat(ts.get(dt(2014, 12, 28))).isEmpty();
    assertThat(ts.get(dt(2014, 12, 29))).hasValue(13d);
    assertThat(ts.get(dt(2014, 12, 30))).hasValue(12d);
    assertThat(ts.get(dt(2014, 12, 31))).hasValue(11d);
    assertThat(ts.get(dt(2015, 1, 1))).isEmpty();
    assertThat(ts.get(dt(2015, 1, 2))).hasValue(11d);
    assertThat(ts.get(dt(2015, 1, 3))).isEmpty();
    assertThat(ts.get(dt(2015, 1, 4))).isEmpty();
    assertThat(ts.get(dt(2015, 1, 5))).hasValue(12d);
    assertThat(ts.get(dt(2015, 1, 6))).hasValue(13d);
    assertThat(ts.get(dt(2015, 1, 7))).hasValue(14d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_coverage() {
    TestHelper.coverImmutableBean(
        (ImmutableBean) DenseLocalDateDoubleTimeSeries.of(DATE_2015_01_05, DATE_2015_01_05,
            Stream.of(LocalDateDoublePoint.of(DATE_2015_01_05, 1d)), SKIP_WEEKENDS));
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

  @Test
  public void checkOffsetsSkipWeekends() {

    Map<LocalDate, Double> map = ImmutableMap.<LocalDate, Double>builder()
        .put(dt(2014, 12, 26), 14d)
        // Weekend
        .put(dt(2014, 12, 29), 13d)
        .put(dt(2014, 12, 30), 12d)
        .put(dt(2014, 12, 31), 11d)
        // bank hol, so no data
        .put(dt(2015, 1, 2), 11d)
        // Weekend, so no data
        .put(dt(2015, 1, 5), 12d)
        .put(dt(2015, 1, 6), 13d)
        .put(dt(2015, 1, 7), 14d)
        .build();

    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.builder().putAll(map).build();
    assertThat(ts.get(dt(2014, 12, 26))).hasValue(14d);
    assertThat(ts.get(dt(2014, 12, 29))).hasValue(13d);
    assertThat(ts.get(dt(2014, 12, 30))).hasValue(12d);
    assertThat(ts.get(dt(2014, 12, 31))).hasValue(11d);
    assertThat(ts.get(dt(2015, 1, 1))).isEmpty();
    assertThat(ts.get(dt(2015, 1, 2))).hasValue(11d);
    assertThat(ts.get(dt(2015, 1, 3))).isEmpty();
    assertThat(ts.get(dt(2015, 1, 4))).isEmpty();
    assertThat(ts.get(dt(2015, 1, 5))).hasValue(12d);
    assertThat(ts.get(dt(2015, 1, 6))).hasValue(13d);
    assertThat(ts.get(dt(2015, 1, 7))).hasValue(14d);
  }

  @Test
  public void underOneWeekNoWeekend() {

    Map<LocalDate, Double> map = ImmutableMap.<LocalDate, Double>builder()
        .put(dt(2015, 1, 5), 12d) // Monday
        .put(dt(2015, 1, 6), 13d)
        .put(dt(2015, 1, 7), 14d)
        .put(dt(2015, 1, 8), 15d)
        .put(dt(2015, 1, 9), 16d) // Friday
        .build();

    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.builder().putAll(map).build();
    assertThat(ts.get(dt(2015, 1, 5))).hasValue(12d);
    assertThat(ts.get(dt(2015, 1, 9))).hasValue(16d);
  }

  @Test
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

    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.builder().putAll(map).build();
    assertThat(ts.get(dt(2015, 1, 1))).hasValue(10d);
    assertThat(ts.get(dt(2015, 1, 2))).hasValue(11d);
    assertThat(ts.get(dt(2015, 1, 5))).hasValue(12d);
    assertThat(ts.get(dt(2015, 1, 6))).hasValue(13d);
    assertThat(ts.get(dt(2015, 1, 7))).hasValue(14d);
    assertThat(ts.get(dt(2015, 1, 8))).hasValue(15d);
    assertThat(ts.get(dt(2015, 1, 9))).hasValue(16d);
  }

  @Test
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

    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.builder().putAll(in).build();

    Map<LocalDate, Double> out = ts.stream()
        .collect(Guavate.toImmutableMap(LocalDateDoublePoint::getDate, LocalDateDoublePoint::getValue));
    assertThat(out).isEqualTo(in);
  }

  @Test
  public void partitionEmptySeries() {

    Pair<LocalDateDoubleTimeSeries, LocalDateDoubleTimeSeries> partitioned =
        LocalDateDoubleTimeSeries.empty().partition((ld, d) -> ld.getYear() == 2015);

    assertThat(partitioned.getFirst()).isEqualTo(LocalDateDoubleTimeSeries.empty());
    assertThat(partitioned.getSecond()).isEqualTo(LocalDateDoubleTimeSeries.empty());
  }

  @Test
  public void partitionSeries() {

    Map<LocalDate, Double> in = ImmutableMap.<LocalDate, Double>builder()
        .put(dt(2015, 1, 1), 10d) // Thursday
        .put(dt(2015, 1, 2), 11d) // Friday
        .put(dt(2015, 1, 5), 12d)
        .put(dt(2015, 1, 6), 13d)
        .put(dt(2015, 1, 7), 14d)
        .put(dt(2015, 1, 8), 15d) // Thursday
        .put(dt(2015, 1, 9), 16d) // Friday
        .build();

    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.builder().putAll(in).build();

    Pair<LocalDateDoubleTimeSeries, LocalDateDoubleTimeSeries> partitioned =
        ts.partition((ld, d) -> ld.getDayOfMonth() % 2 == 0);

    LocalDateDoubleTimeSeries even = partitioned.getFirst();
    LocalDateDoubleTimeSeries odd = partitioned.getSecond();

    assertThat(even.size()).isEqualTo(3);
    assertThat(even.get(dt(2015, 1, 2))).hasValue(11d);
    assertThat(even.get(dt(2015, 1, 6))).hasValue(13d);
    assertThat(even.get(dt(2015, 1, 8))).hasValue(15d);

    assertThat(odd.size()).isEqualTo(4);
    assertThat(odd.get(dt(2015, 1, 1))).hasValue(10d);
    assertThat(odd.get(dt(2015, 1, 5))).hasValue(12d);
    assertThat(odd.get(dt(2015, 1, 7))).hasValue(14d);
    assertThat(odd.get(dt(2015, 1, 9))).hasValue(16d);
  }

  @Test
  public void partitionByValueEmptySeries() {

    Pair<LocalDateDoubleTimeSeries, LocalDateDoubleTimeSeries> partitioned =
        LocalDateDoubleTimeSeries.empty().partitionByValue(d -> d > 10);

    assertThat(partitioned.getFirst()).isEqualTo(LocalDateDoubleTimeSeries.empty());
    assertThat(partitioned.getSecond()).isEqualTo(LocalDateDoubleTimeSeries.empty());
  }

  @Test
  public void partitionByValueSeries() {

    Map<LocalDate, Double> in = ImmutableMap.<LocalDate, Double>builder()
        .put(dt(2015, 1, 1), 10d) // Thursday
        .put(dt(2015, 1, 2), 11d) // Friday
        .put(dt(2015, 1, 5), 12d)
        .put(dt(2015, 1, 6), 13d)
        .put(dt(2015, 1, 7), 14d)
        .put(dt(2015, 1, 8), 15d) // Thursday
        .put(dt(2015, 1, 9), 16d) // Friday
        .build();

    LocalDateDoubleTimeSeries ts = LocalDateDoubleTimeSeries.builder().putAll(in).build();

    Pair<LocalDateDoubleTimeSeries, LocalDateDoubleTimeSeries> partitioned =
        ts.partitionByValue(d -> d < 12 || d > 15);

    LocalDateDoubleTimeSeries extreme = partitioned.getFirst();
    LocalDateDoubleTimeSeries mid = partitioned.getSecond();

    assertThat(extreme.size()).isEqualTo(3);
    assertThat(extreme.get(dt(2015, 1, 1))).hasValue(10d);
    assertThat(extreme.get(dt(2015, 1, 2))).hasValue(11d);
    assertThat(extreme.get(dt(2015, 1, 9))).hasValue(16d);

    assertThat(mid.size()).isEqualTo(4);
    assertThat(mid.get(dt(2015, 1, 5))).hasValue(12d);
    assertThat(mid.get(dt(2015, 1, 6))).hasValue(13d);
    assertThat(mid.get(dt(2015, 1, 7))).hasValue(14d);
    assertThat(mid.get(dt(2015, 1, 8))).hasValue(15d);
  }

  private LocalDate dt(int yr, int mth, int day) {
    return LocalDate.of(yr, mth, day);
  }
}
