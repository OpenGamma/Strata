/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.sesame.engine.ComponentMap;
import com.opengamma.util.ArgumentChecker;

/**
 * Creates a {@link MarketDataProviderFunction} given a {@link MarketDataSpecification}.
 */
public class SpecificationMarketDataFactory implements MarketDataFactory {

  private final MarketDataSpecification _marketDataSpecification;

  public SpecificationMarketDataFactory(MarketDataSpecification marketDataSpecification) {
    _marketDataSpecification = ArgumentChecker.notNull(marketDataSpecification, "marketDataSpecification");
  }

  @Override
  public MarketDataProviderFunction create(ComponentMap components) {
    // TODO implement create()
    throw new UnsupportedOperationException("create not implemented");
  }
}
