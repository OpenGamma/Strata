/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrixUtils;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;

/**
 * Abstract test.
 */
@Test
public abstract class SVDecompositionCalculationTestCase {
  private static final double EPS = 1e-10;
  private static final DoubleMatrix2D A = new DoubleMatrix2D(new double[][] {new double[] {1, 2, 3 }, new double[] {-3.4, -1, 4 }, new double[] {1, 6, 1 } });

  protected abstract Decomposition<SVDecompositionResult> getSVD();

  protected abstract MatrixAlgebra getAlgebra();

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullObjectMatrix() {
    getSVD().evaluate((DoubleMatrix2D) null);
  }

  @Test
  public void testRecoverOrginal() {
    final MatrixAlgebra algebra = getAlgebra();
    final DecompositionResult result = getSVD().evaluate(A);
    assertTrue(result instanceof SVDecompositionResult);
    final SVDecompositionResult svd_result = (SVDecompositionResult) result;
    final DoubleMatrix2D u = svd_result.getU();
    final double[] sv = svd_result.getSingularValues();
    final DoubleMatrix2D w = DoubleMatrixUtils.getTwoDimensionalDiagonalMatrix(sv);
    final DoubleMatrix2D vt = svd_result.getVT();
    final DoubleMatrix2D a = (DoubleMatrix2D) algebra.multiply(algebra.multiply(u, w), vt);
    checkEquals(A, a);
  }

  @Test
  public void testInvert() {
    final MatrixAlgebra algebra = getAlgebra();
    final SVDecompositionResult result = getSVD().evaluate(A);
    final DoubleMatrix2D ut = result.getUT();
    final DoubleMatrix2D v = result.getV();
    final double[] sv = result.getSingularValues();
    final int n = sv.length;
    final double[] svinv = new double[n];
    for (int i = 0; i < n; i++) {
      if (sv[i] == 0.0) {
        svinv[i] = 0.0;
      } else {
        svinv[i] = 1.0 / sv[i];
      }
    }
    final DoubleMatrix2D winv = DoubleMatrixUtils.getTwoDimensionalDiagonalMatrix(svinv);
    final DoubleMatrix2D ainv = (DoubleMatrix2D) algebra.multiply(algebra.multiply(v, winv), ut);
    final DoubleMatrix2D identity = (DoubleMatrix2D) algebra.multiply(A, ainv);
    checkIdentity(identity);

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

  private void checkIdentity(final DoubleMatrix2D x) {
    final int n = x.getNumberOfRows();
    assertEquals(x.getNumberOfColumns(), n);
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        if (i == j) {
          assertEquals(1.0, x.getEntry(i, i), EPS);
        } else {
          assertEquals(0.0, x.getEntry(i, j), EPS);
        }
      }
    }
  }
}
