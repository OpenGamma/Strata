/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import java.util.Set;

import com.opengamma.sesame.marketdata.MarketDataFn;
import com.opengamma.sesame.marketdata.MarketDataRequirement;
import com.opengamma.sesame.marketdata.MarketDataValues;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.result.Result;

/**
 * Simple delegating market data provider that allows the engine to switch market data providers between cycles
 * while the functions still point at the same provider.
 */
/* package */ class DelegatingMarketDataFn implements MarketDataFn {

  private MarketDataFn _delegate;

  /**
   * Sets the underlying provider, should only be called between calculation cycles.
   * @param delegate The underlying market data provider
   */
  /* package */ void setDelegate(MarketDataFn delegate) {
    _delegate = ArgumentChecker.notNull(delegate, "delegate");
  }

  @Override
  public Result<MarketDataValues> requestData(MarketDataRequirement requirement) {
    return _delegate.requestData(requirement);
  }

  @Override
  public Result<MarketDataValues> requestData(Set<MarketDataRequirement> requirements) {
    return _delegate.requestData(requirements);
  }
}
