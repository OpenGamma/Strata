/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

/**
 * A predicate of three arguments - {@code int}, {@code int} and {@code double}.
 * <p>
 * This takes three arguments and returns a {@code boolean} result.
 */
@FunctionalInterface
public interface IntIntDoublePredicate {

  /**
   * Evaluates the predicate.
   *
   * @param intValue1  the first argument
   * @param intValue2  the second argument
   * @param doubleValue  the third argument
   * @return true if the arguments match the predicate
   */
  boolean test(int intValue1, int intValue2, double doubleValue);

}
