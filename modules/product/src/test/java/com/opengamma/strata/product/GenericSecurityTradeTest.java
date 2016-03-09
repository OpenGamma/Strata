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

/**
 * Test {@link GenericSecurityTrade}.
 */
@Test
public class GenericSecurityTradeTest {

  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2016, 6, 30));
  private static final GenericSecurity SECURITY = GenericSecurityTest.sut();
  private static final GenericSecurity SECURITY2 = GenericSecurityTest.sut2();
  private static final double QUANTITY = 100;
  private static final double QUANTITY2 = 200;
  private static final double PRICE = 123.50;
  private static final double PRICE2 = 120.50;

  //-------------------------------------------------------------------------
  public void test_of() {
    GenericSecurityTrade test = GenericSecurityTrade.of(TRADE_INFO, SECURITY, QUANTITY, PRICE);
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getPrice(), PRICE);
    assertEquals(test.getCurrency(), SECURITY.getCurrency());
    assertEquals(test.getSecurityId(), SECURITY.getSecurityId());
  }

  public void test_builder() {
    GenericSecurityTrade test = sut();
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getSecurity(), SECURITY);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getPrice(), PRICE);
    assertEquals(test.getCurrency(), SECURITY.getCurrency());
    assertEquals(test.getSecurityId(), SECURITY.getSecurityId());
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
  static GenericSecurityTrade sut() {
    return GenericSecurityTrade.builder()
        .info(TRADE_INFO)
        .security(SECURITY)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static GenericSecurityTrade sut2() {
    return GenericSecurityTrade.builder()
        .info(TradeInfo.empty())
        .security(SECURITY2)
        .quantity(QUANTITY2)
        .price(PRICE2)
        .build();
  }

}
