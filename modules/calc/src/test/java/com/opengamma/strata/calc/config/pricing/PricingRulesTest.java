/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config.pricing;

import static com.opengamma.strata.basics.currency.Currency.USD;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.result.Result;

/**
 * Test {@link PricingRules}.
 */
@Test
public class PricingRulesTest {

  private static final CalculationTarget TRADE1 = new TestTrade1();
  private static final CalculationTarget TRADE2 = new TestTrade2();
  private static final Set<Measure> MEASURES = ImmutableSet.of(Measures.PRESENT_VALUE, Measures.PAR_RATE);

  private static final FunctionGroup<TestTrade1> FUNCTION_GROUP1 =
      DefaultFunctionGroup.builder(TestTrade1.class)
          .name("group1")
          .addFunction(Measures.PRESENT_VALUE, TestFunction1.class)
          .addFunction(Measures.PAR_RATE, TestFunction2.class)
          .build();

  private static final FunctionGroup<TestTrade2> FUNCTION_GROUP2 =
      DefaultFunctionGroup.builder(TestTrade2.class)
          .name("group2")
          .addFunction(Measures.PRESENT_VALUE, TestFunction3.class)
          .build();

  private static final PricingRule<?> PRICING_RULE1 =
      PricingRule.builder(TestTrade1.class)
          .functionGroup(FUNCTION_GROUP1)
          .addMeasures(Measures.PRESENT_VALUE, Measures.PAR_RATE)
          .build();

  private static final PricingRule<?> PRICING_RULE2 =
      PricingRule.builder(TestTrade2.class)
          .functionGroup(FUNCTION_GROUP2)
          .addMeasures(Measures.PRESENT_VALUE)
          .build();

  private static final PricingRules PRICING_RULES1 = PricingRules.of(PRICING_RULE1);
  private static final PricingRules PRICING_RULES2 = PricingRules.of(PRICING_RULE2);

  public void ofEmpty() {
    PricingRules rules = PricingRules.of();
    Optional<ConfiguredFunctionGroup> functionGroup = rules.functionGroup(TRADE1, Measures.PRESENT_VALUE);
    Set<Measure> measures = rules.configuredMeasures(TRADE1);

    assertThat(functionGroup).isEmpty();
    assertThat(measures).isEmpty();
  }

  public void composedWith() {
    PricingRules rules = PRICING_RULES1.composedWith(PRICING_RULES2);
    Optional<ConfiguredFunctionGroup> functionGroup1 = rules.functionGroup(TRADE1, Measures.PRESENT_VALUE);
    Optional<ConfiguredFunctionGroup> functionGroup2 = rules.functionGroup(TRADE1, Measures.PAR_RATE);
    Optional<ConfiguredFunctionGroup> functionGroup3 = rules.functionGroup(TRADE2, Measures.PRESENT_VALUE);
    Optional<ConfiguredFunctionGroup> functionGroup4 = rules.functionGroup(TRADE1, Measures.PV01);

    assertThat(functionGroup1).hasValue(ConfiguredFunctionGroup.of(FUNCTION_GROUP1));
    assertThat(functionGroup2).hasValue(ConfiguredFunctionGroup.of(FUNCTION_GROUP1));
    assertThat(functionGroup3).hasValue(ConfiguredFunctionGroup.of(FUNCTION_GROUP2));
    assertThat(functionGroup4).isEmpty();

    Set<Measure> measures1 = rules.configuredMeasures(TRADE1);
    assertThat(measures1).containsOnly(Measures.PRESENT_VALUE, Measures.PAR_RATE);

    Set<Measure> measures2 = rules.configuredMeasures(TRADE2);
    assertThat(measures2).containsOnly(Measures.PRESENT_VALUE);
  }

  public void composedWithEmpty() {
    PricingRules rules = PricingRules.empty().composedWith(PRICING_RULES1);
    Optional<ConfiguredFunctionGroup> functionConfig = rules.functionGroup(TRADE1, Measures.PRESENT_VALUE);
    Set<Measure> measures = rules.configuredMeasures(TRADE1);

    assertThat(rules).isInstanceOf(DefaultPricingRules.class);
    assertThat(functionConfig).hasValue(ConfiguredFunctionGroup.of(FUNCTION_GROUP1));
    assertThat(measures).containsOnly(Measures.PRESENT_VALUE, Measures.PAR_RATE);
  }

