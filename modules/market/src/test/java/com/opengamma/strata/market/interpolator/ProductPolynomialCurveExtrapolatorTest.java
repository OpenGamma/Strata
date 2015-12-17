/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialWithSensitivityFunction1D;
import com.opengamma.strata.math.impl.interpolation.CubicSplineInterpolator;
import com.opengamma.strata.math.impl.interpolation.Extrapolator1D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.math.impl.interpolation.MonotonicityPreservingCubicSplineInterpolator;
import com.opengamma.strata.math.impl.interpolation.NaturalSplineInterpolator;
import com.opengamma.strata.math.impl.interpolation.PiecewiseCubicHermiteSplineInterpolatorWithSensitivity;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialInterpolator;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResult;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResultsWithSensitivity;
import com.opengamma.strata.math.impl.interpolation.ProductPiecewisePolynomialInterpolator;
import com.opengamma.strata.math.impl.interpolation.ProductPiecewisePolynomialInterpolator1D;
import com.opengamma.strata.math.impl.interpolation.ProductPolynomialExtrapolator1D;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Test {@link ProductPolynomialCurveExtrapolator}.
 */
@Test
public class ProductPolynomialCurveExtrapolatorTest {

  private static final Random RANDOM = new Random(0L);
  private static final CurveExtrapolator PP_EXTRAPOLATOR = ProductPolynomialCurveExtrapolator.INSTANCE;

  private static final DoubleArray X_DATA = DoubleArray.of(0.3, 0.4, 1.0, 1.8, 2.8, 5.0);
  private static final DoubleArray Y_DATA = DoubleArray.of(3.0, 4.0, 3.1, 2.0, 7.0, 2.0);

  private static final PiecewisePolynomialWithSensitivityFunction1D FUNC =
      new PiecewisePolynomialWithSensitivityFunction1D();
  private static final double EPS = 1.0e-12;
  private static final double DELTA = 1.0e-6;
  private static final double TOL = 1.e-12;

  //-------------------------------------------------------------------------
  public void test_basics() {
    assertEquals(PP_EXTRAPOLATOR.getName(), ProductPolynomialCurveExtrapolator.NAME);
    assertEquals(PP_EXTRAPOLATOR.toString(), ProductPolynomialCurveExtrapolator.NAME);
  }

  @DataProvider(name = "baseInterpolator")
  Object[][] data_baseInterpolator() {
    PiecewisePolynomialInterpolator cubic = new CubicSplineInterpolator();
    PiecewisePolynomialInterpolator natural = new NaturalSplineInterpolator();
    PiecewiseCubicHermiteSplineInterpolatorWithSensitivity pchip =
        new PiecewiseCubicHermiteSplineInterpolatorWithSensitivity();
    PiecewisePolynomialInterpolator hymanNat = new MonotonicityPreservingCubicSplineInterpolator(natural);
    return new Object[][] {
        {cubic},
        {natural},
        {pchip},
        {hymanNat},
    };
  }

