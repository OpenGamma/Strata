/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import static org.testng.AssertJUnit.assertEquals;

import java.util.function.Function;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;

/**
 * Test.
 */
@Test
public class DoubleFunction1DTest {

  private static final DoubleFunction1D F1 = x -> x * x * x + 2 * x * x - 7 * x + 12;
  private static final DoubleFunction1D DF1 = x -> 3 * x * x + 4 * x - 7;
  private static final DoubleFunction1D F2 = x -> Math.sin(x);
  private static final DoubleFunction1D DF2 = x -> Math.cos(x);
  private static final DoubleFunction1D F3 = new DoubleFunction1D() {

    @Override
    public double applyAsDouble(double x) {
      return x * x * x + 2 * x * x - 7 * x + 12;
    }

    @SuppressWarnings("synthetic-access")
    @Override
    public DoubleFunction1D derivative() {
      return DF1;
    }
  };
  private static final DoubleFunction1D F4 = new DoubleFunction1D() {

    @Override
    public double applyAsDouble(double x) {
      return Math.sin(x);
    }

    @SuppressWarnings("synthetic-access")
    @Override
    public DoubleFunction1D derivative() {
      return DF2;
    }
  };
  private static final double X = 0.1234;
  private static final double A = 5.67;
  private static final double EPS = 1e-15;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testAddNull() {
    F1.add(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDivideNull() {
    F1.divide(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testMultiplyNull() {
    F1.multiply(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testSubtractNull() {
    F1.subtract(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testConvertNull() {
    DoubleFunction1D.from(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testDerivativeNullType() {
    F1.derivative(null, EPS);
  }

  @Test
  public void testAdd() {
    assertEquals(F1.add(F2).applyAsDouble(X), F1.applyAsDouble(X) + F2.applyAsDouble(X), EPS);
    assertEquals(F1.add(A).applyAsDouble(X), F1.applyAsDouble(X) + A, EPS);
  }

  @Test
  public void testDivide() {
    assertEquals(F1.divide(F2).applyAsDouble(X), F1.applyAsDouble(X) / F2.applyAsDouble(X), EPS);
    assertEquals(F1.divide(A).applyAsDouble(X), F1.applyAsDouble(X) / A, EPS);
  }

  @Test
  public void testMultiply() {
    assertEquals(F1.multiply(F2).applyAsDouble(X), F1.applyAsDouble(X) * F2.applyAsDouble(X), EPS);
    assertEquals(F1.multiply(A).applyAsDouble(X), F1.applyAsDouble(X) * A, EPS);
  }

  @Test
  public void testSubtract() {
    assertEquals(F1.subtract(F2).applyAsDouble(X), F1.applyAsDouble(X) - F2.applyAsDouble(X), EPS);
    assertEquals(F1.subtract(A).applyAsDouble(X), F1.applyAsDouble(X) - A, EPS);
  }

  @Test
  public void testDerivative() {
    assertEquals(F1.derivative().applyAsDouble(X), DF1.applyAsDouble(X), 1e-3);
    assertEquals(F2.derivative().applyAsDouble(X), DF2.applyAsDouble(X), 1e-3);
    assertEquals(F1.derivative(FiniteDifferenceType.CENTRAL, 1e-5).applyAsDouble(X), DF1.applyAsDouble(X), 1e-3);
    assertEquals(F2.derivative(FiniteDifferenceType.CENTRAL, 1e-5).applyAsDouble(X), DF2.applyAsDouble(X), 1e-3);
    assertEquals(F1.derivative(FiniteDifferenceType.FORWARD, 1e-5).applyAsDouble(X), DF1.applyAsDouble(X), 1e-3);
    assertEquals(F2.derivative(FiniteDifferenceType.FORWARD, 1e-5).applyAsDouble(X), DF2.applyAsDouble(X), 1e-3);
    assertEquals(F1.derivative(FiniteDifferenceType.BACKWARD, 1e-5).applyAsDouble(X), DF1.applyAsDouble(X), 1e-3);
    assertEquals(F2.derivative(FiniteDifferenceType.BACKWARD, 1e-5).applyAsDouble(X), DF2.applyAsDouble(X), 1e-3);
    assertEquals(F3.derivative().applyAsDouble(X), DF1.applyAsDouble(X), 1e-15);
    assertEquals(F4.derivative().applyAsDouble(X), DF2.applyAsDouble(X), 1e-15);
  }

  @Test
  public void testConversion() {
    final Function<Double, Double> f1 = x -> x * x * x + 2 * x * x - 7 * x + 12;
    final DoubleFunction1D f2 = DoubleFunction1D.from(f1);
    for (int i = 0; i < 100; i++) {
      final double x = Math.random();
      assertEquals(f2.applyAsDouble(x), F1.applyAsDouble(x), 0);
      assertEquals(f2.derivative().applyAsDouble(x), F1.derivative().applyAsDouble(x), 0);
    }
  }
}
