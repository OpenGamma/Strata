/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.etd;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link EtdExpiryType}.
 */
@Test
public class EtdStyleTypeTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {EtdExpiryType.MONTHLY, "Monthly"},
        {EtdExpiryType.WEEKLY, "Weekly"},
        {EtdExpiryType.DAILY, "Daily"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(EtdExpiryType convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(EtdExpiryType convention, String name) {
    assertEquals(EtdExpiryType.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> EtdExpiryType.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> EtdExpiryType.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(EtdExpiryType.class);
  }

  public void test_serialization() {
    assertSerialization(EtdExpiryType.MONTHLY);
  }

  public void test_jodaConvert() {
    assertJodaConvert(EtdExpiryType.class, EtdExpiryType.MONTHLY);
  }

}
