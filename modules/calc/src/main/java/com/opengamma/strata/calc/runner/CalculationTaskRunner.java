/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.util.List;
import java.util.concurrent.ExecutorService;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.CalculationRunner;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

/**
 * Component that provides the ability to run calculation tasks.
 * <p>
 * This interface is the lower-level counterpart to {@link CalculationRunner}.
 * It provides the ability to calculate results based on {@link CalculationTasks}.
 * Unless you need to optimize, the {@code CalculationRunner} is a simpler entry point.
 * <p>
 * The purpose of the runner is to produce a grid of results, with a row for each target
 * and a column for each measure. The targets and columns that define the grid of results
 * are passed in using an instance of {@code CalculationTasks}.
 * <p>
 * The {@code CalculationTasks} instance is obtained using a
 * {@linkplain CalculationTasks#of(CalculationRules, List, List) static factory method}.
 * It consists of a list of {@code CalculationTask} instances, where each task instance
 * corresponds to a single cell in the grid of results. When the {@code CalculationTasks}
 * instance is created for a set of trades and measures some one-off initialization is performed.
 * Providing access to the instance allows the initialization to occur once, which could
 * be a performance optimization if many different calculations are performed with the
 * same set of trades and measures.
 * <p>
 * Once obtained, the {@code CalculationTasks} instance may be used to calculate results.
 * The four "calculate" methods handle the combination of single versus scenario market data,
 * and synchronous versus asynchronous.
 * <p>
 * A calculation runner is typically obtained using the static methods on this interface.
 * The instance contains an executor thread-pool, thus care should be taken to ensure
 * the thread-pool is correctly managed. For example, try-with-resources could be used:
 * <pre>
 *  try (CalculationTaskRunner runner = CalculationTaskRunner.ofMultiThreaded()) {
 *    // use the runner
 *  }
 * </pre>
 */
public interface CalculationTaskRunner extends AutoCloseable {

  /**
   * Creates a standard multi-threaded calculation task runner capable of performing calculations.
   * <p>
   * This factory creates an executor basing the number of threads on the number of available processors.
   * It is recommended to use try-with-resources to manage the runner:
   * <pre>
   *  try (CalculationTaskRunner runner = CalculationTaskRunner.ofMultiThreaded()) {
   *    // use the runner
   *  }
   * </pre>
   * 
   * @return the calculation task runner
   */
  public static CalculationTaskRunner ofMultiThreaded() {
    return DefaultCalculationTaskRunner.ofMultiThreaded();
  }

  /**
   * Creates a calculation task runner capable of performing calculations, specifying the executor.
   * <p>
   * It is the callers responsibility to manage the life-cycle of the executor.
   * 
   * @param executor  the executor to use
   * @return the calculation task runner
   */
  public static CalculationTaskRunner of(ExecutorService executor) {
    return DefaultCalculationTaskRunner.of(executor);
  }

  //-------------------------------------------------------------------------
  /**
   * Performs calculations for a single set of market data.
   * <p>
   * This returns a grid of results based on the specified tasks and market data.
   * The grid will contain a row for each target and a column for each measure.
   * 
   * @param tasks  the calculation tasks to invoke
   * @param marketData  the market data to be used in the calculations
   * @param refData  the reference data to be used in the calculations
   * @return the grid of calculation results, based on the tasks and market data
   */
  public abstract Results calculate(
      CalculationTasks tasks,
      MarketData marketData,
      ReferenceData refData);

  /**
   * Performs calculations asynchronously for a single set of market data,
   * invoking a listener as each calculation completes.
   * <p>
   * This method requires the listener to assemble the results, but it can be much more memory efficient when
   * calculating aggregate results. If the individual results are discarded after they are incorporated into
   * the aggregate they can be garbage collected.
   * 
   * @param tasks  the calculation tasks to invoke
   * @param marketData  the market data to be used in the calculations
   * @param refData  the reference data to be used in the calculations
   * @param listener  listener that is invoked when individual results are calculated
   */
  public abstract void calculateAsync(
      CalculationTasks tasks,
      MarketData marketData,
      ReferenceData refData,
      CalculationListener listener);

  //-------------------------------------------------------------------------
  /**
   * Performs calculations for multiple scenarios, each with a different set of market data.
   * <p>
   * This returns a grid of results based on the specified tasks and market data.
   * The grid will contain a row for each target and a column for each measure.
   * Each cell will contain multiple results, one for each scenario.
   * 
   * @param tasks  the calculation tasks to invoke
   * @param marketData  the market data to be used in the calculations
   * @param refData  the reference data to be used in the calculations
   * @return the grid of calculation results, based on the tasks and market data
   */
  public abstract Results calculateMultiScenario(
      CalculationTasks tasks,
      ScenarioMarketData marketData,
      ReferenceData refData);

  /**
   * Performs calculations asynchronously for a multiple scenarios, each with a different set of market data,
   * invoking a listener as each calculation completes.
   * <p>
   * This method requires the listener to assemble the results, but it can be much more memory efficient when
   * calculating aggregate results. If the individual results are discarded after they are incorporated into
   * the aggregate they can be garbage collected.
   * 
   * @param tasks  the calculation tasks to invoke
   * @param marketData  the market data to be used in the calculations
   * @param refData  the reference data to be used in the calculations
   * @param listener  listener that is invoked when individual results are calculated
   */
  public abstract void calculateMultiScenarioAsync(
      CalculationTasks tasks,
      ScenarioMarketData marketData,
      ReferenceData refData,
      CalculationListener listener);

  //-------------------------------------------------------------------------
  /**
   * Closes any resources held by the component.
   * <p>
   * If the component holds an {@link ExecutorService}, this method will typically
   * call {@link ExecutorService#shutdown()}.
   */
  @Override
  public abstract void close();

}
