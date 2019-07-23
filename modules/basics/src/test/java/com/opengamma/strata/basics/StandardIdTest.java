/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link StandardId}.
 */
public class StandardIdTest {

  private static final String SCHEME = "Scheme";
  private static final String OTHER_SCHEME = "Other";

  //-------------------------------------------------------------------------
  @Test
  public void test_factory_String_String() {
    StandardId test = StandardId.of("scheme:/+foo", "value");
    assertThat(test.getScheme()).isEqualTo("scheme:/+foo");
    assertThat(test.getValue()).isEqualTo("value");
    assertThat(test.toString()).isEqualTo("scheme:/+foo~value");
  }

  @Test
  public void test_factory_String_String_nullScheme() {
    assertThatIllegalArgumentException().isThrownBy(() -> StandardId.of(null, "value"));
  }

  @Test
  public void test_factory_String_String_nullValue() {
    assertThatIllegalArgumentException().isThrownBy(() -> StandardId.of("Scheme", null));
  }

  @Test
  public void test_factory_String_String_emptyValue() {
    assertThatIllegalArgumentException().isThrownBy(() -> StandardId.of("Scheme", ""));
  }

  public static Object[][] data_factoryValid() {
    return new Object[][] {
        {"ABCDEFGHIJKLMNOPQRSTUVWXYZ", "123"},
        {"abcdefghijklmnopqrstuvwxyz", "123"},
        {"0123456789:/+.=_-", "123"},
        {"ABC", "! !\"$%%^&*()123abcxyzABCXYZ"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_factoryValid")
  public void test_factory_String_String_valid(String scheme, String value) {
    StandardId.of(scheme, value);
  }

  public static Object[][] data_factoryInvalid() {
    return new Object[][] {
        {"", ""},
        {" ", "123"},
        {"{", "123"},
        {"ABC", " 123"},
        {"ABC", "12}3"},
        {"ABC", "12\u00003"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_factoryInvalid")
  public void test_factory_String_String_invalid(String scheme, String value) {
    assertThatIllegalArgumentException().isThrownBy(() -> StandardId.of(scheme, value));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_encodeScheme() {
    String test = StandardId.encodeScheme("https://opengamma.com/foo/../~bar#test");
    assertThat(test).isEqualTo("https://opengamma.com/foo/../%7Ebar%23test");
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_formats() {
    return new Object[][] {
        {"Value", "A~Value"},
        {"a+b", "A~a+b"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_formats")
  public void test_formats_toString(String value, String expected) {
    StandardId test = StandardId.of("A", value);
    assertThat(test.toString()).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("data_formats")
  public void test_formats_parse(String value, String text) {
    StandardId test = StandardId.parse(text);
    assertThat(test.getScheme()).isEqualTo("A");
    assertThat(test.getValue()).isEqualTo(value);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parse() {
    StandardId test = StandardId.parse("Scheme~value");
    assertThat(test.getScheme()).isEqualTo(SCHEME);
    assertThat(test.getValue()).isEqualTo("value");
    assertThat(test.toString()).isEqualTo("Scheme~value");
  }

  public static Object[][] data_parseInvalidFormat() {
    return new Object[][] {
        {"Scheme"},
        {"Scheme~"},
        {"~value"},
        {"Scheme:value"},
        {"a~b~c"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_parseInvalidFormat")
  public void test_parse_invalidFormat(String text) {
    assertThatIllegalArgumentException().isThrownBy(() -> StandardId.parse(text));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equals() {
    StandardId d1a = StandardId.of(SCHEME, "d1");
    StandardId d1b = StandardId.of(SCHEME, "d1");
    StandardId d2 = StandardId.of(SCHEME, "d2");
    StandardId d3 = StandardId.of("Different", "d1");
    assertThat(d1a)
        .isEqualTo(d1a)
        .isEqualTo(d1b)
        .isNotEqualTo(d2)
        .isNotEqualTo(d3)
        .isNotEqualTo("")
        .isNotEqualTo(null)
        .hasSameHashCodeAs(d1b);
  }

  @Test
  public void test_comparisonByScheme() {
    StandardId id1 = StandardId.of(SCHEME, "123");
    StandardId id2 = StandardId.of(OTHER_SCHEME, "234");
    // as schemes are different, will compare by scheme
    assertThat(id1).isGreaterThan(id2);
  }

  @Test
  public void test_comparisonWithSchemeSame() {
    StandardId id1 = StandardId.of(SCHEME, "123");
    StandardId id2 = StandardId.of(SCHEME, "234");
    // as schemes are same, will compare by id
    assertThat(id1).isLessThan(id2);
  }

  @Test
  public void coverage() {
    coverImmutableBean(StandardId.of(SCHEME, "123"));
  }

  @Test
  public void test_serialization() {
    assertSerialization(StandardId.of(SCHEME, "123"));
  }

}
