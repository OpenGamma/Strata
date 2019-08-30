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
 * Test {@link EtdExpiryType}.
 */
public class EtdStyleTypeTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {EtdExpiryType.MONTHLY, "Monthly"},
        {EtdExpiryType.WEEKLY, "Weekly"},
        {EtdExpiryType.DAILY, "Daily"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(EtdExpiryType convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(EtdExpiryType convention, String name) {
    assertThat(EtdExpiryType.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdExpiryType.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> EtdExpiryType.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(EtdExpiryType.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(EtdExpiryType.MONTHLY);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(EtdExpiryType.class, EtdExpiryType.MONTHLY);
  }

}
