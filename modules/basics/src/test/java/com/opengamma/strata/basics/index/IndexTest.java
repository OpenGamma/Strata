/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link Index}.
 */
@Test
public class IndexTest {

  @DataProvider(name = "name")
  public static Object[][] data_name() {
    return new Object[][] {
        {IborIndices.GBP_LIBOR_6M, "GBP-LIBOR-6M"},
        {IborIndices.CHF_LIBOR_6M, "CHF-LIBOR-6M"},
        {IborIndices.EUR_LIBOR_6M, "EUR-LIBOR-6M"},
        {IborIndices.JPY_LIBOR_6M, "JPY-LIBOR-6M"},
        {IborIndices.USD_LIBOR_6M, "USD-LIBOR-6M"},

        {OvernightIndices.GBP_SONIA, "GBP-SONIA"},
        {OvernightIndices.CHF_TOIS, "CHF-TOIS"},
        {OvernightIndices.EUR_EONIA, "EUR-EONIA"},
        {OvernightIndices.JPY_TONAR, "JPY-TONAR"},
        {OvernightIndices.USD_FED_FUND, "USD-FED-FUND"},

        {PriceIndices.GB_HICP, "GB-HICP"},
        {PriceIndices.CH_CPI, "CH-CPI"},
        {PriceIndices.EU_AI_CPI, "EU-AI-CPI"},

        {FxIndices.EUR_CHF_ECB, "EUR/CHF-ECB"},
        {FxIndices.EUR_GBP_ECB, "EUR/GBP-ECB"},
        {FxIndices.GBP_USD_WM, "GBP/USD-WM"},
        {FxIndices.USD_JPY_WM, "USD/JPY-WM"},
    };
  }

  @Test(dataProvider = "name")
  public void test_name(Index convention, String name) {
    assertEquals(convention.getName(), name);
  }

  @Test(dataProvider = "name")
  public void test_toString(Index convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(Index convention, String name) {
    assertEquals(Index.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> Index.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> Index.of((String) null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(Indices.class);
  }

}
