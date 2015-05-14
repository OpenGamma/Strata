/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.calculations.function;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.engine.marketdata.CalculationRequirements;

/**
 * Supertype of all functions that calculate values of measures for a target.
 * <p>
 * All functions must be able to specify the data they require to perform their calculations.
 * <p>
 * More specific function types can calculate values for one measure or multiple measures, and
 * may rely on the engine to convert values into the reporting currency or handle it themselves.
 *
 * @param <T>  the type of target handled by this function
 */
public interface CalculationFunction<T extends CalculationTarget> {

  /**
   * Returns requirements specifying the market data the function needs to perform its calculations.
   *
   * @param target  a target
   * @return requirements specifying the market data the function needs to perform its calculations for the target
   */
  public abstract CalculationRequirements requirements(T target);
}
