/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fra;

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
public class FraDiscountingMethodTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {FraDiscountingMethod.NONE, "None"},
        {FraDiscountingMethod.ISDA, "ISDA"},
        {FraDiscountingMethod.AFMA, "AFMA"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(FraDiscountingMethod convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(FraDiscountingMethod convention, String name) {
    assertEquals(FraDiscountingMethod.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> FraDiscountingMethod.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> FraDiscountingMethod.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(FraDiscountingMethod.class);
  }

  public void test_serialization() {
    assertSerialization(FraDiscountingMethod.ISDA);
  }

  public void test_jodaConvert() {
    assertJodaConvert(FraDiscountingMethod.class, FraDiscountingMethod.ISDA);
  }

}
