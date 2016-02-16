/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import com.opengamma.strata.basics.market.MarketDataBox;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.ObservableKey;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;

/**
 * Market data mappings used when the market data rules don't match a calculation target.
 * <p>
 * This means there are no mappings available to choose the market data for the calculations.
 * If market data is requested, this mapping returns an ID which results in a failure in the
 * market data with an error message explaining the problem.
 */
class NoMatchingRuleMappings implements MarketDataMappings {

  /** Singleton instance. */
  static final NoMatchingRuleMappings INSTANCE = new NoMatchingRuleMappings();

  // This class has no state so there is no need to create multiple instances
  private NoMatchingRuleMappings() {
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T, K extends MarketDataKey<T>> MarketDataId<T> getIdForKey(K key) {
    if (key instanceof ObservableKey) {
      return (MarketDataId<T>) getIdForObservableKey((ObservableKey) key);
    }
    return (MarketDataId<T>) NoMatchingRuleId.of(key);
  }

  /**
   * Returns an ID with the market data feed {@link MarketDataFeed#NO_RULE}.
   *
   * @param key  a market data key identifying an item of observable market data
   * @return an ID with the market data feed {@code NO_RULE}.
   */
  @Override
  public ObservableId getIdForObservableKey(ObservableKey key) {
    return key.toMarketDataId(MarketDataFeed.NO_RULE);
  }

  @Override
  public boolean containsValue(MarketDataKey<?> key, CalculationEnvironment marketData) {
    return false;
  }

  @Override
  public boolean containsTimeSeries(ObservableKey key, CalculationEnvironment marketData) {
    return false;
  }

  @Override
  public <T> MarketDataBox<T> getValue(MarketDataKey<T> key, CalculationEnvironment marketData) {
    throw new IllegalArgumentException("No market data available for key " + key);
  }

  @Override
  public LocalDateDoubleTimeSeries getTimeSeries(ObservableKey key, CalculationEnvironment marketData) {
    throw new IllegalArgumentException("No market data available for key " + key);
  }
}
