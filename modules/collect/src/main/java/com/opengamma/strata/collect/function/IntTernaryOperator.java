/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

/**
 * A function of three arguments that returns a value.
 * <p>
 * All the inputs and outputs are of type {@code int}.
 */
@FunctionalInterface
public interface IntTernaryOperator {

  /**
   * Applies the function.
   *
   * @param a  the first argument
   * @param b  the second argument
   * @param c  the third argument
   * @return the result of the function
   */
  int applyAsInt(int a, int b, int c);

}
