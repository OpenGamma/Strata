/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResult2D;

/**
 * Test.
 */
public class PiecewisePolynomialFunction2DTest {

  private static final double EPS = 1e-14;
  private static final double INF = 1. / 0.;

  private static final DoubleArray KNOTS0 = DoubleArray.of(1., 2., 3., 4.);
  private static final DoubleArray KNOTS1 = DoubleArray.of(2., 3., 4.);

  private static final int NKNOTS0 = 4;
  private static final int NKNOTS1 = 3;
  private static DoubleMatrix[][] COEFS;
  static {
    COEFS = new DoubleMatrix[NKNOTS0 - 1][NKNOTS1 - 1];
    COEFS[0][0] = DoubleMatrix.copyOf(
        new double[][] {{1d, 0d, 0d, 0d}, {0d, 0d, 0d, 0d}, {0d, 0d, 0d, 0d}, {0d, 0d, 0d, 0d}, {0d, 0d, 0d, 0d}});
    COEFS[1][0] = DoubleMatrix.copyOf(
        new double[][] {{1d, 0d, 0d, 0d}, {4d, 0d, 0d, 0d}, {6d, 0d, 0d, 0d}, {4d, 0d, 0d, 0d}, {1d, 0d, 0d, 0d}});
    COEFS[2][0] = DoubleMatrix.copyOf(
        new double[][] {{1d, 0d, 0d, 0d}, {8d, 0d, 0d, 0d}, {24d, 0d, 0d, 0d}, {32d, 0d, 0d, 0d}, {16d, 0d, 0d, 0d}});
    COEFS[0][1] = DoubleMatrix.copyOf(
        new double[][] {{1d, 3d, 3d, 1d}, {0d, 0d, 0d, 0d}, {0d, 0d, 0d, 0d}, {0d, 0d, 0d, 0d}, {0d, 0d, 0d, 0d}});
    COEFS[1][1] = DoubleMatrix.copyOf(
        new double[][] {
            {1d, 3d, 3d, 1d},
            {4. * 1d, 4. * 3d, 4. * 3d, 4. * 1d},
            {6. * 1d, 6. * 3d, 6. * 3d, 6. * 1d},
            {4. * 1d, 4. * 3d, 4. * 3d, 4. * 1d},
            {1d, 3d, 3d, 1d}});
    COEFS[2][1] = DoubleMatrix.copyOf(
        new double[][] {
            {1d, 3d, 3d, 1d},
            {8. * 1d, 8. * 3d, 8. * 3d, 8. * 1d},
            {24. * 1d, 24. * 3d, 24. * 3d, 24. * 1d},
            {32. * 1d, 32. * 3d, 32. * 3d, 32. * 1d},
            {16. * 1d, 16. * 3d, 16. * 3d, 16. * 1d}
        });
  }

  private static DoubleMatrix[][] COEFS_CONST;
  static {
    COEFS_CONST = new DoubleMatrix[NKNOTS0 - 1][NKNOTS1 - 1];
    COEFS_CONST[0][0] = DoubleMatrix.of(1, 1, 4d);
    COEFS_CONST[1][0] = DoubleMatrix.of(1, 1, 4d);
    COEFS_CONST[2][0] = DoubleMatrix.of(1, 1, 4d);
    COEFS_CONST[0][1] = DoubleMatrix.of(1, 1, 4d);
    COEFS_CONST[1][1] = DoubleMatrix.of(1, 1, 4d);
    COEFS_CONST[2][1] = DoubleMatrix.of(1, 1, 4d);
  }

  private static DoubleMatrix[][] COEFS_LIN;
  static {
    COEFS_LIN = new DoubleMatrix[NKNOTS0 - 1][NKNOTS1 - 1];
    COEFS_LIN[0][0] = DoubleMatrix.of(2, 2, 1d, 2d, 2d, 4d);
    COEFS_LIN[1][0] = DoubleMatrix.of(2, 2, 1d, 2d, 2d, 4d);
    COEFS_LIN[2][0] = DoubleMatrix.of(2, 2, 1d, 2d, 2d, 4d);
    COEFS_LIN[0][1] = DoubleMatrix.of(2, 2, 1d, 3d, 3d, 9d);
    COEFS_LIN[1][1] = DoubleMatrix.of(2, 2, 1d, 3d, 3d, 9d);
    COEFS_LIN[2][1] = DoubleMatrix.of(2, 2, 1d, 3d, 3d, 9d);
  }

