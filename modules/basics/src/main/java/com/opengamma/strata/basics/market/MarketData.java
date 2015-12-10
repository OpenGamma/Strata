/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import java.time.LocalDate;

import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Provides access to market data, such as curves, surfaces and time-series.
 * <p>
 * Market data is looked up using subclasses of {@link MarketDataKey}.
 * All data is valid for a single date, defined by {@link #getValuationDate()}.
 * When performing calculations with scenarios, only the data of a single scenario is accessible.
 * <p>
 * See {@link ImmutableMarketData} for a standalone implementation.
 */
public interface MarketData {

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
   * @param <T>  the type of the market data
   * @param key  the key identifying the item of market data
   * @return true if this set of data contains a value for the specified key
   */
  public abstract <T> boolean containsValue(MarketDataKey<T> key);

  /**
   * Gets the market data value identified by the specified key.
   * <p>
   * The result will be a single piece of market data valid for the valuation date.
   *
   * @param <T>  the type of the market data
   * @param key  the key identifying the item of market data
   * @return the market data value
   * @throws IllegalArgumentException if no value is found
   * @throws RuntimeException if an unexpected error occurs
   */
  public abstract <T> T getValue(MarketDataKey<T> key);

  //-------------------------------------------------------------------------
  /**
   * Checks if this set of data contains a time-series for the specified key.
   *
   * @param key  the key identifying the item of market data
   * @return true if this set of data contains a time-series of market data for the specified key
   */
  public abstract boolean containsTimeSeries(ObservableKey key);

  /**
   * Gets the time-series identified by the specified key, empty if not found.
   *
   * @param key  the key identifying the item of market data
   * @return the time-series, empty if no time-series found
   * @throws RuntimeException if an unexpected error occurs
   */
  public abstract LocalDateDoubleTimeSeries getTimeSeries(ObservableKey key);

}
