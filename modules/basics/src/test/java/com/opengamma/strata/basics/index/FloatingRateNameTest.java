/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.Tenor;

/**
 * Test {@link FloatingRateName}.
 */
@Test
public class FloatingRateNameTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "nameType")
  static Object[][] data_name_type() {
    return new Object[][] {
        {"GBP-LIBOR", "GBP-LIBOR-", FloatingRateType.IBOR},
        {"GBP-LIBOR-BBA", "GBP-LIBOR-", FloatingRateType.IBOR},
        {"CHF-LIBOR", "CHF-LIBOR-", FloatingRateType.IBOR},
        {"CHF-LIBOR-BBA", "CHF-LIBOR-", FloatingRateType.IBOR},
        {"EUR-LIBOR", "EUR-LIBOR-", FloatingRateType.IBOR},
        {"EUR-LIBOR-BBA", "EUR-LIBOR-", FloatingRateType.IBOR},
        {"JPY-LIBOR", "JPY-LIBOR-", FloatingRateType.IBOR},
        {"JPY-LIBOR-BBA", "JPY-LIBOR-", FloatingRateType.IBOR},
        {"USD-LIBOR", "USD-LIBOR-", FloatingRateType.IBOR},
        {"USD-LIBOR-BBA", "USD-LIBOR-", FloatingRateType.IBOR},
        {"EUR-EURIBOR", "EUR-EURIBOR-", FloatingRateType.IBOR},
        {"EUR-EURIBOR-Reuters", "EUR-EURIBOR-", FloatingRateType.IBOR},
        {"JPY-TIBOR-JAPAN", "JPY-TIBOR-JAPAN-", FloatingRateType.IBOR},
        {"JPY-TIBOR-TIBM", "JPY-TIBOR-JAPAN-", FloatingRateType.IBOR},

        {"GBP-SONIA", "GBP-SONIA", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"GBP-WMBA-SONIA-COMPOUND", "GBP-SONIA", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"CHF-TOIS", "CHF-TOIS", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"CHF-TOIS-OIS-COMPOUND", "CHF-TOIS", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"EUR-EONIA", "EUR-EONIA", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"EUR-EONIA-OIS-COMPOUND", "EUR-EONIA", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"JPY-TONAR", "JPY-TONAR", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"JPY-TONA-OIS-COMPOUND", "JPY-TONAR", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"USD-FED-FUND", "USD-FED-FUND", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"USD-Federal Funds-H.15-OIS-COMPOUND", "USD-FED-FUND", FloatingRateType.OVERNIGHT_COMPOUNDED},

        {"USD-FED-FUND-AVG", "USD-FED-FUND", FloatingRateType.OVERNIGHT_AVERAGED},
        {"USD-Federal Funds-H.15", "USD-FED-FUND", FloatingRateType.OVERNIGHT_AVERAGED},

        {"UK-HICP", "UK-HICP", FloatingRateType.PRICE},
        {"UK-RPI", "UK-RPI", FloatingRateType.PRICE},
        {"UK-RPIX", "UK-RPIX", FloatingRateType.PRICE},
        {"SWF-CPI", "SWF-CPI", FloatingRateType.PRICE},
        {"EUR-AI-CPI", "EUR-AI-CPI", FloatingRateType.PRICE},
        {"EUR-EXT-CPI", "EUR-EXT-CPI", FloatingRateType.PRICE},
        {"JPY-CPI-EXF", "JPY-CPI-EXF", FloatingRateType.PRICE},
        {"USA-CPI-U", "USA-CPI-U", FloatingRateType.PRICE},
        {"FRC-EXT-CPI", "FRC-EXT-CPI", FloatingRateType.PRICE},
    };
  }

  @Test(dataProvider = "nameType")
  public void test_name(String name, String indexName, FloatingRateType type) {
    FloatingRateName convention = FloatingRateName.of(name);
    assertEquals(convention.getName(), name);
    assertEquals(convention.getIndexName(), indexName);
    assertEquals(convention.getType(), type);
  }

  @Test(dataProvider = "nameType")
  public void test_toString(String name, String indexName, FloatingRateType type) {
    FloatingRateName convention = FloatingRateName.of(name);
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "nameType")
  public void test_of_lookup(String name, String indexName, FloatingRateType type) {
    FloatingRateName convention = FloatingRateName.of(name);
    assertEquals(FloatingRateName.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> FloatingRateName.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> FloatingRateName.of(null));
  }

  //-------------------------------------------------------------------------
  public void test_toIborIndex_tenor() {
    assertEquals(FloatingRateName.of("GBP-LIBOR-BBA").toIborIndex(Tenor.TENOR_6M), IborIndices.GBP_LIBOR_6M);
    assertThrows(() -> FloatingRateName.of("GBP-WMBA-SONIA-COMPOUND").toIborIndex(Tenor.TENOR_6M), IllegalStateException.class);
  }

  public void test_toOvernightIndex() {
    assertEquals(FloatingRateName.of("GBP-WMBA-SONIA-COMPOUND").toOvernightIndex(), OvernightIndices.GBP_SONIA);
    assertThrows(() -> FloatingRateName.of("GBP-LIBOR-BBA").toOvernightIndex(), IllegalStateException.class);
  }

  public void test_toPriceIndex() {
    assertEquals(FloatingRateName.of("UK-HICP").toPriceIndex(), PriceIndices.UK_HICP);
    assertThrows(() -> FloatingRateName.of("GBP-LIBOR-BBA").toPriceIndex(), IllegalStateException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(FloatingRateNames.class);
    coverImmutableBean(FloatingRateName.of("GBP-LIBOR-BBA"));
    coverBeanEquals(FloatingRateName.of("GBP-LIBOR-BBA"), FloatingRateName.of("USD-Federal Funds-H.15"));
  }

  public void test_jodaConvert() {
    assertJodaConvert(FloatingRateName.class, FloatingRateName.of("GBP-LIBOR-BBA"));
  }

  public void test_serialization() {
    assertSerialization(FloatingRateName.of("GBP-LIBOR-BBA"));
  }

}
