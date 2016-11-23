/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.timeseries;

import static java.util.stream.Collectors.partitioningBy;

import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.function.ObjDoublePredicate;
import com.opengamma.strata.collect.tuple.Pair;

/**
 * Interface for all local date time-series types containing
 * {@code double} values.
 * <p>
 * A time-series is similar to both a {@code SortedMap} of value keyed
 * by date-time and a {@code List} of date-time to value pairs. As such,
 * the date/times do not have to be evenly spread over time within the series.
 * <p>
 * The distribution of the data will influence which implementation
 * is most appropriate.
 * <p>
 * Implementations must be immutable and thread-safe beans.
 * <p>
 * Note that {@link Double#NaN} is used internally as a sentinel
 * value and is therefore not allowed as a value.
 */
public interface LocalDateDoubleTimeSeries {

  /**
   * Returns an empty time-series. Generally a singleton instance
   * is returned.
   *
   * @return an empty time-series
   */
  public static LocalDateDoubleTimeSeries empty() {
    return SparseLocalDateDoubleTimeSeries.EMPTY;
  }

  /**
   * Obtains a time-series containing a single date and value.
   *
   * @param date  the singleton date
   * @param value  the singleton value
   * @return the time-series
   */
  public static LocalDateDoubleTimeSeries of(LocalDate date, double value) {
    ArgChecker.notNull(date, "date");
    return builder().put(date, value).build();
  }

  /**
   * Creates an empty builder, used to create time-series.
   * <p>
   * The builder has methods to create and modify a time-series.
   *
   * @return the time-series builder
   */
  public static LocalDateDoubleTimeSeriesBuilder builder() {
    return new LocalDateDoubleTimeSeriesBuilder();
  }

  /**
   * Returns a collector that can be used to create a time-series from a stream of points.
   *
   * @return the time-series collector
   */
  public static Collector<LocalDateDoublePoint, LocalDateDoubleTimeSeriesBuilder, LocalDateDoubleTimeSeries> collector() {
    return Collector.of(
        LocalDateDoubleTimeSeriesBuilder::new,
        LocalDateDoubleTimeSeriesBuilder::put,
        LocalDateDoubleTimeSeriesBuilder::putAll,
        LocalDateDoubleTimeSeriesBuilder::build);
  }

  //-------------------------------------------------------------------------
  /**
   * Return the size of this time-series.
   *
   * @return the size of the time-series
   */
  public abstract int size();

  /**
   * Indicates if this time-series is empty.
   *
   * @return true if the time-series contains no entries
   */
  public abstract boolean isEmpty();

  /**
   * Checks if this time-series contains a value for the specified date.
   *
   * @param date  the date to check for
   * @return true if there is a value associated with the date
   */
  public abstract boolean containsDate(LocalDate date);

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
  public abstract OptionalDouble get(LocalDate date);

  //-------------------------------------------------------------------------
  /**
   * Get the earliest date contained in this time-series.
   * <p>
   * If the time-series is empty then {@link NoSuchElementException} will be thrown.
   *
   * @return the earliest date contained in this time-series
   * @throws NoSuchElementException if the time-series is empty
   */
  public default LocalDate getEarliestDate() {
    return dates().findFirst()
        .orElseThrow(() -> new NoSuchElementException("Unable to return earliest date, time-series is empty"));
  }

  /**
   * Get the value held for the earliest date contained in this time-series.
   * <p>
   * If the time-series is empty then {@link NoSuchElementException} will be thrown.
   *
   * @return the value held for the earliest date contained in this time-series
   * @throws NoSuchElementException if the time-series is empty
   */
  public default double getEarliestValue() {
    return values().findFirst()
        .orElseThrow(() -> new NoSuchElementException("Unable to return earliest value, time-series is empty"));
  }

  /**
   * Get the latest date contained in this time-series.
   * <p>
   * If the time-series is empty then {@link NoSuchElementException} will be thrown.
   *
   * @return the latest date contained in this time-series
   * @throws NoSuchElementException if the time-series is empty
   */
  public abstract LocalDate getLatestDate();

  /**
   * Get the value held for the latest date contained in this time-series.
   * <p>
   * If the time-series is empty then {@link NoSuchElementException} will be thrown.
   *
   * @return the value held for the latest date contained in this time-series
   * @throws NoSuchElementException if the time-series is empty
   */
  public abstract double getLatestValue();

