/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.MarketDataFactory;
import com.opengamma.strata.calc.marketdata.MarketDataRequirements;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.scenario.ScenarioDefinition;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Market data aware calculation runner.
 * <p>
 * This implementation is capable of calibrating market data based on the requirements of the runner.
 */
class MarketDataCalculationRunner implements CalculationRunner {

  /**
   * The underlying calculation runner.
   */
  private final CalculationRunner underlying;
  /**
   * The market data factory to use.
   */
  private final MarketDataFactory marketDataFactory;
  /**
   * The market data factory to use.
   */
  private final MarketDataConfig marketDataConfig;
  /**
   * The scenario definition to use.
   */
  private final ScenarioDefinition scenarioDefinition;

  /**
   * Creates an instance.
   * 
   * @param underlying  the underlying calculation runner
   * @param marketDataFactory  the market data factory
   * @param marketDataConfig  the market data configuration
   * @param scenarioDefinition  the scenario definition
   */
  MarketDataCalculationRunner(
      CalculationRunner underlying,
      MarketDataFactory marketDataFactory,
      MarketDataConfig marketDataConfig,
      ScenarioDefinition scenarioDefinition) {

    this.underlying = ArgChecker.notNull(underlying, "underlying");
    this.marketDataFactory = ArgChecker.notNull(marketDataFactory, "marketDataFactory");
    this.marketDataConfig = ArgChecker.notNull(marketDataConfig, "marketDataConfig");
    this.scenarioDefinition = ArgChecker.notNull(scenarioDefinition, "scenarioDefinition");
  }

  //-------------------------------------------------------------------------
  @Override
  public CalculationTasks getTasks() {
    return underlying.getTasks();
  }

  //-------------------------------------------------------------------------
  @Override
  public Results calculateSingleScenario(CalculationEnvironment marketData) {
    CalculationEnvironment calibratedData = calibrateMarketData(marketData);
    return underlying.calculateSingleScenario(calibratedData);
  }

  @Override
  public Results calculateMultipleScenarios(CalculationEnvironment marketData) {
    CalculationEnvironment calibratedData = calibrateMarketData(marketData);
    return underlying.calculateMultipleScenarios(calibratedData);
  }

  @Override
  public void calculateSingleScenarioAsync(CalculationEnvironment marketData, CalculationListener listener) {
    CalculationEnvironment calibratedData = calibrateMarketData(marketData);
    underlying.calculateSingleScenarioAsync(calibratedData, listener);
  }

  @Override
  public void calculateMultipleScenariosAsync(CalculationEnvironment marketData, CalculationListener listener) {
    CalculationEnvironment calibratedData = calibrateMarketData(marketData);
    underlying.calculateMultipleScenariosAsync(calibratedData, listener);
  }

  private CalculationEnvironment calibrateMarketData(CalculationEnvironment marketData) {
    MarketDataRequirements reqs = underlying.getTasks().getRequirements();
    CalculationEnvironment calibratedData =
        marketDataFactory.buildMarketData(reqs, marketData, marketDataConfig, scenarioDefinition);
    return calibratedData;
  }

}
