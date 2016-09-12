/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Test {@link PriceIndexCalculationMethod}.
 */
@Test
public class PriceIndexCalculationMethodTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {PriceIndexCalculationMethod.MONTHLY, "Monthly"},
        {PriceIndexCalculationMethod.INTERPOLATED, "Interpolated"},
        {PriceIndexCalculationMethod.INTERPOLATED_JAPAN, "InterpolatedJapan"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(PriceIndexCalculationMethod convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(PriceIndexCalculationMethod convention, String name) {
    assertEquals(PriceIndexCalculationMethod.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> PriceIndexCalculationMethod.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> PriceIndexCalculationMethod.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(PriceIndexCalculationMethod.class);
  }

  public void test_serialization() {
    assertSerialization(PriceIndexCalculationMethod.INTERPOLATED);
  }

  public void test_jodaConvert() {
    assertJodaConvert(PriceIndexCalculationMethod.class, PriceIndexCalculationMethod.INTERPOLATED);
  }

}
