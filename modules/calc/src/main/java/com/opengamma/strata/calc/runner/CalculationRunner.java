/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import com.opengamma.strata.calc.marketdata.CalculationEnvironment;

/**
 * Provides the ability to run calculations on multiple targets, measures and scenarios.
 * <p>
 * The strata-pricer project provides the ability to calculate results for a single trade,
 * single measure and single set of market data. {@code CalculationRunner} provides the ability
 * to calculate results for many trades, many measures and many sets of market data.
 * <p>
 * The trades and measures form part of the state of an instance of {@code CalculationRunner}.
 * The available methods provide access to that state, plus four different ways to calculate results.
 * The handle the combination of single market data versus scenario market data, and synchronous
 * versus asynchronous.
 * <p>
 * A calculation runner is obtained using {@link CalculationRunnerFactory}:
 * <pre>
 *  runner = CalculationRunnerFactory.ofMultiThreaded().create(trades, columns, rules);
 * </pre>
 */
public interface CalculationRunner {

  /**
   * Gets the tasks that perform the individual calculations.
   * <p>
   * The results can be visualized as a grid of columns with a row for each target.
   * There is one calculation task for each cell in the grid.
   * The tasks in the list are arranged in row order.
   * For example, if a grid has 5 columns, the calculations 0-4 in the list are the first row,
   * calculations 5-9 are the second row and so on.
   * <p>
   * The tasks object provides access to the list of targets, columns and tasks.
   * It also provides the ability to query what market data is needed to perform pricing.
   *
   * @return the tasks that perform the calculations
   */
  public abstract CalculationTasks getTasks();

  //-------------------------------------------------------------------------
  /**
   * Performs calculations for a single set of market data.
   *
   * @param marketData  market data to be used in the calculations
   * @return the calculation results
   */
  public abstract Results calculateSingleScenario(CalculationEnvironment marketData);

  /**
   * Performs calculations for multiple scenarios, each with a different set of market data.
   *
   * @param marketData  the market data used in the calculations
   * @return the results of running the calculations in the view for every item in the portfolio and every scenario
   */
  public abstract Results calculateMultipleScenarios(CalculationEnvironment marketData);

  //-------------------------------------------------------------------------
  /**
   * Performs calculations asynchronously for a single scenario,
   * invoking a listener as each calculation completes.
   * <p>
   * This method requires the listener to assemble the results, but it can be much more memory efficient when
   * calculating aggregate results. If the individual results are discarded after they are incorporated into
   * the aggregate they can be garbage collected.
   *
   * @param marketData  market data to be used in the calculations
   * @param listener  listener that is invoked when individual results are calculated
   */
  public abstract void calculateSingleScenarioAsync(CalculationEnvironment marketData, CalculationListener listener);

  /**
   * Performs calculations asynchronously for a multiple scenarios, each with a different set of market data,
   * invoking a listener as each calculation completes.
   * <p>
   * This method requires the listener to assemble the results, but it can be much more memory efficient when
   * calculating aggregate results. If the individual results are discarded after they are incorporated into
   * the aggregate they can be garbage collected.
   *
   * @param marketData  the market data used in the calculations
   * @param listener  listener that is invoked when individual results are calculated
   */
  public abstract void calculateMultipleScenariosAsync(CalculationEnvironment marketData, CalculationListener listener);

}
