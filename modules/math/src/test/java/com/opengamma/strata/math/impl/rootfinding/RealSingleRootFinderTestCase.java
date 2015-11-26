/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding;

import static org.testng.AssertJUnit.assertEquals;

import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.function.DoubleFunction1D;

/**
 * Abstract test.
 */
@Test
public abstract class RealSingleRootFinderTestCase {
  protected static final Function<Double, Double> F = new Function<Double, Double>() {
    @Override
    public Double apply(Double x) {
      return x * x * x - 4 * x * x + x + 6;
    }
  };
  protected static final double EPS = 1e-9;

  protected abstract RealSingleRootFinder getRootFinder();

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction() {
    getRootFinder().checkInputs((DoubleFunction1D) null, 1., 2.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullLower() {
    getRootFinder().checkInputs(F, null, 2.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullUpper() {
    getRootFinder().checkInputs(F, 1., null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOutsideRoots() {
    getRootFinder().getRoot(F, 10., 100.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBracketTwoRoots() {
    getRootFinder().getRoot(F, 1.5, 3.5);
  }

  @Test
  public void test() {
    RealSingleRootFinder finder = getRootFinder();
    assertEquals(finder.getRoot(F, 2.5, 3.5), 3, EPS);
    assertEquals(finder.getRoot(F, 1.5, 2.5), 2, EPS);
    assertEquals(finder.getRoot(F, -1.5, 0.5), -1, EPS);
  }
}
