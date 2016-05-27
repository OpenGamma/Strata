/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.calc.runner;

import java.util.Optional;

import com.opengamma.strata.basics.CalculationTarget;
import com.opengamma.strata.calc.CalculationRules;
import com.opengamma.strata.calc.Column;
import com.opengamma.strata.calc.Measure;
import com.opengamma.strata.calc.ReportingCurrency;

/**
 * The base interface for calculation parameters.
 * <p>
 * Parameters are used to control the calculation.
 * <p>
 * For example, {@link ReportingCurrency} is a parameter that controls currency conversion.
 * If specified, on a {@link Column}, or in {@link CalculationRules}, then the output will
 * be converted to the specified currency.
 * <p>
 * Applications may implement this interface to add new parameters to the system.
 * In order to be used, new implementations of {@link CalculationFunction} must be written
 * that receive the parameters and perform appropriate behavior.
 * <p>
 * Implementations of this interface must be immutable.
 */
public interface CalculationParameter {

  /**
   * Gets the type that the parameter will be queried by.
   * <p>
   * Parameters can be queried using {@link CalculationParameters#findParameter(Class)}.
   * This type is the key that callers must use in that method.
   * <p>
   * By default, this is just {@link Object#getClass()}.
   * It will only differ if the query type is an interface rather than the concrete class.
   * 
   * @return the type of the parameter implementation
   */
  public default Class<? extends CalculationParameter> queryType() {
    return getClass();
  }

  /**
   * Filters this parameter to the specified target and measure.
   * <p>
   * Parameters may apply to all targets and measures or just a subset.
   * The {@link CalculationParameters#filter(CalculationTarget, Measure)} method
   * uses this method to filter a complete set of parameters.
   * <p>
   * By default, this returns {@code Optional.of(this)}.
   * If the parameter does not apply to either the target or measure, then optional empty must be returned.
   * If desired, the result can be a different parameter, allowing one parameter to delegate
   * to another when filtered.
   * 
   * @param target  the calculation target, such as a trade
   * @param measure  the measure to be calculated
   * @return the parameter appropriate to the target and measure, empty if this parameter does not apply
   */
  public default Optional<CalculationParameter> filter(CalculationTarget target, Measure measure) {
    return Optional.of(this);
  }

}
