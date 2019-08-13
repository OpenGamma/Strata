/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link Index}.
 */
public class IndexTest {

  public static Object[][] data_name() {
    return new Object[][] {
        {IborIndices.GBP_LIBOR_6M, "GBP-LIBOR-6M"},
        {IborIndices.CHF_LIBOR_6M, "CHF-LIBOR-6M"},
        {IborIndices.EUR_LIBOR_6M, "EUR-LIBOR-6M"},
        {IborIndices.JPY_LIBOR_6M, "JPY-LIBOR-6M"},
        {IborIndices.USD_LIBOR_6M, "USD-LIBOR-6M"},

        {OvernightIndices.GBP_SONIA, "GBP-SONIA"},
        {OvernightIndices.CHF_SARON, "CHF-SARON"},
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

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_name(Index convention, String name) {
    assertThat(convention.getName()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(Index convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(Index convention, String name) {
    assertThat(Index.of(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> Index.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> Index.of((String) null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(Indices.class);
  }

}
