/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableSet;
import com.opengamma.util.result.Result;

/**
 * Provides market data and stores the requests which have been made. By starting with an empty
 * instance provided for the first cycle, it will be filled with the market data requirements
 * by the end of the cycle.
 */
// todo - consider that at some point we may want to track market data that is no longer needed e.g. trade removal, option expiry
public class DefaultResettableMarketDataFn implements ResettableMarketDataFn {

  /**
   * The market data which has previously been requested and which may have a value present. The status
   * value will indicate whether the data is available or is likely to become available at some
   * point in the future.
   * TODO this was requested in a previous cycle and will have been sent to the MDS
   */
  private final Map<MarketDataRequirement, MarketDataItem> _requestedMarketData = new HashMap<>();

  /**
   * Records the requests which have been made by clients that we don't have status data for. Uses
   * a concurrent map which should avoid contention, however it may sometimes block. If this becomes a
   * problem then consider using a ConcurrentLinkedQueue which should be entirely wait-free. Using the queue
   * could also allow another thread to drain off the requests and send them out to the market data server.
   * TODO newly requested data in this cycle
   */
  // todo we should size based on portfolio size and concurrency requirements
  private final Set<MarketDataRequirement> _marketDataRequests =
      Collections.newSetFromMap(new ConcurrentHashMap<MarketDataRequirement, Boolean>());

  @Override
  public Result<MarketDataValues> requestData(MarketDataRequirement requirement) {
    return requestData(ImmutableSet.of(requirement));
  }

  @Override
  public Result<MarketDataValues> requestData(Set<MarketDataRequirement> requirements) {
    MarketDataValuesResultBuilder builder = new MarketDataValuesResultBuilder();
    for (MarketDataRequirement requirement : requirements) {
      if (_requestedMarketData.containsKey(requirement)) {
        builder.foundData(requirement, _requestedMarketData.get(requirement));
      } else {
        _marketDataRequests.add(requirement);
        builder.missingData(requirement, MarketDataStatus.PENDING);
      }
    }
    return builder.build();
  }

  @Override
  public Set<MarketDataRequirement> getCollectedRequests() {
    return _marketDataRequests;
  }

  @Override
  public void resetMarketData(Map<MarketDataRequirement, MarketDataItem> replacementData) {
    _marketDataRequests.clear();
    _requestedMarketData.clear();
    _requestedMarketData.putAll(replacementData);
  }

}
