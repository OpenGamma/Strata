/**
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;
import org.testng.internal.junit.ArrayAsserts;

import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
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
  private static final DoubleMatrix2D A3 = new DoubleMatrix2D(new double[][] {new double[] {10.0, 2.0, -1.0 }, new double[] {2.0, 5.0, -2.0 }, new double[] {-1.0, -2.0, 15.0 } });
  private static final DoubleMatrix2D A5 = new DoubleMatrix2D(new double[][] {new double[] {10.0, 2.0, -1.0, 1.0, 1.0 }, new double[] {2.0, 5.0, -2.0, 0.5, 0.5 },
    new double[] {-1.0, -2.0, 15.0, 1.0, 0.5 }, new double[] {1.0, 0.5, 1.0, 10.0, -1.0 }, new double[] {1.0, 0.5, 0.5, -1.0, 25.0 } });
  private static final double EPS = 1e-9;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullObjectMatrix() {
    CDOG.evaluate((DoubleMatrix2D) null);
  }

  /**
   * Tests A = L L^T.
   */
  public void recoverOrginal() {
    final CholeskyDecompositionResult result = CDOG.evaluate(A3);
    final DoubleMatrix2D a = (DoubleMatrix2D) ALGEBRA.multiply(result.getL(), result.getLT());
    checkEquals(A3, a);
  }

  /**
   * Tests solve Ax = b from A and b.
   */
  public void solveVector() {
    final CholeskyDecompositionResult result = CDOG.evaluate(A5);
    double[] b = new double[] {1.0, 2.0, 3.0, 4.0, -1.0 };
    double[] x = result.solve(b);
    DoubleMatrix1D ax = (DoubleMatrix1D) ALGEBRA.multiply(A5, new DoubleMatrix1D(x));
    ArrayAsserts.assertArrayEquals("Cholesky decomposition OpenGamma - solve", b, ax.getData(), 1.0E-10);
  }

  /**
   * Tests solve AX = B from A and B.
   */
  public void solveMatrix() {
    final CholeskyDecompositionResult result = CDOG.evaluate(A5);
    double[][] b = new double[][] { {1.0, 2.0 }, {2.0, 3.0 }, {3.0, 4.0 }, {4.0, -2.0 }, {-1.0, -1.0 } };
    DoubleMatrix2D x = result.solve(new DoubleMatrix2D(b));
    DoubleMatrix2D ax = (DoubleMatrix2D) ALGEBRA.multiply(A5, x);
    ArrayAsserts.assertArrayEquals("Cholesky decomposition OpenGamma - solve", b[0], ax.getData()[0], 1.0E-10);
    ArrayAsserts.assertArrayEquals("Cholesky decomposition OpenGamma - solve", b[1], ax.getData()[1], 1.0E-10);
  }

  /**
   * Compare results with Common decomposition
   */
  public void compareCommon() {
    final CholeskyDecompositionResult resultOG = CDOG.evaluate(A3);
    final CholeskyDecompositionResult resultC = CDC.evaluate(A3);
    checkEquals(resultC.getL(), resultOG.getL());
    checkEquals(ALGEBRA.getTranspose(resultC.getL()), resultOG.getLT());
    assertEquals("Determinant", resultC.getDeterminant(), resultOG.getDeterminant(), 1.0E-10);
  }

  private void checkEquals(final DoubleMatrix2D x, final DoubleMatrix2D y) {
    final int n = x.getNumberOfRows();
    final int m = x.getNumberOfColumns();
    assertEquals(n, y.getNumberOfRows());
    assertEquals(m, y.getNumberOfColumns());
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < m; j++) {
        assertEquals(x.getEntry(i, j), y.getEntry(i, j), EPS);
      }
    }
  }

}
