/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Arrays;

import org.apache.commons.math3.random.Well44497b;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class PercentileCalculatorTest {

  private static final PercentileCalculator CALCULATOR = new PercentileCalculator(0.1);
  private static final Well44497b RANDOM = new Well44497b(0L);
  private static final int N = 100;
  private static final double[] X = new double[N];

  static {
    for (int i = 0; i < N; i++) {
      X[i] = RANDOM.nextDouble();
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testHighPercentile() {
    new PercentileCalculator(1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testLowPercentile() {
    new PercentileCalculator(0);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSetHighPercentile() {
    CALCULATOR.setPercentile(1);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSetLowPercentile() {
    CALCULATOR.setPercentile(0);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullArray() {
    CALCULATOR.apply((double[]) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyArray() {
    CALCULATOR.apply(new double[0]);
  }

  @Test
  public void testExtremes() {
    final double[] y = Arrays.copyOf(X, X.length);
    Arrays.sort(y);
    CALCULATOR.setPercentile(1e-15);
    assertEquals(CALCULATOR.apply(X), y[0], 0);
    CALCULATOR.setPercentile(1 - 1e-15);
    assertEquals(CALCULATOR.apply(X), y[N - 1], 0);
  }

  @Test
  public void test() {
    assertResult(X, 10);
    assertResult(X, 99);
    assertResult(X, 50);
  }

  private void assertResult(final double[] x, final int percentile) {
    final double[] copy = Arrays.copyOf(x, N);
    Arrays.sort(copy);
    int count = 0;
    CALCULATOR.setPercentile(((double) percentile) / N);
    final double value = CALCULATOR.apply(x);
    while (copy[count++] < value) {
      //intended
    }
    assertEquals(count - 1, percentile);
  }
}
