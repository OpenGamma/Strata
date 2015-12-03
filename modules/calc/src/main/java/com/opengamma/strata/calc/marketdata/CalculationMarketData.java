/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata;

import java.time.LocalDate;

import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.basics.market.ScenarioMarketDataKey;
import com.opengamma.strata.basics.market.ScenarioMarketDataValue;
import com.opengamma.strata.calc.config.MarketDataRules;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * A source of market data provided to an engine function and used for a calculation across multiple scenarios.
 * <p>
 * The set of data provided by this interface is a subset of the set provided by {@link CalculationEnvironment}.
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
  public abstract MarketDataBox<LocalDate> getValuationDate();

  /**
   * Returns the number of scenarios.
   *
   * @return the number of scenarios
   */
  public abstract int getScenarioCount();

  /**
   * Returns a box that can provide an item of market data for a scenario.
   *
   * @param <T>  type of the market data
   * @param key  a key identifying the market data
   * @return a list of market data values, one from each scenario
   */
  public abstract <T> MarketDataBox<T> getValue(MarketDataKey<T> key);

  /**
   * Returns an object containing market data for multiple scenarios.
   * <p>
   * There are many possible ways to store scenario market data for a data type. For example, if the single
   * values are doubles, the scenario value might simply be a {@code List<Double>} or it might be a wrapper
   * class that stores the values more efficiently in a {@code double[]}.
   * <p>
   * If the market data contains a single value for the key or a scenario value of the wrong type,
   * a value of the required type is created by invoking {@link ScenarioMarketDataKey#createScenarioValue}.
   * <p>
   * Normally this should not be necessary. It is assumed the required scenario values will be created by the
   * perturbations that create scenario data. However there is no mechanism in the market data system to guarantee
   * that scenario values of a particular type are available. If they are not they are created on demand.
   * <p>
   * Values returned from this method might be cached for efficiency.
   *
   * @param key  identifies the market data required
   * @param <T>  the type of the individual market data values used when performing calculations for one scenario
   * @param <U>  the type of the object containing the market data for all scenarios
   * @return an object containing market data for multiple scenarios
   */
  @SuppressWarnings("unchecked")
  public default <T, U extends ScenarioMarketDataValue<T>> U getScenarioValue(ScenarioMarketDataKey<T, U> key) {
    MarketDataBox<T> box = getValue(key.getMarketDataKey());

    if (box.isSingleValue()) {
      return key.createScenarioValue(box);
    }
    ScenarioMarketDataValue<T> scenarioValue = box.getScenarioValue();

    if (key.getScenarioMarketDataType().isInstance(scenarioValue)) {
      return (U) scenarioValue;
    }
    return key.createScenarioValue(box);
  }

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
}
