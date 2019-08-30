/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link CmsPeriodType}.
 */
public class CmsPeriodTypeTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {CmsPeriodType.COUPON, "Coupon"},
        {CmsPeriodType.CAPLET, "Caplet"},
        {CmsPeriodType.FLOORLET, "Floorlet"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(CmsPeriodType convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(CmsPeriodType convention, String name) {
    assertThat(CmsPeriodType.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CmsPeriodType.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CmsPeriodType.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(CmsPeriodType.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(CmsPeriodType.CAPLET);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(CmsPeriodType.class, CmsPeriodType.COUPON);
  }

}
