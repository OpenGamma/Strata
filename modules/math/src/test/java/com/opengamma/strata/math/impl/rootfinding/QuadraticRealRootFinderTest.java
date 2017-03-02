/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.opengamma.strata.math.MathException;
import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;

/**
 * Test.
 */
@Test
public class QuadraticRealRootFinderTest {
  private static final double EPS = 1e-9;
  private static final RealPolynomialFunction1D F = new RealPolynomialFunction1D(12., 7., 1.);
  private static final Polynomial1DRootFinder<Double> FINDER = new QuadraticRealRootFinder();

  @Test
  public void test() {
    try {
      FINDER.getRoots(null);
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
    try {
      FINDER.getRoots(new RealPolynomialFunction1D(1., 2., 3., 4.));
      Assert.fail();
    } catch (final IllegalArgumentException e) {
      // Expected
    }
    try {
      FINDER.getRoots(new RealPolynomialFunction1D(12., 1., 12.));
      Assert.fail();
    } catch (final MathException e) {
      // Expected
    }
    final Double[] roots = FINDER.getRoots(F);
    assertEquals(roots[0], -4.0, EPS);
    assertEquals(roots[1], -3.0, EPS);
  }
}
