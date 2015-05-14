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
import com.opengamma.strata.engine.marketdata.functions.ObservableMarketDataFunction;
import com.opengamma.strata.engine.marketdata.mapping.FeedIdMapping;
import com.opengamma.strata.examples.marketdata.ExampleDiscountingCurveFunction;
import com.opengamma.strata.examples.marketdata.ExampleForwardCurveFunction;
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

  //-------------------------------------------------------------------------
  /**
   * Creates a calculation engine instance configured for use in the examples environment.
   * <p>
   * The engine is configured to source market data from JSON resources using the
   * example functions and example time-series provider. It has no ability to perform
   * link resolution and operates with a single calculation thread.
   * 
   * @return a new calculation engine instance
   */
  public static CalculationEngine create() {
    // create the calculation runner, that calculates the results
    ExecutorService executor = createExecutor();
    CalculationRunner calcRunner = new DefaultCalculationRunner(executor);

    // create the market data factory, that builds market data
    ExampleTimeSeriesProvider timeSeriesProvider = new ExampleTimeSeriesProvider();
    ExampleDiscountingCurveFunction discountingCurveBuilder = new ExampleDiscountingCurveFunction();
    ExampleForwardCurveFunction forwardCurveBuilder = new ExampleForwardCurveFunction();
    MarketDataFactory marketDataFactory = new DefaultMarketDataFactory(
        timeSeriesProvider,
        ObservableMarketDataFunction.none(),
        FeedIdMapping.identity(),
        discountingCurveBuilder,
        forwardCurveBuilder);

    // combine the runner and market data factory
    return new DefaultCalculationEngine(calcRunner, marketDataFactory, LinkResolver.none());
  }

  // create an executor with daemon threads
  private static ExecutorService createExecutor() {
    ExecutorService executor = Executors.newFixedThreadPool(1, r -> {
      Thread t = Executors.defaultThreadFactory().newThread(r);
      t.setDaemon(true);
      return t;
    });
    return executor;
  }

}
