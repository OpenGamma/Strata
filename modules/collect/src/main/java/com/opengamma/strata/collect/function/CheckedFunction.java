/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import com.opengamma.strata.collect.Unchecked;

/**
 * A checked version of {@code Function}.
 * <p>
 * This is intended to be used with {@link Unchecked}.
 *
 * @param <T> the type of the object parameter
 * @param <R> the type of the result
 */
@FunctionalInterface
public interface CheckedFunction<T, R> {

  /**
   * Applies this function to the given argument.
   *
   * @param t  the function argument
   * @return the function result
   * @throws Throwable if an error occurs
   */
  public R apply(T t) throws Throwable;

}
