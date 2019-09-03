/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.DoubleArrayMath;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.IntArray;

/**
 * Test {@link StepUpperCurveInterpolator}.
 */
public class StepUpperCurveInterpolatorTest {

  private static final CurveInterpolator STEP_UPPER_INTERPOLATOR = StepUpperCurveInterpolator.INSTANCE;
  private static final CurveExtrapolator FLAT_EXTRAPOLATOR = CurveExtrapolators.FLAT;

  private static final double TOL = 1.e-12;
  private static final double SMALL = 1.e-13;
  private static final DoubleArray X_DATA = DoubleArray.of(0.0, 0.4, 1.0, 1.8, 2.8, 5.0);
  private static final DoubleArray Y_DATA = DoubleArray.of(3.0, 4.0, 3.1, 2.0, 7.0, 2.0);
  private static final int SIZE = X_DATA.size();
  private static final DoubleArray X_TEST = DoubleArray.of(-1.0, SMALL, SMALL * 100d, 0.4, 1.1, 2.3, 2.8 + SMALL, 6.0);
  private static final IntArray INDEX_TEST = IntArray.of(0, 0, 1, 1, 3, 4, 4, 5);

  @Test
  public void test_basics() {
    assertThat(STEP_UPPER_INTERPOLATOR.getName()).isEqualTo(StepUpperCurveInterpolator.NAME);
    assertThat(STEP_UPPER_INTERPOLATOR.toString()).isEqualTo(StepUpperCurveInterpolator.NAME);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_interpolation() {
    BoundCurveInterpolator bci = STEP_UPPER_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    for (int i = 0; i < X_DATA.size(); i++) {
      assertThat(bci.interpolate(X_DATA.get(i))).isCloseTo(Y_DATA.get(i), offset(TOL));
    }
    for (int i = 0; i < X_TEST.size(); i++) {
      assertThat(bci.interpolate(X_TEST.get(i))).isCloseTo(Y_DATA.get(INDEX_TEST.get(i)), offset(TOL));
    }
  }

  @Test
  public void test_firstDerivative() {
    BoundCurveInterpolator bci = STEP_UPPER_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    for (int i = 0; i < X_DATA.size(); i++) {
      assertThat(bci.firstDerivative(X_DATA.get(i))).isCloseTo(0d, offset(TOL));
    }
    for (int i = 0; i < X_TEST.size(); i++) {
      assertThat(bci.firstDerivative(X_TEST.get(i))).isCloseTo(0d, offset(TOL));
    }
  }

  @Test
  public void test_parameterSensitivity() {
    BoundCurveInterpolator bci = STEP_UPPER_INTERPOLATOR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);
    for (int i = 0; i < X_DATA.size(); i++) {
      assertThat(DoubleArrayMath.fuzzyEquals(
          bci.parameterSensitivity(X_DATA.get(i)).toArray(),
          DoubleArray.filled(SIZE).with(i, 1d).toArray(),
          TOL)).isTrue();
    }
    for (int i = 0; i < X_TEST.size(); i++) {
      assertThat(DoubleArrayMath.fuzzyEquals(
          bci.parameterSensitivity(X_TEST.get(i)).toArray(),
          DoubleArray.filled(SIZE).with(INDEX_TEST.get(i), 1d).toArray(),
          TOL)).isTrue();
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_serialization() {
    assertSerialization(STEP_UPPER_INTERPOLATOR);
  }

}
