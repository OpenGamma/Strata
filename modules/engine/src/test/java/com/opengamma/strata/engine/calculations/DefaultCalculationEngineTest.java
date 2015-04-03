/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.engine.Column;
import com.opengamma.strata.engine.config.CalculationTaskConfig;
import com.opengamma.strata.engine.config.CalculationTasksConfig;
import com.opengamma.strata.engine.config.EngineFunctionConfig;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.config.SimpleMarketDataRules;
import com.opengamma.strata.engine.config.SimplePricingRules;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;

@Test
public class DefaultCalculationEngineTest {

  public void createCalculationConfig() {
    SimpleMarketDataRules marketDataRules = SimpleMarketDataRules.builder().build();
    Measure measure = Measure.of("foo");
    SimplePricingRules pricingRules =
        SimplePricingRules.builder()
            .addCalculation(measure, TestTarget.class, TestFunction.class)
            .build();
    ReportingRules reportingRules = ReportingRules.fixedCurrency(Currency.GBP);
    DefaultCalculationEngine engine = new DefaultCalculationEngine(MoreExecutors.newDirectExecutorService());
    TestTarget target1 = new TestTarget();
    TestTarget target2 = new TestTarget();
    List<TestTarget> targets = ImmutableList.of(target1, target2);
    Column column = Column.builder().measure(measure).build();
    List<Column> columns = ImmutableList.of(column);
    MarketDataMappings expectedMappings = MarketDataMappings.builder().build();
    EngineFunctionConfig expectedFnConfig = EngineFunctionConfig.builder().functionType(TestFunction.class).build();

    CalculationTasksConfig calculationConfig =
        engine.createCalculationConfig(targets, columns, pricingRules, marketDataRules, reportingRules);

    List<CalculationTaskConfig> taskConfigs = calculationConfig.getTaskConfigurations();

    assertThat(taskConfigs).hasSize(2);

    CalculationTaskConfig taskConfig1 = taskConfigs.get(0);
    assertThat(taskConfig1.getTarget()).isEqualTo(target1);
    assertThat(taskConfig1.getReportingRules()).isEqualTo(reportingRules);
    assertThat(taskConfig1.getMarketDataMappings()).isEqualTo(expectedMappings);
    assertThat(taskConfig1.getEngineFunctionConfig()).isEqualTo(expectedFnConfig);

    CalculationTaskConfig taskConfig2 = taskConfigs.get(1);
    assertThat(taskConfig2.getTarget()).isEqualTo(target2);
    assertThat(taskConfig2.getReportingRules()).isEqualTo(reportingRules);
    assertThat(taskConfig2.getMarketDataMappings()).isEqualTo(expectedMappings);
    assertThat(taskConfig2.getEngineFunctionConfig()).isEqualTo(expectedFnConfig);
  }

  private static class TestTarget implements CalculationTarget { }

  private static final class TestFunction implements VectorEngineFunction<TestTarget, Object> {

    @Override
    public CalculationRequirements requirements(TestTarget target) {
      return CalculationRequirements.EMPTY;
    }

    @Override
    public Object execute(TestTarget input, CalculationMarketData marketData, ReportingRules reportingRules) {
      return "bar";
    }
  }
}
