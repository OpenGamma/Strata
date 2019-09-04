/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link ExponentialCurveExtrapolator}.
 */
public class ExponentialCurveExtrapolatorTest {

  private static final CurveExtrapolator EXP_EXTRAPOLATOR = ExponentialCurveExtrapolator.INSTANCE;

  private static final DoubleArray X_DATA = DoubleArray.of(0.01, 0.4, 1.0, 1.8, 2.8, 5.0);
  private static final DoubleArray Y_DATA = DoubleArray.of(3.0, 4.0, 3.1, 2.0, 7.0, 2.0);
  private static final double TOLERANCE_VALUE = 1.0E-10;
  private static final double TOLERANCE_SENSI = 1.0E-5;

  @Test
  public void test_basics() {
    assertThat(EXP_EXTRAPOLATOR.getName()).isEqualTo(ExponentialCurveExtrapolator.NAME);
    assertThat(EXP_EXTRAPOLATOR.toString()).isEqualTo(ExponentialCurveExtrapolator.NAME);
  }

  @Test
  public void value() {
    BoundCurveInterpolator bci = CurveInterpolators.LINEAR.bind(X_DATA, Y_DATA, EXP_EXTRAPOLATOR, EXP_EXTRAPOLATOR);

    double mLeft = Math.log(Y_DATA.get(0)) / X_DATA.get(0);
    double mRight = Math.log(Y_DATA.get(X_DATA.size() - 1)) / X_DATA.get(X_DATA.size() - 1);
    assertThat(bci.interpolate(0.0)).isCloseTo(1d, offset(TOLERANCE_VALUE));
    assertThat(bci.interpolate(-0.2)).isCloseTo(Math.exp(mLeft * -0.2), offset(TOLERANCE_VALUE));
    assertThat(bci.interpolate(6.0)).isCloseTo(Math.exp(mRight * 6.0), offset(TOLERANCE_VALUE));
  }

  @Test
  public void sensitivity1() {
    BoundCurveInterpolator bci = CurveInterpolators.LINEAR.bind(X_DATA, Y_DATA, EXP_EXTRAPOLATOR, EXP_EXTRAPOLATOR);

    double shift = 1e-8;
    double value = 0d;
    double[] yDataShifted = Y_DATA.toArray();
    yDataShifted[0] += shift;
    BoundCurveInterpolator bciShifted1 =
        CurveInterpolators.LINEAR.bind(X_DATA, DoubleArray.ofUnsafe(yDataShifted), EXP_EXTRAPOLATOR, EXP_EXTRAPOLATOR);
    assertThat(bci.parameterSensitivity(value).get(0))
        .isCloseTo((bciShifted1.interpolate(value) - bci.interpolate(value)) / shift, offset(TOLERANCE_SENSI));
  }

  @Test
  public void sensitivity2() {
    BoundCurveInterpolator bci = CurveInterpolators.LINEAR.bind(X_DATA, Y_DATA, EXP_EXTRAPOLATOR, EXP_EXTRAPOLATOR);

    double shift = 1e-8;
    double value = -0.2;
    double[] yDataShifted = Y_DATA.toArray();
    yDataShifted[0] += shift;
    BoundCurveInterpolator bciShifted =
        CurveInterpolators.LINEAR.bind(X_DATA, DoubleArray.ofUnsafe(yDataShifted), EXP_EXTRAPOLATOR, EXP_EXTRAPOLATOR);
    assertThat(bci.parameterSensitivity(value).get(0))
        .isCloseTo((bciShifted.interpolate(value) - bci.interpolate(value)) / shift, offset(TOLERANCE_SENSI));
  }

  @Test
  public void sensitivity3() {
    BoundCurveInterpolator bci = CurveInterpolators.LINEAR.bind(X_DATA, Y_DATA, EXP_EXTRAPOLATOR, EXP_EXTRAPOLATOR);

    double shift = 1e-8;
    double value = 6d;
    double[] yDataShifted = Y_DATA.toArray();
    yDataShifted[Y_DATA.size() - 1] += shift;
    BoundCurveInterpolator bciShifted =
        CurveInterpolators.LINEAR.bind(X_DATA, DoubleArray.ofUnsafe(yDataShifted), EXP_EXTRAPOLATOR, EXP_EXTRAPOLATOR);
    assertThat(bci.parameterSensitivity(value).get(Y_DATA.size() - 1))
        .isCloseTo((bciShifted.interpolate(value) - bci.interpolate(value)) / shift, offset(TOLERANCE_SENSI));
  }

  @Test
  public void test_serialization() {
    assertSerialization(EXP_EXTRAPOLATOR);
  }

}
