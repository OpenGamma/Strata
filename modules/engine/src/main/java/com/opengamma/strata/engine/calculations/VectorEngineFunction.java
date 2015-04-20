/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.marketdata.CalculationMarketData;

// TODO Set of Measures should be declared in an annotation
/**
 * A function used by the calculation engine to performs calculation for a target given multiple sets of market data.
 *
 * @param <T>  the type of target handled by this function
 * @param <R>  the return type of this function
 */
public interface VectorEngineFunction<T extends CalculationTarget, R> {

  /**
   * Returns requirements specifying the market data the function needs to perform its calculations.
   *
   * @param target  a target
   * @return requirements specifying the market data the function needs to perform its calculations for the target
   */
  public abstract CalculationRequirements requirements(T target);

  /**
   * Performs calculations for the specified target using multiple sets of market data.
   *
   * @param target  the target of the calculation
   * @param marketData  the market data used in the calculation
   * @param reportingRules  the currency in which monetary values should be reported
   * @return the result of the calculation
   */
  public abstract R execute(T target, CalculationMarketData marketData, ReportingRules reportingRules);

  // TODO Getter for the target type so the engine can confirm the types are compatible?
}
