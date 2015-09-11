/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.config.pricing;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;

import java.util.Optional;
import java.util.Set;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.engine.calculation.function.CalculationSingleFunction;
import com.opengamma.strata.engine.config.FunctionConfig;
import com.opengamma.strata.engine.config.Measure;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;
import com.opengamma.strata.engine.marketdata.FunctionRequirements;

/**
 * Test {@link PricingRule}.
 */
@Test
public class PricingRuleTest {

  private static final Measure MEASURE1 = Measure.of("1");
  private static final Measure MEASURE2 = Measure.of("2");
  private static final Measure MEASURE3 = Measure.of("3");

  private static final FunctionGroup<TestTrade1> GROUP =
      DefaultFunctionGroup.builder(TestTrade1.class)
          .name("GroupName")
          .addFunction(MEASURE1, FunctionConfig.of(TestFunction1.class))
          .addFunction(MEASURE2, FunctionConfig.of(TestFunction2.class))
          .build();

  private static final PricingRule<TestTrade1> PRICING_RULE =
      PricingRule.builder(TestTrade1.class)
          .functionGroup(GROUP)
          .addArgument("foo", "bar")
          .addMeasures(MEASURE1, MEASURE2)
          .build();

  public void groupAvailable() {
    Optional<ConfiguredFunctionGroup> result = PRICING_RULE.functionGroup(new TestTrade1(), MEASURE1);
    assertThat(result).hasValue(ConfiguredFunctionGroup.of(GROUP, ImmutableMap.of("foo", "bar")));
  }

  public void differentTargetType() {
    Optional<ConfiguredFunctionGroup> result = PRICING_RULE.functionGroup(new TestTrade2(), MEASURE1);
    assertThat(result).isEmpty();
  }

  public void measureNotFound() {
    Optional<ConfiguredFunctionGroup> result = PRICING_RULE.functionGroup(new TestTrade1(), MEASURE3);
    assertThat(result).isEmpty();
  }

  public void measureInRuleButNotGroup() {
    FunctionGroup<TestTrade1> group =
        DefaultFunctionGroup.builder(TestTrade1.class)
            .name("GroupName")
            .addFunction(MEASURE1, FunctionConfig.of(TestFunction1.class))
            .addFunction(MEASURE2, FunctionConfig.of(TestFunction2.class))
            .build();

    PricingRule<TestTrade1> pricingRule =
        PricingRule.builder(TestTrade1.class)
            .functionGroup(group)
            .addMeasures(MEASURE1)
            .build();

    Optional<ConfiguredFunctionGroup> functionGroup = pricingRule.functionGroup(new TestTrade2(), MEASURE2);
    assertThat(functionGroup).isEmpty();

    Set<Measure> trade1Measures = pricingRule.configuredMeasures(new TestTrade1());
    assertThat(trade1Measures).containsOnly(MEASURE1);

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

    Optional<ConfiguredFunctionGroup> functionGroup = rule.functionGroup(new TestTrade1(), MEASURE1);
    assertThat(functionGroup).hasValue(ConfiguredFunctionGroup.of(GROUP));

    Set<Measure> measures = rule.configuredMeasures(new TestTrade1());
    assertThat(measures).containsOnly(MEASURE1, MEASURE2);
  }

  public void measuresMatchingFunctionGroup() {
    Set<Measure> measures = PRICING_RULE.configuredMeasures(new TestTrade1());
    assertThat(measures).containsOnly(MEASURE1, MEASURE2);
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

}
