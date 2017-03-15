/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.regression;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class GeneralizedLeastSquaresRegressionTest {
  private static final double EPS = 1e-9;

  @Test
  public void test() {
    final double a0 = 2.3;
    final double a1 = 4.7;
    final double a2 = -0.99;
    final double a3 = -5.1;
    final double a4 = 0.27;
    final int n = 30;
    final double[][] x = new double[n][4];
    final double[] yIntercept = new double[n];
    final double[] yNoIntercept = new double[n];
    final double[][] w = new double[n][n];
    double y, x1, x2, x3, x4;
    for (int i = 0; i < n; i++) {
      x1 = i;
      x2 = x1 * x1;
      x3 = Math.sqrt(x1);
      x4 = x1 * x2;
      x[i] = new double[] {x1, x2, x3, x4 };
      y = x1 * a1 + x2 * a2 + x3 * a3 + x4 * a4;
      yNoIntercept[i] = y;
      yIntercept[i] = y + a0;
      for (int j = 0; j < n; j++) {
        w[i][j] = 0.;
      }
      w[i][i] = 1.;
    }
    final GeneralizedLeastSquaresRegression regression = new GeneralizedLeastSquaresRegression();
    final OrdinaryLeastSquaresRegression olsRegression = new OrdinaryLeastSquaresRegression();
    LeastSquaresRegressionResult gls = regression.regress(x, w, yIntercept, true);
    LeastSquaresRegressionResult ols = olsRegression.regress(x, yIntercept, true);
    assertRegressions(n, 5, gls, ols);
    gls = regression.regress(x, w, yNoIntercept, false);
    ols = olsRegression.regress(x, yNoIntercept, false);
    assertRegressions(n, 4, gls, ols);
    gls = regression.regress(x, w, yIntercept, true);
    ols = olsRegression.regress(x, yIntercept, true);
    assertRegressions(n, 5, gls, ols);
    gls = regression.regress(x, w, yNoIntercept, false);
    ols = olsRegression.regress(x, yNoIntercept, false);
    assertRegressions(n, 4, gls, ols);
  }

  private void assertRegressions(final int n, final int k, final LeastSquaresRegressionResult regression1,
      final LeastSquaresRegressionResult regression2) {
    final double[] r1 = regression1.getResiduals();
    final double[] r2 = regression2.getResiduals();
    for (int i = 0; i < n; i++) {
      assertEquals(r1[i], r2[i], EPS);
    }
    final double[] b1 = regression1.getBetas();
    final double[] b2 = regression2.getBetas();
    for (int i = 0; i < k; i++) {
      assertEquals(b1[i], b2[i], EPS);
    }
  }
}
