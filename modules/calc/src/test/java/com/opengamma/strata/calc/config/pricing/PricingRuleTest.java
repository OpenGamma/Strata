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
import com.opengamma.strata.calc.config.FunctionConfig;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.calc.config.Measures;
import com.opengamma.strata.calc.marketdata.CalculationMarketData;
import com.opengamma.strata.calc.marketdata.FunctionRequirements;
import com.opengamma.strata.calc.runner.function.CalculationFunction;
import com.opengamma.strata.calc.runner.function.result.ScenarioResult;
import com.opengamma.strata.collect.result.Result;

/**
 * Test {@link PricingRule}.
 */
@Test
public class PricingRuleTest {

  private static final FunctionGroup<TestTrade1> GROUP =
      DefaultFunctionGroup.builder(TestTrade1.class)
          .name("GroupName")
          .addFunction(Measures.PRESENT_VALUE, FunctionConfig.of(TestFunction1.class))
          .addFunction(Measures.PAR_RATE, FunctionConfig.of(TestFunction2.class))
          .build();

  private static final PricingRule<TestTrade1> PRICING_RULE =
      PricingRule.builder(TestTrade1.class)
          .functionGroup(GROUP)
          .addArgument("foo", "bar")
          .addMeasures(Measures.PRESENT_VALUE, Measures.PAR_RATE)
          .build();

  public void groupAvailable() {
    Optional<ConfiguredFunctionGroup> result = PRICING_RULE.functionGroup(new TestTrade1(), Measures.PRESENT_VALUE);
    assertThat(result).hasValue(ConfiguredFunctionGroup.of(GROUP, ImmutableMap.of("foo", "bar")));
  }

  public void differentTargetType() {
    Optional<ConfiguredFunctionGroup> result = PRICING_RULE.functionGroup(new TestTrade2(), Measures.PRESENT_VALUE);
    assertThat(result).isEmpty();
  }

  public void measureNotFound() {
    Optional<ConfiguredFunctionGroup> result = PRICING_RULE.functionGroup(new TestTrade1(), Measures.PV01);
    assertThat(result).isEmpty();
  }

  public void measureInRuleButNotGroup() {
    FunctionGroup<TestTrade1> group =
        DefaultFunctionGroup.builder(TestTrade1.class)
            .name("GroupName")
            .addFunction(Measures.PRESENT_VALUE, FunctionConfig.of(TestFunction1.class))
            .addFunction(Measures.PAR_RATE, FunctionConfig.of(TestFunction2.class))
            .build();

    PricingRule<TestTrade1> pricingRule =
        PricingRule.builder(TestTrade1.class)
            .functionGroup(group)
            .addMeasures(Measures.PRESENT_VALUE)
            .build();

    Optional<ConfiguredFunctionGroup> functionGroup = pricingRule.functionGroup(new TestTrade2(), Measures.PAR_RATE);
    assertThat(functionGroup).isEmpty();

    Set<Measure> trade1Measures = pricingRule.configuredMeasures(new TestTrade1());
    assertThat(trade1Measures).containsOnly(Measures.PRESENT_VALUE);

    Set<Measure> trade2Measures = pricingRule.configuredMeasures(new TestTrade2());
    assertThat(trade2Measures).isEmpty();
  }

  /**
   * If a rule has an empty set of measures it means it should be used for all measures
   */
  public void ruleWithNoMeasures() {
    PricingRule<TestTrade1> rule =
        PricingRule.builder(TestTrade1.class)
            .functionGroup(GROUP)
            .build();

    Optional<ConfiguredFunctionGroup> functionGroup = rule.functionGroup(new TestTrade1(), Measures.PRESENT_VALUE);
    assertThat(functionGroup).hasValue(ConfiguredFunctionGroup.of(GROUP));

    Set<Measure> measures = rule.configuredMeasures(new TestTrade1());
    assertThat(measures).containsOnly(Measures.PRESENT_VALUE, Measures.PAR_RATE);
  }

  public void measuresMatchingFunctionGroup() {
    Set<Measure> measures = PRICING_RULE.configuredMeasures(new TestTrade1());
    assertThat(measures).containsOnly(Measures.PRESENT_VALUE, Measures.PAR_RATE);
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
      return ImmutableSet.of(Measures.PRESENT_VALUE);
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
      return ImmutableSet.of(Measures.PRESENT_VALUE);
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

}
