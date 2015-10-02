/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import com.opengamma.strata.collect.ArgChecker;

/**
 * 2-D function implementation.
 * @param <S> Type of the arguments
 * @param <T> Return type of function
 */
public abstract class Function2D<S, T> implements Function<S, T> {

  /**
   * Implementation of the interface. This method only uses the first and second arguments.
   * @param x The list of inputs into the function, not null and no null elements
   * @return The value of the function
   */
  @SuppressWarnings("unchecked")
  @Override
  public T evaluate(final S... x) {
    ArgChecker.noNulls(x, "parameter list");
    ArgChecker.isTrue(x.length == 2, "parameter list must be of length 2");
    return evaluate(x[0], x[1]);
  }

  /**
   * 2-D function method
   * @param x1 The first argument of the function, not null
   * @param x2 The second argument of the function, not null
   * @return The value of the function
   */
  public abstract T evaluate(S x1, S x2);
}
