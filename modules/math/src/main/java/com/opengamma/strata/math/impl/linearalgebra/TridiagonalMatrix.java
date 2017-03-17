/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import java.util.Arrays;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.array.Matrix;

/**
 * Class representing a tridiagonal matrix.
 * $$
 * \begin{align*}
 * \begin{pmatrix}
 * a_1     & b_1     & 0       & \cdots  & 0       & 0       & 0        \\
 * c_1     & a_2     & b_2     & \cdots  & 0       & 0       & 0        \\
 * 0       &         & \ddots  &         & \vdots  & \vdots  & \vdots   \\
 * 0       & 0       & 0       &         & c_{n-2} & a_{n-1} & b_{n-1}  \\
 * 0       & 0       & 0       & \cdots  & 0       & c_{n-1} & a_n     
 * \end{pmatrix}
 * \end{align*}
 * $$
 */
public class TridiagonalMatrix implements Matrix {

  private final double[] _a;
  private final double[] _b;
  private final double[] _c;
  private DoubleMatrix _matrix;

  /**
   * @param a An array containing the diagonal values of the matrix, not null
   * @param b An array containing the upper sub-diagonal values of the matrix, not null.
   *   Its length must be one less than the length of the diagonal array
   * @param c An array containing the lower sub-diagonal values of the matrix, not null.
   *   Its length must be one less than the length of the diagonal array
   */
  public TridiagonalMatrix(double[] a, double[] b, double[] c) {
    ArgChecker.notNull(a, "a");
    ArgChecker.notNull(b, "b");
    ArgChecker.notNull(c, "c");
    int n = a.length;
    ArgChecker.isTrue(b.length == n - 1, "Length of subdiagonal b is incorrect");
    ArgChecker.isTrue(c.length == n - 1, "Length of subdiagonal c is incorrect");
    _a = a;
    _b = b;
    _c = c;
  }

  /**
   * Direct access to Diagonal Data.
   * @return An array of the values of the diagonal
   */
  public double[] getDiagonalData() {
    return _a;
  }

  /**
   * @return An array of the values of the diagonal
   */
  public double[] getDiagonal() {
    return Arrays.copyOf(_a, _a.length);
  }

  /**
   * Direct access to upper sub-Diagonal Data.
   * @return An array of the values of the upper sub-diagonal
   */
  public double[] getUpperSubDiagonalData() {
    return _b;
  }

  /**
   * @return An array of the values of the upper sub-diagonal
   */
  public double[] getUpperSubDiagonal() {
    return Arrays.copyOf(_b, _b.length);
  }

  /**
   * Direct access to lower sub-Diagonal Data.
   * @return An array of the values of the lower sub-diagonal
   */
  public double[] getLowerSubDiagonalData() {
    return _c;
  }

  /**
   * @return An array of the values of the lower sub-diagonal
   */
  public double[] getLowerSubDiagonal() {
    return Arrays.copyOf(_c, _c.length);
  }

  /**
   * @return Returns the tridiagonal matrix as a {@link DoubleMatrix}
   */
  public DoubleMatrix toDoubleMatrix() {
    if (_matrix == null) {
      calMatrix();
    }
    return _matrix;
  }

  private void calMatrix() {
    int n = _a.length;
    double[][] data = new double[n][n];
    for (int i = 0; i < n; i++) {
      data[i][i] = _a[i];
    }
    for (int i = 1; i < n; i++) {
      data[i - 1][i] = _b[i - 1];
    }
    for (int i = 1; i < n; i++) {
      data[i][i - 1] = _c[i - 1];
    }
    _matrix = DoubleMatrix.copyOf(data);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(_a);
    result = prime * result + Arrays.hashCode(_b);
    result = prime * result + Arrays.hashCode(_c);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    TridiagonalMatrix other = (TridiagonalMatrix) obj;
    if (!Arrays.equals(_a, other._a)) {
      return false;
    }
    if (!Arrays.equals(_b, other._b)) {
      return false;
    }
    if (!Arrays.equals(_c, other._c)) {
      return false;
    }
    return true;
  }

  @Override
  public int dimensions() {
    return 2;
  }

  @Override
  public int size() {
    return _a.length;
  }

  /**
   * Gets the entry for the indices.
   * 
   * @param index  the indices
   * @return the entry
   */
  public double getEntry(int... index) {
    ArgChecker.notNull(index, "indices");
    int n = _a.length;
    int i = index[0];
    int j = index[1];
    ArgChecker.isTrue(i >= 0 && i < n, "x index {} out of range. Matrix has {} rows", index[0], n);
    ArgChecker.isTrue(j >= 0 && j < n, "y index {} out of range. Matrix has {} columns", index[1], n);
    if (i == j) {
      return _a[i];
    } else if ((i - 1) == j) {
      return _c[i - 1];
    } else if ((i + 1) == j) {
      return _b[i];
    }

    return 0.0;
  }

}
