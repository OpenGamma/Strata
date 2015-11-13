/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config.pricing;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;

import java.util.Optional;
import java.util.Set;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.CalculationSingleFunction;

/**
 * Test {@link PricingRules}.
 */
@Test
public class PricingRulesTest {

  private static final CalculationTarget TRADE1 = new TestTrade1();
  private static final CalculationTarget TRADE2 = new TestTrade2();
  private static final Measure FOO_MEASURE = Measure.of("foo");
  private static final Measure BAR_MEASURE = Measure.of("bar");
  private static final Measure BAZ_MEASURE = Measure.of("baz");

  private static final FunctionGroup<TestTrade1> FUNCTION_GROUP1 =
      DefaultFunctionGroup.builder(TestTrade1.class)
          .name("group1")
          .addFunction(FOO_MEASURE, TestFunction1.class)
          .addFunction(BAR_MEASURE, TestFunction2.class)
          .build();

  private static final FunctionGroup<TestTrade2> FUNCTION_GROUP2 =
      DefaultFunctionGroup.builder(TestTrade2.class)
          .name("group2")
          .addFunction(FOO_MEASURE, TestFunction3.class)
          .build();

  private static final PricingRule<?> PRICING_RULE1 =
      PricingRule.builder(TestTrade1.class)
          .functionGroup(FUNCTION_GROUP1)
          .addMeasures(FOO_MEASURE, BAR_MEASURE)
          .build();

  private static final PricingRule<?> PRICING_RULE2 =
      PricingRule.builder(TestTrade2.class)
          .functionGroup(FUNCTION_GROUP2)
          .addMeasures(FOO_MEASURE)
          .build();

  private static final PricingRules PRICING_RULES1 = DefaultPricingRules.of(PRICING_RULE1);
  private static final PricingRules PRICING_RULES2 = DefaultPricingRules.of(PRICING_RULE2);

  public void ofEmpty() {
    PricingRules rules = PricingRules.of();
    Optional<ConfiguredFunctionGroup> functionGroup = rules.functionGroup(TRADE1, FOO_MEASURE);
    Set<Measure> measures = rules.configuredMeasures(TRADE1);

    assertThat(rules).isInstanceOf(EmptyPricingRules.class);
    assertThat(functionGroup).isEmpty();
    assertThat(measures).isEmpty();
  }

  public void ofSingle() {
    PricingRules rules = PricingRules.of(PRICING_RULES1);
    Optional<ConfiguredFunctionGroup> functionGroup = rules.functionGroup(TRADE1, FOO_MEASURE);
    Set<Measure> measures = rules.configuredMeasures(TRADE1);

    assertThat(rules).isInstanceOf(DefaultPricingRules.class);
    assertThat(functionGroup).hasValue(ConfiguredFunctionGroup.of(FUNCTION_GROUP1));
    assertThat(measures).containsOnly(FOO_MEASURE, BAR_MEASURE);
  }

  public void ofMultiple() {
    PricingRules rules = PricingRules.of(PRICING_RULES1, PRICING_RULES2);
    Optional<ConfiguredFunctionGroup> functionGroup1 = rules.functionGroup(TRADE1, FOO_MEASURE);
    Optional<ConfiguredFunctionGroup> functionGroup2 = rules.functionGroup(TRADE1, BAR_MEASURE);
    Optional<ConfiguredFunctionGroup> functionGroup3 = rules.functionGroup(TRADE2, FOO_MEASURE);
    Optional<ConfiguredFunctionGroup> functionGroup4 = rules.functionGroup(TRADE1, BAZ_MEASURE);

    assertThat(functionGroup1).hasValue(ConfiguredFunctionGroup.of(FUNCTION_GROUP1));
    assertThat(functionGroup2).hasValue(ConfiguredFunctionGroup.of(FUNCTION_GROUP1));
    assertThat(functionGroup3).hasValue(ConfiguredFunctionGroup.of(FUNCTION_GROUP2));
    assertThat(functionGroup4).isEmpty();

    Set<Measure> measures1 = rules.configuredMeasures(TRADE1);
    assertThat(measures1).containsOnly(FOO_MEASURE, BAR_MEASURE);

    Set<Measure> measures2 = rules.configuredMeasures(TRADE2);
    assertThat(measures2).containsOnly(FOO_MEASURE);
  }

  public void composedWithEmpty() {
    PricingRules rules = PricingRules.empty().composedWith(PRICING_RULES1);
    Optional<ConfiguredFunctionGroup> functionConfig = rules.functionGroup(TRADE1, FOO_MEASURE);
    Set<Measure> measures = rules.configuredMeasures(TRADE1);

    assertThat(rules).isInstanceOf(DefaultPricingRules.class);
    assertThat(functionConfig).hasValue(ConfiguredFunctionGroup.of(FUNCTION_GROUP1));
    assertThat(measures).containsOnly(FOO_MEASURE, BAR_MEASURE);
  }

  public void composedWithDefault() {
    PricingRules rules = PRICING_RULES1.composedWith(PRICING_RULES2);
    Optional<ConfiguredFunctionGroup> functionGroup1 = rules.functionGroup(TRADE1, FOO_MEASURE);
    Optional<ConfiguredFunctionGroup> functionGroup2 = rules.functionGroup(TRADE1, BAR_MEASURE);
    Optional<ConfiguredFunctionGroup> functionGroup3 = rules.functionGroup(TRADE2, FOO_MEASURE);
    Optional<ConfiguredFunctionGroup> functionGroup4 = rules.functionGroup(TRADE2, BAR_MEASURE);
    Optional<ConfiguredFunctionGroup> functionGroup5 = rules.functionGroup(TRADE1, BAZ_MEASURE);
    Optional<ConfiguredFunctionGroup> functionGroup6 = rules.functionGroup(TRADE2, BAZ_MEASURE);

    assertThat(functionGroup1).hasValue(ConfiguredFunctionGroup.of(FUNCTION_GROUP1));
    assertThat(functionGroup2).hasValue(ConfiguredFunctionGroup.of(FUNCTION_GROUP1));
    assertThat(functionGroup3).hasValue(ConfiguredFunctionGroup.of(FUNCTION_GROUP2));
    assertThat(functionGroup4).isEmpty();
    assertThat(functionGroup5).isEmpty();
    assertThat(functionGroup6).isEmpty();
  }

  private static final class TestTrade1 implements CalculationTarget {
  }

  private static final class TestTrade2 implements CalculationTarget {
  }

  private static final class TestFunction1 implements CalculationSingleFunction<TestTrade1, Object> {

    @Override
    public FunctionRequirements requirements(TestTrade1 trade) {
      return FunctionRequirements.empty();
    }

    @Override
    public Object execute(TestTrade1 target, CalculationMarketData marketData) {
      return "foo";
    }
  }

  private static final class TestFunction2 implements CalculationSingleFunction<TestTrade1, Object> {

    @Override
    public FunctionRequirements requirements(TestTrade1 trade) {
      return FunctionRequirements.empty();
    }

    @Override
    public Object execute(TestTrade1 target, CalculationMarketData marketData) {
      return "foo";
    }
  }

  private static final class TestFunction3 implements CalculationSingleFunction<TestTrade2, Object> {

    @Override
    public FunctionRequirements requirements(TestTrade2 target) {
      return FunctionRequirements.empty();
    }

    @Override
    public Object execute(TestTrade2 target, CalculationMarketData marketData) {
      return "bar";
    }
  }
}