  // No clamped points added.
  // checking agreement with the extrapolation done by the underlying interpolation
  @Test(dataProvider = "baseInterpolator")
  public void notClampedTest(PiecewisePolynomialInterpolator baseInterpolator) {
    double[][] xValuesSet = new double[][] { {-5.0, -1.4, 3.2, 3.5, 7.6}, {1., 2., 4.5, 12.1, 14.2},
        {-5.2, -3.4, -3.2, -0.9, -0.2}};
    double[][] yValuesSet = new double[][] { {-2.2, 1.1, 1.9, 2.3, -0.1}, {3.4, 5.2, 4.3, 1.1, 0.2},
        {1.4, 2.2, 4.1, 1.9, 0.99}};

    for (int k = 0; k < xValuesSet.length; ++k) {
      double[] xValues = Arrays.copyOf(xValuesSet[k], xValuesSet[k].length);
      double[] yValues = Arrays.copyOf(yValuesSet[k], yValuesSet[k].length);
      int nData = xValues.length;
      int nKeys = 100;
      double interval = (xValues[2] - xValues[0]) / (nKeys - 1.0);

      ProductPiecewisePolynomialInterpolator interp = new ProductPiecewisePolynomialInterpolator(baseInterpolator);
      PiecewisePolynomialResult result = interp.interpolateWithSensitivity(xValues, yValues);
      ProductPiecewisePolynomialInterpolator1D interpolator1D =
          new ProductPiecewisePolynomialInterpolator1D(baseInterpolator);
      StandardCurveInterpolator pp = new StandardCurveInterpolator("ProductPiecewise", interpolator1D);
      BoundCurveInterpolator bci = pp.bind(
          DoubleArray.copyOf(xValues), DoubleArray.copyOf(yValues), PP_EXTRAPOLATOR, PP_EXTRAPOLATOR);

      // left extrapolation
      for (int j = 1; j < nKeys; ++j) {
        double key = xValues[0] - interval * j;
        assertRelative("notClampedTest", FUNC.evaluate(result, key).get(0) / key, bci.interpolate(key), EPS);
        double keyUp = key + DELTA;
        double keyDw = key - DELTA;
        double refDeriv = 0.5 * (bci.interpolate(keyUp) - bci.interpolate(keyDw)) / DELTA;
        assertRelative("notClampedTest", refDeriv,
            bci.firstDerivative(key), DELTA);
        double[] refSense = new double[nData];
        for (int l = 0; l < nData; ++l) {
          double[] yValuesUp = Arrays.copyOf(yValues, nData);
          double[] yValuesDw = Arrays.copyOf(yValues, nData);
          yValuesUp[l] += DELTA;
          yValuesDw[l] -= DELTA;
          BoundCurveInterpolator bciUp = pp.bind(
              DoubleArray.copyOf(xValues), DoubleArray.copyOf(yValuesUp), PP_EXTRAPOLATOR, PP_EXTRAPOLATOR);
          BoundCurveInterpolator bciDw = pp.bind(
              DoubleArray.copyOf(xValues), DoubleArray.copyOf(yValuesDw), PP_EXTRAPOLATOR, PP_EXTRAPOLATOR);
          refSense[l] = 0.5 * (bciUp.interpolate(key) - bciDw.interpolate(key)) / DELTA;
        }
        assertArrayRelative("notClampedTest", bci.parameterSensitivity(key), DoubleArray.copyOf(refSense), DELTA * 10.0);
      }

      // right extrapolation
      for (int j = 1; j < nKeys; ++j) {
        double key = xValues[nData - 1] + interval * j;
        assertRelative("notClampedTest", FUNC.evaluate(result, key).get(0) / key, bci.interpolate(key), EPS);
        double keyUp = key + DELTA;
        double keyDw = key - DELTA;
        double refDeriv = 0.5 * (bci.interpolate(keyUp) - bci.interpolate(keyDw)) / DELTA;
        assertRelative("notClampedTest", refDeriv,
            bci.firstDerivative(key), DELTA);
        double[] refSense = new double[nData];
        for (int l = 0; l < nData; ++l) {
          double[] yValuesUp = Arrays.copyOf(yValues, nData);
          double[] yValuesDw = Arrays.copyOf(yValues, nData);
          yValuesUp[l] += DELTA;
          yValuesDw[l] -= DELTA;
          BoundCurveInterpolator bciUp = pp.bind(
              DoubleArray.copyOf(xValues), DoubleArray.copyOf(yValuesUp), PP_EXTRAPOLATOR, PP_EXTRAPOLATOR);
          BoundCurveInterpolator bciDw = pp.bind(
              DoubleArray.copyOf(xValues), DoubleArray.copyOf(yValuesDw), PP_EXTRAPOLATOR, PP_EXTRAPOLATOR);
          refSense[l] = 0.5 * (bciUp.interpolate(key) - bciDw.interpolate(key)) / DELTA;
        }
        assertArrayRelative("notClampedTest", bci.parameterSensitivity(key), DoubleArray.copyOf(refSense), DELTA * 10.0);
      }
    }
  }

