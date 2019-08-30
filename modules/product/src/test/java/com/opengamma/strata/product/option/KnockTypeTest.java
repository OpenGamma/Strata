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
 * Test {@link KnockType}.
 */
public class KnockTypeTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_isKnockIn() {
    assertThat(KnockType.KNOCK_IN.isKnockIn()).isTrue();
    assertThat(KnockType.KNOCK_OUT.isKnockIn()).isFalse();
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {KnockType.KNOCK_IN, "KnockIn"},
        {KnockType.KNOCK_OUT, "KnockOut"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(KnockType type, String name) {
    assertThat(type.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(KnockType type, String name) {
    assertThat(KnockType.of(name)).isEqualTo(type);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> KnockType.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> KnockType.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(KnockType.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(KnockType.KNOCK_OUT);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(KnockType.class, KnockType.KNOCK_IN);
  }

}
