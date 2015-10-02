/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import java.util.List;

/**
 * Interface for anything the provides a vector function which depends on some extraneous data
 * @param <T> type of extraneous data 
 * @see VectorFunction
 */
public interface VectorFunctionProvider<T> {

  /**
   * Produce a vector function that maps from some 'model' parameters to values at the sample points 
   * @param samplePoints List of sample points 
   * @return a {@link VectorFunction}
   */
  VectorFunction from(final List<T> samplePoints);

  /**
   * Produce a vector function that maps from some 'model' parameters to values at the sample points 
   * @param samplePoints Array of sample points
   * @return a {@link VectorFunction}
   */
  VectorFunction from(final T[] samplePoints);

}
