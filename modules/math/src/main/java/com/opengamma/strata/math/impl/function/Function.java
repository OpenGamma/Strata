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
 * @param <S> Type of the arguments
 * @param <T> Return type of the function
 */
public interface Function<S, T> {

  /**
   *
   * @param x The list of inputs into the function, not null and no null elements
   * @return The value of the function
   */
  @SuppressWarnings("unchecked")
  T evaluate(S... x);
}
