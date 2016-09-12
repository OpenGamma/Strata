/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc;

import java.util.List;
import java.util.concurrent.ExecutorService;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.calc.runner.CalculationListener;
import com.opengamma.strata.calc.runner.CalculationTaskRunner;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

/**
 * Component that provides the ability to perform calculations on multiple targets, measures and scenarios.
 * <p>
 * The strata-pricer module provides the ability to calculate results for a single trade,
 * single measure and single set of market data. {@code CalculationRunner} provides the ability
 * to calculate results for many trades, many measures and many sets of market data.
 * <p>
 * Once obtained, the {@code CalculationRunner} instance may be used to calculate results.
 * The four "calculate" methods handle the combination of single versus scenario market data,
 * and synchronous versus asynchronous.
 * <p>
 * A calculation runner is typically obtained using the static methods on this interface.
 * The instance contains an executor thread-pool, thus care should be taken to ensure
 * the thread-pool is correctly managed. For example, try-with-resources could be used:
 * <pre>
 *  try (CalculationRunner runner = CalculationRunner.ofMultiThreaded()) {
 *    // use the runner
 *  }
 * </pre>
 */
public interface CalculationRunner extends AutoCloseable {

  /**
   * Creates a standard multi-threaded calculation runner capable of performing calculations.
   * <p>
   * This factory creates an executor basing the number of threads on the number of available processors.
   * It is recommended to use try-with-resources to manage the runner:
   * <pre>
   *  try (CalculationRunner runner = CalculationRunner.ofMultiThreaded()) {
   *    // use the runner
   *  }
   * </pre>
   * 
   * @return the calculation runner
   */
  public static CalculationRunner ofMultiThreaded() {
    return DefaultCalculationRunner.ofMultiThreaded();
  }

  /**
   * Creates a calculation runner capable of performing calculations, specifying the executor.
   * <p>
   * It is the callers responsibility to manage the life-cycle of the executor.
   * 
   * @param executor  the executor to use
   * @return the calculation runner
   */
  public static CalculationRunner of(ExecutorService executor) {
    return DefaultCalculationRunner.of(executor);
  }

  //-------------------------------------------------------------------------
  /**
   * Performs calculations for a single set of market data.
   * <p>
   * This returns a grid of results based on the specified targets, columns, rules and market data.
   * The grid will contain a row for each target and a column for each measure.
   * 
   * @param calculationRules  the rules defining how the calculation is performed
   * @param targets  the targets for which values of the measures will be calculated
   * @param columns  the configuration for the columns that will be calculated,
   *   including the measure and any column-specific overrides
   * @param marketData  the market data to be used in the calculations
   * @param refData  the reference data to be used in the calculations
   * @return the grid of calculation results, based on the targets and columns
   */
  public abstract Results calculate(
      CalculationRules calculationRules,
      List<? extends CalculationTarget> targets,
      List<Column> columns,
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
   * @param calculationRules  the rules defining how the calculation is performed
   * @param targets  the targets for which values of the measures will be calculated
   * @param columns  the configuration for the columns that will be calculated,
   *   including the measure and any column-specific overrides
   * @param marketData  the market data to be used in the calculations
   * @param refData  the reference data to be used in the calculations
   * @param listener  listener that is invoked when individual results are calculated
   */
  public abstract void calculateAsync(
      CalculationRules calculationRules,
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      MarketData marketData,
      ReferenceData refData,
      CalculationListener listener);

  //-------------------------------------------------------------------------
  /**
   * Performs calculations for multiple scenarios, each with a different set of market data.
   * <p>
   * This returns a grid of results based on the specified targets, columns, rules and market data.
   * The grid will contain a row for each target and a column for each measure.
   * 
   * @param calculationRules  the rules defining how the calculation is performed
   * @param targets  the targets for which values of the measures will be calculated
   * @param columns  the configuration for the columns that will be calculated,
   *   including the measure and any column-specific overrides
   * @param marketData  the market data to be used in the calculations
   * @param refData  the reference data to be used in the calculations
   * @return the grid of calculation results, based on the targets and columns
   */
  public abstract Results calculateMultiScenario(
      CalculationRules calculationRules,
      List<? extends CalculationTarget> targets,
      List<Column> columns,
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
   * @param calculationRules  the rules defining how the calculation is performed
   * @param targets  the targets for which values of the measures will be calculated
   * @param columns  the configuration for the columns that will be calculated,
   *   including the measure and any column-specific overrides
   * @param marketData  the market data to be used in the calculations
   * @param refData  the reference data to be used in the calculations
   * @param listener  listener that is invoked when individual results are calculated
   */
  public abstract void calculateMultiScenarioAsync(
      CalculationRules calculationRules,
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      ScenarioMarketData marketData,
      ReferenceData refData,
      CalculationListener listener);

  //-------------------------------------------------------------------------
  /**
   * Gets the underlying task runner.
   * <p>
   * In most cases, this runner will be implemented using an instance of {@link CalculationTaskRunner}.
   * That interface provides a lower-level API, with the ability optimize if similar calculations
   * are being made repeatedly.
   * 
   * @return the underlying task runner
   * @throws UnsupportedOperationException if access to the task runner is not provided
   */
  public abstract CalculationTaskRunner getTaskRunner();

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
