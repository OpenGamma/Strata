/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config.pricing;

import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.config.Measure;

/**
 * Pricing rules specify how a measure should be calculated for a target.
 * <p>
 * Pricing rules return a configured function group that provides function configuration for calculating a value.
 * The function group specifies which function implementation should be used for the calculation and also
 * provides any arguments for configuring the function.
 */
public interface PricingRules {

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
        return PricingRules.empty();
      case 1:
        return rules[0];
      default:
        return CompositePricingRules.builder().rules(ImmutableList.copyOf(rules)).build();
    }
  }

  /**
   * Returns an empty set of rules.
   *
   * @return an empty set of rules
   */
  public static PricingRules empty() {
    return EmptyPricingRules.INSTANCE;
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
   * Returns a function group specifying how a measure should be calculated for the target.
   *
   * @param target  a target
   * @param measure  a measure
   * @return a function group specifying how a measure should be calculated for the target
   */
  public abstract Optional<ConfiguredFunctionGroup> functionGroup(CalculationTarget target, Measure measure);

  /**
   * Returns the set of measures that are configured for a calculation target.
   * 
   * @param target  a target
   * @return a set of available measures for the target
   */
  public abstract ImmutableSet<Measure> configuredMeasures(CalculationTarget target);

}
