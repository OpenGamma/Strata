/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;
import static com.opengamma.strata.collect.TestHelper.date;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.basics.market.TestObservableKey;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.config.CalculationTaskConfig;
import com.opengamma.strata.calc.config.CalculationTasksConfig;
import com.opengamma.strata.calc.config.FunctionConfig;
import com.opengamma.strata.calc.config.MarketDataRule;
import com.opengamma.strata.calc.config.MarketDataRules;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.ReportingRules;
import com.opengamma.strata.calc.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.calc.config.pricing.DefaultPricingRules;
import com.opengamma.strata.calc.config.pricing.PricingRule;
import com.opengamma.strata.calc.marketdata.CalculationEnvironment;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.CalculationRequirements;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.marketdata.TestKey;
import com.opengamma.strata.calc.marketdata.mapping.DefaultMarketDataMappings;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.calc.runner.function.CalculationSingleFunction;
import com.opengamma.strata.calc.runner.function.result.DefaultScenarioResult;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.result.Result;

@Test
public class DefaultCalculationRunnerTest {

  public void createCalculationConfig() {
    Measure measure = Measure.of("foo");

    MarketDataMappings marketDataMappings = DefaultMarketDataMappings.builder()
        .marketDataFeed(MarketDataFeed.of("MarketDataFeed"))
        .build();

    MarketDataRules marketDataRules = MarketDataRules.of(MarketDataRule.of(marketDataMappings, TestTarget.class));

    DefaultFunctionGroup<TestTarget> functionGroup =
        DefaultFunctionGroup.builder(TestTarget.class)
            .name("DefaultGroup")
            .addFunction(measure, TestFunction.class)
            .build();

    PricingRule<TestTarget> pricingRule = PricingRule.builder(TestTarget.class)
        .functionGroup(functionGroup)
        .addMeasures(measure)
        .build();

    DefaultPricingRules pricingRules = DefaultPricingRules.of(pricingRule);

    ReportingRules reportingRules = ReportingRules.fixedCurrency(Currency.GBP);
    DefaultCalculationRunner engine = new DefaultCalculationRunner(MoreExecutors.newDirectExecutorService());
    TestTarget target1 = new TestTarget();
    TestTarget target2 = new TestTarget();
    List<TestTarget> targets = ImmutableList.of(target1, target2);
    Column column = Column.of(measure);
    List<Column> columns = ImmutableList.of(column);

    FunctionConfig<TestTarget> expectedFnConfig = FunctionConfig.of(TestFunction.class);

    CalculationTasksConfig calculationConfig =
        engine.createCalculationConfig(targets, columns, pricingRules, marketDataRules, reportingRules);

    List<CalculationTaskConfig> taskConfigs = calculationConfig.getTaskConfigurations();

    assertThat(taskConfigs).hasSize(2);

    CalculationTaskConfig taskConfig1 = taskConfigs.get(0);
    assertThat(taskConfig1.getTarget()).isEqualTo(target1);
    assertThat(taskConfig1.getReportingRules()).isEqualTo(reportingRules);
    assertThat(taskConfig1.getMarketDataMappings()).isEqualTo(marketDataMappings);
    assertThat(taskConfig1.getFunctionConfig()).isEqualTo(expectedFnConfig);

    CalculationTaskConfig taskConfig2 = taskConfigs.get(1);
    assertThat(taskConfig2.getTarget()).isEqualTo(target2);
    assertThat(taskConfig2.getReportingRules()).isEqualTo(reportingRules);
    assertThat(taskConfig2.getMarketDataMappings()).isEqualTo(marketDataMappings);
    assertThat(taskConfig2.getFunctionConfig()).isEqualTo(expectedFnConfig);
  }

  public void noMatchingMarketDataRules() {
    MarketDataRules marketDataRules = MarketDataRules.empty();
    Measure measure = Measure.of("foo");

    DefaultFunctionGroup<TestTarget> functionGroup = DefaultFunctionGroup.builder(TestTarget.class)
        .name("DefaultGroup")
        .addFunction(measure, TestFunction.class)
        .build();

    PricingRule<TestTarget> pricingRule = PricingRule.builder(TestTarget.class)
        .functionGroup(functionGroup)
        .addMeasures(measure)
        .build();

    DefaultPricingRules pricingRules = DefaultPricingRules.of(pricingRule);

    ReportingRules reportingRules = ReportingRules.fixedCurrency(Currency.GBP);
    DefaultCalculationRunner runner = new DefaultCalculationRunner(MoreExecutors.newDirectExecutorService());
    TestTarget target1 = new TestTarget();
    List<TestTarget> targets = ImmutableList.of(target1);
    Column column = Column.of(measure);
    List<Column> columns = ImmutableList.of(column);

    CalculationTasksConfig calculationConfig =
        runner.createCalculationConfig(targets, columns, pricingRules, marketDataRules, reportingRules);
    CalculationTasks calculationTasks = runner.createCalculationTasks(calculationConfig);
    CalculationRequirements requirements = calculationTasks.getRequirements();
    Set<? extends MarketDataId<?>> nonObservables = requirements.getNonObservables();
    ImmutableSet<? extends ObservableId> observables = requirements.getObservables();
    ImmutableSet<ObservableId> timeSeries = requirements.getTimeSeries();

    NoMatchingRuleId nonObservableId = NoMatchingRuleId.of(TestKey.of("1"));
    assertThat(nonObservables).hasSize(1);
    assertThat(nonObservables.iterator().next()).isEqualTo(nonObservableId);

    MarketDataId<?> observableId = TestObservableKey.of("2").toObservableId(MarketDataFeed.NO_RULE);
    assertThat(observables).hasSize(1);
    assertThat(observables.iterator().next()).isEqualTo(observableId);

    MarketDataId<?> timeSeriesId = TestObservableKey.of("3").toObservableId(MarketDataFeed.NO_RULE);
    assertThat(timeSeries).hasSize(1);
    assertThat(timeSeries.iterator().next()).isEqualTo(timeSeriesId);
  }

