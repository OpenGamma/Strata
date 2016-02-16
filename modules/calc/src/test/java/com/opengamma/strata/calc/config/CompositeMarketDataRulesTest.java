/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;

@Test
public class CompositeMarketDataRulesTest {

  private static final MarketDataMappings MAPPINGS1 =
      MarketDataMappings.of(MarketDataFeed.of("feed1"), ImmutableList.of());

  private static final MarketDataMappings MAPPINGS2 =
      MarketDataMappings.of(MarketDataFeed.of("feed2"), ImmutableList.of());

  private static final MarketDataMappings MAPPINGS3 =
      MarketDataMappings.of(MarketDataFeed.of("feed3"), ImmutableList.of());

  private static final MarketDataRules RULE1 = MarketDataRules.ofTargetTypes(MAPPINGS1, TargetClass1.class);
  private static final MarketDataRules RULE2 = MarketDataRules.ofTargetTypes(MAPPINGS2, TargetClass2.class);
  private static final MarketDataRules RULE3 = MarketDataRules.anyTarget(MAPPINGS3);

  /**
   * Tests that the expected mappings are returned from matching rules.
   */
  public void match() {
    MarketDataRules rules = RULE1.composedWith(RULE2, RULE3);
    assertThat(rules.mappings(new TargetClass1())).hasValue(MAPPINGS1);
    assertThat(rules.mappings(new TargetClass2())).hasValue(MAPPINGS2);
  }

  /**
   * Tests that the mappings are returned from the fallback rule when there is no specific rule for a target.
   */
  public void matchFallbackRule() {
    MarketDataRules rules = RULE1.composedWith(RULE3);
    assertThat(rules.mappings(new TargetClass2())).hasValue(MAPPINGS3);
  }

  private static final class TargetClass1 implements CalculationTarget { }
  private static final class TargetClass2 implements CalculationTarget { }

}
