/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class TrigonometricFunctionUtilsTest {
  private static final Double X = 0.12;
  private static final ComplexNumber Y = new ComplexNumber(X, 0);
  private static final ComplexNumber Z = new ComplexNumber(X, -0.34);
  private static final double EPS = 1e-9;

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull1() {
    TrigonometricFunctionUtils.acos(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull2() {
    TrigonometricFunctionUtils.acosh(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull3() {
    TrigonometricFunctionUtils.asin(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull4() {
    TrigonometricFunctionUtils.asinh(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull5() {
    TrigonometricFunctionUtils.atan(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull6() {
    TrigonometricFunctionUtils.atanh(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull7() {
    TrigonometricFunctionUtils.cos(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull8() {
    TrigonometricFunctionUtils.cosh(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull9() {
    TrigonometricFunctionUtils.sin(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull10() {
    TrigonometricFunctionUtils.sinh(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull11() {
    TrigonometricFunctionUtils.tan(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull12() {
    TrigonometricFunctionUtils.tanh(null);
  }

  @Test
  public void test() {
    assertEquals(TrigonometricFunctionUtils.acos(TrigonometricFunctionUtils.cos(X)), X, EPS);
    assertEquals(TrigonometricFunctionUtils.asin(TrigonometricFunctionUtils.sin(X)), X, EPS);
    assertEquals(TrigonometricFunctionUtils.atan(TrigonometricFunctionUtils.tan(X)), X, EPS);
    assertComplexEquals(TrigonometricFunctionUtils.cos(Y), Math.cos(X));
    assertComplexEquals(TrigonometricFunctionUtils.sin(Y), Math.sin(X));
    assertComplexEquals(TrigonometricFunctionUtils.tan(Y), Math.tan(X));
    assertComplexEquals(TrigonometricFunctionUtils.acos(Y), Math.acos(X));
    assertComplexEquals(TrigonometricFunctionUtils.asin(Y), Math.asin(X));
    assertComplexEquals(TrigonometricFunctionUtils.atan(Y), Math.atan(X));
    assertComplexEquals(TrigonometricFunctionUtils.acos(TrigonometricFunctionUtils.cos(Z)), Z);
    assertComplexEquals(TrigonometricFunctionUtils.asin(TrigonometricFunctionUtils.sin(Z)), Z);
    assertComplexEquals(TrigonometricFunctionUtils.atan(TrigonometricFunctionUtils.tan(Z)), Z);
    assertEquals(TrigonometricFunctionUtils.acosh(TrigonometricFunctionUtils.cosh(X)), X, EPS);
    assertEquals(TrigonometricFunctionUtils.asinh(TrigonometricFunctionUtils.sinh(X)), X, EPS);
    assertEquals(TrigonometricFunctionUtils.atanh(TrigonometricFunctionUtils.tanh(X)), X, EPS);
    assertComplexEquals(TrigonometricFunctionUtils.acosh(TrigonometricFunctionUtils.cosh(Z)), Z);
    assertComplexEquals(TrigonometricFunctionUtils.asinh(TrigonometricFunctionUtils.sinh(Z)), Z);
    assertComplexEquals(TrigonometricFunctionUtils.atanh(TrigonometricFunctionUtils.tanh(Z)), Z);
  }

  @Test
  public void testAtanh() {
    double x = 0.76;
    ComplexNumber z = new ComplexNumber(x);
    double real = 0.5 * Math.log((1 + x) / (1 - x));
    ComplexNumber res = TrigonometricFunctionUtils.atanh(z);
    assertEquals(real, res.getReal(), 1e-15);
    assertEquals(0.0, res.getImaginary(), 0);
  }

  private void assertComplexEquals(final ComplexNumber z1, final ComplexNumber z2) {
    assertEquals(z1.getReal(), z2.getReal(), EPS);
    assertEquals(z1.getImaginary(), z2.getImaginary(), EPS);
  }

  private void assertComplexEquals(final ComplexNumber z, final double x) {
    assertEquals(z.getImaginary(), 0, EPS);
    assertEquals(z.getReal(), x, EPS);
  }
}
