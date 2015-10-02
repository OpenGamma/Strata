/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Parent class for matrix algebra operations. Basic operations (add, subtract, scale) are implemented in this class.
 */
public abstract class MatrixAlgebra {

  /**
   * Adds two matrices. This operation can only be performed if the matrices are of the same type and dimensions.
   * @param m1 The first matrix, not null
   * @param m2 The second matrix, not null
   * @return The sum of the two matrices
   * @throws IllegalArgumentException If the matrices are not of the same type, if the matrices are not the same shape.
   */
  public Matrix<?> add(final Matrix<?> m1, final Matrix<?> m2) {
    ArgChecker.notNull(m1, "m1");
    ArgChecker.notNull(m2, "m2");
    if (m1 instanceof DoubleMatrix1D) {
      if (m2 instanceof DoubleMatrix1D) {
        final double[] x1 = ((DoubleMatrix1D) m1).getData();
        final double[] x2 = ((DoubleMatrix1D) m2).getData();
        final int n = x1.length;
        ArgChecker.isTrue(n == x2.length, "Can only add matrices of the same shape");
        final double[] sum = new double[n];
        for (int i = 0; i < n; i++) {
          sum[i] = x1[i] + x2[i];
        }
        return new DoubleMatrix1D(sum);
      }
      throw new IllegalArgumentException("Tried to add a " + m1.getClass() + " and " + m2.getClass());
    } else if (m1 instanceof DoubleMatrix2D) {
      if (m2 instanceof DoubleMatrix2D) {

        final double[][] x1 = ((DoubleMatrix2D) m1).getData();
        final double[][] x2 = ((DoubleMatrix2D) m2).getData();
        final int n = x1.length;
        final int m = x1[0].length;
        ArgChecker.isTrue(n == x2.length, "Can only add matrices of the same shape");
        final double[][] sum = new double[n][x1[0].length];
        for (int i = 0; i < n; i++) {
          ArgChecker.isTrue(m == x2[i].length, "Can only add matrices of the same shape");
          for (int j = 0; j < m; j++) {
            sum[i][j] = x1[i][j] + x2[i][j];
          }
        }
        return new DoubleMatrix2D(sum);
      }
      throw new IllegalArgumentException("Tried to add a " + m1.getClass() + " and " + m2.getClass());
    }
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the quotient of two matrices $C = \frac{A}{B} = AB^{-1}$, where
   * $B^{-1}$ is the pseudo-inverse of $B$ i.e. $BB^{-1} = \mathbb{1}$.
   * @param m1 The numerator matrix, not null. This matrix must be a {@link DoubleMatrix2D}.
   * @param m2 The denominator, not null. This matrix must be a {@link DoubleMatrix2D}.
   * @return The result
   */
  public Matrix<?> divide(final Matrix<?> m1, final Matrix<?> m2) {
    ArgChecker.notNull(m1, "m1");
    ArgChecker.notNull(m2, "m2");
    ArgChecker.isTrue(m1 instanceof DoubleMatrix2D, "Can only divide a 2D matrix");
    ArgChecker.isTrue(m2 instanceof DoubleMatrix2D, "Can only perform division with a 2D matrix");
    return multiply(m1, getInverse(m2));
  }

  /**
   * Returns the Kronecker product of two matrices. If $\mathbf{A}$ is an $m
   * \times n$ matrix and $\mathbf{B}$ is a $p \times q$ matrix, then the
   * Kronecker product $A \otimes B$ is an $mp \times nq$ matrix with elements
   * $$
   * \begin{align*}
   * A \otimes B &=
   * \begin{pmatrix}
   * a_{11}\mathbf{B} & \cdots & a_{1n}\mathbf{B} \\
   * \vdots & \ddots & \vdots \\
   * a_{m1}\mathbf{B} & \cdots & a_{mn}\mathbf{B}
   * \end{pmatrix}\\
   * &=
   * \begin{pmatrix}
   * a_{11}b_{11} & a_{11}b_{12} & \cdots & a_{11}b_{1q} & \cdots & a_{1n}b_{11} & a_{1n}b_{12} & \cdots & a_{1n}b_{1q}\\
   * a_{11}b_{21} & a_{11}b_{22} & \cdots & a_{11}b_{2q} & \cdots & a_{1n}b_{21} & a_{1n}b_{22} & \cdots & a_{1n}b_{2q} \\
   * \vdots & \vdots & \ddots & \vdots & \cdots & \vdots & \vdots & \ddots & \cdots \\
   * a_{11}b_{p1} & a_{11}b_{p2} & \cdots & a_{11}b_{pq} & \cdots & a_{1n}b_{p1} & a_{1n}b_{p2} & \cdots & a_{1n}b_{pq} \\
   * \vdots & \vdots & & \vdots & \ddots & \vdots & \vdots & & \cdots \\
   * a_{m1}b_{11} & a_{m1}b_{12} & \cdots & a_{m1}b_{1q} & \cdots & a_{mn}b_{11} & a_{mn}b_{12} & \cdots & a_{mn}b_{1q} \\
   * a_{m1}b_{21} & a_{m1}b_{22} & \cdots & a_{m1}b_{2q} & \cdots & a_{mn}b_{21} & a_{mn}b_{22} & \cdots & a_{mn}b_{2q} \\
   * \vdots & \vdots & \ddots & \vdots & \cdots & \vdots & \vdots & \ddots & \cdots \\
   * a_{m1}b_{p1} & a_{m1}b_{p2} & \cdots & a_{m1}b_{pq} & \cdots & a_{mn}b_{p1} & a_{mn}b_{p2} & \cdots & a_{mn}b_{pq}
   * \end{pmatrix}
   * \end{align*}
   * $$
   * @param m1 The first matrix, not null. This matrix must be a {@link DoubleMatrix2D}.
   * @param m2 The second matrix, not null. This matrix must be a {@link DoubleMatrix2D}.
   * @return The Kronecker product
   */
  public Matrix<?> kroneckerProduct(final Matrix<?> m1, final Matrix<?> m2) {
    ArgChecker.notNull(m1, "m1");
    ArgChecker.notNull(m2, "m2");
    if (m1 instanceof DoubleMatrix2D && m2 instanceof DoubleMatrix2D) {
      final double[][] a = ((DoubleMatrix2D) m1).getData();
      final double[][] b = ((DoubleMatrix2D) m2).getData();
      final int aRows = a.length;
      final int aCols = a[0].length;
      final int bRows = b.length;
      final int bCols = b[0].length;
      final int rRows = aRows * bRows;
      final int rCols = aCols * bCols;
      final double[][] res = new double[rRows][rCols];
      for (int i = 0; i < aRows; i++) {
        for (int j = 0; j < aCols; j++) {
          final double t = a[i][j];
          if (t != 0.0) {
            for (int k = 0; k < bRows; k++) {
              for (int l = 0; l < bCols; l++) {
                res[i * bRows + k][j * bCols + l] = t * b[k][l];
              }
            }
          }
        }
      }
      return new DoubleMatrix2D(res);
    }
    throw new IllegalArgumentException("Can only calculate the Kronecker product of two DoubleMatrix2D.");
  }

  /**
   * Multiplies two matrices.
   * @param m1 The first matrix, not null.
   * @param m2 The second matrix, not null.
   * @return The product of the two matrices.
   */
  public abstract Matrix<?> multiply(final Matrix<?> m1, final Matrix<?> m2);

  /**
   * Scale a vector or matrix by a given amount, i.e. each element is multiplied by the scale.
   * @param m A vector or matrix, not null
   * @param scale The scale
   * @return the scaled vector or matrix
   */
  public Matrix<?> scale(final Matrix<?> m, final double scale) {
    ArgChecker.notNull(m, "m");
    if (m instanceof DoubleMatrix1D) {
      final double[] x = ((DoubleMatrix1D) m).getData();
      final int n = x.length;
      final double[] scaled = new double[n];
      for (int i = 0; i < n; i++) {
        scaled[i] = x[i] * scale;
      }
      return new DoubleMatrix1D(scaled);
    } else if (m instanceof DoubleMatrix2D) {
      final double[][] x = ((DoubleMatrix2D) m).getData();
      final int n = x.length;
      final double[][] scaled = new double[n][x[0].length];
      for (int i = 0; i < n; i++) {
        for (int j = 0; j < x[0].length; j++) {
          scaled[i][j] = x[i][j] * scale;
        }
      }
      return new DoubleMatrix2D(scaled);
    }
    throw new UnsupportedOperationException();
  }

  /**
   * Subtracts two matrices. This operation can only be performed if the matrices are of the same type and dimensions.
   * @param m1 The first matrix, not null
   * @param m2 The second matrix, not null
   * @return The second matrix subtracted from the first
   * @throws IllegalArgumentException If the matrices are not of the same type, if the matrices are not the same shape.
   */
  public Matrix<?> subtract(final Matrix<?> m1, final Matrix<?> m2) {
    ArgChecker.notNull(m1, "m1");
    ArgChecker.notNull(m2, "m2");
    if (m1 instanceof DoubleMatrix1D) {
      if (m2 instanceof DoubleMatrix1D) {
        final double[] x1 = ((DoubleMatrix1D) m1).getData();
        final double[] x2 = ((DoubleMatrix1D) m2).getData();
        final int n = x1.length;
        ArgChecker.isTrue(n == x2.length, "Can only subtract matrices of the same shape");
        final double[] sum = new double[n];
        for (int i = 0; i < n; i++) {
          sum[i] = x1[i] - x2[i];
        }
        return new DoubleMatrix1D(sum);
      }
      throw new IllegalArgumentException("Tried to subtract a " + m1.getClass() + " and " + m2.getClass());
    } else if (m1 instanceof DoubleMatrix2D) {
      if (m2 instanceof DoubleMatrix2D) {
        final double[][] x1 = ((DoubleMatrix2D) m1).getData();
        final double[][] x2 = ((DoubleMatrix2D) m2).getData();
        final int n = x1.length;
        final int m = x1[0].length;
        ArgChecker.isTrue(n == x2.length, "Can only subtract matrices of the same shape");
        final double[][] sum = new double[n][x1[0].length];
        for (int i = 0; i < n; i++) {
          ArgChecker.isTrue(m == x2[i].length, "Can only subtract matrices of the same shape");
          for (int j = 0; j < m; j++) {
            sum[i][j] = x1[i][j] - x2[i][j];
          }
        }
        return new DoubleMatrix2D(sum);
      }
      throw new IllegalArgumentException("Tried to subtract a " + m1.getClass() + " and " + m2.getClass());
    }
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the condition number of the matrix.
   * @param m A matrix, not null
   * @return The condition number of the matrix
   */
  public abstract double getCondition(final Matrix<?> m);

  /**
   * Returns the determinant of the matrix.
   * @param m A matrix, not null
   * @return The determinant of the matrix
   */
  public abstract double getDeterminant(final Matrix<?> m);

  /**
   * Returns the inverse (or pseudo-inverse) of the matrix.
   * @param m A matrix, not null
   * @return The inverse matrix
   */
  public abstract DoubleMatrix2D getInverse(final Matrix<?> m);

  /**
   * Returns the inner (or dot) product.
   * @param m1 A vector, not null
   * @param m2 A vector, not null
   * @return The scalar dot product
   * @exception IllegalArgumentException If the vectors are not the same size
   */
  public abstract double getInnerProduct(final Matrix<?> m1, final Matrix<?> m2);

  /**
   * Returns the outer product.
   * @param m1 A vector, not null
   * @param m2 A vector, not null
   * @return The outer product
   * @exception IllegalArgumentException If the vectors are not the same size
   */
  public abstract DoubleMatrix2D getOuterProduct(final Matrix<?> m1, final Matrix<?> m2);

  /**
   * For a vector, returns the <a href="http://mathworld.wolfram.com/L1-Norm.html">$L_1$ norm</a>
   * (also known as the Taxicab norm or Manhattan norm), i.e. $\Sigma |x_i|$.
   * <p>
   * For a matrix, returns the <a href="http://mathworld.wolfram.com/MaximumAbsoluteColumnSumNorm.html">maximum absolute column sum norm</a> of the matrix.
   * @param m A vector or matrix, not null
   * @return The $L_1$ norm
   */
  public abstract double getNorm1(final Matrix<?> m);

  /**
   * For a vector, returns <a href="http://mathworld.wolfram.com/L2-Norm.html">$L_2$ norm</a> (also known as the
   * Euclidean norm).
   * <p>
   * For a matrix, returns the <a href="http://mathworld.wolfram.com/SpectralNorm.html">spectral norm</a>
   * @param m A vector or matrix, not null
   * @return the norm
   */
  public abstract double getNorm2(final Matrix<?> m);

  /**
   * For a vector, returns the <a href="http://mathworld.wolfram.com/L-Infinity-Norm.html">$L_\infty$ norm</a>.
   * $L_\infty$ norm is the maximum of the absolute values of the elements.
   * <p>
   * For a matrix, returns the <a href="http://mathworld.wolfram.com/MaximumAbsoluteRowSumNorm.html">maximum absolute row sum norm</a>
   * @param m a vector or a matrix, not null
   * @return the norm
   */
  public abstract double getNormInfinity(final Matrix<?> m);

  /**
   * Returns a matrix raised to an integer power, e.g. $\mathbf{A}^3 = \mathbf{A}\mathbf{A}\mathbf{A}$.
   * @param m A square matrix, not null
   * @param p An integer power
   * @return The result
   */
  public abstract DoubleMatrix2D getPower(final Matrix<?> m, final int p);

  /**
   * Returns a matrix raised to a power, $\mathbf{A}^3 = \mathbf{A}\mathbf{A}\mathbf{A}$.
   * @param m A square matrix, not null
   * @param p The power
   * @return The result
   */
  public abstract DoubleMatrix2D getPower(Matrix<?> m, double p);

  /**
   * Returns the trace (i.e. sum of diagonal elements) of a matrix.
   * @param m A matrix, not null. The matrix must be square.
   * @return The trace
   */
  public abstract double getTrace(final Matrix<?> m);

  /**
   * Returns the transpose of a matrix.
   * @param m A matrix, not null
   * @return The transpose matrix
   */
  public abstract DoubleMatrix2D getTranspose(final Matrix<?> m);

  /**
   * Compute $A^T A$, where A is a matrix
   * @param a The matrix
   * @return The result of $A^T A$
   */
  public DoubleMatrix2D matrixTransposeMultiplyMatrix(DoubleMatrix2D a) {
    ArgChecker.notNull(a, "a");
    final int n = a.getNumberOfRows();
    final int m = a.getNumberOfColumns();

    DoubleMatrix2D res = new DoubleMatrix2D(m, m);
    double[][] data = res.getData();
    double[][] aData = a.getData();

    for (int i = 0; i < m; i++) {
      double sum = 0.0;
      for (int k = 0; k < n; k++) {
        sum += aData[k][i] * aData[k][i];
      }
      data[i][i] = sum;

      for (int j = i + 1; j < m; j++) {
        sum = 0.0;
        for (int k = 0; k < n; k++) {
          sum += aData[k][i] * aData[k][j];
        }
        data[i][j] = sum;
        data[j][i] = sum;
      }
    }
    return res;
  }
}
