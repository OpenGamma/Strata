/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.collect.array.Matrix;

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
  public Matrix add(Matrix m1, Matrix m2) {
    ArgChecker.notNull(m1, "m1");
    ArgChecker.notNull(m2, "m2");
    if (m1 instanceof DoubleArray) {
      if (m2 instanceof DoubleArray) {
        DoubleArray array1 = (DoubleArray) m1;
        DoubleArray array2 = (DoubleArray) m2;
        return array1.plus(array2);
      }
      throw new IllegalArgumentException("Tried to add a " + m1.getClass() + " and " + m2.getClass());

    } else if (m1 instanceof DoubleMatrix) {
      if (m2 instanceof DoubleMatrix) {
        DoubleMatrix matrix1 = (DoubleMatrix) m1;
        DoubleMatrix matrix2 = (DoubleMatrix) m2;
        return matrix1.plus(matrix2);
      }
      throw new IllegalArgumentException("Tried to add a " + m1.getClass() + " and " + m2.getClass());
    }
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the quotient of two matrices $C = \frac{A}{B} = AB^{-1}$, where
   * $B^{-1}$ is the pseudo-inverse of $B$ i.e. $BB^{-1} = \mathbb{1}$.
   * @param m1 The numerator matrix, not null. This matrix must be a {@link DoubleMatrix}.
   * @param m2 The denominator, not null. This matrix must be a {@link DoubleMatrix}.
   * @return The result
   */
  public Matrix divide(Matrix m1, Matrix m2) {
    ArgChecker.notNull(m1, "m1");
    ArgChecker.notNull(m2, "m2");
    ArgChecker.isTrue(m1 instanceof DoubleMatrix, "Can only divide a 2D matrix");
    ArgChecker.isTrue(m2 instanceof DoubleMatrix, "Can only perform division with a 2D matrix");
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
   * @param m1 The first matrix, not null. This matrix must be a {@link DoubleMatrix}.
   * @param m2 The second matrix, not null. This matrix must be a {@link DoubleMatrix}.
   * @return The Kronecker product
   */
  public Matrix kroneckerProduct(Matrix m1, Matrix m2) {
    ArgChecker.notNull(m1, "m1");
    ArgChecker.notNull(m2, "m2");
    if (m1 instanceof DoubleMatrix && m2 instanceof DoubleMatrix) {
      DoubleMatrix matrix1 = (DoubleMatrix) m1;
      DoubleMatrix matrix2 = (DoubleMatrix) m2;
      int aRows = matrix1.rowCount();
      int aCols = matrix1.columnCount();
      int bRows = matrix2.rowCount();
      int bCols = matrix2.columnCount();
      int rRows = aRows * bRows;
      int rCols = aCols * bCols;
      double[][] res = new double[rRows][rCols];
      for (int i = 0; i < aRows; i++) {
        for (int j = 0; j < aCols; j++) {
          double t = matrix1.get(i, j);
          if (t != 0.0) {
            for (int k = 0; k < bRows; k++) {
              for (int l = 0; l < bCols; l++) {
                res[i * bRows + k][j * bCols + l] = t * matrix2.get(k, l);
              }
            }
          }
        }
      }
      return DoubleMatrix.ofUnsafe(res);
    }
    throw new IllegalArgumentException("Can only calculate the Kronecker product of two DoubleMatrix.");
  }

  /**
   * Multiplies two matrices.
   * @param m1 The first matrix, not null.
   * @param m2 The second matrix, not null.
   * @return The product of the two matrices.
   */
  public abstract Matrix multiply(Matrix m1, Matrix m2);

  /**
   * Scale a vector or matrix by a given amount, i.e. each element is multiplied by the scale.
   * @param m A vector or matrix, not null
   * @param scale The scale
   * @return the scaled vector or matrix
   */
  public Matrix scale(Matrix m, double scale) {
    ArgChecker.notNull(m, "m");
    if (m instanceof DoubleArray) {
      return ((DoubleArray) m).multipliedBy(scale);

    } else if (m instanceof DoubleMatrix) {
      return ((DoubleMatrix) m).multipliedBy(scale);
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
  public Matrix subtract(Matrix m1, Matrix m2) {
    ArgChecker.notNull(m1, "m1");
    ArgChecker.notNull(m2, "m2");
    if (m1 instanceof DoubleArray) {
      if (m2 instanceof DoubleArray) {
        DoubleArray array1 = (DoubleArray) m1;
        DoubleArray array2 = (DoubleArray) m2;
        return array1.minus(array2);
      }
      throw new IllegalArgumentException("Tried to subtract a " + m1.getClass() + " and " + m2.getClass());
    } else if (m1 instanceof DoubleMatrix) {
      if (m2 instanceof DoubleMatrix) {
        DoubleMatrix matrix1 = (DoubleMatrix) m1;
        DoubleMatrix matrix2 = (DoubleMatrix) m2;
        return matrix1.minus(matrix2);
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
  public abstract double getCondition(Matrix m);

  /**
   * Returns the determinant of the matrix.
   * @param m A matrix, not null
   * @return The determinant of the matrix
   */
  public abstract double getDeterminant(Matrix m);

  /**
   * Returns the inverse (or pseudo-inverse) of the matrix.
   * @param m A matrix, not null
   * @return The inverse matrix
   */
  public abstract DoubleMatrix getInverse(Matrix m);

  /**
   * Returns the inner (or dot) product.
   * @param m1 A vector, not null
   * @param m2 A vector, not null
   * @return The scalar dot product
   * @exception IllegalArgumentException If the vectors are not the same size
   */
  public abstract double getInnerProduct(Matrix m1, Matrix m2);

  /**
   * Returns the outer product.
   * @param m1 A vector, not null
   * @param m2 A vector, not null
   * @return The outer product
   * @exception IllegalArgumentException If the vectors are not the same size
   */
  public abstract DoubleMatrix getOuterProduct(Matrix m1, Matrix m2);

  /**
   * For a vector, returns the <a href="http://mathworld.wolfram.com/L1-Norm.html">$L_1$ norm</a>
   * (also known as the Taxicab norm or Manhattan norm), i.e. $\Sigma |x_i|$.
   * <p>
   * For a matrix, returns the <a href="http://mathworld.wolfram.com/MaximumAbsoluteColumnSumNorm.html">maximum absolute column sum norm</a> of the matrix.
   * @param m A vector or matrix, not null
   * @return The $L_1$ norm
   */
  public abstract double getNorm1(Matrix m);

  /**
   * For a vector, returns <a href="http://mathworld.wolfram.com/L2-Norm.html">$L_2$ norm</a> (also known as the
   * Euclidean norm).
   * <p>
   * For a matrix, returns the <a href="http://mathworld.wolfram.com/SpectralNorm.html">spectral norm</a>
   * @param m A vector or matrix, not null
   * @return the norm
   */
  public abstract double getNorm2(Matrix m);

  /**
   * For a vector, returns the <a href="http://mathworld.wolfram.com/L-Infinity-Norm.html">$L_\infty$ norm</a>.
   * $L_\infty$ norm is the maximum of the absolute values of the elements.
   * <p>
   * For a matrix, returns the <a href="http://mathworld.wolfram.com/MaximumAbsoluteRowSumNorm.html">maximum absolute row sum norm</a>
   * @param m a vector or a matrix, not null
   * @return the norm
   */
  public abstract double getNormInfinity(Matrix m);

  /**
   * Returns a matrix raised to an integer power, e.g. $\mathbf{A}^3 = \mathbf{A}\mathbf{A}\mathbf{A}$.
   * @param m A square matrix, not null
   * @param p An integer power
   * @return The result
   */
  public abstract DoubleMatrix getPower(Matrix m, int p);

  /**
   * Returns a matrix raised to a power, $\mathbf{A}^3 = \mathbf{A}\mathbf{A}\mathbf{A}$.
   * @param m A square matrix, not null
   * @param p The power
   * @return The result
   */
  public abstract DoubleMatrix getPower(Matrix m, double p);

  /**
   * Returns the trace (i.e. sum of diagonal elements) of a matrix.
   * @param m A matrix, not null. The matrix must be square.
   * @return The trace
   */
  public abstract double getTrace(Matrix m);

  /**
   * Returns the transpose of a matrix.
   * @param m A matrix, not null
   * @return The transpose matrix
   */
  public abstract DoubleMatrix getTranspose(Matrix m);

  /**
   * Compute $A^T A$, where A is a matrix
   * @param a The matrix
   * @return The result of $A^T A$
   */
  public DoubleMatrix matrixTransposeMultiplyMatrix(DoubleMatrix a) {
    ArgChecker.notNull(a, "a");
    int n = a.rowCount();
    int m = a.columnCount();

    double[][] data = new double[m][m];
    for (int i = 0; i < m; i++) {
      double sum = 0d;
      for (int k = 0; k < n; k++) {
        sum += a.get(k, i) * a.get(k, i);
      }
      data[i][i] = sum;

      for (int j = i + 1; j < m; j++) {
        sum = 0d;
        for (int k = 0; k < n; k++) {
          sum += a.get(k, i) * a.get(k, j);
        }
        data[i][j] = sum;
        data[j][i] = sum;
      }
    }
    return DoubleMatrix.ofUnsafe(data);
  }

}
