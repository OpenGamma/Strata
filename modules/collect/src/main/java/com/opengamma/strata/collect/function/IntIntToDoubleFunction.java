/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

/**
 * A function of two arguments - {@code int} and {@code int}.
 * <p>
 * This takes two arguments and returns a {@code double} result.
 */
@FunctionalInterface
public interface IntIntToDoubleFunction {

  /**
   * Performs an operation on the values.
   *
   * @param intValue1  the first argument
   * @param intValue2  the second argument
   * @return the result
   */
  double applyAsDouble(int intValue1, int intValue2);

}
