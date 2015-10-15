/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class DoubleMatrixUtilsTest {

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeDimension() {
    DoubleMatrixUtils.getIdentityMatrix2D(-3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullVector() {
    DoubleMatrixUtils.getTwoDimensionalDiagonalMatrix((DoubleMatrix1D) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullArray() {
    DoubleMatrixUtils.getTwoDimensionalDiagonalMatrix((double[]) null);
  }

  @Test
  public void testIdentity() {
    assertEquals(DoubleMatrixUtils.getIdentityMatrix2D(0), DoubleMatrix2D.EMPTY);
    assertEquals(DoubleMatrixUtils.getIdentityMatrix2D(1), DoubleMatrix2D.copyOf(new double[][] {{1}}));
    assertEquals(DoubleMatrixUtils.getIdentityMatrix2D(4),
        DoubleMatrix2D.copyOf(new double[][] { {1, 0, 0, 0}, {0, 1, 0, 0}, {0, 0, 1, 0}, {0, 0, 0, 1}}));
  }

  @Test
  public void testDiagonalMatrix() {
    assertEquals(DoubleMatrixUtils.getTwoDimensionalDiagonalMatrix(DoubleMatrix1D.EMPTY), DoubleMatrix2D.EMPTY);
    assertEquals(DoubleMatrixUtils.getTwoDimensionalDiagonalMatrix(
        DoubleMatrix1D.of(1, 1, 1, 1)), DoubleMatrixUtils.getIdentityMatrix2D(4));
    assertEquals(DoubleMatrixUtils.getTwoDimensionalDiagonalMatrix(new double[0]), DoubleMatrix2D.EMPTY);
    assertEquals(DoubleMatrixUtils.getTwoDimensionalDiagonalMatrix(new double[] {1, 1, 1, 1 }), DoubleMatrixUtils.getIdentityMatrix2D(4));
  }

  @Test
  public void testTransposeMatrix() {
    DoubleMatrix2D m =
        DoubleMatrix2D.copyOf(new double[][] { {1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
    assertEquals(DoubleMatrixUtils.getTranspose(m),
        DoubleMatrix2D.copyOf(new double[][] { {1, 4, 7}, {2, 5, 8}, {3, 6, 9}}));
    m = DoubleMatrix2D.copyOf(new double[][] { {1, 2, 3, 4, 5, 6}, {7, 8, 9, 10, 11, 12}, {13, 14, 15, 16, 17, 18}});
    assertEquals(
        DoubleMatrixUtils.getTranspose(m),
        DoubleMatrix2D.copyOf(new double[][] { {1, 7, 13}, {2, 8, 14}, {3, 9, 15}, {4, 10, 16}, {5, 11, 17}, {6, 12, 18}}));
  }
}
