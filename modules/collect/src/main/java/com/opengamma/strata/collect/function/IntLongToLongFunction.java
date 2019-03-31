/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

/**
 * A function of two arguments - {@code int} and {@code long}.
 * <p>
 * This takes two arguments and returns a {@code long} result.
 */
@FunctionalInterface
public interface IntLongToLongFunction {

  /**
   * Performs an operation on the values.
   *
   * @param intValue  the first argument
   * @param longValue  the second argument
   * @return the result
   */
  public abstract long applyAsLong(int intValue, long longValue);

}
