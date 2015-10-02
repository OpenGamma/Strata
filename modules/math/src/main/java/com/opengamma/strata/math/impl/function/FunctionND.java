/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import com.opengamma.strata.collect.ArgChecker;

/**
 * N-D function implementation, where N is specified in the constructor.
 * @param <S> Type of the arguments
 * @param <T> Return type of the function
 */
public abstract class FunctionND<S, T> implements Function<S, T> {

  /**
   * Implementation of the interface.
   * @param x The list of inputs into the function, not null
   * @return The value of the function
   * @throws IllegalArgumentException If the number of arguments is not equal to the dimension
   */
  @SuppressWarnings("unchecked")
  @Override
  public T evaluate(final S... x) {
    ArgChecker.noNulls(x, "x");
    return evaluateFunction(x);
  }

  protected abstract T evaluateFunction(S[] x);

}
