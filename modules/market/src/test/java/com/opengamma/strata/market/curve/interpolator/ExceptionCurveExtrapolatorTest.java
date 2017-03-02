/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link ExceptionCurveExtrapolator}.
 */
@Test
public class ExceptionCurveExtrapolatorTest {

  private static final CurveExtrapolator EXCEPTION_EXTRAPOLATOR = ExceptionCurveExtrapolator.INSTANCE;

  private static final DoubleArray X_DATA = DoubleArray.of(0.0, 0.4, 1.0, 1.8, 2.8, 5.0);
  private static final DoubleArray Y_DATA = DoubleArray.of(3.0, 4.0, 3.1, 2.0, 7.0, 2.0);

  public void test_basics() {
    assertEquals(EXCEPTION_EXTRAPOLATOR.getName(), ExceptionCurveExtrapolator.NAME);
    assertEquals(EXCEPTION_EXTRAPOLATOR.toString(), ExceptionCurveExtrapolator.NAME);
  }

  public void test_exceptionThrown() {
    BoundCurveInterpolator bci =
        CurveInterpolators.LINEAR.bind(X_DATA, Y_DATA, EXCEPTION_EXTRAPOLATOR, EXCEPTION_EXTRAPOLATOR);
    assertThrows(() -> bci.interpolate(-1d), UnsupportedOperationException.class);
    assertThrows(() -> bci.firstDerivative(-1d), UnsupportedOperationException.class);
    assertThrows(() -> bci.parameterSensitivity(-1d), UnsupportedOperationException.class);
    assertThrows(() -> bci.interpolate(10d), UnsupportedOperationException.class);
    assertThrows(() -> bci.firstDerivative(10d), UnsupportedOperationException.class);
    assertThrows(() -> bci.parameterSensitivity(10d), UnsupportedOperationException.class);
  }

  public void test_serialization() {
    assertSerialization(EXCEPTION_EXTRAPOLATOR);
  }

}
