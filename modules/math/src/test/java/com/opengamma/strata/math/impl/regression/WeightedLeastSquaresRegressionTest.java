/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.regression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.apache.commons.math3.random.Well44497b;
import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class WeightedLeastSquaresRegressionTest {

  private static final Well44497b RANDOM = new Well44497b(0L);
  private static final double EPS = 1e-2;

  @Test
  public void test() {
    final double a0 = 2.3;
    final double a1 = -4.5;
    final double a2 = 0.76;
    final double a3 = 3.4;
    final int n = 30;
    final double[][] x = new double[n][3];
    final double[] yIntercept = new double[n];
    final double[] yNoIntercept = new double[n];
    final double[][] w1 = new double[n][n];
    final double[] w2 = new double[n];
    double y, x1, x2, x3;
    for (int i = 0; i < n; i++) {
      x1 = i;
      x2 = x1 * x1;
      x3 = Math.sqrt(x1);
      x[i] = new double[] {x1, x2, x3};
      y = x1 * a1 + x2 * a2 + x3 * a3;
      yNoIntercept[i] = y;
      yIntercept[i] = y + a0;
      for (int j = 0; j < n; j++) {
        w1[i][j] = RANDOM.nextDouble();
      }
      w1[i][i] = 1.;
      w2[i] = 1.;
    }
    final WeightedLeastSquaresRegression wlsRegression = new WeightedLeastSquaresRegression();
    final OrdinaryLeastSquaresRegression olsRegression = new OrdinaryLeastSquaresRegression();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> wlsRegression.regress(x, (double[]) null, yNoIntercept, false));
    LeastSquaresRegressionResult wls = wlsRegression.regress(x, w1, yIntercept, true);
    LeastSquaresRegressionResult ols = olsRegression.regress(x, yIntercept, true);
    assertRegressions(n, 4, wls, ols);
    wls = wlsRegression.regress(x, w1, yNoIntercept, false);
    ols = olsRegression.regress(x, yNoIntercept, false);
    assertRegressions(n, 3, wls, ols);
    wls = wlsRegression.regress(x, w2, yIntercept, true);
    ols = olsRegression.regress(x, yIntercept, true);
    assertRegressions(n, 4, wls, ols);
    wls = wlsRegression.regress(x, w2, yNoIntercept, false);
    ols = olsRegression.regress(x, yNoIntercept, false);
    assertRegressions(n, 3, wls, ols);
  }

  private void assertRegressions(final int n, final int k, final LeastSquaresRegressionResult regression1,
      final LeastSquaresRegressionResult regression2) {
    final double[] r1 = regression1.getResiduals();
    final double[] r2 = regression2.getResiduals();
    for (int i = 0; i < n; i++) {
      assertThat(r1[i]).isCloseTo(r2[i], offset(EPS));
    }
    final double[] b1 = regression1.getBetas();
    final double[] t1 = regression1.getTStatistics();
    final double[] p1 = regression1.getPValues();
    final double[] s1 = regression1.getStandardErrorOfBetas();
    final double[] b2 = regression2.getBetas();
    final double[] t2 = regression2.getTStatistics();
    final double[] p2 = regression2.getPValues();
    final double[] s2 = regression2.getStandardErrorOfBetas();
    for (int i = 0; i < k; i++) {
      assertThat(b1[i]).isCloseTo(b2[i], offset(EPS));
      assertThat(t1[i]).isCloseTo(t2[i], offset(EPS));
      assertThat(p1[i]).isCloseTo(p2[i], offset(EPS));
      assertThat(s1[i]).isCloseTo(s2[i], offset(EPS));
    }
    assertThat(regression1.getRSquared()).isCloseTo(regression2.getRSquared(), offset(EPS));
    assertThat(regression1.getAdjustedRSquared()).isCloseTo(regression2.getAdjustedRSquared(), offset(EPS));
    assertThat(regression1.getMeanSquareError()).isCloseTo(regression2.getMeanSquareError(), offset(EPS));
  }
}