  // Clamped points.
  // checking agreement with the extrapolation done by the underlying interpolation
  @Test(dataProvider = "baseInterpolator")
  public void clampedTest(PiecewisePolynomialInterpolator baseInterpolator) {
    double[] xValues = new double[] {-5.0, -1.4, 3.2, 3.5, 7.6};
    double[] yValues = new double[] {-2.2, 1.1, 1.9, 2.3, -0.1};
    double[][] xValuesClampedSet = new double[][] { {0.0}, {-7.2, -2.5, 8.45}, {}};
    double[][] yValuesClampedSet = new double[][] { {0.0}, {-1.2, -1.4, 2.2}, {}};

    for (int k = 0; k < xValuesClampedSet.length; ++k) {
      double[] xValuesClamped = Arrays.copyOf(xValuesClampedSet[k], xValuesClampedSet[k].length);
      double[] yValuesClamped = Arrays.copyOf(yValuesClampedSet[k], yValuesClampedSet[k].length);
      int nData = xValues.length;
      int nKeys = 100;
      double interval = (xValues[2] - xValues[0]) / (nKeys - 1.0);

      ProductPiecewisePolynomialInterpolator interp =
          new ProductPiecewisePolynomialInterpolator(baseInterpolator, xValuesClamped, yValuesClamped);
      PiecewisePolynomialResultsWithSensitivity result = interp.interpolateWithSensitivity(xValues, yValues);
      ProductPiecewisePolynomialInterpolator1D interpolator1D =
          new ProductPiecewisePolynomialInterpolator1D(baseInterpolator, xValuesClamped, yValuesClamped);
      StandardCurveInterpolator pp = new StandardCurveInterpolator("ProductPiecewise", interpolator1D);
      BoundCurveInterpolator bci = pp.bind(
          DoubleArray.copyOf(xValues), DoubleArray.copyOf(yValues), PP_EXTRAPOLATOR, PP_EXTRAPOLATOR);

      // left extrapolation
      for (int j = 1; j < nKeys; ++j) {
        double key = xValues[0] - interval * j;
        assertRelative("notClampedTest", FUNC.evaluate(result, key).get(0) / key, bci.interpolate(key), EPS);
        double keyUp = key + DELTA;
        double keyDw = key - DELTA;
        double refDeriv = 0.5 * (bci.interpolate(keyUp) - bci.interpolate(keyDw)) / DELTA;
        assertRelative("notClampedTest", refDeriv, bci.firstDerivative(key), DELTA * 10.0);
        double[] refSense = new double[nData];
        for (int l = 0; l < nData; ++l) {
          double[] yValuesUp = Arrays.copyOf(yValues, nData);
          double[] yValuesDw = Arrays.copyOf(yValues, nData);
          yValuesUp[l] += DELTA;
          yValuesDw[l] -= DELTA;
          BoundCurveInterpolator bciUp = pp.bind(
              DoubleArray.copyOf(xValues), DoubleArray.copyOf(yValuesUp), PP_EXTRAPOLATOR, PP_EXTRAPOLATOR);
          BoundCurveInterpolator bciDw = pp.bind(
              DoubleArray.copyOf(xValues), DoubleArray.copyOf(yValuesDw), PP_EXTRAPOLATOR, PP_EXTRAPOLATOR);
          refSense[l] = 0.5 * (bciUp.interpolate(key) - bciDw.interpolate(key)) / DELTA;
        }
        assertArrayRelative("notClampedTest", bci.parameterSensitivity(key), DoubleArray.copyOf(refSense), DELTA * 10.0);
      }

      // right extrapolation
      for (int j = 1; j < nKeys; ++j) {
        double key = xValues[nData - 1] + interval * j;
        assertRelative("notClampedTest", FUNC.evaluate(result, key).get(0) / key, bci.interpolate(key), EPS);
        double keyUp = key + DELTA;
        double keyDw = key - DELTA;
        double refDeriv = 0.5 * (bci.interpolate(keyUp) - bci.interpolate(keyDw)) / DELTA;
        assertRelative("notClampedTest", refDeriv, bci.firstDerivative(key), DELTA * 10.0);
        double[] refSense = new double[nData];
        for (int l = 0; l < nData; ++l) {
          double[] yValuesUp = Arrays.copyOf(yValues, nData);
          double[] yValuesDw = Arrays.copyOf(yValues, nData);
          yValuesUp[l] += DELTA;
          yValuesDw[l] -= DELTA;
          BoundCurveInterpolator bciUp = pp.bind(
              DoubleArray.copyOf(xValues), DoubleArray.copyOf(yValuesUp), PP_EXTRAPOLATOR, PP_EXTRAPOLATOR);
          BoundCurveInterpolator bciDw = pp.bind(
              DoubleArray.copyOf(xValues), DoubleArray.copyOf(yValuesDw), PP_EXTRAPOLATOR, PP_EXTRAPOLATOR);
          refSense[l] = 0.5 * (bciUp.interpolate(key) - bciDw.interpolate(key)) / DELTA;
        }
        assertArrayRelative("notClampedTest", bci.parameterSensitivity(key), DoubleArray.copyOf(refSense), DELTA * 10.0);
      }
    }
  }

