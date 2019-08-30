/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Test {@link PriceIndexCalculationMethod}.
 */
public class PriceIndexCalculationMethodTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {PriceIndexCalculationMethod.MONTHLY, "Monthly"},
        {PriceIndexCalculationMethod.INTERPOLATED, "Interpolated"},
        {PriceIndexCalculationMethod.INTERPOLATED_JAPAN, "InterpolatedJapan"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(PriceIndexCalculationMethod convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(PriceIndexCalculationMethod convention, String name) {
    assertThat(PriceIndexCalculationMethod.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PriceIndexCalculationMethod.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> PriceIndexCalculationMethod.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(PriceIndexCalculationMethod.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(PriceIndexCalculationMethod.INTERPOLATED);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(PriceIndexCalculationMethod.class, PriceIndexCalculationMethod.INTERPOLATED);
  }

}
