/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResult;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix1D;
import com.opengamma.strata.math.impl.matrix.DoubleMatrix2D;

/**
 * Test.
 */
@Test
public class PiecewisePolynomialFunction1DTest {

  private static final double EPS = 1e-14;
  private static final double INF = 1. / 0.;

  /**
   * 
   */
  @Test
  public void evaluateAllTest() {
    final double[] xValues = new double[] {1, 2, 3, 4 };
    final DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    final double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final double[][][] valuesExp = new double[][][] { { {-64., -1., 0., 1. / 8. }, {-1. / 8., 1. / 27., 3375. / 7. / 7. / 7., 27. } },
      { {125., 20., 5., 5. / 4. }, {45. / 4., 20. / 9., 2240. / 7. / 7. / 7., 20. } } };
    final int dim = 2;
    final int nCoefs = 4;
    final int keyLength = xKeys[0].length;
    final int keyDim = xKeys.length;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    final DoubleMatrix2D[] valuesResMat = function.evaluate(pp, xKeys);
    for (int i = 0; i < dim; ++i) {
      for (int k = 0; k < keyDim; ++k) {
        for (int j = 0; j < keyLength; ++j) {
          final double ref = valuesExp[i][k][j] == 0. ? 1. : Math.abs(valuesExp[i][k][j]);
          assertEquals(valuesResMat[i].getData()[k][j], valuesExp[i][k][j], ref * EPS);
        }
      }
    }

    final double[][] valuesRes = function.evaluate(pp, xKeys[0]).getData();
    for (int i = 0; i < dim; ++i) {
      for (int j = 0; j < keyLength; ++j) {
        final double ref = valuesExp[i][0][j] == 0. ? 1. : Math.abs(valuesExp[i][0][j]);
        assertEquals(valuesRes[i][j], valuesExp[i][0][j], ref * EPS);
      }
    }

    double[] valuesResVec = function.evaluate(pp, xKeys[0][0]).getData();
    for (int i = 0; i < dim; ++i) {
      final double ref = valuesExp[i][0][0] == 0. ? 1. : Math.abs(valuesExp[i][0][0]);
      assertEquals(valuesResVec[i], valuesExp[i][0][0], ref * EPS);
    }

    valuesResVec = function.evaluate(pp, xKeys[0][3]).getData();
    for (int i = 0; i < dim; ++i) {
      final double ref = valuesExp[i][0][3] == 0. ? 1. : Math.abs(valuesExp[i][0][3]);
      assertEquals(valuesResVec[i], valuesExp[i][0][3], ref * EPS);
    }

  }

