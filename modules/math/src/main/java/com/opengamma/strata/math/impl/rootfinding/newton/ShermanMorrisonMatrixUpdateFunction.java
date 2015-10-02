/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.function.Function1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;

/**
 * 
 */
public class ShermanMorrisonMatrixUpdateFunction implements NewtonRootFinderMatrixUpdateFunction {
  private final MatrixAlgebra _algebra;

  public ShermanMorrisonMatrixUpdateFunction(final MatrixAlgebra algebra) {
    ArgChecker.notNull(algebra, "algebra");
    _algebra = algebra;
  }

  @Override
  public DoubleMatrix2D getUpdatedMatrix(final Function1D<DoubleMatrix1D, DoubleMatrix2D> g, DoubleMatrix1D x, final DoubleMatrix1D deltaX, final DoubleMatrix1D deltaY, final DoubleMatrix2D matrix) {
    ArgChecker.notNull(deltaX, "deltaX");
    ArgChecker.notNull(deltaY, "deltaY");
    ArgChecker.notNull(matrix, "matrix");
    DoubleMatrix1D v1 = (DoubleMatrix1D) _algebra.multiply(deltaX, matrix);
    final double length = _algebra.getInnerProduct(v1, deltaY);
    if (length == 0) {
      return matrix;
    }
    v1 = (DoubleMatrix1D) _algebra.scale(v1, 1. / length);
    final DoubleMatrix1D v2 = (DoubleMatrix1D) _algebra.subtract(deltaX, _algebra.multiply(matrix, deltaY));
    final DoubleMatrix2D m = _algebra.getOuterProduct(v2, v1);
    return (DoubleMatrix2D) _algebra.add(matrix, m);
  }

}
