/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link CompoundedRateType}.
 */
public class CompoundedRateTypeTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {CompoundedRateType.PERIODIC, "Periodic"},
        {CompoundedRateType.CONTINUOUS, "Continuous"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(CompoundedRateType convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(CompoundedRateType convention, String name) {
    assertThat(CompoundedRateType.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CompoundedRateType.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CompoundedRateType.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(CompoundedRateType.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(CompoundedRateType.CONTINUOUS);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(CompoundedRateType.class, CompoundedRateType.CONTINUOUS);
  }

}
