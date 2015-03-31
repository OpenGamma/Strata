/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.config;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;

import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.engine.calculations.CalculationRequirements;
import com.opengamma.strata.engine.calculations.VectorEngineFunction;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;

@Test
public class PricingRulesTest {

  private static final CalculationTarget TRADE = new TestTrade();
  private static final Measure FOO_MEASURE = Measure.of("foo");
  private static final Measure BAR_MEASURE = Measure.of("bar");
  private static final Measure BAZ_MEASURE = Measure.of("baz");

  private static final SimplePricingRules SIMPLE_RULES1 =
      SimplePricingRules.builder()
          .addCalculation(FOO_MEASURE, TestTrade.class, TestFunction1.class)
          .build();

  private static final SimplePricingRules SIMPLE_RULES2 =
      SimplePricingRules.builder()
          .addCalculation(BAR_MEASURE, TestTrade.class, TestFunction2.class)
          .build();

  private static final EngineFunctionConfig FUNCTION_CONFIG1 =
      EngineFunctionConfig.builder().functionType(TestFunction1.class).build();

  private static final EngineFunctionConfig FUNCTION_CONFIG2 =
      EngineFunctionConfig.builder().functionType(TestFunction2.class).build();

  public void ofEmpty() {
    PricingRules rules = PricingRules.of();
    Optional<EngineFunctionConfig> functionConfig = rules.functionConfig(TRADE, FOO_MEASURE);

    assertThat(rules).isInstanceOf(EmptyPricingRules.class);
    assertThat(functionConfig).isEmpty();
  }

  public void ofSingle() {
    PricingRules rules = PricingRules.of(SIMPLE_RULES1);
    Optional<EngineFunctionConfig> functionConfig = rules.functionConfig(TRADE, FOO_MEASURE);

    assertThat(rules).isInstanceOf(SimplePricingRules.class);
    assertThat(functionConfig).hasValue(FUNCTION_CONFIG1);
  }

  public void ofMultiple() {
    PricingRules rules = PricingRules.of(SIMPLE_RULES1, SIMPLE_RULES2);
    Optional<EngineFunctionConfig> functionConfig1 = rules.functionConfig(TRADE, FOO_MEASURE);
    Optional<EngineFunctionConfig> functionConfig2 = rules.functionConfig(TRADE, BAR_MEASURE);
    Optional<EngineFunctionConfig> functionConfig3 = rules.functionConfig(TRADE, BAZ_MEASURE);

    assertThat(functionConfig1).hasValue(FUNCTION_CONFIG1);
    assertThat(functionConfig2).hasValue(FUNCTION_CONFIG2);
    assertThat(functionConfig3).isEmpty();
  }

  public void composedWithComposite() {
    PricingRules compositeRules = CompositePricingRules.builder().rules(SIMPLE_RULES1, SIMPLE_RULES2).build();
    SimplePricingRules bazRules =
        SimplePricingRules.builder()
            .addCalculation(BAZ_MEASURE, TestTrade.class, TestFunction2.class)
            .build();
    PricingRules rules = compositeRules.composedWith(bazRules);
    EngineFunctionConfig bazConfig = EngineFunctionConfig.builder().functionType(TestFunction2.class).build();

    Optional<EngineFunctionConfig> functionConfig1 = rules.functionConfig(TRADE, FOO_MEASURE);
    Optional<EngineFunctionConfig> functionConfig2 = rules.functionConfig(TRADE, BAR_MEASURE);
    Optional<EngineFunctionConfig> functionConfig3 = rules.functionConfig(TRADE, BAZ_MEASURE);
    Optional<EngineFunctionConfig> functionConfig4 = rules.functionConfig(TRADE, Measure.of("qux"));

    assertThat(functionConfig1).hasValue(FUNCTION_CONFIG1);
    assertThat(functionConfig2).hasValue(FUNCTION_CONFIG2);
    assertThat(functionConfig3).hasValue(bazConfig);
    assertThat(functionConfig4).isEmpty();
  }

  public void composedWithEmpty() {
    PricingRules rules = PricingRules.EMPTY.composedWith(SIMPLE_RULES1);
    Optional<EngineFunctionConfig> functionConfig = rules.functionConfig(TRADE, FOO_MEASURE);

    assertThat(rules).isInstanceOf(SimplePricingRules.class);
    assertThat(functionConfig).hasValue(FUNCTION_CONFIG1);
  }

  public void composedWithSimple() {
    PricingRules rules = SIMPLE_RULES1.composedWith(SIMPLE_RULES2);
    Optional<EngineFunctionConfig> functionConfig1 = rules.functionConfig(TRADE, FOO_MEASURE);
    Optional<EngineFunctionConfig> functionConfig2 = rules.functionConfig(TRADE, BAR_MEASURE);
    Optional<EngineFunctionConfig> functionConfig3 = rules.functionConfig(TRADE, BAZ_MEASURE);

    assertThat(functionConfig1).hasValue(FUNCTION_CONFIG1);
    assertThat(functionConfig2).hasValue(FUNCTION_CONFIG2);
    assertThat(functionConfig3).isEmpty();
  }

  private static final class TestTrade implements CalculationTarget { }

  private static final class TestFunction1 implements VectorEngineFunction<TestTrade, Object> {

    @Override
    public CalculationRequirements requirements(TestTrade trade) {
      return CalculationRequirements.EMPTY;
    }

    @Override
    public Object execute(TestTrade input, CalculationMarketData marketData, ReportingRules reportingRules) {
      return "foo";
    }
  }

  private static final class TestFunction2 implements VectorEngineFunction<TestTrade, Object> {

    @Override
    public CalculationRequirements requirements(TestTrade trade) {
      return CalculationRequirements.EMPTY;
    }

    @Override
    public Object execute(TestTrade input, CalculationMarketData marketData, ReportingRules reportingRules) {
      return "foo";
    }
  }
}
