/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.Guavate.toImmutableList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.result.Result;

/**
 * The default calculation task runner.
 * <p>
 * This uses a single instance of {@link ExecutorService}.
 */
class DefaultCalculationTaskRunner implements CalculationTaskRunner {

  /**
   * Executes the tasks that perform the individual calculations.
   * This will typically be multi-threaded, but single or direct executors also work.
   */
  private final ExecutorService executor;

  //-------------------------------------------------------------------------
  /**
   * Creates a standard multi-threaded calculation task runner capable of performing calculations.
   * <p>
   * This factory creates an executor basing the number of threads on the number of available processors.
   * It is recommended to use try-with-resources to manage the runner:
   * <pre>
   *  try (DefaultCalculationTaskRunner runner = DefaultCalculationTaskRunner.ofMultiThreaded()) {
   *    // use the runner
   *  }
   * </pre>
   * 
   * @return the calculation task runner
   */
  static DefaultCalculationTaskRunner ofMultiThreaded() {
    return new DefaultCalculationTaskRunner(createExecutor(Runtime.getRuntime().availableProcessors()));
  }

  /**
   * Creates a calculation task runner capable of performing calculations, specifying the executor.
   * <p>
   * It is the callers responsibility to manage the life-cycle of the executor.
   * 
   * @param executor  the executor to use
   * @return the calculation task runner
   */
  static DefaultCalculationTaskRunner of(ExecutorService executor) {
    return new DefaultCalculationTaskRunner(executor);
  }

  // create an executor with daemon threads
  private static ExecutorService createExecutor(int threads) {
    int effectiveThreads = (threads <= 0 ? Runtime.getRuntime().availableProcessors() : threads);
    ThreadFactory threadFactory = r -> {
      Thread t = Executors.defaultThreadFactory().newThread(r);
      t.setName("CalculationTaskRunner-" + t.getName());
      t.setDaemon(true);
      return t;
    };
    return Executors.newFixedThreadPool(effectiveThreads, threadFactory);
  }

  //-------------------------------------------------------------------------
  /**
   * Creates an instance specifying the executor to use.
   * 
   * @param executor  the executor that is used to perform the calculations
   */
  private DefaultCalculationTaskRunner(ExecutorService executor) {
    this.executor = ArgChecker.notNull(executor, "executor");
  }

  //-------------------------------------------------------------------------
  @Override
  public Results calculateSingleScenario(
      CalculationTasks tasks,
      CalculationEnvironment marketData,
      ReferenceData refData) {

    // perform the calculations
    Results results = calculateMultipleScenarios(tasks, marketData, refData);

    // unwrap the results
    // since there is only one scenario it is not desirable to return scenario result containers
    List<Result<?>> unwrappedResults = results.getItems().stream()
        .map(DefaultCalculationTaskRunner::unwrapScenarioResult)
        .collect(toImmutableList());

    return results.toBuilder().items(unwrappedResults).build();
  }

  /**
   * Unwraps the result from an instance of {@link ScenarioResult} containing a single result.
   * <p>
   * When the user executes a single scenario the functions are invoked with a set of scenario market data
   * of size 1. This means the functions are simpler and always deal with scenarios. But if the user has
   * asked for a single set of results they don't want to see a collection of size 1 so the scenario results
   * need to be unwrapped.
   * <p>
   * If {@code result} is a failure or doesn't contain a {@code ScenarioResult} it is returned.
   * <p>
   * If this method is called with a {@code ScenarioResult} containing more than one value it throws an exception.
   */
  private static Result<?> unwrapScenarioResult(Result<?> result) {
    if (result.isFailure()) {
      return result;
    }
    Object value = result.getValue();
    if (!(value instanceof ScenarioResult)) {
      return result;
    }
    ScenarioResult<?> scenarioResult = (ScenarioResult<?>) value;

    if (scenarioResult.size() != 1) {
      throw new IllegalArgumentException(Messages.format(
          "Expected one result but found {} in {}", scenarioResult.size(), scenarioResult));
    }
    return Result.success(scenarioResult.get(0));
  }

  @Override
  public Results calculateMultipleScenarios(
      CalculationTasks tasks,
      CalculationEnvironment marketData,
      ReferenceData refData) {

    AggregatingListener listener = new AggregatingListener(tasks.getColumns());
    calculateMultipleScenariosAsync(tasks, marketData, refData, listener);
    return listener.result();
  }

