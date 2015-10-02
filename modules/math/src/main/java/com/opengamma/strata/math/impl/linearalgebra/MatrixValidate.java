/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import com.opengamma.strata.math.impl.MathException;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * 
 */
public abstract class MatrixValidate {

  public static void notNaNOrInfinite(final DoubleMatrix2D x) {
    final int rows = x.getNumberOfRows();
    final int cols = x.getNumberOfColumns();
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        final double temp = x.getEntry(i, j);
        if (Double.isNaN(temp)) {
          throw new MathException("Matrix contains a NaN");
        }
        if (Double.isInfinite(temp)) {
          throw new MathException("Matrix contains an infinite");
        }
      }
    }
  }
}
