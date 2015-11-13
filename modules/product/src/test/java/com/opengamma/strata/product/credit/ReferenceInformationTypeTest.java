/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.product.credit.ReferenceInformationType.INDEX;
import static com.opengamma.strata.product.credit.ReferenceInformationType.SINGLE_NAME;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class ReferenceInformationTypeTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {SINGLE_NAME, "SINGLE_NAME"},
        {INDEX, "INDEX"}
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(ReferenceInformationType type, String name) {
    assertEquals(type.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(ReferenceInformationType type, String name) {
    assertEquals(ReferenceInformationType.valueOf(name), type);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> ReferenceInformationType.valueOf("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> ReferenceInformationType.valueOf(null), NullPointerException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(ReferenceInformationType.class);
  }

  public void test_serialization() {
    assertSerialization(SINGLE_NAME);
  }

  public void test_jodaConvert() {
    assertJodaConvert(ReferenceInformationType.class, INDEX);
  }

}
