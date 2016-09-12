/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import java.util.List;

/**
 * Interface for anything the provides a vector function which depends on some extraneous data.
 * 
 * @param <T> the type of extraneous data 
 * @see VectorFunction
 */
public interface VectorFunctionProvider<T> {

  /**
   * Produces a vector function that maps from some 'model' parameters to values at the sample points.
   * 
   * @param samplePoints  the list of sample points 
   * @return a {@link VectorFunction}
   */
  VectorFunction from(List<T> samplePoints);

  /**
   * Produces a vector function that maps from some 'model' parameters to values at the sample points.
   * 
   * @param samplePoints the array of sample points
   * @return a {@link VectorFunction}
   */
  VectorFunction from(T[] samplePoints);

}
