/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.DKCO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.MXMC;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.joda.beans.ImmutableBean;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;

/**
 * Test {@link FloatingRateName}.
 */
@Test
public class FloatingRateNameTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "nameType")
  public static Object[][] data_name_type() {
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
        {"GBP-SONIA-COMPOUND", "GBP-SONIA", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"CHF-SARON", "CHF-SARON", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"CHF-SARON-OIS-COMPOUND", "CHF-SARON", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"CHF-TOIS", "CHF-TOIS", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"CHF-TOIS-OIS-COMPOUND", "CHF-TOIS", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"EUR-EONIA", "EUR-EONIA", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"EUR-EONIA-OIS-COMPOUND", "EUR-EONIA", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"JPY-TONAR", "JPY-TONAR", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"JPY-TONA-OIS-COMPOUND", "JPY-TONAR", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"USD-FED-FUND", "USD-FED-FUND", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"USD-Federal Funds-H.15-OIS-COMPOUND", "USD-FED-FUND", FloatingRateType.OVERNIGHT_COMPOUNDED},

        {"USD-FED-FUND-AVG", "USD-FED-FUND-AVG", FloatingRateType.OVERNIGHT_AVERAGED},
        {"USD-Federal Funds-H.15", "USD-FED-FUND-AVG", FloatingRateType.OVERNIGHT_AVERAGED},

        {"GB-HICP", "GB-HICP", FloatingRateType.PRICE},
        {"UK-HICP", "GB-HICP", FloatingRateType.PRICE},
        {"GB-RPI", "GB-RPI", FloatingRateType.PRICE},
        {"UK-RPI", "GB-RPI", FloatingRateType.PRICE},
        {"GB-RPIX", "GB-RPIX", FloatingRateType.PRICE},
        {"UK-RPIX", "GB-RPIX", FloatingRateType.PRICE},
        {"CH-CPI", "CH-CPI", FloatingRateType.PRICE},
        {"SWF-CPI", "CH-CPI", FloatingRateType.PRICE},
        {"EU-AI-CPI", "EU-AI-CPI", FloatingRateType.PRICE},
        {"EUR-AI-CPI", "EU-AI-CPI", FloatingRateType.PRICE},
        {"EU-EXT-CPI", "EU-EXT-CPI", FloatingRateType.PRICE},
        {"EUR-EXT-CPI", "EU-EXT-CPI", FloatingRateType.PRICE},
        {"JP-CPI-EXF", "JP-CPI-EXF", FloatingRateType.PRICE},
        {"JPY-CPI-EXF", "JP-CPI-EXF", FloatingRateType.PRICE},
        {"US-CPI-U", "US-CPI-U", FloatingRateType.PRICE},
        {"USA-CPI-U", "US-CPI-U", FloatingRateType.PRICE},
        {"FR-EXT-CPI", "FR-EXT-CPI", FloatingRateType.PRICE},
        {"FRC-EXT-CPI", "FR-EXT-CPI", FloatingRateType.PRICE},

        {"AUD-BBR-BBSW", "AUD-BBSW", FloatingRateType.IBOR},
        {"CAD-BA-CDOR", "CAD-CDOR", FloatingRateType.IBOR},
        {"CNY-CNREPOFIX=CFXS-Reuters", "CNY-REPO", FloatingRateType.IBOR},
        {"CZK-PRIBOR-PRBO", "CZK-PRIBOR", FloatingRateType.IBOR},
        {"DKK-CIBOR-DKNA13", "DKK-CIBOR", FloatingRateType.IBOR},
        {"HKD-HIBOR-ISDC", "HKD-HIBOR", FloatingRateType.IBOR},
        {"HKD-HIBOR-HIBOR=", "HKD-HIBOR", FloatingRateType.IBOR},
        {"HUF-BUBOR-Reuters", "HUF-BUBOR", FloatingRateType.IBOR},
        {"KRW-CD-KSDA-Bloomberg", "KRW-CD", FloatingRateType.IBOR},
        {"MXN-TIIE-Banxico", "MZN-TIIE", FloatingRateType.IBOR},
        {"NOK-NIBOR-OIBOR", "NOK-NIBOR", FloatingRateType.IBOR},
        {"NZD-BBR-FRA", "NZD-BBR", FloatingRateType.IBOR},
        {"PLN-WIBOR-WIBO", "PLN-WIBOR", FloatingRateType.IBOR},
        {"SEK-STIBOR-Bloomberg", "SEK-STIBOR", FloatingRateType.IBOR},
        {"SGD-SOR-VWAP", "SGD-SOR", FloatingRateType.IBOR},
        {"ZAR-JIBAR-SAFEX", "ZAR-JIBAR", FloatingRateType.IBOR},

        {"INR-MIBOR-OIS-COMPOUND", "INR-OMIBOR", FloatingRateType.OVERNIGHT_COMPOUNDED},
    };
  }

  @Test(dataProvider = "nameType")
  public void test_name(String name, String indexName, FloatingRateType type) {
    FloatingRateName test = FloatingRateName.of(name);
    assertEquals(test.getName(), name);
    assertEquals(test.getType(), type);
    assertEquals(test.getCurrency(), test.toFloatingRateIndex().getCurrency());
  }

  @Test(dataProvider = "nameType")
  public void test_toString(String name, String indexName, FloatingRateType type) {
    FloatingRateName test = FloatingRateName.of(name);
    assertEquals(test.toString(), name);
  }

  @Test(dataProvider = "nameType")
  public void test_of_lookup(String name, String indexName, FloatingRateType type) {
    FloatingRateName test = FloatingRateName.of(name);
    assertEquals(FloatingRateName.of(name), test);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> FloatingRateName.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> FloatingRateName.of(null));
  }

  public void test_parse() {
    assertEquals(FloatingRateName.parse("GBP-LIBOR"), FloatingRateNames.GBP_LIBOR);
    assertEquals(FloatingRateName.parse("GBP-LIBOR-3M"), FloatingRateNames.GBP_LIBOR);
    assertEquals(FloatingRateName.parse("GBP-SONIA"), FloatingRateNames.GBP_SONIA);
    assertEquals(FloatingRateName.parse("GB-RPI"), FloatingRateNames.GB_RPI);
    assertThrowsIllegalArg(() -> FloatingRateName.parse(null));
    assertThrowsIllegalArg(() -> FloatingRateName.parse("NotAnIndex"));
  }

  //-------------------------------------------------------------------------
  public void test_defaultIborIndex() {
    assertEquals(FloatingRateName.defaultIborIndex(Currency.GBP), FloatingRateNames.GBP_LIBOR);
    assertEquals(FloatingRateName.defaultIborIndex(Currency.EUR), FloatingRateNames.EUR_EURIBOR);
    assertEquals(FloatingRateName.defaultIborIndex(Currency.USD), FloatingRateNames.USD_LIBOR);
    assertEquals(FloatingRateName.defaultIborIndex(Currency.AUD), FloatingRateNames.AUD_BBSW);
    assertEquals(FloatingRateName.defaultIborIndex(Currency.CAD), FloatingRateNames.CAD_CDOR);
    assertEquals(FloatingRateName.defaultIborIndex(Currency.NZD), FloatingRateNames.NZD_BKBM);
  }

  public void test_defaultOvernightIndex() {
    assertEquals(FloatingRateName.defaultOvernightIndex(Currency.GBP), FloatingRateName.of("GBP-SONIA"));
    assertEquals(FloatingRateName.defaultOvernightIndex(Currency.EUR), FloatingRateName.of("EUR-EONIA"));
    assertEquals(FloatingRateName.defaultOvernightIndex(Currency.USD), FloatingRateName.of("USD-FED-FUND"));
  }

  //-------------------------------------------------------------------------
  public void test_normalized() {
    assertEquals(FloatingRateName.of("GBP-LIBOR-BBA").normalized(), FloatingRateName.of("GBP-LIBOR"));
    assertEquals(FloatingRateName.of("GBP-WMBA-SONIA-COMPOUND").normalized(), FloatingRateName.of("GBP-SONIA"));
    for (FloatingRateName name : FloatingRateName.extendedEnum().lookupAll().values()) {
      assertNotNull(name.normalized());
    }
  }

  //-------------------------------------------------------------------------
  public void test_iborIndex_tenor() {
    assertEquals(FloatingRateName.of("GBP-LIBOR-BBA").getDefaultTenor(), Tenor.TENOR_3M);
    assertEquals(FloatingRateName.of("GBP-LIBOR-BBA").toFloatingRateIndex(), IborIndices.GBP_LIBOR_3M);
    assertEquals(FloatingRateName.of("GBP-LIBOR-BBA").toFloatingRateIndex(Tenor.TENOR_1M), IborIndices.GBP_LIBOR_1M);
    assertEquals(FloatingRateName.of("GBP-LIBOR-BBA").toIborIndex(Tenor.TENOR_6M), IborIndices.GBP_LIBOR_6M);
    assertEquals(FloatingRateName.of("GBP-LIBOR-BBA").toIborIndex(Tenor.TENOR_12M), IborIndices.GBP_LIBOR_12M);
    assertEquals(FloatingRateName.of("GBP-LIBOR-BBA").toIborIndex(Tenor.TENOR_1Y), IborIndices.GBP_LIBOR_12M);
    assertThrows(() -> FloatingRateName.of("GBP-WMBA-SONIA-COMPOUND").toIborIndex(Tenor.TENOR_6M), IllegalStateException.class);
    assertEquals(
        ImmutableList.copyOf(FloatingRateName.of("GBP-LIBOR-BBA").getTenors()),
        ImmutableList.of(Tenor.TENOR_1W, Tenor.TENOR_1M, Tenor.TENOR_2M, Tenor.TENOR_3M, Tenor.TENOR_6M, Tenor.TENOR_12M));
    assertEquals(
        FloatingRateName.of("GBP-LIBOR-BBA").toIborIndexFixingOffset(),
        DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(PRECEDING, GBLO)));
  }

  public void test_overnightIndex() {
    assertEquals(FloatingRateName.of("GBP-WMBA-SONIA-COMPOUND").getDefaultTenor(), Tenor.TENOR_1D);
    assertEquals(FloatingRateName.of("GBP-WMBA-SONIA-COMPOUND").toFloatingRateIndex(), OvernightIndices.GBP_SONIA);
    assertEquals(FloatingRateName.of("GBP-WMBA-SONIA-COMPOUND").toFloatingRateIndex(Tenor.TENOR_1M), OvernightIndices.GBP_SONIA);
    assertEquals(FloatingRateName.of("GBP-WMBA-SONIA-COMPOUND").toOvernightIndex(), OvernightIndices.GBP_SONIA);
    assertEquals(FloatingRateNames.USD_FED_FUND.toOvernightIndex(), OvernightIndices.USD_FED_FUND);
    assertEquals(FloatingRateNames.USD_FED_FUND_AVG.toOvernightIndex(), OvernightIndices.USD_FED_FUND);
    assertThrows(() -> FloatingRateName.of("GBP-LIBOR-BBA").toOvernightIndex(), IllegalStateException.class);
    assertEquals(FloatingRateName.of("GBP-WMBA-SONIA-COMPOUND").getTenors(), ImmutableSet.of());
    assertThrows(() -> FloatingRateName.of("GBP-WMBA-SONIA-COMPOUND").toIborIndexFixingOffset(), IllegalStateException.class);
  }

  public void test_priceIndex() {
    assertEquals(FloatingRateName.of("UK-HICP").getDefaultTenor(), Tenor.TENOR_1Y);
    assertEquals(FloatingRateName.of("UK-HICP").toFloatingRateIndex(), PriceIndices.GB_HICP);
    assertEquals(FloatingRateName.of("UK-HICP").toFloatingRateIndex(Tenor.TENOR_1M), PriceIndices.GB_HICP);
    assertEquals(FloatingRateName.of("UK-HICP").toPriceIndex(), PriceIndices.GB_HICP);
    assertThrows(() -> FloatingRateName.of("GBP-LIBOR-BBA").toPriceIndex(), IllegalStateException.class);
    assertEquals(FloatingRateName.of("UK-HICP").getTenors(), ImmutableSet.of());
    assertThrows(() -> FloatingRateName.of("UK-HICP").toIborIndexFixingOffset(), IllegalStateException.class);
  }

  //-------------------------------------------------------------------------
  public void test_cibor() {
    assertEquals(FloatingRateName.of("DKK-CIBOR-DKNA13").getDefaultTenor(), Tenor.TENOR_3M);
    assertEquals(FloatingRateName.of("DKK-CIBOR-DKNA13").toFloatingRateIndex(), IborIndices.DKK_CIBOR_3M);
    assertEquals(FloatingRateName.of("DKK-CIBOR-DKNA13").toFloatingRateIndex(Tenor.TENOR_1M), IborIndices.DKK_CIBOR_1M);
    assertEquals(FloatingRateName.of("DKK-CIBOR-DKNA13").toIborIndex(Tenor.TENOR_6M), IborIndices.DKK_CIBOR_6M);
    assertEquals(FloatingRateName.of("DKK-CIBOR2-DKNA13").toIborIndex(Tenor.TENOR_6M), IborIndices.DKK_CIBOR_6M);
    assertEquals(
        FloatingRateName.of("DKK-CIBOR-DKNA13").toIborIndexFixingOffset(),
        DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(PRECEDING, DKCO)));
    assertEquals(
        FloatingRateName.of("DKK-CIBOR2-DKNA13").toIborIndexFixingOffset(),
        DaysAdjustment.ofBusinessDays(-2, DKCO));
  }

  public void test_tiee() {
    assertEquals(FloatingRateName.of("MXN-TIIE").getDefaultTenor(), Tenor.TENOR_13W);
    assertEquals(FloatingRateName.of("MXN-TIIE").toFloatingRateIndex(), IborIndices.MXN_TIIE_13W);
    assertEquals(FloatingRateName.of("MXN-TIIE").toFloatingRateIndex(Tenor.TENOR_4W), IborIndices.MXN_TIIE_4W);
    assertEquals(FloatingRateName.of("MXN-TIIE").toIborIndex(Tenor.TENOR_4W), IborIndices.MXN_TIIE_4W);
    assertEquals(FloatingRateName.of("MXN-TIIE").toIborIndexFixingOffset(), DaysAdjustment.ofBusinessDays(-1, MXMC));
  }

  public void test_nzd() {
    assertEquals(FloatingRateName.of("NZD-BKBM").getCurrency(), Currency.NZD);
    assertEquals(FloatingRateName.of("NZD-NZIONA").getCurrency(), Currency.NZD);
  }

  //-------------------------------------------------------------------------
  public void test_types() {
    // ensure no stupid copy and paste errors
    Field[] fields = FloatingRateNames.class.getFields();
    for (Field field : fields) {
      if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
        assertEquals(field.getType(), FloatingRateName.class);
      }
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(FloatingRateNames.class);
    ImmutableBean test = (ImmutableBean) FloatingRateName.of("GBP-LIBOR-BBA");
    coverImmutableBean(test);
    coverBeanEquals(test, (ImmutableBean) FloatingRateName.of("USD-Federal Funds-H.15"));
  }

  public void test_jodaConvert() {
    assertJodaConvert(FloatingRateName.class, FloatingRateName.of("GBP-LIBOR-BBA"));
  }

  public void test_serialization() {
    assertSerialization(FloatingRateName.of("GBP-LIBOR-BBA"));
  }

}
