/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.util.ArgumentChecker;

/**
 * Returns an instance of {@link MarketDataProviderFunction}.
 */
public class SimpleMarketDataFactory implements MarketDataFactory {

  private final MarketDataProviderFunction _function;

  public SimpleMarketDataFactory(MarketDataProviderFunction function) {
    _function = ArgumentChecker.notNull(function, "function");
  }

  @Override
  public MarketDataProviderFunction create(ComponentMap components) {
    return _function;
  }
}
