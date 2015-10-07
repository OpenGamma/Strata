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

  public static void notNaNOrInfinite(DoubleMatrix2D x) {
    int rows = x.getNumberOfRows();
    int cols = x.getNumberOfColumns();
    for (int i = 0; i < rows; i++) {
      for (int j = 0; j < cols; j++) {
        double temp = x.getEntry(i, j);
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
