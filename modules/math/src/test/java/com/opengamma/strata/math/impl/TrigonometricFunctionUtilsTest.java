/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class TrigonometricFunctionUtilsTest {
  private static final Double X = 0.12;
  private static final ComplexNumber Y = new ComplexNumber(X, 0);
  private static final ComplexNumber Z = new ComplexNumber(X, -0.34);
  private static final double EPS = 1e-9;

  @Test
  public void testNull1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TrigonometricFunctionUtils.acos(null));
  }

  @Test
  public void testNull2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TrigonometricFunctionUtils.acosh(null));
  }

  @Test
  public void testNull3() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TrigonometricFunctionUtils.asin(null));
  }

  @Test
  public void testNull4() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TrigonometricFunctionUtils.asinh(null));
  }

  @Test
  public void testNull5() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TrigonometricFunctionUtils.atan(null));
  }

  @Test
  public void testNull6() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TrigonometricFunctionUtils.atanh(null));
  }

  @Test
  public void testNull7() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TrigonometricFunctionUtils.cos(null));
  }

  @Test
  public void testNull8() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TrigonometricFunctionUtils.cosh(null));
  }

  @Test
  public void testNull9() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TrigonometricFunctionUtils.sin(null));
  }

  @Test
  public void testNull10() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TrigonometricFunctionUtils.sinh(null));
  }

  @Test
  public void testNull11() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TrigonometricFunctionUtils.tan(null));
  }

  @Test
  public void testNull12() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> TrigonometricFunctionUtils.tanh(null));
  }

  @Test
  public void test() {
    assertThat(TrigonometricFunctionUtils.acos(TrigonometricFunctionUtils.cos(X))).isCloseTo(X, offset(EPS));
    assertThat(TrigonometricFunctionUtils.asin(TrigonometricFunctionUtils.sin(X))).isCloseTo(X, offset(EPS));
    assertThat(TrigonometricFunctionUtils.atan(TrigonometricFunctionUtils.tan(X))).isCloseTo(X, offset(EPS));
    assertComplexEquals(TrigonometricFunctionUtils.cos(Y), Math.cos(X));
    assertComplexEquals(TrigonometricFunctionUtils.sin(Y), Math.sin(X));
    assertComplexEquals(TrigonometricFunctionUtils.tan(Y), Math.tan(X));
    assertComplexEquals(TrigonometricFunctionUtils.acos(Y), Math.acos(X));
    assertComplexEquals(TrigonometricFunctionUtils.asin(Y), Math.asin(X));
    assertComplexEquals(TrigonometricFunctionUtils.atan(Y), Math.atan(X));
    assertComplexEquals(TrigonometricFunctionUtils.acos(TrigonometricFunctionUtils.cos(Z)), Z);
    assertComplexEquals(TrigonometricFunctionUtils.asin(TrigonometricFunctionUtils.sin(Z)), Z);
    assertComplexEquals(TrigonometricFunctionUtils.atan(TrigonometricFunctionUtils.tan(Z)), Z);
    assertThat(TrigonometricFunctionUtils.acosh(TrigonometricFunctionUtils.cosh(X))).isCloseTo(X, offset(EPS));
    assertThat(TrigonometricFunctionUtils.asinh(TrigonometricFunctionUtils.sinh(X))).isCloseTo(X, offset(EPS));
    assertThat(TrigonometricFunctionUtils.atanh(TrigonometricFunctionUtils.tanh(X))).isCloseTo(X, offset(EPS));
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
    assertThat(real).isCloseTo(res.getReal(), offset(1e-15));
    assertThat(res.getImaginary()).isEqualTo(0d);
  }

  private void assertComplexEquals(final ComplexNumber z1, final ComplexNumber z2) {
    assertThat(z1.getReal()).isCloseTo(z2.getReal(), offset(EPS));
    assertThat(z1.getImaginary()).isCloseTo(z2.getImaginary(), offset(EPS));
  }

  private void assertComplexEquals(final ComplexNumber z, final double x) {
    assertThat(z.getImaginary()).isCloseTo(0, offset(EPS));
    assertThat(z.getReal()).isCloseTo(x, offset(EPS));
  }
}
