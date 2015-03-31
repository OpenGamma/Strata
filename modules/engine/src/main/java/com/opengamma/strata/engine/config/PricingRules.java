/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.config;

import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.CalculationTarget;

/**
 * Pricing rules specify how a measure should be calculated for a target. This includes the model,
 * model parameters and any other parameters that control the pricing calculations.
 * <p>
 * Pricing rules return a calculation policy for a target. The policy specifies how the calculation is
 * performed and creates the function to perform the calculation.
 */
public interface PricingRules {

  /** An empty set of pricing rules. */
  public static final PricingRules EMPTY = EmptyPricingRules.builder().build();

  /**
   * Returns a set of pricing rules that delegates to multiple underlying sets of rules, returning the first
   * valid configuration it finds.
   *
   * @param rules  the delegate pricing rules
   * @return a set of market data rules that delegates to multiple underlying sets of rules, returning the first
   *   valid configuration it finds
   */
  public static PricingRules of(PricingRules... rules) {
    switch (rules.length) {
      case 0:
        return PricingRules.EMPTY;
      case 1:
        return rules[0];
      default:
        return CompositePricingRules.builder().rules(ImmutableList.copyOf(rules)).build();
    }
  }

  /**
   * Returns a set of rules that return function configuration from this rule if available, otherwise returning
   * configuration from the other rule.
   *
   * @param otherRules  the delegate pricing rules
   * @return a set of rules that return function configuration from this rule if available, otherwise returning
   *   configuration from the other rule
   */
  public default PricingRules composedWith(PricingRules otherRules) {
    return of(this, otherRules);
  }

  /**
   * Returns the calculation policy specifying how a measure should be calculated for a target.
   *
   * @param target  a target
   * @param measure  a measure
   * @return a calculation policy specifying how a measure should be calculated for a target
   */
  public abstract Optional<EngineFunctionConfig> functionConfig(CalculationTarget target, Measure measure);
}
