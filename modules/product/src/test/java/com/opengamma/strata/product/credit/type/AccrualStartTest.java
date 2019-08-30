/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link AccrualStart}.
 */
public class AccrualStartTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {AccrualStart.NEXT_DAY, "NextDay"},
        {AccrualStart.IMM_DATE, "ImmDate"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(AccrualStart convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(AccrualStart convention, String name) {
    assertThat(AccrualStart.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> AccrualStart.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> AccrualStart.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(AccrualStart.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(AccrualStart.IMM_DATE);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(AccrualStart.class, AccrualStart.IMM_DATE);
  }

}
