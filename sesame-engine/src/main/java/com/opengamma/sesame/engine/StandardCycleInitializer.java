/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import com.opengamma.service.ServiceContext;
import com.opengamma.sesame.graph.Graph;
import com.opengamma.sesame.marketdata.MarketDataSource;

/**
 * A cycle initializer to be used in standard (non-capturing)
 * cycles.
 */
class StandardCycleInitializer implements CycleInitializer {

  private final ServiceContext _originalContext;
  private final MarketDataSource _marketDataSource;
  private final Graph _graph;

  /**
   * Create the cycle initializer.
   *
   * @param originalContext the current service context
   * @param marketDataSource the current market data source
   * @param graph the current graph
   */
  StandardCycleInitializer(ServiceContext originalContext,
                           MarketDataSource marketDataSource,
                           Graph graph) {
    _originalContext = originalContext;
    _marketDataSource = marketDataSource;
    _graph = graph;
  }

  @Override
  public MarketDataSource getMarketDataSource() {
    return _marketDataSource;
  }

  @Override
  public ServiceContext getServiceContext() {
    return _originalContext;
  }

  @Override
  public Graph getGraph() {
    return _graph;
  }

  /**
   * No processing to be done, just returns the results directly.
   *
   * @param results  the results of the cycle run
   * @return the supplied results unchanged
   */
  @Override
  public Results complete(Results results) {
    return results;
  }
}
