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
 * Test {@link EtdOptionType}.
 */
@Test
public class EtdOptionTypeTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {EtdOptionType.AMERICAN, "American"},
        {EtdOptionType.EUROPEAN, "European"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(EtdOptionType convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(EtdOptionType convention, String name) {
    assertEquals(EtdOptionType.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> EtdOptionType.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> EtdOptionType.of(null), IllegalArgumentException.class);
  }

  public void test_getCode() {
    assertEquals(EtdOptionType.AMERICAN.getCode(), "A");
    assertEquals(EtdOptionType.EUROPEAN.getCode(), "E");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(EtdOptionType.class);
  }

  public void test_serialization() {
    assertSerialization(EtdOptionType.EUROPEAN);
  }

  public void test_jodaConvert() {
    assertJodaConvert(EtdOptionType.class, EtdOptionType.EUROPEAN);
  }

}
