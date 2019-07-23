/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

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
 * Test {@link FloatingRateType}.
 */
public class FloatingRateTypeTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_isIbor() {
    assertThat(FloatingRateType.IBOR.isIbor()).isEqualTo(true);
    assertThat(FloatingRateType.OVERNIGHT_AVERAGED.isIbor()).isEqualTo(false);
    assertThat(FloatingRateType.OVERNIGHT_COMPOUNDED.isIbor()).isEqualTo(false);
    assertThat(FloatingRateType.PRICE.isIbor()).isEqualTo(false);
    assertThat(FloatingRateType.OTHER.isIbor()).isEqualTo(false);
  }

  @Test
  public void test_isOvernight() {
    assertThat(FloatingRateType.IBOR.isOvernight()).isEqualTo(false);
    assertThat(FloatingRateType.OVERNIGHT_AVERAGED.isOvernight()).isEqualTo(true);
    assertThat(FloatingRateType.OVERNIGHT_COMPOUNDED.isOvernight()).isEqualTo(true);
    assertThat(FloatingRateType.PRICE.isOvernight()).isEqualTo(false);
    assertThat(FloatingRateType.OTHER.isOvernight()).isEqualTo(false);
  }

  @Test
  public void test_isPrice() {
    assertThat(FloatingRateType.IBOR.isPrice()).isEqualTo(false);
    assertThat(FloatingRateType.OVERNIGHT_AVERAGED.isPrice()).isEqualTo(false);
    assertThat(FloatingRateType.OVERNIGHT_COMPOUNDED.isPrice()).isEqualTo(false);
    assertThat(FloatingRateType.PRICE.isPrice()).isEqualTo(true);
    assertThat(FloatingRateType.OTHER.isPrice()).isEqualTo(false);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {FloatingRateType.IBOR, "Ibor"},
        {FloatingRateType.OVERNIGHT_AVERAGED, "OvernightAveraged"},
        {FloatingRateType.OVERNIGHT_COMPOUNDED, "OvernightCompounded"},
        {FloatingRateType.PRICE, "Price"},
        {FloatingRateType.OTHER, "Other"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(FloatingRateType convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(FloatingRateType convention, String name) {
    assertThat(FloatingRateType.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupUpperCase(FloatingRateType convention, String name) {
    assertThat(FloatingRateType.of(name.toUpperCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupLowerCase(FloatingRateType convention, String name) {
    assertThat(FloatingRateType.of(name.toLowerCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> FloatingRateType.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> FloatingRateType.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(FloatingRateType.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(FloatingRateType.IBOR);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(FloatingRateType.class, FloatingRateType.IBOR);
  }

}
