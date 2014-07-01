/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.collect.timeseries;

import static com.opengamma.collect.timeseries.LocalDateDoubleTimeSeries.EMPTY_SERIES;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.OptionalDouble;

import org.testng.annotations.Test;

/**
 * Test LocalDateDoubleTimeSeriesBuilder.
 */
@Test
public class LocalDateDoubleTimeSeriesBuilderTest {

  @Test
  public void test_buildEmptySeries() {
    assertEquals(LocalDateDoubleTimeSeries.builder().build(), EMPTY_SERIES);
  }

  //-------------------------------------------------------------------------
  public void test_get() {
    LocalDateDoubleTimeSeriesBuilder test = LocalDateDoubleTimeSeries.builder()
        .put(date(2014, 1, 1), 14)
        .put(date(2012, 1, 1), 12)
        .put(date(2013, 1, 1), 13);

    assertEquals(test.get(date(2012, 1, 1)), OptionalDouble.of(12d));
    assertEquals(test.get(date(2013, 1, 1)), OptionalDouble.of(13d));
    assertEquals(test.get(date(2014, 1, 1)), OptionalDouble.of(14d));
    assertEquals(test.get(date(2015, 1, 1)), OptionalDouble.empty());
  }

  //-------------------------------------------------------------------------
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_putAll_arraysMismatch() {
    LocalDateDoubleTimeSeriesBuilder test = LocalDateDoubleTimeSeries.builder();
    LocalDate[] dates = {date(2014, 1, 1)};
    double[] values = {2d, 3d};
    test.putAll(dates, values);
  }

  //-------------------------------------------------------------------------
  public void test_putAll_stream() {
    LocalDate[] dates = {date(2013, 1, 1), date(2014, 1, 1)};
    double[] values = {2d, 3d};
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.of(dates, values);
    
    LocalDateDoubleTimeSeriesBuilder test = LocalDateDoubleTimeSeries.builder();
    test.put(date(2012, 1, 1), 0d);
    test.put(date(2013, 1, 1), 1d);
    test.putAll(base.stream());

    assertEquals(test.get(date(2012, 1, 1)), OptionalDouble.of(0d));
    assertEquals(test.get(date(2013, 1, 1)), OptionalDouble.of(2d));
    assertEquals(test.get(date(2014, 1, 1)), OptionalDouble.of(3d));
  }

  public void test_putAll_toBuilder() {
    LocalDate[] dates = {date(2013, 1, 1), date(2014, 1, 1)};
    double[] values = {2d, 3d};
    LocalDateDoubleTimeSeries base = LocalDateDoubleTimeSeries.of(dates, values);
    
    LocalDateDoubleTimeSeriesBuilder test = LocalDateDoubleTimeSeries.builder();
    test.put(date(2012, 1, 1), 0d);
    test.put(date(2013, 1, 1), 1d);
    test.putAll(base.toBuilder());

    assertEquals(test.get(date(2012, 1, 1)), OptionalDouble.of(0d));
    assertEquals(test.get(date(2013, 1, 1)), OptionalDouble.of(2d));
    assertEquals(test.get(date(2014, 1, 1)), OptionalDouble.of(3d));
  }

  //-------------------------------------------------------------------------
  public void test_seriesGetsSorted() {
    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.builder()
        .put(date(2014, 1, 1), 14)
        .put(date(2012, 1, 1), 12)
        .put(date(2013, 1, 1), 13)
        .build();

    assertEquals(test.size(), 3);
    assertEquals(test.getEarliestDate(), date(2012, 1, 1));
    assertEquals(test.getLatestDate(), date(2014, 1, 1));
    assertEquals(test.get(date(2012, 1, 1)), OptionalDouble.of(12d));
    assertEquals(test.get(date(2013, 1, 1)), OptionalDouble.of(13d));
    assertEquals(test.get(date(2014, 1, 1)), OptionalDouble.of(14d));
  }

  public void test_duplicatesGetOverwritten() {
    LocalDateDoubleTimeSeries test = LocalDateDoubleTimeSeries.builder()
        .put(date(2014, 1, 1), 12)
        .put(date(2014, 1, 1), 14)
        .build();

    assertEquals(test.size(), 1);
    assertEquals(test.get(date(2014, 1, 1)), OptionalDouble.of(14d));
  }

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

    assertEquals(test.size(), 4);
    assertEquals(test.getEarliestDate(), date(2011, 1, 1));
    assertEquals(test.getLatestDate(), date(2014, 1, 1));
    // new value
    assertEquals(test.get(date(2011, 1, 1)), OptionalDouble.of(21d));
    assertEquals(test.get(date(2012, 1, 1)), OptionalDouble.of(12d));
    // updated value
    assertEquals(test.get(date(2013, 1, 1)), OptionalDouble.of(23d));
    assertEquals(test.get(date(2014, 1, 1)), OptionalDouble.of(14d));
  }

  //-------------------------------------------------------------------------
  private static LocalDate date(int year, int month, int day) {
    return LocalDate.of(year, month, day);
  }

}
