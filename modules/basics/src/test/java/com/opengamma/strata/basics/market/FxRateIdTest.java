/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.market;

import static com.opengamma.strata.basics.currency.Currency.CHF;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.market.FxRateId.Meta;

/**
 * Test {@link FxRateId}.
 */
@Test
public class FxRateIdTest {

  private static final MarketDataFeed FEED = MarketDataFeed.of("Test");
  private static final CurrencyPair PAIR = CurrencyPair.of(GBP, USD);
  private static final CurrencyPair INVERSE = PAIR.inverse();

  //-------------------------------------------------------------------------
  public void test_of_pair() {
    FxRateId test = FxRateId.of(PAIR);
    FxRateId inverse = FxRateId.of(INVERSE);
    assertEquals(test.getPair(), PAIR);
    assertEquals(inverse.getPair(), PAIR);
    assertEquals(test.getMarketDataFeed(), MarketDataFeed.NONE);
    assertEquals(test.getMarketDataType(), FxRate.class);
  }

  public void test_of_currencies() {
    FxRateId test = FxRateId.of(GBP, USD);
    FxRateId inverse = FxRateId.of(USD, GBP);
    assertEquals(test.getPair(), PAIR);
    assertEquals(inverse.getPair(), PAIR);
    assertEquals(test.getMarketDataFeed(), MarketDataFeed.NONE);
    assertEquals(test.getMarketDataType(), FxRate.class);
  }

  //-------------------------------------------------------------------------
  public void test_of_pairAndFeed() {
    FxRateId test = FxRateId.of(PAIR, FEED);
    FxRateId inverse = FxRateId.of(INVERSE);
    assertEquals(test.getPair(), PAIR);
    assertEquals(inverse.getPair(), PAIR);
    assertEquals(test.getMarketDataFeed(), FEED);
    assertEquals(test.getMarketDataType(), FxRate.class);
  }

  public void test_of_currenciesAndFeed() {
    FxRateId test = FxRateId.of(GBP, USD, FEED);
    FxRateId inverse = FxRateId.of(USD, GBP);
    assertEquals(test.getPair(), PAIR);
    assertEquals(inverse.getPair(), PAIR);
    assertEquals(test.getMarketDataFeed(), FEED);
    assertEquals(test.getMarketDataType(), FxRate.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxRateId test = FxRateId.of(GBP, USD);
    coverImmutableBean(test);
    FxRateId test2 = FxRateId.of(EUR, CHF, FEED);
    coverBeanEquals(test, test2);
  }

  public void coverage_builder() {
    Meta meta = FxRateId.meta();
    FxRateId test1 = meta.builder().setString(meta.pair(), "EUR/GBP").set(meta.marketDataFeed(), FEED).build();
    FxRateId test2 = meta.builder().setString(meta.pair().name(), "EUR/GBP").set(meta.marketDataFeed(), FEED).build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    FxRateId test = FxRateId.of(GBP, USD);
    assertSerialization(test);
  }

}
