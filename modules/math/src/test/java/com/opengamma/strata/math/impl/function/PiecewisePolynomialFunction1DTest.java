/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResult;

/**
 * Test.
 */
@Test
public class PiecewisePolynomialFunction1DTest {

  private static final double EPS = 1e-14;
  private static final double INF = 1. / 0.;
  private static final DoubleArray X_VALUES = DoubleArray.of(1, 2, 3, 4);

  /**
   * 
   */
  @Test
  public void evaluateAllTest() {
    final DoubleMatrix coefsMatrix =
        DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    final double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final double[][][] valuesExp = new double[][][] { { {-64., -1., 0., 1. / 8. }, {-1. / 8., 1. / 27., 3375. / 7. / 7. / 7., 27. } },
      { {125., 20., 5., 5. / 4. }, {45. / 4., 20. / 9., 2240. / 7. / 7. / 7., 20. } } };
    final int dim = 2;
    final int nCoefs = 4;
    final int keyLength = xKeys[0].length;
    final int keyDim = xKeys.length;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    final DoubleMatrix[] valuesResMat = function.evaluate(pp, xKeys);
    for (int i = 0; i < dim; ++i) {
      for (int k = 0; k < keyDim; ++k) {
        for (int j = 0; j < keyLength; ++j) {
          final double ref = valuesExp[i][k][j] == 0. ? 1. : Math.abs(valuesExp[i][k][j]);
          assertEquals(valuesResMat[i].get(k, j), valuesExp[i][k][j], ref * EPS);
        }
      }
    }

    final DoubleMatrix valuesRes = function.evaluate(pp, xKeys[0]);
    for (int i = 0; i < dim; ++i) {
      for (int j = 0; j < keyLength; ++j) {
        final double ref = valuesExp[i][0][j] == 0. ? 1. : Math.abs(valuesExp[i][0][j]);
        assertEquals(valuesRes.get(i, j), valuesExp[i][0][j], ref * EPS);
      }
    }

    DoubleArray valuesResVec = function.evaluate(pp, xKeys[0][0]);
    for (int i = 0; i < dim; ++i) {
      final double ref = valuesExp[i][0][0] == 0. ? 1. : Math.abs(valuesExp[i][0][0]);
      assertEquals(valuesResVec.get(i), valuesExp[i][0][0], ref * EPS);
    }

    valuesResVec = function.evaluate(pp, xKeys[0][3]);
    for (int i = 0; i < dim; ++i) {
      final double ref = valuesExp[i][0][3] == 0. ? 1. : Math.abs(valuesExp[i][0][3]);
      assertEquals(valuesResVec.get(i), valuesExp[i][0][3], ref * EPS);
    }

  }

