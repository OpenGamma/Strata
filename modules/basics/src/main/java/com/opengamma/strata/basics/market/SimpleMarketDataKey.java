/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

/**
 * Interface for market data keys representing simple types of market data for which no market data rules
 * are required.
 * <p>
 * For some types of market data the ID which uniquely identifies the value contains more
 * information than the key used by functions to request the value. For example, the key for a discount curve
 * contains the curve currency, and the ID contains the currency and also the curve group
 * which contains the curve. The curve group is specified by the market data rules.
 * <p>
 * All market data ID classes contains a reference to a {@link MarketDataFeed} which is the source of
 * the underlying market data used to build the value.
 * <p>
 * For many types of market data the feed is the only information in the ID that is not in the key.
 * Every set of market data rules must specify the market data feed. This means that there is no need to
 * specify additional market data rules for these types of market data; knowing the feed is all that is
 * required to create the ID from the key.
 * <p>
 * This interface should be implemented by keys for these types of simple market data. The calculation
 * engine has built-in support for this interface which means the user does not need to specify a market data rule
 * for any types implementing this interface.
 */
public interface SimpleMarketDataKey<T> extends MarketDataKey<T> {

  /**
   * Returns a market data ID identifying the same market data value as this key.
   *
   * @param marketDataFeed  the market data feed which provides the underlying data used to build the value
   * @return a market data ID identifying the same market data value as this key
   */
  public abstract MarketDataId<T> toMarketDataId(MarketDataFeed marketDataFeed);
}
