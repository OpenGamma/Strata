/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.linearalgebra;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Contains the results of matrix decomposition.
 * <p>
 * The decomposed matrices (such as the L and U matrices for LU decomposition) are stored in this class.
 * There are methods that allow calculations to be performed using these matrices.
 */
public interface DecompositionResult {

  /**
   * Solves $\mathbf{A}x = b$ where $\mathbf{A}$ is a (decomposed) matrix and $b$ is a vector.
   * 
   * @param input  the vector to calculate with
   * @return the vector x
   */
  public abstract DoubleArray solve(DoubleArray input);

  /**
   * Solves $\mathbf{A}x = b$ where $\mathbf{A}$ is a (decomposed) matrix and $b$ is a vector.
   * 
   * @param input  the vector to calculate with
   * @return the vector x 
   */
  public abstract double[] solve(double[] input);

  /**
   * Solves $\mathbf{A}x = \mathbf{B}$ where $\mathbf{A}$ is a (decomposed) matrix and $\mathbf{B}$ is a matrix.
   * 
   * @param input  the matrix to calculate with
   * @return the matrix x
   */
  public abstract DoubleMatrix solve(DoubleMatrix input);

}
