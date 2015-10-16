/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResult2D;

/**
 * Test.
 */
@Test
public class PiecewisePolynomialFunction2DTest {

  private static final double EPS = 1e-14;
  private static final double INF = 1. / 0.;

  private static final DoubleArray knots0 = DoubleArray.of(1., 2., 3., 4.);
  private static final DoubleArray knots1 = DoubleArray.of(2., 3., 4.);

  private static final int nKnots0 = 4;
  private static final int nKnots1 = 3;
  private static DoubleMatrix[][] coefs;
  static {
    coefs = new DoubleMatrix[nKnots0 - 1][nKnots1 - 1];
    coefs[0][0] = DoubleMatrix.copyOf(
        new double[][] { {1d, 0d, 0d, 0d}, {0d, 0d, 0d, 0d}, {0d, 0d, 0d, 0d}, {0d, 0d, 0d, 0d}, {0d, 0d, 0d, 0d}});
    coefs[1][0] = DoubleMatrix.copyOf(
        new double[][] { {1d, 0d, 0d, 0d}, {4d, 0d, 0d, 0d}, {6d, 0d, 0d, 0d}, {4d, 0d, 0d, 0d}, {1d, 0d, 0d, 0d}});
    coefs[2][0] = DoubleMatrix.copyOf(
        new double[][] { {1d, 0d, 0d, 0d}, {8d, 0d, 0d, 0d}, {24d, 0d, 0d, 0d}, {32d, 0d, 0d, 0d}, {16d, 0d, 0d, 0d}});
    coefs[0][1] = DoubleMatrix.copyOf(
        new double[][] { {1d, 3d, 3d, 1d}, {0d, 0d, 0d, 0d}, {0d, 0d, 0d, 0d}, {0d, 0d, 0d, 0d}, {0d, 0d, 0d, 0d}});
    coefs[1][1] = DoubleMatrix.copyOf(
        new double[][] {
            {1d, 3d, 3d, 1d},
            {4. * 1d, 4. * 3d, 4. * 3d, 4. * 1d},
            {6. * 1d, 6. * 3d, 6. * 3d, 6. * 1d},
            {4. * 1d, 4. * 3d, 4. * 3d, 4. * 1d},
            {1d, 3d, 3d, 1d}});
    coefs[2][1] = DoubleMatrix.copyOf(
        new double[][] {
            {1d, 3d, 3d, 1d},
            {8. * 1d, 8. * 3d, 8. * 3d, 8. * 1d},
            {24. * 1d, 24. * 3d, 24. * 3d, 24. * 1d},
            {32. * 1d, 32. * 3d, 32. * 3d, 32. * 1d},
            {16. * 1d, 16. * 3d, 16. * 3d, 16. * 1d}
        });
  }

  private static DoubleMatrix[][] coefsConst;
  static {
    coefsConst = new DoubleMatrix[nKnots0 - 1][nKnots1 - 1];
    coefsConst[0][0] = DoubleMatrix.of(1, 1, 4d);
    coefsConst[1][0] = DoubleMatrix.of(1, 1, 4d);
    coefsConst[2][0] = DoubleMatrix.of(1, 1, 4d);
    coefsConst[0][1] = DoubleMatrix.of(1, 1, 4d);
    coefsConst[1][1] = DoubleMatrix.of(1, 1, 4d);
    coefsConst[2][1] = DoubleMatrix.of(1, 1, 4d);
  }

  private static DoubleMatrix[][] coefsLin;
  static {
    coefsLin = new DoubleMatrix[nKnots0 - 1][nKnots1 - 1];
    coefsLin[0][0] = DoubleMatrix.of(2, 2, 1d, 2d, 2d, 4d);
    coefsLin[1][0] = DoubleMatrix.of(2, 2, 1d, 2d, 2d, 4d);
    coefsLin[2][0] = DoubleMatrix.of(2, 2, 1d, 2d, 2d, 4d);
    coefsLin[0][1] = DoubleMatrix.of(2, 2, 1d, 3d, 3d, 9d);
    coefsLin[1][1] = DoubleMatrix.of(2, 2, 1d, 3d, 3d, 9d);
    coefsLin[2][1] = DoubleMatrix.of(2, 2, 1d, 3d, 3d, 9d);
  }

