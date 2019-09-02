/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class DoubleRangeLimitTransformTest extends ParameterLimitsTransformTestCase {
  private static final double A = -2.5;
  private static final double B = 1.0;
  private static final ParameterLimitsTransform RANGE_LIMITS = new DoubleRangeLimitTransform(A, B);

  @Test
  public void testOutOfRange1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RANGE_LIMITS.transform(-3));
  }

  @Test
  public void testOutOfRange2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RANGE_LIMITS.transform(1.01));
  }

  @Test
  public void testOutOfRange3() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RANGE_LIMITS.transformGradient(-3));
  }

  @Test
  public void testOutOfRange4() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> RANGE_LIMITS.transformGradient(1.01));
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
    assertThat(other).isEqualTo(RANGE_LIMITS);
    assertThat(other.hashCode()).isEqualTo(RANGE_LIMITS.hashCode());
    other = new DoubleRangeLimitTransform(A - 1, B);
    assertThat(other.equals(RANGE_LIMITS)).isFalse();
    other = new DoubleRangeLimitTransform(A, B + 1);
    assertThat(other.equals(RANGE_LIMITS)).isFalse();
  }
}
