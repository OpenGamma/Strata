/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fra;

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
 * Test.
 */
public class FraDiscountingMethodTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {FraDiscountingMethod.NONE, "None"},
        {FraDiscountingMethod.ISDA, "ISDA"},
        {FraDiscountingMethod.AFMA, "AFMA"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(FraDiscountingMethod convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(FraDiscountingMethod convention, String name) {
    assertThat(FraDiscountingMethod.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupUpperCase(FraDiscountingMethod convention, String name) {
    assertThat(FraDiscountingMethod.of(name.toUpperCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupLowerCase(FraDiscountingMethod convention, String name) {
    assertThat(FraDiscountingMethod.of(name.toLowerCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FraDiscountingMethod.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FraDiscountingMethod.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(FraDiscountingMethod.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(FraDiscountingMethod.ISDA);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(FraDiscountingMethod.class, FraDiscountingMethod.ISDA);
  }

}
