/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.sesame.engine;

import com.opengamma.service.ServiceContext;
import com.opengamma.sesame.graph.Graph;
import com.opengamma.sesame.marketdata.CycleMarketDataFactory;

/**
 * A cycle initializer to be used in standard (non-capturing)
 * cycles.
 */
class StandardCycleInitializer implements CycleInitializer {

  private final ServiceContext _originalContext;
  private final CycleMarketDataFactory _cycleMarketDataFactory;
  private final Graph _graph;

  /**
   * Create the cycle initializer.
   *  @param originalContext the current service context
   * @param cycleMarketDataFactory the current market data source
   * @param graph the current graph
   */
  StandardCycleInitializer(ServiceContext originalContext,
                           CycleMarketDataFactory cycleMarketDataFactory,
                           Graph graph) {
    _originalContext = originalContext;
    _cycleMarketDataFactory = cycleMarketDataFactory;
    _graph = graph;
  }

  @Override
  public CycleMarketDataFactory getCycleMarketDataFactory() {
    return _cycleMarketDataFactory;
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
