/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Map;

import com.opengamma.core.marketdatasnapshot.SnapshotDataBundle;
import com.opengamma.sesame.FunctionResult;

// TODO a class for the type parameter?
// TODO type parameter for the value? Pair<status, value> -> MarketDataItem?
// or a type instead of the map? MarketDataItem getValue(MarketDataRequirement)
public interface MarketDataSingleResult extends FunctionResult<Map<MarketDataRequirement, MarketDataItem<?>>> {

  <T> MarketDataValue<T> getSingleValue();

  // TODO is this the right thing to do? should it return a MarketDataSeries? or should there be a different result type?
  //<T> MarketDataValue<T> getMarketDataSeries();

  MarketDataStatus getStatus(MarketDataRequirement requirement);

  <T> MarketDataValue<T> getValue(MarketDataRequirement requirement);

  /**
   * Temporary method to allow conversion to the old-style market data bundle.
   *
   * @return a snapshot bundle with the data from the result
   */
  SnapshotDataBundle toSnapshot();
}
