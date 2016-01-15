/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import java.util.function.BiFunction;

/**
 * A function of two arguments - one {@code int} and one object.
 * <p>
 * This takes two arguments and returns an object result.
 *
 * @param <T> the type of the object parameter
 * @param <R> the type of the result
 * @see BiFunction
 */
@FunctionalInterface
public interface IntObjFunction<T, R> {

  /**
   * Applies the function.
   *
   * @param intValue  the first argument
   * @param objectValue  the second argument
   * @return the result of the function
   */
  R apply(int intValue, T objectValue);

}
