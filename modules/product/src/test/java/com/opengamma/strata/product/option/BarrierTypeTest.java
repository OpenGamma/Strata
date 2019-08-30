/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.option;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link BarrierType}.
 */
public class BarrierTypeTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_isDown() {
    assertThat(BarrierType.UP.isDown()).isFalse();
    assertThat(BarrierType.DOWN.isDown()).isTrue();
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {BarrierType.UP, "Up"},
        {BarrierType.DOWN, "Down"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(BarrierType type, String name) {
    assertThat(type.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(BarrierType type, String name) {
    assertThat(BarrierType.of(name)).isEqualTo(type);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BarrierType.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BarrierType.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(BarrierType.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(BarrierType.DOWN);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(BarrierType.class, BarrierType.UP);
  }

}
