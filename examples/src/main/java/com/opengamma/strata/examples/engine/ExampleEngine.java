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
import com.opengamma.strata.engine.calculation.CalculationRunner;
import com.opengamma.strata.engine.calculation.DefaultCalculationRunner;
import com.opengamma.strata.engine.marketdata.DefaultMarketDataFactory;
import com.opengamma.strata.engine.marketdata.MarketDataFactory;
import com.opengamma.strata.engine.marketdata.function.ObservableMarketDataFunction;
import com.opengamma.strata.engine.marketdata.function.TimeSeriesProvider;
import com.opengamma.strata.engine.marketdata.mapping.FeedIdMapping;
import com.opengamma.strata.examples.marketdata.ExampleMarketData;
import com.opengamma.strata.function.marketdata.curve.DiscountFactorsMarketDataFunction;
import com.opengamma.strata.function.marketdata.curve.IborIndexRatesMarketDataFunction;
import com.opengamma.strata.function.marketdata.curve.OvernightIndexRatesMarketDataFunction;

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
   * The engine is not wired up to any external market data sources, so all required market
   * data must be present in the snapshot passed to the engine when it is invoked.
   * <p>
   * The engine may be used in conjunction with {@link ExampleMarketData} which provides
   * access to snapshots of example data.
   * 
   * @return a calculation engine instance
   */
  public static CalculationEngine create() {
    // create the calculation runner that calculates the results
    ExecutorService executor = createExecutor();
    CalculationRunner calcRunner = new DefaultCalculationRunner(executor);

    // create the market data factory that builds market data
    MarketDataFactory marketDataFactory = new DefaultMarketDataFactory(
        TimeSeriesProvider.none(),
        ObservableMarketDataFunction.none(),
        FeedIdMapping.identity(),
        new DiscountFactorsMarketDataFunction(),
        new IborIndexRatesMarketDataFunction(),
        new OvernightIndexRatesMarketDataFunction());

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
