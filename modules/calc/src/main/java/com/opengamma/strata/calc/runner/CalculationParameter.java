/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.config.Measure;

/**
 * The base interface for calculation parameters.
 * <p>
 * Parameters are used to control the calculation.
 */
public interface CalculationParameter {

  /**
   * Gets the type that the parameter will be queried by.
   * <p>
   * Parameters can be queried using {@link CalculationParameters#findParameter(Class)}.
   * This type is the key that callers must use in that method.
   * 
   * @return the type of the parameter implementation
   */
  public abstract Class<?> queryType();

  /**
   * Checks if this parameter applies to the specified target and measure.
   * <p>
   * Parameters may apply to all targets and measures or just a subset.
   * The {@link CalculationParameters#filter(CalculationTarget, Measure)} method
   * uses this method to filter a complete set of parameters.
   * 
   * @param target  the calculation target, such as a trade
   * @param measure  the measure to be calculated
   * @return true if the parameter applies
   */
  public abstract boolean appliesTo(CalculationTarget target, Measure measure);

}
