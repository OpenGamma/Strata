/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.cms;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link CmsPeriodType}.
 */
@Test
public class CmsPeriodTypeTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {CmsPeriodType.COUPON, "Coupon"},
        {CmsPeriodType.CAPLET, "Caplet"},
        {CmsPeriodType.FLOORLET, "Floorlet"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(CmsPeriodType convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(CmsPeriodType convention, String name) {
    assertEquals(CmsPeriodType.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> CmsPeriodType.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> CmsPeriodType.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(CmsPeriodType.class);
  }

  public void test_serialization() {
    assertSerialization(CmsPeriodType.CAPLET);
  }

  public void test_jodaConvert() {
    assertJodaConvert(CmsPeriodType.class, CmsPeriodType.COUPON);
  }

}
