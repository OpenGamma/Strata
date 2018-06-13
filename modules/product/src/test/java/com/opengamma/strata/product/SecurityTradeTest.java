/*
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

import com.opengamma.strata.basics.ImmutableReferenceData;

/**
 * Test {@link SecurityTrade}.
 */
@Test
public class SecurityTradeTest {

  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2016, 6, 30));
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "Id");
  private static final SecurityId SECURITY_ID2 = SecurityId.of("OG-Test", "Id2");
  private static final double QUANTITY = 100;
  private static final double QUANTITY2 = 200;
  private static final double PRICE = 123.50;
  private static final double PRICE2 = 120.50;

  //-------------------------------------------------------------------------
  public void test_of() {
    SecurityTrade test = SecurityTrade.of(TRADE_INFO, SECURITY_ID, QUANTITY, PRICE);
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getSecurityId(), SECURITY_ID);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getPrice(), PRICE);
    assertEquals(test.withInfo(TRADE_INFO).getInfo(), TRADE_INFO);
    assertEquals(test.withQuantity(129).getQuantity(), 129d, 0d);
    assertEquals(test.withPrice(129).getPrice(), 129d, 0d);
  }

  public void test_builder() {
    SecurityTrade test = sut();
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getSecurityId(), SECURITY_ID);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getPrice(), PRICE);
  }

  //-------------------------------------------------------------------------
  public void test_summarize() {
    SecurityTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.SECURITY)
        .description("Id x 100")
        .build();
    assertEquals(trade.summarize(), expected);
  }

  //-------------------------------------------------------------------------
  public void test_resolveTarget() {
    GenericSecurity security = GenericSecurityTest.sut();
    Trade test = sut().resolveTarget(ImmutableReferenceData.of(SECURITY_ID, security));
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
  static SecurityTrade sut() {
    return SecurityTrade.builder()
        .info(TRADE_INFO)
        .securityId(SECURITY_ID)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static SecurityTrade sut2() {
    return SecurityTrade.builder()
        .info(TradeInfo.empty())
        .securityId(SECURITY_ID2)
        .quantity(QUANTITY2)
        .price(PRICE2)
        .build();
  }

}
