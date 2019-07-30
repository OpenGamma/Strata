/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link FixedAccrualMethod}.
 */
@Test
public class FixedAccrualMethodTest {

  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {FixedAccrualMethod.DEFAULT, "Default"},
        {FixedAccrualMethod.OVERNIGHT_COMPOUNDED_ANNUAL_RATE, "OvernightCompoundedAnnualRate"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(FixedAccrualMethod convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(FixedAccrualMethod convention, String name) {
    assertEquals(FixedAccrualMethod.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixedAccrualMethod.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixedAccrualMethod.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(FixedAccrualMethod.class);
  }

  public void test_serialization() {
    assertSerialization(FixedAccrualMethod.DEFAULT);
  }

  public void test_jodaConvert() {
    assertJodaConvert(FixedAccrualMethod.class, FixedAccrualMethod.DEFAULT);
  }

}
