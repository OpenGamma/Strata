/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve.interpolator;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test {@link LinearCurveExtrapolator}.
 */
@Test
public class LinearCurveExtrapolatorTest {

  private static final CurveExtrapolator LINEAR_EXTRAPOLATOR = LinearCurveExtrapolator.INSTANCE;

  private static final DoubleArray X_DATA = DoubleArray.of(0.0, 0.4, 1.0, 1.8, 2.8, 5.0);
  private static final DoubleArray Y_DATA = DoubleArray.of(3.0, 4.0, 3.1, 2.0, 7.0, 2.0);
  private static final DoubleArray X_TEST = DoubleArray.of(-1.0, 6.0);
  private static final DoubleArray Y_TEST = DoubleArray.of(-1.1, -5.272727273);

  public void test_basics() {
    assertEquals(LINEAR_EXTRAPOLATOR.getName(), LinearCurveExtrapolator.NAME);
    assertEquals(LINEAR_EXTRAPOLATOR.toString(), LinearCurveExtrapolator.NAME);
  }

  public void test_extrapolation() {
    BoundCurveInterpolator bci =
        CurveInterpolators.DOUBLE_QUADRATIC.bind(X_DATA, Y_DATA, LINEAR_EXTRAPOLATOR, LINEAR_EXTRAPOLATOR);
    for (int i = 0; i < X_TEST.size(); i++) {
      assertEquals(bci.interpolate(X_TEST.get(i)), Y_TEST.get(i), 1e-6);
    }
  }

  public void test_serialization() {
    assertSerialization(LINEAR_EXTRAPOLATOR);
  }

}
