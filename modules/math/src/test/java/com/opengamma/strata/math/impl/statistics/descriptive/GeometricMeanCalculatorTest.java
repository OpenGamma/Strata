/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import static org.testng.Assert.assertEquals;

import java.util.function.Function;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class GeometricMeanCalculatorTest {
  private static final Function<double[], Double> ARITHMETIC = new MeanCalculator();
  private static final Function<double[], Double> GEOMETRIC = new GeometricMeanCalculator();
  private static final int N = 100;
  private static final double[] FLAT = new double[N];
  private static final double[] X = new double[N];
  private static final double[] LN_X = new double[N];

  static {
    for (int i = 0; i < N; i++) {
      FLAT[i] = 2;
      X[i] = Math.random();
      LN_X[i] = Math.log(X[i]);
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullArray() {
    GEOMETRIC.apply(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEmptyArray() {
    GEOMETRIC.apply(new double[0]);
  }

  @Test
  public void test() {
    assertEquals(GEOMETRIC.apply(FLAT), 2, 0);
    assertEquals(GEOMETRIC.apply(X), Math.exp(ARITHMETIC.apply(LN_X)), 1e-15);
  }
}
