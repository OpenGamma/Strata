/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import com.opengamma.strata.collect.ArgChecker;

/**
 * 1-D function implementation.
 * 
 * @param <S> the type of the arguments
 * @param <T> the return type of the function
 */
public abstract class Function1D<S, T> implements Function<S, T> {

  /**
   * Implementation of the interface.
   * This method only uses the first argument.
   * 
   * @param x  the list of inputs into the function, not null and no null elements
   * @return the value of the function
   */
  @SuppressWarnings("unchecked")
  @Override
  public T evaluate(S... x) {
    ArgChecker.noNulls(x, "parameter list");
    ArgChecker.isTrue(x.length == 1, "parameter list must have one element");
    return evaluate(x[0]);
  }

  /**
   * 1-D function method.
   * 
   * @param x  the argument of the function, not null
   * @return the value of the function
   */
  public abstract T evaluate(S x);

}
