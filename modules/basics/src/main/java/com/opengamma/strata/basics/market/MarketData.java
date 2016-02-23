/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Provides access to market data, such as curves, surfaces and time-series.
 * <p>
 * Market data is looked up using subclasses of {@link MarketDataKey}.
 * All data is valid for a single date, defined by {@link #getValuationDate()}.
 * When performing calculations with scenarios, only the data of a single scenario is accessible.
 * <p>
 * The standard implementation is {@link ImmutableMarketData}.
 */
public interface MarketData {

  /**
   * Obtains an instance from a valuation date and map of values.
   *
   * @param valuationDate  the valuation date associated with the market data
   * @param values  the market data values
   * @return a set of market data containing the values in the map
   */
  public static MarketData of(LocalDate valuationDate, Map<? extends MarketDataKey<?>, ?> values) {
    return ImmutableMarketData.of(valuationDate, values);
  }

  /**
   * Obtains an instance from a valuation date, map of values and time-series.
   *
   * @param valuationDate  the valuation date associated with the market data
   * @param values  the market data values
   * @param timeSeries  the time-series
   * @return a set of market data containing the values and time-series
   */
  public static MarketData of(
      LocalDate valuationDate,
      Map<? extends MarketDataKey<?>, ?> values,
      Map<? extends ObservableKey, LocalDateDoubleTimeSeries> timeSeries) {

    return ImmutableMarketData.builder(valuationDate).values(values).timeSeries(timeSeries).build();
  }

  //-------------------------------------------------------------------------

  /**
   * Gets the valuation date of the market data.
   * <p>
   * All values accessible through this interface have the same valuation date.
   * 
   * @return the valuation date
   */
  public abstract LocalDate getValuationDate();

  //-------------------------------------------------------------------------

  /**
   * Checks if this set of data contains a value for the specified key.
   *
   * @param key  the key identifying the item of market data
   * @return true if this set of data contains a value for the specified key
   */
  public abstract boolean containsValue(MarketDataKey<?> key);

  /**
   * Returns a value for the specified ID if available.
   *
   * @param key  the key identifying the item of market data
   * @return a value for the specified ID if available
   */
  public abstract <T> Optional<T> findValue(MarketDataKey<T> key);

  /**
   * Gets the market data value identified by the specified key.
   * <p>
   * The result will be a single piece of market data valid for the valuation date.
   *
   * @param <T>  the type of the market data
   * @param key  the key identifying the item of market data
   * @return the market data value
   * @throws IllegalArgumentException if no value is found
   */
  public abstract <T> T getValue(MarketDataKey<T> key);

  //-------------------------------------------------------------------------

  /**
   * Gets the time-series identified by the specified key, empty if not found.
   *
   * @param key  the key identifying the item of market data
   * @return the time-series, empty if no time-series found
   */
  public abstract LocalDateDoubleTimeSeries getTimeSeries(ObservableKey key);

}
