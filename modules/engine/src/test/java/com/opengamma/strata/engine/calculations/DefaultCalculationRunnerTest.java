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
import com.opengamma.strata.basics.index.IborIndices;
import com.opengamma.strata.basics.index.OvernightIndices;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.calculations.function.EngineSingleFunction;
import com.opengamma.strata.engine.config.CalculationTaskConfig;
import com.opengamma.strata.engine.config.CalculationTasksConfig;
import com.opengamma.strata.engine.config.FunctionConfig;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.config.SimpleMarketDataRules;
import com.opengamma.strata.engine.config.pricing.DefaultFunctionGroup;
import com.opengamma.strata.engine.config.pricing.DefaultPricingRules;
import com.opengamma.strata.engine.config.pricing.PricingRule;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.MarketDataRequirements;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;
import com.opengamma.strata.marketdata.id.MarketDataFeed;
import com.opengamma.strata.marketdata.id.MarketDataId;
import com.opengamma.strata.marketdata.id.ObservableId;
import com.opengamma.strata.marketdata.key.DiscountingCurveKey;
import com.opengamma.strata.marketdata.key.IndexRateKey;

@Test
public class DefaultCalculationRunnerTest {

  public void createCalculationConfig() {
    Measure measure = Measure.of("foo");

    MarketDataMappings marketDataMappings =
        MarketDataMappings.builder()
            .curveGroup("curve group")
            .marketDataFeed(MarketDataFeed.of("MarketDataFeed"))
            .build();

    SimpleMarketDataRules marketDataRules =
        SimpleMarketDataRules.builder()
            .addMappings(TestTarget.class, marketDataMappings)
            .build();

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
    SimpleMarketDataRules marketDataRules = SimpleMarketDataRules.builder().build();
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

    NoMatchingRuleId curveId = NoMatchingRuleId.of(DiscountingCurveKey.of(Currency.GBP));
    assertThat(nonObservables).hasSize(1);
    assertThat(nonObservables.iterator().next()).isEqualTo(curveId);

    MarketDataId<?> indexId =
        IndexRateKey.of(IborIndices.CHF_LIBOR_12M).toObservableId(MarketDataFeed.NO_RULE);
    assertThat(observables).hasSize(1);
    assertThat(observables.iterator().next()).isEqualTo(indexId);

    MarketDataId<?> toisId =
        IndexRateKey.of(OvernightIndices.CHF_TOIS).toObservableId(MarketDataFeed.NO_RULE);
    assertThat(timeSeries).hasSize(1);
    assertThat(timeSeries.iterator().next()).isEqualTo(toisId);
  }

  private static class TestTarget implements CalculationTarget { }

  public static final class TestFunction implements EngineSingleFunction<TestTarget, Object> {

    @Override
    public CalculationRequirements requirements(TestTarget target) {
      return CalculationRequirements.builder()
          .singleValueRequirements(
              ImmutableSet.of(
                  DiscountingCurveKey.of(Currency.GBP),
                  IndexRateKey.of(IborIndices.CHF_LIBOR_12M)))
          .timeSeriesRequirements(IndexRateKey.of(OvernightIndices.CHF_TOIS))
          .build();
    }

    @Override
    public Object execute(TestTarget target, CalculationMarketData marketData) {
      return "bar";
    }
  }
}
