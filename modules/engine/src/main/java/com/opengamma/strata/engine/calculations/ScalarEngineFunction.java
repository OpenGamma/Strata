/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * <p>
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.engine.config.ReportingRules;
import com.opengamma.strata.engine.marketdata.SingleCalculationMarketData;

// TODO Set of Measures should be declared in an annotation
/**
 * A function used by the calculation engine to calculate a single value for a target given one set of market data.
 *
 *
 * @param <T>  the type of target handled by this function
 * @param <R>  the return type of this function
 */
public interface ScalarEngineFunction<T extends CalculationTarget, R> {

  /**
   * Returns requirements specifying the market data the function needs to perform its calculations.
   *
   * @param target  a target
   * @return requirements specifying the market data the function needs to perform its calculations for the target
   */
  public abstract CalculationRequirements requirements(T target);

  /**
   * Performs calculations for the specified input using multiple sets of market data.
   *
   * @param target  a target
   * @param marketData  the market data used in the calculation
   * @param reportingRules  rules defining how results should be reported
   * @return the result of the calculation for the target
   */
  public abstract R execute(T target, SingleCalculationMarketData marketData, ReportingRules reportingRules);

  // TODO Getter for the input type so the pricing context can verify the config is correct?
}
