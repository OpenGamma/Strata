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
  public static DoubleMatrix2D getTranspose(final DoubleMatrix2D matrix) {
    final int rows = matrix.getNumberOfRows();
    final int columns = matrix.getNumberOfColumns();
    final double[][] primitives = new double[columns][rows];
    for (int i = 0; i < columns; i++) {
      for (int j = 0; j < rows; j++) {
        primitives[i][j] = matrix.getEntry(j, i);
      }
    }
    return new DoubleMatrix2D(primitives);
  }

  /**
   * The identity matrix is a matrix with diagonal elements equals to one and zero elsewhere.
   * @param dimension The dimension of matrix required, not negative or zero
   * @return The identity matrix
   */
  public static DoubleMatrix2D getIdentityMatrix2D(final int dimension) {
    ArgChecker.isTrue(dimension >= 0, "dimension must be >= 0");
    if (dimension == 0) {
      return DoubleMatrix2D.EMPTY_MATRIX;
    }
    if (dimension == 1) {
      return new DoubleMatrix2D(new double[][] {new double[] {1 } });
    }
    final double[][] data = new double[dimension][dimension];
    for (int i = 0; i < dimension; i++) {
      data[i][i] = 1;
    }
    return new DoubleMatrix2D(data);
  }

  /**
   * Converts a vector into a diagonal matrix.
   * @param vector The vector, not null
   * @return A diagonal matrix 
   */
  public static DoubleMatrix2D getTwoDimensionalDiagonalMatrix(final DoubleMatrix1D vector) {
    ArgChecker.notNull(vector, "vector");
    final int n = vector.getNumberOfElements();
    if (n == 0) {
      return DoubleMatrix2D.EMPTY_MATRIX;
    }
    final double[][] data = new double[n][n];
    for (int i = 0; i < n; i++) {
      data[i][i] = vector.getEntry(i);
    }
    return new DoubleMatrix2D(data);
  }

  /**
   * Converts a vector into a diagonal matrix.
   * @param vector The vector, not null
   * @return A diagonal matrix
   */
  public static DoubleMatrix2D getTwoDimensionalDiagonalMatrix(final double[] vector) {
    ArgChecker.notNull(vector, "vector");
    return getTwoDimensionalDiagonalMatrix(new DoubleMatrix1D(vector));
  }
}
