/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.timeseries;

import com.google.common.collect.ImmutableSortedMap;

/**
 * Base interface for all time-series types with a {@code double} value.
 * <p>
 * A time-series is similar to both a {@code SortedMap} of value keyed
 * by date-time and a {@code List} of date-time to value pairs. As such,
 * the date/times do not have to be evenly spread over time within the series.
 *
 * @param <T> the date-time type, such as {@code Instant} or {@code LocalDate}
 */
public interface DoubleTimeSeries<T> {

  /**
   * Return the size of this time-series.
   *
   * @return the size of the time-series
   */
  public abstract int size();

  /**
   * Returns a map of times and values in this time-series.
   *
   * @return a map of the elements of this time-series
   */
  public abstract ImmutableSortedMap<T, Double> toMap();

}
