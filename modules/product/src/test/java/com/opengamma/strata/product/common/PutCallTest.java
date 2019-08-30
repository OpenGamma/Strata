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
 * Test {@link PutCall}.
 */
public class PutCallTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_ofPut() {
    assertThat(PutCall.ofPut(true)).isEqualTo(PutCall.PUT);
    assertThat(PutCall.ofPut(false)).isEqualTo(PutCall.CALL);
  }

  @Test
  public void test_isPut() {
    assertThat(PutCall.PUT.isPut()).isTrue();
    assertThat(PutCall.CALL.isPut()).isFalse();
  }

  @Test
  public void test_isCall() {
    assertThat(PutCall.PUT.isCall()).isFalse();
    assertThat(PutCall.CALL.isCall()).isTrue();
  }

  @Test
  public void test_opposite() {
    assertThat(PutCall.PUT.opposite()).isEqualTo(PutCall.CALL);
    assertThat(PutCall.CALL.opposite()).isEqualTo(PutCall.PUT);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {PutCall.PUT, "Put"},
        {PutCall.CALL, "Call"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(PutCall convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(PutCall convention, String name) {
    assertThat(PutCall.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupUpperCase(PutCall convention, String name) {
    assertThat(PutCall.of(name.toUpperCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupLowerCase(PutCall convention, String name) {
    assertThat(PutCall.of(name.toLowerCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PutCall.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PutCall.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(PutCall.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(PutCall.PUT);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(PutCall.class, PutCall.PUT);
  }

}
