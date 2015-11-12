/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata.mapping;

import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataKey;

/**
 * A market data mapping can be thought of as a configuration rule that tells the system where to
 * find a piece of market data that is required for a calculation.
 * <p>
 * A market data mapping accepts a {@link MarketDataKey} identifying an item of market data and returns
 * a {@link MarketDataId} that uniquely identifies an item of market data, including its source.
 * <p>
 * Market data keys identify items of market data that are unique in the context of a single calculation.
 * For example, there might be a key identifying the USD discounting curve.
 * <p>
 * Market data IDs are a unique identifier for an item of market data that includes the source of
 * the data. For example, the system might contains many curve groups, and each curve group can
 * contain a USD discounting curve. So the market data ID for a USD discounting curve must include the
 * name of the curve group that contains it.
 * 
 * @param <T>  the type of the market data identified by the key
 * @param <K>  the type of the market data key
 */
public interface MarketDataMapping<T, K extends MarketDataKey<T>> {

  /**
   * @return the types of {@link MarketDataId} for which the mapper return data.
   */
  public abstract Class<? extends K> getMarketDataKeyType();

  /**
   * Returns a market data ID which uniquely identifies the piece of market data referred to by the key.
   * <p>
   * The key might identify an item of data of which there are many instances in the system, for example
   * the USD discounting curve. The ID identifies a globally unique instance, for example the USD discounting
   * curve from a named curve group.
   *
   * @param key  a key identifying an item of market data
   * @return an ID uniquely identifying an item of market data
   */
  public abstract MarketDataId<T> getIdForKey(K key);
}
