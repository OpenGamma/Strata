/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.Assert.assertEquals;

import java.util.Arrays;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Test interpolateWithSensitivity method via PiecewisePolynomialInterpolator1D
 */
@Test
public class ConstrainedCubicSplineInterpolator1DTest {

  private static final ConstrainedCubicSplineInterpolator INTERP = new ConstrainedCubicSplineInterpolator();
  private static final ConstrainedCubicSplineInterpolator1D INTERP1D = new ConstrainedCubicSplineInterpolator1D();

  private static final double EPS = 1.e-7;

  /**
   * Recovery test on polynomial, rational, exponential functions, and node sensitivity test by finite difference method
   */
  @Test
  public void sampleFunctionTest() {
    final int nData = 10;
    final double[] xValues = new double[nData];
    final double[] yValues1 = new double[nData];
    final double[] yValues2 = new double[nData];
    final double[] yValues3 = new double[nData];
    final double[] yValues1Up = new double[nData];
    final double[] yValues2Up = new double[nData];
    final double[] yValues3Up = new double[nData];
    final double[] yValues1Dw = new double[nData];
    final double[] yValues2Dw = new double[nData];
    final double[] yValues3Dw = new double[nData];
    final double[] xKeys = new double[10 * nData];

    for (int i = 0; i < nData; ++i) {
      xValues[i] = i * i + i - 1.;
      yValues1[i] = 0.5 * xValues[i] * xValues[i] * xValues[i] - 1.5 * xValues[i] * xValues[i] + xValues[i] - 2.;
      yValues2[i] = Math.exp(0.1 * xValues[i] - 6.);
      yValues3[i] = (2. * xValues[i] * xValues[i] + xValues[i]) / (xValues[i] * xValues[i] + xValues[i] * xValues[i] * xValues[i] + 5. * xValues[i] + 2.);
      yValues1Up[i] = yValues1[i];
      yValues2Up[i] = yValues2[i];
      yValues3Up[i] = yValues3[i];
      yValues1Dw[i] = yValues1[i];
      yValues2Dw[i] = yValues2[i];
      yValues3Dw[i] = yValues3[i];
    }

    final double xMin = xValues[0];
    final double xMax = xValues[nData - 1];
    for (int i = 0; i < 10 * nData; ++i) {
      xKeys[i] = xMin + (xMax - xMin) / (10 * nData - 1) * i;
    }

    final double[] resPrim1 = INTERP.interpolate(xValues, yValues1, xKeys).getData();
    final double[] resPrim2 = INTERP.interpolate(xValues, yValues2, xKeys).getData();
    final double[] resPrim3 = INTERP.interpolate(xValues, yValues3, xKeys).getData();

    Interpolator1DDataBundle dataBund1 = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues1);
    Interpolator1DDataBundle dataBund2 = INTERP1D.getDataBundle(xValues, yValues2);
    Interpolator1DDataBundle dataBund3 = INTERP1D.getDataBundle(xValues, yValues3);
    for (int i = 0; i < 10 * nData; ++i) {
      assertEquals(resPrim1[i], INTERP1D.interpolate(dataBund1, xKeys[i]), 1.e-15);
      assertEquals(resPrim2[i], INTERP1D.interpolate(dataBund2, xKeys[i]), 1.e-15);
      assertEquals(resPrim3[i], INTERP1D.interpolate(dataBund3, xKeys[i]), 1.e-15);
    }

