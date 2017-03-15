/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

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
public class NegativeRateMethodTest {

  //-------------------------------------------------------------------------
  public void adjust_allowNegative() {
    assertEquals(NegativeRateMethod.ALLOW_NEGATIVE.adjust(1d), 1d, 0d);
    assertEquals(NegativeRateMethod.ALLOW_NEGATIVE.adjust(0d), 0d, 0d);
    assertEquals(NegativeRateMethod.ALLOW_NEGATIVE.adjust(-0d), -0d, 0d);
    assertEquals(NegativeRateMethod.ALLOW_NEGATIVE.adjust(-1d), -1d, 0d);
    assertEquals(NegativeRateMethod.ALLOW_NEGATIVE.adjust(Double.MAX_VALUE), Double.MAX_VALUE, 0d);
    assertEquals(NegativeRateMethod.ALLOW_NEGATIVE.adjust(Double.MIN_VALUE), Double.MIN_VALUE, 0d);
    assertEquals(NegativeRateMethod.ALLOW_NEGATIVE.adjust(Double.POSITIVE_INFINITY), Double.POSITIVE_INFINITY, 0d);
    assertEquals(NegativeRateMethod.ALLOW_NEGATIVE.adjust(Double.NEGATIVE_INFINITY), Double.NEGATIVE_INFINITY, 0d);
    assertEquals(NegativeRateMethod.ALLOW_NEGATIVE.adjust(Double.NaN), Double.NaN);  // force to Double for comparison
  }

  public void adjust_notNegative() {
    assertEquals(NegativeRateMethod.NOT_NEGATIVE.adjust(1d), 1d, 0d);
    assertEquals(NegativeRateMethod.NOT_NEGATIVE.adjust(0d), 0d, 0d);
    assertEquals(NegativeRateMethod.NOT_NEGATIVE.adjust(-0d), 0d, 0d);
    assertEquals(NegativeRateMethod.NOT_NEGATIVE.adjust(-1d), 0d, 0d);
    assertEquals(NegativeRateMethod.NOT_NEGATIVE.adjust(Double.MAX_VALUE), Double.MAX_VALUE, 0d);
    assertEquals(NegativeRateMethod.NOT_NEGATIVE.adjust(Double.MIN_VALUE), Double.MIN_VALUE, 0d);
    assertEquals(NegativeRateMethod.NOT_NEGATIVE.adjust(Double.POSITIVE_INFINITY), Double.POSITIVE_INFINITY, 0d);
    assertEquals(NegativeRateMethod.NOT_NEGATIVE.adjust(Double.NEGATIVE_INFINITY), 0d, 0d);
    assertEquals(NegativeRateMethod.NOT_NEGATIVE.adjust(Double.NaN), Double.NaN);  // force to Double for comparison
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {NegativeRateMethod.ALLOW_NEGATIVE, "AllowNegative"},
        {NegativeRateMethod.NOT_NEGATIVE, "NotNegative"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(NegativeRateMethod convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(NegativeRateMethod convention, String name) {
    assertEquals(NegativeRateMethod.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> NegativeRateMethod.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> NegativeRateMethod.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(NegativeRateMethod.class);
  }

  public void test_serialization() {
    assertSerialization(NegativeRateMethod.ALLOW_NEGATIVE);
  }

  public void test_jodaConvert() {
    assertJodaConvert(NegativeRateMethod.class, NegativeRateMethod.ALLOW_NEGATIVE);
  }

}
