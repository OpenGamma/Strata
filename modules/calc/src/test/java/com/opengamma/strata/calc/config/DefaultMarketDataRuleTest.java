/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;

@Test
public class DefaultMarketDataRuleTest {

  /**
   * Tests matching a target whose exact type is included in the list of target types.
   */
  public void matchExactTargetType() {
    MarketDataRule rule = MarketDataRule.of(MarketDataMappings.empty(), TargetClass1.class, TargetClass2.class);
    assertThat(rule.mappings(new TargetClass1())).hasValue(MarketDataMappings.empty());
    assertThat(rule.mappings(new TargetClass2())).hasValue(MarketDataMappings.empty());
  }

  /**
   * Tests matching a target which implements an interface included in the list of target types.
   */
  public void matchSubtypeOfTargetType() {
    MarketDataRule rule = MarketDataRule.of(MarketDataMappings.empty(), TargetInterface1.class, TargetInterface2.class);
    assertThat(rule.mappings(new TargetClass1())).hasValue(MarketDataMappings.empty());
    assertThat(rule.mappings(new TargetClass2())).hasValue(MarketDataMappings.empty());
  }

  /**
   * Tests that the rule doesn't match a target which has no supertype in the set of target types.
   */
  public void noMatch() {
    MarketDataRule rule = MarketDataRule.of(MarketDataMappings.empty(), TargetClass1.class);
    assertThat(rule.mappings(new TargetClass2())).isEmpty();
  }

  private interface TargetInterface1 extends CalculationTarget { }
  private interface TargetInterface2 extends CalculationTarget { }
  private static final class TargetClass1 implements TargetInterface1 { }
  private static final class TargetClass2 implements TargetInterface2 { }
}
