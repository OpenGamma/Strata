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
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;

import org.joda.beans.ImmutableBean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.Tenor;

/**
 * Test {@link FloatingRateName}.
 */
public class FloatingRateNamesTest {

  //-------------------------------------------------------------------------
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
        {"EUR-ESTR", "EUR-ESTR", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"EUR-ESTR-COMPOUND", "EUR-ESTR", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"EUR-ESTER", "EUR-ESTR", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"EUR-ESTER-OIS-COMPOUND", "EUR-ESTR", FloatingRateType.OVERNIGHT_COMPOUNDED},
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
        {"MXN-TIIE-Banxico", "MXN-TIIE", FloatingRateType.IBOR},
        {"NOK-NIBOR-OIBOR", "NOK-NIBOR", FloatingRateType.IBOR},
        {"NZD-BBR-FRA", "NZD-BKBM", FloatingRateType.IBOR},
        {"PLN-WIBOR-WIBO", "PLN-WIBOR", FloatingRateType.IBOR},
        {"SEK-STIBOR-Bloomberg", "SEK-STIBOR", FloatingRateType.IBOR},
        {"SGD-SOR-VWAP", "SGD-SOR", FloatingRateType.IBOR},
        {"ZAR-JIBAR-SAFEX", "ZAR-JIBAR", FloatingRateType.IBOR},

        {"HKD-HONIA-OIS-COMPOUND", "HKD-HONIA", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"HKD-HONIX-OIS-COMPOUND", "HKD-HONIA", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"INR-MIBOR-OIS-COMPOUND", "INR-OMIBOR", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"RUB-RUONIA-OIS-COMPOUND", "RUB-RUONIA", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"SGD-SORA-COMPOUND", "SGD-SORA", FloatingRateType.OVERNIGHT_COMPOUNDED},
        {"TRY-TLREF-OIS-COMPOUND", "TRY-TLREF", FloatingRateType.OVERNIGHT_COMPOUNDED},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name_type")
  public void test_name(String name, String indexName, FloatingRateType type) {
    FloatingRateName test = FloatingRateName.of(name);
    assertThat(test.getName()).isEqualTo(name);
    assertThat(test.normalized().getName())
        .isEqualTo(indexName.endsWith("-") ? indexName.substring(0, indexName.length() - 1) : indexName);
    assertThat(test.getType()).isEqualTo(type);
    assertThat(test.getCurrency()).isEqualTo(test.toFloatingRateIndex().getCurrency());
  }

