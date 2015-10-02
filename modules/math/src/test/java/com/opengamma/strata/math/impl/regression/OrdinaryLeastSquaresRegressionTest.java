/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.regression;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import org.apache.commons.math3.random.Well44497b;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class OrdinaryLeastSquaresRegressionTest {

  private static final LeastSquaresRegression REGRESSION = new OrdinaryLeastSquaresRegression();
  private static final Well44497b RANDOM = new Well44497b(0L);
  private static final double EPS = 1e-2;
  private static final double FACTOR = 1. / EPS;

  @Test
  public void test() {
    final int n = 20;
    final double[][] x = new double[n][5];
    final double[] y1 = new double[n];
    final double[] y2 = new double[n];
    final double[] a1 = new double[] {3.4, 1.2, -0.62, -0.44, 0.65 };
    final double[] a2 = new double[] {0.98, 3.4, 1.2, -0.62, -0.44, 0.65 };
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < 5; j++) {
        x[i][j] = RANDOM.nextDouble() + (RANDOM.nextDouble() - 0.5) / FACTOR;
      }
      y1[i] = a1[0] * x[i][0] + a1[1] * x[i][1] + a1[2] * x[i][2] + a1[3] * x[i][3] + a1[4] * x[i][4]
          + RANDOM.nextDouble() / FACTOR;
      y2[i] = a2[0] + a2[1] * x[i][0] + a2[2] * x[i][1] + a2[3] * x[i][2] + a2[4] * x[i][3] + a2[5] * x[i][4]
          + RANDOM.nextDouble() / FACTOR;
    }
    final LeastSquaresRegressionResult result1 = REGRESSION.regress(x, null, y1, false);
    final LeastSquaresRegressionResult result2 = REGRESSION.regress(x, null, y2, true);
    assertRegression(result1, a1);
    assertRegression(result2, a2);
    final double[] residuals1 = result1.getResiduals();
    for (int i = 0; i < n; i++) {
      assertEquals(y1[i], a1[0] * x[i][0] + a1[1] * x[i][1] + a1[2] * x[i][2] + a1[3] * x[i][3] + a1[4] * x[i][4]
          + residuals1[i], 10 * EPS);
    }
    final double[] residuals2 = result2.getResiduals();
    for (int i = 0; i < n; i++) {
      assertEquals(y2[i], a2[0] + a2[1] * x[i][0] + a2[2] * x[i][1] + a2[3] * x[i][2] + a2[4] * x[i][3] + a2[5]
          * x[i][4] + residuals2[i], 10 * EPS);
    }
  }

  private void assertRegression(final LeastSquaresRegressionResult result, final double[] a) {
    final double[] beta = result.getBetas();
    final double[] tStat = result.getTStatistics();
    final double[] pStat = result.getPValues();
    final double[] stdErr = result.getStandardErrorOfBetas();
    for (int i = 0; i < 5; i++) {
      assertEquals(beta[i], a[i], EPS);
      assertTrue(Math.abs(tStat[i]) > FACTOR);
      assertTrue(pStat[i] < EPS);
      assertTrue(stdErr[i] < EPS);
    }
    assertEquals(result.getRSquared(), 1, EPS);
    assertEquals(result.getAdjustedRSquared(), 1, EPS);
  }
}
