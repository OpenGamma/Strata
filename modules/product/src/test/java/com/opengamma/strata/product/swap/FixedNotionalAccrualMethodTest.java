/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link FixedNotionalAccrualMethod}.
 */
@Test
public class FixedNotionalAccrualMethodTest {

  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {FixedNotionalAccrualMethod.NONE, "None"},
        {FixedNotionalAccrualMethod.OVERNIGHT_COMPOUNDED_ANNUAL_RATE, "OvernightCompoundedAnnualRate"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(FixedNotionalAccrualMethod convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(FixedNotionalAccrualMethod convention, String name) {
    assertEquals(FixedNotionalAccrualMethod.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> FixedNotionalAccrualMethod.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> FixedNotionalAccrualMethod.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(FixedNotionalAccrualMethod.class);
  }

  public void test_serialization() {
    assertSerialization(FixedNotionalAccrualMethod.NONE);
  }

  public void test_jodaConvert() {
    assertJodaConvert(FixedNotionalAccrualMethod.class, FixedNotionalAccrualMethod.NONE);
  }

}
