/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.collect.timeseries;

import java.time.LocalDate;
import java.util.OptionalDouble;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.opengamma.collect.validate.ArgChecker;

/**
 * Builder to create the immutable {@code LocalDateDoubleTimeSeries}.
 * <p>
 * This builder allows a time-series to be created.
 * Entries can be added to the builder in any order.
 * If a date is duplicated it will overwrite an earlier entry.
 * <p>
 * Use {@link LocalDateDoubleTimeSeries#builder()} to create an instance.
 */
public final class LocalDateDoubleTimeSeriesBuilder {

  /**
   * The entries for the time-series.
   * They will be sorted when the time-series is created.
   */
  private final SortedMap<LocalDate, Double> entries = new TreeMap<>();

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * <p>
   * Use {@link LocalDateDoubleTimeSeries#builder()}.
   */
  LocalDateDoubleTimeSeriesBuilder() {
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the specified date from the builder.
   *
   * @param date  the date to be added
   * @param value  the value for the time
   * @return the value, null if 
   */
  public OptionalDouble get(LocalDate date) {
    Double value = entries.get(date);
    return (value != null ? OptionalDouble.of(value) : OptionalDouble.empty());
  }

  //-------------------------------------------------------------------------
  /**
   * Puts the specified date/value point into this builder.
   *
   * @param date  the date to be added
   * @param value  the value associated with the date
   * @return this builder
   */
  public LocalDateDoubleTimeSeriesBuilder put(LocalDate date, double value) {
    ArgChecker.notNull(date, "date");
    entries.put(date, value);
    return this;
  }

  /**
   * Puts the specified date/value point into this builder.
   *
   * @param point  the point to be added
   * @return this builder
   */
  public LocalDateDoubleTimeSeriesBuilder put(LocalDateDoublePoint point) {
    ArgChecker.notNull(point, "point");
    entries.put(point.getDate(), point.getValue());
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Puts all the specified dates and values into this builder.
   * <p>
   * The dates and values arrays must be the same size.
   * <p>
   * The date-value pairs are added one by one.
   * If a date is duplicated it will overwrite an earlier entry.
   *
   * @param dates  the dates to be added
   * @param values  the values to be added
   * @return this builder
   */
  public LocalDateDoubleTimeSeriesBuilder putAll(LocalDate[] dates, double[] values) {
    ArgChecker.isTrue(dates.length == values.length,
        "Arrays are of different sizes - dates: {}, values: {}", dates.length, values.length);
    for (int i = 0; i < dates.length; i++) {
      entries.put(dates[i], values[i]);
    }
    return this;
  }

  /**
   * Puts all the specified points into this builder.
   * <p>
   * The points are added one by one.
   * If a date is duplicated it will overwrite an earlier entry.
   *
   * @param points  the points to be added
   * @return this builder
   */
  public LocalDateDoubleTimeSeriesBuilder putAll(Stream<LocalDateDoublePoint> points) {
    ArgChecker.notNull(points, "points");
    points.forEachOrdered(point -> put(point));
    return this;
  }

  /**
   * Puts the contents of the specified builder into this builder.
   * <p>
   * The points are added one by one.
   * If a date is duplicated it will overwrite an earlier entry.
   *
   * @param other  the other builder
   * @return this builder
   */
  public LocalDateDoubleTimeSeriesBuilder putAll(LocalDateDoubleTimeSeriesBuilder other) {
    ArgChecker.notNull(other, "other");
    entries.putAll(other.entries);
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Build the time-series from the builder.
   *
   * @return a time-series containing the entries from the builder
   */
  public LocalDateDoubleTimeSeries build() {
    return LocalDateDoubleTimeSeries.of(entries.keySet(), entries.values());
  }

}
