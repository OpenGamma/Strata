/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.distribution;

import static org.testng.AssertJUnit.assertEquals;

import org.apache.commons.math3.random.Well44497b;
import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.function.Function1D;

/**
 * Test.
 */
@Test
public class StudentTOneTailedCriticalValueCalculatorTest {

  private static final Well44497b RANDOM = new Well44497b(0L);
  private static final double NU = 3;
  private static final Function1D<Double, Double> F = new StudentTOneTailedCriticalValueCalculator(NU);
  private static final ProbabilityDistribution<Double> T = new StudentTDistribution(NU);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNu1() {
    new StudentTOneTailedCriticalValueCalculator(-3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNu2() {
    new StudentTOneTailedCriticalValueCalculator(-3, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEngine() {
    new StudentTDistribution(3, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull() {
    F.evaluate((Double) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegative() {
    F.evaluate(-4.);
  }

  @Test
  public void test() {
    double x;
    final double eps = 1e-5;
    for (int i = 0; i < 100; i++) {
      x = RANDOM.nextDouble();
      assertEquals(x, F.evaluate(T.getCDF(x)), eps);
    }
  }
}