  @ParameterizedTest
  @MethodSource("data_name_type")
  public void test_toString(String name, String indexName, FloatingRateType type) {
    FloatingRateName test = FloatingRateName.of(name);
    assertThat(test.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name_type")
  public void test_of_lookup(String name, String indexName, FloatingRateType type) {
    FloatingRateName test = FloatingRateName.of(name);
    assertThat(FloatingRateName.of(name)).isEqualTo(test);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> FloatingRateName.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> FloatingRateName.of(null));
  }

  @Test
  public void test_parse() {
    assertThat(FloatingRateName.parse("GBP-LIBOR")).isEqualTo(FloatingRateNames.GBP_LIBOR);
    assertThat(FloatingRateName.parse("GBP-LIBOR-3M")).isEqualTo(FloatingRateNames.GBP_LIBOR);
    assertThat(FloatingRateName.parse("GBP-SONIA")).isEqualTo(FloatingRateNames.GBP_SONIA);
    assertThat(FloatingRateName.parse("GB-RPI")).isEqualTo(FloatingRateNames.GB_RPI);
    assertThatIllegalArgumentException().isThrownBy(() -> FloatingRateName.parse(null));
    assertThatIllegalArgumentException().isThrownBy(() -> FloatingRateName.parse("NotAnIndex"));
  }

  @Test
  public void test_tryParse() {
    assertThat(FloatingRateName.tryParse("GBP-LIBOR")).isEqualTo(Optional.of(FloatingRateNames.GBP_LIBOR));
    assertThat(FloatingRateName.tryParse("GBP-LIBOR-3M")).isEqualTo(Optional.of(FloatingRateNames.GBP_LIBOR));
    assertThat(FloatingRateName.tryParse("GBP-SONIA")).isEqualTo(Optional.of(FloatingRateNames.GBP_SONIA));
    assertThat(FloatingRateName.tryParse("GB-RPI")).isEqualTo(Optional.of(FloatingRateNames.GB_RPI));
    assertThat(FloatingRateName.tryParse(null)).isEqualTo(Optional.empty());
    assertThat(FloatingRateName.tryParse("NotAnIndex")).isEqualTo(Optional.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_defaultIborIndex() {
    assertThat(FloatingRateName.defaultIborIndex(Currency.GBP)).isEqualTo(FloatingRateNames.GBP_LIBOR);
    assertThat(FloatingRateName.defaultIborIndex(Currency.EUR)).isEqualTo(FloatingRateNames.EUR_EURIBOR);
    assertThat(FloatingRateName.defaultIborIndex(Currency.USD)).isEqualTo(FloatingRateNames.USD_LIBOR);
    assertThat(FloatingRateName.defaultIborIndex(Currency.AUD)).isEqualTo(FloatingRateNames.AUD_BBSW);
    assertThat(FloatingRateName.defaultIborIndex(Currency.CAD)).isEqualTo(FloatingRateNames.CAD_CDOR);
    assertThat(FloatingRateName.defaultIborIndex(Currency.NZD)).isEqualTo(FloatingRateNames.NZD_BKBM);
  }

  @Test
  public void test_defaultOvernightIndex() {
    assertThat(FloatingRateName.defaultOvernightIndex(Currency.GBP)).isEqualTo(FloatingRateName.of("GBP-SONIA"));
    assertThat(FloatingRateName.defaultOvernightIndex(Currency.EUR)).isEqualTo(FloatingRateName.of("EUR-EONIA"));
    assertThat(FloatingRateName.defaultOvernightIndex(Currency.USD)).isEqualTo(FloatingRateName.of("USD-FED-FUND"));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_normalized() {
    assertThat(FloatingRateName.of("GBP-LIBOR-BBA").normalized()).isEqualTo(FloatingRateName.of("GBP-LIBOR"));
    assertThat(FloatingRateName.of("GBP-WMBA-SONIA-COMPOUND").normalized()).isEqualTo(FloatingRateName.of("GBP-SONIA"));
    for (FloatingRateName name : FloatingRateName.extendedEnum().lookupAll().values()) {
      assertThat(name.normalized()).isNotNull();
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_getFloatingRateName() {
    for (FloatingRateName name : FloatingRateName.extendedEnum().lookupAll().values()) {
      assertThat(name.getFloatingRateName()).isEqualTo(name);
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_iborIndex_tenor() {
    assertThat(FloatingRateName.of("GBP-LIBOR-BBA").getDefaultTenor()).isEqualTo(Tenor.TENOR_3M);
    assertThat(FloatingRateName.of("GBP-LIBOR-BBA").toFloatingRateIndex()).isEqualTo(IborIndices.GBP_LIBOR_3M);
    assertThat(FloatingRateName.of("GBP-LIBOR-BBA").toFloatingRateIndex(Tenor.TENOR_1M)).isEqualTo(IborIndices.GBP_LIBOR_1M);
    assertThat(FloatingRateName.of("GBP-LIBOR-BBA").toIborIndex(Tenor.TENOR_6M)).isEqualTo(IborIndices.GBP_LIBOR_6M);
    assertThat(FloatingRateName.of("GBP-LIBOR-BBA").toIborIndex(Tenor.TENOR_12M)).isEqualTo(IborIndices.GBP_LIBOR_12M);
    assertThat(FloatingRateName.of("GBP-LIBOR-BBA").toIborIndex(Tenor.TENOR_1Y)).isEqualTo(IborIndices.GBP_LIBOR_12M);
    assertThatIllegalStateException()
        .isThrownBy(() -> FloatingRateName.of("GBP-WMBA-SONIA-COMPOUND").toIborIndex(Tenor.TENOR_6M));
    assertThat(ImmutableList.copyOf(FloatingRateName.of("GBP-LIBOR-BBA").getTenors()))
        .containsExactly(Tenor.TENOR_1W, Tenor.TENOR_1M, Tenor.TENOR_2M, Tenor.TENOR_3M, Tenor.TENOR_6M, Tenor.TENOR_12M);
    assertThat(FloatingRateName.of("GBP-LIBOR-BBA").toIborIndexFixingOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(PRECEDING, GBLO)));
  }

  @Test
  public void test_overnightIndex() {
    assertThat(FloatingRateName.of("GBP-WMBA-SONIA-COMPOUND").getDefaultTenor()).isEqualTo(Tenor.TENOR_1D);
    assertThat(FloatingRateName.of("GBP-WMBA-SONIA-COMPOUND").toFloatingRateIndex()).isEqualTo(OvernightIndices.GBP_SONIA);
    assertThat(FloatingRateName.of("GBP-WMBA-SONIA-COMPOUND").toFloatingRateIndex(Tenor.TENOR_1M))
        .isEqualTo(OvernightIndices.GBP_SONIA);
    assertThat(FloatingRateName.of("GBP-WMBA-SONIA-COMPOUND").toOvernightIndex()).isEqualTo(OvernightIndices.GBP_SONIA);
    assertThat(FloatingRateNames.USD_FED_FUND.toOvernightIndex()).isEqualTo(OvernightIndices.USD_FED_FUND);
    assertThat(FloatingRateNames.USD_FED_FUND_AVG.toOvernightIndex()).isEqualTo(OvernightIndices.USD_FED_FUND);
    assertThatIllegalStateException()
        .isThrownBy(() -> FloatingRateName.of("GBP-LIBOR-BBA").toOvernightIndex());
    assertThat(FloatingRateName.of("GBP-WMBA-SONIA-COMPOUND").getTenors()).isEqualTo(ImmutableSet.of());
    assertThatIllegalStateException()
        .isThrownBy(() -> FloatingRateName.of("GBP-WMBA-SONIA-COMPOUND").toIborIndexFixingOffset());
  }

  @Test
  public void test_priceIndex() {
    assertThat(FloatingRateName.of("UK-HICP").getDefaultTenor()).isEqualTo(Tenor.TENOR_1Y);
    assertThat(FloatingRateName.of("UK-HICP").toFloatingRateIndex()).isEqualTo(PriceIndices.GB_HICP);
    assertThat(FloatingRateName.of("UK-HICP").toFloatingRateIndex(Tenor.TENOR_1M)).isEqualTo(PriceIndices.GB_HICP);
    assertThat(FloatingRateName.of("UK-HICP").toPriceIndex()).isEqualTo(PriceIndices.GB_HICP);
    assertThatIllegalStateException()
        .isThrownBy(() -> FloatingRateName.of("GBP-LIBOR-BBA").toPriceIndex());
    assertThat(FloatingRateName.of("UK-HICP").getTenors()).isEqualTo(ImmutableSet.of());
    assertThatIllegalStateException()
        .isThrownBy(() -> FloatingRateName.of("UK-HICP").toIborIndexFixingOffset());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_cibor() {
    assertThat(FloatingRateName.of("DKK-CIBOR-DKNA13").getDefaultTenor()).isEqualTo(Tenor.TENOR_3M);
    assertThat(FloatingRateName.of("DKK-CIBOR-DKNA13").toFloatingRateIndex()).isEqualTo(IborIndices.DKK_CIBOR_3M);
    assertThat(FloatingRateName.of("DKK-CIBOR-DKNA13").toFloatingRateIndex(Tenor.TENOR_1M)).isEqualTo(IborIndices.DKK_CIBOR_1M);
    assertThat(FloatingRateName.of("DKK-CIBOR-DKNA13").toIborIndex(Tenor.TENOR_6M)).isEqualTo(IborIndices.DKK_CIBOR_6M);
    assertThat(FloatingRateName.of("DKK-CIBOR2-DKNA13").toIborIndex(Tenor.TENOR_6M)).isEqualTo(IborIndices.DKK_CIBOR_6M);
    assertThat(FloatingRateName.of("DKK-CIBOR-DKNA13").toIborIndexFixingOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(PRECEDING, DKCO)));
    assertThat(FloatingRateName.of("DKK-CIBOR2-DKNA13").toIborIndexFixingOffset())
        .isEqualTo(DaysAdjustment.ofBusinessDays(-2, DKCO));
  }

  @Test
  public void test_tiee() {
    assertThat(FloatingRateName.of("MXN-TIIE").getDefaultTenor()).isEqualTo(Tenor.TENOR_13W);
    assertThat(FloatingRateName.of("MXN-TIIE").toFloatingRateIndex()).isEqualTo(IborIndices.MXN_TIIE_13W);
    assertThat(FloatingRateName.of("MXN-TIIE").toFloatingRateIndex(Tenor.TENOR_4W)).isEqualTo(IborIndices.MXN_TIIE_4W);
    assertThat(FloatingRateName.of("MXN-TIIE").toIborIndex(Tenor.TENOR_4W)).isEqualTo(IborIndices.MXN_TIIE_4W);
    assertThat(FloatingRateName.of("MXN-TIIE").toIborIndexFixingOffset())
        .isEqualTo(DaysAdjustment.ofBusinessDays(-1, MXMC));
  }

  @Test
  public void test_nzd() {
    assertThat(FloatingRateName.of("NZD-BKBM").getCurrency()).isEqualTo(Currency.NZD);
    assertThat(FloatingRateName.of("NZD-NZIONA").getCurrency()).isEqualTo(Currency.NZD);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_types() {
    // ensure no stupid copy and paste errors
    Field[] fields = FloatingRateNames.class.getFields();
    for (Field field : fields) {
      if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
        assertThat(field.getType()).isEqualTo(FloatingRateName.class);
      }
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(FloatingRateNames.class);
    ImmutableBean test = (ImmutableBean) FloatingRateName.of("GBP-LIBOR-BBA");
    coverImmutableBean(test);
    coverBeanEquals(test, (ImmutableBean) FloatingRateName.of("USD-Federal Funds-H.15"));
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(FloatingRateName.class, FloatingRateName.of("GBP-LIBOR-BBA"));
  }

  @Test
  public void test_serialization() {
    assertSerialization(FloatingRateName.of("GBP-LIBOR-BBA"));
  }

}