  // Check Math.abs(value) < SMALL is smoothly connected to general cases
  @Test(dataProvider = "baseInterpolator")
  public void closeToZeroTest(PiecewisePolynomialInterpolator baseInterpolator) {
    DoubleArray xValues = DoubleArray.of(2.4, 3.2, 3.5, 7.6);
    DoubleArray yValues = DoubleArray.of(1.1, 1.9, 2.3, -0.1);
    ProductPiecewisePolynomialInterpolator1D interpolator1D =
        new ProductPiecewisePolynomialInterpolator1D(baseInterpolator, new double[] {0.0}, new double[] {0.0});
    StandardCurveInterpolator pp = new StandardCurveInterpolator("ProductPiecewise", interpolator1D);
    BoundCurveInterpolator bci = pp.bind(xValues, yValues, PP_EXTRAPOLATOR, PP_EXTRAPOLATOR);
    double eps = 1.0e-5;
    assertRelative("closeToZeroTest", bci.interpolate(eps), bci.interpolate(0.0), eps);
    assertRelative("closeToZeroTest", bci.firstDerivative(eps), bci.firstDerivative(0.0), eps);
    assertArrayRelative("closeToZeroTest", bci.parameterSensitivity(eps), bci.parameterSensitivity(0.0), eps);
  }

  public void test_sameAsPrevious() {
    Interpolator1D oldInterp = new ProductPiecewisePolynomialInterpolator1D(new CubicSplineInterpolator());
    StandardCurveInterpolator pp = new StandardCurveInterpolator("ProductPiecewise", oldInterp);
    BoundCurveInterpolator bci = pp.bind(X_DATA, Y_DATA, PP_EXTRAPOLATOR, PP_EXTRAPOLATOR);
    Extrapolator1D oldExtrap = new ProductPolynomialExtrapolator1D();
    Interpolator1DDataBundle data = oldInterp.getDataBundle(X_DATA.toArray(), Y_DATA.toArray());

    for (int i = 0; i < 100; i++) {
      double x = RANDOM.nextDouble() * 20.0 - 10;
      if (x < 0.3) {
        assertEquals(bci.interpolate(x), oldExtrap.extrapolate(data, x, oldInterp), TOL);
        assertEquals(bci.firstDerivative(x), oldExtrap.firstDerivative(data, x, oldInterp), TOL);
        assertTrue(bci.parameterSensitivity(x).equalWithTolerance(
            DoubleArray.copyOf(oldExtrap.getNodeSensitivitiesForValue(data, x, oldInterp)), TOL));
      }
    }
  }

  public void test_serialization() {
    assertSerialization(PP_EXTRAPOLATOR);
  }

  //-------------------------------------------------------------------------
  private static void assertArrayRelative(String message, DoubleArray expected, DoubleArray obtained, double relativeTol) {
    int nData = expected.size();
    assertEquals(obtained.size(), nData, message);
    for (int i = 0; i < nData; ++i) {
      assertRelative(message, expected.get(i), obtained.get(i), relativeTol);
    }
  }

  private static void assertRelative(String message, double expected, double obtained, double relativeTol) {
    double ref = Math.max(Math.abs(expected), 1d);
    assertEquals(obtained, expected, ref * relativeTol, message);
  }

}
