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
public class CompoundingMethodTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {CompoundingMethod.NONE, "None"},
        {CompoundingMethod.STRAIGHT, "Straight"},
        {CompoundingMethod.FLAT, "Flat"},
        {CompoundingMethod.SPREAD_EXCLUSIVE, "SpreadExclusive"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(CompoundingMethod convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(CompoundingMethod convention, String name) {
    assertEquals(CompoundingMethod.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> CompoundingMethod.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> CompoundingMethod.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(CompoundingMethod.class);
  }

  public void test_serialization() {
    assertSerialization(CompoundingMethod.STRAIGHT);
  }

  public void test_jodaConvert() {
    assertJodaConvert(CompoundingMethod.class, CompoundingMethod.STRAIGHT);
  }

}