  /**
   * Sample function is f(x,y) = (x-1)^4 * (y-2)^3
   */
  @Test
  public void sampleFunctionTest() {

    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, COEFS, new int[] {5, 4});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }

    /*
     * "Evaluate" test
     */
    double[][] valuesExp = new double[n0Keys][n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      for (int j = 0; j < n1Keys; ++j) {
        valuesExp[i][j] = Math.pow(x0Keys[i] - 1., 4.) * Math.pow(x1Keys[j] - 2., 3.);
      }
    }
    final double[][] values = function.evaluate(result, x0Keys, x1Keys).toArray();
    for (int i = 0; i < n0Keys; ++i) {
      for (int j = 0; j < n1Keys; ++j) {
        final double ref = valuesExp[i][j] == 0. ? 1. : Math.abs(valuesExp[i][j]);
        assertThat(values[i][j]).isCloseTo(valuesExp[i][j], offset(ref * EPS));
      }
    }
    {
      final double value = function.evaluate(result, x0Keys[1], x1Keys[1]);
      final double ref = valuesExp[1][1] == 0. ? 1. : Math.abs(valuesExp[1][1]);
      assertThat(value).isCloseTo(valuesExp[1][1], offset(ref * EPS));
    }
    {
      final double value = function.evaluate(result, x0Keys[n0Keys - 2], x1Keys[n1Keys - 2]);
      final double ref = valuesExp[n0Keys - 2][n1Keys - 2] == 0. ? 1. : Math.abs(valuesExp[n0Keys - 2][n1Keys - 2]);
      assertThat(value).isCloseTo(valuesExp[n0Keys - 2][n1Keys - 2], offset(ref * EPS));
    }

    /*
     * First derivative test
     */
    double[][] valuesDiffX0Exp = new double[n0Keys][n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      for (int j = 0; j < n1Keys; ++j) {
        valuesDiffX0Exp[i][j] = 4. * Math.pow(x0Keys[i] - 1., 3.) * Math.pow(x1Keys[j] - 2., 3.);
      }
    }
    final double[][] valuesDiffX0 = function.differentiateX0(result, x0Keys, x1Keys).toArray();
    for (int i = 0; i < n0Keys; ++i) {
      for (int j = 0; j < n1Keys; ++j) {
        final double ref = valuesDiffX0Exp[i][j] == 0. ? 1. : Math.abs(valuesDiffX0Exp[i][j]);
        assertThat(valuesDiffX0[i][j]).isCloseTo(valuesDiffX0Exp[i][j], offset(ref * EPS));
      }
    }
    {
      final double value = function.differentiateX0(result, x0Keys[1], x1Keys[1]);
      final double ref = valuesDiffX0Exp[1][1] == 0. ? 1. : Math.abs(valuesDiffX0Exp[1][1]);
      assertThat(value).isCloseTo(valuesDiffX0Exp[1][1], offset(ref * EPS));
    }
    {
      final double value = function.differentiateX0(result, x0Keys[n0Keys - 2], x1Keys[n1Keys - 2]);
      final double ref = valuesDiffX0Exp[n0Keys - 2][n1Keys - 2] == 0. ? 1. : Math.abs(valuesDiffX0Exp[n0Keys - 2][n1Keys - 2]);
      assertThat(value).isCloseTo(valuesDiffX0Exp[n0Keys - 2][n1Keys - 2], offset(ref * EPS));
    }

    double[][] valuesDiffX1Exp = new double[n0Keys][n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      for (int j = 0; j < n1Keys; ++j) {
        valuesDiffX1Exp[i][j] = Math.pow(x0Keys[i] - 1., 4.) * Math.pow(x1Keys[j] - 2., 2.) * 3.;
      }
    }
    final double[][] valuesDiffX1 = function.differentiateX1(result, x0Keys, x1Keys).toArray();
    for (int i = 0; i < n0Keys; ++i) {
      for (int j = 0; j < n1Keys; ++j) {
        final double ref = valuesDiffX1Exp[i][j] == 0. ? 1. : Math.abs(valuesDiffX1Exp[i][j]);
        assertThat(valuesDiffX1[i][j]).isCloseTo(valuesDiffX1Exp[i][j], offset(ref * EPS));
      }
    }
    {
      final double value = function.differentiateX1(result, x0Keys[1], x1Keys[1]);
      final double ref = valuesDiffX1Exp[1][1] == 0. ? 1. : Math.abs(valuesDiffX1Exp[1][1]);
      assertThat(value).isCloseTo(valuesDiffX1Exp[1][1], offset(ref * EPS));
    }
    {
      final double value = function.differentiateX1(result, x0Keys[n0Keys - 2], x1Keys[n1Keys - 2]);
      final double ref = valuesDiffX1Exp[n0Keys - 2][n1Keys - 2] == 0. ? 1. : Math.abs(valuesDiffX1Exp[n0Keys - 2][n1Keys - 2]);
      assertThat(value).isCloseTo(valuesDiffX1Exp[n0Keys - 2][n1Keys - 2], offset(ref * EPS));
    }

    /*
     * Second derivative test
     */
    double[][] valuesDiffCrossExp = new double[n0Keys][n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      for (int j = 0; j < n1Keys; ++j) {
        valuesDiffCrossExp[i][j] = 4. * Math.pow(x0Keys[i] - 1., 3.) * 3. * Math.pow(x1Keys[j] - 2., 2.);
      }
    }
    final double[][] valuesDiffCross = function.differentiateCross(result, x0Keys, x1Keys).toArray();
    for (int i = 0; i < n0Keys; ++i) {
      for (int j = 0; j < n1Keys; ++j) {
        final double ref = valuesDiffCrossExp[i][j] == 0. ? 1. : Math.abs(valuesDiffCrossExp[i][j]);
        assertThat(valuesDiffCross[i][j]).isCloseTo(valuesDiffCrossExp[i][j], offset(ref * EPS));
      }
    }
    {
      final double value = function.differentiateCross(result, x0Keys[1], x1Keys[1]);
      final double ref = valuesDiffCrossExp[1][1] == 0. ? 1. : Math.abs(valuesDiffCrossExp[1][1]);
      assertThat(value).isCloseTo(valuesDiffCrossExp[1][1], offset(ref * EPS));
    }
    {
      final double value = function.differentiateCross(result, x0Keys[n0Keys - 2], x1Keys[n1Keys - 2]);
      final double ref =
          valuesDiffCrossExp[n0Keys - 2][n1Keys - 2] == 0. ? 1. : Math.abs(valuesDiffCrossExp[n0Keys - 2][n1Keys - 2]);
      assertThat(value).isCloseTo(valuesDiffCrossExp[n0Keys - 2][n1Keys - 2], offset(ref * EPS));
    }

    double[][] valuesDiffTwiceX0Exp = new double[n0Keys][n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      for (int j = 0; j < n1Keys; ++j) {
        valuesDiffTwiceX0Exp[i][j] = 4. * 3. * Math.pow(x0Keys[i] - 1., 2.) * Math.pow(x1Keys[j] - 2., 3.);
      }
    }
    final double[][] valuesDiffTwiceX0 = function.differentiateTwiceX0(result, x0Keys, x1Keys).toArray();
    for (int i = 0; i < n0Keys; ++i) {
      for (int j = 0; j < n1Keys; ++j) {
        final double ref = valuesDiffTwiceX0Exp[i][j] == 0. ? 1. : Math.abs(valuesDiffTwiceX0Exp[i][j]);
        assertThat(valuesDiffTwiceX0[i][j]).isCloseTo(valuesDiffTwiceX0Exp[i][j], offset(ref * EPS));
      }
    }
    {
      final double value = function.differentiateTwiceX0(result, x0Keys[1], x1Keys[1]);
      final double ref = valuesDiffTwiceX0Exp[1][1] == 0. ? 1. : Math.abs(valuesDiffTwiceX0Exp[1][1]);
      assertThat(value).isCloseTo(valuesDiffTwiceX0Exp[1][1], offset(ref * EPS));
    }
    {
      final double value = function.differentiateTwiceX0(result, x0Keys[n0Keys - 2], x1Keys[n1Keys - 2]);
      final double ref =
          valuesDiffTwiceX0Exp[n0Keys - 2][n1Keys - 2] == 0. ? 1. : Math.abs(valuesDiffTwiceX0Exp[n0Keys - 2][n1Keys - 2]);
      assertThat(value).isCloseTo(valuesDiffTwiceX0Exp[n0Keys - 2][n1Keys - 2], offset(ref * EPS));
    }

    double[][] valuesDiffTwiceX1Exp = new double[n0Keys][n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      for (int j = 0; j < n1Keys; ++j) {
        valuesDiffTwiceX1Exp[i][j] = Math.pow(x0Keys[i] - 1., 4.) * Math.pow(x1Keys[j] - 2., 1.) * 3. * 2.;
      }
    }
    final double[][] valuesDiffTwiceX1 = function.differentiateTwiceX1(result, x0Keys, x1Keys).toArray();
    for (int i = 0; i < n0Keys; ++i) {
      for (int j = 0; j < n1Keys; ++j) {
        final double ref = valuesDiffTwiceX1Exp[i][j] == 0. ? 1. : Math.abs(valuesDiffTwiceX1Exp[i][j]);
        assertThat(valuesDiffTwiceX1[i][j]).isCloseTo(valuesDiffTwiceX1Exp[i][j], offset(ref * EPS));
      }
    }
    {
      final double value = function.differentiateTwiceX1(result, x0Keys[1], x1Keys[1]);
      final double ref = valuesDiffTwiceX1Exp[1][1] == 0. ? 1. : Math.abs(valuesDiffTwiceX1Exp[1][1]);
      assertThat(value).isCloseTo(valuesDiffTwiceX1Exp[1][1], offset(ref * EPS));
    }
    {
      final double value = function.differentiateTwiceX1(result, x0Keys[n0Keys - 2], x1Keys[n1Keys - 2]);
      final double ref =
          valuesDiffTwiceX1Exp[n0Keys - 2][n1Keys - 2] == 0. ? 1. : Math.abs(valuesDiffTwiceX1Exp[n0Keys - 2][n1Keys - 2]);
      assertThat(value).isCloseTo(valuesDiffTwiceX1Exp[n0Keys - 2][n1Keys - 2], offset(ref * EPS));
    }
  }

  /*
   * PiecewisePolynomialResult2D is null
   */
  /**
   * 
   */
  @Test
  public void nullPpEvaluateTest() {
    PiecewisePolynomialResult2D result = null;
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.evaluate(result, x0Keys[1], x1Keys[1]));
  }

  /**
   * 
   */
  @Test
  public void nullPpEvaluateMultiTest() {
    PiecewisePolynomialResult2D result = null;
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.evaluate(result, x0Keys, x1Keys));
  }

  /**
   * 
   */
  @Test
  public void nullPpDiffX0Test() {
    PiecewisePolynomialResult2D result = null;
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateX0(result, x0Keys[1], x1Keys[1]));
  }

  /**
   * 
   */
  @Test
  public void nullPpDiffX0MultiTest() {
    PiecewisePolynomialResult2D result = null;
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateX0(result, x0Keys, x1Keys));
  }

  /**
   * 
   */
  @Test
  public void nullPpDiffX1Test() {
    PiecewisePolynomialResult2D result = null;
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateX1(result, x0Keys[1], x1Keys[1]));
  }

  /**
   * 
   */
  @Test
  public void nullPpDiffX1MultiTest() {
    PiecewisePolynomialResult2D result = null;
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateX1(result, x0Keys, x1Keys));
  }

  /**
   * 
   */
  @Test
  public void nullPpDiffCrossTest() {
    PiecewisePolynomialResult2D result = null;
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateCross(result, x0Keys[1], x1Keys[1]));
  }

  /**
   * 
   */
  @Test
  public void nullPpDiffCrossMultiTest() {
    PiecewisePolynomialResult2D result = null;
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateCross(result, x0Keys, x1Keys));
  }

  /**
   * 
   */
  @Test
  public void nullPpDiffTwiceX0Test() {
    PiecewisePolynomialResult2D result = null;
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateTwiceX0(result, x0Keys[1], x1Keys[1]));
  }

  /**
   * 
   */
  @Test
  public void nullPpDiffTwiceX0MultiTest() {
    PiecewisePolynomialResult2D result = null;
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateTwiceX0(result, x0Keys, x1Keys));
  }

  /**
   * 
   */
  @Test
  public void nullPpDiffTwiceX1Test() {
    PiecewisePolynomialResult2D result = null;
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateTwiceX1(result, x0Keys[1], x1Keys[1]));
  }

  /**
   * 
   */
  @Test
  public void nullPpDiffTwiceX1MultiTest() {
    PiecewisePolynomialResult2D result = null;
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateTwiceX1(result, x0Keys, x1Keys));
  }

  /**
   * 
   */
  @Test
  public void infX0Test() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, COEFS, new int[] {5, 4});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    x0Keys[2] = INF;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.evaluate(result, x0Keys[2], x1Keys[2]));
  }

  /*
   * Input contains NaN or infinity
   */
  /**
   * 
   */
  @Test
  public void nanX0Test() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, COEFS, new int[] {5, 4});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    x0Keys[3] = Double.NaN;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.evaluate(result, x0Keys[3], x1Keys[3]));
  }

  /**
   * 
   */
  @Test
  public void infX1Test() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, COEFS, new int[] {5, 4});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    x1Keys[2] = INF;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.evaluate(result, x0Keys[2], x1Keys[2]));
  }

  /**
   * 
   */
  @Test
  public void nanX1Test() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, COEFS, new int[] {5, 4});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    x1Keys[3] = Double.NaN;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.evaluate(result, x0Keys[3], x1Keys[3]));
  }

  /**
   * 
   */
  @Test
  public void infX0MultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, COEFS, new int[] {5, 4});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    x0Keys[2] = INF;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.evaluate(result, x0Keys, x1Keys));
  }

  /**
   * 
   */
  @Test
  public void nanX0MultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, COEFS, new int[] {5, 4});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    x0Keys[3] = Double.NaN;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.evaluate(result, x0Keys, x1Keys));
  }

  /**
   * 
   */
  @Test
  public void infX1MultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, COEFS, new int[] {5, 4});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    x1Keys[2] = INF;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.evaluate(result, x0Keys, x1Keys));
  }

  /**
   * 
   */
  @Test
  public void nanX1MultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, COEFS, new int[] {5, 4});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    x1Keys[3] = Double.NaN;
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.evaluate(result, x0Keys, x1Keys));
  }

  /*
   * Polynomial degree is too low
   */
  /**
   * 
   */
  @Test
  public void constDiffX0Test() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, COEFS_CONST, new int[] {1, 1});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateX0(result, x0Keys[0], x1Keys[0]));
  }

  /**
   * 
   */
  @Test
  public void constDiffX0MultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, COEFS_CONST, new int[] {1, 1});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateX0(result, x0Keys, x1Keys));
  }

  /**
   * 
   */
  @Test
  public void constDiffX1Test() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, COEFS_CONST, new int[] {1, 1});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateX1(result, x0Keys[0], x1Keys[0]));
  }

  /**
   * 
   */
  @Test
  public void constDiffX1MultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, COEFS_CONST, new int[] {1, 1});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateX1(result, x0Keys, x1Keys));
  }

  /**
   * 
   */
  @Test
  public void linearDiffTwiceX0Test() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, COEFS_LIN, new int[] {2, 2});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateTwiceX0(result, x0Keys[0], x1Keys[0]));
  }

  /**
   * 
   */
  @Test
  public void linearDiffTwiceX0MultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, COEFS_LIN, new int[] {2, 2});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateTwiceX0(result, x0Keys, x1Keys));
  }

  /**
   * 
   */
  @Test
  public void linearDiffTwiceX1Test() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, COEFS_LIN, new int[] {2, 2});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateTwiceX1(result, x0Keys[0], x1Keys[0]));
  }

  /**
   * 
   */
  @Test
  public void linearDiffTwiceX1MultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, COEFS_LIN, new int[] {2, 2});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateTwiceX1(result, x0Keys, x1Keys));
  }

  /**
   * 
   */
  @Test
  public void constDiffCrossTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, COEFS_CONST, new int[] {1, 1});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateCross(result, x0Keys[0], x1Keys[0]));
  }

  /**
   * 
   */
  @Test
  public void linConstDiffCrossTest() {

    DoubleMatrix[][] coefsLinConst;
    coefsLinConst = new DoubleMatrix[NKNOTS0 - 1][NKNOTS1 - 1];
    coefsLinConst[0][0] = DoubleMatrix.copyOf(new double[][] {{2.}, {2. * 2.}});
    coefsLinConst[1][0] = DoubleMatrix.copyOf(new double[][] {{2.}, {2. * 2.}});
    coefsLinConst[2][0] = DoubleMatrix.copyOf(new double[][] {{2.}, {2. * 2.}});
    coefsLinConst[0][1] = DoubleMatrix.copyOf(new double[][] {{2.}, {3. * 2.}});
    coefsLinConst[1][1] = DoubleMatrix.copyOf(new double[][] {{2.}, {3. * 2.}});
    coefsLinConst[2][1] = DoubleMatrix.copyOf(new double[][] {{2.}, {3. * 2.}});

    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, coefsLinConst, new int[] {2, 1});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateCross(result, x0Keys[0], x1Keys[0]));
  }

  /**
   * 
   */
  @Test
  public void constDiffCrossMultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, COEFS_CONST, new int[] {1, 1});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateCross(result, x0Keys, x1Keys));
  }

  /**
   * 
   */
  @Test
  public void linConstDiffCrossMultiTest() {

    DoubleMatrix[][] coefsLinConst;
    coefsLinConst = new DoubleMatrix[NKNOTS0 - 1][NKNOTS1 - 1];
    coefsLinConst[0][0] = DoubleMatrix.copyOf(new double[][] {{2.}, {2. * 2.}});
    coefsLinConst[1][0] = DoubleMatrix.copyOf(new double[][] {{2.}, {2. * 2.}});
    coefsLinConst[2][0] = DoubleMatrix.copyOf(new double[][] {{2.}, {2. * 2.}});
    coefsLinConst[0][1] = DoubleMatrix.copyOf(new double[][] {{2.}, {3. * 2.}});
    coefsLinConst[1][1] = DoubleMatrix.copyOf(new double[][] {{2.}, {3. * 2.}});
    coefsLinConst[2][1] = DoubleMatrix.copyOf(new double[][] {{2.}, {3. * 2.}});

    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(KNOTS0, KNOTS1, coefsLinConst, new int[] {2, 1});
    PiecewisePolynomialFunction2D function = new PiecewisePolynomialFunction2D();

    final int n0Keys = 21;
    final int n1Keys = 31;
    double[] x0Keys = new double[n0Keys];
    double[] x1Keys = new double[n1Keys];
    for (int i = 0; i < n0Keys; ++i) {
      x0Keys[i] = 0. + 4. / (n0Keys - 1) * i;
    }
    for (int i = 0; i < n1Keys; ++i) {
      x1Keys[i] = 1. + 3. / (n1Keys - 1) * i;
    }
    assertThatIllegalArgumentException()
        .isThrownBy(() -> function.differentiateCross(result, x0Keys, x1Keys));
  }

}
