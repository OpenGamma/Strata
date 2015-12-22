/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.google.common.base.Function;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialFunction1D;

/**
 * Test {@link ClampedPiecewisePolynomialInterpolator}.
 */
@Test
public class ClampedPiecewisePolynomialInterpolatorTest {
  private static final double[] X_VALUES = new double[] {-1.0, -0.04, 0.1, 3.2, 15.0 };
  private static final double[] Y_VALUES = new double[] {12.4, -2.03, 11.41, 11.0, 0.2 };
  private static final double[] X_CLAMPED = new double[] {-1.5, 3.4, 22.0, 0.0 };
  private static final double[] Y_CLAMPED = new double[] {6.0, 2.2, 6.1, 3.2 };
  private static final double[] X_VALUES_TOTAL = new double[] {-1.5, -1.0, -0.04, 0.0, 0.1, 3.2, 3.4, 15.0, 22.0 };
  private static final double[] Y_VALUES_TOTAL = new double[] {6.0, 12.4, -2.03, 3.2, 11.41, 11.0, 2.2, 0.2, 6.1 };
  private static final PiecewisePolynomialInterpolator[] BASE_INTERP = new PiecewisePolynomialInterpolator[] {
    new NaturalSplineInterpolator(), new PiecewiseCubicHermiteSplineInterpolatorWithSensitivity(),
    new MonotonicityPreservingCubicSplineInterpolator(new CubicSplineInterpolator()) };
  private static final double TOL = 1.0e-14;

  public void testInterpolate() {
    for (PiecewisePolynomialInterpolator baseInterp : BASE_INTERP) {
      ClampedPiecewisePolynomialInterpolator interp =
          new ClampedPiecewisePolynomialInterpolator(baseInterp, X_CLAMPED, Y_CLAMPED);
      PiecewisePolynomialResult computed = interp.interpolate(X_VALUES, Y_VALUES);
      PiecewisePolynomialResult expected = baseInterp.interpolate(X_VALUES_TOTAL, Y_VALUES_TOTAL);
      assertEquals(computed, expected);
      assertEquals(interp.getPrimaryMethod(), baseInterp);
    }
  }

  public void testInterpolateWithSensitivity() {
    for (PiecewisePolynomialInterpolator baseInterp : BASE_INTERP) {
      ClampedPiecewisePolynomialInterpolator interp =
          new ClampedPiecewisePolynomialInterpolator(baseInterp, X_CLAMPED, Y_CLAMPED);
      PiecewisePolynomialResultsWithSensitivity computed = interp.interpolateWithSensitivity(X_VALUES, Y_VALUES);
      PiecewisePolynomialResultsWithSensitivity expected =
          baseInterp.interpolateWithSensitivity(X_VALUES_TOTAL, Y_VALUES_TOTAL);
      assertEquals(computed, expected);
    }
  }

  @Test(expectedExceptions = UnsupportedOperationException.class)
  public void testInterpolateMultiDim() {
    ClampedPiecewisePolynomialInterpolator interp = new ClampedPiecewisePolynomialInterpolator(
        new NaturalSplineInterpolator(), new double[] {1d }, new double[] {2d });
    interp.interpolate(X_VALUES, new double[][] {Y_VALUES, Y_VALUES });
  }

  public void testWrongClampedPoints() {
    assertThrowsIllegalArg(() -> new ClampedPiecewisePolynomialInterpolator(
        new NaturalSplineInterpolator(), new double[] {0d }, new double[] {0d, 1d }));
    assertThrowsIllegalArg(() -> new ClampedPiecewisePolynomialInterpolator(
        new CubicSplineInterpolator(), new double[] {}, new double[] {}));
  }

  public void testFunctionalForm() {
    double[] xValues = new double[] {0.5, 1.0, 3.0, 5.0, 10.0, 30.0 };
    double lambda0 = 0.14;
    double[] lambda = new double[] {0.25, 0.05, -0.12, 0.03, -0.15, 0.0 };
    double pValueTmp = 0d;
    int nData = xValues.length;
    for (int i = 0; i < nData - 1; ++i) {
      lambda[nData - 1] += lambda[i] * xValues[i];
      pValueTmp += lambda[i];
    }
    lambda[nData - 1] *= -1d / xValues[nData - 1];
    pValueTmp += lambda[nData - 1];
    final double pValue = pValueTmp;
    Function<Double, Double> func = new Function<Double, Double>() {
      @Override
      public Double apply(Double t) {
        int index = 0;
        double res = lambda0 * t - pValue * Math.pow(t, 3) / 6.0;
        while (index < nData && t > xValues[index]) {
          res += lambda[index] * Math.pow(t - xValues[index], 3) / 6.0;
          ++index;
        }
        return res;
      }
    };
    double[] rt = new double[nData];
    for (int i = 0; i < nData; ++i) {
      rt[i] = func.apply(xValues[i]);
    }
    ClampedPiecewisePolynomialInterpolator interp =
        new ClampedPiecewisePolynomialInterpolator(BASE_INTERP[0], new double[] {0d }, new double[] {0d });
    PiecewisePolynomialResult result = interp.interpolate(xValues, rt);
    PiecewisePolynomialFunction1D polyFunc = new PiecewisePolynomialFunction1D();
    for (int i = 0; i < 600; ++i) {
      double tm = 0.05 * i;
      double exp = func.apply(tm);
      assertEquals(exp, polyFunc.evaluate(result, tm).get(0), Math.abs(exp) * TOL);
    }
  }

}
