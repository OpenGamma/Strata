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
    return new Object[][]{
        {FloatingRateName.of("GBP-LIBOR-BBA"), "GBP-LIBOR-BBA", FloatingRateType.IBOR},
        {FloatingRateName.of("CHF-LIBOR-BBA"), "CHF-LIBOR-BBA", FloatingRateType.IBOR},
        {FloatingRateName.of("EUR-LIBOR-BBA"), "EUR-LIBOR-BBA", FloatingRateType.IBOR},
        {FloatingRateName.of("JPY-LIBOR-BBA"), "JPY-LIBOR-BBA", FloatingRateType.IBOR},
        {FloatingRateName.of("USD-LIBOR-BBA"), "USD-LIBOR-BBA", FloatingRateType.IBOR},
        {FloatingRateName.of("EUR-EURIBOR-Reuters"), "EUR-EURIBOR-Reuters", FloatingRateType.IBOR},
        {FloatingRateName.of("JPY-TIBOR-TIBM"), "JPY-TIBOR-TIBM", FloatingRateType.IBOR},

        {FloatingRateName.of("GBP-WMBA-SONIA-COMPOUND"), "GBP-WMBA-SONIA-COMPOUND", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {FloatingRateName.of("CHF-TOIS-OIS-COMPOUND"), "CHF-TOIS-OIS-COMPOUND", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {FloatingRateName.of("EUR-EONIA-OIS-COMPOUND"), "EUR-EONIA-OIS-COMPOUND", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {FloatingRateName.of("JPY-TONA-OIS-COMPOUND"), "JPY-TONA-OIS-COMPOUND", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {FloatingRateName.of("USD-Federal Funds-H.15-OIS-COMPOUND"), "USD-Federal Funds-H.15-OIS-COMPOUND", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {FloatingRateName.of("USD-Federal Funds-H.15"), "USD-Federal Funds-H.15", FloatingRateType.OVERNIGHT_AVERAGED},

        {FloatingRateName.of("UK-HICP"), "UK-HICP", FloatingRateType.PRICE},
        {FloatingRateName.of("UK-RPI"), "UK-RPI", FloatingRateType.PRICE},
        {FloatingRateName.of("UK-RPIX"), "UK-RPIX", FloatingRateType.PRICE},
        {FloatingRateName.of("SWF-CPI"), "SWF-CPI", FloatingRateType.PRICE},
        {FloatingRateName.of("EUR-AI-CPI"), "EUR-AI-CPI", FloatingRateType.PRICE},
        {FloatingRateName.of("EUR-EXT-CPI"), "EUR-EXT-CPI", FloatingRateType.PRICE},
        {FloatingRateName.of("JPY-CPI-EXF"), "JPY-CPI-EXF", FloatingRateType.PRICE},
        {FloatingRateName.of("USA-CPI-U"), "USA-CPI-U", FloatingRateType.PRICE},
        {FloatingRateName.of("FRC-EXT-CPI"), "FRC-EXT-CPI", FloatingRateType.PRICE},
    };
  }

  @Test(dataProvider = "nameType")
  public void test_name(FloatingRateName convention, String name, FloatingRateType type) {
    assertEquals(convention.getName(), name);
    assertEquals(convention.getType(), type);
  }

  @Test(dataProvider = "nameType")
  public void test_toString(FloatingRateName convention, String name, FloatingRateType type) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "nameType")
  public void test_of_lookup(FloatingRateName convention, String name, FloatingRateType type) {
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
