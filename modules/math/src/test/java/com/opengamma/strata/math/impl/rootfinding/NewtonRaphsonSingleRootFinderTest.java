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
 * Test.
 */
@Test
public class NewtonRaphsonSingleRootFinderTest {
  private static final DoubleFunction1D F1 = new DoubleFunction1D() {

    @Override
    public double applyAsDouble(double x) {
      return x * x * x - 6 * x * x + 11 * x - 106;
    }

    @Override
    public DoubleFunction1D derivative() {
      return x -> 3 * x * x - 12 * x + 11;
    }

  };
  private static final Function<Double, Double> F2 = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return x * x * x - 6 * x * x + 11 * x - 106;
    }

  };
  private static final DoubleFunction1D DF1 = x -> 3 * x * x - 12 * x + 11;
  private static final Function<Double, Double> DF2 = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return 3 * x * x - 12 * x + 11;
    }

  };
  private static final NewtonRaphsonSingleRootFinder ROOT_FINDER = new NewtonRaphsonSingleRootFinder();
  private static final double X1 = 4;
  private static final double X2 = 10;
  private static final double X3 = -10;
  private static final double X = 6;
  private static final double ROOT;
  private static final double EPS = 1e-12;

  static {
    final double q = 1. / 3;
    final double r = -50;
    final double a = Math.pow(Math.abs(r) + Math.sqrt(r * r - q * q * q), 1. / 3);
    final double b = q / a;
    ROOT = a + b + 2;
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction1() {
    ROOT_FINDER.getRoot((Function<Double, Double>) null, X1, X2);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullLower1() {
    ROOT_FINDER.getRoot(F2, (Double) null, X2);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullHigher1() {
    ROOT_FINDER.getRoot(F2, X1, (Double) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction2() {
    ROOT_FINDER.getRoot((DoubleFunction1D) null, X1, X2);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullLower2() {
    ROOT_FINDER.getRoot(F1, (Double) null, X2);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullHigher2() {
    ROOT_FINDER.getRoot(F1, X1, (Double) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction3() {
    ROOT_FINDER.getRoot(null, DF2, X1, X2);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullDerivative1() {
    ROOT_FINDER.getRoot(F2, null, X1, X2);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullLower3() {
    ROOT_FINDER.getRoot(F2, DF2, null, X2);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullHigher3() {
    ROOT_FINDER.getRoot(F2, DF2, X1, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction4() {
    ROOT_FINDER.getRoot(null, DF1, X1, X2);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullDerivative2() {
    ROOT_FINDER.getRoot(F1, null, X1, X2);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullLower4() {
    ROOT_FINDER.getRoot(F1, DF1, null, X2);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullHigher4() {
    ROOT_FINDER.getRoot(F1, DF1, X1, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testEnclosedExtremum() {
    ROOT_FINDER.getRoot(F2, DF2, X1, X3);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullDerivative3() {
    ROOT_FINDER.getRoot(F1, (DoubleFunction1D) null, X);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullDerivative4() {
    ROOT_FINDER.getRoot(F2, (Function<Double, Double>) null, X);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction5() {
    ROOT_FINDER.getRoot((Function<Double, Double>) null, X);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction6() {
    ROOT_FINDER.getRoot((DoubleFunction1D) null, X);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullInitialGuess1() {
    ROOT_FINDER.getRoot(F1, (Double) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullInitialGuess2() {
    ROOT_FINDER.getRoot(F2, (Double) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullInitialGuess3() {
    ROOT_FINDER.getRoot(F1, DF1, null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullInitialGuess4() {
    ROOT_FINDER.getRoot(F2, DF2, null);
  }

  @Test
  public void test() {
    assertEquals(ROOT_FINDER.getRoot(F2, DF2, ROOT, X2), ROOT, 0);
    assertEquals(ROOT_FINDER.getRoot(F2, DF2, X1, ROOT), ROOT, 0);
    assertEquals(ROOT_FINDER.getRoot(F1, X1, X2), ROOT, EPS);
    assertEquals(ROOT_FINDER.getRoot(F1, DF1, X1, X2), ROOT, EPS);
    assertEquals(ROOT_FINDER.getRoot(F2, X1, X2), ROOT, EPS);
    assertEquals(ROOT_FINDER.getRoot(F2, DF2, X1, X2), ROOT, EPS);
    assertEquals(ROOT_FINDER.getRoot(F1, X), ROOT, EPS);
    assertEquals(ROOT_FINDER.getRoot(F1, DF1, X), ROOT, EPS);
    assertEquals(ROOT_FINDER.getRoot(F2, X), ROOT, EPS);
    assertEquals(ROOT_FINDER.getRoot(F2, DF2, X), ROOT, EPS);
  }
}
