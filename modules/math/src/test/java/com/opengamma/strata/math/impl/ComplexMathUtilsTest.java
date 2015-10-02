/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl;

import static com.opengamma.strata.math.impl.ComplexMathUtils.multiply;
import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class ComplexMathUtilsTest {
  private static final double V = 0.123;
  private static final double W = 0.456;
  private static final double X = 7.89;
  private static final double Y = -12.34;
  private static final ComplexNumber X_C = new ComplexNumber(X, 0);
  private static final ComplexNumber Z1 = new ComplexNumber(V, W);
  private static final ComplexNumber Z2 = new ComplexNumber(X, Y);
  private static final double EPS = 1e-9;

  @Test
  public void testNull() {
    try {
      ComplexMathUtils.add(null, Z1);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.add(Z1, null);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.add(X, null);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.add(null, X);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.arg(null);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.conjugate(null);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.divide(null, Z1);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.divide(Z1, null);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.divide(X, null);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.divide(null, X);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.exp(null);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.inverse(null);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.log(null);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.mod(null);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.multiply(null, Z1);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.multiply(Z1, null);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.multiply(X, (ComplexNumber) null);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.multiply(null, X);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.pow(null, Z1);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.pow(Z1, null);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.pow(X, null);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.pow(null, X);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.sqrt(null);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.subtract(null, Z1);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.subtract(Z1, null);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.subtract(X, null);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
    try {
      ComplexMathUtils.subtract(null, X);
    } catch (final IllegalArgumentException e) {
      assertStackTraceElement(e.getStackTrace());
    }
  }

  @Test
  public void testAddSubtract() {
    assertComplexEquals(ComplexMathUtils.subtract(ComplexMathUtils.add(Z1, Z2), Z2), Z1);
    assertComplexEquals(ComplexMathUtils.subtract(ComplexMathUtils.add(Z1, X), X), Z1);
    assertComplexEquals(ComplexMathUtils.subtract(ComplexMathUtils.add(X, Z1), Z1), X_C);
  }

  @Test
  public void testArg() {
    assertEquals(Math.atan2(W, V), ComplexMathUtils.arg(Z1), EPS);
  }

  @Test
  public void testConjugate() {
    assertComplexEquals(ComplexMathUtils.conjugate(ComplexMathUtils.conjugate(Z1)), Z1);
  }

  @Test
  public void testDivideMultiply() {
    assertComplexEquals(ComplexMathUtils.multiply(ComplexMathUtils.divide(Z1, Z2), Z2), Z1);
    assertComplexEquals(ComplexMathUtils.multiply(ComplexMathUtils.divide(Z1, X), X), Z1);
    assertComplexEquals(ComplexMathUtils.multiply(ComplexMathUtils.divide(X, Z1), Z1), X_C);
    assertComplexEquals(ComplexMathUtils.multiply(X, Z1), ComplexMathUtils.multiply(Z1, X));
  }

  @Test
  public void testMultiplyMany() {
    ComplexNumber a = multiply(Z1, multiply(Z2, Z1));
    ComplexNumber b = multiply(Z1, Z2, Z1);
    assertComplexEquals(a, b);
    double x = 3.142;
    ComplexNumber c = multiply(a, x);
    ComplexNumber d = multiply(x, Z1, Z1, Z2);
    assertComplexEquals(c, d);
  }

  @Test
  public void testExpLn() {
    assertComplexEquals(ComplexMathUtils.log(ComplexMathUtils.exp(Z1)), Z1);
    //TODO test principal value
  }

  @Test
  public void testInverse() {
    assertComplexEquals(ComplexMathUtils.inverse(ComplexMathUtils.inverse(Z1)), Z1);
  }

  @Test
  public void testModulus() {
    assertEquals(Math.sqrt(V * V + W * W), ComplexMathUtils.mod(Z1), EPS);
  }

  @Test
  public void testPower() {
    assertComplexEquals(ComplexMathUtils.pow(Z1, 0), new ComplexNumber(1, 0));
    assertComplexEquals(ComplexMathUtils.pow(X, new ComplexNumber(0, 0)), new ComplexNumber(1, 0));
    assertComplexEquals(ComplexMathUtils.sqrt(ComplexMathUtils.pow(Z1, 2)), Z1);
    assertComplexEquals(ComplexMathUtils.sqrt(ComplexMathUtils.pow(Z2, 2)), Z2);
    assertComplexEquals(ComplexMathUtils.pow(ComplexMathUtils.pow(Z1, 1. / 3), 3), Z1);
    assertComplexEquals(ComplexMathUtils.pow(ComplexMathUtils.pow(X, ComplexMathUtils.inverse(Z2)), Z2), new ComplexNumber(X, 0));
    assertComplexEquals(ComplexMathUtils.pow(ComplexMathUtils.pow(Z1, ComplexMathUtils.inverse(Z2)), Z2), Z1);
  }

  @Test
  public void testSqrt() {
    ComplexNumber z1 = new ComplexNumber(3, -2);
    ComplexNumber z2 = new ComplexNumber(-3, 4);
    ComplexNumber z3 = new ComplexNumber(-3, -4);

    ComplexNumber rZ1 = ComplexMathUtils.sqrt(z1);
    ComplexNumber rZ2 = ComplexMathUtils.sqrt(z2);
    ComplexNumber rZ3 = ComplexMathUtils.sqrt(z3);

    assertComplexEquals(ComplexMathUtils.pow(z1, 0.5), rZ1);
    assertComplexEquals(ComplexMathUtils.pow(z2, 0.5), rZ2);
    assertComplexEquals(ComplexMathUtils.pow(z3, 0.5), rZ3);

    assertComplexEquals(z1, ComplexMathUtils.square(rZ1));
    assertComplexEquals(z2, ComplexMathUtils.square(rZ2));
    assertComplexEquals(z3, ComplexMathUtils.square(rZ3));
  }

  private void assertComplexEquals(final ComplexNumber z1, final ComplexNumber z2) {
    assertEquals(z1.getReal(), z2.getReal(), EPS);
    assertEquals(z1.getImaginary(), z2.getImaginary(), EPS);
  }

  private void assertStackTraceElement(final StackTraceElement[] ste) {
    assertEquals(ste[0].getClassName(), "com.opengamma.strata.collect.ArgChecker");
  }
}
