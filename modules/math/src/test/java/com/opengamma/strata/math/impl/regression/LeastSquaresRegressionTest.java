/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.regression;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class LeastSquaresRegressionTest {

  @Test
  public void test() {
    final LeastSquaresRegression regression = new OrdinaryLeastSquaresRegression();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> regression.checkData(null, null));
    double[][] x1 = new double[0][0];
    assertThatIllegalArgumentException()
        .isThrownBy(() -> regression.checkData(x1, null));
    double[] y1 = new double[0];
    assertThatIllegalArgumentException()
        .isThrownBy(() -> regression.checkData(x1, (double[]) null, y1));
    double[][] x2 = new double[1][2];
    double[] y2 = new double[3];
    assertThatIllegalArgumentException()
        .isThrownBy(() -> regression.checkData(x2, (double[]) null, y2));
    double[][] x = new double[][] {{1., 2., 3.}, {4., 5.}, {6., 7., 8.}, {9., 0., 0.}};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> regression.checkData(x, (double[]) null, y2));
    x[1] = new double[] {4., 5., 6.};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> regression.checkData(x, (double[]) null, y2));
    double[] y3 = new double[] {1., 2., 3., 4.};
    double[] w11 = new double[0];
    assertThatIllegalArgumentException()
        .isThrownBy(() -> regression.checkData(x, w11, y3));
    double[][] w1 = new double[0][0];
    assertThatIllegalArgumentException()
        .isThrownBy(() -> regression.checkData(x, w1, y3));
    double[] w12 = new double[3];
    assertThatIllegalArgumentException()
        .isThrownBy(() -> regression.checkData(x, w12, y3));
    double[][] w2 = new double[3][0];
    assertThatIllegalArgumentException()
        .isThrownBy(() -> regression.checkData(x, w2, y3));
    double[][] w3 = new double[][] {{1., 2., 3.}, {4., 5.}, {6., 7., 8.}, {9., 0., 0.}};
    assertThatIllegalArgumentException()
        .isThrownBy(() -> regression.checkData(x, w3, y3));
  }
}
