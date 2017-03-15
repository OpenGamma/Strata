/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import java.util.Arrays;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialWithSensitivityFunction1D;

/**
 * 
 */
@Test
public class ProductPiecewisePolynomialInterpolatorTest {
  private static final double EPS = 1.0e-12;
  private static final double DELTA = 1.0e-6;

  private static final PiecewisePolynomialInterpolator[] INTERP;
  private static final PiecewisePolynomialInterpolator[] INTERP_SENSE;
  static {
    PiecewisePolynomialInterpolator cubic = new CubicSplineInterpolator();
    PiecewisePolynomialInterpolator natural = new NaturalSplineInterpolator();
    PiecewiseCubicHermiteSplineInterpolatorWithSensitivity pchip =
        new PiecewiseCubicHermiteSplineInterpolatorWithSensitivity();
    PiecewisePolynomialInterpolator hymanNat = new MonotonicityPreservingCubicSplineInterpolator(natural);
    INTERP = new PiecewisePolynomialInterpolator[] {cubic, natural, hymanNat };
    INTERP_SENSE = new PiecewisePolynomialInterpolator[] {cubic, natural, pchip, hymanNat };
  }
  private static final PiecewisePolynomialWithSensitivityFunction1D FUNC =
      new PiecewisePolynomialWithSensitivityFunction1D();

  /**
   * No clamped points added
   */
  @Test
  public void notClampedTest() {
    double[][] xValuesSet = new double[][] { {-5.0, -1.4, 3.2, 3.5, 7.6 }, {1., 2., 4.5, 12.1, 14.2 },
      {-5.2, -3.4, -3.2, -0.9, -0.2 } };
    double[][] yValuesSet = new double[][] { {-2.2, 1.1, 1.9, 2.3, -0.1 }, {3.4, 5.2, 4.3, 1.1, 0.2 },
      {1.4, 2.2, 4.1, 1.9, 0.99 } };

    for (int k = 0; k < xValuesSet.length; ++k) {
      double[] xValues = Arrays.copyOf(xValuesSet[k], xValuesSet[k].length);
      double[] yValues = Arrays.copyOf(yValuesSet[k], yValuesSet[k].length);
      int nData = xValues.length;
      double[] xyValues = new double[nData];
      for (int j = 0; j < nData; ++j) {
        xyValues[j] = xValues[j] * yValues[j];
      }
      int nKeys = 100;
      double interval = (xValues[nData - 1] - xValues[0]) / (nKeys - 1.0);

      int n = INTERP.length;
      for (int i = 0; i < n; ++i) {
        ProductPiecewisePolynomialInterpolator interp = new ProductPiecewisePolynomialInterpolator(INTERP[i]);
        for (int j = 0; j < nKeys; ++j) {
          double key = xValues[0] + interval * j;
          InterpolatorTestUtil.assertRelative("notClampedTest", INTERP[i].interpolate(xValues, xyValues, key),
              interp.interpolate(xValues, yValues, key), EPS);
        }
      }
      n = INTERP_SENSE.length;
      for (int i = 0; i < n; ++i) {
        ProductPiecewisePolynomialInterpolator interp = new ProductPiecewisePolynomialInterpolator(INTERP_SENSE[i]);
        PiecewisePolynomialResultsWithSensitivity result = interp.interpolateWithSensitivity(xValues, yValues);
        PiecewisePolynomialResultsWithSensitivity resultBase = INTERP_SENSE[i].interpolateWithSensitivity(xValues,
            xyValues);
        for (int j = 0; j < nKeys; ++j) {
          double key = xValues[0] + interval * j;
          InterpolatorTestUtil.assertRelative("notClampedTest", FUNC.evaluate(resultBase, key).get(0), FUNC
              .evaluate(result, key)
              .get(0), EPS);
          InterpolatorTestUtil.assertArrayRelative("notClampedTest", FUNC.nodeSensitivity(resultBase, key).toArray(),
              FUNC.nodeSensitivity(result, key).toArray(), EPS);
        }
      }
    }
  }

