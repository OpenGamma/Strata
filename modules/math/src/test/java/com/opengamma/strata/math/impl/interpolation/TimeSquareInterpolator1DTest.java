/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Tests related to the "time/square of value" interpolator.
 */
@Test
public class TimeSquareInterpolator1DTest {

  private static final Interpolator1D INTERPOLATOR = new TimeSquareInterpolator1D();
  private static final double[] X = new double[] {1, 2, 3 };
  private static final double[] Y = new double[] {4, 5, 6 };
  private static final Interpolator1DDataBundle DATA = INTERPOLATOR.getDataBundle(X, Y);

  private static final double TOLERANCE_Y = 1.0E-10;
  private static final double TOLERANCE_SENSI = 1.0E-6;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void nullDataBundle() {
    INTERPOLATOR.interpolate(null, 2.3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void lowValue() {
    INTERPOLATOR.interpolate(DATA, -4.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void highValue() {
    INTERPOLATOR.interpolate(DATA, 10.0);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void lowValueFirstDerivative() {
    INTERPOLATOR.firstDerivative(DATA, -4.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void lowValueSensitivity() {
    INTERPOLATOR.getNodeSensitivitiesForValue(DATA, -4.);
  }

  @Test
  /**
   * Tests the sensitivity of the interpolated value to the Y values.
   */
  public void interpolation() {
    final Interpolator1D interpolatorFromFactory = Interpolator1DFactory.TIME_SQUARE_INSTANCE;
    double[] x = new double[] {1.0, 1.5, 2, 2.75 };
    for (int loopx = 0; loopx < x.length; loopx++) {
      double yCalculated = INTERPOLATOR.interpolate(DATA, x[loopx]);
      double yCalculatedFactory = interpolatorFromFactory.interpolate(DATA, x[loopx]);
      final int index = DATA.getLowerBoundIndex(x[loopx]);
      final double weight = (DATA.getKeys()[index + 1] - x[loopx]) / (DATA.getKeys()[index + 1] - DATA.getKeys()[index]);
      final double val1 = DATA.getKeys()[index] * DATA.getValues()[index] * DATA.getValues()[index];
      final double val2 = DATA.getKeys()[index + 1] * DATA.getValues()[index + 1] * DATA.getValues()[index + 1];
      final double yExpected = Math.sqrt((weight * val1 + (1.0 - weight) * val2) / x[loopx]);
      assertEquals("TimeSquare interpolator: data point " + loopx, yExpected, yCalculated, TOLERANCE_Y);
      assertEquals("TimeSquare interpolator: data point " + loopx, yCalculated, yCalculatedFactory, TOLERANCE_Y);
    }
    int lenghtx = DATA.getKeys().length;
    double lastx = DATA.getKeys()[lenghtx - 1];
    double yCalculated = INTERPOLATOR.interpolate(DATA, lastx);
    final double yExpected = DATA.getValues()[lenghtx - 1];
    assertEquals("TimeSquare interpolator: last point", yExpected, yCalculated, TOLERANCE_Y);
  }

  @Test
  /**
   * Tests the sensitivity of the interpolated value to the Y values.
   */
  public void interpolationSensitivity() {
    double shift = 1.0E-6;
    double[] x = new double[] {1.0, 1.5, 2, 2.75 };
    for (int loopx = 0; loopx < x.length; loopx++) {
      double yInit = INTERPOLATOR.interpolate(DATA, x[loopx]);
      double[] ySensiCalculated = INTERPOLATOR.getNodeSensitivitiesForValue(DATA, x[loopx]);
      for (int loopsens = 0; loopsens < X.length; loopsens++) {
        double[] yVectorBumped = Y.clone();
        yVectorBumped[loopsens] += shift;
        Interpolator1DDataBundle dataBumped = INTERPOLATOR.getDataBundle(X, yVectorBumped);
        double yBumped = INTERPOLATOR.interpolate(dataBumped, x[loopx]);
        double ySensiExpected = (yBumped - yInit) / shift;
        assertEquals("TimeSquare interpolator: test " + loopx + " node " + loopsens, ySensiExpected, ySensiCalculated[loopsens], TOLERANCE_SENSI);
      }
    }
    int lenghtx = DATA.getKeys().length;
    double lastx = DATA.getKeys()[lenghtx - 1];
    double yInitLast = INTERPOLATOR.interpolate(DATA, lastx);
    double[] ySensiCalculated = INTERPOLATOR.getNodeSensitivitiesForValue(DATA, lastx);
    for (int loopsens = 0; loopsens < X.length; loopsens++) {
      double[] yVectorBumped = Y.clone();
      yVectorBumped[loopsens] += shift;
      Interpolator1DDataBundle dataBumped = INTERPOLATOR.getDataBundle(X, yVectorBumped);
      double yBumped = INTERPOLATOR.interpolate(dataBumped, lastx);
      double ySensiExpected = (yBumped - yInitLast) / shift;
      assertEquals("TimeSquare interpolator: test last node " + loopsens, ySensiExpected, ySensiCalculated[loopsens], TOLERANCE_SENSI);
    }
  }

  /**
   * Test first derivative values at end points
   */
  @Test
  public void firstDerivativeEndpointsTest() {
    double eps = 1.0e-5;
    double[][] xValues = new double[][] { {1., 2., 3., 4., 5., 6. }, {2., 3.6, 5., 5.1, 7.12, 8.8 } };
    double[][] yValues = new double[][] { {1., 1.1, 3., 4., 6.9, 9. }, {1., 1.6, 4., 1.1, 5.32, 7.8 } };
    int dim = xValues.length;
    Interpolator1D interp = new TimeSquareInterpolator1D();
    for (int j = 0; j < dim; ++j) {
      int nData = xValues[j].length;
      Interpolator1DDataBundle data = interp.getDataBundleFromSortedArrays(xValues[j], yValues[j]);
      double xMin = xValues[j][0];
      double xMax = xValues[j][nData - 1];
      double minFirst = (interp.interpolate(data, xMin + eps) - interp.interpolate(data, xMin)) / eps;
      double maxFirst = (interp.interpolate(data, xMax) - interp.interpolate(data, xMax - eps)) / eps;
      assertEquals("firstDerivativeInterpolatorsTest", minFirst, interp.firstDerivative(data, xMin), eps);
      assertEquals("firstDerivativeInterpolatorsTest", maxFirst, interp.firstDerivative(data, xMax), eps);
    }
  }

  /** Tests interpolation when all values are 0. */
  @Test
  public void interpolationAll0() {
    double[] xData = new double[] {1.0, 2.0, 3.0, 4.0 };
    double[] yData = new double[] {0.0, 0.0, 0.0, 0.0 };
    Interpolator1DDataBundle bundle = INTERPOLATOR.getDataBundle(xData, yData);
    double[] xTest = new double[] {1.0, 2.5, 3.0, 3.5, 4.0 };
    int nbTest = xTest.length;
    for (int i = 0; i < nbTest; i++) {
      assertEquals("SquareLinearInterpolator - 0 values", 0, INTERPOLATOR.interpolate(bundle, xTest[i]), TOLERANCE_Y);
    }
  }

  /** Tests first derivative at node when value is 0. */
  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void firstDerivativeNodeOne0Exception1() {
    double[] xData = new double[] {1.0, 2.0, 3.0 };
    double[] yData = new double[] {1.0, 0.0, 1.0 };
    Interpolator1DDataBundle bundle = INTERPOLATOR.getDataBundle(xData, yData);
    INTERPOLATOR.firstDerivative(bundle, 2.1);
  }

  /** Tests first derivative at node when value is 0. */
  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void firstDerivativeNodeOne0Exception2() {
    double[] xData = new double[] {1.0, 2.0, 3.0 };
    double[] yData = new double[] {1.0, 0.0, 1.0 };
    Interpolator1DDataBundle bundle = INTERPOLATOR.getDataBundle(xData, yData);
    INTERPOLATOR.firstDerivative(bundle, 1.9);
  }

  /** Tests sensitivity at node when all values are 0. */
  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void sensitivityNodeOne0Exception1() {
    double[] xData = new double[] {1.0, 2.0, 3.0 };
    double[] yData = new double[] {1.0, 0.0, 1.0 };
    Interpolator1DDataBundle bundle = INTERPOLATOR.getDataBundle(xData, yData);
    INTERPOLATOR.getNodeSensitivitiesForValue(bundle, 2.1);
  }

  /** Tests sensitivity at node when all values are 0. */
  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void sensitivityNodeOne0Exception2() {
    double[] xData = new double[] {1.0, 2.0, 3.0 };
    double[] yData = new double[] {1.0, 0.0, 1.0 };
    Interpolator1DDataBundle bundle = INTERPOLATOR.getDataBundle(xData, yData);
    INTERPOLATOR.getNodeSensitivitiesForValue(bundle, 1.9);
  }

  /** Tests data with a negative value. */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeValueException() {
    double[] xData = new double[] {1.0, 2.0, 3.0 };
    double[] yData = new double[] {1.0, -0.1, 1.0 };
    INTERPOLATOR.getDataBundle(xData, yData);
  }

  /** Tests data with a negative value. */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void negativeValueException2() {
    double[] xData = new double[] {1.0, 2.0, 3.0 };
    double[] yData = new double[] {1.0, -0.1, 1.0 };
    INTERPOLATOR.getDataBundleFromSortedArrays(xData, yData);
  }

}
