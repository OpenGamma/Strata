/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.linearalgebra.TridiagonalMatrix;
import com.opengamma.strata.math.impl.statistics.distribution.NormalDistribution;
import com.opengamma.strata.math.impl.statistics.distribution.ProbabilityDistribution;
import com.opengamma.strata.math.impl.util.AssertMatrix;

/**
 * Test.
 */
@Test
public class OGMatrixAlgebraTest {
  private static ProbabilityDistribution<Double> RANDOM = new NormalDistribution(0, 1);
  private static final MatrixAlgebra ALGEBRA = MatrixAlgebraFactory.getMatrixAlgebra("OG");
  private static final DoubleMatrix A = DoubleMatrix.copyOf(
      new double[][] { {1., 2., 3.}, {-1., 1., 0.}, {-2., 1., -2.}});
  private static final DoubleMatrix B = DoubleMatrix.copyOf(new double[][] { {1, 1}, {2, -2}, {3, 1}});
  private static final DoubleMatrix C = DoubleMatrix.copyOf(new double[][] { {14, 0}, {1, -3}, {-6, -6}});
  private static final DoubleArray D = DoubleArray.of(1, 1, 1);
  private static final DoubleArray E = DoubleArray.of(-1, 2, 3);
  private static final DoubleArray F = DoubleArray.of(2, -2, 1);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testMatrixSizeMismatch() {
    ALGEBRA.multiply(B, A);
  }

  @Test
  public void testDotProduct() {
    double res = ALGEBRA.getInnerProduct(E, F);
    assertEquals(-3.0, res, 1e-15);
    res = ALGEBRA.getNorm2(E);
    assertEquals(Math.sqrt(14.0), res, 1e-15);
  }

  @Test
  public void testOuterProduct() {
    final DoubleMatrix res = ALGEBRA.getOuterProduct(E, F);
    final int rows = res.rowCount();
    final int cols = res.columnCount();
    int i, j;
    for (i = 0; i < rows; i++) {
      for (j = 0; j < cols; j++) {
        assertEquals(res.get(i, j), E.get(i) * F.get(j), 1e-15);
      }
    }

  }

  @Test
  public void testMultiply() {
    final DoubleMatrix c = (DoubleMatrix) ALGEBRA.multiply(A, B);
    final int rows = c.rowCount();
    final int cols = c.columnCount();
    int i, j;
    for (i = 0; i < rows; i++) {
      for (j = 0; j < cols; j++) {
        assertEquals(c.get(i, j), C.get(i, j), 1e-15);
      }
    }

    final DoubleArray d = (DoubleArray) ALGEBRA.multiply(A, D);
    assertEquals(6, d.get(0), 1e-15);
    assertEquals(0, d.get(1), 1e-15);
    assertEquals(-3, d.get(2), 1e-15);
  }

  @Test
  public void testTridiagonalMultiply() {
    final int n = 37;
    double[] l = new double[n - 1];
    double[] c = new double[n];
    double[] u = new double[n - 1];
    double[] x = new double[n];

    for (int ii = 0; ii < n; ii++) {
      c[ii] = RANDOM.nextRandom();
      x[ii] = RANDOM.nextRandom();
      if (ii < n - 1) {
        l[ii] = RANDOM.nextRandom();
        u[ii] = RANDOM.nextRandom();
      }
    }

    final TridiagonalMatrix m = new TridiagonalMatrix(c, u, l);
    final DoubleArray xVec = DoubleArray.copyOf(x);
    DoubleArray y1 = (DoubleArray) ALGEBRA.multiply(m, xVec);
    DoubleMatrix full = m.toDoubleMatrix();
    DoubleArray y2 = (DoubleArray) ALGEBRA.multiply(full, xVec);

    for (int i = 0; i < n; i++) {
      assertEquals(y1.get(i), y2.get(i), 1e-12);
    }

  }

  @Test
  public void testTranspose() {
    final DoubleMatrix a = DoubleMatrix.copyOf(new double[][] { {1, 2}, {3, 4}, {5, 6}});
    assertEquals(3, a.rowCount());
    assertEquals(2, a.columnCount());
    DoubleMatrix aT = ALGEBRA.getTranspose(a);
    assertEquals(2, aT.rowCount());
    assertEquals(3, aT.columnCount());

  }

  @Test
  public void matrixTransposeMultipleMatrixTest() {
    DoubleMatrix a = DoubleMatrix.copyOf(new double[][] { {1.0, 2.0, 3.0}, {-3.0, 1.3, 7.0}});
    DoubleMatrix aTa = ALGEBRA.matrixTransposeMultiplyMatrix(a);
    DoubleMatrix aTaRef = (DoubleMatrix) ALGEBRA.multiply(ALGEBRA.getTranspose(a), a);
    AssertMatrix.assertEqualsMatrix(aTaRef, aTa, 1e-15);
  }

}
