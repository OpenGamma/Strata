/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import com.opengamma.strata.collect.Unchecked;

/**
 * A checked version of {@code BiPredicate}.
 * <p>
 * This is intended to be used with {@link Unchecked}.
 *
 * @param <T> the type of the first object parameter
 * @param <U> the type of the second object parameter
 */
@FunctionalInterface
public interface CheckedBiPredicate<T, U> {

  /**
   * Evaluates this predicate on the given arguments.
   *
   * @param t  the first input argument
   * @param u  the second input argument
   * @return true if the input arguments match the predicate
   * @throws Throwable if an error occurs
   */
  public boolean test(T t, U u) throws Throwable;

}
