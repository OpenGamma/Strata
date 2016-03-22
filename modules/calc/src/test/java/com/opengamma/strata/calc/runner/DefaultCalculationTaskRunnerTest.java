/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.calc.config.ReportingCurrency.NATURAL;
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
import com.opengamma.strata.basics.market.TestObservableKey;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.marketdata.MarketEnvironment;
import com.opengamma.strata.calc.marketdata.TestKey;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
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
   * Test that ScenarioResults containing a single value are unwrapped when calling calculate() with BaseMarketData.
   */
  public void unwrapScenarioResults() {
    ScenarioResult<String> scenarioResult = ScenarioResult.of("foo");
    ScenarioResultFunction fn = new ScenarioResultFunction(Measures.PRESENT_VALUE, scenarioResult);
    CalculationTask task = CalculationTask.of(TARGET, Measures.PRESENT_VALUE, 0, 0, fn, MarketDataMappings.empty(), NATURAL);
    Column column = Column.of(Measures.PRESENT_VALUE);
    CalculationTasks tasks = CalculationTasks.of(ImmutableList.of(task), ImmutableList.of(column));

    // using the direct executor means there is no need to close/shutdown the runner
    CalculationTaskRunner test = CalculationTaskRunner.of(MoreExecutors.newDirectExecutorService());

    CalculationEnvironment marketData = MarketEnvironment.empty(VAL_DATE);
    Results results1 = test.calculateSingleScenario(tasks, marketData, REF_DATA);
    Result<?> result1 = results1.get(0, 0);
    // Check the result contains the string directly, not the result wrapping the string
    assertThat(result1).hasValue("foo");

    CalculationEnvironment scenarioMarketData = MarketEnvironment.empty(VAL_DATE);
    Results results2 = test.calculateMultipleScenarios(tasks, scenarioMarketData, REF_DATA);
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
    CalculationTask task = CalculationTask.of(TARGET, Measures.PAR_RATE, 0, 0, fn, MarketDataMappings.empty(), NATURAL);
    Column column = Column.of(Measures.PAR_RATE);
    CalculationTasks tasks = CalculationTasks.of(ImmutableList.of(task), ImmutableList.of(column));

    // using the direct executor means there is no need to close/shutdown the runner
    CalculationTaskRunner test = CalculationTaskRunner.of(MoreExecutors.newDirectExecutorService());

    CalculationEnvironment marketData = MarketEnvironment.empty(VAL_DATE);
    assertThrowsIllegalArg(() -> test.calculateSingleScenario(tasks, marketData, REF_DATA));
  }

  /**
   * Test that ScenarioResults containing a single value are unwrapped when calling calculateAsync() with BaseMarketData.
   */
  public void unwrapScenarioResultsAsync() {
    ScenarioResult<String> scenarioResult = ScenarioResult.of("foo");
    ScenarioResultFunction fn = new ScenarioResultFunction(Measures.PRESENT_VALUE, scenarioResult);
    CalculationTask task = CalculationTask.of(TARGET, Measures.PRESENT_VALUE, 0, 0, fn, MarketDataMappings.empty(), NATURAL);
    Column column = Column.of(Measures.PRESENT_VALUE);
    CalculationTasks tasks = CalculationTasks.of(ImmutableList.of(task), ImmutableList.of(column));

    // using the direct executor means there is no need to close/shutdown the runner
    CalculationTaskRunner test = CalculationTaskRunner.of(MoreExecutors.newDirectExecutorService());
    Listener listener = new Listener();

    CalculationEnvironment marketData = MarketEnvironment.empty(VAL_DATE);
    test.calculateSingleScenarioAsync(tasks, marketData, REF_DATA, listener);
    CalculationResult calculationResult1 = listener.result;
    Result<?> result1 = calculationResult1.getResult();
    // Check the result contains the string directly, not the result wrapping the string
    assertThat(result1).hasValue("foo");

    CalculationEnvironment scenarioMarketData = MarketEnvironment.empty(VAL_DATE);
    test.calculateMultipleScenariosAsync(tasks, scenarioMarketData, REF_DATA, listener);
    CalculationResult calculationResult2 = listener.result;
    Result<?> result2 = calculationResult2.getResult();
    // Check the result contains the scenario result wrapping the string
    assertThat(result2).hasValue(scenarioResult);
  }

  //-------------------------------------------------------------------------
  private static class TestTarget implements CalculationTarget {
  }

  //-------------------------------------------------------------------------
  public static final class TestFunction implements CalculationFunction<TestTarget> {

    @Override
    public Set<Measure> supportedMeasures() {
      return MEASURES;
    }

    @Override
    public Currency naturalCurrency(TestTarget trade, ReferenceData refData) {
      return USD;
    }

    @Override
    public FunctionRequirements requirements(TestTarget target, Set<Measure> measures, ReferenceData refData) {
      return FunctionRequirements.builder()
          .singleValueRequirements(
              ImmutableSet.of(
                  TestKey.of("1"),
                  TestObservableKey.of("2")))
          .timeSeriesRequirements(TestObservableKey.of("3"))
          .build();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTarget target,
        Set<Measure> measures,
        CalculationMarketData marketData,
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
    public Set<Measure> supportedMeasures() {
      return ImmutableSet.of(measure);
    }

    @Override
    public Currency naturalCurrency(TestTarget trade, ReferenceData refData) {
      return USD;
    }

    @Override
    public FunctionRequirements requirements(TestTarget target, Set<Measure> measures, ReferenceData refData) {
      return FunctionRequirements.empty();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTarget target,
        Set<Measure> measures,
        CalculationMarketData marketData,
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
