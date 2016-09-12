/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.differentiation;

import java.util.function.Function;

/**
 * Given a one-dimensional function (see {@link Function}), returns a function that calculates the gradient.
 * 
 * @param <S> the domain type of the function
 * @param <T> the range type of the function
 * @param <U> the range type of the differential
 */
public interface Differentiator<S, T, U> {

  /**
   * Provides a function that performs the differentiation.
   * 
   * @param function  a function for which to get the differential function
   * @return a function that calculates the differential
   */
  Function<S, U> differentiate(Function<S, T> function);

  /**
   * Provides a function that performs the differentiation.
   * 
   * @param function  a function for which to get the differential function
   * @param domain  a function that returns false if the requested value is not in  the domain, true otherwise
   * @return a function that calculates the differential
   */
  Function<S, U> differentiate(Function<S, T> function, Function<S, Boolean> domain);

}
