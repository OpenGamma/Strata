/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Map;
import java.util.Set;

/**
 * Extension to the MarketDataProviderFunction which adds management methods.
 * Implementations of this interface will collect the market data requests
 * as they are being made which can be queried once requests have been completed.
 * Additionally, the currently available market data can be replaced with new data.
 * <p>
 * The intention is that an empty provider function is used on the first engine
 * cycle. This will get populated with the market data requirements for the
 * calculations being used as client functions call the {@link #requestData(MarketDataRequirement)}
 * method. In subsequent cycles, the function will be populated
 * with availability data indicating the state of the market data requests
 * (e.g. requested but not received, not available etc) and thus client functions
 * will receive the data they need to complete their calculations. Changes such as
 * trades being added may result in additional requests being made in later cycles.
 */
public interface ResettableMarketDataFn extends MarketDataFn {

  /**
   * Return the set of market data that has been requested for which no status data
   * was available. i.e. this indicates requests for data that have not been made or
   * known about previously.
   *
   * @return the requests that have been made
   */
  Set<MarketDataRequirement> getCollectedRequests();

  /**
   * Reset the data about which requests have been made and the market availability data. The latter
   * will be replaced by the data passed in.
   *
   * @param replacementData  the new map of availability data
   */
  void resetMarketData(Map<MarketDataRequirement, MarketDataItem> replacementData);

}
