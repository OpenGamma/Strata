/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.mapping;

import com.opengamma.strata.marketdata.id.MarketDataId;
import com.opengamma.strata.marketdata.key.MarketDataKey;

/**
 * Market data mapping implementation used when there is no mapping for an ID. It throws an exception
 * from every method.
 */
final class FailureMapping implements MarketDataMapping<Object, MarketDataKey<Object>> {

  @Override
  public Class<? extends MarketDataKey<Object>> getMarketDataKeyType() {
    throw new UnsupportedOperationException("getMarketDataIdType not supported");
  }

  @Override
  public MarketDataId<Object> getIdForKey(MarketDataKey<Object> key) {
    throw new IllegalArgumentException("No market data mapping available for ID type " + key.getClass().getName());
  }
}
