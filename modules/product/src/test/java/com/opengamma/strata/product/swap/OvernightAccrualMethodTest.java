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
public class OvernightAccrualMethodTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {OvernightAccrualMethod.AVERAGED, "Averaged"},
        {OvernightAccrualMethod.COMPOUNDED, "Compounded"},
        {OvernightAccrualMethod.OVERNIGHT_COMPOUNDED_ANNUAL_RATE, "OvernightCompoundedAnnualRate"},
        {OvernightAccrualMethod.AVERAGED_DAILY, "AveragedDaily"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(OvernightAccrualMethod convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(OvernightAccrualMethod convention, String name) {
    assertThat(OvernightAccrualMethod.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightAccrualMethod.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightAccrualMethod.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(OvernightAccrualMethod.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(OvernightAccrualMethod.AVERAGED);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(OvernightAccrualMethod.class, OvernightAccrualMethod.AVERAGED);
  }

}
