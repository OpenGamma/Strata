/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Arrays;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.FuzzyEquals;

/**
 * Tests the LUDecompositionCommonsResult class with well conditioned data and 
 * poorly conditioned data.
 */
public class LUDecompositionCommonsResultTest {

  private static final double[][] RAW_AOK = new double[][] {
      {100.0000000000000000, 9.0000000000000000, 10.0000000000000000, 1.0000000000000000},
      {9.0000000000000000, 50.0000000000000000, 19.0000000000000000, 15.0000000000000000},
      {10.0000000000000000, 11.0000000000000000, 29.0000000000000000, 21.0000000000000000},
      {8.0000000000000000, 10.0000000000000000, 20.0000000000000000, 28.0000000000000000}};
  private static final double[][] RAW_AS_SINGULAR = new double[][] {
      {1000000.0000000000000000, 2.0000000000000000, 3.0000000000000000},
      {1000000.0000000000000000, 2.0000000000000000, 3.0000000000000000},
      {4.0000000000000000, 5.0000000000000000, 6.0000000000000000}};
  private static final double[] RAW_RHS_VECT = new double[] {1, 2, 3, 4};
  private static final double[][] RAW_RHS_MAT = new double[][] {{1, 2}, {3, 4}, {5, 6}, {7, 8}};

  private static final RealMatrix CONDOK = new Array2DRowRealMatrix(RAW_AOK);
  private static final LUDecomposition DECOMP = new LUDecomposition(CONDOK);
  private static final LUDecompositionCommonsResult RESULT = new LUDecompositionCommonsResult(DECOMP);

  @Test
  public void checkThrowOnNull() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new LUDecompositionCommonsResult(null));
  }

  @Test
  public void checkThrowOnSingular() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new LUDecompositionCommonsResult(new LUDecomposition(new Array2DRowRealMatrix(RAW_AS_SINGULAR))));
  }

  @Test
  public void testGetL() {
    double[][] expectedRaw = new double[][] {
        {1.0000000000000000, 0.0000000000000000, 0.0000000000000000, 0.0000000000000000},
        {0.0900000000000000, 1.0000000000000000, 0.0000000000000000, 0.0000000000000000},
        {0.1000000000000000, 0.2053262858304534, 1.0000000000000000, 0.0000000000000000},
        {0.0800000000000000, 0.1886562309412482, 0.6500406024227509, 1.0000000000000000}};
    assertThat(FuzzyEquals.ArrayFuzzyEquals(RESULT.getL().toArray(), expectedRaw)).isTrue();
  }

  @Test
  public void testGetU() {
    double[][] expectedRaw = new double[][] {{100.0000000000000000, 9.0000000000000000, 10.0000000000000000, 1.0000000000000000},
        {0.0000000000000000, 49.1899999999999977, 18.1000000000000014, 14.9100000000000001},
        {0.0000000000000000, 0.0000000000000000, 24.2835942264687930, 17.8385850782679398},
        {0.0000000000000000, 0.0000000000000000, 0.0000000000000000, 13.5113310060192049}};
    assertThat(FuzzyEquals.ArrayFuzzyEquals(RESULT.getU().toArray(), expectedRaw)).isTrue();
  }

  @Test
  public void testGetDeterminant() {
    assertThat(FuzzyEquals.SingleValueFuzzyEquals(RESULT.getDeterminant(), 1613942.00000000)).isTrue();
  }

  @Test
  public void testGetP() {
    double[][] expectedRaw = new double[][] {
        {1.0000000000000000, 0.0000000000000000, 0.0000000000000000, 0.0000000000000000},
        {0.0000000000000000, 1.0000000000000000, 0.0000000000000000, 0.0000000000000000},
        {0.0000000000000000, 0.0000000000000000, 1.0000000000000000, 0.0000000000000000},
        {0.0000000000000000, 0.0000000000000000, 0.0000000000000000, 1.0000000000000000}};
    assertThat(FuzzyEquals.ArrayFuzzyEquals(RESULT.getP().toArray(), expectedRaw)).isTrue();
  }

  @Test
  public void testGetPivot() {
    int[] expectedRaw = new int[] {0, 1, 2, 3};
    assertThat(Arrays.equals(RESULT.getPivot(), expectedRaw)).isTrue();
  }

  @Test
  public void testSolveForVector() {
    double[] expectedRaw = new double[] {0.0090821107573878, -0.0038563963265099, -0.0016307897061976, 0.1428043882617839};
    assertThat(FuzzyEquals.ArrayFuzzyEquals(RESULT.solve(RAW_RHS_VECT), expectedRaw)).isTrue();

    assertThat(FuzzyEquals.ArrayFuzzyEquals(RESULT.solve(DoubleArray.copyOf(RAW_RHS_VECT)).toArray(), expectedRaw)).isTrue();
  }

  @Test
  public void testSolveForMatrix() {
    double[][] expectedRaw = new double[][] {
        {0.0103938059732010, 0.0181642215147756},
        {-0.0147149030138629, -0.0077127926530197},
        {-0.0171480759531631, -0.0032615794123952},
        {0.2645342893362958, 0.2856087765235678}};

    assertThat(FuzzyEquals.ArrayFuzzyEquals(RESULT.solve(DoubleMatrix.copyOf(RAW_RHS_MAT)).toArray(), expectedRaw)).isTrue();
  }

}
