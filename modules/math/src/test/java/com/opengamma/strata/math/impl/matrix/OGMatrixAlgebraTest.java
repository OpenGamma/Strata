/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

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
  private static final DoubleMatrix2D A = new DoubleMatrix2D(new double[][] { {1., 2., 3. }, {-1., 1., 0. },
    {-2., 1., -2. } });
  private static final DoubleMatrix2D B = new DoubleMatrix2D(new double[][] { {1, 1 }, {2, -2 }, {3, 1 } });
  private static final DoubleMatrix2D C = new DoubleMatrix2D(new double[][] { {14, 0 }, {1, -3 }, {-6, -6 } });
  private static final DoubleMatrix1D D = new DoubleMatrix1D(new double[] {1, 1, 1 });
  private static final DoubleMatrix1D E = new DoubleMatrix1D(new double[] {-1, 2, 3 });
  private static final DoubleMatrix1D F = new DoubleMatrix1D(new double[] {2, -2, 1 });

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
    final DoubleMatrix2D res = ALGEBRA.getOuterProduct(E, F);
    final int rows = res.getNumberOfRows();
    final int cols = res.getNumberOfColumns();
    int i, j;
    for (i = 0; i < rows; i++) {
      for (j = 0; j < cols; j++) {
        assertEquals(res.getEntry(i, j), E.getEntry(i) * F.getEntry(j), 1e-15);
      }
    }

  }

  @Test
  public void testMultiply() {
    final DoubleMatrix2D c = (DoubleMatrix2D) ALGEBRA.multiply(A, B);
    final int rows = c.getNumberOfRows();
    final int cols = c.getNumberOfColumns();
    int i, j;
    for (i = 0; i < rows; i++) {
      for (j = 0; j < cols; j++) {
        assertEquals(c.getEntry(i, j), C.getEntry(i, j), 1e-15);
      }
    }

    final DoubleMatrix1D d = (DoubleMatrix1D) ALGEBRA.multiply(A, D);
    assertEquals(6, d.getEntry(0), 1e-15);
    assertEquals(0, d.getEntry(1), 1e-15);
    assertEquals(-3, d.getEntry(2), 1e-15);
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
    final DoubleMatrix1D xVec = new DoubleMatrix1D(x);
    DoubleMatrix1D y1 = (DoubleMatrix1D) ALGEBRA.multiply(m, xVec);
    DoubleMatrix2D full = m.toDoubleMatrix2D();
    DoubleMatrix1D y2 = (DoubleMatrix1D) ALGEBRA.multiply(full, xVec);

    for (int i = 0; i < n; i++) {
      assertEquals(y1.getEntry(i), y2.getEntry(i), 1e-12);
    }

  }

  @Test
  public void testTranspose() {
    final DoubleMatrix2D a = new DoubleMatrix2D(new double[][] { {1, 2 }, {3, 4 }, {5, 6 } });
    assertEquals(3, a.getNumberOfRows());
    assertEquals(2, a.getNumberOfColumns());
    DoubleMatrix2D aT = ALGEBRA.getTranspose(a);
    assertEquals(2, aT.getNumberOfRows());
    assertEquals(3, aT.getNumberOfColumns());

  }

  @Test
  public void matrixTransposeMultipleMatrixTest() {
    DoubleMatrix2D a = new DoubleMatrix2D(new double[][] { {1.0, 2.0, 3.0 }, {-3.0, 1.3, 7.0 } });
    DoubleMatrix2D aTa = ALGEBRA.matrixTransposeMultiplyMatrix(a);
    DoubleMatrix2D aTaRef = (DoubleMatrix2D) ALGEBRA.multiply(ALGEBRA.getTranspose(a), a);
    AssertMatrix.assertEqualsMatrix(aTaRef, aTa, 1e-15);
  }

}
