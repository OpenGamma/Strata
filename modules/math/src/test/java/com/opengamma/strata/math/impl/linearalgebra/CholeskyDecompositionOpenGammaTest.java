/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;
import org.testng.internal.junit.ArrayAsserts;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * Tests the Cholesky decomposition OpenGamma implementation.
 */
@Test
public class CholeskyDecompositionOpenGammaTest {

  private static final MatrixAlgebra ALGEBRA = new OGMatrixAlgebra();
  private static final CholeskyDecompositionOpenGamma CDOG = new CholeskyDecompositionOpenGamma();
  private static final Decomposition<CholeskyDecompositionResult> CDC = new CholeskyDecompositionCommons();
  private static final DoubleMatrix A3 = DoubleMatrix.copyOf(
      new double[][] { {10.0, 2.0, -1.0}, {2.0, 5.0, -2.0}, {-1.0, -2.0, 15.0}});
  private static final DoubleMatrix A5 = DoubleMatrix.copyOf(
      new double[][] {
          {10.0, 2.0, -1.0, 1.0, 1.0},
          {2.0, 5.0, -2.0, 0.5, 0.5},
          {-1.0, -2.0, 15.0, 1.0, 0.5},
          {1.0, 0.5, 1.0, 10.0, -1.0},
          {1.0, 0.5, 0.5, -1.0, 25.0}});
  private static final double EPS = 1e-9;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullObjectMatrix() {
    CDOG.apply((DoubleMatrix) null);
  }

  /**
   * Tests A = L L^T.
   */
  public void recoverOrginal() {
    final CholeskyDecompositionResult result = CDOG.apply(A3);
    final DoubleMatrix a = (DoubleMatrix) ALGEBRA.multiply(result.getL(), result.getLT());
    checkEquals(A3, a);
  }

  /**
   * Tests solve Ax = b from A and b.
   */
  public void solveVector() {
    final CholeskyDecompositionResult result = CDOG.apply(A5);
    double[] b = new double[] {1.0, 2.0, 3.0, 4.0, -1.0 };
    double[] x = result.solve(b);
    DoubleArray ax = (DoubleArray) ALGEBRA.multiply(A5, DoubleArray.copyOf(x));
    ArrayAsserts.assertArrayEquals("Cholesky decomposition OpenGamma - solve", b, ax.toArray(), 1.0E-10);
  }

  /**
   * Tests solve AX = B from A and B.
   */
  public void solveMatrix() {
    final CholeskyDecompositionResult result = CDOG.apply(A5);
    double[][] b = new double[][] { {1.0, 2.0 }, {2.0, 3.0 }, {3.0, 4.0 }, {4.0, -2.0 }, {-1.0, -1.0 } };
    DoubleMatrix x = result.solve(DoubleMatrix.copyOf(b));
    DoubleMatrix ax = (DoubleMatrix) ALGEBRA.multiply(A5, x);
    ArrayAsserts.assertArrayEquals("Cholesky decomposition OpenGamma - solve", b[0], ax.rowArray(0), 1.0E-10);
    ArrayAsserts.assertArrayEquals("Cholesky decomposition OpenGamma - solve", b[1], ax.rowArray(1), 1.0E-10);
  }

  /**
   * Compare results with Common decomposition
   */
  public void compareCommon() {
    final CholeskyDecompositionResult resultOG = CDOG.apply(A3);
    final CholeskyDecompositionResult resultC = CDC.apply(A3);
    checkEquals(resultC.getL(), resultOG.getL());
    checkEquals(ALGEBRA.getTranspose(resultC.getL()), resultOG.getLT());
    assertEquals("Determinant", resultC.getDeterminant(), resultOG.getDeterminant(), 1.0E-10);
  }

  private void checkEquals(final DoubleMatrix x, final DoubleMatrix y) {
    final int n = x.rowCount();
    final int m = x.columnCount();
    assertEquals(n, y.rowCount());
    assertEquals(m, y.columnCount());
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < m; j++) {
        assertEquals(x.get(i, j), y.get(i, j), EPS);
      }
    }
  }

}
