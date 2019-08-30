/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.option;

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
public class FutureOptionPremiumStyleTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {FutureOptionPremiumStyle.DAILY_MARGIN, "DailyMargin"},
        {FutureOptionPremiumStyle.UPFRONT_PREMIUM, "UpfrontPremium"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(FutureOptionPremiumStyle convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(FutureOptionPremiumStyle convention, String name) {
    assertThat(FutureOptionPremiumStyle.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FutureOptionPremiumStyle.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FutureOptionPremiumStyle.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(FutureOptionPremiumStyle.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(FutureOptionPremiumStyle.DAILY_MARGIN);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(FutureOptionPremiumStyle.class, FutureOptionPremiumStyle.DAILY_MARGIN);
  }

}
