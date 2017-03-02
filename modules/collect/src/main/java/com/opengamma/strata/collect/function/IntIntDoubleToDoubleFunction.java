/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

/**
 * A function of three arguments - {@code int}, {@code int} and {@code double}.
 * <p>
 * This takes three arguments and returns a {@code double} result.
 */
@FunctionalInterface
public interface IntIntDoubleToDoubleFunction {

  /**
   * Performs an operation on the values.
   *
   * @param intValue1  the first argument
   * @param intValue2  the second argument
   * @param doubleValue  the third argument
   * @return the result
   */
  double applyAsDouble(int intValue1, int intValue2, double doubleValue);

}
