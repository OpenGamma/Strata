/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import org.apache.commons.math3.random.Well44497b;
import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class StudentTTwoTailedCriticalValueCalculatorTest {

  private static final Well44497b RANDOM = new Well44497b(0L);
  private static final double NU = 3;
  private static final Function<Double, Double> F = new StudentTTwoTailedCriticalValueCalculator(NU);
  private static final ProbabilityDistribution<Double> T = new StudentTDistribution(NU);

  @Test
  public void testNu1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new StudentTTwoTailedCriticalValueCalculator(-3));
  }

  @Test
  public void testNu2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new StudentTTwoTailedCriticalValueCalculator(-3, null));
  }

  @Test
  public void testNullEngine() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new StudentTDistribution(3, null));
  }

  @Test
  public void testNull() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> F.apply((Double) null));
  }

  @Test
  public void testNegative() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> F.apply(-4.));
  }

  @Test
  public void test() {
    double x, y;
    final double eps = 1e-5;
    for (int i = 0; i < 100; i++) {
      x = RANDOM.nextDouble();
      y = 0.5 * (1 + x);
      assertThat(y).isCloseTo(T.getCDF(F.apply(x)), offset(eps));
    }
  }
}