  /**
   * Test that ScenarioResults containing a single value are unwrapped when calling calculate() with BaseMarketData.
   */
  public void unwrapScenarioResults() {
    DefaultScenarioResult<String> scenarioResult = DefaultScenarioResult.of("foo");
    ScenarioResultFunction fn = new ScenarioResultFunction(scenarioResult);
    TestTarget target = new TestTarget();
    CalculationTask task = new CalculationTask(target, 0, 0, fn, MarketDataMappings.empty(), ReportingRules.empty());
    Column column = Column.of(Measure.PRESENT_VALUE);
    CalculationTasks tasks = new CalculationTasks(ImmutableList.of(task), ImmutableList.of(column));
    DefaultCalculationRunner runner = new DefaultCalculationRunner(MoreExecutors.newDirectExecutorService());
    LocalDate valuationDate = date(2011, 3, 8);

    CalculationEnvironment marketData = CalculationEnvironment.builder().valuationDate(date(2011, 3, 8)).build();
    Results results1 = runner.calculateSingleScenario(tasks, marketData);
    Result<?> result1 = results1.get(0, 0);
    // Check the result contains the string directly, not the result wrapping the string
    assertThat(result1).hasValue("foo");

    CalculationEnvironment scenarioMarketData = CalculationEnvironment.builder().valuationDate(valuationDate).build();
    Results results2 = runner.calculateMultipleScenarios(tasks, scenarioMarketData);
    Result<?> result2 = results2.get(0, 0);
    // Check the result contains the scenario result wrapping the string
    assertThat(result2).hasValue(scenarioResult);
  }

  /**
   * Test that ScenarioResults containing a single value are unwrapped when calling calculateAsync() with BaseMarketData.
   */
  public void unwrapScenarioResultsAsync() {
    DefaultScenarioResult<String> scenarioResult = DefaultScenarioResult.of("foo");
    ScenarioResultFunction fn = new ScenarioResultFunction(scenarioResult);
    TestTarget target = new TestTarget();
    CalculationTask task = new CalculationTask(target, 0, 0, fn, MarketDataMappings.empty(), ReportingRules.empty());
    Column column = Column.of(Measure.PRESENT_VALUE);
    CalculationTasks tasks = new CalculationTasks(ImmutableList.of(task), ImmutableList.of(column));
    DefaultCalculationRunner runner = new DefaultCalculationRunner(MoreExecutors.newDirectExecutorService());
    LocalDate valuationDate = date(2011, 3, 8);
    Listener listener = new Listener();

    CalculationEnvironment marketData = CalculationEnvironment.builder().valuationDate(date(2011, 3, 8)).build();
    runner.calculateSingleScenarioAsync(tasks, marketData, listener);
    CalculationResult calculationResult1 = listener.result;
    Result<?> result1 = calculationResult1.getResult();
    // Check the result contains the string directly, not the result wrapping the string
    assertThat(result1).hasValue("foo");

    CalculationEnvironment scenarioMarketData = CalculationEnvironment.builder().valuationDate(valuationDate).build();
    runner.calculateMultipleScenariosAsync(tasks, scenarioMarketData, listener);
    CalculationResult calculationResult2 = listener.result;
    Result<?> result2 = calculationResult2.getResult();
    // Check the result contains the scenario result wrapping the string
    assertThat(result2).hasValue(scenarioResult);
  }

  //--------------------------------------------------------------------------------------------------------------------

  private static class TestTarget implements CalculationTarget { }

  public static final class TestFunction implements CalculationSingleFunction<TestTarget, Object> {

    @Override
    public FunctionRequirements requirements(TestTarget target) {
      return FunctionRequirements.builder()
          .singleValueRequirements(
              ImmutableSet.of(
                  TestKey.of("1"),
                  TestObservableKey.of("2")))
          .timeSeriesRequirements(TestObservableKey.of("3"))
          .build();
    }

    @Override
    public Object execute(TestTarget target, CalculationMarketData marketData) {
      return "bar";
    }
  }

  private static final class ScenarioResultFunction
      implements CalculationSingleFunction<TestTarget, ScenarioResult<String>> {

    private final ScenarioResult<String> result;

    private ScenarioResultFunction(ScenarioResult<String> result) {
      this.result = result;
    }

    @Override
    public ScenarioResult<String> execute(TestTarget target, CalculationMarketData marketData) {
      return result;
    }

    @Override
    public FunctionRequirements requirements(TestTarget target) {
      return FunctionRequirements.empty();
    }
  }

  private static final class Listener implements CalculationListener {

    private CalculationResult result;

    @Override
    public void resultReceived(CalculationResult result) {
      this.result = result;
    }

    @Override
    public void calculationsComplete() {
      // Do nothing
    }
  }
}
