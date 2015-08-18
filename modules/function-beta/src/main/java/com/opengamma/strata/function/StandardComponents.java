/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.function;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.id.LinkResolver;
import com.opengamma.strata.engine.CalculationEngine;
import com.opengamma.strata.engine.DefaultCalculationEngine;
import com.opengamma.strata.engine.calculations.CalculationRunner;
import com.opengamma.strata.engine.calculations.DefaultCalculationRunner;
import com.opengamma.strata.engine.config.pricing.PricingRules;
import com.opengamma.strata.engine.marketdata.DefaultMarketDataFactory;
import com.opengamma.strata.engine.marketdata.MarketDataFactory;
import com.opengamma.strata.engine.marketdata.functions.MarketDataFunction;
import com.opengamma.strata.engine.marketdata.functions.ObservableMarketDataFunction;
import com.opengamma.strata.engine.marketdata.functions.TimeSeriesProvider;
import com.opengamma.strata.engine.marketdata.mapping.FeedIdMapping;
import com.opengamma.strata.function.marketdata.curve.CurveGroupMarketDataFunction;
import com.opengamma.strata.function.marketdata.curve.DiscountCurveMarketDataFunction;
import com.opengamma.strata.function.marketdata.curve.DiscountFactorsMarketDataFunction;
import com.opengamma.strata.function.marketdata.curve.IborIndexRatesMarketDataFunction;
import com.opengamma.strata.function.marketdata.curve.OvernightIndexRatesMarketDataFunction;
import com.opengamma.strata.function.marketdata.curve.ParRatesMarketDataFunction;
import com.opengamma.strata.function.marketdata.curve.RateIndexCurveMarketDataFunction;
import com.opengamma.strata.function.marketdata.curve.RootFinderConfig;

/**
 * Factory methods for creating standard Strata components.
 * <p>
 * These components are suitable for performing calculations using the built-in asset classes, market data types
 * and pricers. The market data must provided by the user.
 * <p>
 * The market data factory can create market data values derived from other values. For example it can create
 * calibrated curves given market quotes. However it cannot request market data from an external provider, for
 * example Bloomberg, or look up data from a data store, for example a time series database.
 */
public class StandardComponents {

  // Only static helper methods so no need to create any instances
  private StandardComponents() {
  }

  /**
   * Returns a calculation engine capable of calculating the standard set of measures for the standard asset classes,
   * using market data provided by the caller.
   * <p>
   * The engine can create market data values derived from other values. For example it can create
   * calibrated curves given market quotes. However it cannot request market data from an external provider, for
   * example Bloomberg, or look up data from a data store, for example a time series database.
   *
   * @return a calculation engine capable of performing calculations for the built-in market data types and measures
   * using market data provided by the caller
   */
  public CalculationEngine calculationEngine() {
    return new DefaultCalculationEngine(calculationRunner(), marketDataFactory(), LinkResolver.none());
  }

  /**
   * Returns a calculation runner which uses a fixed thread pool.
   * <p>
   * The thread count is the same as the number of cores.
   *
   * @return a calculation runner which uses a fixed thread pool
   */
  public CalculationRunner calculationRunner() {
    ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    return new DefaultCalculationRunner(executor);
  }

  /**
   * Returns a market data factory containing the standard set of market data functions.
   * <p>
   * This factory can create market data values from other market data. For example it
   * can create calibrated curves given a set of market quotes for the points on the curve.
   * <p>
   * The set functions are the ones provided by {@link #marketDataFunctions()}.
   *
   * @return a market data factory containing the standard set of market data functions
   */
  public MarketDataFactory marketDataFactory() {
    return new DefaultMarketDataFactory(
        TimeSeriesProvider.none(),
        ObservableMarketDataFunction.none(),
        FeedIdMapping.identity(),
        marketDataFunctions());
  }

  /**
   * Returns the standard market data functions used to build market data values from other market data.
   * <p>
   * These include functions to build:
   * <ul>
   *   <li>Par rates from quotes</li>
   *   <li>Curve groups from par rates</li>
   *   <li>Curves from curve groups</li>
   *   <li>Discount factors from curves</li>
   * </ul>
   *
   * @return the standard market data functions
   */
  public List<MarketDataFunction<?, ?>> marketDataFunctions() {
    return ImmutableList.of(
        new DiscountCurveMarketDataFunction(),
        new RateIndexCurveMarketDataFunction(),
        new DiscountFactorsMarketDataFunction(),
        new IborIndexRatesMarketDataFunction(),
        new OvernightIndexRatesMarketDataFunction(),
        new CurveGroupMarketDataFunction(RootFinderConfig.defaults()), // RootFinderConfig will be removed #343
        new ParRatesMarketDataFunction());
  }

  /**
   * Returns pricing rules defining how to calculate the standard measures for the standard asset classes.
   *
   * @return pricing rules defining how to calculate the standard measures for the standard asset classes
   */
  public PricingRules pricingRules() {
    return OpenGammaPricingRules.standard();
  }
}
