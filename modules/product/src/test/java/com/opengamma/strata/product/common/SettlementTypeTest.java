/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Test {@link SettlementType}.
 */
public class SettlementTypeTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {SettlementType.CASH, "Cash"},
        {SettlementType.PHYSICAL, "Physical"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(SettlementType convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(SettlementType convention, String name) {
    assertThat(SettlementType.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupUpperCase(SettlementType convention, String name) {
    assertThat(SettlementType.of(name.toUpperCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupLowerCase(SettlementType convention, String name) {
    assertThat(SettlementType.of(name.toLowerCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SettlementType.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> SettlementType.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(SettlementType.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(SettlementType.CASH);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(SettlementType.class, SettlementType.CASH);
  }

}
