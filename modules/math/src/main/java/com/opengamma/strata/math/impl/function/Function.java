/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

/**
 *
 * Interface for function definition. The function arguments can be
 * multi-dimensional (but not multi-type), as can the function value. The return
 * type of the function is not necessarily the same as that of the inputs.
 *
 * @param <S> the type of the arguments
 * @param <T> the return type of the function
 */
public interface Function<S, T> {

  /**
   * Evaluates the function.
   *
   * @param x  the list of inputs into the function, not null and no null elements
   * @return the value of the function
   */
  @SuppressWarnings("unchecked")
  T evaluate(S... x);
}
