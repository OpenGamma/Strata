/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate.swap;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class IborRateAveragingMethodTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {IborRateAveragingMethod.WEIGHTED, "Weighted"},
        {IborRateAveragingMethod.UNWEIGHTED, "Unweighted"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(IborRateAveragingMethod convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(IborRateAveragingMethod convention, String name) {
    assertEquals(IborRateAveragingMethod.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> IborRateAveragingMethod.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> IborRateAveragingMethod.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(IborRateAveragingMethod.class);
  }

  public void test_serialization() {
    assertSerialization(IborRateAveragingMethod.WEIGHTED);
  }

  public void test_jodaConvert() {
    assertJodaConvert(IborRateAveragingMethod.class, IborRateAveragingMethod.WEIGHTED);
  }

}
