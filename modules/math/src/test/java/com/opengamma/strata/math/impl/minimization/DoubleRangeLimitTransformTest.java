/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class DoubleRangeLimitTransformTest extends ParameterLimitsTransformTestCase {
  private static final double A = -2.5;
  private static final double B = 1.0;
  private static final ParameterLimitsTransform RANGE_LIMITS = new DoubleRangeLimitTransform(A, B);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOutOfRange1() {
    RANGE_LIMITS.transform(-3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOutOfRange2() {
    RANGE_LIMITS.transform(1.01);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOutOfRange3() {
    RANGE_LIMITS.transformGradient(-3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOutOfRange4() {
    RANGE_LIMITS.transformGradient(1.01);
  }

  @Test
  public void test() {
    for (int i = 0; i < 10; i++) {
      final double x = A + (B - A) * RANDOM.nextDouble();
      final double y = 5 * NORMAL.nextRandom();
      assertRoundTrip(RANGE_LIMITS, x);
      assertReverseRoundTrip(RANGE_LIMITS, y);

      assertGradient(RANGE_LIMITS, x);
      assertInverseGradient(RANGE_LIMITS, y);
      assertGradientRoundTrip(RANGE_LIMITS, x);
    }
  }

  @Test
  public void testHashCodeAndEquals() {
    ParameterLimitsTransform other = new DoubleRangeLimitTransform(A, B);
    assertEquals(other, RANGE_LIMITS);
    assertEquals(other.hashCode(), RANGE_LIMITS.hashCode());
    other = new DoubleRangeLimitTransform(A - 1, B);
    assertFalse(other.equals(RANGE_LIMITS));
    other = new DoubleRangeLimitTransform(A, B + 1);
    assertFalse(other.equals(RANGE_LIMITS));
  }
}
