/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import static com.opengamma.strata.math.impl.matrix.MatrixAlgebraFactory.OG_ALGEBRA;
import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Test.
 */
@Test
public class InverseTridiagonalMatrixCalculatorTest {
  private static final InverseTridiagonalMatrixCalculator CALCULATOR = new InverseTridiagonalMatrixCalculator();
  private static final double[] A = new double[] {1.0, 2.4, -0.4, -0.8, 1.5, 7.8, -5.0, 1.0, 2.4, -0.4, 3.14 };
  private static final double[] B = new double[] {1.56, 0.33, 0.42, -0.23, 0.276, 4.76, 1.0, 2.4, -0.4, 0.2355 };
  private static final double[] C = new double[] {0.56, 0.63, -0.42, -0.23, 0.76, 1.76, 1.0, 2.4, -0.4, 2.4234 };

  private static final TridiagonalMatrix MATRIX = new TridiagonalMatrix(A, B, C);
  private static final DoubleMatrix TRI = MATRIX.toDoubleMatrix();
  private static final double EPS = 1e-15;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullArray() {
    CALCULATOR.apply((TridiagonalMatrix) null);
  }

  @Test
  public void testInvertIdentity() {
    final int n = 11;
    final double[] a = new double[n];
    final double[] b = new double[n - 1];
    final double[] c = new double[n - 1];
    int i, j;

    for (i = 0; i < n; i++) {
      a[i] = 1.0;
    }
    final DoubleMatrix res = CALCULATOR.apply(new TridiagonalMatrix(a, b, c));
    for (i = 0; i < n; i++) {
      for (j = 0; j < n; j++) {
        assertEquals((i == j ? 1.0 : 0.0), res.get(i, j), EPS);
      }
    }

  }

  @Test
  public void testInvert() {
    final DoubleMatrix res = CALCULATOR.apply(MATRIX);
    final DoubleMatrix idet = (DoubleMatrix) OG_ALGEBRA.multiply(TRI, res);

    final int n = idet.rowCount();
    int i, j;
    for (i = 0; i < n; i++) {
      for (j = 0; j < n; j++) {
        assertEquals((i == j ? 1.0 : 0.0), idet.get(i, j), EPS);
      }
    }

  }

}
