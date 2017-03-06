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
 * Test {@link EtdStyleType}.
 */
@Test
public class EtdStyleTypeTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {EtdStyleType.MONTHLY, "Monthly"},
        {EtdStyleType.WEEKLY, "Weekly"},
        {EtdStyleType.DAILY, "Daily"},
        {EtdStyleType.FLEX, "Flex"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(EtdStyleType convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(EtdStyleType convention, String name) {
    assertEquals(EtdStyleType.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> EtdStyleType.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> EtdStyleType.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(EtdStyleType.class);
  }

  public void test_serialization() {
    assertSerialization(EtdStyleType.MONTHLY);
  }

  public void test_jodaConvert() {
    assertJodaConvert(EtdStyleType.class, EtdStyleType.MONTHLY);
  }

}
