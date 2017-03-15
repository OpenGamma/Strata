/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import com.opengamma.strata.collect.Unchecked;

/**
 * A checked version of {@code BiFunction}.
 * <p>
 * This is intended to be used with {@link Unchecked}.
 *
 * @param <T> the type of the first object parameter
 * @param <U> the type of the second object parameter
 * @param <R> the type of the result
 */
@FunctionalInterface
public interface CheckedBiFunction<T, U, R> {

  /**
   * Applies this function to the given arguments.
   *
   * @param t  the first function argument
   * @param u  the second function argument
   * @return the function result
   * @throws Throwable if an error occurs
   */
  public R apply(T t, U u) throws Throwable;

}
