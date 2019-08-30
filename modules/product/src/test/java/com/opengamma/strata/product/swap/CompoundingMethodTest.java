/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test.
 */
public class CompoundingMethodTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {CompoundingMethod.NONE, "None"},
        {CompoundingMethod.STRAIGHT, "Straight"},
        {CompoundingMethod.FLAT, "Flat"},
        {CompoundingMethod.SPREAD_EXCLUSIVE, "SpreadExclusive"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(CompoundingMethod convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(CompoundingMethod convention, String name) {
    assertThat(CompoundingMethod.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> CompoundingMethod.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> CompoundingMethod.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(CompoundingMethod.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(CompoundingMethod.STRAIGHT);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(CompoundingMethod.class, CompoundingMethod.STRAIGHT);
  }

}
