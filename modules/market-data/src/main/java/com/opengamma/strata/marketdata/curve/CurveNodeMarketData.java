/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.marketdata.curve;

import java.time.LocalDate;

import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.marketdata.key.MarketDataKey;
import com.opengamma.strata.marketdata.key.ObservableKey;

/**
 * A set of market data used when building instruments for curve nodes.
 */
public interface CurveNodeMarketData {

  /**
   * Returns the valuation date.
   *
   * @return the valuation date
   */
  public abstract LocalDate getValuationDate();

  /**
   * Returns a market data value.
   * <p>
   * The date of the market data is the same as the valuation date.
   *
   * @param key  a key identifying the market data
   * @param <T>  type of the market data
   * @param <K>  type of the market data key
   * @return a market data value
   */
  public abstract <T, K extends MarketDataKey<T>> T getValue(K key);

  /**
   * Returns a time series of market data values.
   * <p>
   * The date of the market data is the same as the valuation date.
   *
   * @param key  a key identifying the market data
   * @return a list of market data time series
   */
  public abstract LocalDateDoubleTimeSeries getTimeSeries(ObservableKey key);
}
