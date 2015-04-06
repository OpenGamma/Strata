/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.config.CalculationTaskConfig;
import com.opengamma.strata.engine.config.CalculationTasksConfig;
import com.opengamma.strata.engine.config.EngineFunctionConfig;
import com.opengamma.strata.engine.config.MarketDataRules;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.config.PricingRules;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.marketdata.BaseMarketData;
import com.opengamma.strata.engine.marketdata.ScenarioMarketData;
import com.opengamma.strata.engine.marketdata.SingleScenarioMarketData;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;

/**
 * The default calculation engine implementation.
 */
public class DefaultCalculationEngine implements CalculationEngine {

  /** Executes the tasks that perform the individual calculations. */
  private final ListeningExecutorService executor;

  /**
   * @param executor  executes the tasks that perform the calculations
   */
  public DefaultCalculationEngine(ExecutorService executor) {
    this.executor = MoreExecutors.listeningDecorator(executor);
  }

  @Override
  public CalculationTasksConfig createCalculationConfig(
      List<? extends CalculationTarget> targets,
      List<Column> columns,
      PricingRules pricingRules,
      MarketDataRules marketDataRules,
      ReportingRules reportingRules) {

    // Create columns with rules that are a combination of the column overrides and the defaults
    List<Column> effectiveColumns =
        columns.stream()
            .map(column -> column.withDefaultRules(pricingRules, marketDataRules, reportingRules))
            .collect(toImmutableList());

    List<CalculationTaskConfig> config =
        targets.stream()
            .flatMap(target -> targetTaskConfigs(target, effectiveColumns))
            .collect(toImmutableList());

    List<Measure> measures =
        columns.stream()
            .map(Column::getMeasure)
            .collect(toImmutableList());

    return CalculationTasksConfig.builder()
        .measures(measures)
        .taskConfigurations(config)
        .build();
  }

  /**
   * Returns a stream of configuration objects for the calculations for a single target.
   *
   * @param target  the target
   * @param columns  the columns defining the values that should be calculated
   * @return a stream of configuration objects for the calculations for the target
   */
  private Stream<CalculationTaskConfig> targetTaskConfigs(CalculationTarget target, List<Column> columns) {
    return columns.stream().map(column -> createTaskConfig(target, column));
  }

  @Override
  public CalculationTasks createCalculationTasks(CalculationTasksConfig config) {
    List<CalculationTask> tasks = config.getTaskConfigurations().stream().map(this::createTask).collect(toImmutableList());
    return new CalculationTasks(tasks, config.getMeasures());
  }

  @Override
  public Results calculate(CalculationTasks calculationTasks, BaseMarketData marketData) {
    return calculate(calculationTasks, new SingleScenarioMarketData(marketData));
  }

  @Override
  public Results calculate(CalculationTasks calculationTasks, ScenarioMarketData marketData) {
    List<CalculationTask> taskList = calculationTasks.getTasks();
    List<ListenableFuture<? extends Result<?>>> futures =
        taskList.stream()
            .map(calc -> executor.submit(() -> calc.execute(marketData)))
            .collect(toImmutableList());
    ListenableFuture<List<Result<?>>> combinedFuture = Futures.allAsList(futures);
    Function<List<Result<?>>, Results> fn = results -> buildResults(results, calculationTasks);
    ListenableFuture<Results> resultsFuture = Futures.transform(combinedFuture, fn);
    try {
      return resultsFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      // If there's an exception here it means there's a bug in the engine. Any exceptions thrown by calculations
      // should be caught in the tasks and converted to failure results.
      throw new RuntimeException("Failed to get results", e);
    }
  }

  private static Results buildResults(List<Result<?>> values, CalculationTasks calculationTasks) {
    List<Measure> measures = calculationTasks.getMeasures();
    Map<Measure, Integer> columnIndices =
        IntStream.range(0, measures.size())
            .mapToObj(Integer::valueOf)
            .collect(toMap(measures::get, i -> i));
    return Results.builder().columnIndices(columnIndices).values(values).build();
  }

  /**
   * Creates configuration for calculating the value of a single measure for a target.
   *
   * @param target  the target for which the measure will be calculated
   * @param column  the column for which the value is calculated
   * @return configuration for calculating the measure for the target
   */
  private static CalculationTaskConfig createTaskConfig(CalculationTarget target, Column column) {
    EngineFunctionConfig functionConfig =
        column.getPricingRules().functionConfig(target, column.getMeasure())
            .orElse(EngineFunctionConfig.DEFAULT);

    MarketDataMappings marketDataMappings =
        column.getMarketDataRules().mappings(target)
            .orElse(MarketDataMappings.empty());

    return CalculationTaskConfig.of(target, functionConfig, marketDataMappings, column.getReportingRules());
  }

  /**
   * Creates a task for performing a single calculation.
   *
   * @param config  configuration for the task
   * @return a task for performing a single calculation
   */
  private CalculationTask createTask(CalculationTaskConfig config) {
    return new CalculationTask(
        config.getTarget(),
        config.getEngineFunctionConfig().createFunction(),
        config.getMarketDataMappings(),
        config.getReportingRules());
  }
}
