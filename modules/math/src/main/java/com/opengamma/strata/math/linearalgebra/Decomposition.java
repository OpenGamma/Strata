/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.linearalgebra;

import java.util.function.Function;

import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Base interface for matrix decompositions, such as SVD and LU.
 * 
 * @param <R> the type of the decomposition result
 */
public interface Decomposition<R extends DecompositionResult> extends Function<DoubleMatrix, R> {

  /**
   * Applies this function to the given argument.
   *
   * @param input  the input matrix
   * @return the resulting decomposition
   */
  @Override
  public abstract R apply(DoubleMatrix input);

}