  /**
   * 
   */
  @Test
  public void linearAllTest() {

    final DoubleArray knots = DoubleArray.of(1d, 4d);
    final DoubleMatrix coefsMatrix = DoubleMatrix.copyOf(
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

    PiecewisePolynomialResult result = new PiecewisePolynomialResult(knots, coefsMatrix, 3, 1);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    final DoubleArray values = function.evaluate(result, xKeys).row(0);
    final DoubleArray differentiate = function.differentiate(result, xKeys).row(0);
    final double[][] integrate = new double[nInit][nKeys];
    for (int i = 0; i < nInit; ++i) {
      for (int j = 0; j < nKeys; ++j) {
        integrate[i][j] = function.integrate(result, initials[i], xKeys).get(j);
      }
    }

    for (int i = 0; i < nKeys; ++i) {
      final double ref = valuesExp[i] == 0. ? 1. : Math.abs(valuesExp[i]);
      assertEquals(values.get(i), valuesExp[i], ref * EPS);
    }

    for (int i = 0; i < nKeys; ++i) {
      final double ref = differentiateExp[i] == 0. ? 1. : Math.abs(differentiateExp[i]);
      assertEquals(differentiate.get(i), differentiateExp[i], ref * EPS);
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

    final DoubleArray knots = DoubleArray.of(1d, 3d);
    final DoubleMatrix coefsMatrix = DoubleMatrix.copyOf(
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

    PiecewisePolynomialResult result = new PiecewisePolynomialResult(knots, coefsMatrix, 3, 1);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    final DoubleArray values = function.evaluate(result, xKeys).row(0);
    final DoubleArray differentiate = function.differentiate(result, xKeys).row(0);
    final DoubleArray differentiateTwice = function.differentiateTwice(result, xKeys).row(0);
    final double[][] integrate = new double[nInit][nKeys];
    for (int i = 0; i < nInit; ++i) {
      for (int j = 0; j < nKeys; ++j) {
        integrate[i][j] = function.integrate(result, initials[i], xKeys).get(j);
      }
    }

    for (int i = 0; i < nKeys; ++i) {
      final double ref = valuesExp[i] == 0. ? 1. : Math.abs(valuesExp[i]);
      assertEquals(values.get(i), valuesExp[i], ref * EPS);
    }

    for (int i = 0; i < nKeys; ++i) {
      final double ref = differentiateExp[i] == 0. ? 1. : Math.abs(differentiateExp[i]);
      assertEquals(differentiate.get(i), differentiateExp[i], ref * EPS);
    }

    for (int i = 0; i < nKeys; ++i) {
      final double ref = differentiateTwiceExp[i] == 0. ? 1. : Math.abs(differentiateTwiceExp[i]);
      assertEquals(differentiateTwice.get(i), differentiateTwiceExp[i], ref * EPS);
    }

    {
      final double ref = differentiateTwiceExp[1] == 0. ? 1. : Math.abs(differentiateTwiceExp[1]);
      assertEquals(differentiateTwice.get(1), differentiateTwiceExp[1], ref * EPS);
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
    PiecewisePolynomialResult result =
        new PiecewisePolynomialResult(X_VALUES, DoubleMatrix.copyOf(coefMat), 5, 1);

    final DoubleArray differentiate = function.differentiate(result, xKeys).row(0);
    final DoubleArray differentiateTwice = function.differentiateTwice(result, xKeys).row(0);
    final double[][] integrate = new double[nInit][nKeys];
    for (int i = 0; i < nInit; ++i) {
      for (int j = 0; j < nKeys; ++j) {
        integrate[i][j] = function.integrate(result, initials[i], xKeys).get(j);
      }
    }

    for (int i = 0; i < nKeys; ++i) {
      final double ref = differentiateExp[i] == 0. ? 1. : Math.abs(differentiateExp[i]);
      assertEquals(differentiate.get(i), differentiateExp[i], ref * EPS);
    }
    for (int i = 0; i < nKeys; ++i) {
      final double ref = differentiateTwiceExp[i] == 0. ? 1. : Math.abs(differentiateTwiceExp[i]);
      assertEquals(differentiateTwice.get(i), differentiateTwiceExp[i], ref * EPS);
    }

    for (int j = 0; j < nInit; ++j) {
      for (int i = 0; i < nKeys; ++i) {
        final double ref = integrateExp[j][i] == 0. ? 1. : Math.abs(integrateExp[j][i]);
        assertEquals(integrate[j][i], integrateExp[j][i], ref * EPS);
      }
    }

    {
      final double ref = differentiateExp[0] == 0. ? 1. : Math.abs(differentiateExp[0]);
      assertEquals(function.differentiate(result, xKeys[0]).get(0), differentiateExp[0], ref * EPS);
    }
    {
      final double ref = differentiateExp[3] == 0. ? 1. : Math.abs(differentiateExp[3]);
      assertEquals(function.differentiate(result, xKeys[3]).get(0), differentiateExp[3], ref * EPS);
    }
    {
      final double ref = differentiateTwiceExp[0] == 0. ? 1. : Math.abs(differentiateTwiceExp[0]);
      assertEquals(function.differentiateTwice(result, xKeys[0]).get(0), differentiateTwiceExp[0], ref * EPS);
    }
    {
      final double ref = differentiateTwiceExp[3] == 0. ? 1. : Math.abs(differentiateTwiceExp[3]);
      assertEquals(function.differentiateTwice(result, xKeys[3]).get(0), differentiateTwiceExp[3], ref * EPS);
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
   * Consistency with evaluate and differentiate.
   */
  @Test
  public void evaluateAndDifferentiateTest() {
    double[][][] coefsMatrix = new double[][][] { { {1., -3., 3., -1 }, {1., 0., 0., 0. }, {1., 3., 3., 1. }, },
          { {0., 5., -20., 20 }, {0., 5., -10., 5 }, {0., 5., 0., 0. } } };
    double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    int dim = 2;
    int nCoefs = 4;
    int keyLength = xKeys[0].length;
    PiecewisePolynomialResult[] pp = new PiecewisePolynomialResult[dim];
    for (int i = 0; i < dim; ++i) {
      pp[i] = new PiecewisePolynomialResult(X_VALUES, DoubleMatrix.ofUnsafe(coefsMatrix[i]), nCoefs, 1);
    }
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();
    for (int i = 0; i < dim; ++i) {
      for (int j = 0; j < keyLength; ++j) {
        ValueDerivatives computed = function.evaluateAndDifferentiate(pp[i], xKeys[i][j]);
        double value = function.evaluate(pp[i], xKeys[i][j]).get(0);
        double deriv = function.differentiate(pp[i], xKeys[i][j]).get(0);
        assertEquals(computed.getValue(), value, EPS);
        assertEquals(computed.getDerivatives().size(), 1);
        assertEquals(computed.getDerivative(0), deriv, EPS);
      }
    }
  }

  /**
   * Error tests below
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullpEvaluateTest() {
    final DoubleMatrix coefsMatrix =
        DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    final double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    pp = null;
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys[0][0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullpEvaluateMultiTest() {
    final DoubleMatrix coefsMatrix =
        DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    final double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    pp = null;
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullpEvaluateMatrixTest() {
    final DoubleMatrix coefsMatrix =
        DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    final double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    pp = null;
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullpIntegrateTest() {
    final DoubleMatrix coefsMatrix = DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {1., 0., 0., 0. }, {1., 3., 3., 1. } });
    final double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 1;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    pp = null;
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.integrate(pp, 1., xKeys[0][0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullpIntegrateMultiTest() {
    final DoubleMatrix coefsMatrix = DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {1., 0., 0., 0. }, {1., 3., 3., 1. } });
    final double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 1;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    pp = null;
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.integrate(pp, 1., xKeys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullpDifferentiateTest() {
    final DoubleMatrix coefsMatrix =
        DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    final double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    pp = null;
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.differentiate(pp, xKeys[0][0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullpDifferentiateMultiTest() {
    final DoubleMatrix coefsMatrix =
        DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    final double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    pp = null;
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.differentiate(pp, xKeys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullxEvaluateTest() {
    DoubleMatrix coefsMatrix =
        DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[] xKeys = new double[] {-2, 1, 2, 2.5 };
    final int dim = 2;
    final int nCoefs = 4;

    xKeys = null;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullxEvaluateMatrixTest() {
    DoubleMatrix coefsMatrix =
        DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 2;
    final int nCoefs = 4;

    xKeys = null;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullxIntTest() {
    DoubleMatrix coefsMatrix = DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {1., 0., 0., 0. }, {1., 3., 3., 1. } });
    double[] xKeys = new double[] {-2, 1, 2, 2.5 };
    final int dim = 1;
    final int nCoefs = 4;

    xKeys = null;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.integrate(pp, 1., xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullxDiffTest() {
    DoubleMatrix coefsMatrix =
        DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[] xKeys = new double[] {-2, 1, 2, 2.5 };
    final int dim = 2;
    final int nCoefs = 4;

    xKeys = null;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.differentiate(pp, xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void infxEvaluateTest() {
    DoubleMatrix coefsMatrix =
        DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[][] xKeys = new double[][] { {INF, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys[0][0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void infxEvaluateMultiTest() {
    DoubleMatrix coefsMatrix =
        DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[][] xKeys = new double[][] { {-2, 1, INF, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void infxEvaluateMatrixTest() {
    DoubleMatrix coefsMatrix =
        DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., INF } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void infxIntTest() {
    DoubleMatrix coefsMatrix = DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {1., 0., 0., 0. }, {1., 3., 3., 1. } });
    double[][] xKeys = new double[][] { {INF, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 1;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.integrate(pp, 1., xKeys[0][0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void infxIntMultiTest() {
    DoubleMatrix coefsMatrix = DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {1., 0., 0., 0. }, {1., 3., 3., 1. } });
    double[] xKeys = new double[] {1.5, 7. / 3., 29. / 7., INF };
    final int dim = 1;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.integrate(pp, 1., xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNxEvaluateTest() {
    DoubleMatrix coefsMatrix =
        DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[][] xKeys = new double[][] { {Double.NaN, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys[0][0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNxEvaluateMultiTest() {
    DoubleMatrix coefsMatrix =
        DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[][] xKeys = new double[][] { {-2, 1, Double.NaN, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNxEvaluateMatrixTest() {
    DoubleMatrix coefsMatrix =
        DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[][] xKeys = new double[][] { {-2, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., Double.NaN } };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.evaluate(pp, xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNxIntTest() {
    DoubleMatrix coefsMatrix = DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {1., 0., 0., 0. }, {1., 3., 3., 1. } });
    double[][] xKeys = new double[][] { {Double.NaN, 1, 2, 2.5 }, {1.5, 7. / 3., 29. / 7., 5. } };
    final int dim = 1;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.integrate(pp, 1., xKeys[0][0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void NaNxIntMultiTest() {
    DoubleMatrix coefsMatrix = DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {1., 0., 0., 0. }, {1., 3., 3., 1. } });
    double[] xKeys = new double[] {1.5, 7. / 3., 29. / 7., Double.NaN };
    final int dim = 1;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.integrate(pp, 1., xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullDimIntTest() {
    DoubleMatrix coefsMatrix =
        DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[] xKeys = new double[] {-2, 1, 2, 2.5 };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.integrate(pp, 1., xKeys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullDimIntMultiTest() {
    DoubleMatrix coefsMatrix =
        DoubleMatrix.copyOf(
        new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 }, {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    double[] xKeys = new double[] {-2, 1, 2, 2.5 };
    final int dim = 2;
    final int nCoefs = 4;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.integrate(pp, 1., xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void constFuncDiffTest() {
    DoubleMatrix coefsMatrix = DoubleMatrix.copyOf(
        new double[][] { {-1 }, {20 }, {0. }, {5 }, {1. }, {0. } });
    double[] xKeys = new double[] {-2, 1, 2, 2.5 };
    final int dim = 2;
    final int nCoefs = 1;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.differentiate(pp, xKeys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void constFuncDiffMultiTest() {
    DoubleMatrix coefsMatrix = DoubleMatrix.copyOf(
        new double[][] { {-1 }, {20 }, {0. }, {5 }, {1. }, {0. } });
    double[] xKeys = new double[] {-2, 1, 2, 2.5 };
    final int dim = 2;
    final int nCoefs = 1;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.differentiate(pp, xKeys);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void linearFuncDiffTwiceTest() {
    DoubleMatrix coefsMatrix = DoubleMatrix.copyOf(
        new double[][] { {1., -3. }, {0., 5. }, {1., 0. }, {0., 5. }, {1., 3. }, {0., 5. } });
    double[] xKeys = new double[] {-2, 1, 2, 2.5 };
    final int dim = 2;
    final int nCoefs = 2;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.differentiateTwice(pp, xKeys[0]);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void linearFuncDiffTwiceMultiTest() {
    DoubleMatrix coefsMatrix = DoubleMatrix.copyOf(
        new double[][] { {1., -3. }, {0., 5. }, {1., 0. }, {0., 5. }, {1., 3. }, {0., 5. } });
    double[] xKeys = new double[] {-2, 1, 2, 2.5 };
    final int dim = 2;
    final int nCoefs = 2;

    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    function.differentiateTwice(pp, xKeys);
  }

  /**
   * dim must be 1 for evaluateAndDifferentiate.
   */
  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void dimFailTest() {
    DoubleMatrix coefsMatrix = DoubleMatrix.copyOf(new double[][] { {1., -3., 3., -1 }, {0., 5., -20., 20 },
      {1., 0., 0., 0. }, {0., 5., -10., 5 }, {1., 3., 3., 1. }, {0., 5., 0., 0. } });
    int dim = 2;
    int nCoefs = 4;
    PiecewisePolynomialResult pp = new PiecewisePolynomialResult(X_VALUES, coefsMatrix, nCoefs, dim);
    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();
    function.evaluateAndDifferentiate(pp, 1.5);
  }

}
