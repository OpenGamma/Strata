/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.engine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.engine.CalculationEngine;
import com.opengamma.strata.engine.DefaultCalculationEngine;
import com.opengamma.strata.engine.calculations.CalculationRunner;
import com.opengamma.strata.engine.calculations.DefaultCalculationRunner;
import com.opengamma.strata.engine.marketdata.DefaultMarketDataFactory;
import com.opengamma.strata.engine.marketdata.MarketDataFactory;
import com.opengamma.strata.engine.marketdata.builders.ObservableMarketDataBuilder;
import com.opengamma.strata.engine.marketdata.mapping.FeedIdMapping;
import com.opengamma.strata.examples.marketdata.ExampleDiscountingCurveBuilder;
import com.opengamma.strata.examples.marketdata.ExampleForwardCurveBuilder;
import com.opengamma.strata.examples.marketdata.ExampleTimeSeriesProvider;

/**
 * Contains utility methods for obtaining a calculation engine configured for use
 * in the examples environment.
 */
public final class ExampleEngine {

  /**
   * Restricted constructor.
   */
  private ExampleEngine() {
  }
  
  /**
   * Creates a calculation engine instance configured for use in the examples environment.
   * <p>
   * The engine is configured to source market data from JSON resources using the
   * example builders and example time-series provider. It has no ability to perform
   * link resolution and operates with a single calculation thread.
   * 
   * @return a new calculation engine instance
   */
  public static CalculationEngine create() {
    ExecutorService executor = createExecutor();
    CalculationRunner calcRunner = new DefaultCalculationRunner(executor);
    
    ExampleTimeSeriesProvider timeSeriesProvider = new ExampleTimeSeriesProvider();
    ExampleDiscountingCurveBuilder discountingCurveBuilder = new ExampleDiscountingCurveBuilder();
    ExampleForwardCurveBuilder forwardCurveBuilder = new ExampleForwardCurveBuilder();
    
    MarketDataFactory marketDataFactory = new DefaultMarketDataFactory(
        timeSeriesProvider,
        ObservableMarketDataBuilder.none(),
        FeedIdMapping.identity(),
        discountingCurveBuilder, forwardCurveBuilder);
    
    return new DefaultCalculationEngine(calcRunner, marketDataFactory, LinkResolver.none());
  }

  private static ExecutorService createExecutor() {
    ExecutorService executor = Executors.newFixedThreadPool(1, r ->  {
      Thread t = Executors.defaultThreadFactory().newThread(r);
      t.setDaemon(true);
      return t;
    });
    return executor;
  }
  
}
