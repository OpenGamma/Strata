/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.timeseries;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.OptionalDouble;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.opengamma.collect.function.ObjDoublePredicate;
import com.opengamma.collect.id.IdentifiableBean;
import org.joda.beans.Bean;

/**
 * Base interface for all local date time-series types containing
 * {@code double} values.
 * <p>
 * A time-series is similar to both a {@code SortedMap} of value keyed
 * by date-time and a {@code List} of date-time to value pairs. As such,
 * the date/times do not have to be evenly spread over time within the series.
 * <p>
 * The distribution of the data will influence which implementation
 * is most appropriate.
 */
public interface LocalDateDoubleTimeSeries extends Bean {

  /**
   * Returns a map of times and values in this time-series.
   *
   * @return a map of the elements of this time-series
   */
  public abstract ImmutableSortedMap<LocalDate, Double> toMap();

  /**
   * Returns the list of dates in this time-series.
   * <p>
   * This provides low-level access to the internal data.
   * The stream and lambda methods should be used in preference
   * to this method. There is no guarantee that the underlying
   * implementation holds the dates in a list.
   *
   * @return the list of dates in this time-series
   */
  public abstract ImmutableList<LocalDate> dates();

  /**
   * Returns the list of values in this time-series.
   * <p>
   * This provides low-level access to the internal data.
   * The stream and lambda methods should be used in preference
   * to this method. There is no guarantee that the underlying
   * implementation holds the values in a list.
   *
   * @return the list of values in this time-series
   */
  public abstract ImmutableList<Double> values();

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

  /**
   * Get the earliest date contained in this time-series.
   * <p>
   * If the time-series is empty then {@link NoSuchElementException} will be thrown.
   *
   * @return the earliest date contained in this time-series
   * @throws NoSuchElementException if the time-series is empty
   */
  public abstract LocalDate getEarliestDate();

  /**
   * Get the value held for the earliest date contained in this time-series.
   * <p>
   * If the time-series is empty then {@link NoSuchElementException} will be thrown.
   *
   * @return the value held for the earliest date contained in this time-series
   * @throws NoSuchElementException if the time-series is empty
   */
  public abstract double getEarliestValue();

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
   * @return a stream over the values of this time-series
   */
  public abstract Stream<LocalDate> dateStream();

  /**
   * Returns a stream over the values of this time-series.
   * <p>
   * This is most useful to summarize the values in the stream, such as calculating
   * the maximum, minimum or average value, or searching for a specific value.
   *
   * @return a stream over the values of this time-series
   */
  public abstract DoubleStream valueStream();

  /**
   * Applies an action to each pair in the time series.
   * <p>
   * This is generally used to apply a mathematical operation to the values.
   * For example, the operator could multiply each value by a constant, or take the inverse.
   * <pre>
   *   multiplied = base.forEach((date, value) -> System.out.println(date + "=" + value));
   * </pre>
   *
   * @param action  the action to be applied to each pair
   */
  public abstract void forEach(ObjDoubleConsumer<LocalDate> action);

  /**
   * Applies an operation to each value in the time series.
   * <p>
   * This is generally used to apply a mathematical operation to the values.
   * For example, the operator could multiply each value by a constant, or take the inverse.
   * <pre>
   *   multiplied = base.map(value -> value * 3);
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

  /**
   * Combines a pair of time series, extracting the dates common to both and
   * applying a function to combine the values.
   *
   * @param other  the time-series to combine with
   * @param mapper  the function to be used to combine the values
   * @return a new time-series containing the dates in common between the
   *  input series with their values combined together using the function
   */
  public abstract LocalDateDoubleTimeSeries combineWith(LocalDateDoubleTimeSeries other, DoubleBinaryOperator mapper);

  /**
   * Return a builder populated with the values from this series.
   *
   * @return a time-series builder
   */
  public abstract LocalDateDoubleTimeSeriesBuilder toBuilder();
}
