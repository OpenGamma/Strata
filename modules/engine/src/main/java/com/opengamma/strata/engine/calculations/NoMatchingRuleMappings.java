/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations;

import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.marketdata.id.MarketDataFeed;
import com.opengamma.strata.marketdata.id.MarketDataId;
import com.opengamma.strata.marketdata.id.NoMatchingRuleId;
import com.opengamma.strata.marketdata.id.ObservableId;
import com.opengamma.strata.marketdata.key.MarketDataKey;
import com.opengamma.strata.marketdata.key.ObservableKey;

/**
 * Market data mappings used when the market data rules don't match a calculation target.
 * <p>
 * This means there are no mappings available to choose the market data for the calculations.
 * If market data is requested, this mapping returns an ID which results in a failure in the
 * market data with an error message explaining the problem.
 * <p>
 * This class uses raw types because there is no other way for it do implement its interface.
 */
class NoMatchingRuleMappings implements MarketDataMappings {

  /** Singleton instance. */
  static final NoMatchingRuleMappings INSTANCE = new NoMatchingRuleMappings();

  // This class has no state so there is no need to create multiple instances
  private NoMatchingRuleMappings() {
  }

  @SuppressWarnings("unchecked")
  @Override
  public MarketDataId getIdForKey(MarketDataKey key) {
    if (key instanceof ObservableKey) {
      return getIdForObservableKey((ObservableKey) key);
    }
    return NoMatchingRuleId.of(key);
  }

  /**
   * Returns an ID with the market data feed {@link MarketDataFeed#NO_RULE}.
   *
   * @param key  a market data key identifying an item of observable market data
   * @return an ID with the market data feed {@code NO_RULE}.
   */
  @Override
  public ObservableId getIdForObservableKey(ObservableKey key) {
    return key.toObservableId(MarketDataFeed.NO_RULE);
  }
}