  /**
   * 
   */
  @Test
  public void linearAllTest() {

    final double[] knots = new double[] {1., 4. };
    final DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] {{0., 1., 1. } });
    final double[] xKeys = new double[] {-2, 1., 2.5, 4. };
    final double[] initials = new double[] {-0.5, 1., 2.5, 5. };
    final int nKeys = xKeys.length;
    final int nInit = initials.length;

    final double[] valuesExp = new double[] {-2, 1, 2.5, 4. };
    final double[][] integrateExp = new double[nInit][nKeys];
    for (int i = 0; i < nInit; ++i) {
      for (int j = 0; j < nKeys; ++j) {
        integrateExp[i][j] = 0.5 * (xKeys[j] * xKeys[j] - initials[i] * initials[i]);
      }
    }
    final double[] differentiateExp = new double[] {1., 1., 1., 1. };

    PiecewisePolynomialResult result = new PiecewisePolynomialResult(new DoubleMatrix1D(knots), coefsMatrix, 3, 1);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    final double[] values = function.evaluate(result, xKeys).getData()[0];
    final double[] differentiate = function.differentiate(result, xKeys).getData()[0];
    final double[][] integrate = new double[nInit][nKeys];
    for (int i = 0; i < nInit; ++i) {
      for (int j = 0; j < nKeys; ++j) {
        integrate[i][j] = function.integrate(result, initials[i], xKeys).getData()[j];
      }
    }

    for (int i = 0; i < nKeys; ++i) {
      final double ref = valuesExp[i] == 0. ? 1. : Math.abs(valuesExp[i]);
      assertEquals(values[i], valuesExp[i], ref * EPS);
    }

    for (int i = 0; i < nKeys; ++i) {
      final double ref = differentiateExp[i] == 0. ? 1. : Math.abs(differentiateExp[i]);
      assertEquals(differentiate[i], differentiateExp[i], ref * EPS);
    }

    for (int j = 0; j < nInit; ++j) {
      for (int i = 0; i < nKeys; ++i) {
        final double ref = integrateExp[j][i] == 0. ? 1. : Math.abs(integrateExp[j][i]);
        assertEquals(integrate[j][i], integrateExp[j][i], ref * EPS);
      }
    }
  }

  /**
   * 
   */
  @Test
  public void quadraticAllTest() {

    final double[] knots = new double[] {1., 3. };
    final DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] {{-1., 2., 1. } });
    final double[] xKeys = new double[] {-2, 1, 2.5, 4. };
    final double[] initials = new double[] {-0.5, 1., 2.5, 5. };
    final int nKeys = xKeys.length;
    final int nInit = initials.length;

    final double[] valuesExp = new double[] {-14., 1., 7. / 4., -2. };
    final double[][] integrateExp = new double[nInit][nKeys];
    for (int i = 0; i < nInit; ++i) {
      for (int j = 0; j < nKeys; ++j) {
        integrateExp[i][j] = -1. / 3. * (xKeys[j] - initials[i]) * (xKeys[j] * xKeys[j] + initials[i] * initials[i] - 6. * xKeys[j] - 6. * initials[i] + 6. + initials[i] * xKeys[j]);
      }
    }
    final double[] differentiateExp = new double[nKeys];
    for (int j = 0; j < nKeys; ++j) {
      differentiateExp[j] = -2. * (xKeys[j] - 1) + 2.;
    }
    final double[] differentiateTwiceExp = new double[nKeys];
    for (int j = 0; j < nKeys; ++j) {
      differentiateTwiceExp[j] = -2.;
    }

    PiecewisePolynomialResult result = new PiecewisePolynomialResult(new DoubleMatrix1D(knots), coefsMatrix, 3, 1);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    final double[] values = function.evaluate(result, xKeys).getData()[0];
    final double[] differentiate = function.differentiate(result, xKeys).getData()[0];
    final double[] differentiateTwice = function.differentiateTwice(result, xKeys).getData()[0];
    final double[][] integrate = new double[nInit][nKeys];
    for (int i = 0; i < nInit; ++i) {
      for (int j = 0; j < nKeys; ++j) {
        integrate[i][j] = function.integrate(result, initials[i], xKeys).getData()[j];
      }
    }

    for (int i = 0; i < nKeys; ++i) {
      final double ref = valuesExp[i] == 0. ? 1. : Math.abs(valuesExp[i]);
      assertEquals(values[i], valuesExp[i], ref * EPS);
    }

    for (int i = 0; i < nKeys; ++i) {
      final double ref = differentiateExp[i] == 0. ? 1. : Math.abs(differentiateExp[i]);
      assertEquals(differentiate[i], differentiateExp[i], ref * EPS);
    }

    for (int i = 0; i < nKeys; ++i) {
      final double ref = differentiateTwiceExp[i] == 0. ? 1. : Math.abs(differentiateTwiceExp[i]);
      assertEquals(differentiateTwice[i], differentiateTwiceExp[i], ref * EPS);
    }

    {
      final double ref = differentiateTwiceExp[1] == 0. ? 1. : Math.abs(differentiateTwiceExp[1]);
      assertEquals(differentiateTwice[1], differentiateTwiceExp[1], ref * EPS);
    }

    for (int j = 0; j < nInit; ++j) {
      for (int i = 0; i < nKeys; ++i) {
        final double ref = integrateExp[j][i] == 0. ? 1. : Math.abs(integrateExp[j][i]);
        assertEquals(integrate[j][i], integrateExp[j][i], ref * EPS);
      }
    }

  }

  /**
   * Sample function is f(x) = (x-1)^4
   */
  @Test
  public void GeneralIntegrateDifferentiateTest() {
    final double[] knots = new double[] {1., 2., 3., 4 };
    final double[][] coefMat = new double[][] { {1., 0., 0., 0., 0. },
      {1., 4., 6., 4., 1. },
      {1., 8., 24., 32., 16. } };
    final double[] xKeys = new double[] {-2, 1, 2.5, 4. };
    final double[] initials = new double[] {1., 2.5, 23. / 7., 7. };
    final int nKeys = xKeys.length;
    final int nInit = initials.length;

    final double[][] integrateExp = new double[nInit][nKeys];
    for (int i = 0; i < nInit; ++i) {
      for (int j = 0; j < nKeys; ++j) {
        integrateExp[i][j] = Math.pow(xKeys[j] - 1., 5.) / 5. - Math.pow(initials[i] - 1., 5.) / 5.;
      }
    }
    final double[] differentiateExp = new double[] {-108., 0., 27. / 2., 108. };
    final double[] differentiateTwiceExp = new double[nKeys];
    for (int i = 0; i < nKeys; ++i) {
      differentiateTwiceExp[i] = 12. * (xKeys[i] - 1.) * (xKeys[i] - 1.);
    }

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();
    PiecewisePolynomialResult result = new PiecewisePolynomialResult(new DoubleMatrix1D(knots), new DoubleMatrix2D(coefMat), 5, 1);

    final double[] differentiate = function.differentiate(result, xKeys).getData()[0];
    final double[] differentiateTwice = function.differentiateTwice(result, xKeys).getData()[0];
    final double[][] integrate = new double[nInit][nKeys];
    for (int i = 0; i < nInit; ++i) {
      for (int j = 0; j < nKeys; ++j) {
        integrate[i][j] = function.integrate(result, initials[i], xKeys).getData()[j];
      }
    }

    for (int i = 0; i < nKeys; ++i) {
      final double ref = differentiateExp[i] == 0. ? 1. : Math.abs(differentiateExp[i]);
      assertEquals(differentiate[i], differentiateExp[i], ref * EPS);
    }
    for (int i = 0; i < nKeys; ++i) {
      final double ref = differentiateTwiceExp[i] == 0. ? 1. : Math.abs(differentiateTwiceExp[i]);
      assertEquals(differentiateTwice[i], differentiateTwiceExp[i], ref * EPS);
    }

    for (int j = 0; j < nInit; ++j) {
      for (int i = 0; i < nKeys; ++i) {
        final double ref = integrateExp[j][i] == 0. ? 1. : Math.abs(integrateExp[j][i]);
        assertEquals(integrate[j][i], integrateExp[j][i], ref * EPS);
      }
    }

    {
      final double ref = differentiateExp[0] == 0. ? 1. : Math.abs(differentiateExp[0]);
      assertEquals(function.differentiate(result, xKeys[0]).getData()[0], differentiateExp[0], ref * EPS);
    }
    {
      final double ref = differentiateExp[3] == 0. ? 1. : Math.abs(differentiateExp[3]);
      assertEquals(function.differentiate(result, xKeys[3]).getData()[0], differentiateExp[3], ref * EPS);
    }
    {
      final double ref = differentiateTwiceExp[0] == 0. ? 1. : Math.abs(differentiateTwiceExp[0]);
      assertEquals(function.differentiateTwice(result, xKeys[0]).getData()[0], differentiateTwiceExp[0], ref * EPS);
    }
    {
      final double ref = differentiateTwiceExp[3] == 0. ? 1. : Math.abs(differentiateTwiceExp[3]);
      assertEquals(function.differentiateTwice(result, xKeys[3]).getData()[0], differentiateTwiceExp[3], ref * EPS);
    }
    {
      final double ref = integrateExp[0][0] == 0. ? 1. : Math.abs(integrateExp[0][0]);
      assertEquals(function.integrate(result, initials[0], xKeys[0]), integrateExp[0][0], ref * EPS);
    }
    {
      final double ref = integrateExp[0][3] == 0. ? 1. : Math.abs(integrateExp[0][3]);
      assertEquals(function.integrate(result, initials[0], xKeys[3]), integrateExp[0][3], ref * EPS);
    }
    {
      final double ref = integrateExp[3][0] == 0. ? 1. : Math.abs(integrateExp[3][0]);
      assertEquals(function.integrate(result, initials[3], xKeys[0]), integrateExp[3][0], ref * EPS);
    }
    {
      final double ref = integrateExp[1][0] == 0. ? 1. : Math.abs(integrateExp[1][0]);
      assertEquals(function.integrate(result, initials[1], xKeys[0]), integrateExp[1][0], ref * EPS);
    }
  }

  /**
   * Error tests below
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullpEvaluateTest() {
    final double[] xValues = new double[] {1, 2, 3, 4 };
    final DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    final double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    pp = null;
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys[0][0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullpEvaluateMultiTest() {
    final double[] xValues = new double[] {1, 2, 3, 4 };
    final DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    final double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    pp = null;
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullpEvaluateMatrixTest() {
    final double[] xValues = new double[] {1, 2, 3, 4 };
    final DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    final double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    pp = null;
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullpIntegrateTest() {
    final double[] xValues = new double[] {1, 2, 3, 4 };
    final DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {1., 0., 0., 0. }, {1., 3., 3., 1. } });
    final double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 1;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    pp = null;
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.integrate(pp, 1., xKeys[0][0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullpIntegrateMultiTest() {
    final double[] xValues = new double[] {1, 2, 3, 4 };
    final DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {1., 0., 0., 0. }, {1., 3., 3., 1. } });
    final double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 1;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    pp = null;
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.integrate(pp, 1., xKeys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullpDifferentiateTest() {
    final double[] xValues = new double[] {1, 2, 3, 4 };
    final DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    final double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    pp = null;
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.differentiate(pp, xKeys[0][0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullpDifferentiateMultiTest() {
    final double[] xValues = new double[] {1, 2, 3, 4 };
    final DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    final double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    pp = null;
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.differentiate(pp, xKeys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullxEvaluateTest() {
    double[] xValues = new double[] {1, 2, 3, 4 };
    DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[] xKeys = new double[] {-2, 1, 2, 2.5 };
    final int dim = 2;
    final int nCoefs = 4;

    xKeys = null;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullxEvaluateMatrixTest() {
    double[] xValues = new double[] {1, 2, 3, 4 };
    DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 2;
    final int nCoefs = 4;

    xKeys = null;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullxIntTest() {
    double[] xValues = new double[] {1, 2, 3, 4 };
    DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {1., 0., 0., 0. }, {1., 3., 3., 1. } });
    double[] xKeys = new double[] {-2, 1, 2, 2.5 };
    final int dim = 1;
    final int nCoefs = 4;

    xKeys = null;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.integrate(pp, 1., xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullxDiffTest() {
    double[] xValues = new double[] {1, 2, 3, 4 };
    DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[] xKeys = new double[] {-2, 1, 2, 2.5 };
    final int dim = 2;
    final int nCoefs = 4;

    xKeys = null;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.differentiate(pp, xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void infxEvaluateTest() {
    double[] xValues = new double[] {1, 2, 3, 4 };
    DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[][] xKeys = new double[][] { {INF, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys[0][0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void infxEvaluateMultiTest() {
    double[] xValues = new double[] {1, 2, 3, 4 };
    DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[][] xKeys = new double[][] { {-2, 1, INF, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void infxEvaluateMatrixTest() {
    double[] xValues = new double[] {1, 2, 3, 4 };
    DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., INF } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void infxIntTest() {
    double[] xValues = new double[] {1, 2, 3, 4 };
    DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {1., 0., 0., 0. }, {1., 3., 3., 1. } });
    double[][] xKeys = new double[][] { {INF, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 1;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.integrate(pp, 1., xKeys[0][0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void infxIntMultiTest() {
    double[] xValues = new double[] {1, 2, 3, 4 };
    DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {1., 0., 0., 0. }, {1., 3., 3., 1. } });
    double[] xKeys = new double[] {1.5, 7. / 3., 29. / 7., INF };
    final int dim = 1;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.integrate(pp, 1., xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNxEvaluateTest() {
    double[] xValues = new double[] {1, 2, 3, 4 };
    DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[][] xKeys = new double[][] { {Double.NaN, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys[0][0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNxEvaluateMultiTest() {
    double[] xValues = new double[] {1, 2, 3, 4 };
    DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[][] xKeys = new double[][] { {-2, 1, Double.NaN, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNxEvaluateMatrixTest() {
    double[] xValues = new double[] {1, 2, 3, 4 };
    DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., Double.NaN } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNxIntTest() {
    double[] xValues = new double[] {1, 2, 3, 4 };
    DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {1., 0., 0., 0. }, {1., 3., 3., 1. } });
    double[][] xKeys = new double[][] { {Double.NaN, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 1;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.integrate(pp, 1., xKeys[0][0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNxIntMultiTest() {
    double[] xValues = new double[] {1, 2, 3, 4 };
    DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {1., 0., 0., 0. }, {1., 3., 3., 1. } });
    double[] xKeys = new double[] {1.5, 7. / 3., 29. / 7., Double.NaN };
    final int dim = 1;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.integrate(pp, 1., xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullDimIntTest() {
    double[] xValues = new double[] {1, 2, 3, 4 };
    DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[] xKeys = new double[] {-2, 1, 2, 2.5 };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.integrate(pp, 1., xKeys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullDimIntMultiTest() {
    double[] xValues = new double[] {1, 2, 3, 4 };
    DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[] xKeys = new double[] {-2, 1, 2, 2.5 };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.integrate(pp, 1., xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void constFuncDiffTest() {
    double[] xValues = new double[] {1, 2, 3, 4 };
    DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {-1 }, {20 }, {0. }, {5 }, {1. }, {0. } });
    double[] xKeys = new double[] {-2, 1, 2, 2.5 };
    final int dim = 2;
    final int nCoefs = 1;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.differentiate(pp, xKeys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void constFuncDiffMultiTest() {
    double[] xValues = new double[] {1, 2, 3, 4 };
    DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {-1 }, {20 }, {0. }, {5 }, {1. }, {0. } });
    double[] xKeys = new double[] {-2, 1, 2, 2.5 };
    final int dim = 2;
    final int nCoefs = 1;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.differentiate(pp, xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void linearFuncDiffTwiceTest() {
    double[] xValues = new double[] {1, 2, 3, 4 };
    DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3. }, {0., 5. }, {1., 0. }, {0., 5. }, {1., 3. }, {0., 5. } });
    double[] xKeys = new double[] {-2, 1, 2, 2.5 };
    final int dim = 2;
    final int nCoefs = 2;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.differentiateTwice(pp, xKeys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void linearFuncDiffTwiceMultiTest() {
    double[] xValues = new double[] {1, 2, 3, 4 };
    DoubleMatrix2D coefsMatrix = new DoubleMatrix2D(
        new double[][] { {1., -3. }, {0., 5. }, {1., 0. }, {0., 5. }, {1., 3. }, {0., 5. } });
    double[] xKeys = new double[] {-2, 1, 2, 2.5 };
    final int dim = 2;
    final int nCoefs = 2;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(new DoubleMatrix1D(xValues), coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.differentiateTwice(pp, xKeys);
  }
}
