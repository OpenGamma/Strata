/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link LongShort}.
 */
public class LongShortTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_ofLong() {
    assertThat(LongShort.ofLong(true)).isEqualTo(LongShort.LONG);
    assertThat(LongShort.ofLong(false)).isEqualTo(LongShort.SHORT);
  }

  @Test
  public void test_isLong() {
    assertThat(LongShort.LONG.isLong()).isTrue();
    assertThat(LongShort.SHORT.isLong()).isFalse();
  }

  @Test
  public void test_isShort() {
    assertThat(LongShort.LONG.isShort()).isFalse();
    assertThat(LongShort.SHORT.isShort()).isTrue();
  }

  @Test
  public void test_sign() {
    assertThat(LongShort.LONG.sign()).isEqualTo(1);
    assertThat(LongShort.SHORT.sign()).isEqualTo(-1);
  }

  @Test
  public void test_opposite() {
    assertThat(LongShort.LONG.opposite()).isEqualTo(LongShort.SHORT);
    assertThat(LongShort.SHORT.opposite()).isEqualTo(LongShort.LONG);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {LongShort.LONG, "Long"},
        {LongShort.SHORT, "Short"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(LongShort convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(LongShort convention, String name) {
    assertThat(LongShort.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupUpperCase(LongShort convention, String name) {
    assertThat(LongShort.of(name.toUpperCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupLowerCase(LongShort convention, String name) {
    assertThat(LongShort.of(name.toLowerCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LongShort.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LongShort.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(LongShort.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(LongShort.LONG);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(LongShort.class, LongShort.LONG);
  }

}
