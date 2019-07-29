/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.timeseries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Test;

import com.google.common.primitives.Doubles;

/**
 * Test {@link LocalDateDoubleTimeSeriesBuilder}.
 */
public class LocalDateDoubleTimeSeriesBuilderTest {

  @Test
  public void test_buildEmptySeries() {
    assertThat(LocalDateDoubleTimeSeries.builder().build()).isEqualTo(LocalDateDoubleTimeSeries.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_get() {
    LocalDateDoubleTimeSeriesBuilder test = LocalDateDoubleTimeSeries.builder()
        .put(date(2014, 1, 1), 14)
        .put(date(2012, 1, 1), 12)
        .put(date(2013, 1, 1), 13);

    assertThat(test.get(date(2012, 1, 1))).hasValue(12d);
    assertThat(test.get(date(2013, 1, 1))).hasValue(13d);
    assertThat(test.get(date(2014, 1, 1))).hasValue(14d);
    assertThat(test.get(date(2015, 1, 1))).isEmpty();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_merge_dateValue() {
    LocalDateDoubleTimeSeriesBuilder test = LocalDateDoubleTimeSeries.builder();
    test.put(date(2013, 1, 1), 2d);
    test.merge(date(2013, 1, 1), 3d, Double::sum);

    assertThat(test.get(date(2013, 1, 1))).hasValue(5d);
  }

  @Test
  public void test_merge_point() {
    LocalDateDoubleTimeSeriesBuilder test = LocalDateDoubleTimeSeries.builder();
    test.put(date(2013, 1, 1), 2d);
    test.merge(LocalDateDoublePoint.of(date(2013, 1, 1), 3d), Double::sum);

    assertThat(test.get(date(2013, 1, 1))).hasValue(5d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_putAll_collections() {
    Collection<LocalDate> dates = Arrays.asList(date(2013, 1, 1), date(2014, 1, 1));
    Collection<Double> values = Doubles.asList(2d, 3d);
    LocalDateDoubleTimeSeriesBuilder test = LocalDateDoubleTimeSeries.builder();
    test.putAll(dates, values);

    assertThat(test.get(date(2013, 1, 1))).hasValue(2d);
    assertThat(test.get(date(2014, 1, 1))).hasValue(3d);
  }

  @Test
  public void test_putAll_collection_array() {
    Collection<LocalDate> dates = Arrays.asList(date(2013, 1, 1), date(2014, 1, 1));
    double[] values = new double[] {2d, 3d};
    LocalDateDoubleTimeSeriesBuilder test = LocalDateDoubleTimeSeries.builder();
    test.putAll(dates, values);

    assertThat(test.get(date(2013, 1, 1))).hasValue(2d);
    assertThat(test.get(date(2014, 1, 1))).hasValue(3d);
  }

  @Test
  public void test_putAll_collectionsMismatch() {
    LocalDateDoubleTimeSeriesBuilder test = LocalDateDoubleTimeSeries.builder();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.putAll(Arrays.asList(date(2014, 1, 1)), Doubles.asList(2d, 3d)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_putAll_stream() {
    Collection<LocalDate> dates = Arrays.asList(date(2013, 1, 1), date(2014, 1, 1));
    Collection<Double> values = Doubles.asList(2d, 3d);
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.builder().putAll(dates, values).build();

    LocalDateDoubleTimeSeriesBuilder test = LocalDateDoubleTimeSeries.builder();
    test.put(date(2012, 1, 1), 0d);
    test.put(date(2013, 1, 1), 1d);
    test.putAll(base.stream());

    assertThat(test.get(date(2012, 1, 1))).hasValue(0d);
    assertThat(test.get(date(2013, 1, 1))).hasValue(2d);
    assertThat(test.get(date(2014, 1, 1))).hasValue(3d);
  }

  @Test
  public void test_putAll_toBuilder() {
    Collection<LocalDate> dates = Arrays.asList(date(2013, 1, 1), date(2014, 1, 1));
    Collection<Double> values = Doubles.asList(2d, 3d);
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.builder().putAll(dates, values).build();

    LocalDateDoubleTimeSeriesBuilder test = LocalDateDoubleTimeSeries.builder();
    test.put(date(2012, 1, 1), 0d);
    test.put(date(2013, 1, 1), 1d);
    test.putAll(base.toBuilder());

    assertThat(test.get(date(2012, 1, 1))).hasValue(0d);
    assertThat(test.get(date(2013, 1, 1))).hasValue(2d);
    assertThat(test.get(date(2014, 1, 1))).hasValue(3d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_seriesGetsSorted() {
    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.builder()
        .put(date(2014, 1, 1), 14)
        .put(date(2012, 1, 1), 12)
        .put(date(2013, 1, 1), 13)
        .build();

    assertThat(test.size()).isEqualTo(3);
    assertThat(test.getEarliestDate()).isEqualTo(date(2012, 1, 1));
    assertThat(test.getLatestDate()).isEqualTo(date(2014, 1, 1));
    assertThat(test.get(date(2012, 1, 1))).hasValue(12d);
    assertThat(test.get(date(2013, 1, 1))).hasValue(13d);
    assertThat(test.get(date(2014, 1, 1))).hasValue(14d);
  }

  @Test
  public void test_duplicatesGetOverwritten() {
    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.builder()
        .put(date(2014, 1, 1), 12)
        .put(date(2014, 1, 1), 14)
        .build();

    assertThat(test.size()).isEqualTo(1);
    assertThat(test.get(date(2014, 1, 1))).hasValue(14d);
  }

  @Test
  public void test_useBuilderToAlterSeries() {
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.builder()
        .put(date(2014, 1, 1), 14)
        .put(date(2012, 1, 1), 12)
        .put(date(2013, 1, 1), 13)
        .build();
    LocalDateDoubleTimeSeries test = base.toBuilder()
        .put(date(2013, 1, 1), 23)
        .put(date(2011, 1, 1), 21)
        .build();

    assertThat(test.size()).isEqualTo(4);
    assertThat(test.getEarliestDate()).isEqualTo(date(2011, 1, 1));
    assertThat(test.getLatestDate()).isEqualTo(date(2014, 1, 1));
    // new value
    assertThat(test.get(date(2011, 1, 1))).hasValue(21d);
    assertThat(test.get(date(2012, 1, 1))).hasValue(12d);
    // updated value
    assertThat(test.get(date(2013, 1, 1))).hasValue(23d);
    assertThat(test.get(date(2014, 1, 1))).hasValue(14d);
  }

  @Test
  public void densityChoosesImplementation() {
    LocalDateDoubleTimeSeries series1 = LocalDateDoubleTimeSeries.builder()
        .put(date(2015, 1, 5), 14) // Monday
        .put(date(2015, 1, 12), 12)
        .put(date(2015, 1, 19), 13)
        .build();

    assertThat(series1.getClass()).isEqualTo(SparseLocalDateDoubleTimeSeries.class);

    // Now add in a week's worth of data
    LocalDateDoubleTimeSeries series2 = series1.toBuilder()
        .put(date(2015, 1, 6), 14)
        .put(date(2015, 1, 7), 13)
        .put(date(2015, 1, 8), 12)
        .put(date(2015, 1, 9), 13)
        .build();

    // Not yet enough as we have 7/11 populated (i.e. below 70%)
    assertThat(series2.getClass()).isEqualTo(SparseLocalDateDoubleTimeSeries.class);

    // Add in 1 more days giving 8/11 populated
    LocalDateDoubleTimeSeries series3 = series2.toBuilder()
        .put(date(2015, 1, 13), 11)
        .build();

    assertThat(series3.getClass()).isEqualTo(DenseLocalDateDoubleTimeSeries.class);

    // Now add in a weekend date, which means we have 9/15
    LocalDateDoubleTimeSeries series4 = series3.toBuilder()
        .put(date(2015, 1, 10), 12) // Saturday
        .build();

    assertThat(series4.getClass()).isEqualTo(SparseLocalDateDoubleTimeSeries.class);

    // Add in 2 new dates giving 11/15
    LocalDateDoubleTimeSeries series5 = series4.toBuilder()
        .put(date(2015, 1, 14), 11)
        .put(date(2015, 1, 15), 10)
        .build();

    assertThat(series5.getClass()).isEqualTo(DenseLocalDateDoubleTimeSeries.class);
  }

  //-------------------------------------------------------------------------
  private static LocalDate date(int year, int month, int day) {
    return LocalDate.of(year, month, day);
  }

}
