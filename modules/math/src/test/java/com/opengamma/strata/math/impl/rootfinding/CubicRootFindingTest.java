/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.ComplexNumber;
import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;

/**
 * Test.
 */
@Test
public class CubicRootFindingTest {
  private static final CubicRootFinder CUBIC = new CubicRootFinder();
  private static final CubicRealRootFinder REAL_ONLY_CUBIC = new CubicRealRootFinder();
  private static final RealPolynomialFunction1D ONE_REAL_ROOT = new RealPolynomialFunction1D(-10, 10, -3, 3);
  private static final RealPolynomialFunction1D ONE_DISTINCT_ROOT = new RealPolynomialFunction1D(-1, 3, -3, 1);
  private static final RealPolynomialFunction1D THREE_ROOTS = new RealPolynomialFunction1D(-6, 11, -6, 1);
  private static final double EPS = 1e-12;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction1() {
    CUBIC.getRoots(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNonCubic1() {
    CUBIC.getRoots(new RealPolynomialFunction1D(1, 1, 1, 1, 1));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction2() {
    REAL_ONLY_CUBIC.getRoots(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNonCubic2() {
    REAL_ONLY_CUBIC.getRoots(new RealPolynomialFunction1D(1, 1, 1, 1, 1));
  }

  @Test
  public void testCubic() {
    ComplexNumber[] result = CUBIC.getRoots(ONE_REAL_ROOT);
    assertEquals(result.length, 3);
    assertComplexEquals(result[0], new ComplexNumber(1, 0));
    assertComplexEquals(result[1], new ComplexNumber(0, Math.sqrt(10 / 3.)));
    assertComplexEquals(result[2], new ComplexNumber(0, -Math.sqrt(10 / 3.)));
    result = CUBIC.getRoots(ONE_DISTINCT_ROOT);
    assertEquals(result.length, 3);
    for (final ComplexNumber c : result) {
      assertComplexEquals(c, new ComplexNumber(1, 0));
    }
    result = CUBIC.getRoots(THREE_ROOTS);
    assertEquals(result.length, 3);
    assertComplexEquals(result[0], new ComplexNumber(1, 0));
    assertComplexEquals(result[1], new ComplexNumber(3, 0));
    assertComplexEquals(result[2], new ComplexNumber(2, 0));
  }

  @Test
  public void testRealOnlyCubic() {
    Double[] result = REAL_ONLY_CUBIC.getRoots(ONE_REAL_ROOT);
    assertEquals(result.length, 1);
    assertEquals(result[0], 1, 0);
    result = REAL_ONLY_CUBIC.getRoots(ONE_DISTINCT_ROOT);
    assertEquals(result.length, 3);
    for (final Double d : result) {
      assertEquals(d, 1, EPS);
    }
    result = REAL_ONLY_CUBIC.getRoots(THREE_ROOTS);
    assertEquals(result.length, 3);
    assertEquals(result[0], 1, EPS);
    assertEquals(result[1], 3, EPS);
    assertEquals(result[2], 2, EPS);
  }

  private void assertComplexEquals(final ComplexNumber c1, final ComplexNumber c2) {
    assertEquals(c1.getReal(), c2.getReal(), EPS);
    assertEquals(c1.getImaginary(), c2.getImaginary(), EPS);
  }
}
