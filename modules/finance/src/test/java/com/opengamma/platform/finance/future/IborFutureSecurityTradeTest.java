package com.opengamma.platform.finance.future;

import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.collect.id.Link;
import com.opengamma.collect.id.StandardId;
import com.opengamma.platform.finance.TradeType;

/**
 * Test IborFutureSecurityTrade. 
 */
@Test
public class IborFutureSecurityTradeTest {
  private static final LocalDate TRADE_DATE = date(2015, 2, 17);
  private static final double MULTIPLIER = 35.0;
  private static final double INITIAL_PRICE = 1.015;
  private static final StandardId TRADE_ID = StandardId.of("OG-Trade", "1");
  private static final Link<IborFutureSecurity> SECURITY_LINK = Link.resolvable(StandardId.of("OG-Ticker", "OG"),
      IborFutureSecurity.class);
  private static final ImmutableMap<String, String> ATTRIBUTES = ImmutableMap.of("a", "b");

  /**
   * Test builder. 
   */
  public void builderTest() {
    IborFutureSecurityTrade iborSecurityTrade = IborFutureSecurityTrade.builder().initialPrice(INITIAL_PRICE)
        .standardId(TRADE_ID).multiplier(MULTIPLIER).tradeDate(TRADE_DATE).securityLink(SECURITY_LINK)
        .attributes(ATTRIBUTES).build();
    assertEquals(iborSecurityTrade.getTradeType(), TradeType.of("IborFuture"));
    assertEquals(iborSecurityTrade.getInitialPrice(), INITIAL_PRICE);
    assertEquals(iborSecurityTrade.getTradeDate(), TRADE_DATE);
    assertEquals(iborSecurityTrade.getMultiplier(), MULTIPLIER);
    assertEquals(iborSecurityTrade.getStandardId(), TRADE_ID);
    assertEquals(iborSecurityTrade.getSecurityLink(), SECURITY_LINK);
    assertEquals(iborSecurityTrade.getAttributes(), ATTRIBUTES);
  }

  /**
   * Coverage test. 
   */
  public void coverageTest() {
    IborFutureSecurityTrade iborSecurityTrade1 = IborFutureSecurityTrade.builder().initialPrice(INITIAL_PRICE)
        .standardId(TRADE_ID).multiplier(MULTIPLIER).tradeDate(TRADE_DATE).securityLink(SECURITY_LINK)
        .attributes(ATTRIBUTES).build();
    coverImmutableBean(iborSecurityTrade1);
    IborFutureSecurityTrade iborSecurityTrade2 = IborFutureSecurityTrade.builder().initialPrice(1.1)
        .standardId(StandardId.of("OG-Trade", "2")).tradeDate(date(2015, 3, 18)).multiplier(100.0)
        .securityLink(Link.resolvable(StandardId.of("OG-Ticker", "OG2"), IborFutureSecurity.class)).build();
    coverBeanEquals(iborSecurityTrade1, iborSecurityTrade2);
  }

  /**
   * Serialization test.
   */
  public void serializationTest() {
    IborFutureSecurityTrade iborSecurityTrade = IborFutureSecurityTrade.builder().initialPrice(INITIAL_PRICE)
        .standardId(TRADE_ID).multiplier(MULTIPLIER).tradeDate(TRADE_DATE).securityLink(SECURITY_LINK).build();
    assertSerialization(iborSecurityTrade);
  }
}
