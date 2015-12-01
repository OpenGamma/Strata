/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Test.
 */
@Test
public class OvernightAccrualMethodTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {OvernightAccrualMethod.AVERAGED, "Averaged"},
        {OvernightAccrualMethod.COMPOUNDED, "Compounded"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(OvernightAccrualMethod convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(OvernightAccrualMethod convention, String name) {
    assertEquals(OvernightAccrualMethod.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> OvernightAccrualMethod.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> OvernightAccrualMethod.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(OvernightAccrualMethod.class);
  }

  public void test_serialization() {
    assertSerialization(OvernightAccrualMethod.AVERAGED);
  }

  public void test_jodaConvert() {
    assertJodaConvert(OvernightAccrualMethod.class, OvernightAccrualMethod.AVERAGED);
  }

}
