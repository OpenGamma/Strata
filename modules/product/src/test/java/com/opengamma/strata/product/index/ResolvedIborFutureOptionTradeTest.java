/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.index;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link ResolvedIborFutureOptionTrade}.
 */
@Test
public class ResolvedIborFutureOptionTradeTest {

  private static final ResolvedIborFutureOption PRODUCT = ResolvedIborFutureOptionTest.sut();
  private static final ResolvedIborFutureOption PRODUCT2 = ResolvedIborFutureOptionTest.sut2();
  private static final LocalDate TRADE_DATE = date(2014, 6, 30);
  private static final TradeInfo TRADE_INFO = TradeInfo.of(TRADE_DATE);
  private static final TradeInfo TRADE_INFO2 = TradeInfo.of(date(2014, 7, 1));
  private static final double QUANTITY = 100;
  private static final double QUANTITY2 = 200;
  private static final double PRICE = 0.99;
  private static final double PRICE2 = 0.98;

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedIborFutureOptionTrade test = sut();
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getPrice(), PRICE);
    assertEquals(test.getTradeDate(), TRADE_DATE);
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
  static ResolvedIborFutureOptionTrade sut() {
    return ResolvedIborFutureOptionTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static ResolvedIborFutureOptionTrade sut2() {
    return ResolvedIborFutureOptionTrade.builder()
        .info(TRADE_INFO2)
        .product(PRODUCT2)
        .quantity(QUANTITY2)
        .price(PRICE2)
        .build();
  }

}
