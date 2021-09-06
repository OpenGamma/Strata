/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.timeseries;

import static com.opengamma.strata.collect.timeseries.DenseLocalDateDoubleTimeSeries.DenseTimeSeriesCalculation.INCLUDE_WEEKENDS;
import static com.opengamma.strata.collect.timeseries.DenseLocalDateDoubleTimeSeries.DenseTimeSeriesCalculation.SKIP_WEEKENDS;

import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.DoubleBinaryOperator;
import java.util.stream.Stream;

import com.opengamma.strata.collect.ArgChecker;

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
   * Threshold for deciding whether we use the dense or sparse time-series implementation.
   */
  private static final double DENSITY_THRESHOLD = 0.7;

  /**
   * The entries for the time-series.
   */
  private final SortedMap<LocalDate, Double> entries = new TreeMap<>();

  /**
   * Keep track of whether we have weekends in the data.
   */
  private boolean containsWeekends;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance.
   * <p>
   * Use {@link LocalDateDoubleTimeSeries#builder()}.
   */
  LocalDateDoubleTimeSeriesBuilder() {
  }

  /**
   * Creates an instance.
   * <p>
   * Use {@link LocalDateDoubleTimeSeries#toBuilder()}.
   * 
   * @param dates  the dates to initialize with
   * @param values  the values to initialize with
   */
  LocalDateDoubleTimeSeriesBuilder(LocalDate[] dates, double[] values) {
    for (int i = 0; i < dates.length; i++) {
      put(dates[i], values[i]);
    }
  }

  /**
   * Creates an instance.
   * <p>
   * Use {@link DenseLocalDateDoubleTimeSeries#toBuilder()}.
   *
   * @param points  the stream of points to initialize with
   */
  LocalDateDoubleTimeSeriesBuilder(Stream<LocalDateDoublePoint> points) {
    points.forEach(pt -> put(pt.getDate(), pt.getValue()));
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the value associated with the specified date.
   * <p>
   * The result is an {@link OptionalDouble} which avoids the need to handle null
   * or exceptions. Use {@code isPresent()} to check whether the value is present.
   * Use {@code orElse(double)} to default a missing value.
   *
   * @param date  the date to get the value for
   * @return the value associated with the date, optional empty if the date is not present
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
    ArgChecker.isFalse(Double.isNaN(value), "NaN is not allowed as a value");
    entries.put(date, value);
    updateWeekendFlag(date);
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
    put(point.getDate(), point.getValue());
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Merges the specified date/value point into this builder.
   * <p>
   * The operator is invoked if the date already exists.
   *
   * @param date  the date to be added
   * @param value  the value associated with the date
   * @param operator  the operator to use for merging
   * @return this builder
   */
  public LocalDateDoubleTimeSeriesBuilder merge(LocalDate date, double value, DoubleBinaryOperator operator) {
    ArgChecker.notNull(date, "date");
    ArgChecker.notNull(operator, "operator");
    entries.merge(date, value, (a, b) -> operator.applyAsDouble(a, b));
    updateWeekendFlag(date);
    return this;
  }

  /**
   * Merges the specified date/value point into this builder.
   * <p>
   * The operator is invoked if the date already exists.
   *
   * @param point  the point to be added
   * @param operator  the operator to use for merging
   * @return this builder
   */
  public LocalDateDoubleTimeSeriesBuilder merge(LocalDateDoublePoint point, DoubleBinaryOperator operator) {
    ArgChecker.notNull(point, "point");
    return merge(point.getDate(), point.getValue(), operator);
  }

  // updates the weekend flag, if not updated exceptions will occur during build()
  private void updateWeekendFlag(LocalDate date) {
    if (!containsWeekends && date.get(ChronoField.DAY_OF_WEEK) > 5) {
      containsWeekends = true;
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Puts all the specified dates and values into this builder.
   * <p>
   * The date and value collections must be the same size.
   * <p>
   * The date-value pairs are added one by one.
   * If a date is duplicated it will overwrite an earlier entry.
   *
   * @param dates  the dates to be added
   * @param values  the values to be added
   * @return this builder
   */
  public LocalDateDoubleTimeSeriesBuilder putAll(Collection<LocalDate> dates, Collection<Double> values) {
    ArgChecker.noNulls(dates, "dates");
    ArgChecker.noNulls(values, "values");
    ArgChecker.isTrue(dates.size() == values.size(),
        "Arrays are of different sizes - dates: {}, values: {}", dates.size(), values.size());
    Iterator<LocalDate> itDate = dates.iterator();
    Iterator<Double> itValue = values.iterator();
    for (int i = 0; i < dates.size(); i++) {
      put(itDate.next(), itValue.next());
    }
    return this;
  }

  /**
   * Puts all the specified dates and values into this builder.
   * <p>
   * The date collection and value array must be the same size.
   * <p>
   * The date-value pairs are added one by one.
   * If a date is duplicated it will overwrite an earlier entry.
   *
   * @param dates  the dates to be added
   * @param values  the values to be added
   * @return this builder
   */
  public LocalDateDoubleTimeSeriesBuilder putAll(Collection<LocalDate> dates, double[] values) {
    ArgChecker.noNulls(dates, "dates");
    ArgChecker.notNull(values, "values");
    ArgChecker.isTrue(dates.size() == values.length,
        "Arrays are of different sizes - dates: {}, values: {}", dates.size(), values.length);
    Iterator<LocalDate> itDate = dates.iterator();
    for (int i = 0; i < dates.size(); i++) {
      put(itDate.next(), values[i]);
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
    points.forEach(this::put);
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
  public LocalDateDoubleTimeSeriesBuilder putAll(List<LocalDateDoublePoint> points) {
    ArgChecker.notNull(points, "points");
    return putAll(points.stream());
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
    containsWeekends = containsWeekends || other.containsWeekends;
    return this;
  }

  /**
   * Puts all the entries from the supplied map into this builder.
   * <p>
   * If a date is duplicated it will overwrite an earlier entry.
   *
   * @param map  the map of points to be added
   * @return this builder
   */
  public LocalDateDoubleTimeSeriesBuilder putAll(Map<LocalDate, Double> map) {
    ArgChecker.noNulls(map, "map");
    map.entrySet().forEach(e -> put(e.getKey(), e.getValue()));
    return this;
  }

  //-------------------------------------------------------------------------
  /**
   * Build the time-series from the builder.
   *
   * @return a time-series containing the entries from the builder
   */
  public LocalDateDoubleTimeSeries build() {
    if (entries.isEmpty()) {
      return LocalDateDoubleTimeSeries.empty();
    }

    // Depending on how dense the data is, judge which type of time series
    // is the best fit
    return density() > DENSITY_THRESHOLD ?
        createDenseSeries() :
        createSparseSeries();
  }

  private LocalDateDoubleTimeSeries createDenseSeries() {
    return DenseLocalDateDoubleTimeSeries.of(
        entries.firstKey(),
        entries.lastKey(),
        streamEntries(),
        determineCalculation());
  }

  private SparseLocalDateDoubleTimeSeries createSparseSeries() {
    return SparseLocalDateDoubleTimeSeries.of(entries.keySet(), entries.values());
  }

  private Stream<LocalDateDoublePoint> streamEntries() {
    return entries.entrySet()
        .stream()
        .map(e -> LocalDateDoublePoint.of(e.getKey(), e.getValue()));
  }

  private DenseLocalDateDoubleTimeSeries.DenseTimeSeriesCalculation determineCalculation() {
    return containsWeekends ? INCLUDE_WEEKENDS : SKIP_WEEKENDS;
  }

  private double density() {
    // We can use the calculators to work out range size
    double rangeSize = determineCalculation().calculatePosition(entries.firstKey(), entries.lastKey()) + 1;
    return entries.size() / rangeSize;
  }

}
