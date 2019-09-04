/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.DISCOUNT_FACTOR_LINEAR_RIGHT_ZERO_RATE;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.DISCOUNT_FACTOR_QUADRATIC_LEFT_ZERO_RATE;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveExtrapolators.QUADRATIC_LEFT;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.LOG_NATURAL_SPLINE_MONOTONE_CUBIC;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.PRODUCT_LINEAR;
import static com.opengamma.strata.market.curve.interpolator.CurveInterpolators.PRODUCT_NATURAL_SPLINE_MONOTONE_CUBIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link DiscountFactorLinearRightZeroRateCurveExtrapolator}.
 */
public class DiscountFactorLinearRightZeroRateCurveExtrapolatorTest {

  private static final DoubleArray X_VALUES = DoubleArray.of(1.0, 2.0, 3.0, 5.0, 7.0);
  private static final DoubleArray Y_VALUES = DoubleArray.of(0.05, 0.01, 0.03, -0.01, 0.02);
  private static final int NUM_DATA = X_VALUES.size();
  private static final DoubleArray DSC_VALUES = DoubleArray.of(
      NUM_DATA, n -> Math.exp(-X_VALUES.get(n) * Y_VALUES.get(n)));
  private static final int NUM_KEYS = 20;
  private static final DoubleArray X_KEYS = DoubleArray.of(
      NUM_KEYS, n -> X_VALUES.get(NUM_DATA - 1) + 0.07d * n);
  private static final double EPS = 1.e-6;
  private static final double TOL = 1.e-11;
  private static final double TOL_E2E = 1.e-8;

  @Test
  public void basicsTest() {
    assertThat(DISCOUNT_FACTOR_LINEAR_RIGHT_ZERO_RATE.getName())
        .isEqualTo(DiscountFactorLinearRightZeroRateCurveExtrapolator.NAME);
    assertThat(DISCOUNT_FACTOR_LINEAR_RIGHT_ZERO_RATE.toString())
        .isEqualTo(DiscountFactorLinearRightZeroRateCurveExtrapolator.NAME);
  }

  @Test
  public void interpolateTest() {
    BoundCurveInterpolator bci = PRODUCT_LINEAR.bind(
        X_VALUES,
        Y_VALUES,
        LINEAR,
        DISCOUNT_FACTOR_LINEAR_RIGHT_ZERO_RATE);
    double grad = -Y_VALUES.get(NUM_DATA - 1) * DSC_VALUES.get(NUM_DATA - 1) -
        X_VALUES.get(NUM_DATA - 1) * DSC_VALUES.get(NUM_DATA - 1) *
            bci.firstDerivative(X_VALUES.get(NUM_DATA - 1));
    for (int i = 0; i < NUM_KEYS; ++i) {
      double key = X_KEYS.get(i);
      double df = grad * (key - X_VALUES.get(NUM_DATA - 1)) + DSC_VALUES.get(NUM_DATA - 1);
      assertThat(bci.interpolate(key)).isCloseTo(-Math.log(df) / key, offset(TOL));
    }
  }

  @Test
  public void derivativeTest() {
    BoundCurveInterpolator bci = PRODUCT_LINEAR.bind(
        X_VALUES,
        Y_VALUES,
        LINEAR,
        DISCOUNT_FACTOR_LINEAR_RIGHT_ZERO_RATE);
    for (int i = 0; i < NUM_KEYS; ++i) {
      double key = X_KEYS.get(i);
      double computed = bci.firstDerivative(key);
      double expected = 0.5d * (bci.interpolate(key + EPS) - bci.interpolate(key - EPS)) / EPS;
      assertThat(computed).isCloseTo(expected, offset(EPS));
    }
  }

  @Test
  public void parameterSensitivityTest() {
    BoundCurveInterpolator bci = PRODUCT_LINEAR.bind(
        X_VALUES,
        Y_VALUES,
        LINEAR,
        DISCOUNT_FACTOR_LINEAR_RIGHT_ZERO_RATE);
    for (int i = 0; i < NUM_KEYS; ++i) {
      double key = X_KEYS.get(i);
      DoubleArray computed = bci.parameterSensitivity(key);
      for (int j = 0; j < NUM_DATA; ++j) {
        double[] yValuesUp = Y_VALUES.toArray();
        double[] yValuesDw = Y_VALUES.toArray();
        yValuesUp[j] += EPS;
        yValuesDw[j] -= EPS;
        BoundCurveInterpolator bciUp = PRODUCT_LINEAR.bind(
            X_VALUES,
            DoubleArray.copyOf(yValuesUp),
            LINEAR,
            DISCOUNT_FACTOR_LINEAR_RIGHT_ZERO_RATE);
        BoundCurveInterpolator bciDw = PRODUCT_LINEAR.bind(
            X_VALUES,
            DoubleArray.copyOf(yValuesDw),
            LINEAR,
            DISCOUNT_FACTOR_LINEAR_RIGHT_ZERO_RATE);
        double expected = 0.5 * (bciUp.interpolate(key) - bciDw.interpolate(key)) / EPS;
        assertThat(computed.get(j)).isCloseTo(expected, offset(EPS * 10d));
      }
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void e2eTest() {
    BoundCurveInterpolator bciZero = PRODUCT_NATURAL_SPLINE_MONOTONE_CUBIC.bind(
        X_VALUES,
        Y_VALUES,
        DISCOUNT_FACTOR_QUADRATIC_LEFT_ZERO_RATE,
        DISCOUNT_FACTOR_LINEAR_RIGHT_ZERO_RATE);
    BoundCurveInterpolator bciDf = LOG_NATURAL_SPLINE_MONOTONE_CUBIC.bind(
        X_VALUES,
        DSC_VALUES,
        QUADRATIC_LEFT,
        LINEAR);
    int nKeys = 170;
    for (int i = 0; i < nKeys; ++i) {
      double key = -0.1d + 0.05d * i;
      double zero = bciZero.interpolate(key);
      double df = bciDf.interpolate(key);
      assertThat(Math.exp(-key * zero)).isCloseTo(df, offset(TOL_E2E));
      double zeroGrad = bciZero.firstDerivative(key);
      double dfGrad = bciDf.firstDerivative(key);
      assertThat(-zero * df - key * df * zeroGrad).isCloseTo(dfGrad, offset(TOL_E2E));
      DoubleArray zeroSensi = bciZero.parameterSensitivity(key);
      DoubleArray dfSensi = bciDf.parameterSensitivity(key);
      for (int j = 0; j < X_VALUES.size(); ++j) {
        assertThat(key * df * zeroSensi.get(j) / (X_VALUES.get(j) * DSC_VALUES.get(j)))
            .isCloseTo(dfSensi.get(j), offset(TOL_E2E * 10d));
      }
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void noLeftTest() {
    BoundCurveInterpolator bci = PRODUCT_LINEAR.bind(
        X_VALUES,
        Y_VALUES,
        DISCOUNT_FACTOR_LINEAR_RIGHT_ZERO_RATE,
        DISCOUNT_FACTOR_LINEAR_RIGHT_ZERO_RATE);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> bci.interpolate(0.2d));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> bci.firstDerivative(0.3d));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> bci.parameterSensitivity(0.6d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void serializationTest() {
    assertSerialization(DISCOUNT_FACTOR_LINEAR_RIGHT_ZERO_RATE);
  }

}
