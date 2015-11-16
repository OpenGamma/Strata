/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.time.LocalDate;

import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.calc.config.MarketDataRules;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * A source of market data provided to an engine function and used for a calculation across a single scenario.
 * <p>
 * The set of data provided by this interface is a subset of the set provided by {@link CalculationMarketDataMap}.
 * For example a function might request a USD discounting curve, but the scenario market data can contain
 * multiple curve groups, each with a USD discounting curve.
 * <p>
 * Typically a set of {@link MarketDataRules} are used to choose the item of market data from the global set.
 */
public interface SingleCalculationMarketData {

  /**
   * Gets the valuation date of the market data.
   * <p>
   * All values accessible through this interface have the same valuation date.
   * 
   * @return the valuation dates of the calculation
   */
  public abstract LocalDate getValuationDate();

  /**
   * Returns the market data value identified by the specified key.
   * <p>
   * The result will be a single piece of market data valid for the valuation date.
   * The value will be for the scenario that this object represents.
   *
   * @param key  key identifying the market data
   * @param <T>  type of the market data
   * @return the market data value
   * @throws RuntimeException if the value cannot be returned
   */
  public abstract <T> T getValue(MarketDataKey<T> key);

  /**
   * Returns the time series identified by the specified key.
   *
   * @param key  key identifying the market data
   * @return the time series
   * @throws RuntimeException if the time-series cannot be returned
   */
  public abstract LocalDateDoubleTimeSeries getTimeSeries(ObservableKey key);
}
