/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.matrix.CommonsMatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;

/**
 * Test.
 */
@Test
public class LUDecompositionCommonsTest {
  private static final MatrixAlgebra ALGEBRA = new CommonsMatrixAlgebra();
  private static final Decomposition<LUDecompositionResult> LU = new LUDecompositionCommons();
  private static final DoubleMatrix A = DoubleMatrix.copyOf(
      new double[][] { {1, 2, -1}, {4, 3, 1}, {2, 2, 3}});
  private static final double EPS = 1e-9;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullObjectMatrix() {
    LU.apply((DoubleMatrix) null);
  }

  @Test
  public void testRecoverOrginal() {
    final DecompositionResult result = LU.apply(A);
    assertTrue(result instanceof LUDecompositionResult);
    final LUDecompositionResult lu = (LUDecompositionResult) result;
    final DoubleMatrix a = (DoubleMatrix) ALGEBRA.multiply(lu.getL(), lu.getU());
    checkEquals((DoubleMatrix) ALGEBRA.multiply(lu.getP(), A), a);
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
