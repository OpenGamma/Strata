/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function.special;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.math.impl.function.DoubleFunction1D;
import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;

/**
 * Test.
 */
@Test
public class LaguerrePolynomialFunctionTest {
  private static final DoubleFunction1D L0 = x -> 1d;
  private static final DoubleFunction1D L1 = x -> 1 - x;
  private static final DoubleFunction1D L2 = x -> 0.5 * (x * x - 4 * x + 2);
  private static final DoubleFunction1D L3 = x -> (-x * x * x + 9 * x * x - 18 * x + 6) / 6;
  private static final DoubleFunction1D L4 = x -> (x * x * x * x - 16 * x * x * x + 72 * x * x - 96 * x + 24) / 24;
  private static final DoubleFunction1D L5 =
    x -> (-x * x * x * x * x + 25 * x * x * x * x - 200 * x * x * x + 600 * x * x - 600 * x + 120) / 120;
  private static final DoubleFunction1D L6 =
    x -> (x * x * x * x * x * x - 36 * x * x * x * x * x + 450 * x * x * x * x - 2400 * x * x * x + 5400 * x * x - 4320 * x + 720) / 720;

  private static final DoubleFunction1D[] L = new DoubleFunction1D[] {L0, L1, L2, L3, L4, L5, L6 };
  private static final LaguerrePolynomialFunction LAGUERRE = new LaguerrePolynomialFunction();
  private static final double EPS = 1e-12;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBadN1() {
    LAGUERRE.getPolynomials(-3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBadN2() {
    LAGUERRE.getPolynomials(-3, 1);
  }

  @Test
  public void test() {
    DoubleFunction1D[] l = LAGUERRE.getPolynomials(0);
    assertEquals(l.length, 1);
    final double x = 1.23;
    assertEquals(l[0].applyAsDouble(x), 1, EPS);
    l = LAGUERRE.getPolynomials(1);
    assertEquals(l.length, 2);
    assertEquals(l[1].applyAsDouble(x), 1 - x, EPS);
    for (int i = 0; i <= 6; i++) {
      l = LAGUERRE.getPolynomials(i);
      for (int j = 0; j <= i; j++) {
        assertEquals(L[j].applyAsDouble(x), l[j].applyAsDouble(x), EPS);
      }
    }
  }

  @Test
  public void testAlpha1() {
    DoubleFunction1D[] l1, l2;
    final double x = 2.34;
    for (int i = 0; i <= 6; i++) {
      l1 = LAGUERRE.getPolynomials(i, 0);
      l2 = LAGUERRE.getPolynomials(i);
      for (int j = 0; j <= i; j++) {
        assertEquals(l1[j].applyAsDouble(x), l2[j].applyAsDouble(x), EPS);
      }
    }
    final double alpha = 3.45;
    l1 = LAGUERRE.getPolynomials(6, alpha);
    final DoubleFunction1D f0 = d -> 1d;
    final DoubleFunction1D f1 = d -> 1 + alpha - d;
    final DoubleFunction1D f2 = d -> d * d / 2 - (alpha + 2) * d + (alpha + 2) * (alpha + 1) / 2.;
    final DoubleFunction1D f3 =
        d -> -d * d * d / 6 + (alpha + 3) * d * d / 2 - (alpha + 2) * (alpha + 3) * d / 2 + (alpha + 1) * (alpha + 2) * (alpha + 3) / 6;
    assertEquals(l1[0].applyAsDouble(x), f0.applyAsDouble(x), EPS);
    assertEquals(l1[1].applyAsDouble(x), f1.applyAsDouble(x), EPS);
    assertEquals(l1[2].applyAsDouble(x), f2.applyAsDouble(x), EPS);
    assertEquals(l1[3].applyAsDouble(x), f3.applyAsDouble(x), EPS);
  }

  @Test
  public void testAlpha2() {
    final int n = 14;
    final Pair<DoubleFunction1D, DoubleFunction1D>[] polynomialAndDerivative1 = LAGUERRE.getPolynomialsAndFirstDerivative(n);
    final Pair<DoubleFunction1D, DoubleFunction1D>[] polynomialAndDerivative2 = LAGUERRE.getPolynomialsAndFirstDerivative(n, 0);
    for (int i = 0; i < n; i++) {
      assertTrue(polynomialAndDerivative1[i].getFirst() instanceof RealPolynomialFunction1D);
      assertTrue(polynomialAndDerivative2[i].getFirst() instanceof RealPolynomialFunction1D);
      final RealPolynomialFunction1D first = (RealPolynomialFunction1D) polynomialAndDerivative1[i].getFirst();
      final RealPolynomialFunction1D second = (RealPolynomialFunction1D) polynomialAndDerivative2[i].getFirst();
      assertEquals(first, second);
    }
  }
}
