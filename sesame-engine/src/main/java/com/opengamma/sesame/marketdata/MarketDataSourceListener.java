/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.id.ExternalIdBundle;
import com.opengamma.util.result.Result;

/**
 * A listener for market data sources. When registered with a
 * {@link ProxiedCycleMarketData} it will get called whenever
 * the market data source has requests made of it.
 */
public interface MarketDataSourceListener {

  /**
   * Called when the underlying market data source has a request
   * made of it.
   *
   * @param id the id of the market data
   * @param fieldName the field name of the market data
   * @param result the result returned by the underlying source
   */
  void requestMade(ExternalIdBundle id, FieldName fieldName, Result<?> result);
}
