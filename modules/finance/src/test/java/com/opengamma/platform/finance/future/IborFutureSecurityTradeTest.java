/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.future;

import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.collect.id.StandardId;
import com.opengamma.platform.finance.SecurityLink;
import com.opengamma.platform.finance.TradeInfo;

/**
 * Test IborFutureSecurityTrade. 
 */
@Test
public class IborFutureSecurityTradeTest {

  private static final LocalDate TRADE_DATE = date(2015, 2, 17);
  private static final double MULTIPLIER = 35.0;
  private static final double INITIAL_PRICE = 1.015;
  private static final StandardId TRADE_ID = StandardId.of("OG-Trade", "1");
  private static final SecurityLink<IborFuture> SECURITY_LINK =
      SecurityLink.resolvable(StandardId.of("OG-Ticker", "OG"), IborFuture.class);
  private static final ImmutableMap<String, String> ATTRIBUTES = ImmutableMap.of("a", "b");

  //-------------------------------------------------------------------------
  public void test_builder() {
    IborFutureSecurityTrade test = IborFutureSecurityTrade.builder()
        .standardId(TRADE_ID)
        .attributes(ATTRIBUTES)
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(SECURITY_LINK)
        .initialPrice(INITIAL_PRICE)
        .multiplier(MULTIPLIER)
        .build();
    assertEquals(test.getStandardId(), TRADE_ID);
    assertEquals(test.getAttributes(), ATTRIBUTES);
    assertEquals(test.getTradeInfo(), TradeInfo.builder().tradeDate(TRADE_DATE).build());
    assertEquals(test.getSecurityLink(), SECURITY_LINK);
    assertEquals(test.getInitialPrice(), INITIAL_PRICE);
    assertEquals(test.getMultiplier(), MULTIPLIER);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborFutureSecurityTrade test = IborFutureSecurityTrade.builder()
        .standardId(TRADE_ID)
        .attributes(ATTRIBUTES)
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(SECURITY_LINK)
        .initialPrice(INITIAL_PRICE)
        .multiplier(MULTIPLIER)
        .build();
    coverImmutableBean(test);
    IborFutureSecurityTrade test2 = IborFutureSecurityTrade.builder()
        .standardId(StandardId.of("OG-Trade", "2"))
        .tradeInfo(TradeInfo.builder().tradeDate(date(2015, 3, 18)).build())
        .securityLink(SecurityLink.resolvable(StandardId.of("OG-Ticker", "OG2"), IborFuture.class))
        .initialPrice(1.1)
        .multiplier(100.0)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborFutureSecurityTrade test = IborFutureSecurityTrade.builder()
        .standardId(TRADE_ID)
        .attributes(ATTRIBUTES)
        .tradeInfo(TradeInfo.builder().tradeDate(TRADE_DATE).build())
        .securityLink(SECURITY_LINK)
        .initialPrice(INITIAL_PRICE)
        .multiplier(MULTIPLIER)
        .build();
    assertSerialization(test);
  }

}
