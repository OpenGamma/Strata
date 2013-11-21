/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import java.util.Map;

import com.opengamma.core.marketdatasnapshot.SnapshotDataBundle;
import com.opengamma.sesame.FunctionResult;
import com.opengamma.util.tuple.Pair;

// TODO a class for the type parameter?
// TODO type parameter for the value?
public interface MarketDataFunctionResult extends FunctionResult<Map<MarketDataRequirement, Pair<MarketDataStatus, ? extends MarketDataValue>>> {

  <T> MarketDataValue<T> getSingleMarketDataValue();

  // TODO is this the right thing to do? should it return a MarketDataSeries?
  //<T> MarketDataValue<T> getMarketDataSeries();

  MarketDataStatus getMarketDataState(MarketDataRequirement requirement);

  <T> MarketDataValue<T> getMarketDataValue(MarketDataRequirement requirement);

  /**
   * Temporary method to allow conversion to the old-style market data bundle.
   *
   * @return a snapshot bundle with the data from the result
   */
  SnapshotDataBundle toSnapshot();
}
