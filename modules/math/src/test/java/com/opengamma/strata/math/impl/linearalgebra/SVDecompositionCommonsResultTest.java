/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import static org.testng.AssertJUnit.assertTrue;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.FuzzyEquals;

/**
 * Tests the SVD decomposition result
 */
@Test
public class SVDecompositionCommonsResultTest {
  static double[][] rawAok = new double[][] { {100.0000000000000000, 9.0000000000000000, 10.0000000000000000, 1.0000000000000000 },
    {9.0000000000000000, 50.0000000000000000, 19.0000000000000000, 15.0000000000000000 }, {10.0000000000000000, 11.0000000000000000, 29.0000000000000000, 21.0000000000000000 },
    {8.0000000000000000, 10.0000000000000000, 20.0000000000000000, 28.0000000000000000 } };
  static double[] rawRHSvect = new double[] {1, 2, 3, 4 };
  static double[][] rawRHSmat = new double[][] { {1, 2 }, {3, 4 }, {5, 6 }, {7, 8 } };

  RealMatrix condok = new Array2DRowRealMatrix(rawAok);
  SingularValueDecomposition svd = new SingularValueDecomposition(condok);
  SVDecompositionCommonsResult result = new SVDecompositionCommonsResult(svd);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testThrowOnNull() {
    new SVDecompositionCommonsResult(null);
  }

  public void testgetConditionNumber() {
    assertTrue(FuzzyEquals.SingleValueFuzzyEquals(result.getConditionNumber(), 13.3945104660836));
  }

  public void testGetRank() {
    assertTrue(result.getRank() == 4);
  }

  public void testGetNorm() {
    assertTrue(FuzzyEquals.SingleValueFuzzyEquals(result.getNorm(), 105.381175990066));
  }

  public void testGetU() {
    double[][] expectedRaw = new double[][] { {0.9320471908445913, -0.3602912226875036, 0.0345478327447140, -0.0168735338827667 },
      {0.2498760786359523, 0.7104436117020503, 0.6575672695227724, -0.0209070794141986 }, {0.2000644169325719, 0.4343672993809221, -0.5228856286213198, 0.7056131359939679 },
      {0.1697769373079140, 0.4204582722157035, -0.5412969173072171, -0.7080877630614966 } };
    assertTrue(FuzzyEquals.ArrayFuzzyEquals(result.getU().toArray(), expectedRaw));
  }

  public void testGetUT() {
    double[][] expectedRaw = new double[][] { {0.9320471908445913, 0.2498760786359523, 0.2000644169325719, 0.1697769373079140 },
      {-0.3602912226875036, 0.7104436117020503, 0.4343672993809221, 0.4204582722157035 }, {0.0345478327447140, 0.6575672695227724, -0.5228856286213198, -0.5412969173072171 },
      {-0.0168735338827667, -0.0209070794141986, 0.7056131359939679, -0.7080877630614966 } };
    assertTrue(FuzzyEquals.ArrayFuzzyEquals(result.getUT().toArray(), expectedRaw));
  }

  public void testGetS() {
    double[][] expectedRaw = new double[][] { {105.3811759900660974, 0.0000000000000000, 0.0000000000000000, 0.0000000000000000 },
      {0.0000000000000000, 64.1183348155958299, 0.0000000000000000, 0.0000000000000000 }, {0.0000000000000000, 0.0000000000000000, 30.3603275655027289, 0.0000000000000000 },
      {0.0000000000000000, 0.0000000000000000, 0.0000000000000000, 7.8674899136406147 } };

    assertTrue(FuzzyEquals.ArrayFuzzyEquals(result.getS().toArray(), expectedRaw));
  }

  public void testGetSingularValues() {
    double[] expectedRaw = new double[] {105.3811759900660974, 64.1183348155958299, 30.3603275655027289, 7.8674899136406147 };
    assertTrue(FuzzyEquals.ArrayFuzzyEquals(result.getSingularValues(), expectedRaw));
  }

  public void testGetV() {
    double[][] expectedRaw = new double[][] { {0.9376671168414974, -0.3419893959342725, -0.0061377112645623, -0.0615301516583371 },
      {0.2351530657721294, 0.6435317248168104, 0.7254395669946609, -0.0656307050919645 }, {0.2207749536020079, 0.4819422340068079, -0.4331430581376550, 0.7289562360867203 },
      {0.1293902373216524, 0.4864584826101194, -0.5348708763114625, -0.6786232068359547 } };
    assertTrue(FuzzyEquals.ArrayFuzzyEquals(result.getV().toArray(), expectedRaw));
  }

  public void testGetVT() {
    double[][] expectedRaw = new double[][] { {0.9376671168414974, 0.2351530657721294, 0.2207749536020079, 0.1293902373216524 },
      {-0.3419893959342725, 0.6435317248168104, 0.4819422340068079, 0.4864584826101194 }, {-0.0061377112645623, 0.7254395669946609, -0.4331430581376550, -0.5348708763114625 },
      {-0.0615301516583371, -0.0656307050919645, 0.7289562360867203, -0.6786232068359547 } };
    assertTrue(FuzzyEquals.ArrayFuzzyEquals(result.getVT().toArray(), expectedRaw));
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
