/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.List;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.config.CalculationTasksConfig;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.MarketDataFactory;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.scenario.ScenarioDefinition;
import com.opengamma.strata.calc.runner.CalculationRunner;
import com.opengamma.strata.calc.runner.CalculationTasks;
import com.opengamma.strata.calc.runner.Results;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.id.LinkResolver;

/**
 * Default implementation of a calculation engine.
 * <p>
 * The {@link CalculationEngine} is the main entry point for performing calculations.
 * It calculates a grid of results, where each row represents the input calculation target,
 * typically a trade, and each column represents the value of a measure for that target.
 * <p>
 * This implementation delegates the main calculation to a {@link CalculationRunner}.
 * Market data is built using a {@link MarketDataFactory}.
 * Any links in the input targets will be resolved using a {@link LinkResolver}.
 */
public final class DefaultCalculationEngine implements CalculationEngine {

  /**
   * The calculation runner that performs the calculations.
   * <p>
   * The runner is responsible for running through each cell to calculate the result grid.
   */
  private final CalculationRunner calculationRunner;
  /**
   * The factory that builds any market data not supplied by the caller.
   * <p>
   * The factory is responsible for ensuring that enough market data is available to calculate the results.
   */
  private final MarketDataFactory marketDataFactory;
  /**
   * The link resolver, that resolves any links in the calculation targets.
   * <p>
   * Links exist to provide loose coupling between different parts of the object model.
   * All links are resolved using this resolver prior to invoking the calculation runner.
   * <p>
   * For example, trades in fungible securities use a link to refer to the security instead of
   * embedding the security details in the trade. In order for the security to be accessible
   * to the calculation logic, the link must be resolved.
   */
  private final LinkResolver linkResolver;

  /**
   * Creates an instance, specifying the runner, market data factory and link resolver.
   * 
   * @param calculationRunner  the calculation runner that performs the calculations
   * @param marketDataFactory  the factory that builds any market data not supplied by the caller
   * @param linkResolver  resolves links in the calculation targets to reference the linked objects
   */
  public DefaultCalculationEngine(
      CalculationRunner calculationRunner,
      MarketDataFactory marketDataFactory,
      LinkResolver linkResolver) {

    this.calculationRunner = ArgChecker.notNull(calculationRunner, "calculationRunner");
    this.marketDataFactory = ArgChecker.notNull(marketDataFactory, "marketDataFactory");
    this.linkResolver = ArgChecker.notNull(linkResolver, "linkResolver");
  }

  //-------------------------------------------------------------------------
  @Override
  public Results calculate(
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      CalculationRules calculationRules,
      CalculationEnvironment marketEnvironment) {

    // create the tasks to be run
    CalculationTasksConfig config = calculationRunner.createCalculationConfig(
        resolveTargetLinks(targets),
        columns,
        calculationRules.getPricingRules(),
        calculationRules.getMarketDataRules(),
        calculationRules.getReportingRules());
    CalculationTasks tasks = calculationRunner.createCalculationTasks(config);

    // build any missing market data
    MarketEnvironment marketData = marketDataFactory.buildMarketData(
        tasks.getRequirements(),
        marketEnvironment,
        calculationRules.getMarketDataConfig());

    // perform the calculations
    return calculationRunner.calculateSingleScenario(tasks, marketData);
  }

  @Override
  public Results calculate(
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      CalculationRules calculationRules,
      CalculationEnvironment suppliedMarketData,
      ScenarioDefinition scenarioDefinition) {

    // create the tasks to be run
    CalculationTasksConfig config = calculationRunner.createCalculationConfig(
        resolveTargetLinks(targets),
        columns,
        calculationRules.getPricingRules(),
        calculationRules.getMarketDataRules(),
        calculationRules.getReportingRules());
    CalculationTasks tasks = calculationRunner.createCalculationTasks(config);

    // build any required scenarios from the base market data
    MarketEnvironment marketData = marketDataFactory.buildMarketData(
        tasks.getRequirements(),
        suppliedMarketData,
        calculationRules.getMarketDataConfig(),
        scenarioDefinition);

    // perform the calculations
    return calculationRunner.calculateMultipleScenarios(tasks, marketData);
  }

  /**
   * Returns calculation targets with any links resolved to reference the linked objects.
   *
   * @param targets  the calculation targets
   * @return the targets with any links resolved to reference the linked objects
   */
  private List<CalculationTarget> resolveTargetLinks(List<? extends CalculationTarget> targets) {
    return targets.stream()
        .map(t -> (CalculationTarget) t)  // annoying cast for Eclipse
        .map(linkResolver::resolveLinksIn)
        .collect(toImmutableList());
  }
}
