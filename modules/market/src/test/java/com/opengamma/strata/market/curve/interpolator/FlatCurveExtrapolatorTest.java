/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.testng.Assert.assertEquals;

import java.util.Random;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link FlatCurveExtrapolator}.
 */
@Test
public class FlatCurveExtrapolatorTest {

  private static final Random RANDOM = new Random(0L);
  private static final CurveExtrapolator FLAT_EXTRAPOLATOR = FlatCurveExtrapolator.INSTANCE;

  private static final DoubleArray X_DATA = DoubleArray.of(0.0, 0.4, 1.0, 1.8, 2.8, 5.0);
  private static final DoubleArray Y_DATA = DoubleArray.of(3.0, 4.0, 3.1, 2.0, 7.0, 2.0);

  public void test_basics() {
    assertEquals(FLAT_EXTRAPOLATOR.getName(), FlatCurveExtrapolator.NAME);
    assertEquals(FLAT_EXTRAPOLATOR.toString(), FlatCurveExtrapolator.NAME);
  }

  public void test_extrapolation() {
    BoundCurveInterpolator bci = CurveInterpolators.LINEAR.bind(X_DATA, Y_DATA, FLAT_EXTRAPOLATOR, FLAT_EXTRAPOLATOR);

    for (int i = 0; i < 100; i++) {
      final double x = RANDOM.nextDouble() * 20.0 - 10;
      if (x < 0) {
        assertEquals(bci.interpolate(x), 3.0, 1e-12);
        assertEquals(bci.firstDerivative(x), 0.0, 1e-12);
        assertEquals(bci.parameterSensitivity(x).get(0), 1.0, 1e-12);
      } else if (x > 5.0) {
        assertEquals(bci.interpolate(x), 2.0, 1e-12);
        assertEquals(bci.firstDerivative(x), 0.0, 1e-12);
        assertEquals(bci.parameterSensitivity(x).get(X_DATA.size() - 1), 1.0, 1e-12);
      }
    }
  }

  public void test_serialization() {
    assertSerialization(FLAT_EXTRAPOLATOR);
  }

}
