/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.engine.marketdata.spec.MarketDataSpecification;
import com.opengamma.util.ArgumentChecker;

/**
 * Manages the live (non-eager) market data sources for a single
 * client of the engine. The {@link StrategyAwareMarketDataSource}
 * instances that this class creates can then be examined between
 * requests to determine the requests to be made from the live market
 * data server.
 */
public class DefaultMarketDataSourceManager implements MarketDataSourceManager {

  /**
   * The market data factory servicing non-live market data requests, not null.
   */
  private final MarketDataFactory _marketDataFactory;

  /**
   * Create the manager.
   *
   * @param marketDataFactory the market data factory servicing non-live
   * market data requests, not null
   */
  public DefaultMarketDataSourceManager(MarketDataFactory marketDataFactory) {
    _marketDataFactory = ArgumentChecker.notNull(marketDataFactory, "marketDataFactory");
  }

  @Override
  public StrategyAwareMarketDataSource createStrategyAwareSource(StrategyAwareMarketDataSource previousDataSource, MarketDataSpecification marketDataSpec) {
    if (previousDataSource.isCompatible(marketDataSpec)) {
      return previousDataSource.createPrimedSource();
    } else {
      previousDataSource.dispose();
      return _marketDataFactory.create(marketDataSpec);      
    }
  }
  
}
