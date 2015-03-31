/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.config;

import java.util.HashMap;
import java.util.Map;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.engine.calculations.VectorEngineFunction;

/**
 * Allows a {@link SimplePricingRules} to be built by adding one mapping at a time.
 * <p>
 * Instances are created with a call to {@link SimplePricingRules#builder()}.
 */
public class SimplePricingRulesBuilder {

  /** Configuration for engine functions, keyed by the target type and measure. */
  private final Map<MeasureKey, EngineFunctionConfig> calculationConfig = new HashMap<>();

  SimplePricingRulesBuilder() {
  }

  /**
   * Adds a mapping that defines what type of function should be used to calculate a measure for a
   * particular type of target.
   *
   * @param targetType the type of the target
   * @param measure the measure that the calculation outputs
   * @param functionType the type of function that should be used to calculate the measure
   * @param <T> the type of the target
   * @return this builder
   */
  public <T extends CalculationTarget> SimplePricingRulesBuilder addCalculation(
      Measure measure,
      Class<T> targetType,
      Class<? extends VectorEngineFunction<T, ?>> functionType) {

    EngineFunctionConfig engineFunctionConfig = EngineFunctionConfig.builder().functionType(functionType).build();
    calculationConfig.put(MeasureKey.of(targetType, measure), engineFunctionConfig);
    return this;
  }

  /**
   * Returns a set of ricing rules built from the data in this builder.
   *
   * @return pricing rules built from the data in this builder
   */
  public SimplePricingRules build() {
    return new SimplePricingRules(calculationConfig);
  }
}
