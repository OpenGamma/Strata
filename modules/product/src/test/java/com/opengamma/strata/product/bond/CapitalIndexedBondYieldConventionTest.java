/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link CapitalIndexedBondYieldConvention}.
 */
@Test
public class CapitalIndexedBondYieldConventionTest {

  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {CapitalIndexedBondYieldConvention.US_IL_REAL, "US-I/L-Real"},
        {CapitalIndexedBondYieldConvention.GB_IL_FLOAT, "GB-I/L-Float"},
        {CapitalIndexedBondYieldConvention.GB_IL_BOND, "GB-I/L-Bond"},
        {CapitalIndexedBondYieldConvention.JP_IL_SIMPLE, "JP-I/L-Simple"},
        {CapitalIndexedBondYieldConvention.JP_IL_COMPOUND, "JP-I/L-Compound"}
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(CapitalIndexedBondYieldConvention convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(CapitalIndexedBondYieldConvention convention, String name) {
    assertEquals(CapitalIndexedBondYieldConvention.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> CapitalIndexedBondYieldConvention.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> CapitalIndexedBondYieldConvention.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(CapitalIndexedBondYieldConvention.class);
  }

  public void test_serialization() {
    assertSerialization(CapitalIndexedBondYieldConvention.US_IL_REAL);
  }

  public void test_jodaConvert() {
    assertJodaConvert(CapitalIndexedBondYieldConvention.class, CapitalIndexedBondYieldConvention.GB_IL_BOND);
  }

}
