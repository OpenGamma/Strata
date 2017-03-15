/*
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.fxopt;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Test {@link VolatilityAndBucketedSensitivities}.
 */
@Test
public class VolatilityAndBucketedSensitivitiesTest {

  private static final double VOL = 0.34;
  private static final DoubleMatrix SENSITIVITIES = DoubleMatrix.of(2, 3, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6);
  private static final DoubleMatrix SENSITIVITIES2 = DoubleMatrix.of(1, 3, 0.1, 0.2, 0.3);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullSensitivities() {
    VolatilityAndBucketedSensitivities.of(VOL, null);
  }

  @Test
  public void test() {
    VolatilityAndBucketedSensitivities object = VolatilityAndBucketedSensitivities.of(VOL, SENSITIVITIES);
    assertEquals(VOL, object.getVolatility());
    assertEquals(SENSITIVITIES, object.getSensitivities());
    VolatilityAndBucketedSensitivities other = VolatilityAndBucketedSensitivities.of(
        VOL, DoubleMatrix.of(2, 3, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6));
    assertEquals(object, other);
    assertEquals(object.hashCode(), other.hashCode());
    other = VolatilityAndBucketedSensitivities.of(VOL + 0.01, SENSITIVITIES);
    assertFalse(other.equals(object));
    other = VolatilityAndBucketedSensitivities.of(VOL, SENSITIVITIES2);
    assertFalse(other.equals(object));
  }

}
