/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import com.opengamma.strata.collect.array.DoubleMatrix;

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
  public static DoubleMatrix getTranspose(DoubleMatrix matrix) {
    return DoubleMatrix.of(matrix.columnCount(), matrix.rowCount(), (i, j) -> matrix.get(j, i));
  }

}
