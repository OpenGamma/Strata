/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.INTERPOLATOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.function.PiecewisePolynomialWithSensitivityFunction1D;
import com.opengamma.strata.math.impl.interpolation.PiecewiseCubicHermiteSplineInterpolatorWithSensitivity;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResult;
import com.opengamma.strata.math.impl.interpolation.PiecewisePolynomialResultsWithSensitivity;

/**
 * Test {@link PiecewiseCubicHermiteMonotonicityCurveInterpolator}.
 */
public class PiecewiseCubicHermiteMonotonicityCurveInterpolatorTest {

  private static final PiecewiseCubicHermiteSplineInterpolatorWithSensitivity BASE =
      new PiecewiseCubicHermiteSplineInterpolatorWithSensitivity();
  private static final PiecewiseCubicHermiteMonotonicityCurveInterpolator PCHIP =
      PiecewiseCubicHermiteMonotonicityCurveInterpolator.INSTANCE;
  private static final PiecewisePolynomialWithSensitivityFunction1D PPVAL = new PiecewisePolynomialWithSensitivityFunction1D();
  private static final double[] X = new double[] {0, 0.4000, 1.0000, 2.0000, 3.0000, 3.25, 5.0000};
  private static final double[][] Y = new double[][] {{1.2200, 1.0, 0.9, 1.1, 1.2000, 1.3, 1.2000}, // no flat sections
      {0.2200, 1.12, 1.5, 1.5, 1.7000, 1.8, 1.9000}, // flat middle section
      {1.2200, 1.12, 1.5, 1.5, 1.5000, 1.8, 1.9000}, // extended flat middle section
      {1.0, 1.0, 0.9, 1.1, 1.2000, 1.3, 1.3000}, // flat ends
      {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0}};

  private static final double[] XX;
  static {
    int nSamples = 66;
    XX = new double[nSamples];
    for (int i = 0; i < nSamples; i++) {
      XX[i] = -0.5 + 0.1 * i;
    }
  }
  private static final double TOL = 1.0e-14;

  @Test
  public void test_basics() {
    assertThat(PCHIP.getName()).isEqualTo(PiecewiseCubicHermiteMonotonicityCurveInterpolator.NAME);
    assertThat(PCHIP.toString()).isEqualTo(PiecewiseCubicHermiteMonotonicityCurveInterpolator.NAME);
  }

  //-------------------------------------------------------------------------
  @Test
  public void baseInterpolationTest() {
    int nExamples = Y.length;
    int n = XX.length;
    for (int example = 0; example < nExamples; example++) {
      PiecewisePolynomialResult pp = BASE.interpolate(X, Y[example]);
      BoundCurveInterpolator bound = PCHIP.bind(
          DoubleArray.ofUnsafe(X), DoubleArray.ofUnsafe(Y[example]), INTERPOLATOR, INTERPOLATOR);
      for (int i = 0; i < n; i++) {
        double computedValue = bound.interpolate(XX[i]);
        double expectedValue = PPVAL.evaluate(pp, XX[i]).get(0);
        assertThat(computedValue).isCloseTo(expectedValue, offset(1e-14));
        double computedDerivative = bound.firstDerivative(XX[i]);
        double expectedDerivative = PPVAL.differentiate(pp, XX[i]).get(0);
        assertThat(computedDerivative).isCloseTo(expectedDerivative, offset(1e-14));
      }
    }
  }

  @Test
  public void sensitivityTest() {
    int nExamples = Y.length;
    int n = XX.length;
    int nData = X.length;
    for (int example = 0; example < nExamples; example++) {
      PiecewisePolynomialResultsWithSensitivity pp = BASE.interpolateWithSensitivity(X, Y[example]);
      BoundCurveInterpolator bound = PCHIP.bind(
          DoubleArray.ofUnsafe(X), DoubleArray.ofUnsafe(Y[example]), INTERPOLATOR, INTERPOLATOR);
      for (int i = 0; i < n; i++) {
        DoubleArray computed = bound.parameterSensitivity(XX[i]);
        DoubleArray expected = PPVAL.nodeSensitivity(pp, XX[i]);
        for (int j = 0; j < nData; j++) {
          assertThat(DoubleArrayMath.fuzzyEquals(computed.toArray(), expected.toArray(), TOL)).isTrue();
        }
      }
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_serialization() {
    assertSerialization(PCHIP);
  }

}
