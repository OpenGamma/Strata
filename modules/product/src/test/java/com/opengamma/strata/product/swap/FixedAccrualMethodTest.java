/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Test {@link FixedAccrualMethod}.
 */
public class FixedAccrualMethodTest {

  public static Object[][] data_name() {
    return new Object[][] {
        {FixedAccrualMethod.DEFAULT, "Default"},
        {FixedAccrualMethod.OVERNIGHT_COMPOUNDED_ANNUAL_RATE, "OvernightCompoundedAnnualRate"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(FixedAccrualMethod convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(FixedAccrualMethod convention, String name) {
    assertThat(FixedAccrualMethod.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixedAccrualMethod.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixedAccrualMethod.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(FixedAccrualMethod.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(FixedAccrualMethod.DEFAULT);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(FixedAccrualMethod.class, FixedAccrualMethod.DEFAULT);
  }

}
