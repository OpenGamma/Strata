/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

/**
 * A function of two arguments - {@code int} and {@code double}.
 * <p>
 * This takes two arguments and returns a {@code double} result.
 */
@FunctionalInterface
public interface IntDoubleToDoubleFunction {

  /**
   * Performs an operation on the values.
   *
   * @param intValue  the first argument
   * @param doubleValue  the second argument
   * @return the result
   */
  double applyAsDouble(int intValue, double doubleValue);

}
