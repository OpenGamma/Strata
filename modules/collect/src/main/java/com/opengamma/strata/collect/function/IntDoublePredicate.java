/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

/**
 * A predicate of two arguments - {@code int} and {@code double}.
 * <p>
 * This takes two arguments and returns a {@code boolean} result.
 */
@FunctionalInterface
public interface IntDoublePredicate {

  /**
   * Evaluates the predicate.
   *
   * @param intValue  the first argument
   * @param doubleValue  the second argument
   * @return true if the arguments match the predicate
   */
  boolean test(int intValue, double doubleValue);

}
