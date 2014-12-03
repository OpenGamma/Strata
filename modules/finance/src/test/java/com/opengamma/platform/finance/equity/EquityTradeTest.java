/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.equity;

import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.currency.CurrencyAmount;
import com.opengamma.platform.finance.TradeType;
import com.opengamma.platform.source.id.StandardId;
import com.opengamma.platform.source.link.Link;

/**
 * Test.
 */
@Test
public class EquityTradeTest {

  public void test_of() {
    EquityTrade test = EquityTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeDate(LocalDate.of(2014, 12, 3))
        .equityLink(Link.resolvable(StandardId.of("OG-Ticker", "OG"), Equity.class))
        .quantity(60)
        .paymentAmount(CurrencyAmount.of(Currency.GBP, -1000))
        .build();
    assertEquals(test.getTradeType(), TradeType.of("Equity"));
    assertEquals(test.getPayment(), Optional.of(CurrencyAmount.of(Currency.GBP, -1000)));
  }

  public void test_of_noPayment() {
    EquityTrade test = EquityTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeDate(LocalDate.of(2014, 12, 3))
        .equityLink(Link.resolvable(StandardId.of("OG-Ticker", "OG"), Equity.class))
        .quantity(60)
        .build();
    assertEquals(test.getTradeType(), TradeType.of("Equity"));
    assertEquals(test.getPayment(), Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    EquityTrade test = EquityTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeDate(LocalDate.of(2014, 12, 3))
        .equityLink(Link.resolvable(StandardId.of("OG-Ticker", "OG"), Equity.class))
        .quantity(60)
        .paymentAmount(CurrencyAmount.of(Currency.GBP, -1000))
        .build();
    coverImmutableBean(test);
    EquityTrade test2 = EquityTrade.builder()
        .standardId(StandardId.of("OG-Trade", "2"))
        .attributes(ImmutableMap.of("a", "b"))
        .tradeDate(LocalDate.of(2014, 12, 4))
        .equityLink(Link.resolvable(StandardId.of("OG-Ticker", "OG2"), Equity.class))
        .quantity(70)
        .paymentAmount(CurrencyAmount.of(Currency.GBP, -2000))
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    EquityTrade test = EquityTrade.builder()
        .standardId(StandardId.of("OG-Trade", "1"))
        .tradeDate(LocalDate.of(2014, 12, 3))
        .equityLink(Link.resolvable(StandardId.of("OG-Ticker", "OG"), Equity.class))
        .quantity(60)
        .paymentAmount(CurrencyAmount.of(Currency.GBP, -1000))
        .build();
    assertSerialization(test);
  }

}