  @Override
  public void calculateSingleScenarioAsync(
      CalculationTasks tasks,
      CalculationEnvironment marketData,
      ReferenceData refData,
      CalculationListener listener) {

    // the listener is decorated to unwrap ScenarioResults containing a single result
    UnwrappingListener unwrappingListener = new UnwrappingListener(listener);
    calculateMultipleScenariosAsync(tasks, marketData, refData, unwrappingListener);
  }

  @Override
  public void calculateMultipleScenariosAsync(
      CalculationTasks tasks,
      CalculationEnvironment marketData,
      ReferenceData refData,
      CalculationListener listener) {

    List<CalculationTask> taskList = tasks.getTasks();
    // the listener is invoked via this wrapper
    // the wrapper ensures thread-safety for the listener
    // it also calls the listener with single CalculationResult cells, not CalculationResults
    Consumer<CalculationResults> consumer = new ListenerWrapper(listener, taskList.size());
    // run each task using the executor
    taskList.stream().forEach(task -> runTask(task, marketData, refData, consumer));
  }

  // submits a task to the executor to be run
  private void runTask(
      CalculationTask task,
      CalculationEnvironment marketData,
      ReferenceData refData,
      Consumer<CalculationResults> consumer) {

    // the task is executed, with the result passed to the consumer
    // the consumer wraps the listener to ensure thread-safety
    Supplier<CalculationResults> taskExecutor = () -> task.execute(marketData, refData);
    CompletableFuture.supplyAsync(taskExecutor, executor).thenAccept(consumer);
  }

  //-------------------------------------------------------------------------
  @Override
  public void close() {
    executor.shutdown();
  }

  //-------------------------------------------------------------------------
  /**
   * Calculation listener that receives the results of individual calculations
   * and builds a set of {@link Results}. This is used by the non-async methods.
   */
  private static final class AggregatingListener extends AggregatingCalculationListener<Results> {

    /** Comparator for sorting the results by row and then column. */
    private static final Comparator<CalculationResult> COMPARATOR =
        Comparator.comparingInt(CalculationResult::getRowIndex)
            .thenComparingInt(CalculationResult::getColumnIndex);

    /** List that is populated with the results as they arrive. */
    private final List<CalculationResult> results = new ArrayList<>();

    /** The columns that define what values are calculated. */
    private final List<Column> columns;

    private AggregatingListener(List<Column> columns) {
      this.columns = columns;
    }

    @Override
    public void resultReceived(CalculationTarget target, CalculationResult result) {
      results.add(result);
    }

    @Override
    protected Results createAggregateResult() {
      results.sort(COMPARATOR);
      return buildResults(results, columns);
    }

    /**
     * Builds a set of results from the results of the individual calculations.
     *
     * @param calculationResults  the results of the individual calculations
     * @param columns  the columns that define what values are calculated
     * @return the results
     */
    private static Results buildResults(List<CalculationResult> calculationResults, List<Column> columns) {
      List<Result<?>> results =
          calculationResults.stream()
              .map(CalculationResult::getResult)
              .collect(toImmutableList());

      int columnCount = columns.size();
      int rowCount = (columnCount == 0) ? 0 : calculationResults.size() / columnCount;
      return Results.of(rowCount, columnCount, results);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Listener that decorates another listener and unwraps {@link ScenarioResult} instances
   * containing a single value before passing the value to the delegate listener.
   * This is used by the single scenario async method.
   */
  private static final class UnwrappingListener implements CalculationListener {

    private final CalculationListener delegate;

    private UnwrappingListener(CalculationListener delegate) {
      this.delegate = delegate;
    }

    @Override
    public void resultReceived(CalculationTarget target, CalculationResult calculationResult) {
      Result<?> result = calculationResult.getResult();
      Result<?> unwrappedResult = unwrapScenarioResult(result);
      CalculationResult unwrappedCalculationResult = calculationResult.withResult(unwrappedResult);
      delegate.resultReceived(target, unwrappedCalculationResult);
    }

    @Override
    public void calculationsComplete() {
      delegate.calculationsComplete();
    }
  }

}
