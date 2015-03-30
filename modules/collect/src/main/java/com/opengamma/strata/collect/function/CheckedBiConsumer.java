/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import com.opengamma.strata.collect.Unchecked;

/**
 * A checked version of {@code BiConsumer}.
 * <p>
 * This is intended to be used with {@link Unchecked}.
 *
 * @param <T> the type of the first object parameter
 * @param <U> the type of the second object parameter
 */
@FunctionalInterface
public interface CheckedBiConsumer<T, U> {

  /**
   * Performs this operation on the given arguments.
   *
   * @param t the first input argument
   * @param u the second input argument
   * @throws Throwable if an error occurs
   */
  public void accept(T t, U u) throws Throwable;

}
