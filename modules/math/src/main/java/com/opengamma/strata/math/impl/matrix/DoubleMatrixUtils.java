/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Various utility classes for matrices.
 */
public final class DoubleMatrixUtils {
  //TODO shouldn't these all be separate calculators implementing Function?
  private DoubleMatrixUtils() {
  }

  /**
   * The transpose of a matrix $\mathbf{A}$ with elements $A_{ij}$ is $A_{ji}$.
   * @param matrix The matrix to transpose, not null
   * @return The transposed matrix
   */
  public static DoubleMatrix2D getTranspose(DoubleMatrix2D matrix) {
    return DoubleMatrix2D.of(matrix.columnCount(), matrix.rowCount(), (i, j) -> matrix.get(j, i));
  }

  /**
   * The identity matrix is a matrix with diagonal elements equals to one and zero elsewhere.
   * @param dimension The dimension of matrix required, not negative or zero
   * @return The identity matrix
   */
  public static DoubleMatrix2D getIdentityMatrix2D(int dimension) {
    ArgChecker.isTrue(dimension >= 0, "dimension must be >= 0");
    if (dimension == 0) {
      return DoubleMatrix2D.EMPTY;
    }
    if (dimension == 1) {
      return DoubleMatrix2D.of(1, 1, 1d);
    }
    return DoubleMatrix2D.of(dimension, dimension, (i, j) -> (i == j) ? 1d : 0d);
  }

  /**
   * Converts a vector into a diagonal matrix.
   * @param vector The vector, not null
   * @return A diagonal matrix 
   */
  public static DoubleMatrix2D getTwoDimensionalDiagonalMatrix(DoubleMatrix1D vector) {
    ArgChecker.notNull(vector, "vector");
    int n = vector.size();
    if (n == 0) {
      return DoubleMatrix2D.EMPTY;
    }
    return DoubleMatrix2D.of(n, n, (i, j) -> (i == j) ? vector.get(i) : 0d);
  }

  /**
   * Converts a vector into a diagonal matrix.
   * @param vector The vector, not null
   * @return A diagonal matrix
   */
  public static DoubleMatrix2D getTwoDimensionalDiagonalMatrix(double[] vector) {
    ArgChecker.notNull(vector, "vector");
    return getTwoDimensionalDiagonalMatrix(DoubleMatrix1D.copyOf(vector));
  }
}
