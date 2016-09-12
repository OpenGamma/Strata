/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;

/**
 * Abstract test.
 */
@Test
public abstract class SVDecompositionCalculationTestCase {
  private static final double EPS = 1e-10;
  private static final DoubleMatrix A = DoubleMatrix.copyOf(
      new double[][] { {1, 2, 3}, {-3.4, -1, 4}, {1, 6, 1}});

  protected abstract Decomposition<SVDecompositionResult> getSVD();

  protected abstract MatrixAlgebra getAlgebra();

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullObjectMatrix() {
    getSVD().apply((DoubleMatrix) null);
  }

  @Test
  public void testRecoverOrginal() {
    final MatrixAlgebra algebra = getAlgebra();
    final DecompositionResult result = getSVD().apply(A);
    assertTrue(result instanceof SVDecompositionResult);
    final SVDecompositionResult svd_result = (SVDecompositionResult) result;
    final DoubleMatrix u = svd_result.getU();
    final DoubleMatrix w = DoubleMatrix.diagonal(DoubleArray.copyOf(svd_result.getSingularValues()));
    final DoubleMatrix vt = svd_result.getVT();
    final DoubleMatrix a = (DoubleMatrix) algebra.multiply(algebra.multiply(u, w), vt);
    checkEquals(A, a);
  }

  @Test
  public void testInvert() {
    final MatrixAlgebra algebra = getAlgebra();
    final SVDecompositionResult result = getSVD().apply(A);
    final DoubleMatrix ut = result.getUT();
    final DoubleMatrix v = result.getV();
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
    final DoubleMatrix winv = DoubleMatrix.diagonal(DoubleArray.copyOf(svinv));
    final DoubleMatrix ainv = (DoubleMatrix) algebra.multiply(algebra.multiply(v, winv), ut);
    final DoubleMatrix identity = (DoubleMatrix) algebra.multiply(A, ainv);
    checkIdentity(identity);

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

  private void checkIdentity(final DoubleMatrix x) {
    final int n = x.rowCount();
    assertEquals(x.columnCount(), n);
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        if (i == j) {
          assertEquals(1.0, x.get(i, i), EPS);
        } else {
          assertEquals(0.0, x.get(i, j), EPS);
        }
      }
    }
  }
}
