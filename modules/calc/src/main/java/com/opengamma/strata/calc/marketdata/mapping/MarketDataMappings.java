/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata.mapping;

import java.util.List;

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.MarketDataKey;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.ObservableKey;

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
   * Returns a set of market data mappings with the specified source of observable data and made up
   * of the specified individual mappings.
   *
   * @param marketDataFeed  the feed that is the source of the market data, for example Bloomberg or Reuters
   * @param mappings  mappings for converting market data requests from calculations into requests that
   *   can be used to query the global set of market data
   * @return a set of mappings containing the specified feed and mapping instances
   */
  public static MarketDataMappings of(MarketDataFeed marketDataFeed, List<? extends MarketDataMapping<?, ?>> mappings) {
    return DefaultMarketDataMappings.of(marketDataFeed, mappings);
  }

  /**
   * Returns a set of market data mappings with the specified source of observable data and made up
   * of the specified individual mappings.
   *
   * @param marketDataFeed  the feed that is the source of the market data, for example Bloomberg or Reuters
   * @param mappings  mappings for converting market data requests from calculations into requests that
   *   can be used to query the global set of market data
   * @return a set of mappings containing the specified feed and mapping instances
   */
  public static MarketDataMappings of(MarketDataFeed marketDataFeed, MarketDataMapping<?, ?>... mappings) {
    return DefaultMarketDataMappings.of(marketDataFeed, mappings);
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
   * @param <T>  the type of the data returned by the key
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
