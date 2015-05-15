/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.config;

// TODO This will be implemented when the market data functions are ported over that perform calibration
/**
 * Configuration required for building non-observable market data, for example curves or surfaces.
 */
public interface MarketDataConfig {

  /**
   * Returns an empty set of market data configuration.
   *
   * @return an empty set of market data configuration
   */
  public static MarketDataConfig empty() {
    return DefaultMarketDataConfig.EMPTY;
  }
}