  //-------------------------------------------------------------------------
  /**
   * Gets part of this series as a sub-series between two dates.
   * <p>
   * The date-times do not have to match exactly.
   * The sub-series contains all entries between the two dates using a half-open interval.
   * The start date is included, the end date is excluded.
   * The dates may be before or after the end of the time-series.
   * <p>
   * To obtain the series before a specific date, used {@code LocalDate.MIN} as the first argument.
   * To obtain the series from a specific date, used {@code LocalDate.MAX} as the second argument.
   *
   * @param startInclusive  the start date, inclusive
   * @param endExclusive  the end date, exclusive
   * @return the sub-series between the dates
   * @throws IllegalArgumentException if the end is before the start
   */
  public abstract LocalDateDoubleTimeSeries subSeries(LocalDate startInclusive, LocalDate endExclusive);

  /**
   * Gets part of this series as a sub-series, choosing the earliest entries.
   * <p>
   * The sub-series contains the earliest part of the series up to the specified number of points.
   * If the series contains less points than the number requested, the whole time-series is returned.
   *
   * @param numPoints  the number of items to select, zero or greater
   * @return the sub-series of the requested size starting with the earliest entry
   * @throws IllegalArgumentException if the number of items is less than zero
   */
  public abstract LocalDateDoubleTimeSeries headSeries(int numPoints);

  /**
   * Gets part of this series as a sub-series, choosing the latest entries.
   * <p>
   * The sub-series contains the latest part of the series up to the specified number of points.
   * If the series contains less points than the number requested, the whole time-series is returned.
   *
   * @param numPoints  the number of items to select, zero or greater
   * @return the sub-series of the requested size ending with the latest entry
   * @throws IllegalArgumentException if the number of items is less than zero
   */
  public abstract LocalDateDoubleTimeSeries tailSeries(int numPoints);

  //-------------------------------------------------------------------------
  /**
   * Returns a stream over the points of this time-series.
   * <p>
   * This provides access to the entire time-series.
   *
   * @return a stream over the points of this time-series
   */
  public abstract Stream<LocalDateDoublePoint> stream();

  /**
   * Returns a stream over the dates of this time-series.
   * <p>
   * This is most useful to summarize the dates in the stream, such as calculating
   * the maximum or minimum date, or searching for a specific value.
   *
   * @return a stream over the dates of this time-series
   */
  public abstract Stream<LocalDate> dates();

  /**
   * Returns a stream over the values of this time-series.
   * <p>
   * This is most useful to summarize the values in the stream, such as calculating
   * the maximum, minimum or average value, or searching for a specific value.
   *
   * @return a stream over the values of this time-series
   */
  public abstract DoubleStream values();

  //-------------------------------------------------------------------------
  /**
   * Applies an action to each pair in the time series.
   * <p>
   * This is generally used to apply a mathematical operation to the values.
   * For example, the operator could multiply each value by a constant, or take the inverse.
   * <pre>
   *   base.forEach((date, value) -> System.out.println(date + "=" + value));
   * </pre>
   *
   * @param action  the action to be applied to each pair
   */
  public abstract void forEach(ObjDoubleConsumer<LocalDate> action);

  /**
   * Applies an operation to each date in the time series which creates a new date, returning a new time series
   * with the new dates and the points from this time series.
   * <p>
   * This operation creates a new time series with the same data but the dates moved.
   * <p>
   * The operation must not change the dates in a way that reorders them. The mapped dates must be in ascending
   * order or an exception is thrown.
   * <pre>
   *   updatedSeries = timeSeries.mapDates(date -> date.plusYears(1));
   * </pre>
   *
   * @param mapper  the operation applied to each date in the time series
   * @return a copy of this time series with new dates
   */
  public abstract LocalDateDoubleTimeSeries mapDates(Function<? super LocalDate, ? extends LocalDate> mapper);

  /**
   * Applies an operation to each value in the time series.
   * <p>
   * This is generally used to apply a mathematical operation to the values.
   * For example, the operator could multiply each value by a constant, or take the inverse.
   * <pre>
   *   multiplied = base.mapValues(value -> value * 3);
   * </pre>
   *
   * @param mapper  the operator to be applied to the values
   * @return a copy of this series with the mapping applied to the original values
   */
  public abstract LocalDateDoubleTimeSeries mapValues(DoubleUnaryOperator mapper);

