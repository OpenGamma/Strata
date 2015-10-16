/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Test.
 */
@Test
public class DoubleMatrixUtilsTest {

  @Test
  public void testTransposeMatrix() {
    DoubleMatrix m =
        DoubleMatrix.copyOf(new double[][] { {1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
    assertEquals(DoubleMatrixUtils.getTranspose(m),
        DoubleMatrix.copyOf(new double[][] { {1, 4, 7}, {2, 5, 8}, {3, 6, 9}}));
    m = DoubleMatrix.copyOf(new double[][] { {1, 2, 3, 4, 5, 6}, {7, 8, 9, 10, 11, 12}, {13, 14, 15, 16, 17, 18}});
    assertEquals(
        DoubleMatrixUtils.getTranspose(m),
        DoubleMatrix.copyOf(new double[][] { {1, 7, 13}, {2, 8, 14}, {3, 9, 15}, {4, 10, 16}, {5, 11, 17}, {6, 12, 18}}));
  }
}
