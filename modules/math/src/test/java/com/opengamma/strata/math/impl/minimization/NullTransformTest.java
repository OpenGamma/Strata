/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class NullTransformTest extends ParameterLimitsTransformTestCase {
  private static final ParameterLimitsTransform NULL_TRANSFORM = new NullTransform();

  @Test
  public void test() {
    for (int i = 0; i < 10; i++) {
      final double y = 5 * NORMAL.nextRandom();
      assertRoundTrip(NULL_TRANSFORM, y);
      assertReverseRoundTrip(NULL_TRANSFORM, y);
      assertGradient(NULL_TRANSFORM, y);
      assertInverseGradient(NULL_TRANSFORM, y);
      assertGradientRoundTrip(NULL_TRANSFORM, y);
    }
  }

  @Test
  public void testHashCodeAndEquals() {
    ParameterLimitsTransform other = new NullTransform();
    assertThat(other).isEqualTo(NULL_TRANSFORM);
    assertThat(other.hashCode()).isEqualTo(NULL_TRANSFORM.hashCode());
    other = new ParameterLimitsTransform() {

      @Override
      public double transformGradient(final double x) {
        return 0;
      }

      @Override
      public double transform(final double x) {
        return 0;
      }

      @Override
      public double inverseTransformGradient(final double y) {
        return 0;
      }

      @Override
      public double inverseTransform(final double y) {
        return 0;
      }
    };
    assertThat(other.equals(NULL_TRANSFORM)).isFalse();
  }
}
