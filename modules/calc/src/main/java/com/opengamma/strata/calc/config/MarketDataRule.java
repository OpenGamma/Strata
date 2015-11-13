/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config;

import java.util.Optional;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.marketdata.mapping.MarketDataMappings;

/**
 * A market data rule decides what market data should be used in calculations for a calculation target.
 * <p>
 * A rule returns a set of {@link MarketDataMappings} for a calculation target that matches the rule, otherwise
 * it returns an empty {@code Optional}.
 */
public interface MarketDataRule {

  /**
   * Returns a market data rule that matches any target which is an instance of any of the target types.
   *
   * @param mappings  the market data mappings used for a target matching this rule
   * @param targetTypes  types that targets must implement to in order to match this rule
   * @return a market data rule that matches any target that is an instance of any of the target types
   */
  @SafeVarargs
  public static MarketDataRule of(MarketDataMappings mappings, Class<? extends CalculationTarget>... targetTypes) {
    return DefaultMarketDataRule.of(mappings, targetTypes);
  }

  /**
   * Returns a market data rule that matches any target.
   *
   * @param mappings  the mappings used for any target passed to this rule
   * @return a market data rule that matches any target
   */
  public static MarketDataRule anyTarget(MarketDataMappings mappings) {
    return AllTargetsMarketDataRule.of(mappings);
  }

  /**
   * Returns a set of market data mappings for the target if it matches this rule, otherwise an empty {@code Optional}.
   *
   * @param target  a calculation target
   * @return a set of market data mappings for the target if it matches this rule, otherwise an empty {@code Optional}
   */
  public abstract Optional<MarketDataMappings> mappings(CalculationTarget target);
}
