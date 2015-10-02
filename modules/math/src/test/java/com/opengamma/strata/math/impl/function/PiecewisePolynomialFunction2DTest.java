/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResult2D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * Test.
 */
@Test
public class PiecewisePolynomialFunction2DTest {

  private static final double EPS = 1e-14;
  private static final double INF = 1. / 0.;

  private static final DoubleMatrix1D knots0 = new DoubleMatrix1D(new double[] {1., 2., 3., 4. });
  private static final DoubleMatrix1D knots1 = new DoubleMatrix1D(new double[] {2., 3., 4. });

  private static final int nKnots0 = 4;
  private static final int nKnots1 = 3;
  private static DoubleMatrix2D[][] coefs;
  static {
    coefs = new DoubleMatrix2D[nKnots0 - 1][nKnots1 - 1];
    coefs[0][0] = new DoubleMatrix2D(new double[][] { {1., 0., 0., 0. }, {0., 0., 0., 0. }, {0., 0., 0., 0. }, {0., 0., 0., 0. }, {0., 0., 0., 0. } });
    coefs[1][0] = new DoubleMatrix2D(new double[][] { {1., 0., 0., 0. }, {4., 0., 0., 0. }, {6., 0., 0., 0. }, {4., 0., 0., 0. }, {1., 0., 0., 0. } });
    coefs[2][0] = new DoubleMatrix2D(new double[][] { {1., 0., 0., 0. }, {8., 0., 0., 0. }, {24., 0., 0., 0. }, {32., 0., 0., 0. }, {16., 0., 0., 0. } });
    coefs[0][1] = new DoubleMatrix2D(new double[][] { {1., 3., 3., 1. }, {0., 0., 0., 0. }, {0., 0., 0., 0. }, {0., 0., 0., 0. }, {0., 0., 0., 0. } });
    coefs[1][1] = new DoubleMatrix2D(new double[][] { {1., 3., 3., 1. }, {4. * 1., 4. * 3., 4. * 3., 4. * 1. }, {6. * 1., 6. * 3., 6. * 3., 6. * 1. }, {4. * 1., 4. * 3., 4. * 3., 4. * 1. },
      {1., 3., 3., 1. } });
    coefs[2][1] = new DoubleMatrix2D(new double[][] { {1., 3., 3., 1. }, {8. * 1., 8. * 3., 8. * 3., 8. * 1. }, {24. * 1., 24. * 3., 24. * 3., 24. * 1. }, {32. * 1., 32. * 3., 32. * 3., 32. * 1. },
      {16. * 1., 16. * 3., 16. * 3., 16. * 1. } });
  }

  private static DoubleMatrix2D[][] coefsConst;
  static {
    coefsConst = new DoubleMatrix2D[nKnots0 - 1][nKnots1 - 1];
    coefsConst[0][0] = new DoubleMatrix2D(new double[][] {{4. } });
    coefsConst[1][0] = new DoubleMatrix2D(new double[][] {{4. } });
    coefsConst[2][0] = new DoubleMatrix2D(new double[][] {{4. } });
    coefsConst[0][1] = new DoubleMatrix2D(new double[][] {{4. } });
    coefsConst[1][1] = new DoubleMatrix2D(new double[][] {{4. } });
    coefsConst[2][1] = new DoubleMatrix2D(new double[][] {{4. } });
  }

  private static DoubleMatrix2D[][] coefsLin;
  static {
    coefsLin = new DoubleMatrix2D[nKnots0 - 1][nKnots1 - 1];
    coefsLin[0][0] = new DoubleMatrix2D(new double[][] { {1., 2. }, {2. * 1., 2. * 2. } });
    coefsLin[1][0] = new DoubleMatrix2D(new double[][] { {1., 2. }, {2. * 1., 2. * 2. } });
    coefsLin[2][0] = new DoubleMatrix2D(new double[][] { {1., 2. }, {2. * 1., 2. * 2. } });
    coefsLin[0][1] = new DoubleMatrix2D(new double[][] { {1., 3. }, {3. * 1., 3. * 3. } });
    coefsLin[1][1] = new DoubleMatrix2D(new double[][] { {1., 3. }, {3. * 1., 3. * 3. } });
    coefsLin[2][1] = new DoubleMatrix2D(new double[][] { {1., 3. }, {3. * 1., 3. * 3. } });
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
    final double[][] values = function.evaluate(result, x0Keys, x1Keys).getData();
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
    final double[][] valuesDiffX0 = function.differentiateX0(result, x0Keys, x1Keys).getData();
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
    final double[][] valuesDiffX1 = function.differentiateX1(result, x0Keys, x1Keys).getData();
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
    final double[][] valuesDiffCross = function.differentiateCross(result, x0Keys, x1Keys).getData();
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
    final double[][] valuesDiffTwiceX0 = function.differentiateTwiceX0(result, x0Keys, x1Keys).getData();
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
    final double[][] valuesDiffTwiceX1 = function.differentiateTwiceX1(result, x0Keys, x1Keys).getData();
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

    DoubleMatrix2D[][] coefsLinConst;
    coefsLinConst = new DoubleMatrix2D[nKnots0 - 1][nKnots1 - 1];
    coefsLinConst[0][0] = new DoubleMatrix2D(new double[][] { {2. }, {2. * 2. } });
    coefsLinConst[1][0] = new DoubleMatrix2D(new double[][] { {2. }, {2. * 2. } });
    coefsLinConst[2][0] = new DoubleMatrix2D(new double[][] { {2. }, {2. * 2. } });
    coefsLinConst[0][1] = new DoubleMatrix2D(new double[][] { {2. }, {3. * 2. } });
    coefsLinConst[1][1] = new DoubleMatrix2D(new double[][] { {2. }, {3. * 2. } });
    coefsLinConst[2][1] = new DoubleMatrix2D(new double[][] { {2. }, {3. * 2. } });

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

    DoubleMatrix2D[][] coefsLinConst;
    coefsLinConst = new DoubleMatrix2D[nKnots0 - 1][nKnots1 - 1];
    coefsLinConst[0][0] = new DoubleMatrix2D(new double[][] { {2. }, {2. * 2. } });
    coefsLinConst[1][0] = new DoubleMatrix2D(new double[][] { {2. }, {2. * 2. } });
    coefsLinConst[2][0] = new DoubleMatrix2D(new double[][] { {2. }, {2. * 2. } });
    coefsLinConst[0][1] = new DoubleMatrix2D(new double[][] { {2. }, {3. * 2. } });
    coefsLinConst[1][1] = new DoubleMatrix2D(new double[][] { {2. }, {3. * 2. } });
    coefsLinConst[2][1] = new DoubleMatrix2D(new double[][] { {2. }, {3. * 2. } });

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
