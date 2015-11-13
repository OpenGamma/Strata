/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.marketdata.mapping;

import java.util.Optional;

import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.ObservableId;

/**
 * Provides mappings from {@link ObservableId} instances requested by calculations to ID instances that
 * are suitable for querying a market data feed to get the market data.
 * <p>
 * The {@code StandardId} and the {@link MarketDataFeed} in an {@code ObservableId} are not necessarily related.
 * Therefore it is necessary to get a standard ID that is suitable for the feed before requesting
 * the market data.
 */
public interface FeedIdMapping {

  /**
   * Returns a mapping that always returns the ID that is passed in.
   *
   * @return a mapping that always returns the ID that is passed in
   */
  public static FeedIdMapping identity() {
    return Optional::of;
  }

  /**
   * Returns an observable ID that can be used for looking up the market data in a market data feed if
   * there is a mapping defined for the ID argument.
   *
   * @param id  an observable ID containing a standard ID and a market data feed if there is a mapping for
   *   the ID argument
   * @return an observable ID that can be used for looking up the market data in a market data feed
   */
  public abstract Optional<ObservableId> idForFeed(ObservableId id);
}
