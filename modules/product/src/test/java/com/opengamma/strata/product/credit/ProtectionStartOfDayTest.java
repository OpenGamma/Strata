/**
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link ProtectionStartOfDay}.
 */
@Test
public class ProtectionStartOfDayTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {ProtectionStartOfDay.NONE, "None"},
        {ProtectionStartOfDay.BEGINNING, "Beginning"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(ProtectionStartOfDay convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(ProtectionStartOfDay convention, String name) {
    assertEquals(ProtectionStartOfDay.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> ProtectionStartOfDay.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> ProtectionStartOfDay.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(ProtectionStartOfDay.class);
  }

  public void test_serialization() {
    assertSerialization(ProtectionStartOfDay.BEGINNING);
  }

  public void test_jodaConvert() {
    assertJodaConvert(ProtectionStartOfDay.class, ProtectionStartOfDay.BEGINNING);
  }

}
