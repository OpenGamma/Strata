/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine;

import java.util.List;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.engine.calculations.CalculationRunner;
import com.opengamma.strata.engine.calculations.CalculationTasks;
import com.opengamma.strata.engine.calculations.Results;
import com.opengamma.strata.engine.config.CalculationTasksConfig;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.MarketDataFactory;
import com.opengamma.strata.engine.marketdata.MarketDataResult;
import com.opengamma.strata.engine.marketdata.ScenarioMarketData;
import com.opengamma.strata.engine.marketdata.scenarios.ScenarioDefinition;

/**
 * Default implementation of {@link CalculationEngine} that delegates to a {@link CalculationRunner}
 * and a {@link MarketDataFactory}.
 */
public final class DefaultCalculationEngine implements CalculationEngine {

  /** The calculation runner that performs the calculations. */
  private final CalculationRunner calculationRunner;

  /** The factory that builds any market data not supplied by the caller. */
  private final MarketDataFactory marketDataFactory;

  /**
   * @param calculationRunner  the calculation runner that performs the calculations
   * @param marketDataFactory  the factory that builds any market data not supplied by the caller
   */
  public DefaultCalculationEngine(CalculationRunner calculationRunner, MarketDataFactory marketDataFactory) {
    this.calculationRunner = ArgChecker.notNull(calculationRunner, "calculationRunner");
    this.marketDataFactory = ArgChecker.notNull(marketDataFactory, "marketDataFactory");
  }

  @Override
  public Results calculate(
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      CalculationRules calculationRules,
      BaseMarketData marketData) {

    CalculationTasksConfig config =
        calculationRunner.createCalculationConfig(
            targets,
            columns,
            calculationRules.getPricingRules(),
            calculationRules.getMarketDataRules(),
            calculationRules.getReportingRules());
    
    CalculationTasks tasks = calculationRunner.createCalculationTasks(config);

    MarketDataResult marketDataResult =
        marketDataFactory.buildBaseMarketData(
            tasks.getMarketDataRequirements(),
            marketData);

    return calculationRunner.calculate(tasks, marketDataResult.getMarketData());
  }

  @Override
  public Results calculate(
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      CalculationRules calculationRules,
      BaseMarketData baseMarketData,
      ScenarioDefinition scenarioDefinition) {

    CalculationTasksConfig config =
        calculationRunner.createCalculationConfig(
            targets,
            columns,
            calculationRules.getPricingRules(),
            calculationRules.getMarketDataRules(),
            calculationRules.getReportingRules());

    CalculationTasks tasks = calculationRunner.createCalculationTasks(config);

    MarketDataResult marketDataResult =
        marketDataFactory.buildBaseMarketData(
            tasks.getMarketDataRequirements(),
            baseMarketData);

    ScenarioMarketData scenarioMarketData =
        marketDataFactory.buildScenarioMarketData(
            marketDataResult.getMarketData(),
            scenarioDefinition);

    return calculationRunner.calculate(tasks, scenarioMarketData);
  }
}
