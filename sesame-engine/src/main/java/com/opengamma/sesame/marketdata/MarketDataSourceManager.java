/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.marketdata;

import com.opengamma.engine.marketdata.spec.MarketDataSpecification;

/**
 * Responsible for creating {@link }StrategyAwareMarketDataSource}
 * instances. These are needed when non-eager (e.g. live) data
 * sources are in use.
 */
public interface MarketDataSourceManager {

  /**
   * Create a strategy aware source for the supplied specification, using
   * information from the previously provided source if appropriate. By
   * supplying the previous, it is possible to check what data was
   * requested and potentially make bulk requests for market data.
   *
   * @param previousDataSource the strategy aware source used
   * previously, not null
   * @param marketDataSpec the specification for the type of market
   * data required, not null
   * @return a suitable strategy aware source, not null
   */
  StrategyAwareMarketDataSource createStrategyAwareSource(StrategyAwareMarketDataSource previousDataSource,
                                                          MarketDataSpecification marketDataSpec);

}
