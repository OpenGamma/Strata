/*
 * Copyright (C) 2020 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link StandardId}.
 */
public class StandardSchemesTest {

  @Test
  public void test_schemes() {
    assertThat(StandardSchemes.OG_TICKER_SCHEME).isEqualTo("OG-Ticker");
    assertThat(StandardSchemes.OG_ETD_SCHEME).isEqualTo("OG-ETD");
    assertThat(StandardSchemes.OG_TRADE_SCHEME).isEqualTo("OG-Trade");
    assertThat(StandardSchemes.OG_POSITION_SCHEME).isEqualTo("OG-Position");
    assertThat(StandardSchemes.OG_SENSITIVITY_SCHEME).isEqualTo("OG-Sensitivity");
    assertThat(StandardSchemes.OG_SECURITY_SCHEME).isEqualTo("OG-Security");
    assertThat(StandardSchemes.OG_COUNTERPARTY).isEqualTo("OG-Counterparty");

    assertThat(StandardSchemes.ISIN_SCHEME).isEqualTo("ISIN");
    assertThat(StandardSchemes.CUSIP_SCHEME).isEqualTo("CUSIP");
    assertThat(StandardSchemes.SEDOL_SCHEME).isEqualTo("SEDOL");
    assertThat(StandardSchemes.WKN_SCHEME).isEqualTo("WKN");
    assertThat(StandardSchemes.VALOR_SCHEME).isEqualTo("VALOR");

    assertThat(StandardSchemes.RIC_SCHEME).isEqualTo("RIC");
    assertThat(StandardSchemes.CHAIN_RIC_SCHEME).isEqualTo("CHAINRIC");
    assertThat(StandardSchemes.BBG_SCHEME).isEqualTo("BBG");
    assertThat(StandardSchemes.FIGI_SCHEME).isEqualTo("FIGI");
    assertThat(StandardSchemes.LEI_SCHEME).isEqualTo("LEI");
    assertThat(StandardSchemes.RED6_SCHEME).isEqualTo("RED6");
    assertThat(StandardSchemes.RED9_SCHEME).isEqualTo("RED9");
  }

}
