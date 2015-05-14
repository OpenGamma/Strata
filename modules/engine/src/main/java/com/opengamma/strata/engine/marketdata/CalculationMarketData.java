/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import java.time.LocalDate;
import java.util.List;

import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.config.MarketDataRules;

/**
 * A source of market data provided to an engine function and used for a calculation across multiple scenarios.
 * <p>
 * The set of data provided by this interface is a subset of the set provided by {@link ScenarioMarketData}.
 * For example a function might request a USD discounting curve, but the scenario market data can contain
 * multiple curve groups, each with a USD discounting curve.
 * <p>
 * Typically a set of {@link MarketDataRules} are used to choose the item of market data from the global set.
 */
public interface CalculationMarketData {

  /**
   * Returns the valuation dates of the scenarios, one for each scenario.
   *
   * @return the valuation dates of the scenarios, one for each scenario
   */
  public abstract List<LocalDate> getValuationDates();

  /**
   * Returns the number of scenarios.
   *
   * @return the number of scenarios
   */
  public abstract int getScenarioCount();

  /**
   * Returns a list of market data values, one from each scenario.
   * <p>
   * The date of the market data is the same as the valuation date of the scenario.
   *
   * @param key  a key identifying the market data
   * @param <T>  type of the market data
   * @param <K>  type of the market data key
   * @return a list of market data values, one from each scenario
   */
  public abstract <T, K extends MarketDataKey<T>> List<T> getValues(K key);

  /**
   * Returns a time series of market data values.
   * <p>
   * Time series are not affected by scenarios, therefore there is a single time series for each key
   * which is shared between all scenarios.
   *
   * @param key  a key identifying the market data
   * @return a list of market data time series
   */
  public abstract LocalDateDoubleTimeSeries getTimeSeries(ObservableKey key);

  /**
   * Returns a single value that is valid for all scenarios.
   * <p>
   * This allows optimizations such as pre-processing of items market data to create a single composite
   * value that can be processed more efficiently.
   *
   * @param key  a key identifying the market data
   * @param <T>  type of the market data
   * @param <K>  type of the market data key
   * @return the market data value
   */
  public abstract <T, K extends MarketDataKey<T>> T getGlobalValue(K key);
}
