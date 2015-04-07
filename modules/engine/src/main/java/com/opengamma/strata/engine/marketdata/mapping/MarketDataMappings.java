/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.mapping;

import com.opengamma.strata.marketdata.id.MarketDataId;
import com.opengamma.strata.marketdata.id.ObservableId;
import com.opengamma.strata.marketdata.key.MarketDataKey;
import com.opengamma.strata.marketdata.key.ObservableKey;

/**
 * Market data mappings specify which market data from the global set of data should be used for a particular
 * calculation.
 * <p>
 * For example, the global set of market data might contain curves from several curve groups but a
 * calculation needs to request (for example) the USD discounting curve without knowing or caring which
 * group contains it.
 * <p>
 * This class provides the mapping from a general piece of data (the USD discounting
 * curve) to a specific piece of data (the USD discounting curve from the curve group named 'XYZ').
 */
public interface MarketDataMappings {

  /**
   * Returns a mutable builder for building instances of {@link MarketDataMappings}.
   *
   * @return a mutable builder for building instances of {@link MarketDataMappings}
   */
  public static MarketDataMappingsBuilder builder() {
    return new MarketDataMappingsBuilder();
  }

  /**
   * Returns an empty set of market data mappings containing no mappers.
   *
   * @return an empty set of market data mappings containing no mappers
   */
  public static MarketDataMappings empty() {
    return DefaultMarketDataMappings.EMPTY;
  }

  /**
   * Returns a market data ID which uniquely identifies the piece of market data referred to by the key.
   * <p>
   * The key might identify an item of data of which there are many copies in the system, for example
   * the USD discounting curve. The ID identifies a globally unique copy, for example the USD discounting
   * curve from a named curve group.
   *
   * @param key  a key identifying an item of market data
   * @param <K>  the type of the market data key accepted by this method
   * @return an ID uniquely identifying an item of market data
   */
  public abstract <T, K extends MarketDataKey<T>> MarketDataId<T> getIdForKey(K key);

  /**
   * Gets the market data ID for an item of observable market data given its key.
   *
   * @param key  a market data key identifying an item of observable market data
   * @return a market data ID that uniquely identifies the data and its source
   */
  public abstract ObservableId getIdForObservableKey(ObservableKey key);
}
