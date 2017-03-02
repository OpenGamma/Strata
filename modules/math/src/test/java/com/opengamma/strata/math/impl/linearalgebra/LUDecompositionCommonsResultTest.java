/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import static org.testng.AssertJUnit.assertTrue;

import java.util.Arrays;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.FuzzyEquals;

/**
 * Tests the LUDecompositionCommonsResult class with well conditioned data and 
 * poorly conditioned data.
 */
@Test
public class LUDecompositionCommonsResultTest {

  static double[][] rawAok = new double[][] { {100.0000000000000000, 9.0000000000000000, 10.0000000000000000, 1.0000000000000000 },
    {9.0000000000000000, 50.0000000000000000, 19.0000000000000000, 15.0000000000000000 }, {10.0000000000000000, 11.0000000000000000, 29.0000000000000000, 21.0000000000000000 },
    {8.0000000000000000, 10.0000000000000000, 20.0000000000000000, 28.0000000000000000 } };
  static double[][] rawAsingular = new double[][] { {1000000.0000000000000000, 2.0000000000000000, 3.0000000000000000 }, {1000000.0000000000000000, 2.0000000000000000, 3.0000000000000000 },
    {4.0000000000000000, 5.0000000000000000, 6.0000000000000000 } };
  static double[] rawRHSvect = new double[] {1, 2, 3, 4 };
  static double[][] rawRHSmat = new double[][] { {1, 2 }, {3, 4 }, {5, 6 }, {7, 8 } };

  RealMatrix condok = new Array2DRowRealMatrix(rawAok);
  LUDecomposition decomp = new LUDecomposition(condok);
  LUDecompositionCommonsResult result = new LUDecompositionCommonsResult(decomp);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkThrowOnNull() {
    new LUDecompositionCommonsResult(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void checkThrowOnSingular() {
    new LUDecompositionCommonsResult(new LUDecomposition(new Array2DRowRealMatrix(rawAsingular)));
  }

  public void testGetL() {
    double[][] expectedRaw = new double[][] { {1.0000000000000000, 0.0000000000000000, 0.0000000000000000, 0.0000000000000000 },
      {0.0900000000000000, 1.0000000000000000, 0.0000000000000000, 0.0000000000000000 }, {0.1000000000000000, 0.2053262858304534, 1.0000000000000000, 0.0000000000000000 },
      {0.0800000000000000, 0.1886562309412482, 0.6500406024227509, 1.0000000000000000 } };
    assertTrue(FuzzyEquals.ArrayFuzzyEquals(result.getL().toArray(), expectedRaw));
  }

  public void testGetU() {
    double[][] expectedRaw = new double[][] { {100.0000000000000000, 9.0000000000000000, 10.0000000000000000, 1.0000000000000000 },
      {0.0000000000000000, 49.1899999999999977, 18.1000000000000014, 14.9100000000000001 }, {0.0000000000000000, 0.0000000000000000, 24.2835942264687930, 17.8385850782679398 },
      {0.0000000000000000, 0.0000000000000000, 0.0000000000000000, 13.5113310060192049 } };
    assertTrue(FuzzyEquals.ArrayFuzzyEquals(result.getU().toArray(), expectedRaw));
  }

  public void testGetDeterminant() {
    assertTrue(FuzzyEquals.SingleValueFuzzyEquals(result.getDeterminant(), 1613942.00000000));
  }

  public void testGetP() {
    double[][] expectedRaw = new double[][] { {1.0000000000000000, 0.0000000000000000, 0.0000000000000000, 0.0000000000000000 },
      {0.0000000000000000, 1.0000000000000000, 0.0000000000000000, 0.0000000000000000 }, {0.0000000000000000, 0.0000000000000000, 1.0000000000000000, 0.0000000000000000 },
      {0.0000000000000000, 0.0000000000000000, 0.0000000000000000, 1.0000000000000000 } };
    assertTrue(FuzzyEquals.ArrayFuzzyEquals(result.getP().toArray(), expectedRaw));
  }

  public void testGetPivot() {
    int[] expectedRaw = new int[] {0, 1, 2, 3 };
    assertTrue(Arrays.equals(result.getPivot(), expectedRaw));
  }

  public void testSolveForVector() {
    double[] expectedRaw = new double[] {0.0090821107573878, -0.0038563963265099, -0.0016307897061976, 0.1428043882617839 };
    assertTrue(FuzzyEquals.ArrayFuzzyEquals(result.solve(rawRHSvect), expectedRaw));

    assertTrue(FuzzyEquals.ArrayFuzzyEquals(result.solve(DoubleArray.copyOf(rawRHSvect)).toArray(), expectedRaw));
  }

  public void testSolveForMatrix() {
    double[][] expectedRaw = new double[][] { {0.0103938059732010, 0.0181642215147756 }, {-0.0147149030138629, -0.0077127926530197 }, {-0.0171480759531631, -0.0032615794123952 },
      {0.2645342893362958, 0.2856087765235678 } };

    assertTrue(FuzzyEquals.ArrayFuzzyEquals(result.solve(DoubleMatrix.copyOf(rawRHSmat)).toArray(), expectedRaw));
  }

}
