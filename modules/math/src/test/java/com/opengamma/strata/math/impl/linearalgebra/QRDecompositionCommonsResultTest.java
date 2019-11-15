/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.QRDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.FuzzyEquals;

/**
 * Tests the QR decomposition result
 */
public class QRDecompositionCommonsResultTest {

  private static final double[][] RAW_AOK = new double[][] {
      {100.0000000000000000, 9.0000000000000000, 10.0000000000000000, 1.0000000000000000},
      {9.0000000000000000, 50.0000000000000000, 19.0000000000000000, 15.0000000000000000},
      {10.0000000000000000, 11.0000000000000000, 29.0000000000000000, 21.0000000000000000},
      {8.0000000000000000, 10.0000000000000000, 20.0000000000000000, 28.0000000000000000}};
  private static final double[] RAW_RHS_VECT = new double[] {1, 2, 3, 4};
  private static final double[][] RAW_RHS_MAT = new double[][] {{1, 2}, {3, 4}, {5, 6}, {7, 8}};

  private static final RealMatrix CONDOK = new Array2DRowRealMatrix(RAW_AOK);
  private static final QRDecomposition QR = new QRDecomposition(CONDOK);
  private static final QRDecompositionCommonsResult RESULT = new QRDecompositionCommonsResult(QR);

  @Test
  public void testThrowOnNull() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new QRDecompositionCommonsResult(null));
  }

  @Test
  public void testGetQ() {
    double[][] expectedRaw = new double[][] {
        {-0.9879705944808324, 0.1189683945357854, 0.0984381737009301, 0.0083994941034602},
        {-0.0889173535032749, -0.9595057553635415, 0.2632608647546777, 0.0462182513606091},
        {-0.0987970594480833, -0.1873133740004731, -0.8116464267094188, 0.5444106161481449},
        {-0.0790376475584666, -0.1735192394127411, -0.5120876107238650, -0.8375024792591188}};
    assertThat(FuzzyEquals.ArrayFuzzyEquals(RESULT.getQ().toArray(), expectedRaw)).isTrue();
  }

  @Test
  public void testGetR() {
    double[][] expectedRaw =
        new double[][] {
            {-101.2175874045612574, -15.2147471550048152, -16.0150033365342956, -6.6095232770767698},
            {0.0000000000000000, -50.7002117254876197, -25.9433980408179785, -23.0657374934840220},
            {0.0000000000000000, 0.0000000000000000, -27.7931604217022681, -27.3356769161449193},
            {0.0000000000000000, 0.0000000000000000, 0.0000000000000000, -11.3157732156316868}};
    assertThat(FuzzyEquals.ArrayFuzzyEquals(RESULT.getR().toArray(), expectedRaw)).isTrue();
  }

  @Test
  public void testGetQT() {
    double[][] expectedRaw = new double[][] {
        {-0.9879705944808324, -0.0889173535032749, -0.0987970594480833, -0.0790376475584666},
        {0.1189683945357854, -0.9595057553635415, -0.1873133740004731, -0.1735192394127411},
        {0.0984381737009301, 0.2632608647546777, -0.8116464267094188, -0.5120876107238650},
        {0.0083994941034602, 0.0462182513606091, 0.5444106161481449, -0.8375024792591188}};
    assertThat(FuzzyEquals.ArrayFuzzyEquals(RESULT.getQT().toArray(), expectedRaw)).isTrue();
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
