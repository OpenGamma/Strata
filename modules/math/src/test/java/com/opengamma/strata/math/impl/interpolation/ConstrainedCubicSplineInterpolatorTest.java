/*
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.function.PiecewisePolynomialFunction1D;

/**
 * Test.
 */
@Test
public class ConstrainedCubicSplineInterpolatorTest {

  private static final double EPS = 1e-13;
  private static final double INF = 1. / 0.;

  /**
   * Recovering linear test
   * Note that quadratic function is not generally recovered
   */
  public void linearTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5., 6. };
    final int nData = xValues.length;
    final double[] yValues = new double[nData];
    for (int i = 0; i < nData; ++i) {
      yValues[i] = xValues[i] / 7. + 1 / 11.;
    }

    final double[][] coefsMatExp = new double[][] { {0., 0., 1. / 7., yValues[0] }, {0., 0., 1. / 7., yValues[1] }, {0., 0., 1. / 7., yValues[2] }, {0., 0., 1. / 7., yValues[3] },
      {0., 0., 1. / 7., yValues[4] } };

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    PiecewisePolynomialInterpolator interp = new ConstrainedCubicSplineInterpolator();
    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    assertEquals(result.getDimensions(), 1);
    assertEquals(result.getNumberOfIntervals(), 5);
    assertEquals(result.getOrder(), 4);

    for (int i = 0; i < result.getNumberOfIntervals(); ++i) {
      for (int j = 0; j < result.getOrder(); ++j) {
        final double ref = Math.abs(coefsMatExp[i][j]) == 0. ? 1. : Math.abs(coefsMatExp[i][j]);
        assertEquals(result.getCoefMatrix().get(i, j), coefsMatExp[i][j], ref * EPS);
      }
    }

    final int nKeys = 101;
    for (int i = 0; i < nKeys; ++i) {
      final double key = 1. + 5. / (nKeys - 1) * i;
      final double ref = key / 7. + 1 / 11.;
      assertEquals(function.evaluate(result, key).get(0), ref, ref * EPS);
    }
  }

  /**
   * 
   */
  public void linearMultiTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5., 6. };
    final int nData = xValues.length;
    final double[][] yValues = new double[2][nData];
    for (int i = 0; i < nData; ++i) {
      yValues[0][i] = xValues[i] / 7. + 1 / 11.;
      yValues[1][i] = xValues[i] / 13. + 1 / 3.;
    }

    final double[][] coefsMatExp = new double[][] { {0., 0., 1. / 7., yValues[0][0] }, {0., 0., 1. / 13., yValues[1][0] }, {0., 0., 1. / 7., yValues[0][1] }, {0., 0., 1. / 13., yValues[1][1] },
      {0., 0., 1. / 7., yValues[0][2] }, {0., 0., 1. / 13., yValues[1][2] }, {0., 0., 1. / 7., yValues[0][3] }, {0., 0., 1. / 13., yValues[1][3] },
      {0., 0., 1. / 7., yValues[0][4] }, {0., 0., 1. / 13., yValues[1][4] } };

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    PiecewisePolynomialInterpolator interp = new ConstrainedCubicSplineInterpolator();
    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    assertEquals(result.getDimensions(), 2);
    assertEquals(result.getNumberOfIntervals(), 5);
    assertEquals(result.getOrder(), 4);

    for (int i = 0; i < result.getNumberOfIntervals() * result.getDimensions(); ++i) {
      for (int j = 0; j < result.getOrder(); ++j) {
        final double ref = Math.abs(coefsMatExp[i][j]) == 0. ? 1. : Math.abs(coefsMatExp[i][j]);
        assertEquals(result.getCoefMatrix().get(i, j), coefsMatExp[i][j], ref * EPS);
      }
    }

    final int nKeys = 101;
    for (int i = 0; i < nKeys; ++i) {
      final double key = 1. + 5. / (nKeys - 1) * i;
      final double ref = key / 7. + 1 / 11.;
      assertEquals(function.evaluate(result, key).get(0), ref, ref * EPS);

    }
  }

  /**
   * 
   */
  public void extremumTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5., 6., 7. };
    final double[] yValues = new double[] {1., 1., 4., 5., 4., 1., 1. };

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    PiecewisePolynomialInterpolator interp = new ConstrainedCubicSplineInterpolator();
    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    assertEquals(result.getDimensions(), 1);
    assertEquals(result.getNumberOfIntervals(), 6);
    assertEquals(result.getOrder(), 4);

    final int nKeys = 31;
    double key0 = 1.;
    for (int i = 1; i < nKeys; ++i) {
      final double key = 1. + 3. / (nKeys - 1) * i;
      assertTrue(function.evaluate(result, key).get(0) - function.evaluate(result, key0).get(0) >= 0.);
      key0 = 1. + 3. / (nKeys - 1) * i;

    }

    key0 = 4.;
    for (int i = 1; i < nKeys; ++i) {
      final double key = 4. + 3. / (nKeys - 1) * i;
      assertTrue(function.evaluate(result, key).get(0) - function.evaluate(result, key0).get(0) <= 0.);
      key0 = 4. + 3. / (nKeys - 1) * i;

    }
  }

  /**
   * Sample data
   */
  public void sampleDataTest() {
    final double[] xValues = new double[] {0., 10., 30., 50., 70., 90., 100. };
    final double[] yValues = new double[] {30., 130., 150., 150., 170., 220., 320. };

    PiecewisePolynomialInterpolator interp = new ConstrainedCubicSplineInterpolator();
    PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);

    PiecewisePolynomialFunction1D function = new PiecewisePolynomialFunction1D();

    final double[][] coefsMatPartExp = new double[][] { {-9. / 220., 0., 155. / 11., 30. }, {-1. / 2200., -7. / 220., 20. / 11., 130. }, {0., 0., 0., 150. } };
    for (int i = 0; i < 3; ++i) {
      for (int j = 0; j < 4; ++j) {
        final double ref = Math.abs(coefsMatPartExp[i][j]) == 0. ? 1. : Math.abs(coefsMatPartExp[i][j]);
        assertEquals(result.getCoefMatrix().get(i, j), coefsMatPartExp[i][j], ref * EPS);
      }
    }

    int nKeys = 101;
    double key0 = 0.;
    for (int i = 1; i < nKeys; ++i) {
      final double key = 0. + 100. / (nKeys - 1) * i;

      assertTrue(function.evaluate(result, key).get(0) - function.evaluate(result, key0).get(0) >= -EPS);
      key0 = 0. + 100. / (nKeys - 1) * i;
    }
  }

  /*
   * Error tests
   */

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void dataShortTest() {
    final double[] xValues = new double[] {1. };
    final double[] yValues = new double[] {0., };

    PiecewisePolynomialInterpolator interpPos = new ConstrainedCubicSplineInterpolator();
    interpPos.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void dataShortMultiTest() {
    final double[] xValues = new double[] {1. };
    final double[][] yValues = new double[][] { {0. }, {0.1 } };

    PiecewisePolynomialInterpolator interpPos = new ConstrainedCubicSplineInterpolator();
    interpPos.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void dataDiffTest() {
    final double[] xValues = new double[] {1., 2., 3., 4. };
    final double[] yValues = new double[] {0., 0.1, 3. };

    PiecewisePolynomialInterpolator interpPos = new ConstrainedCubicSplineInterpolator();
    interpPos.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void dataDiffMultiTest() {
    final double[] xValues = new double[] {1., 2., 3., 4. };
    final double[][] yValues = new double[][] { {0., 0.1, 3. }, {0., 0.1, 3. } };

    PiecewisePolynomialInterpolator interpPos = new ConstrainedCubicSplineInterpolator();
    interpPos.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void coincideDataTest() {
    final double[] xValues = new double[] {1., 1., 3. };
    final double[] yValues = new double[] {0., 0.1, 0.05 };

    PiecewisePolynomialInterpolator interpPos = new ConstrainedCubicSplineInterpolator();
    interpPos.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void coincideDataMultiTest() {
    final double[] xValues = new double[] {1., 2., 2. };
    final double[][] yValues = new double[][] { {0., 0.1, 0.05 }, {0., 0.1, 1.05 } };

    PiecewisePolynomialInterpolator interpPos = new ConstrainedCubicSplineInterpolator();
    interpPos.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullXdataTest() {
    double[] xValues = new double[] {1., 2., 3., 4. };
    double[] yValues = new double[] {0., 0.1, 0.05, 0.2 };
    xValues = null;

    PiecewisePolynomialInterpolator interpPos = new ConstrainedCubicSplineInterpolator();
    interpPos.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullYdataTest() {
    double[] xValues = new double[] {1., 2., 3., 4. };
    double[] yValues = new double[] {0., 0.1, 0.05, 0.2 };
    yValues = null;

    PiecewisePolynomialInterpolator interpPos = new ConstrainedCubicSplineInterpolator();
    interpPos.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullXdataMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 4. };
    double[][] yValues = new double[][] { {0., 0.1, 0.05, 0.2 }, {0., 0.1, 0.05, 0.2 } };
    xValues = null;

    PiecewisePolynomialInterpolator interpPos = new ConstrainedCubicSplineInterpolator();
    interpPos.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullYdataMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 4. };
    double[][] yValues = new double[][] { {0., 0.1, 0.05, 0.2 }, {0., 0.1, 0.05, 0.2 } };
    yValues = null;

    PiecewisePolynomialInterpolator interpPos = new ConstrainedCubicSplineInterpolator();
    interpPos.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void infXdataTest() {
    double[] xValues = new double[] {1., 2., 3., INF };
    double[] yValues = new double[] {0., 0.1, 0.05, 0.2 };

    PiecewisePolynomialInterpolator interpPos = new ConstrainedCubicSplineInterpolator();
    interpPos.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void infYdataTest() {
    double[] xValues = new double[] {1., 2., 3., 4. };
    double[] yValues = new double[] {0.1, 0.05, 0.2, INF };

    PiecewisePolynomialInterpolator interpPos = new ConstrainedCubicSplineInterpolator();
    interpPos.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanXdataTest() {
    double[] xValues = new double[] {1., 2., 3., Double.NaN };
    double[] yValues = new double[] {0., 0.1, 0.05, 0.2 };

    PiecewisePolynomialInterpolator interpPos = new ConstrainedCubicSplineInterpolator();
    interpPos.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanYdataTest() {
    double[] xValues = new double[] {1., 2., 3., 4. };
    double[] yValues = new double[] {0.1, 0.05, 0.2, Double.NaN };

    PiecewisePolynomialInterpolator interpPos = new ConstrainedCubicSplineInterpolator();
    interpPos.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void infXdataMultiTest() {
    double[] xValues = new double[] {1., 2., 3., INF };
    double[][] yValues = new double[][] { {0., 0.1, 0.05, 0.2 }, {0., 0.1, 0.05, 0.2 } };

    PiecewisePolynomialInterpolator interpPos = new ConstrainedCubicSplineInterpolator();
    interpPos.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void infYdataMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 4. };
    double[][] yValues = new double[][] { {0.1, 0.05, 0.2, 1. }, {0.1, 0.05, 0.2, INF } };

    PiecewisePolynomialInterpolator interpPos = new ConstrainedCubicSplineInterpolator();
    interpPos.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanXdataMultiTest() {
    double[] xValues = new double[] {1., 2., 3., Double.NaN };
    double[][] yValues = new double[][] { {0., 0.1, 0.05, 0.2 }, {0., 0.1, 0.05, 0.2 } };

    PiecewisePolynomialInterpolator interpPos = new ConstrainedCubicSplineInterpolator();
    interpPos.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nanYdataMultiTest() {
    double[] xValues = new double[] {1., 2., 3., 4. };
    double[][] yValues = new double[][] { {0.1, 0.05, 0.2, 1.1 }, {0.1, 0.05, 0.2, Double.NaN } };

    PiecewisePolynomialInterpolator interpPos = new ConstrainedCubicSplineInterpolator();
    interpPos.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void notReconnectedTest() {
    double[] xValues = new double[] {1., 2., 2.0000001, 4. };
    double[] yValues = new double[] {2., 3., 40000000., 5. };

    PiecewisePolynomialInterpolator interpPos = new ConstrainedCubicSplineInterpolator();
    interpPos.interpolate(xValues, yValues);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void notReconnectedMultiTest() {
    double[] xValues = new double[] {1., 2., 2.0000001, 4. };
    double[][] yValues = new double[][] {{2., 3., 40000000., 5. } };

    PiecewisePolynomialInterpolator interpPos = new ConstrainedCubicSplineInterpolator();
    interpPos.interpolate(xValues, yValues);
  }
}
