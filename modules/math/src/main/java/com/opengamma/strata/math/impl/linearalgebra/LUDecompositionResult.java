/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Contains the results of LU matrix decomposition.
 */
public interface LUDecompositionResult extends DecompositionResult {

  /**
   * Returns the $\mathbf{L}$ matrix of the decomposition.
   * <p>
   * $\mathbf{L}$ is a lower-triangular matrix.
   * @return the $\mathbf{L}$ matrix
   */
  DoubleMatrix getL();

  /**
   * Returns the $\mathbf{U}$ matrix of the decomposition.
   * <p>
   * $\mathbf{U}$ is an upper-triangular matrix.
   * @return the U matrix
   */
  DoubleMatrix getU();

  /**
   * Returns the rows permutation matrix, $\mathbf{P}$.
   * <p>
   * P is a sparse matrix with exactly one element set to 1.0 in
   * each row and each column, all other elements being set to 0.0.
   * <p>
   * The positions of the 1 elements are given by the {@link #getPivot()
   * pivot permutation vector}.
   * @return the $\mathbf{P}$ rows permutation matrix
   * @see #getPivot()
   */
  DoubleMatrix getP();

  /**
   * Returns the pivot permutation vector.
   * @return the pivot permutation vector
   */
  int[] getPivot();

  /**
   * Return the determinant of the matrix.
   * @return determinant of the matrix
   */
  double getDeterminant();

}
