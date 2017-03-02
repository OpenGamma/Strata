/*
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
public class SingleRangeLimitTransformTest extends ParameterLimitsTransformTestCase {
  private static final double A = -2.5;
  private static final double B = 1.0;
  private static final ParameterLimitsTransform LOWER_LIMIT = new SingleRangeLimitTransform(B, ParameterLimitsTransform.LimitType.GREATER_THAN);
  private static final ParameterLimitsTransform UPPER_LIMIT = new SingleRangeLimitTransform(A, ParameterLimitsTransform.LimitType.LESS_THAN);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOutOfRange1() {
    LOWER_LIMIT.transform(-3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOutOfRange2() {
    UPPER_LIMIT.transform(1.01);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOutOfRange3() {
    LOWER_LIMIT.transformGradient(-3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOutOfRange4() {
    UPPER_LIMIT.transformGradient(1.01);
  }

  @Test
  public void testLower() {
    for (int i = 0; i < 10; i++) {
      final double x = B - 5 * Math.log(RANDOM.nextDouble());
      final double y = 5 * NORMAL.nextRandom();
      assertRoundTrip(LOWER_LIMIT, x);
      assertReverseRoundTrip(LOWER_LIMIT, y);
      assertGradient(LOWER_LIMIT, x);
      assertInverseGradient(LOWER_LIMIT, y);
      assertGradientRoundTrip(LOWER_LIMIT, x);
    }
  }

  @Test
  public void testUpper() {
    for (int i = 0; i < 10; i++) {
      final double x = A + 5 * Math.log(RANDOM.nextDouble());
      final double y = 5 * NORMAL.nextRandom();
      assertRoundTrip(UPPER_LIMIT, x);
      assertReverseRoundTrip(UPPER_LIMIT, y);
      assertGradient(UPPER_LIMIT, x);
      assertInverseGradient(UPPER_LIMIT, y);
      assertGradientRoundTrip(UPPER_LIMIT, x);
    }
  }

  @Test
  public void testHashCodeAndEquals() {
    ParameterLimitsTransform other = new SingleRangeLimitTransform(B, ParameterLimitsTransform.LimitType.GREATER_THAN);
    assertEquals(other, LOWER_LIMIT);
    assertEquals(other.hashCode(), LOWER_LIMIT.hashCode());
    other = new SingleRangeLimitTransform(A, ParameterLimitsTransform.LimitType.GREATER_THAN);
    assertFalse(other.equals(LOWER_LIMIT));
    other = new SingleRangeLimitTransform(B, ParameterLimitsTransform.LimitType.LESS_THAN);
    assertFalse(other.equals(LOWER_LIMIT));
  }
}
