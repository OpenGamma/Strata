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
 * Test IborFutureOptionSecurityTrade.
 */
@Test
public class IborFutureOptionSecurityTradeTest {
  private static final LocalDate TRADE_DATE = date(2015, 2, 17);
  private static final double MULTIPLIER = 35.0;
  private static final double INITIAL_PRICE = 0.015;
  private static final StandardId TRADE_ID = StandardId.of("OG-Trade", "1");
  private static final Link<IborFutureOptionSecurity> SECURITY_LINK = Link.resolvable(StandardId.of("OG-Ticker", "OG"),
      IborFutureOptionSecurity.class);
  private static final ImmutableMap<String, String> ATTRIBUTES = ImmutableMap.of("a", "b");

  /**
   * Test builder. 
   */
  public void builderTest() {
    IborFutureOptionSecurityTrade optionTrade = IborFutureOptionSecurityTrade.builder().attributes(ATTRIBUTES)
        .initialPrice(INITIAL_PRICE).multiplier(MULTIPLIER).securityLink(SECURITY_LINK).standardId(TRADE_ID)
        .tradeDate(TRADE_DATE).build();
    assertEquals(optionTrade.getTradeType(), TradeType.of("IborFutureOption"));
    assertEquals(optionTrade.getInitialPrice(), INITIAL_PRICE);
    assertEquals(optionTrade.getAttributes(), ATTRIBUTES);
    assertEquals(optionTrade.getStandardId(), TRADE_ID);
    assertEquals(optionTrade.getTradeDate(), TRADE_DATE);
    assertEquals(optionTrade.getMultiplier(), MULTIPLIER);
    assertEquals(optionTrade.getSecurityLink(), SECURITY_LINK);
  }

  /**
   * Coverage test. 
   */
  public void coverageTest() {
    IborFutureOptionSecurityTrade optionTrade1 = IborFutureOptionSecurityTrade.builder().attributes(ATTRIBUTES)
        .initialPrice(INITIAL_PRICE).multiplier(MULTIPLIER).securityLink(SECURITY_LINK).standardId(TRADE_ID)
        .tradeDate(TRADE_DATE).build();
    coverImmutableBean(optionTrade1);
    IborFutureOptionSecurityTrade optionTrade2 = IborFutureOptionSecurityTrade.builder().initialPrice(0.05)
        .multiplier(100.0).standardId(StandardId.of("OG-Trade", "2")).tradeDate(date(2015, 3, 17))
        .securityLink(Link.resolvable(StandardId.of("OG-Ticker", "OG2"), IborFutureOptionSecurity.class)).build();
    coverBeanEquals(optionTrade1, optionTrade2);
  }

  /**
   * Serialization test.
   */
  public void serializationTest() {
    IborFutureOptionSecurityTrade optionTrade1 = IborFutureOptionSecurityTrade.builder().attributes(ATTRIBUTES)
        .initialPrice(INITIAL_PRICE).multiplier(MULTIPLIER).securityLink(SECURITY_LINK).standardId(TRADE_ID)
        .tradeDate(TRADE_DATE).build();
    assertSerialization(optionTrade1);
  }
}
