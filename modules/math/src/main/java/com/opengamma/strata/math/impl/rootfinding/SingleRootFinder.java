/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding;

import java.util.function.Function;

/**
 * Interface for classes that attempt to find a root for a one-dimensional function
 * (see {@link Function}) $f(x)$ bounded by user-supplied values,
 * $x_1$ and $x_2$. If there is not a single root between these  bounds, an exception is thrown.
 * 
 * @param <S> The input type of the function
 * @param <T> The output type of the function
 */
public interface SingleRootFinder<S, T> {

  /**
   * Finds the root.
   * 
   * @param function the function, not null
   * @param roots  the roots, not null
   * @return a root lying between x1 and x2
   */
  @SuppressWarnings("unchecked")
  S getRoot(Function<S, T> function, S... roots);

}
