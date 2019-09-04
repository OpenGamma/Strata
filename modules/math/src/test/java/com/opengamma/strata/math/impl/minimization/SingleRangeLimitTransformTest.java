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
public class SingleRangeLimitTransformTest extends ParameterLimitsTransformTestCase {
  private static final double A = -2.5;
  private static final double B = 1.0;
  private static final ParameterLimitsTransform LOWER_LIMIT =
      new SingleRangeLimitTransform(B, ParameterLimitsTransform.LimitType.GREATER_THAN);
  private static final ParameterLimitsTransform UPPER_LIMIT =
      new SingleRangeLimitTransform(A, ParameterLimitsTransform.LimitType.LESS_THAN);

  @Test
  public void testOutOfRange1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LOWER_LIMIT.transform(-3));
  }

  @Test
  public void testOutOfRange2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> UPPER_LIMIT.transform(1.01));
  }

  @Test
  public void testOutOfRange3() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LOWER_LIMIT.transformGradient(-3));
  }

  @Test
  public void testOutOfRange4() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> UPPER_LIMIT.transformGradient(1.01));
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
    assertThat(other).isEqualTo(LOWER_LIMIT);
    assertThat(other.hashCode()).isEqualTo(LOWER_LIMIT.hashCode());
    other = new SingleRangeLimitTransform(A, ParameterLimitsTransform.LimitType.GREATER_THAN);
    assertThat(other.equals(LOWER_LIMIT)).isFalse();
    other = new SingleRangeLimitTransform(B, ParameterLimitsTransform.LimitType.LESS_THAN);
    assertThat(other.equals(LOWER_LIMIT)).isFalse();
  }
}
