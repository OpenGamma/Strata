/*
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Contains the results of Cholesky matrix decomposition.
 */
// CSOFF: AbbreviationAsWordInName
public interface CholeskyDecompositionResult extends DecompositionResult {

  /**
   * Returns the $\mathbf{L}$ matrix of the decomposition.
   * <p>
   * $\mathbf{L}$ is a lower-triangular matrix.
   * @return the $\mathbf{L}$ matrix
   */
  DoubleMatrix getL();

  /**
   * Returns the transpose of the matrix $\mathbf{L}$ of the decomposition.
   * <p>
   * $\mathbf{L}^T$ is a upper-triangular matrix.
   * @return the $\mathbf{L}^T$ matrix
   */
  DoubleMatrix getLT();

  /**
   * Return the determinant of the matrix.
   * @return determinant of the matrix
   */
  double getDeterminant();

}
