/**
 * Copyright (C) 2013 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Test interpolateWithSensitivity method via PiecewisePolynomialInterpolator1D
 */
@Test
public class ClampedCubicSplineInterpolator1DTest {

  private static final CubicSplineInterpolator INTERP = new CubicSplineInterpolator();
  private static final ClampedCubicSplineInterpolator1D INTERP1D = new ClampedCubicSplineInterpolator1D();

  private static final double EPS = 1.e-7;

  /**
   * Recovery test on polynomial, rational, exponential functions, and node sensitivity test by finite difference method
   * Note that when conditioning number is large, both of the interpolation and node sensitivity produce a poor result
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
      xValues[i] = 0.1 * i * i + i - 8.;
      yValues1[i] = 0.5 * xValues[i] * xValues[i] * xValues[i] - 1.5 * xValues[i] * xValues[i] + xValues[i] - 2.;
      yValues2[i] = Math.exp(0.1 * xValues[i] + 1.);
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

    final double[] yValues1Add = new double[nData + 2];
    final double[] yValues2Add = new double[nData + 2];
    final double[] yValues3Add = new double[nData + 2];

    /*
     * If endpoint condition is not specified when getting data bundle, the first derivative values are set to be 0 at endpoints
     */
    {
      yValues1Add[0] = 0.;
      yValues1Add[nData + 1] = 0.;
      yValues2Add[0] = 0.;
      yValues2Add[nData + 1] = 0.;
      yValues3Add[0] = 0.;
      yValues3Add[nData + 1] = 0.;
      for (int i = 1; i < nData + 1; ++i) {
        yValues1Add[i] = yValues1[i - 1];
        yValues2Add[i] = yValues2[i - 1];
        yValues3Add[i] = yValues3[i - 1];
      }

      final double[] resPrim1 = INTERP.interpolate(xValues, yValues1Add, xKeys).toArray();
      final double[] resPrim2 = INTERP.interpolate(xValues, yValues2Add, xKeys).toArray();
      final double[] resPrim3 = INTERP.interpolate(xValues, yValues3Add, xKeys).toArray();

      Interpolator1DDataBundle dataBund1 = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues1);
      Interpolator1DDataBundle dataBund2 = INTERP1D.getDataBundle(xValues, yValues2);
      Interpolator1DDataBundle dataBund3 = INTERP1D.getDataBundle(xValues, yValues3);

      for (int i = 0; i < 10 * nData; ++i) {
        assertEquals(resPrim1[i], INTERP1D.interpolate(dataBund1, xKeys[i]), 1.e-15);
        assertEquals(resPrim2[i], INTERP1D.interpolate(dataBund2, xKeys[i]), 1.e-15);
        assertEquals(resPrim3[i], INTERP1D.interpolate(dataBund3, xKeys[i]), 1.e-15);
      }

