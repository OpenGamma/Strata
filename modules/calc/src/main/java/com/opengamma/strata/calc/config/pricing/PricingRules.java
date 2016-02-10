/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config.pricing;

import java.util.List;
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
   * Obtains an instance based on the specified individual rules.
   *
   * @param rules  the individual rules
   * @return the combined rules
   */
  public static PricingRules of(PricingRule<?>... rules) {
    return DefaultPricingRules.of(ImmutableList.copyOf(rules));
  }

  /**
   * Obtains an instance based on the specified individual rules.
   *
   * @param rules  the individual rules
   * @return the combined rules
   */
  public static PricingRules of(List<PricingRule<?>> rules) {
    return DefaultPricingRules.of(rules);
  }

  /**
   * Returns an empty set of rules.
   *
   * @return an empty set of rules
   */
  public static PricingRules empty() {
    return EmptyPricingRules.INSTANCE;
  }

  //-------------------------------------------------------------------------
  /**
   * Returns a function group specifying how a measure should be calculated for the target.
   *
   * @param target  the target
   * @param measure  the measure
   * @return a function group specifying how a measure should be calculated for the target
   */
  public abstract Optional<ConfiguredFunctionGroup> functionGroup(CalculationTarget target, Measure measure);

  /**
   * Returns the set of measures that are configured for a calculation target.
   * 
   * @param target  the target
   * @return a set of available measures for the target
   */
  public abstract ImmutableSet<Measure> configuredMeasures(CalculationTarget target);

  /**
   * Combines these rules with the specified rules.
   * <p>
   * The resulting rules will return mappings from this rule if available,
   * otherwise mappings will be returned from the other rule.
   *
   * @param otherRules  the other rules
   * @return the combined rules
   */
  public default PricingRules composedWith(PricingRules otherRules) {
    return CompositePricingRules.of(this, otherRules);
  }

}
