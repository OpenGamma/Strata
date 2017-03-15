/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.regression;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.internal.junit.ArrayAsserts.assertArrayEquals;

import java.util.function.DoubleBinaryOperator;

import org.apache.commons.math3.random.Well44497b;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class LeastSquaresRegressionResultTest {

  private static final LeastSquaresRegression REGRESSION = new OrdinaryLeastSquaresRegression();
  private static final Well44497b RANDOM = new Well44497b(0L);
  private static final LeastSquaresRegressionResult NO_INTERCEPT;
  private static final LeastSquaresRegressionResult INTERCEPT;
  private static final double BETA_0 = 3.9;
  private static final double BETA_1 = -1.4;
  private static final double BETA_2 = 4.6;
  private static final DoubleBinaryOperator F1 = (x1, x2) -> x1 * BETA_1 + x2 * BETA_2;
  private static final DoubleBinaryOperator F2 = (x1, x2) -> BETA_0 + x1 * BETA_1 + x2 * BETA_2;
  private static final double EPS = 1e-9;

  static {
    final int n = 100;
    final double[][] x = new double[n][2];
    final double[] y1 = new double[n];
    final double[] y2 = new double[n];
    for (int i = 0; i < n; i++) {
      x[i][0] = RANDOM.nextDouble();
      x[i][1] = RANDOM.nextDouble();
      y1[i] = F1.applyAsDouble(x[i][0], x[i][1]);
      y2[i] = F2.applyAsDouble(x[i][0], x[i][1]);
    }
    NO_INTERCEPT = REGRESSION.regress(x, null, y1, false);
    INTERCEPT = REGRESSION.regress(x, null, y2, true);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testInputs() {
    new LeastSquaresRegressionResult(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullArray() {
    NO_INTERCEPT.getPredictedValue(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testLongArray() {
    NO_INTERCEPT.getPredictedValue(new double[] {2.4, 2.5, 3.4 });
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testShortArray() {
    NO_INTERCEPT.getPredictedValue(new double[] {2.4 });
  }

  @Test
  public void testPredictedValue() {
    double[] z;
    for (int i = 0; i < 10; i++) {
      z = new double[] {RANDOM.nextDouble(), RANDOM.nextDouble() };
      assertEquals(F1.applyAsDouble(z[0], z[1]), NO_INTERCEPT.getPredictedValue(z), EPS);
      assertEquals(F2.applyAsDouble(z[0], z[1]), INTERCEPT.getPredictedValue(z), EPS);
    }
  }

  @Test
  public void testEqualsAndHashCode() {
    final double[] residuals = new double[] {1, 2, 3 };
    final double[] betas = new double[] {1.1, 2.1, 3.1 };
    final double meanSquareError = 0.78;
    final double[] standardErrorOfBeta = new double[] {1.2, 2.2, 3.2 };
    final double rSquared = 0.98;
    final double rSquaredAdjusted = 0.96;
    final double[] tStats = new double[] {1.3, 2.3, 3.3 };
    final double[] pValues = new double[] {1.4, 2.4, 3.4 };
    final boolean hasIntercept = false;
    final LeastSquaresRegressionResult result = new LeastSquaresRegressionResult(betas, residuals, meanSquareError, standardErrorOfBeta, rSquared, rSquaredAdjusted, tStats, pValues, hasIntercept);
    LeastSquaresRegressionResult other = new LeastSquaresRegressionResult(result);
    assertEquals(result, other);
    assertEquals(result.hashCode(), other.hashCode());
    other = new LeastSquaresRegressionResult(betas, residuals, meanSquareError, standardErrorOfBeta, rSquared, rSquaredAdjusted, tStats, pValues, hasIntercept);
    assertEquals(result, other);
    assertEquals(result.hashCode(), other.hashCode());
    final double[] x = new double[] {1.5, 2.5, 3.5 };
    other = new LeastSquaresRegressionResult(x, residuals, meanSquareError, standardErrorOfBeta, rSquared, rSquaredAdjusted, tStats, pValues, hasIntercept);
    assertFalse(result.equals(other));
    other = new LeastSquaresRegressionResult(betas, x, meanSquareError, standardErrorOfBeta, rSquared, rSquaredAdjusted, tStats, pValues, hasIntercept);
    assertFalse(result.equals(other));
    other = new LeastSquaresRegressionResult(betas, residuals, meanSquareError + 1, standardErrorOfBeta, rSquared, rSquaredAdjusted, tStats, pValues, hasIntercept);
    assertFalse(result.equals(other));
    other = new LeastSquaresRegressionResult(betas, residuals, meanSquareError, x, rSquared, rSquaredAdjusted, tStats, pValues, hasIntercept);
    assertFalse(result.equals(other));
    other = new LeastSquaresRegressionResult(betas, residuals, meanSquareError, standardErrorOfBeta, rSquared + 1, rSquaredAdjusted, tStats, pValues, hasIntercept);
    assertFalse(result.equals(other));
    other = new LeastSquaresRegressionResult(betas, residuals, meanSquareError, standardErrorOfBeta, rSquared, rSquaredAdjusted + 1, tStats, pValues, hasIntercept);
    assertFalse(result.equals(other));
    other = new LeastSquaresRegressionResult(betas, residuals, meanSquareError, standardErrorOfBeta, rSquared, rSquaredAdjusted, x, pValues, hasIntercept);
    assertFalse(result.equals(other));
    other = new LeastSquaresRegressionResult(betas, residuals, meanSquareError, standardErrorOfBeta, rSquared, rSquaredAdjusted, tStats, x, hasIntercept);
    assertFalse(result.equals(other));
    other = new LeastSquaresRegressionResult(betas, residuals, meanSquareError, standardErrorOfBeta, rSquared, rSquaredAdjusted, tStats, pValues, !hasIntercept);
    assertFalse(result.equals(other));
  }

  @Test
  public void testGetters() {
    final double[] residuals = new double[] {1, 2, 3 };
    final double[] betas = new double[] {1.1, 2.1, 3.1 };
    final double meanSquareError = 0.78;
    final double[] standardErrorOfBeta = new double[] {1.2, 2.2, 3.2 };
    final double rSquared = 0.98;
    final double rSquaredAdjusted = 0.96;
    final double[] tStats = new double[] {1.3, 2.3, 3.3 };
    final double[] pValues = new double[] {1.4, 2.4, 3.4 };
    final boolean hasIntercept = false;
    final LeastSquaresRegressionResult result = new LeastSquaresRegressionResult(betas, residuals, meanSquareError, standardErrorOfBeta, rSquared, rSquaredAdjusted, tStats, pValues, hasIntercept);
    assertEquals(result.getAdjustedRSquared(), rSquaredAdjusted, 0);
    assertArrayEquals(result.getBetas(), betas, 0);
    assertEquals(result.getMeanSquareError(), meanSquareError, 0);
    assertArrayEquals(result.getPValues(), pValues, 0);
    assertArrayEquals(result.getResiduals(), residuals, 0);
    assertEquals(result.getRSquared(), rSquared, 0);
    assertArrayEquals(result.getStandardErrorOfBetas(), standardErrorOfBeta, 0);
    assertArrayEquals(result.getTStatistics(), tStats, 0);
  }
}
