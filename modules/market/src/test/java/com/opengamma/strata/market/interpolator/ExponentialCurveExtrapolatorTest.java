/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Random;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.math.impl.interpolation.ExponentialExtrapolator1D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1D;
import com.opengamma.strata.math.impl.interpolation.Interpolator1DFactory;
import com.opengamma.strata.math.impl.interpolation.data.Interpolator1DDataBundle;

/**
 * Test {@link ExponentialCurveExtrapolator}.
 */
@Test
public class ExponentialCurveExtrapolatorTest {

  private static final Random RANDOM = new Random(0L);
  private static final CurveExtrapolator EXP_EXTRAPOLATOR = ExponentialCurveExtrapolator.INSTANCE;

  private static final DoubleArray X_DATA = DoubleArray.of(0.01, 0.4, 1.0, 1.8, 2.8, 5.0);
  private static final DoubleArray Y_DATA = DoubleArray.of(3.0, 4.0, 3.1, 2.0, 7.0, 2.0);
  private static final double TOL = 1.e-12;
  private static final double TOLERANCE_VALUE = 1.0E-10;
  private static final double TOLERANCE_SENSI = 1.0E-5;

  public void test_basics() {
    assertEquals(EXP_EXTRAPOLATOR.getName(), ExponentialCurveExtrapolator.NAME);
    assertEquals(EXP_EXTRAPOLATOR.toString(), ExponentialCurveExtrapolator.NAME);
  }

  public void value() {
    BoundCurveInterpolator bci = CurveInterpolators.LINEAR.bind(X_DATA, Y_DATA, EXP_EXTRAPOLATOR, EXP_EXTRAPOLATOR);

    double mLeft = Math.log(Y_DATA.get(0)) / X_DATA.get(0);
    double mRight = Math.log(Y_DATA.get(X_DATA.size() - 1)) / X_DATA.get(X_DATA.size() - 1);
    assertEquals(bci.interpolate(0.0), 1d, TOLERANCE_VALUE);
    assertEquals(bci.interpolate(-0.2), Math.exp(mLeft * -0.2), TOLERANCE_VALUE);
    assertEquals(bci.interpolate(6.0), Math.exp(mRight * 6.0), TOLERANCE_VALUE);
  }

  public void sensitivity1() {
    BoundCurveInterpolator bci = CurveInterpolators.LINEAR.bind(X_DATA, Y_DATA, EXP_EXTRAPOLATOR, EXP_EXTRAPOLATOR);

    double shift = 1e-8;
    double value = 0d;
    double[] yDataShifted = Y_DATA.toArray();
    yDataShifted[0] += shift;
    BoundCurveInterpolator bciShifted1 =
        CurveInterpolators.LINEAR.bind(X_DATA, DoubleArray.ofUnsafe(yDataShifted), EXP_EXTRAPOLATOR, EXP_EXTRAPOLATOR);
    assertEquals(
        bci.parameterSensitivity(value).get(0),
        (bciShifted1.interpolate(value) - bci.interpolate(value)) / shift,
        TOLERANCE_SENSI);
  }

  public void sensitivity2() {
    BoundCurveInterpolator bci = CurveInterpolators.LINEAR.bind(X_DATA, Y_DATA, EXP_EXTRAPOLATOR, EXP_EXTRAPOLATOR);

    double shift = 1e-8;
    double value = -0.2;
    double[] yDataShifted = Y_DATA.toArray();
    yDataShifted[0] += shift;
    BoundCurveInterpolator bciShifted =
        CurveInterpolators.LINEAR.bind(X_DATA, DoubleArray.ofUnsafe(yDataShifted), EXP_EXTRAPOLATOR, EXP_EXTRAPOLATOR);
    assertEquals(
        bci.parameterSensitivity(value).get(0),
        (bciShifted.interpolate(value) - bci.interpolate(value)) / shift,
        TOLERANCE_SENSI);
  }

  public void sensitivity3() {
    BoundCurveInterpolator bci = CurveInterpolators.LINEAR.bind(X_DATA, Y_DATA, EXP_EXTRAPOLATOR, EXP_EXTRAPOLATOR);

    double shift = 1e-8;
    double value = 6d;
    double[] yDataShifted = Y_DATA.toArray();
    yDataShifted[Y_DATA.size() - 1] += shift;
    BoundCurveInterpolator bciShifted =
        CurveInterpolators.LINEAR.bind(X_DATA, DoubleArray.ofUnsafe(yDataShifted), EXP_EXTRAPOLATOR, EXP_EXTRAPOLATOR);
    assertEquals(
        bci.parameterSensitivity(value).get(Y_DATA.size() - 1),
        (bciShifted.interpolate(value) - bci.interpolate(value)) / shift,
        TOLERANCE_SENSI);
  }

  public void test_sameAsPrevious() {
    BoundCurveInterpolator bci = CurveInterpolators.LINEAR.bind(X_DATA, Y_DATA, EXP_EXTRAPOLATOR, EXP_EXTRAPOLATOR);
    ExponentialExtrapolator1D oldExtrap = new ExponentialExtrapolator1D();
    Interpolator1D oldInterp = Interpolator1DFactory.LINEAR_INSTANCE;
    Interpolator1DDataBundle data = oldInterp.getDataBundle(X_DATA.toArray(), Y_DATA.toArray());

    for (int i = 0; i < 100; i++) {
      final double x = RANDOM.nextDouble() * 20.0 - 10;
      if (x < 0 || x > 5.0) {
        assertEquals(bci.interpolate(x), oldExtrap.extrapolate(data, x, oldInterp), TOL);
        assertEquals(bci.firstDerivative(x), oldExtrap.firstDerivative(data, x, oldInterp), TOL);
        assertTrue(bci.parameterSensitivity(x).equalWithTolerance(
            DoubleArray.copyOf(oldExtrap.getNodeSensitivitiesForValue(data, x, oldInterp)), TOL));
      }
    }
  }

  public void test_serialization() {
    assertSerialization(EXP_EXTRAPOLATOR);
  }

}
