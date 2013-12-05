/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.util.ArgumentChecker;

/**
 * Returns an instance of {@link MarketDataFn}.
 */
public class SimpleMarketDataFactory implements MarketDataFactory {

  private final MarketDataFn _function;

  public SimpleMarketDataFactory(MarketDataFn function) {
    _function = ArgumentChecker.notNull(function, "function");
  }

  @Override
  public MarketDataFn create(ComponentMap components) {
    return _function;
  }
}
