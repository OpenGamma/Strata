/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import com.opengamma.strata.math.impl.function.Function1D;

/**
 * Interface for classes that extends the functionality of {@link Minimizer} by providing a method that takes a gradient function.
 * @param <F> The type of the function to minimize
 * @param <G> The type of the gradient function
 * @param <S> The type of the start position of the minimization
 */
public interface MinimizerWithGradient<F extends Function1D<S, ?>, G extends Function1D<S, ?>, S> extends Minimizer<F, S> {

  /**
   * @param function The function to minimize, not null
   * @param gradient The gradient function, not null
   * @param startPosition The start position, not null
   * @return The minimum
   */
  S minimize(F function, G gradient, S startPosition);

}