  /**
   * Clamped points 
   */
  @Test
  public void clampedTest() {
    double[] xValues = new double[] {-5.0, -1.4, 3.2, 3.5, 7.6 };
    double[] yValues = new double[] {-2.2, 1.1, 1.9, 2.3, -0.1 };
    double[][] xValuesClampedSet = new double[][] { {0.0 }, {-7.2, -2.5, 8.45 }, {} };
    double[][] yValuesClampedSet = new double[][] { {0.0 }, {-1.2, -1.4, 2.2 }, {} };

    for (int k = 0; k < xValuesClampedSet.length; ++k) {
      double[] xValuesClamped = Arrays.copyOf(xValuesClampedSet[k], xValuesClampedSet[k].length);
      double[] yValuesClamped = Arrays.copyOf(yValuesClampedSet[k], yValuesClampedSet[k].length);
      int nData = xValues.length;
      int nClamped = xValuesClamped.length;
      int nTotal = nData + nClamped;
      double[] xValuesForBase = new double[nTotal];
      double[] yValuesForBase = new double[nTotal];
      System.arraycopy(xValues, 0, xValuesForBase, 0, nData);
      System.arraycopy(yValues, 0, yValuesForBase, 0, nData);
      System.arraycopy(xValuesClamped, 0, xValuesForBase, nData, nClamped);
      System.arraycopy(yValuesClamped, 0, yValuesForBase, nData, nClamped);
      DoubleArrayMath.sortPairs(xValuesForBase, yValuesForBase);

      double[] xyValuesBase = new double[nTotal];
      for (int j = 0; j < nTotal; ++j) {
        xyValuesBase[j] = xValuesForBase[j] * yValuesForBase[j];
      }
      int nKeys = 100;
      double interval = (xValues[nData - 1] - xValues[0]) / (nKeys - 1.0);

      int n = INTERP.length;
      for (int i = 0; i < n; ++i) {
        ProductPiecewisePolynomialInterpolator interp = new ProductPiecewisePolynomialInterpolator(INTERP[i],
            xValuesClamped, yValuesClamped);
        for (int j = 0; j < nKeys; ++j) {
          double key = xValues[0] + interval * j;
          InterpolatorTestUtil.assertRelative("clampedTest", INTERP[i].interpolate(xValuesForBase, xyValuesBase, key),
              interp.interpolate(xValues, yValues, key), EPS);
        }
      }
      n = INTERP_SENSE.length;
      for (int i = 0; i < n; ++i) {
        ProductPiecewisePolynomialInterpolator interp = new ProductPiecewisePolynomialInterpolator(INTERP_SENSE[i],
            xValuesClamped, yValuesClamped);
        PiecewisePolynomialResultsWithSensitivity result = interp.interpolateWithSensitivity(xValues, yValues);
        PiecewisePolynomialResultsWithSensitivity resultBase = INTERP_SENSE[i].interpolateWithSensitivity(
            xValuesForBase, xyValuesBase);
        for (int j = 0; j < nKeys; ++j) {
          double key = xValues[0] + interval * j;
          InterpolatorTestUtil.assertRelative("clampedTest", FUNC.evaluate(resultBase, key).get(0),
              FUNC.evaluate(result, key)
                  .get(0), EPS);
          InterpolatorTestUtil.assertArrayRelative("clampedTest", FUNC.nodeSensitivity(resultBase, key).toArray(),
              FUNC.nodeSensitivity(result, key).toArray(), EPS);
        }
      }
    }
  }

