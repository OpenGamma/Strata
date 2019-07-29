/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

import static com.opengamma.strata.basics.currency.Currency.CHF;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.joda.beans.Bean;
import org.joda.beans.MetaBean;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;

/**
 * Test {@link FxRateId}.
 */
public class FxRateIdTest {

  private static final ObservableSource OBS_SOURCE = ObservableSource.of("Test");
  private static final CurrencyPair PAIR = CurrencyPair.of(GBP, USD);
  private static final CurrencyPair INVERSE = PAIR.inverse();

  //-------------------------------------------------------------------------
  @Test
  public void test_of_pair() {
    FxRateId test = FxRateId.of(PAIR);
    FxRateId inverse = FxRateId.of(INVERSE);
    assertThat(test.getPair()).isEqualTo(PAIR);
    assertThat(inverse.getPair()).isEqualTo(PAIR);
    assertThat(test.getObservableSource()).isEqualTo(ObservableSource.NONE);
    assertThat(test.getMarketDataType()).isEqualTo(FxRate.class);
    assertThat(test.toString()).isEqualTo("FxRateId:GBP/USD");
  }

  @Test
  public void test_of_currencies() {
    FxRateId test = FxRateId.of(GBP, USD);
    FxRateId inverse = FxRateId.of(USD, GBP);
    assertThat(test.getPair()).isEqualTo(PAIR);
    assertThat(inverse.getPair()).isEqualTo(PAIR);
    assertThat(test.getObservableSource()).isEqualTo(ObservableSource.NONE);
    assertThat(test.getMarketDataType()).isEqualTo(FxRate.class);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_pairAndSource() {
    FxRateId test = FxRateId.of(PAIR, OBS_SOURCE);
    FxRateId inverse = FxRateId.of(INVERSE);
    assertThat(test.getPair()).isEqualTo(PAIR);
    assertThat(inverse.getPair()).isEqualTo(PAIR);
    assertThat(test.getObservableSource()).isEqualTo(OBS_SOURCE);
    assertThat(test.getMarketDataType()).isEqualTo(FxRate.class);
  }

  @Test
  public void test_of_currenciesAndSource() {
    FxRateId test = FxRateId.of(GBP, USD, OBS_SOURCE);
    FxRateId inverse = FxRateId.of(USD, GBP);
    assertThat(test.getPair()).isEqualTo(PAIR);
    assertThat(inverse.getPair()).isEqualTo(PAIR);
    assertThat(test.getObservableSource()).isEqualTo(OBS_SOURCE);
    assertThat(test.getMarketDataType()).isEqualTo(FxRate.class);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FxRateId test = FxRateId.of(GBP, USD);
    coverImmutableBean(test);
    FxRateId test2 = FxRateId.of(EUR, CHF, OBS_SOURCE);
    coverBeanEquals(test, test2);
  }

  @Test
  public void coverage_builder() {
    MetaBean meta = MetaBean.of(FxRateId.class);
    Bean test1 = meta.builder()
        .set("pair", CurrencyPair.parse("EUR/GBP"))
        .set("observableSource", OBS_SOURCE)
        .build();
    Bean test2 = meta.builder()
        .set("pair", CurrencyPair.parse("EUR/GBP"))
        .set("observableSource", OBS_SOURCE)
        .build();
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    FxRateId test = FxRateId.of(GBP, USD);
    assertSerialization(test);
  }

}
