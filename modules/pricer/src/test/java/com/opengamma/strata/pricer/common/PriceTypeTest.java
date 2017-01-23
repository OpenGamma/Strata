/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.common;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link PriceType}.
 */
@Test
public class PriceTypeTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {PriceType.CLEAN, "Clean"},
        {PriceType.DIRTY, "Dirty"}
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(PriceType convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(PriceType convention, String name) {
    assertEquals(PriceType.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> PriceType.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> PriceType.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(PriceType.class);
  }

  public void test_serialization() {
    assertSerialization(PriceType.CLEAN);
  }

  public void test_jodaConvert() {
    assertJodaConvert(PriceType.class, PriceType.DIRTY);
  }

}
