/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.marketdata.id;

import com.opengamma.strata.collect.type.TypedString;

/**
 * Identifies a feed of market data, for example Bloomberg or Reuters.
 * <p>
 * A feed can represent the default source of data for a particular data provider, or it can
 * represent a subset of the data from the provider, for example data from a specific broker
 * published by Bloomberg. Therefore there can be multiple feeds providing data from a single
 * physical market data system.
 */
public final class MarketDataFeed extends TypedString<MarketDataFeed> {

  /** A market data feed used where a feed is required but no data is expected to be requested. */
  public static final MarketDataFeed NONE = of("None");

  /** A market data feed used to indicate there are no market data rules for a calculation. */
  public static final MarketDataFeed NO_RULE = of("NoMatchingMarketDataRule");

  private MarketDataFeed(String name) {
    super(name);
  }

  /**
   * Returns a feed with the specified name.
   *
   * @param feedName  the feed name
   * @return a feed with the specified name
   */
  public static MarketDataFeed of(String feedName) {
    return new MarketDataFeed(feedName);
  }
}
