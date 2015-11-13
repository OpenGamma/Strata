/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate.bond;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link YieldConvention}.
 */
@Test
public class YieldConventionTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {YieldConvention.UK_BUMP_DMO, "UK_BUMP_DMO"},
        {YieldConvention.US_STREET, "US_STREET"},
        {YieldConvention.GERMAN_BONDS, "GERMAN_BONDS"},
        {YieldConvention.JAPAN_SIMPLE, "JAPAN_SIMPLE"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(YieldConvention convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(YieldConvention convention, String name) {
    assertEquals(YieldConvention.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> YieldConvention.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> YieldConvention.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(YieldConvention.class);
  }

  public void test_serialization() {
    assertSerialization(YieldConvention.UK_BUMP_DMO);
  }

  public void test_jodaConvert() {
    assertJodaConvert(YieldConvention.class, YieldConvention.UK_BUMP_DMO);
  }

}
