/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.marketdata.MarketDataFactory;
import com.opengamma.strata.calc.marketdata.config.MarketDataConfig;
import com.opengamma.strata.calc.marketdata.scenario.ScenarioDefinition;
import com.opengamma.strata.collect.ArgChecker;

/**
 * The default calculation runner factory.
 */
class DefaultCalculationRunnerFactory implements CalculationRunnerFactory {

  /**
   * Executes the tasks that perform the individual calculations.
   * This will typically be multi-threaded, but single or direct executors also work.
   */
  private final ExecutorService executor;

  //-------------------------------------------------------------------------
  /**
   * Creates an instance using a single thread.
   * 
   * @return the factory
   */
  static DefaultCalculationRunnerFactory ofSingleThreaded() {
    return new DefaultCalculationRunnerFactory(createExecutor(1));
  }

  /**
   * Creates an instance basing the number of threads on the number of available processors.
   * 
   * @return the factory
   */
  static DefaultCalculationRunnerFactory ofMultiThreaded() {
    return new DefaultCalculationRunnerFactory(createExecutor(Runtime.getRuntime().availableProcessors()));
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance specifying the executor to use.
   * 
   * @param executor  executes the tasks that perform the calculations
   */
  DefaultCalculationRunnerFactory(ExecutorService executor) {
    this.executor = ArgChecker.notNull(executor, "executor");
  }

  // create an executor with daemon threads
  static ExecutorService createExecutor(int threads) {
    int effectiveThreads = (threads <= 0 ? Runtime.getRuntime().availableProcessors() : threads);
    ThreadFactory threadFactory = r -> {
      Thread t = Executors.defaultThreadFactory().newThread(r);
      t.setName("CalculationRunner-" + t.getName());
      t.setDaemon(true);
      return t;
    };
    return Executors.newFixedThreadPool(effectiveThreads, threadFactory);
  }

  //-------------------------------------------------------------------------
  @Override
  public CalculationRunner create(
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      CalculationRules calculationRules) {

    CalculationTasks tasks = CalculationTasks.of(targets, columns, calculationRules);
    return new DefaultCalculationRunner(executor, tasks);
  }

  @Override
  public CalculationRunner createWithMarketDataBuilder(
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      CalculationRules calculationRules,
      MarketDataFactory marketDataFactory,
      MarketDataConfig marketDataConfig) {

    CalculationRunner runner = create(targets, columns, calculationRules);
    return new MarketDataCalculationRunner(runner, marketDataFactory, marketDataConfig, ScenarioDefinition.empty());
  }

  @Override
  public CalculationRunner createWithMarketDataBuilder(
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      CalculationRules calculationRules,
      MarketDataFactory marketDataFactory,
      MarketDataConfig marketDataConfig,
      ScenarioDefinition scenarioDefinition) {

    CalculationRunner runner = create(targets, columns, calculationRules);
    return new MarketDataCalculationRunner(runner, marketDataFactory, marketDataConfig, scenarioDefinition);
  }

}
