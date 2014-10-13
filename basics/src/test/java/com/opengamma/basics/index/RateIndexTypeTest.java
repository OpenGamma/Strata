/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.index;

import static com.opengamma.collect.TestHelper.assertJodaConvert;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrows;
import static com.opengamma.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests for the rate index type class.
 */
@Test
public class RateIndexTypeTest {

  @DataProvider(name = "name")
  static Object[][] data_name() {
      return new Object[][] {
          {RateIndexType.OVERNIGHT, "Overnight"},
          {RateIndexType.TENOR, "Tenor"},
      };
  }

  @Test(dataProvider = "name")
  public void test_toString(RateIndexType convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(RateIndexType convention, String name) {
    assertEquals(RateIndexType.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> RateIndexType.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> RateIndexType.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(RateIndexType.class);
  }

  public void test_serialization() {
    assertSerialization(RateIndexType.OVERNIGHT);
  }

  public void test_jodaConvert() {
    assertJodaConvert(RateIndexType.class, RateIndexType.TENOR);
  }

}