  /**
   * Test linear extrapolation without clamped points
   */
  @Test
  public void linearExtrapolationNoClampedTest() {
    double[] xValues = new double[] {-5.0, -1.4, 3.2, 3.5, 7.6 };
    double[] yValues = new double[] {-2.2, 1.1, 1.9, 2.3, -0.1 };
    int nData = xValues.length;
    int nKeys = 20;
    double interval = (3.0 * xValues[nData - 1] - xValues[nData - 1]) / (nKeys - 1);
    double[] keys = new double[nKeys];
    int n = INTERP.length;
    for (int i = 0; i < n; ++i) {
      ProductPiecewisePolynomialInterpolator interp = new ProductPiecewisePolynomialInterpolator(INTERP[i]);
      PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);
      for (int j = 0; j < nKeys; ++j) {
        keys[j] = xValues[nData - 1] + j * interval;
      }
      double[] values = FUNC.evaluate(result, keys).row(0).toArray();
      for (int j = 2; j < nKeys; ++j) {
        InterpolatorTestUtil.assertRelative("linearExtrapolationTest", values[j - 1] - values[j - 2], values[j - 1] -
            values[j - 2], EPS);
      }
    }
    n = INTERP_SENSE.length;
    for (int i = 0; i < n; ++i) {
      ProductPiecewisePolynomialInterpolator interp = new ProductPiecewisePolynomialInterpolator(INTERP_SENSE[i]);
      PiecewisePolynomialResultsWithSensitivity result = interp.interpolateWithSensitivity(xValues, yValues);
      for (int j = 0; j < nKeys; ++j) {
        keys[j] = xValues[nData - 1] + j * interval;
      }
      double[] values = FUNC.evaluate(result, keys).row(0).toArray();
      for (int j = 2; j < nKeys; ++j) {
        InterpolatorTestUtil.assertRelative("linearExtrapolationTest", values[j - 1] - values[j - 2], values[j - 1] -
            values[j - 2], EPS);
      }
      DoubleArray[] sense = FUNC.nodeSensitivity(result, keys);
      for (int k = 0; k < nData; ++k) {
        double[] yValuesUp = Arrays.copyOf(yValues, nData);
        double[] yValuesDw = Arrays.copyOf(yValues, nData);
        yValuesUp[k] += DELTA / xValues[k];
        yValuesDw[k] -= DELTA / xValues[k];
        PiecewisePolynomialResultsWithSensitivity resultUp = interp.interpolateWithSensitivity(xValues, yValuesUp);
        PiecewisePolynomialResultsWithSensitivity resultDw = interp.interpolateWithSensitivity(xValues, yValuesDw);
        double[] tmpUp = FUNC.evaluate(resultUp, keys).rowArray(0);
        double[] tmpDw = FUNC.evaluate(resultDw, keys).rowArray(0);
        for (int l = 0; l < nKeys; ++l) {
          double res = 0.5 * (tmpUp[l] - tmpDw[l]) / DELTA; // lk
          InterpolatorTestUtil.assertRelative("linearExtrapolationTest", sense[l].get(k), res, DELTA);
        }
      }
    }
  }

  /**
   * Test linear extrapolation with clamped points
   */
  public void linearExtrapolationClampedTest() {
    double[] xValues = new double[] {-5.0, -1.4, 3.2, 3.5, 7.6 };
    double[] yValues = new double[] {-2.2, 1.1, 1.9, 2.3, -0.1 };
    double[] xValuesClamped = new double[] {8.9 };
    double[] yValuesClamped = new double[] {3.2 };
    int nData = xValues.length;
    int nClamped = xValuesClamped.length;
    int nKeys = 20;
    double interval = (3.0 * xValuesClamped[nClamped - 1] - xValuesClamped[nClamped - 1]) / (nKeys - 1);
    double[] keys = new double[nKeys];
    int n = INTERP.length;
    for (int i = 0; i < n; ++i) {
      ProductPiecewisePolynomialInterpolator interp = new ProductPiecewisePolynomialInterpolator(INTERP[i],
          xValuesClamped, yValuesClamped);
      PiecewisePolynomialResult result = interp.interpolate(xValues, yValues);
      for (int j = 0; j < nKeys; ++j) {
        keys[j] = xValuesClamped[nClamped - 1] + j * interval;
      }
      double[] values = FUNC.evaluate(result, keys).row(0).toArray();
      for (int j = 2; j < nKeys; ++j) {
        InterpolatorTestUtil.assertRelative("linearExtrapolationTest", values[j - 1] - values[j - 2], values[j - 1] -
            values[j - 2], EPS);
      }
    }
    n = INTERP_SENSE.length;
    for (int i = 0; i < n; ++i) {
      ProductPiecewisePolynomialInterpolator interp = new ProductPiecewisePolynomialInterpolator(INTERP_SENSE[i],
          xValuesClamped, yValuesClamped);
      PiecewisePolynomialResultsWithSensitivity result = interp.interpolateWithSensitivity(xValues, yValues);
      for (int j = 0; j < nKeys; ++j) {
        keys[j] = xValuesClamped[nClamped - 1] + j * interval;
      }
      double[] values = FUNC.evaluate(result, keys).row(0).toArray();
      for (int j = 2; j < nKeys; ++j) {
        InterpolatorTestUtil.assertRelative("linearExtrapolationTest", values[j - 1] - values[j - 2], values[j - 1] -
            values[j - 2], EPS);
      }
      DoubleArray[] sense = FUNC.nodeSensitivity(result, keys);
      for (int k = 0; k < nData; ++k) {
        double[] yValuesUp = Arrays.copyOf(yValues, nData);
        double[] yValuesDw = Arrays.copyOf(yValues, nData);
        yValuesUp[k] += DELTA / xValues[k];
        yValuesDw[k] -= DELTA / xValues[k];
        PiecewisePolynomialResultsWithSensitivity resultUp = interp.interpolateWithSensitivity(xValues, yValuesUp);
        PiecewisePolynomialResultsWithSensitivity resultDw = interp.interpolateWithSensitivity(xValues, yValuesDw);
        double[] tmpUp = FUNC.evaluate(resultUp, keys).rowArray(0);
        double[] tmpDw = FUNC.evaluate(resultDw, keys).rowArray(0);
        for (int l = 0; l < nKeys; ++l) {
          double res = 0.5 * (tmpUp[l] - tmpDw[l]) / DELTA;
          InterpolatorTestUtil.assertRelative("linearExtrapolationTest", sense[l].get(k), res, DELTA * 10.0);
        }
      }
      ProductPiecewisePolynomialInterpolator interpUp = new ProductPiecewisePolynomialInterpolator(INTERP_SENSE[i],
          xValuesClamped, new double[] {yValuesClamped[0] + DELTA / xValuesClamped[0] });
      PiecewisePolynomialResultsWithSensitivity resultUp = interpUp.interpolateWithSensitivity(xValues, yValues);
      ProductPiecewisePolynomialInterpolator interpDw = new ProductPiecewisePolynomialInterpolator(INTERP_SENSE[i],
          xValuesClamped, new double[] {yValuesClamped[0] - DELTA / xValuesClamped[0] });
      PiecewisePolynomialResultsWithSensitivity resultDw = interpDw.interpolateWithSensitivity(xValues, yValues);
      double[] tmpUp = FUNC.evaluate(resultUp, keys).rowArray(0);
      double[] tmpDw = FUNC.evaluate(resultDw, keys).rowArray(0);
      for (int l = 0; l < nKeys; ++l) {
        double res = 0.5 * (tmpUp[l] - tmpDw[l]) / DELTA;
        InterpolatorTestUtil.assertRelative("linearExtrapolationTest", sense[l].get(nData), res, DELTA);
      }
    }
  }

  /**
   * Wrong data length
   */
  @SuppressWarnings("unused")
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void clampedDifferentLengthTest() {
    double[] xValues = new double[] {-5.0, };
    double[] yValues = new double[] {-2.2, 1.1 };
    new ProductPiecewisePolynomialInterpolator(INTERP[0], xValues, yValues);
  }

  /**
   * Wrong data length
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void dataDifferentLengthTest() {
    double[] xValues = new double[] {-5.0, -1.4, 3.2, 3.5, 7.6 };
    double[] yValues = new double[] {-2.2, 1.1, 1.9, 2.3 };
    ProductPiecewisePolynomialInterpolator interp = new ProductPiecewisePolynomialInterpolator(INTERP_SENSE[1]);
    interp.interpolate(xValues, yValues);
  }

  /**
   * Wrong data length
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void dataDifferentLengthWithSenseTest() {
    double[] xValues = new double[] {-5.0, -1.4, 3.2, 3.5 };
    double[] yValues = new double[] {-2.2, 1.1, 1.9, 2.3, 1.2 };
    ProductPiecewisePolynomialInterpolator interp = new ProductPiecewisePolynomialInterpolator(INTERP_SENSE[1]);
    interp.interpolateWithSensitivity(xValues, yValues);
  }

  /**
   * 2D method is not implemented
   */
  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void notImplementedTest() {
    double[] xValues = new double[] {-5.0, -1.4, 3.2, 3.5, 7.6 };
    double[][] yValues = new double[][] { {-2.2, 1.1, 1.9, 2.3, 1.2 }, {-2.2, 1.1, 1.9, 2.3, 1.2 } };
    ProductPiecewisePolynomialInterpolator interp = new ProductPiecewisePolynomialInterpolator(INTERP_SENSE[1]);
    interp.interpolate(xValues, yValues);
  }
}
