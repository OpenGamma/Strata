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
 * Test {@link PaymentOnDefault}.
 */
public class PaymentOnDefaultTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {PaymentOnDefault.NONE, "None"},
        {PaymentOnDefault.ACCRUED_PREMIUM, "AccruedPremium"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(PaymentOnDefault convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(PaymentOnDefault convention, String name) {
    assertThat(PaymentOnDefault.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PaymentOnDefault.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PaymentOnDefault.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(PaymentOnDefault.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(PaymentOnDefault.ACCRUED_PREMIUM);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(PaymentOnDefault.class, PaymentOnDefault.ACCRUED_PREMIUM);
  }

}