    for (int j = 0; j < nData; ++j) {
      yValues1Up[j] = yValues1[j] * (1. + EPS);
      yValues2Up[j] = yValues2[j] * (1. + EPS);
      yValues3Up[j] = yValues3[j] * (1. + EPS);
      yValues1Dw[j] = yValues1[j] * (1. - EPS);
      yValues2Dw[j] = yValues2[j] * (1. - EPS);
      yValues3Dw[j] = yValues3[j] * (1. - EPS);
      Interpolator1DDataBundle dataBund1Up = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues1Up);
      Interpolator1DDataBundle dataBund2Up = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues2Up);
      Interpolator1DDataBundle dataBund3Up = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues3Up);
      Interpolator1DDataBundle dataBund1Dw = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues1Dw);
      Interpolator1DDataBundle dataBund2Dw = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues2Dw);
      Interpolator1DDataBundle dataBund3Dw = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues3Dw);
      for (int i = 0; i < 10 * nData; ++i) {
        double res1 = 0.5 * (INTERP1D.interpolate(dataBund1Up, xKeys[i]) - INTERP1D.interpolate(dataBund1Dw, xKeys[i])) / EPS / yValues1[j];
        double res2 = 0.5 * (INTERP1D.interpolate(dataBund2Up, xKeys[i]) - INTERP1D.interpolate(dataBund2Dw, xKeys[i])) / EPS / yValues2[j];
        double res3 = 0.5 * (INTERP1D.interpolate(dataBund3Up, xKeys[i]) - INTERP1D.interpolate(dataBund3Dw, xKeys[i])) / EPS / yValues3[j];
        assertEquals(res1, INTERP1D.getNodeSensitivitiesForValue(dataBund1, xKeys[i])[j], Math.max(Math.abs(yValues1[j]) * EPS, EPS));
        assertEquals(res2, INTERP1D.getNodeSensitivitiesForValue(dataBund2, xKeys[i])[j], Math.max(Math.abs(yValues2[j]) * EPS, EPS));
        assertEquals(res3, INTERP1D.getNodeSensitivitiesForValue(dataBund3, xKeys[i])[j], Math.max(Math.abs(yValues3[j]) * EPS, EPS));
      }
      yValues1Up[j] = yValues1[j];
      yValues2Up[j] = yValues2[j];
      yValues3Up[j] = yValues3[j];
      yValues1Dw[j] = yValues1[j];
      yValues2Dw[j] = yValues2[j];
      yValues3Dw[j] = yValues3[j];
    }
  }

  /**
   * The sensitivity is ambiguous. 
   * The returned values are set to be in accordance with central finite difference approximation
   */
  @Test
  public void locallyFlatDataTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5., 6., 7., 8. };
    final double[] yValues = new double[] {2., 4.0, 4., 7.0, 7., 6., 6., 5. };
    final int nData = xValues.length;
    double[] yValuesUp = Arrays.copyOf(yValues, nData);
    double[] yValuesDw = Arrays.copyOf(yValues, nData);
    final double[] xKeys = new double[10 * nData];
    final double xMin = xValues[0];
    final double xMax = xValues[nData - 1];
    for (int i = 0; i < 10 * nData; ++i) {
      xKeys[i] = xMin + (xMax - xMin) / (10 * nData - 1) * i;
    }

    Interpolator1DDataBundle dataBund = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues);

    for (int j = 0; j < nData; ++j) {
      yValuesUp[j] = yValues[j] * (1. + EPS);
      yValuesDw[j] = yValues[j] * (1. - EPS);
      Interpolator1DDataBundle dataBundUp = INTERP1D.getDataBundleFromSortedArrays(xValues, yValuesUp);
      Interpolator1DDataBundle dataBundDw = INTERP1D.getDataBundleFromSortedArrays(xValues, yValuesDw);
      for (int i = 0; i < 10 * nData; ++i) {
        double res0 = 0.5 * (INTERP1D.interpolate(dataBundUp, xKeys[i]) - INTERP1D.interpolate(dataBundDw, xKeys[i])) / EPS / yValues[j];
        assertEquals(res0, INTERP1D.getNodeSensitivitiesForValue(dataBund, xKeys[i])[j], Math.max(Math.abs(yValues[j]) * EPS, EPS));
      }
      yValuesUp[j] = yValues[j];
      yValuesDw[j] = yValues[j];
    }
  }

  /**
   * If the data are completely flat, the sensitivity is not ambiguous
   */
  @Test
  public void flatDataTest() {
    final double[] xValues = new double[] {-12., -2.1, 3.9, 4.2, 5.9, 7.8, 11.3 };
    final double[] yValues = new double[] {4., 4.0, 4., 4.0, 4., 4.0, 4. };
    final int nData = xValues.length;
    double[] yValuesUp = Arrays.copyOf(yValues, nData);
    double[] yValuesDw = Arrays.copyOf(yValues, nData);
    final double[] xKeys = new double[10 * nData];
    final double xMin = xValues[0];
    final double xMax = xValues[nData - 1];
    for (int i = 0; i < 10 * nData; ++i) {
      xKeys[i] = xMin + (xMax - xMin) / (10 * nData - 1) * i;
    }

    Interpolator1DDataBundle dataBund = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues);

    for (int j = 0; j < nData; ++j) {
      yValuesUp[j] = yValues[j] * (1. + EPS);
      yValuesDw[j] = yValues[j] * (1. - EPS);
      Interpolator1DDataBundle dataBundUp = INTERP1D.getDataBundle(xValues, yValuesUp);
      Interpolator1DDataBundle dataBundDw = INTERP1D.getDataBundle(xValues, yValuesDw);
      for (int i = 0; i < 10 * nData; ++i) {
        double res0 = 0.5 * (INTERP1D.interpolate(dataBundUp, xKeys[i]) - INTERP1D.interpolate(dataBundDw, xKeys[i])) / EPS / yValues[j];
        assertEquals(res0, INTERP1D.getNodeSensitivitiesForValue(dataBund, xKeys[i])[j], Math.max(Math.abs(yValues[j]) * EPS, EPS));
      }
      yValuesUp[j] = yValues[j];
      yValuesDw[j] = yValues[j];
    }
  }

  /**
   * Data points lie on a straight line 
   */
  @Test
  public void linearDataTest() {
    final double[] xValues = new double[] {1., 2., 3., 4., 5., 6., 7., 8. };
    final double[] yValues1 = new double[] {1., 2., 3., 4., 5., 6., 7., 8. };
    final double[] yValues2 = new double[] {-11.5, -7.5, -3.5, 0.5, 4.5, 8.5, 12.5, 16.5, };
    final int nData = xValues.length;
    double[] yValues1Up = Arrays.copyOf(yValues1, nData);
    double[] yValues1Dw = Arrays.copyOf(yValues1, nData);
    double[] yValues2Up = Arrays.copyOf(yValues2, nData);
    double[] yValues2Dw = Arrays.copyOf(yValues2, nData);
    final double[] xKeys = new double[10 * nData];
    final double xMin = xValues[0];
    final double xMax = xValues[nData - 1];
    for (int i = 0; i < 10 * nData; ++i) {
      xKeys[i] = xMin + (xMax - xMin) / (10 * nData - 1) * i;
    }

    Interpolator1DDataBundle dataBund1 = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues1);
    Interpolator1DDataBundle dataBund2 = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues2);

    for (int j = 0; j < nData; ++j) {
      yValues1Up[j] = yValues1[j] * (1. + EPS);
      yValues1Dw[j] = yValues1[j] * (1. - EPS);
      yValues2Up[j] = yValues2[j] * (1. + EPS);
      yValues2Dw[j] = yValues2[j] * (1. - EPS);
      Interpolator1DDataBundle dataBund1Up = INTERP1D.getDataBundle(xValues, yValues1Up);
      Interpolator1DDataBundle dataBund1Dw = INTERP1D.getDataBundle(xValues, yValues1Dw);
      Interpolator1DDataBundle dataBund2Up = INTERP1D.getDataBundle(xValues, yValues2Up);
      Interpolator1DDataBundle dataBund2Dw = INTERP1D.getDataBundle(xValues, yValues2Dw);
      for (int i = 0; i < 10 * nData; ++i) {
        double res1 = 0.5 * (INTERP1D.interpolate(dataBund1Up, xKeys[i]) - INTERP1D.interpolate(dataBund1Dw, xKeys[i])) / EPS / yValues1[j];
        double res2 = 0.5 * (INTERP1D.interpolate(dataBund2Up, xKeys[i]) - INTERP1D.interpolate(dataBund2Dw, xKeys[i])) / EPS / yValues2[j];
        assertEquals(res1, INTERP1D.getNodeSensitivitiesForValue(dataBund1, xKeys[i])[j], Math.max(Math.abs(yValues1[j]) * EPS, EPS));
        assertEquals(res2, INTERP1D.getNodeSensitivitiesForValue(dataBund2, xKeys[i])[j], Math.max(Math.abs(yValues2[j]) * EPS, EPS));
      }
      yValues1Up[j] = yValues1[j];
      yValues1Dw[j] = yValues1[j];
      yValues2Up[j] = yValues2[j];
      yValues2Dw[j] = yValues2[j];
    }
  }

  /**
   * Endpoint condition is not relevant for constrained cubic spline
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void boundaryConditionTest() {
    final double[] xValues = new double[] {0., 1., 2., 3., };
    final double[] yValues = new double[] {0., 1., 2., 3., };

    INTERP1D.getDataBundle(xValues, yValues, 0., 0.);
  }

  /**
   * 
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void boundaryConditionSortedTest() {
    final double[] xValues = new double[] {0., 1., 2., 3., };
    final double[] yValues = new double[] {0., 1., 2., 3., };

    INTERP1D.getDataBundleFromSortedArrays(xValues, yValues, 0., 0.);
  }

}