      for (int j = 0; j < nData; ++j) {
        yValues1Up[j] = yValues1[j] == 0. ? EPS : yValues1[j] * (1. + EPS);
        yValues2Up[j] = yValues2[j] == 0. ? EPS : yValues2[j] * (1. + EPS);
        yValues3Up[j] = yValues3[j] == 0. ? EPS : yValues3[j] * (1. + EPS);
        yValues1Dw[j] = yValues1[j] == 0. ? -EPS : yValues1[j] * (1. - EPS);
        yValues2Dw[j] = yValues2[j] == 0. ? -EPS : yValues2[j] * (1. - EPS);
        yValues3Dw[j] = yValues3[j] == 0. ? -EPS : yValues3[j] * (1. - EPS);
        Interpolator1DDataBundle dataBund1Up = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues1Up);
        Interpolator1DDataBundle dataBund2Up = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues2Up);
        Interpolator1DDataBundle dataBund3Up = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues3Up);
        Interpolator1DDataBundle dataBund1Dw = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues1Dw);
        Interpolator1DDataBundle dataBund2Dw = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues2Dw);
        Interpolator1DDataBundle dataBund3Dw = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues3Dw);
        for (int i = 0; i < 10 * nData; ++i) {
          final double ref1 = yValues1[j] == 0. ? EPS : yValues1[j] * EPS;
          final double ref2 = yValues2[j] == 0. ? EPS : yValues2[j] * EPS;
          final double ref3 = yValues3[j] == 0. ? EPS : yValues3[j] * EPS;
          double res1 = 0.5 * (INTERP1D.interpolate(dataBund1Up, xKeys[i]) - INTERP1D.interpolate(dataBund1Dw, xKeys[i])) / ref1;
          double res2 = 0.5 * (INTERP1D.interpolate(dataBund2Up, xKeys[i]) - INTERP1D.interpolate(dataBund2Dw, xKeys[i])) / ref2;
          double res3 = 0.5 * (INTERP1D.interpolate(dataBund3Up, xKeys[i]) - INTERP1D.interpolate(dataBund3Dw, xKeys[i])) / ref3;
          assertEquals(res1, INTERP1D.getNodeSensitivitiesForValue(dataBund1, xKeys[i])[j], Math.max(Math.abs(yValues1[j]) * EPS, EPS) * 10.);
          assertEquals(res2, INTERP1D.getNodeSensitivitiesForValue(dataBund2, xKeys[i])[j], Math.max(Math.abs(yValues2[j]) * EPS, EPS) * 10.);
          assertEquals(res3, INTERP1D.getNodeSensitivitiesForValue(dataBund3, xKeys[i])[j], Math.max(Math.abs(yValues3[j]) * EPS, EPS) * 10.);
        }
        yValues1Up[j] = yValues1[j];
        yValues2Up[j] = yValues2[j];
        yValues3Up[j] = yValues3[j];
        yValues1Dw[j] = yValues1[j];
        yValues2Dw[j] = yValues2[j];
        yValues3Dw[j] = yValues3[j];
      }
    }

    /*
     * Endpoint condition is fixed 
     */
    for (int k = 0; k < 5; ++k) {
      final double grad1 = -0.5 + 0.25 * k;
      final double grad2 = 0.5 - 0.25 * k;
      yValues1Add[0] = grad1;
      yValues1Add[nData + 1] = grad2;
      yValues2Add[0] = grad1;
      yValues2Add[nData + 1] = grad2;
      yValues3Add[0] = grad1;
      yValues3Add[nData + 1] = grad2;
      for (int i = 1; i < nData + 1; ++i) {
        yValues1Add[i] = yValues1[i - 1];
        yValues2Add[i] = yValues2[i - 1];
        yValues3Add[i] = yValues3[i - 1];
      }

      final double[] resPrim1 = INTERP.interpolate(xValues, yValues1Add, xKeys).toArray();
      final double[] resPrim2 = INTERP.interpolate(xValues, yValues2Add, xKeys).toArray();
      final double[] resPrim3 = INTERP.interpolate(xValues, yValues3Add, xKeys).toArray();

      Interpolator1DDataBundle dataBund1 = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues1, grad1, grad2);
      Interpolator1DDataBundle dataBund2 = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues2, grad1, grad2);
      Interpolator1DDataBundle dataBund3 = INTERP1D.getDataBundle(xValues, yValues3, grad1, grad2);

      for (int i = 0; i < 10 * nData; ++i) {
        assertEquals(resPrim1[i], INTERP1D.interpolate(dataBund1, xKeys[i]), 1.e-15);
        assertEquals(resPrim2[i], INTERP1D.interpolate(dataBund2, xKeys[i]), 1.e-15);
        assertEquals(resPrim3[i], INTERP1D.interpolate(dataBund3, xKeys[i]), 1.e-15);
      }

      for (int j = 0; j < nData; ++j) {
        yValues1Up[j] = yValues1[j] == 0. ? EPS : yValues1[j] * (1. + EPS);
        yValues2Up[j] = yValues2[j] == 0. ? EPS : yValues2[j] * (1. + EPS);
        yValues3Up[j] = yValues3[j] == 0. ? EPS : yValues3[j] * (1. + EPS);
        yValues1Dw[j] = yValues1[j] == 0. ? -EPS : yValues1[j] * (1. - EPS);
        yValues2Dw[j] = yValues2[j] == 0. ? -EPS : yValues2[j] * (1. - EPS);
        yValues3Dw[j] = yValues3[j] == 0. ? -EPS : yValues3[j] * (1. - EPS);
        Interpolator1DDataBundle dataBund1Up = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues1Up, grad1, grad2);
        Interpolator1DDataBundle dataBund2Up = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues2Up, grad1, grad2);
        Interpolator1DDataBundle dataBund3Up = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues3Up, grad1, grad2);
        Interpolator1DDataBundle dataBund1Dw = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues1Dw, grad1, grad2);
        Interpolator1DDataBundle dataBund2Dw = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues2Dw, grad1, grad2);
        Interpolator1DDataBundle dataBund3Dw = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues3Dw, grad1, grad2);
        for (int i = 0; i < 10 * nData; ++i) {
          final double ref1 = yValues1[j] == 0. ? EPS : yValues1[j] * EPS;
          final double ref2 = yValues2[j] == 0. ? EPS : yValues2[j] * EPS;
          final double ref3 = yValues3[j] == 0. ? EPS : yValues3[j] * EPS;
          double res1 = 0.5 * (INTERP1D.interpolate(dataBund1Up, xKeys[i]) - INTERP1D.interpolate(dataBund1Dw, xKeys[i])) / ref1;
          double res2 = 0.5 * (INTERP1D.interpolate(dataBund2Up, xKeys[i]) - INTERP1D.interpolate(dataBund2Dw, xKeys[i])) / ref2;
          double res3 = 0.5 * (INTERP1D.interpolate(dataBund3Up, xKeys[i]) - INTERP1D.interpolate(dataBund3Dw, xKeys[i])) / ref3;
          assertEquals(res1, INTERP1D.getNodeSensitivitiesForValue(dataBund1, xKeys[i])[j], Math.max(Math.abs(yValues1[j]) * EPS, EPS) * 10.);
          assertEquals(res2, INTERP1D.getNodeSensitivitiesForValue(dataBund2, xKeys[i])[j], Math.max(Math.abs(yValues2[j]) * EPS, EPS) * 10.);
          assertEquals(res3, INTERP1D.getNodeSensitivitiesForValue(dataBund3, xKeys[i])[j], Math.max(Math.abs(yValues3[j]) * EPS, EPS) * 10.);
        }
        yValues1Up[j] = yValues1[j];
        yValues2Up[j] = yValues2[j];
        yValues3Up[j] = yValues3[j];
        yValues1Dw[j] = yValues1[j];
        yValues2Dw[j] = yValues2[j];
        yValues3Dw[j] = yValues3[j];
      }
    }
  }

  /**
   * Data points line on a straight line
   */
  @Test
  public void linearTest() {
    final int nData = 10;
    final double[] xValues = new double[nData];
    double[] yValues1 = new double[nData];
    double[] yValues2 = new double[nData];
    double[] yValues1Ext = new double[nData + 2];
    double[] yValues2Ext = new double[nData + 2];
    final double[] yValues1Up = new double[nData];
    final double[] yValues2Up = new double[nData];
    final double[] yValues1Dw = new double[nData];
    final double[] yValues2Dw = new double[nData];
    final double[] xKeys = new double[10 * nData];

    yValues1 = new double[] {3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0 };
    yValues2 = new double[] {-3.5, -2.0, -0.5, 1.0, 2.5, 4.0, 5.5, 7.0, 8.5, 10.0 };
    for (int i = 0; i < nData; ++i) {
      xValues[i] = i + 1;
      yValues1Up[i] = yValues1[i];
      yValues1Dw[i] = yValues1[i];
      yValues2Up[i] = yValues2[i];
      yValues2Dw[i] = yValues2[i];
      yValues1Ext[i + 1] = yValues1[i];
      yValues2Ext[i + 1] = yValues2[i];
    }

    final double xMin = xValues[0];
    final double xMax = xValues[nData - 1];
    for (int i = 0; i < 10 * nData; ++i) {
      xKeys[i] = xMin + (xMax - xMin) / (10 * nData - 1) * i;
    }

    {
      yValues1Ext[0] = 0.;
      yValues2Ext[0] = 0.;
      yValues1Ext[nData + 1] = 0.;
      yValues2Ext[nData + 1] = 0.;
      final double[] resPrim1 = INTERP.interpolate(xValues, yValues1Ext, xKeys).toArray();
      final double[] resPrim2 = INTERP.interpolate(xValues, yValues2Ext, xKeys).toArray();

      Interpolator1DDataBundle dataBund1 = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues1);
      Interpolator1DDataBundle dataBund2 = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues2);
      for (int i = 0; i < 10 * nData; ++i) {
        final double ref1 = resPrim1[i];
        final double ref2 = resPrim2[i];
        assertEquals(ref1, INTERP1D.interpolate(dataBund1, xKeys[i]), 1.e-15 * Math.max(Math.abs(ref1), 1.));
        assertEquals(ref2, INTERP1D.interpolate(dataBund2, xKeys[i]), 1.e-15 * Math.max(Math.abs(ref2), 1.));
      }

      for (int j = 0; j < nData; ++j) {
        final double den1 = Math.abs(yValues1[j]) == 0. ? EPS : yValues1[j] * EPS;
        final double den2 = Math.abs(yValues2[j]) == 0. ? EPS : yValues2[j] * EPS;
        yValues1Up[j] = Math.abs(yValues1[j]) == 0. ? EPS : yValues1[j] * (1. + EPS);
        yValues1Dw[j] = Math.abs(yValues1[j]) == 0. ? -EPS : yValues1[j] * (1. - EPS);
        yValues2Up[j] = Math.abs(yValues2[j]) == 0. ? EPS : yValues2[j] * (1. + EPS);
        yValues2Dw[j] = Math.abs(yValues2[j]) == 0. ? -EPS : yValues2[j] * (1. - EPS);
        Interpolator1DDataBundle dataBund1Up = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues1Up);
        Interpolator1DDataBundle dataBund2Up = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues2Up);
        Interpolator1DDataBundle dataBund1Dw = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues1Dw);
        Interpolator1DDataBundle dataBund2Dw = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues2Dw);
        for (int i = 0; i < 10 * nData; ++i) {
          double res1 = 0.5 * (INTERP1D.interpolate(dataBund1Up, xKeys[i]) - INTERP1D.interpolate(dataBund1Dw, xKeys[i])) / den1;
          double res2 = 0.5 * (INTERP1D.interpolate(dataBund2Up, xKeys[i]) - INTERP1D.interpolate(dataBund2Dw, xKeys[i])) / den2;
          assertEquals(res1, INTERP1D.getNodeSensitivitiesForValue(dataBund1, xKeys[i])[j], Math.max(Math.abs(yValues1[j]) * EPS, EPS) * 10.);
          assertEquals(res2, INTERP1D.getNodeSensitivitiesForValue(dataBund2, xKeys[i])[j], Math.max(Math.abs(yValues2[j]) * EPS, EPS) * 10.);
        }
        yValues1Up[j] = yValues1[j];
        yValues1Dw[j] = yValues1[j];
        yValues2Up[j] = yValues2[j];
        yValues2Dw[j] = yValues2[j];
      }
    }

    final double[] bdConds = new double[] {-11. / 7., -1. / 3., 0., 1., 2., 2.1 };
    final int nConds = bdConds.length;

    for (int k = 0; k < nConds; ++k) {
      for (int l = 0; l < nConds; ++l) {
        yValues1Ext[0] = bdConds[k];
        yValues2Ext[0] = bdConds[k];
        yValues1Ext[nData + 1] = bdConds[l];
        yValues2Ext[nData + 1] = bdConds[l];
        final double[] resPrim1 = INTERP.interpolate(xValues, yValues1Ext, xKeys).toArray();
        final double[] resPrim2 = INTERP.interpolate(xValues, yValues2Ext, xKeys).toArray();

        Interpolator1DDataBundle dataBund1 = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues1, bdConds[k], bdConds[l]);
        Interpolator1DDataBundle dataBund2 = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues2, bdConds[k], bdConds[l]);
        for (int i = 0; i < 10 * nData; ++i) {
          final double ref1 = resPrim1[i];
          final double ref2 = resPrim2[i];
          assertEquals(ref1, INTERP1D.interpolate(dataBund1, xKeys[i]), 1.e-15 * Math.max(Math.abs(ref1), 1.));
          assertEquals(ref2, INTERP1D.interpolate(dataBund2, xKeys[i]), 1.e-15 * Math.max(Math.abs(ref2), 1.));
        }

        for (int j = 0; j < nData; ++j) {
          final double den1 = Math.abs(yValues1[j]) == 0. ? EPS : yValues1[j] * EPS;
          final double den2 = Math.abs(yValues2[j]) == 0. ? EPS : yValues2[j] * EPS;
          yValues1Up[j] = Math.abs(yValues1[j]) == 0. ? EPS : yValues1[j] * (1. + EPS);
          yValues1Dw[j] = Math.abs(yValues1[j]) == 0. ? -EPS : yValues1[j] * (1. - EPS);
          yValues2Up[j] = Math.abs(yValues2[j]) == 0. ? EPS : yValues2[j] * (1. + EPS);
          yValues2Dw[j] = Math.abs(yValues2[j]) == 0. ? -EPS : yValues2[j] * (1. - EPS);
          Interpolator1DDataBundle dataBund1Up = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues1Up, bdConds[k], bdConds[l]);
          Interpolator1DDataBundle dataBund2Up = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues2Up, bdConds[k], bdConds[l]);
          Interpolator1DDataBundle dataBund1Dw = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues1Dw, bdConds[k], bdConds[l]);
          Interpolator1DDataBundle dataBund2Dw = INTERP1D.getDataBundleFromSortedArrays(xValues, yValues2Dw, bdConds[k], bdConds[l]);
          for (int i = 0; i < 10 * nData; ++i) {
            double res1 = 0.5 * (INTERP1D.interpolate(dataBund1Up, xKeys[i]) - INTERP1D.interpolate(dataBund1Dw, xKeys[i])) / den1;
            double res2 = 0.5 * (INTERP1D.interpolate(dataBund2Up, xKeys[i]) - INTERP1D.interpolate(dataBund2Dw, xKeys[i])) / den2;
            assertEquals(res1, INTERP1D.getNodeSensitivitiesForValue(dataBund1, xKeys[i])[j], Math.max(Math.abs(yValues1[j]) * EPS, EPS) * 10.);
            assertEquals(res2, INTERP1D.getNodeSensitivitiesForValue(dataBund2, xKeys[i])[j], Math.max(Math.abs(yValues2[j]) * EPS, EPS) * 10.);
          }
          yValues1Up[j] = yValues1[j];
          yValues1Dw[j] = yValues1[j];
          yValues2Up[j] = yValues2[j];
          yValues2Dw[j] = yValues2[j];
        }
      }
    }
  }
}
