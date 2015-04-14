/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.marketdata.id;

import com.opengamma.strata.collect.type.TypedString;

// TODO Should this be a bean with feed metadata? Set of schemes and the default scheme?
/**
 * Identifies a feed of market data, for example Bloomberg or Reuters.
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