  /**
   * Sample function is f(x,y) = (x-1)^4 * (y-2)^3
   */
  @Test
  public void sampleFunctionTest() {

    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
        assertEquals(values[i][j], valuesExp[i][j], ref * EPS);
      }
    }
    {
      final double value = function.evaluate(result, x0Keys[1], x1Keys[1]);
      final double ref = valuesExp[1][1] == 0. ? 1. : Math.abs(valuesExp[1][1]);
      assertEquals(value, valuesExp[1][1], ref * EPS);
    }
    {
      final double value = function.evaluate(result, x0Keys[n0Keys - 2], x1Keys[n1Keys - 2]);
      final double ref = valuesExp[n0Keys - 2][n1Keys - 2] == 0. ? 1. : Math.abs(valuesExp[n0Keys - 2][n1Keys - 2]);
      assertEquals(value, valuesExp[n0Keys - 2][n1Keys - 2], ref * EPS);
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
        assertEquals(valuesDiffX0[i][j], valuesDiffX0Exp[i][j], ref * EPS);
      }
    }
    {
      final double value = function.differentiateX0(result, x0Keys[1], x1Keys[1]);
      final double ref = valuesDiffX0Exp[1][1] == 0. ? 1. : Math.abs(valuesDiffX0Exp[1][1]);
      assertEquals(value, valuesDiffX0Exp[1][1], ref * EPS);
    }
    {
      final double value = function.differentiateX0(result, x0Keys[n0Keys - 2], x1Keys[n1Keys - 2]);
      final double ref = valuesDiffX0Exp[n0Keys - 2][n1Keys - 2] == 0. ? 1. : Math.abs(valuesDiffX0Exp[n0Keys - 2][n1Keys - 2]);
      assertEquals(value, valuesDiffX0Exp[n0Keys - 2][n1Keys - 2], ref * EPS);
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
        assertEquals(valuesDiffX1[i][j], valuesDiffX1Exp[i][j], ref * EPS);
      }
    }
    {
      final double value = function.differentiateX1(result, x0Keys[1], x1Keys[1]);
      final double ref = valuesDiffX1Exp[1][1] == 0. ? 1. : Math.abs(valuesDiffX1Exp[1][1]);
      assertEquals(value, valuesDiffX1Exp[1][1], ref * EPS);
    }
    {
      final double value = function.differentiateX1(result, x0Keys[n0Keys - 2], x1Keys[n1Keys - 2]);
      final double ref = valuesDiffX1Exp[n0Keys - 2][n1Keys - 2] == 0. ? 1. : Math.abs(valuesDiffX1Exp[n0Keys - 2][n1Keys - 2]);
      assertEquals(value, valuesDiffX1Exp[n0Keys - 2][n1Keys - 2], ref * EPS);
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
        assertEquals(valuesDiffCross[i][j], valuesDiffCrossExp[i][j], ref * EPS);
      }
    }
    {
      final double value = function.differentiateCross(result, x0Keys[1], x1Keys[1]);
      final double ref = valuesDiffCrossExp[1][1] == 0. ? 1. : Math.abs(valuesDiffCrossExp[1][1]);
      assertEquals(value, valuesDiffCrossExp[1][1], ref * EPS);
    }
    {
      final double value = function.differentiateCross(result, x0Keys[n0Keys - 2], x1Keys[n1Keys - 2]);
      final double ref = valuesDiffCrossExp[n0Keys - 2][n1Keys - 2] == 0. ? 1. : Math.abs(valuesDiffCrossExp[n0Keys - 2][n1Keys - 2]);
      assertEquals(value, valuesDiffCrossExp[n0Keys - 2][n1Keys - 2], ref * EPS);
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
        assertEquals(valuesDiffTwiceX0[i][j], valuesDiffTwiceX0Exp[i][j], ref * EPS);
      }
    }
    {
      final double value = function.differentiateTwiceX0(result, x0Keys[1], x1Keys[1]);
      final double ref = valuesDiffTwiceX0Exp[1][1] == 0. ? 1. : Math.abs(valuesDiffTwiceX0Exp[1][1]);
      assertEquals(value, valuesDiffTwiceX0Exp[1][1], ref * EPS);
    }
    {
      final double value = function.differentiateTwiceX0(result, x0Keys[n0Keys - 2], x1Keys[n1Keys - 2]);
      final double ref = valuesDiffTwiceX0Exp[n0Keys - 2][n1Keys - 2] == 0. ? 1. : Math.abs(valuesDiffTwiceX0Exp[n0Keys - 2][n1Keys - 2]);
      assertEquals(value, valuesDiffTwiceX0Exp[n0Keys - 2][n1Keys - 2], ref * EPS);
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
        assertEquals(valuesDiffTwiceX1[i][j], valuesDiffTwiceX1Exp[i][j], ref * EPS);
      }
    }
    {
      final double value = function.differentiateTwiceX1(result, x0Keys[1], x1Keys[1]);
      final double ref = valuesDiffTwiceX1Exp[1][1] == 0. ? 1. : Math.abs(valuesDiffTwiceX1Exp[1][1]);
      assertEquals(value, valuesDiffTwiceX1Exp[1][1], ref * EPS);
    }
    {
      final double value = function.differentiateTwiceX1(result, x0Keys[n0Keys - 2], x1Keys[n1Keys - 2]);
      final double ref = valuesDiffTwiceX1Exp[n0Keys - 2][n1Keys - 2] == 0. ? 1. : Math.abs(valuesDiffTwiceX1Exp[n0Keys - 2][n1Keys - 2]);
      assertEquals(value, valuesDiffTwiceX1Exp[n0Keys - 2][n1Keys - 2], ref * EPS);
    }
  }

  /*
   * PiecewisePolynomialResult2D is null
   */
  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullPpEvaluateTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
    result = null;
    function.evaluate(result, x0Keys[1], x1Keys[1]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullPpEvaluateMultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
    result = null;
    function.evaluate(result, x0Keys, x1Keys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullPpDiffX0Test() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
    result = null;
    function.differentiateX0(result, x0Keys[1], x1Keys[1]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullPpDiffX0MultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
    result = null;
    function.differentiateX0(result, x0Keys, x1Keys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullPpDiffX1Test() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
    result = null;
    function.differentiateX1(result, x0Keys[1], x1Keys[1]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullPpDiffX1MultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
    result = null;
    function.differentiateX1(result, x0Keys, x1Keys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullPpDiffCrossTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
    result = null;
    function.differentiateCross(result, x0Keys[1], x1Keys[1]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullPpDiffCrossMultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
    result = null;
    function.differentiateCross(result, x0Keys, x1Keys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullPpDiffTwiceX0Test() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
    result = null;
    function.differentiateTwiceX0(result, x0Keys[1], x1Keys[1]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullPpDiffTwiceX0MultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
    result = null;
    function.differentiateTwiceX0(result, x0Keys, x1Keys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullPpDiffTwiceX1Test() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
    result = null;
    function.differentiateTwiceX1(result, x0Keys[1], x1Keys[1]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullPpDiffTwiceX1MultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
    result = null;
    function.differentiateTwiceX1(result, x0Keys, x1Keys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void infX0Test() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
    function.evaluate(result, x0Keys[2], x1Keys[2]);
  }

  /*
   * Input contains NaN or infinity
   */
  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanX0Test() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
    function.evaluate(result, x0Keys[3], x1Keys[3]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void infX1Test() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
    function.evaluate(result, x0Keys[2], x1Keys[2]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanX1Test() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
    function.evaluate(result, x0Keys[3], x1Keys[3]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void infX0MultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
    function.evaluate(result, x0Keys, x1Keys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanX0MultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
    function.evaluate(result, x0Keys, x1Keys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void infX1MultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
    function.evaluate(result, x0Keys, x1Keys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanX1MultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefs, new int[] {5, 4 });
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
    function.evaluate(result, x0Keys, x1Keys);
  }

  /*
   * Polynomial degree is too low
   */
  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void constDiffX0Test() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefsConst, new int[] {1, 1 });
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
    function.differentiateX0(result, x0Keys[0], x1Keys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void constDiffX0MultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefsConst, new int[] {1, 1 });
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
    function.differentiateX0(result, x0Keys, x1Keys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void constDiffX1Test() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefsConst, new int[] {1, 1 });
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
    function.differentiateX1(result, x0Keys[0], x1Keys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void constDiffX1MultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefsConst, new int[] {1, 1 });
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
    function.differentiateX1(result, x0Keys, x1Keys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void linearDiffTwiceX0Test() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefsLin, new int[] {2, 2 });
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
    function.differentiateTwiceX0(result, x0Keys[0], x1Keys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void linearDiffTwiceX0MultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefsLin, new int[] {2, 2 });
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
    function.differentiateTwiceX0(result, x0Keys, x1Keys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void linearDiffTwiceX1Test() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefsLin, new int[] {2, 2 });
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
    function.differentiateTwiceX1(result, x0Keys[0], x1Keys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void linearDiffTwiceX1MultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefsLin, new int[] {2, 2 });
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
    function.differentiateTwiceX1(result, x0Keys, x1Keys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void constDiffCrossTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefsConst, new int[] {1, 1 });
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
    function.differentiateCross(result, x0Keys[0], x1Keys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void linConstDiffCrossTest() {

    DoubleMatrix[][] coefsLinConst;
    coefsLinConst = new DoubleMatrix[nKnots0 - 1][nKnots1 - 1];
    coefsLinConst[0][0] = DoubleMatrix.copyOf(new double[][] { {2.}, {2. * 2.}});
    coefsLinConst[1][0] = DoubleMatrix.copyOf(new double[][] { {2.}, {2. * 2.}});
    coefsLinConst[2][0] = DoubleMatrix.copyOf(new double[][] { {2.}, {2. * 2.}});
    coefsLinConst[0][1] = DoubleMatrix.copyOf(new double[][] { {2.}, {3. * 2.}});
    coefsLinConst[1][1] = DoubleMatrix.copyOf(new double[][] { {2.}, {3. * 2.}});
    coefsLinConst[2][1] = DoubleMatrix.copyOf(new double[][] { {2.}, {3. * 2.}});

    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefsLinConst, new int[] {2, 1 });
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
    function.differentiateCross(result, x0Keys[0], x1Keys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void constDiffCrossMultiTest() {
    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefsConst, new int[] {1, 1 });
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
    function.differentiateCross(result, x0Keys, x1Keys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void linConstDiffCrossMultiTest() {

    DoubleMatrix[][] coefsLinConst;
    coefsLinConst = new DoubleMatrix[nKnots0 - 1][nKnots1 - 1];
    coefsLinConst[0][0] = DoubleMatrix.copyOf(new double[][] { {2.}, {2. * 2.}});
    coefsLinConst[1][0] = DoubleMatrix.copyOf(new double[][] { {2.}, {2. * 2.}});
    coefsLinConst[2][0] = DoubleMatrix.copyOf(new double[][] { {2.}, {2. * 2.}});
    coefsLinConst[0][1] = DoubleMatrix.copyOf(new double[][] { {2.}, {3. * 2.}});
    coefsLinConst[1][1] = DoubleMatrix.copyOf(new double[][] { {2.}, {3. * 2.}});
    coefsLinConst[2][1] = DoubleMatrix.copyOf(new double[][] { {2.}, {3. * 2.}});

    PiecewisePolynomialResult2D result = new PiecewisePolynomialResult2D(knots0, knots1, coefsLinConst, new int[] {2, 1 });
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
    function.differentiateCross(result, x0Keys, x1Keys);
  }

}
