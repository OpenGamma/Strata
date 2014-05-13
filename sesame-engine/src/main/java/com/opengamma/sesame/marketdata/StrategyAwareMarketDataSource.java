/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.engine.marketdata.spec.MarketDataSpecification;

/**
 * Extension to the MarketDataSource that means an non-eager
 * source is able to capture and report back the market data
 * requests that have been made.
 */
public interface StrategyAwareMarketDataSource extends MarketDataSource {

  /**
   * Create a new market data source based on this one which
   * is primed with a new set of market data.
   *
   * @return a new market data source
   */
  StrategyAwareMarketDataSource createPrimedSource();

  /**
   * Indicates if this market data source is compatible with
   * the supplied specification.
   *
   * @param specification  the specification to check compatibility with
   * @return true if this source is compatible
   */
  boolean isCompatible(MarketDataSpecification specification);

  /**
   * Dispose of this market data source. Cleans up an resources
   * that may be associated with the source.
   */
  void dispose();
}
