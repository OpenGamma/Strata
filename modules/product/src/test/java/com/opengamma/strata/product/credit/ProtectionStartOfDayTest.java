/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link ProtectionStartOfDay}.
 */
public class ProtectionStartOfDayTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {ProtectionStartOfDay.NONE, "None"},
        {ProtectionStartOfDay.BEGINNING, "Beginning"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(ProtectionStartOfDay convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(ProtectionStartOfDay convention, String name) {
    assertThat(ProtectionStartOfDay.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ProtectionStartOfDay.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ProtectionStartOfDay.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(ProtectionStartOfDay.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(ProtectionStartOfDay.BEGINNING);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(ProtectionStartOfDay.class, ProtectionStartOfDay.BEGINNING);
  }

}
