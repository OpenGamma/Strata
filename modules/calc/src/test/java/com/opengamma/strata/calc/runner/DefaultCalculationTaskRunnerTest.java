/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.calc.ReportingCurrency.NATURAL;
import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.calc.TestingMeasures;
import com.opengamma.strata.calc.runner.CalculationTaskTest.TestTarget;
import com.opengamma.strata.collect.result.FailureReason;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.scenario.ScenarioArray;
import com.opengamma.strata.data.scenario.ScenarioMarketData;

/**
 * Test {@link CalculationTaskRunner} and {@link DefaultCalculationTaskRunner}.
 */
public class DefaultCalculationTaskRunnerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final TestTarget TARGET = new TestTarget();
  private static final LocalDate VAL_DATE = date(2011, 3, 8);
  private static final Set<Measure> MEASURES = ImmutableSet.of(TestingMeasures.PRESENT_VALUE);

  //-------------------------------------------------------------------------
  // Test that ScenarioArrays containing a single value are unwrapped.
  @Test
  public void unwrapScenarioResults() throws Exception {
    ScenarioArray<String> scenarioResult = ScenarioArray.of("foo");
    ScenarioResultFunction fn = new ScenarioResultFunction(TestingMeasures.PRESENT_VALUE, scenarioResult);
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, NATURAL);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    Column column = Column.of(TestingMeasures.PRESENT_VALUE);
    CalculationTasks tasks = CalculationTasks.of(ImmutableList.of(task), ImmutableList.of(column));

    // using the direct executor means there is no need to close/shutdown the runner
    CalculationTaskRunner test = CalculationTaskRunner.of(MoreExecutors.newDirectExecutorService());

    MarketData marketData = MarketData.empty(VAL_DATE);
    Results results1 = test.calculate(tasks, marketData, REF_DATA);
    Result<?> result1 = results1.get(0, 0);
    // Check the result contains the string directly, not the result wrapping the string
    assertThat(result1).hasValue("foo");

    Results results2 = test.calculateMultiScenario(tasks, ScenarioMarketData.of(1, marketData), REF_DATA);
    Result<?> result2 = results2.get(0, 0);
    // Check the result contains the scenario result wrapping the string
    assertThat(result2).hasValue(scenarioResult);

    ResultsListener resultsListener = new ResultsListener();
    test.calculateAsync(tasks, marketData, REF_DATA, resultsListener);
    CompletableFuture<Results> future = resultsListener.getFuture();
    // The future is guaranteed to be done because everything is running on a single thread
    assertThat(future.isDone()).isTrue();
    Results results3 = future.get();
    Result<?> result3 = results3.get(0, 0);
    // Check the result contains the string directly, not the result wrapping the string
    assertThat(result3).hasValue("foo");
  }

  /**
   * Test that ScenarioArrays containing multiple values are an error.
   */
  @Test
  public void unwrapMultipleScenarioResults() {
    ScenarioArray<String> scenarioResult = ScenarioArray.of("foo", "bar");
    ScenarioResultFunction fn = new ScenarioResultFunction(TestingMeasures.PAR_RATE, scenarioResult);
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PAR_RATE, NATURAL);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    Column column = Column.of(TestingMeasures.PAR_RATE);
    CalculationTasks tasks = CalculationTasks.of(ImmutableList.of(task), ImmutableList.of(column));

    // using the direct executor means there is no need to close/shutdown the runner
    CalculationTaskRunner test = CalculationTaskRunner.of(MoreExecutors.newDirectExecutorService());

    MarketData marketData = MarketData.empty(VAL_DATE);
    assertThatIllegalArgumentException().isThrownBy(() -> test.calculate(tasks, marketData, REF_DATA));
  }

  /**
   * Test that ScenarioArrays containing a single value are unwrapped when calling calculateAsync().
   */
  @Test
  public void unwrapScenarioResultsAsync() {
    ScenarioArray<String> scenarioResult = ScenarioArray.of("foo");
    ScenarioResultFunction fn = new ScenarioResultFunction(TestingMeasures.PRESENT_VALUE, scenarioResult);
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, NATURAL);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    Column column = Column.of(TestingMeasures.PRESENT_VALUE);
    CalculationTasks tasks = CalculationTasks.of(ImmutableList.of(task), ImmutableList.of(column));

    // using the direct executor means there is no need to close/shutdown the runner
    CalculationTaskRunner test = CalculationTaskRunner.of(MoreExecutors.newDirectExecutorService());
    Listener listener = new Listener();

    MarketData marketData = MarketData.empty(VAL_DATE);
    test.calculateAsync(tasks, marketData, REF_DATA, listener);
    CalculationResult calculationResult1 = listener.result;
    Result<?> result1 = calculationResult1.getResult();
    // Check the result contains the string directly, not the result wrapping the string
    assertThat(result1).hasValue("foo");

    test.calculateMultiScenarioAsync(tasks, ScenarioMarketData.of(1, marketData), REF_DATA, listener);
    CalculationResult calculationResult2 = listener.result;
    Result<?> result2 = calculationResult2.getResult();
    // Check the result contains the scenario result wrapping the string
    assertThat(result2).hasValue(scenarioResult);
  }

  //-------------------------------------------------------------------------
  /**
   * Tests that running an empty list of tasks completes and returns a set of results with zero rows.
   */
  @Test
  public void runWithNoTasks() {
    Column column = Column.of(TestingMeasures.PRESENT_VALUE);
    CalculationTasks tasks = CalculationTasks.of(ImmutableList.of(), ImmutableList.of(column));

    // using the direct executor means there is no need to close/shutdown the runner
    CalculationTaskRunner test = CalculationTaskRunner.of(MoreExecutors.newDirectExecutorService());

    MarketData marketData = MarketData.empty(VAL_DATE);
    Results results = test.calculate(tasks, marketData, REF_DATA);
    assertThat(results.getRowCount()).isEqualTo(0);
    assertThat(results.getColumnCount()).isEqualTo(1);
    assertThat(results.getColumns().get(0).getMeasure()).isEqualTo(TestingMeasures.PRESENT_VALUE);
  }

  //-------------------------------------------------------------------------
  private static final class ScenarioResultFunction implements CalculationFunction<TestTarget> {

    private final Measure measure;
    private final ScenarioArray<String> result;

    private ScenarioResultFunction(Measure measure, ScenarioArray<String> result) {
      this.measure = measure;
      this.result = result;
    }

    @Override
    public Class<TestTarget> targetType() {
      return TestTarget.class;
    }

    @Override
    public Set<Measure> supportedMeasures() {
      return ImmutableSet.of(measure);
    }

    @Override
    public Currency naturalCurrency(TestTarget trade, ReferenceData refData) {
      return USD;
    }

    @Override
    public FunctionRequirements requirements(
        TestTarget target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ReferenceData refData) {

      return FunctionRequirements.empty();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTarget target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ScenarioMarketData marketData,
        ReferenceData refData) {

      return ImmutableMap.of(measure, Result.success(result));
    }
  }

  //-------------------------------------------------------------------------
  private static final class Listener implements CalculationListener {

    private CalculationResult result;

    @Override
    public void resultReceived(CalculationTarget target, CalculationResult result) {
      this.result = result;
    }

    @Override
    public void calculationsComplete() {
      // Do nothing
    }
  }

  //-------------------------------------------------------------------------
  @Test
  @Timeout(5)
  public void interruptHangingCalculate() throws InterruptedException {
    HangingFunction fn = new HangingFunction();
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, NATURAL);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    Column column = Column.of(TestingMeasures.PRESENT_VALUE);
    CalculationTasks tasks = CalculationTasks.of(ImmutableList.of(task), ImmutableList.of(column));

    // using the direct executor means there is no need to close/shutdown the runner
    CalculationTaskRunner test = CalculationTaskRunner.of(MoreExecutors.newDirectExecutorService());
    MarketData marketData = MarketData.empty(VAL_DATE);

    AtomicBoolean shouldNeverThrow = new AtomicBoolean();
    AtomicBoolean interrupted = new AtomicBoolean();
    AtomicReference<Results> results = new AtomicReference<>();
    CountDownLatch latch = new CountDownLatch(1);
    Thread thread = new Thread(() -> {
      try {
        Results result = test.calculate(tasks, marketData, REF_DATA);  // when interrupted, should get a normal result
        interrupted.set(Thread.currentThread().isInterrupted());
        results.set(result);
      } catch (RuntimeException ex) {
        shouldNeverThrow.set(true);
      }
      latch.countDown();
    });
    // run the thread, wait until properly started, then interrupt, wait until properly handled
    thread.start();
    while (!fn.started) {
      // spin until started
    }
    thread.interrupt();
    latch.await();
    // asserts
    assertThat(interrupted.get()).isTrue();
    assertThat(shouldNeverThrow.get()).isFalse();
    Result<?> result00 = results.get().get(0, 0);
    assertThat(result00.isFailure()).isTrue();
    assertThat(result00.getFailure().getReason()).isEqualTo(FailureReason.CALCULATION_FAILED);
    assertThat(result00.getFailure().getMessage().contains("Runtime interrupted")).isTrue();
  }

  @Test
  @Timeout(5)
  public void interruptHangingResultsListener() throws InterruptedException {
    HangingFunction fn = new HangingFunction();
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, TestingMeasures.PRESENT_VALUE, NATURAL);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    Column column = Column.of(TestingMeasures.PRESENT_VALUE);
    CalculationTasks tasks = CalculationTasks.of(ImmutableList.of(task), ImmutableList.of(column));

    ExecutorService executor = Executors.newSingleThreadExecutor();
    try {
      CalculationTaskRunner test = CalculationTaskRunner.of(executor);
      MarketData marketData = MarketData.empty(VAL_DATE);

      AtomicBoolean shouldNeverComplete = new AtomicBoolean();
      AtomicBoolean interrupted = new AtomicBoolean();
      AtomicReference<RuntimeException> thrown = new AtomicReference<>();
      ResultsListener listener = new ResultsListener();
      test.calculateAsync(tasks, marketData, REF_DATA, listener);
      CountDownLatch latch = new CountDownLatch(1);
      Thread thread = new Thread(() -> {
        try {
          listener.result();  // test the interrupt behavior of this method
          shouldNeverComplete.set(true);
        } catch (RuntimeException ex) {
          interrupted.set(Thread.currentThread().isInterrupted());
          thrown.set(ex);
        }
        latch.countDown();
      });
      // run the thread, wait until properly started, then interrupt, wait until properly handled
      thread.start();
      while (!fn.started) {
        // spin until started
      }
      thread.interrupt();
      latch.await();
      // asserts
      assertThat(interrupted.get()).isTrue();
      assertThat(shouldNeverComplete.get()).isFalse();
      assertThat(thrown.get() instanceof RuntimeException).isTrue();
      assertThat(thrown.get().getCause() instanceof InterruptedException).isTrue();
    } finally {
      executor.shutdownNow();
    }
  }

  //-------------------------------------------------------------------------
  public static final class HangingFunction implements CalculationFunction<TestTarget> {

    private volatile boolean started;

    @Override
    public Class<TestTarget> targetType() {
      return TestTarget.class;
    }

    @Override
    public Set<Measure> supportedMeasures() {
      return MEASURES;
    }

    @Override
    public Currency naturalCurrency(TestTarget trade, ReferenceData refData) {
      return USD;
    }

    @Override
    public FunctionRequirements requirements(
        TestTarget target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ReferenceData refData) {

      return FunctionRequirements.empty();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTarget target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ScenarioMarketData marketData,
        ReferenceData refData) {

      while (true) {
        if (Thread.currentThread().isInterrupted()) {
          throw new RuntimeException("Runtime interrupted");
        }
        started = true;
      }
    }
  }

}
