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
public class SwapLegTypeTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {SwapLegType.FIXED, "Fixed"},
        {SwapLegType.IBOR, "Ibor"},
        {SwapLegType.OVERNIGHT, "Overnight"},
        {SwapLegType.OTHER, "Other"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(SwapLegType convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(SwapLegType convention, String name) {
    assertThat(SwapLegType.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SwapLegType.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SwapLegType.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_isFixed() {
    assertThat(SwapLegType.FIXED.isFixed()).isTrue();
    assertThat(SwapLegType.IBOR.isFixed()).isFalse();
    assertThat(SwapLegType.OVERNIGHT.isFixed()).isFalse();
    assertThat(SwapLegType.INFLATION.isFixed()).isFalse();
    assertThat(SwapLegType.OTHER.isFixed()).isFalse();
  }

  @Test
  public void test_isFloat() {
    assertThat(SwapLegType.FIXED.isFloat()).isFalse();
    assertThat(SwapLegType.IBOR.isFloat()).isTrue();
    assertThat(SwapLegType.OVERNIGHT.isFloat()).isTrue();
    assertThat(SwapLegType.INFLATION.isFloat()).isTrue();
    assertThat(SwapLegType.OTHER.isFloat()).isFalse();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(SwapLegType.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(SwapLegType.FIXED);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(SwapLegType.class, SwapLegType.IBOR);
  }

}
