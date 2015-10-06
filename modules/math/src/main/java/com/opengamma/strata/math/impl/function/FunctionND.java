/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import com.opengamma.strata.collect.ArgChecker;

/**
 * N-D function implementation, where N is specified in the constructor.
 * 
 * @param <S> the type of the arguments
 * @param <T> the return type of the function
 */
public abstract class FunctionND<S, T> implements Function<S, T> {

  /**
   * Implementation of the interface.
   * 
   * @param x  the list of inputs into the function, not null
   * @return the value of the function
   * @throws IllegalArgumentException if the number of arguments is not equal to the dimension
   */
  @SuppressWarnings("unchecked")
  @Override
  public T evaluate(S... x) {
    ArgChecker.noNulls(x, "x");
    return evaluateFunction(x);
  }

  protected abstract T evaluateFunction(S[] x);

}
