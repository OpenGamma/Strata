/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config;

import static com.opengamma.strata.collect.CollectProjectAssertions.assertThat;

import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.basics.market.MarketDataFeed;
import com.opengamma.strata.calc.marketdata.TestKey;
import com.opengamma.strata.calc.marketdata.TestMapping;
import com.opengamma.strata.calc.marketdata.mapping.DefaultMarketDataMappings;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;

@Test
public class MarketDataRulesTest {

  private static final TestTrade1 TRADE1 = new TestTrade1();
  private static final TestTrade2 TRADE2 = new TestTrade2();
  private static final TestTrade3 TRADE3 = new TestTrade3();
  private static final TestTrade4 TRADE4 = new TestTrade4();
  private static final MarketDataMappings MAPPINGS1 = mappings("1");
  private static final MarketDataMappings MAPPINGS2 = mappings("2");
  private static final MarketDataMappings MAPPINGS3 = mappings("3");

  private static final MarketDataRules RULES1 = MarketDataRules.ofTargetTypes(MAPPINGS1, TestTrade1.class);
  private static final MarketDataRules RULES2 = MarketDataRules.ofTargetTypes(MAPPINGS2, TestTrade2.class);
  private static final MarketDataRules RULES3 = MarketDataRules.ofTargetTypes(MAPPINGS3, TestTrade3.class);

  public void composedWithComposite() {
    CompositeMarketDataRules compositeRules = CompositeMarketDataRules.of(RULES1, RULES2);
    MarketDataRules rules = compositeRules.composedWith(RULES3);
    Optional<MarketDataMappings> mappings1 = rules.mappings(TRADE1);
    Optional<MarketDataMappings> mappings2 = rules.mappings(TRADE2);
    Optional<MarketDataMappings> mappings3 = rules.mappings(TRADE3);
    Optional<MarketDataMappings> mappings4 = rules.mappings(TRADE4);

    assertThat(mappings1).hasValue(MAPPINGS1);
    assertThat(mappings2).hasValue(MAPPINGS2);
    assertThat(mappings3).hasValue(MAPPINGS3);
    assertThat(mappings4).isEmpty();
  }

  public void composedWithEmpty() {
    MarketDataRules rules = MarketDataRules.empty().composedWith(RULES1);
    Optional<MarketDataMappings> mappings = rules.mappings(TRADE1);
    assertThat(mappings).hasValue(MAPPINGS1);
  }

  public void composedWithSimple() {
    MarketDataRules rules = RULES1.composedWith(RULES2);
    Optional<MarketDataMappings> mappings1 = rules.mappings(TRADE1);
    Optional<MarketDataMappings> mappings2 = rules.mappings(TRADE2);
    Optional<MarketDataMappings> mappings3 = rules.mappings(TRADE3);

    assertThat(mappings1).hasValue(MAPPINGS1);
    assertThat(mappings2).hasValue(MAPPINGS2);
    assertThat(mappings3).isEmpty();
  }

  private static MarketDataMappings mappings(String str) {
    return DefaultMarketDataMappings.builder()
        .marketDataFeed(MarketDataFeed.NONE)
        .mappings(ImmutableMap.of(TestKey.class, new TestMapping(str)))
        .build();
  }

  private static final class TestTrade1 implements CalculationTarget { }
  private static final class TestTrade2 implements CalculationTarget { }
  private static final class TestTrade3 implements CalculationTarget { }
  private static final class TestTrade4 implements CalculationTarget { }

}
