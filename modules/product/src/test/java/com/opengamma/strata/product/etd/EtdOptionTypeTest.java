/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link EtdOptionType}.
 */
public class EtdOptionTypeTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {EtdOptionType.AMERICAN, "American"},
        {EtdOptionType.EUROPEAN, "European"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(EtdOptionType convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(EtdOptionType convention, String name) {
    assertThat(EtdOptionType.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdOptionType.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdOptionType.of(null));
  }

  @Test
  public void test_getCode() {
    assertThat(EtdOptionType.AMERICAN.getCode()).isEqualTo("A");
    assertThat(EtdOptionType.EUROPEAN.getCode()).isEqualTo("E");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(EtdOptionType.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(EtdOptionType.EUROPEAN);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(EtdOptionType.class, EtdOptionType.EUROPEAN);
  }

}
