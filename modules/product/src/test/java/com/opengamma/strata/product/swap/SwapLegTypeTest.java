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
public class SwapLegTypeTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {SwapLegType.FIXED, "Fixed"},
        {SwapLegType.IBOR, "Ibor"},
        {SwapLegType.OVERNIGHT, "Overnight"},
        {SwapLegType.OTHER, "Other"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(SwapLegType convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(SwapLegType convention, String name) {
    assertEquals(SwapLegType.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> SwapLegType.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> SwapLegType.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_isFixed() {
    assertEquals(SwapLegType.FIXED.isFixed(), true);
    assertEquals(SwapLegType.IBOR.isFixed(), false);
    assertEquals(SwapLegType.OVERNIGHT.isFixed(), false);
    assertEquals(SwapLegType.INFLATION.isFixed(), false);
    assertEquals(SwapLegType.OTHER.isFixed(), false);
  }

  public void test_isFloat() {
    assertEquals(SwapLegType.FIXED.isFloat(), false);
    assertEquals(SwapLegType.IBOR.isFloat(), true);
    assertEquals(SwapLegType.OVERNIGHT.isFloat(), true);
    assertEquals(SwapLegType.INFLATION.isFloat(), true);
    assertEquals(SwapLegType.OTHER.isFloat(), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(SwapLegType.class);
  }

  public void test_serialization() {
    assertSerialization(SwapLegType.FIXED);
  }

  public void test_jodaConvert() {
    assertJodaConvert(SwapLegType.class, SwapLegType.IBOR);
  }

}
