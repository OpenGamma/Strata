/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.matrix.CommonsMatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;

/**
 * Test.
 */
@Test
public class LUDecompositionCommonsTest {
  private static final MatrixAlgebra ALGEBRA = new CommonsMatrixAlgebra();
  private static final Decomposition<LUDecompositionResult> LU = new LUDecompositionCommons();
  private static final DoubleMatrix2D A = new DoubleMatrix2D(new double[][] {new double[] {1, 2, -1 }, new double[] {4, 3, 1 }, new double[] {2, 2, 3 } });
  private static final double EPS = 1e-9;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullObjectMatrix() {
    LU.evaluate((DoubleMatrix2D) null);
  }

  @Test
  public void testRecoverOrginal() {
    final DecompositionResult result = LU.evaluate(A);
    assertTrue(result instanceof LUDecompositionResult);
    final LUDecompositionResult lu = (LUDecompositionResult) result;
    final DoubleMatrix2D a = (DoubleMatrix2D) ALGEBRA.multiply(lu.getL(), lu.getU());
    checkEquals((DoubleMatrix2D) ALGEBRA.multiply(lu.getP(), A), a);
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
