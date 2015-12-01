/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import static org.testng.AssertJUnit.assertEquals;

import java.util.function.Function;

import org.apache.commons.math3.random.Well44497b;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class StudentTTwoTailedCriticalValueCalculatorTest {

  private static final Well44497b RANDOM = new Well44497b(0L);
  private static final double NU = 3;
  private static final Function<Double, Double> F = new StudentTTwoTailedCriticalValueCalculator(NU);
  private static final ProbabilityDistribution<Double> T = new StudentTDistribution(NU);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNu1() {
    new StudentTTwoTailedCriticalValueCalculator(-3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNu2() {
    new StudentTTwoTailedCriticalValueCalculator(-3, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullEngine() {
    new StudentTDistribution(3, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull() {
    F.apply((Double) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegative() {
    F.apply(-4.);
  }

  @Test
  public void test() {
    double x, y;
    final double eps = 1e-5;
    for (int i = 0; i < 100; i++) {
      x = RANDOM.nextDouble();
      y = 0.5 * (1 + x);
      assertEquals(y, T.getCDF(F.apply(x)), eps);
    }
  }
}
