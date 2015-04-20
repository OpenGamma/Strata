/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.config;

import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.engine.marketdata.mapping.MarketDataMappings;

/**
 * Market data rules specify what market data should be used when calculating measures for a target.
 * <p>
 * For example, a calculation might require a USD discounting curve, but the system can contain
 * multiple curve groups, each with a USD discounting curve. The market data rules allow the system
 * to choose the correct curve.
 */
public interface MarketDataRules {

  /** An empty set of rules. */
  public static final MarketDataRules EMPTY = EmptyMarketDataRules.builder().build();

  /**
   * Returns a set of market data rules that delegates to multiple underlying sets of rules, returning the first
   * valid mapping it finds
   *
   * @param rules  the delegate market data rules
   * @return a set of market data rules that delegates to multiple underlying sets of rules, returning the first
   *   valid mapping it finds
   */
  public static MarketDataRules of(MarketDataRules... rules) {
    switch (rules.length) {
      case 0:
        return MarketDataRules.EMPTY;
      case 1:
        return rules[0];
      default:
        return CompositeMarketDataRules.builder().rules(ImmutableList.copyOf(rules)).build();
    }
  }

  /**
   * Returns a set of rules that return mappings from this rule if available, otherwise returning mappings
   * from the other rule.
   *
   * @param rules  other market data rules
   * @return a set of rules that return mappings from this rule if available, otherwise returning mappings
   *   from the other rule
   */
  public default MarketDataRules composedWith(MarketDataRules rules) {
    return of(this, rules);
  }

  /**
   * Returns a set of market data mappings which specify the market data that should be used when
   * performing calculations for a target.
   *
   * @param target  the target
   * @return mappings specifying which market data should be used when performing calculations for the target
   */
  public abstract Optional<MarketDataMappings> mappings(CalculationTarget target);
}