  /**
   * Create a new time-series by filtering this one.
   * <p>
   * The time-series can be filtered by both date and value.
   * Note that if filtering by date range is required, it is likely to be more efficient to
   * use {@link #subSeries(LocalDate, LocalDate)} as that avoids traversal of the whole series.
   *
   * @param predicate  the predicate to use to the filter the elements of the series
   * @return a filtered version of the series
   */
  public abstract LocalDateDoubleTimeSeries filter(ObjDoublePredicate<LocalDate> predicate);

  //-------------------------------------------------------------------------
  /**
   * Obtains the intersection of a pair of time series.
   * <p>
   * This returns a time-series with the intersection of the dates of the two inputs.
   * The operator is invoked to combine the values.
   *
   * @param other  the time-series to combine with
   * @param mapper  the function to be used to combine the values
   * @return a new time-series containing the dates in common between the
   *  input series with their values combined together using the function
   */
  public default LocalDateDoubleTimeSeries intersection(LocalDateDoubleTimeSeries other, DoubleBinaryOperator mapper) {
    ArgChecker.notNull(other, "other");
    ArgChecker.notNull(mapper, "mapper");
    return new LocalDateDoubleTimeSeriesBuilder()
        .putAll(stream()
            .filter(pt -> other.containsDate(pt.getDate()))
            .map(pt -> LocalDateDoublePoint.of(
                pt.getDate(), mapper.applyAsDouble(pt.getValue(), other.get(pt.getDate()).getAsDouble()))))
        .build();
  }

  /**
   * Obtains the union of a pair of time series.
   * <p>
   * This returns a time-series with the union of the dates of the two inputs.
   * When the same date occurs in both time-series, the operator is invoked to combine the values.
   *
   * @param other  the time-series to combine with
   * @param mapper  the function to be used to combine the values
   * @return a new time-series containing the dates in common between the
   *  input series with their values combined together using the function
   */
  public default LocalDateDoubleTimeSeries union(LocalDateDoubleTimeSeries other, DoubleBinaryOperator mapper) {
    ArgChecker.notNull(other, "other");
    ArgChecker.notNull(mapper, "mapper");
    LocalDateDoubleTimeSeriesBuilder builder = new LocalDateDoubleTimeSeriesBuilder(stream());
    other.stream().forEach(pt -> builder.merge(pt, mapper));
    return builder.build();
  }

  /**
   * Partition the time-series into a pair of distinct series using a predicate.
   * <p>
   * Points in the time-series which match the predicate will be put into the first series,
   * whilst those points which do not match will be put into the second.
   *
   * @param predicate  predicate used to test the points in the time-series
   * @return a {@code Pair} containing two time-series. The first is a series
   *   made of all the points in this series which match the predicate. The
   *   second is a series made of the points which do not match.
   */
  public default Pair<LocalDateDoubleTimeSeries, LocalDateDoubleTimeSeries> partition(
      ObjDoublePredicate<LocalDate> predicate) {

    Map<Boolean, LocalDateDoubleTimeSeries> partitioned = stream()
        .collect(
            partitioningBy(
                pt -> predicate.test(pt.getDate(), pt.getValue()),
                LocalDateDoubleTimeSeries.collector()));

    return Pair.of(partitioned.get(true), partitioned.get(false));
  }

  /**
   * Partition the time-series into a pair of distinct series using a predicate.
   * <p>
   * Points in the time-series whose values match the predicate will be put into the first series,
   * whilst those points whose values do not match will be put into the second.
   *
   * @param predicate  predicate used to test the points in the time-series
   * @return a {@code Pair} containing two time-series. The first is a series
   *   made of all the points in this series which match the predicate. The
   *   second is a series made of the points which do not match.
   */
  public default Pair<LocalDateDoubleTimeSeries, LocalDateDoubleTimeSeries> partitionByValue(
      DoublePredicate predicate) {
    return partition((obj, value) -> predicate.test(value));
  }

  /**
   * Return a builder populated with the values from this series.
   * <p>
   * This can be used to mutate the time-series.
   *
   * @return a builder containing the point from this time-series
   */
  public abstract LocalDateDoubleTimeSeriesBuilder toBuilder();

}
