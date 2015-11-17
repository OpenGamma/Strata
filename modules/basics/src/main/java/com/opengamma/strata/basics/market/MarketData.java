/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import java.util.Map;

import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * A set of market data used when calculating values for a single scenario.
 */
public interface MarketData {

  /**
   * Checks if this set of data contains a value for the specified key.
   *
   * @param key  a key identifying an item of market data
   * @return true if this set of data contains a value for the specified key
   */
  public abstract boolean containsValue(MarketDataKey<?> key);

  /**
   * Checks if this set of data contains a time series for the specified key.
   *
   * @param key  a key identifying an item of market data
   * @return true if this set of data contains a time series of market data for the specified key
   */
  public abstract boolean containsTimeSeries(ObservableKey key);

  /**
   * Returns a market data value.
   * <p>
   * The date of the market data is the same as the valuation date of the calculations.
   *
   * @param <T>  type of the market data
   * @param key  key identifying the market data
   * @return a market data value
   * @throws IllegalArgumentException if there is no value for the specified key
   */
  public abstract <T> T getValue(MarketDataKey<T> key);

  /**
   * Returns a time series of market data values.
   *
   * @param key  key identifying the market data
   * @return a time series of market data values
   * @throws IllegalArgumentException if there is no time series for the specified key
   */
  public abstract LocalDateDoubleTimeSeries getTimeSeries(ObservableKey key);

  /**
   * Returns a mutable builder for building instances of {@code MarketData}.
   *
   * @return a mutable builder for building instances of {@code MarketData}
   */
  public static MarketDataBuilder builder() {
    return new MarketDataBuilder();
  }

  /**
   * Builds a set of market data from the values in a map.
   * <p>
   * The {@link #builder()} method and {@link MarketDataBuilder} class provide additional options when building
   * {@code MarketData} instances.
   *
   * @param values  a map of market data values
   * @return a {@code MarketData} instance containing the values in the map
   */
  public static MarketData of(Map<? extends MarketDataKey<?>, ?> values) {
    return ImmutableMarketData.of(values);
  }
}
