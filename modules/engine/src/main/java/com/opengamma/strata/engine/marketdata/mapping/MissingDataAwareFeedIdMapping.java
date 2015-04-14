/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.mapping;

import java.util.Optional;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.marketdata.id.MarketDataFeed;
import com.opengamma.strata.marketdata.id.ObservableId;

/**
 * ID mapping that returns the input ID if it has the feed {@link MarketDataFeed#NO_RULE}
 * else it delegates to another instance to perform the mapping.
 */
public final class MissingDataAwareFeedIdMapping implements FeedIdMapping {

  /** Mapping used for IDs that don't have the feed {@link MarketDataFeed#NO_RULE}. */
  private final FeedIdMapping delegate;

  /**
   * @param delegate mapping used for IDs that don't have the feed {@link MarketDataFeed#NO_RULE}
   */
  public MissingDataAwareFeedIdMapping(FeedIdMapping delegate) {
    this.delegate = ArgChecker.notNull(delegate, "delegate");
  }

  @Override
  public Optional<ObservableId> idForFeed(ObservableId id) {
    return id.getMarketDataFeed().equals(MarketDataFeed.NO_RULE) ?
        Optional.of(id) :
        delegate.idForFeed(id);
  }
}
