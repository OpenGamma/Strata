/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.config.pricing;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.config.Measure;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Mutable builder for building instances of {@link PricingRule}.
 * 
 * @param <T>  the type of the calculation target
 */
public final class PricingRuleBuilder<T extends CalculationTarget> {

  /** The target type to which the pricing rule applies. */
  private final Class<T> targetType;

  /** The measures the rule applies to. An empty set means the rule applies to all measures. */
  private final Set<Measure> measures = new HashSet<>();

  /** The function group used for calculations matching the rule. */
  private FunctionGroup<T> functionGroup;

  /** The arguments used by the function group when creating functions. */
  private final Map<String, Object> arguments = new HashMap<>();

  // package-private constructor used by PricingRule.builder()
  PricingRuleBuilder(Class<T> targetType) {
    this.targetType = ArgChecker.notNull(targetType, "targetType");
  }

  /**
   * Sets the function group that performs the calculations matching the rule.
   *
   * @param functionGroup  the function group used for calculations matching the rule
   * @return this builder
   */
  public PricingRuleBuilder<T> functionGroup(FunctionGroup<T> functionGroup) {
    this.functionGroup = ArgChecker.notNull(functionGroup, "functionGroup");
    return this;
  }

  /**
   * Adds measures to the pricing rule.
   *
   * @param measures  measures handled by the pricing rule
   * @return this builder
   */
  public PricingRuleBuilder<T> addMeasures(Measure... measures) {
    ArgChecker.noNulls(measures, "measures");
    this.measures.addAll(Arrays.asList(measures));
    return this;
  }

  /**
   * Adds a constructor argument for creating function instances to perform calculations.
   *
   * @param name  the parameter name
   * @param value  the argument value
   * @return this builder
   */
  public PricingRuleBuilder<T> addArgument(String name, Object value) {
    ArgChecker.notNull(name, "name");
    ArgChecker.notNull(value, "value");
    arguments.put(name, value);
    return this;
  }

  /**
   * Returns a pricing rule built from the data in this builder.
   *
   * @return a pricing rule built from the data in this builder
   */
  public PricingRule<T> build() {
    return new PricingRule<>(targetType, measures, functionGroup, arguments);
  }
}
