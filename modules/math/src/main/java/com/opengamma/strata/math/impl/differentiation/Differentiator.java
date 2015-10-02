/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.differentiation;

import com.opengamma.strata.math.impl.function.Function1D;

/**
 * Given a one-dimensional function (see {@link Function1D}), returns a function that calculates the gradient.
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
  Function1D<S, U> differentiate(Function1D<S, T> function);

  /**
   * Provides a function that performs the differentiation.
   * 
   * @param function  a function for which to get the differential function
   * @param domain  a function that returns false if the requested value is not in  the domain, true otherwise
   * @return a function that calculates the differential
   */
  Function1D<S, U> differentiate(Function1D<S, T> function, Function1D<S, Boolean> domain);

}
