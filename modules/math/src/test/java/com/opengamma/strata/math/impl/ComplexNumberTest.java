/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class ComplexNumberTest {

  private static final ComplexNumber Z1 = new ComplexNumber(1, 2);
  private static final ComplexNumber Z2 = new ComplexNumber(1, 2);
  private static final ComplexNumber Z3 = new ComplexNumber(1, 3);
  private static final ComplexNumber Z4 = new ComplexNumber(2, 2);
  private static final ComplexNumber Z5 = new ComplexNumber(2, 3);

  @Test
  public void testByteValue() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> Z1.byteValue());
  }

  @Test
  public void testIntValue() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> Z1.intValue());
  }

  @Test
  public void testLongValue() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> Z1.longValue());
  }

  @Test
  public void testFloatValue() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> Z1.floatValue());
  }

  @Test
  public void testDoubleValue() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> Z1.doubleValue());
  }

  @Test
  public void test() {
    assertThat(Double.valueOf(1)).isEqualTo(Double.valueOf(Z1.getReal()));
    assertThat(Double.valueOf(2)).isEqualTo(Double.valueOf(Z1.getImaginary()));
    assertThat(Z1).isEqualTo(Z2);
    assertThat(Z1.hashCode()).isEqualTo(Z2.hashCode());
    assertThat("1.0 + 2.0i").isEqualTo(Z1.toString());
    assertThat("1.0 + 0.0i").isEqualTo(new ComplexNumber(1, 0).toString());
    assertThat("0.0 + 2.3i").isEqualTo(new ComplexNumber(0, 2.3).toString());
    assertThat("-1.0 + 0.0i").isEqualTo(new ComplexNumber(-1, 0).toString());
    assertThat("0.0 - 2.3i").isEqualTo(new ComplexNumber(0, -2.3).toString());
    assertThat(Z1.equals(Z3)).isFalse();
    assertThat(Z1.equals(Z4)).isFalse();
    assertThat(Z1.equals(Z5)).isFalse();
  }
}
