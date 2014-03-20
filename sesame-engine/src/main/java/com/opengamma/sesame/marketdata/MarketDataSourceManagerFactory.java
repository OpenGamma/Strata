/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

/**
 * Factory for creating instances of {@link MarketDataSourceManager}.
 * This is required as a MarketDataSourceManager is scoped to a
 * particular client of the engine.
 */
public interface MarketDataSourceManagerFactory {

  /**
   * Creates a {@link MarketDataSourceManager} for use by
   * the current client. Note that if no live data is required,
   * the factory may return the same instance on each call.
   *
   * @return a {@link MarketDataSourceManager} for use by
   * the current client
   */
  MarketDataSourceManager createMarketDataSourceManager();
}
