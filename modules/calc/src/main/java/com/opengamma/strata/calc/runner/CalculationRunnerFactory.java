/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.util.List;
import java.util.concurrent.ExecutorService;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.marketdata.MarketDataFactory;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.scenario.ScenarioDefinition;

/**
 * Factory allowing instances of {@code CalculationRunner} to be created.
 * <p>
 * The strata-pricer project provides the ability to calculate results for a single trade,
 * single measure and single set of market data. {@link CalculationRunner} provides the ability
 * to calculate results for many trades, many measures and many sets of market data.
 * This factory is used to create instances of the runner.
 * <p>
 * The static methods on this interface provide standard ways to create the factory instance.
 * They select between single and multi threading, or allow an explicit executor to be passed in.
 * <p>
 * The instance methods on this interface provide the ability to create a calculation runner.
 * Two types of runner are available, simple and market data aware.
 * In many cases, the caller will already have a complete set of market data, thus the simple runner is sufficient.
 * If curve calibration or scenario creation is to be performed, a market data aware runner must be used.
 */
public interface CalculationRunnerFactory {

  /**
   * Creates a single-threaded calculation runner capable of performing calculations.
   *
   * @return the calculation runner factory
   */
  public static CalculationRunnerFactory ofSingleThreaded() {
    return DefaultCalculationRunnerFactory.ofSingleThreaded();
  }

  /**
   * Creates a multi-threaded calculation runner capable of performing calculations.
   *
   * @return the calculation runner factory
   */
  public static CalculationRunnerFactory ofMultiThreaded() {
    return DefaultCalculationRunnerFactory.ofMultiThreaded();
  }

  /**
   * Creates a calculation runner capable of performing calculations, specifying the executor.
   *
   * @param executor  the executor to use
   * @return the calculation runner factory
   */
  public static CalculationRunnerFactory of(ExecutorService executor) {
    return new DefaultCalculationRunnerFactory(executor);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a runner capable of calculating measures for a set of targets.
   * <p>
   * The runner embeds the specified targets, columns and rules.
   * The targets and columns are the two dimensions of the grid of results.
   *
   * @param targets  the targets for which values of the measures will be calculated
   * @param columns  the configuration for the columns that will be calculated, including the measure and
   *   any column-specific overrides
   * @param calculationRules  the rules defining how the calculation is performed
   * @return the calculation runner
   */
  public abstract CalculationRunner create(
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      CalculationRules calculationRules);

  /**
   * Creates a runner capable of calculating measures for a set of targets,
   * with the ability to calibrate market data.
   * <p>
   * The runner embeds the specified targets, columns and rules.
   * The targets and columns are the two dimensions of the grid of results.
   * <p>
   * The market data factory and configuration provide the ability to calibrate additional market data.
   * Internally, this determines the market data needed by the targets and columns and requests
   * the market data factory to build it.
   *
   * @param targets  the targets for which values of the measures will be calculated
   * @param columns  the configuration for the columns that will be calculated, including the measure and
   *   any column-specific overrides
   * @param calculationRules  the rules defining how the calculation is performed
   * @param marketDataFactory  the market data factory
   * @param marketDataConfig  the market data configuration
   * @return the calculation runner
   */
  public abstract CalculationRunner createWithMarketDataBuilder(
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      CalculationRules calculationRules,
      MarketDataFactory marketDataFactory,
      MarketDataConfig marketDataConfig);

  /**
   * Creates a runner capable of calculating measures for a set of targets,
   * with the ability to calibrate market data and create scenarios.
   * <p>
   * The runner embeds the specified targets, columns and rules.
   * The targets and columns are the two dimensions of the grid of results.
   * <p>
   * The market data factory and configuration provide the ability to calibrate additional market data.
   * Internally, this determines the market data needed by the targets and columns and requests
   * the market data factory to build it, taking into account the scenario definition
   *
   * @param targets  the targets for which values of the measures will be calculated
   * @param columns  the configuration for the columns that will be calculated, including the measure and
   *   any column-specific overrides
   * @param calculationRules  the rules defining how the calculation is performed
   * @param marketDataFactory  the market data factory
   * @param marketDataConfig  the market data configuration
   * @param scenarioDefinition  the scenario definition
   * @return the calculation runner
   */
  public abstract CalculationRunner createWithMarketDataBuilder(
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      CalculationRules calculationRules,
      MarketDataFactory marketDataFactory,
      MarketDataConfig marketDataConfig,
      ScenarioDefinition scenarioDefinition);

}
