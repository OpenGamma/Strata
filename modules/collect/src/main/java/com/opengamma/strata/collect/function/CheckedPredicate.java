/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import com.opengamma.strata.collect.Unchecked;

/**
 * A checked version of {@code Predicate}.
 * <p>
 * This is intended to be used with {@link Unchecked}.
 *
 * @param <T> the type of the object parameter
 */
@FunctionalInterface
public interface CheckedPredicate<T> {

  /**
   * Evaluates this predicate on the given argument.
   *
   * @param t  the input argument
   * @return true if the input argument matches the predicate
   * @throws Throwable if an error occurs
   */
  public boolean test(T t) throws Throwable;

}
