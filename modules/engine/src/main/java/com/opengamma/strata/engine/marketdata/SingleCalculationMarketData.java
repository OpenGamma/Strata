/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import java.time.LocalDate;

import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.config.MarketDataRules;

/**
 * A source of market data provided to an engine function and used for a calculation across a single scenario.
 * <p>
 * The set of data provided by this interface is a subset of the set provided by {@link ScenarioMarketData}.
 * For example a function might request a USD discounting curve, but the scenario market data can contain
 * multiple curve groups, each with a USD discounting curve.
 * <p>
 * Typically a set of {@link MarketDataRules} are used to choose the item of market data from the global set.
 */
public interface SingleCalculationMarketData {

  /**
   * @return the valuation dates of the calculation
   */
  public abstract LocalDate getValuationDate();

  /**
   * Returns a list of market data values, one from each scenario.
   * <p>
   * The date of the market data is the same as the valuation date of the scenario.
   *
   * @param key  key identifying the market data
   * @param <T>  type of the market data
   * @param <K>  type of the market data key
   * @return a list of market data values, one from each scenario
   */
  public abstract <T, K extends MarketDataKey<T>> T getValue(K key);

  /**
   * Returns a list of market data time series, one from each scenario.
   *
   * @param key  key identifying the market data
   * @return a list of market data time series, one from each scenario
   */
  public abstract LocalDateDoubleTimeSeries getTimeSeries(ObservableKey key);
}
