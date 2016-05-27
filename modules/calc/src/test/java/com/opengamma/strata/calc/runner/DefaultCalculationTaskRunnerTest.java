/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.calc.ReportingCurrency.NATURAL;
import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.date;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.basics.market.TestObservableId;
import com.opengamma.strata.calc.ScenarioMarketData;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.ImmutableScenarioMarketData;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.Measures;
import com.opengamma.strata.calc.Results;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.marketdata.TestId;
import com.opengamma.strata.calc.result.ScenarioResult;
import com.opengamma.strata.calc.runner.CalculationTaskTest.TestTarget;
import com.opengamma.strata.collect.result.Result;

/**
 * Test {@link CalculationTaskRunner} and {@link DefaultCalculationTaskRunner}.
 */
@Test
public class DefaultCalculationTaskRunnerTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final TestTarget TARGET = new TestTarget();
  private static final LocalDate VAL_DATE = date(2011, 3, 8);
  private static final Set<Measure> MEASURES = ImmutableSet.of(Measures.PRESENT_VALUE);

  //-------------------------------------------------------------------------
  /**
   * Test that ScenarioResults containing a single value are unwrapped.
   */
  public void unwrapScenarioResults() {
    ScenarioResult<String> scenarioResult = ScenarioResult.of("foo");
    ScenarioResultFunction fn = new ScenarioResultFunction(Measures.PRESENT_VALUE, scenarioResult);
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, Measures.PRESENT_VALUE, NATURAL);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    Column column = Column.of(Measures.PRESENT_VALUE);
    CalculationTasks tasks = CalculationTasks.of(ImmutableList.of(task), ImmutableList.of(column));

    // using the direct executor means there is no need to close/shutdown the runner
    CalculationTaskRunner test = CalculationTaskRunner.of(MoreExecutors.newDirectExecutorService());

    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(VAL_DATE).build();
    Results results1 = test.calculateSingleScenario(tasks, marketData, REF_DATA);
    Result<?> result1 = results1.get(0, 0);
    // Check the result contains the string directly, not the result wrapping the string
    assertThat(result1).hasValue("foo");

    Results results2 = test.calculateMultipleScenarios(tasks, marketData, REF_DATA);
    Result<?> result2 = results2.get(0, 0);
    // Check the result contains the scenario result wrapping the string
    assertThat(result2).hasValue(scenarioResult);
  }

  /**
   * Test that ScenarioResults containing multiple values are an error.
   */
  public void unwrapMultipleScenarioResults() {
    ScenarioResult<String> scenarioResult = ScenarioResult.of("foo", "bar");
    ScenarioResultFunction fn = new ScenarioResultFunction(Measures.PAR_RATE, scenarioResult);
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, Measures.PAR_RATE, NATURAL);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    Column column = Column.of(Measures.PAR_RATE);
    CalculationTasks tasks = CalculationTasks.of(ImmutableList.of(task), ImmutableList.of(column));

    // using the direct executor means there is no need to close/shutdown the runner
    CalculationTaskRunner test = CalculationTaskRunner.of(MoreExecutors.newDirectExecutorService());

    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(VAL_DATE).build();
    assertThrowsIllegalArg(() -> test.calculateSingleScenario(tasks, marketData, REF_DATA));
  }

  /**
   * Test that ScenarioResults containing a single value are unwrapped when calling calculateAsync().
   */
  public void unwrapScenarioResultsAsync() {
    ScenarioResult<String> scenarioResult = ScenarioResult.of("foo");
    ScenarioResultFunction fn = new ScenarioResultFunction(Measures.PRESENT_VALUE, scenarioResult);
    CalculationTaskCell cell = CalculationTaskCell.of(0, 0, Measures.PRESENT_VALUE, NATURAL);
    CalculationTask task = CalculationTask.of(TARGET, fn, cell);
    Column column = Column.of(Measures.PRESENT_VALUE);
    CalculationTasks tasks = CalculationTasks.of(ImmutableList.of(task), ImmutableList.of(column));

    // using the direct executor means there is no need to close/shutdown the runner
    CalculationTaskRunner test = CalculationTaskRunner.of(MoreExecutors.newDirectExecutorService());
    Listener listener = new Listener();

    ScenarioMarketData marketData = ImmutableScenarioMarketData.builder(VAL_DATE).build();
    test.calculateSingleScenarioAsync(tasks, marketData, REF_DATA, listener);
    CalculationResult calculationResult1 = listener.result;
    Result<?> result1 = calculationResult1.getResult();
    // Check the result contains the string directly, not the result wrapping the string
    assertThat(result1).hasValue("foo");

    test.calculateMultipleScenariosAsync(tasks, marketData, REF_DATA, listener);
    CalculationResult calculationResult2 = listener.result;
    Result<?> result2 = calculationResult2.getResult();
    // Check the result contains the scenario result wrapping the string
    assertThat(result2).hasValue(scenarioResult);
  }

  //-------------------------------------------------------------------------
  public static final class TestFunction implements CalculationFunction<TestTarget> {

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

      return FunctionRequirements.builder()
          .singleValueRequirements(
              ImmutableSet.of(
                  TestId.of("1"),
                  TestObservableId.of("2")))
          .timeSeriesRequirements(TestObservableId.of("3"))
          .build();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTarget target,
        Set<Measure> measures,
        CalculationParameters parameters,
        ScenarioMarketData marketData,
        ReferenceData refData) {

      ScenarioResult<String> array = ScenarioResult.of("bar");
      return ImmutableMap.of(Measures.PRESENT_VALUE, Result.success(array));
    }
  }

  //-------------------------------------------------------------------------
  private static final class ScenarioResultFunction implements CalculationFunction<TestTarget> {

    private final Measure measure;
    private final ScenarioResult<String> result;

    private ScenarioResultFunction(Measure measure, ScenarioResult<String> result) {
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
}