  public void composedWithDefault() {
    PricingRules rules = PRICING_RULES1.composedWith(PRICING_RULES2);
    Optional<ConfiguredFunctionGroup> functionGroup1 = rules.functionGroup(TRADE1, Measures.PRESENT_VALUE);
    Optional<ConfiguredFunctionGroup> functionGroup2 = rules.functionGroup(TRADE1, Measures.PAR_RATE);
    Optional<ConfiguredFunctionGroup> functionGroup3 = rules.functionGroup(TRADE2, Measures.PRESENT_VALUE);
    Optional<ConfiguredFunctionGroup> functionGroup4 = rules.functionGroup(TRADE2, Measures.PAR_RATE);
    Optional<ConfiguredFunctionGroup> functionGroup5 = rules.functionGroup(TRADE1, Measures.PV01);
    Optional<ConfiguredFunctionGroup> functionGroup6 = rules.functionGroup(TRADE2, Measures.PV01);

    assertThat(functionGroup1).hasValue(ConfiguredFunctionGroup.of(FUNCTION_GROUP1));
    assertThat(functionGroup2).hasValue(ConfiguredFunctionGroup.of(FUNCTION_GROUP1));
    assertThat(functionGroup3).hasValue(ConfiguredFunctionGroup.of(FUNCTION_GROUP2));
    assertThat(functionGroup4).isEmpty();
    assertThat(functionGroup5).isEmpty();
    assertThat(functionGroup6).isEmpty();
  }

  //-------------------------------------------------------------------------
  private static final class TestTrade1 implements CalculationTarget {
  }

  //-------------------------------------------------------------------------
  private static final class TestTrade2 implements CalculationTarget {
  }

  //-------------------------------------------------------------------------
  // function for testing
  public static final class TestFunction1 implements CalculationFunction<TestTrade1> {

    @Override
    public Class<TestTrade1> targetType() {
      return TestTrade1.class;
    }

    @Override
    public Set<Measure> supportedMeasures() {
      return MEASURES;
    }

    @Override
    public Currency naturalCurrency(TestTrade1 trade, ReferenceData refData) {
      return USD;
    }

    @Override
    public FunctionRequirements requirements(TestTrade1 target, Set<Measure> measures, ReferenceData refData) {
      return FunctionRequirements.empty();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTrade1 target,
        Set<Measure> measures,
        CalculationMarketData marketData,
        ReferenceData refData) {

      ScenarioResult<String> array = ScenarioResult.of("foo");
      return ImmutableMap.of(Measures.PRESENT_VALUE, Result.success(array));
    }
  }

  //-------------------------------------------------------------------------
  // function for testing
  public static final class TestFunction2 implements CalculationFunction<TestTrade1> {

    @Override
    public Class<TestTrade1> targetType() {
      return TestTrade1.class;
    }

    @Override
    public Set<Measure> supportedMeasures() {
      return MEASURES;
    }

    @Override
    public Currency naturalCurrency(TestTrade1 trade, ReferenceData refData) {
      return USD;
    }

    @Override
    public FunctionRequirements requirements(TestTrade1 target, Set<Measure> measures, ReferenceData refData) {
      return FunctionRequirements.empty();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTrade1 target,
        Set<Measure> measures,
        CalculationMarketData marketData,
        ReferenceData refData) {

      ScenarioResult<String> array = ScenarioResult.of("foo");
      return ImmutableMap.of(Measures.PRESENT_VALUE, Result.success(array));
    }
  }

  //-------------------------------------------------------------------------
  // function for testing
  public static final class TestFunction3 implements CalculationFunction<TestTrade2> {

    @Override
    public Class<TestTrade2> targetType() {
      return TestTrade2.class;
    }

    @Override
    public Set<Measure> supportedMeasures() {
      return MEASURES;
    }

    @Override
    public Currency naturalCurrency(TestTrade2 trade, ReferenceData refData) {
      return USD;
    }

    @Override
    public FunctionRequirements requirements(TestTrade2 target, Set<Measure> measures, ReferenceData refData) {
      return FunctionRequirements.empty();
    }

    @Override
    public Map<Measure, Result<?>> calculate(
        TestTrade2 target,
        Set<Measure> measures,
        CalculationMarketData marketData,
        ReferenceData refData) {

      ScenarioResult<String> array = ScenarioResult.of("bar");
      return ImmutableMap.of(Measures.PAR_RATE, Result.success(array));
    }
  }

}
