/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.calculations.function.CalculationSingleFunction;
import com.opengamma.strata.engine.config.CalculationTaskConfig;
import com.opengamma.strata.engine.config.CalculationTasksConfig;
import com.opengamma.strata.engine.config.FunctionConfig;
import com.opengamma.strata.engine.config.MarketDataRule;
import com.opengamma.strata.engine.config.MarketDataRules;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.engine.config.pricing.DefaultPricingRules;
import com.opengamma.strata.engine.config.pricing.PricingRule;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.CalculationRequirements;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.TestKey;
import com.opengamma.strata.engine.marketdata.TestObservableKey;
import com.opengamma.strata.engine.marketdata.mapping.DefaultMarketDataMappings;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;

@Test
public class DefaultCalculationRunnerTest {

  public void createCalculationConfig() {
    Measure measure = Measure.of("foo");

    MarketDataMappings marketDataMappings =
        DefaultMarketDataMappings.builder()
            .marketDataFeed(MarketDataFeed.of("MarketDataFeed"))
            .build();

    MarketDataRules marketDataRules = MarketDataRules.of(MarketDataRule.of(marketDataMappings, TestTarget.class));

    DefaultFunctionGroup<TestTarget> functionGroup =
        DefaultFunctionGroup.builder(TestTarget.class)
            .name("DefaultGroup")
            .addFunction(measure, TestFunction.class)
            .build();

    PricingRule pricingRule =
        PricingRule.builder(TestTarget.class)
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

    DefaultFunctionGroup<TestTarget> functionGroup =
        DefaultFunctionGroup.builder(TestTarget.class)
            .name("DefaultGroup")
            .addFunction(measure, TestFunction.class)
            .build();

    PricingRule<TestTarget> pricingRule =
        PricingRule.builder(TestTarget.class)
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
    MarketDataRequirements requirements = calculationTasks.getMarketDataRequirements();
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

  private static class TestTarget implements CalculationTarget { }

  public static final class TestFunction implements CalculationSingleFunction<TestTarget, Object> {

    @Override
    public CalculationRequirements requirements(TestTarget target) {
      return CalculationRequirements.builder()
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
}
