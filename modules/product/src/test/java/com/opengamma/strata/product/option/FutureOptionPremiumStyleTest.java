/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.option;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class FutureOptionPremiumStyleTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {FutureOptionPremiumStyle.DAILY_MARGIN, "DailyMargin"},
        {FutureOptionPremiumStyle.UPFRONT_PREMIUM, "UpfrontPremium"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(FutureOptionPremiumStyle convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(FutureOptionPremiumStyle convention, String name) {
    assertEquals(FutureOptionPremiumStyle.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FutureOptionPremiumStyle.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FutureOptionPremiumStyle.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(FutureOptionPremiumStyle.class);
  }

  public void test_serialization() {
    assertSerialization(FutureOptionPremiumStyle.DAILY_MARGIN);
  }

  public void test_jodaConvert() {
    assertJodaConvert(FutureOptionPremiumStyle.class, FutureOptionPremiumStyle.DAILY_MARGIN);
  }

}
