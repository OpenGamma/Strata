/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.examples.exampleccp;

import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.CalculationEngine;
import com.opengamma.strata.engine.DefaultCalculationEngine;
import com.opengamma.strata.engine.calculations.CalculationRunner;
import com.opengamma.strata.engine.calculations.DefaultCalculationRunner;
import com.opengamma.strata.engine.marketdata.DefaultMarketDataFactory;
import com.opengamma.strata.engine.marketdata.MarketDataFactory;
import com.opengamma.strata.engine.marketdata.functions.ObservableMarketDataFunction;
import com.opengamma.strata.engine.marketdata.functions.TimeSeriesProvider;
import com.opengamma.strata.engine.marketdata.mapping.FeedIdMapping;
import com.opengamma.strata.examples.exampleccp.marketdatafunctions.MyRateIndexCurveMarketDataFunction;
import com.opengamma.strata.examples.exampleccp.marketdatafunctions.MyZeroRateDiscountFactorsMarketDataFunction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MyCalculationEngine {

  /**
   * Restricted constructor.
   */
  private MyCalculationEngine() {
  }

  public static CalculationEngine create() {
    // create the calculation runner, that calculates the results
    ExecutorService executor = createExecutor();
    CalculationRunner calcRunner = new DefaultCalculationRunner(executor);

    TimeSeriesProvider tsProvider = id -> Result.success(LocalDateDoubleTimeSeries.empty());
    // create the market data factory that builds market data
    MarketDataFactory marketDataFactory = new DefaultMarketDataFactory(
        tsProvider,
        ObservableMarketDataFunction.none(),
        FeedIdMapping.identity(),
        MyZeroRateDiscountFactorsMarketDataFunction.create(),
        MyRateIndexCurveMarketDataFunction.create()
    );

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
