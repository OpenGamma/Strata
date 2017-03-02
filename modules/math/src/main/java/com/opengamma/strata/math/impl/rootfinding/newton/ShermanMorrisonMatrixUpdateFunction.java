/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import java.util.function.Function;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;

/**
 * 
 */
public class ShermanMorrisonMatrixUpdateFunction implements NewtonRootFinderMatrixUpdateFunction {

  private final MatrixAlgebra _algebra;

  public ShermanMorrisonMatrixUpdateFunction(MatrixAlgebra algebra) {
    ArgChecker.notNull(algebra, "algebra");
    _algebra = algebra;
  }

  @Override
  public DoubleMatrix getUpdatedMatrix(
      Function<DoubleArray, DoubleMatrix> g,
      DoubleArray x,
      DoubleArray deltaX,
      DoubleArray deltaY,
      DoubleMatrix matrix) {

    ArgChecker.notNull(deltaX, "deltaX");
    ArgChecker.notNull(deltaY, "deltaY");
    ArgChecker.notNull(matrix, "matrix");
    DoubleArray v1 = (DoubleArray) _algebra.multiply(deltaX, matrix);
    double length = _algebra.getInnerProduct(v1, deltaY);
    if (length == 0) {
      return matrix;
    }
    v1 = (DoubleArray) _algebra.scale(v1, 1. / length);
    DoubleArray v2 = (DoubleArray) _algebra.subtract(deltaX, _algebra.multiply(matrix, deltaY));
    DoubleMatrix m = _algebra.getOuterProduct(v2, v1);
    return (DoubleMatrix) _algebra.add(matrix, m);
  }

}
