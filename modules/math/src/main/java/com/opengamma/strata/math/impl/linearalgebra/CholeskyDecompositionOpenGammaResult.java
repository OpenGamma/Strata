/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * Results of the OpenGamma implementation of Cholesky decomposition.
 */
public class CholeskyDecompositionOpenGammaResult implements CholeskyDecompositionResult {

  private static final MatrixAlgebra ALGEBRA = new OGMatrixAlgebra();
  /**
   * The array that store the data.
   */
  private final double[][] _lArray;
  /**
   * The matrix L, result of the decomposition.
   */
  private final DoubleMatrix2D _l;
  /**
   * The matrix L^T, result of the decomposition.
   */
  private final DoubleMatrix2D _lT;
  /**
   * The determinant of the original matrix A = L L^T.
   */
  private double _determinant;

  /**
   * Constructor.
   * @param lArray The matrix L as an array of doubles.
   */
  public CholeskyDecompositionOpenGammaResult(final double[][] lArray) {
    _lArray = lArray;
    _l = new DoubleMatrix2D(_lArray);
    _lT = ALGEBRA.getTranspose(_l);
    _determinant = 1.0;
    for (int loopdiag = 0; loopdiag < _lArray.length; ++loopdiag) {
      _determinant *= _lArray[loopdiag][loopdiag] * _lArray[loopdiag][loopdiag];
    }
  }

  @Override
  public DoubleMatrix1D solve(DoubleMatrix1D b) {
    return new DoubleMatrix1D(b.getData());
  }

  @Override
  public double[] solve(double[] b) {
    int dim = b.length;
    ArgChecker.isTrue(dim == _lArray.length, "b array of incorrect size");
    final double[] x = new double[dim];
    System.arraycopy(b, 0, x, 0, dim);
    // L y = b (y stored in x array)
    for (int looprow = 0; looprow < dim; looprow++) {
      x[looprow] /= _lArray[looprow][looprow];
      for (int j = looprow + 1; j < dim; j++) {
        x[j] -= x[looprow] * _lArray[j][looprow];
      }
    }
    // L^T x = y
    for (int looprow = dim - 1; looprow >= -0; looprow--) {
      x[looprow] /= _lArray[looprow][looprow];
      for (int j = 0; j < looprow; j++) {
        x[j] -= x[looprow] * _lArray[looprow][j];
      }
    }
    return x;
  }

  @Override
  public DoubleMatrix2D solve(DoubleMatrix2D b) {
    int nbRow = b.getNumberOfRows();
    int nbCol = b.getNumberOfColumns();
    ArgChecker.isTrue(nbRow == _lArray.length, "b array of incorrect size");
    double[][] bArray = b.getData();
    final double[][] x = new double[nbRow][nbCol];
    for (int looprow = 0; looprow < nbRow; looprow++) {
      System.arraycopy(bArray[looprow], 0, x[looprow], 0, nbCol);
    }
    // L Y = B (Y stored in x array)
    for (int loopcol = 0; loopcol < nbCol; loopcol++) {
      for (int looprow = 0; looprow < nbRow; looprow++) {
        x[looprow][loopcol] /= _lArray[looprow][looprow];
        for (int j = looprow + 1; j < nbRow; j++) {
          x[j][loopcol] -= x[looprow][loopcol] * _lArray[j][looprow];
        }
      }
    }
    // L^T X = Y
    for (int loopcol = 0; loopcol < nbCol; loopcol++) {
      for (int looprow = nbRow - 1; looprow >= -0; looprow--) {
        x[looprow][loopcol] /= _lArray[looprow][looprow];
        for (int j = 0; j < looprow; j++) {
          x[j][loopcol] -= x[looprow][loopcol] * _lArray[looprow][j];
        }
      }
    }
    return new DoubleMatrix2D(x);
  }

  @Override
  public DoubleMatrix2D getL() {
    return _l;
  }

  @Override
  public DoubleMatrix2D getLT() {
    return _lT;
  }

  @Override
  public double getDeterminant() {
    return _determinant;
  }

}
