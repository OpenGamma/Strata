/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.common;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link SettlementType}.
 */
@Test
public class SettlementTypeTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {SettlementType.CASH, "Cash"},
        {SettlementType.PHYSICAL, "Physical"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(SettlementType convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(SettlementType convention, String name) {
    assertEquals(SettlementType.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> SettlementType.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> SettlementType.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(SettlementType.class);
  }

  public void test_serialization() {
    assertSerialization(SettlementType.CASH);
  }

  public void test_jodaConvert() {
    assertJodaConvert(SettlementType.class, SettlementType.CASH);
  }

}
