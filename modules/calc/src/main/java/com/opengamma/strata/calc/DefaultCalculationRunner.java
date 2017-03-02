/*
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
import com.opengamma.strata.calc.runner.CalculationTasks;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

/**
 * The default calculation runner.
 * <p>
 * This delegates to an instance of {@link CalculationTaskRunner}.
 */
class DefaultCalculationRunner implements CalculationRunner {

  /**
   * The underlying task runner.
   */
  private final CalculationTaskRunner taskRunner;

  //-------------------------------------------------------------------------
  /**
   * Creates a standard multi-threaded calculation runner capable of performing calculations.
   * <p>
   * This factory creates an executor basing the number of threads on the number of available processors.
   * It is recommended to use try-with-resources to manage the runner:
   * <pre>
   *  try (DefaultCalculationRunner runner = DefaultCalculationRunner.ofMultiThreaded()) {
   *    // use the runner
   *  }
   * </pre>
   * 
   * @return the calculation runner
   */
  static DefaultCalculationRunner ofMultiThreaded() {
    return new DefaultCalculationRunner(CalculationTaskRunner.ofMultiThreaded());
  }

  /**
   * Creates a calculation runner capable of performing calculations, specifying the executor.
   * <p>
   * It is the callers responsibility to manage the life-cycle of the executor.
   * 
   * @param executor  the executor to use
   * @return the calculation runner
   */
  static DefaultCalculationRunner of(ExecutorService executor) {
    return new DefaultCalculationRunner(CalculationTaskRunner.of(executor));
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance specifying the underlying task runner to use.
   * 
   * @param taskRunner  the underlying task runner
   */
  DefaultCalculationRunner(CalculationTaskRunner taskRunner) {
    this.taskRunner = ArgChecker.notNull(taskRunner, "taskRunner");
  }

  //-------------------------------------------------------------------------
  @Override
  public Results calculate(
      CalculationRules calculationRules,
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      MarketData marketData,
      ReferenceData refData) {

    CalculationTasks tasks = CalculationTasks.of(calculationRules, targets, columns);
    return taskRunner.calculate(tasks, marketData, refData);
  }

  @Override
  public void calculateAsync(
      CalculationRules calculationRules,
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      MarketData marketData,
      ReferenceData refData,
      CalculationListener listener) {

    CalculationTasks tasks = CalculationTasks.of(calculationRules, targets, columns);
    taskRunner.calculateAsync(tasks, marketData, refData, listener);
  }

  //-------------------------------------------------------------------------
  @Override
  public Results calculateMultiScenario(
      CalculationRules calculationRules,
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      ScenarioMarketData marketData,
      ReferenceData refData) {

    CalculationTasks tasks = CalculationTasks.of(calculationRules, targets, columns);
    return taskRunner.calculateMultiScenario(tasks, marketData, refData);
  }

  @Override
  public void calculateMultiScenarioAsync(
      CalculationRules calculationRules,
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      ScenarioMarketData marketData,
      ReferenceData refData,
      CalculationListener listener) {

    CalculationTasks tasks = CalculationTasks.of(calculationRules, targets, columns);
    taskRunner.calculateMultiScenarioAsync(tasks, marketData, refData, listener);
  }

  //-------------------------------------------------------------------------
  @Override
  public CalculationTaskRunner getTaskRunner() {
    return taskRunner;
  }

  @Override
  public void close() {
    taskRunner.close();
  }

}
