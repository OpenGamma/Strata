/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.ImmutableReferenceData;

/**
 * Test {@link SecurityIdTrade}.
 */
@Test
public class SecurityIdTradeTest {

  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(date(2016, 6, 30)).build();
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "Id");
  private static final SecurityId SECURITY_ID2 = SecurityId.of("OG-Test", "Id2");
  private static final int QUANTITY = 100;
  private static final int QUANTITY2 = 200;
  private static final double PRICE = 123.50;
  private static final double PRICE2 = 120.50;

  //-------------------------------------------------------------------------
  public void test_of() {
    SecurityIdTrade test = SecurityIdTrade.of(TRADE_INFO, SECURITY_ID, QUANTITY, PRICE);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getSecurityId(), SECURITY_ID);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getPrice(), PRICE);
  }

  public void test_builder() {
    SecurityIdTrade test = sut();
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getSecurityId(), SECURITY_ID);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getPrice(), PRICE);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    GenericSecurity security = GenericSecurityTest.sut();
    FinanceTrade test = sut().resolve(ImmutableReferenceData.of(SECURITY_ID, security));
    GenericSecurityTrade expected = GenericSecurityTrade.of(TRADE_INFO, security, QUANTITY, PRICE);
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static SecurityIdTrade sut() {
    return SecurityIdTrade.builder()
        .tradeInfo(TRADE_INFO)
        .securityId(SECURITY_ID)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static SecurityIdTrade sut2() {
    return SecurityIdTrade.builder()
        .tradeInfo(TradeInfo.EMPTY)
        .securityId(SECURITY_ID2)
        .quantity(QUANTITY2)
        .price(PRICE2)
        .build();
  }

}
