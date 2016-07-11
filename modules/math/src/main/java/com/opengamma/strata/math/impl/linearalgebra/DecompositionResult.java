/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Contains the results of matrix decomposition. The decomposed matrices (e.g. the L and U matrices for LU decomposition) are stored in this class.
 * There are methods that allow calculations to be performed using these matrices.
 */
public interface DecompositionResult {

  /**
   * Solves $\mathbf{A}x = b$ where $\mathbf{A}$ is a (decomposed) matrix and $b$ is a vector. 
   * @param b a vector, not null
   * @return the vector x
   */
  DoubleArray solve(DoubleArray b);

  /**
   * Solves $\mathbf{A}x = b$ where $\mathbf{A}$ is a (decomposed) matrix and $b$ is a vector. 
   * @param b vector, not null
   * @return the vector x 
   */
  double[] solve(double[] b);

  /**
   * Solves $\mathbf{A}x = \mathbf{B}$ where $\mathbf{A}$ is a (decomposed) matrix and $\mathbf{B}$ is a matrix.
   * @param b matrix, not null
   * @return the matrix x
   */
  DoubleMatrix solve(DoubleMatrix b);

}
