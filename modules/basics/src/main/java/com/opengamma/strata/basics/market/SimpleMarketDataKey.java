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
 * 
 * @param <T>  the type of the market data identified by the key
 */
public interface SimpleMarketDataKey<T> extends MarketDataKey<T> {

  /**
   * Converts this key to the matching identifier.
   * <p>
   * Market data keys identify a piece of market data in the context of a single calculation, whereas
   * market data identifiers are a globally unique identifier for an item of data.
   * <p>
   * For example, a calculation has access to a single USD discounting curve, but the system
   * can contain multiple curve groups, each with a USD discounting curve. For cases such as curves there
   * is a mapping step to transform keys to IDs, controlled by the market data rules.
   * <p>
   * The market data key for a simple item of market data contains enough information to uniquely
   * identify the market data in the global set of data. Therefore there is no mapping step required
   * for the data and the market data ID can be directly derived from the market data key.
   *
   * @param marketDataFeed  the market data feed that is the source of the market data
   * @return the identifier corresponding to this key
   */
  public abstract MarketDataId<T> toMarketDataId(MarketDataFeed marketDataFeed);

}
