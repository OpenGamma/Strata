/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding;

import static org.testng.AssertJUnit.assertEquals;

import java.util.Arrays;

import org.testng.annotations.Test;

import com.opengamma.strata.math.MathException;
import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;

/**
 * Test.
 */
@Test
public class LaguerrePolynomialRealRootFinderTest {
  private static final double EPS = 1e-12;
  private static final LaguerrePolynomialRealRootFinder ROOT_FINDER = new LaguerrePolynomialRealRootFinder();
  private static final RealPolynomialFunction1D TWO_REAL_ROOTS = new RealPolynomialFunction1D(12, 7, 1);
  private static final RealPolynomialFunction1D ONE_REAL_ROOT = new RealPolynomialFunction1D(9, -6, 1);
  private static final RealPolynomialFunction1D CLOSE_ROOTS = new RealPolynomialFunction1D(9 + 3 * 1e-6, -6 - 1e-6, 1);
  private static final RealPolynomialFunction1D NO_REAL_ROOTS = new RealPolynomialFunction1D(12, 0, 1);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction() {
    ROOT_FINDER.getRoots(null);
  }

  @Test(expectedExceptions = MathException.class)
  public void testNoRealRoots() {
    ROOT_FINDER.getRoots(NO_REAL_ROOTS);
  }

  @Test
  public void test() {
    Double[] result = ROOT_FINDER.getRoots(TWO_REAL_ROOTS);
    Arrays.sort(result);
    assertEquals(result.length, 2);
    assertEquals(result[0], -4, EPS);
    assertEquals(result[1], -3, EPS);
    result = ROOT_FINDER.getRoots(ONE_REAL_ROOT);
    assertEquals(result.length, 2);
    assertEquals(result[0], 3, EPS);
    assertEquals(result[1], 3, EPS);
    result = ROOT_FINDER.getRoots(CLOSE_ROOTS);
    Arrays.sort(result);
    assertEquals(result.length, 2);
    assertEquals(result[1] - result[0], 1e-6, 1e-8);
  }
}
