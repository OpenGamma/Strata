/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;

/**
 * 
 */
public class TridiagonalSolver {

  /**
   * Solves the system Ax = y for the unknown vector x, where A is a tridiagonal matrix and y is a vector.
   * This takes order n operations where n is the size of the system
   * (number of linear equations), as opposed to order n^3 for the general problem.
   * @param aM tridiagonal matrix
   * @param b known vector (must be same length as rows/columns of matrix)
   * @return vector (as an array of doubles) with same length as y
   */
  public static double[] solvTriDag(TridiagonalMatrix aM, double[] b) {

    ArgChecker.notNull(aM, "null matrix");
    ArgChecker.notNull(b, "null vector");
    double[] d = aM.getDiagonal(); //b is modified, so get copy of diagonal
    int n = d.length;
    ArgChecker.isTrue(n == b.length, "vector y wrong length for matrix");
    double[] y = Arrays.copyOf(b, n);

    double[] l = aM.getLowerSubDiagonalData();
    double[] u = aM.getUpperSubDiagonalData();

    double[] x = new double[n];
    for (int i = 1; i < n; i++) {
      double m = l[i - 1] / d[i - 1];
      d[i] = d[i] - m * u[i - 1];
      y[i] = y[i] - m * y[i - 1];
    }

    x[n - 1] = y[n - 1] / d[n - 1];

    for (int i = n - 2; i >= 0; i--) {
      x[i] = (y[i] - u[i] * x[i + 1]) / d[i];
    }

    return x;
  }

  /**
   * Solves the system Ax = y for the unknown vector x, where A is a tridiagonal matrix and y is a vector.
   * This takes order n operations where n is the size of the system
   * (number of linear equations), as opposed to order n^3 for the general problem.
   * @param aM tridiagonal matrix
   * @param b known vector (must be same length as rows/columns of matrix)
   * @return vector with same length as y
   */
  public static DoubleArray solvTriDag(TridiagonalMatrix aM, DoubleArray b) {
    return DoubleArray.copyOf(solvTriDag(aM, b.toArray()));
  }

}
