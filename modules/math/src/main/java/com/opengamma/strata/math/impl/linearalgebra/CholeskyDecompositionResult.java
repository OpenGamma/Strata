/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * Contains the results of Cholesky matrix decomposition.
 */
public interface CholeskyDecompositionResult extends DecompositionResult {

  /**
   * Returns the $\mathbf{L}$ matrix of the decomposition.
   * <p>
   * $\mathbf{L}$ is a lower-triangular matrix.
   * @return the $\mathbf{L}$ matrix
   */
  DoubleMatrix2D getL();

  /**
   * Returns the transpose of the matrix $\mathbf{L}$ of the decomposition.
   * <p>
   * $\mathbf{L}^T$ is a upper-triangular matrix.
   * @return the $\mathbf{L}^T$ matrix
   */
  DoubleMatrix2D getLT();

  /**
   * Return the determinant of the matrix.
   * @return determinant of the matrix
   */
  double getDeterminant();

}
