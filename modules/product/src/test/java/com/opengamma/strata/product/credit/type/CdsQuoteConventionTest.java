/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit.type;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link CdsQuoteConvention}.
 */
@Test
public class CdsQuoteConventionTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {CdsQuoteConvention.PAR_SPREAD, "ParSpread"},
        {CdsQuoteConvention.POINTS_UPFRONT, "PointsUpfront"},
        {CdsQuoteConvention.QUOTED_SPREAD, "QuotedSpread"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(CdsQuoteConvention convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(CdsQuoteConvention convention, String name) {
    assertEquals(CdsQuoteConvention.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> CdsQuoteConvention.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> CdsQuoteConvention.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(CdsQuoteConvention.class);
  }

  public void test_serialization() {
    assertSerialization(CdsQuoteConvention.POINTS_UPFRONT);
  }

  public void test_jodaConvert() {
    assertJodaConvert(CdsQuoteConvention.class, CdsQuoteConvention.POINTS_UPFRONT);
  }

}
