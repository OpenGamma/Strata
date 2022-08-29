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
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.TradedPrice;

/**
 * Test {@link ResolvedOvernightFutureOptionTrade}.
 */
public class ResolvedOvernightFutureOptionTradeTest {

  private static final ResolvedOvernightFutureOption PRODUCT = ResolvedOvernightFutureOptionTest.sut();
  private static final ResolvedOvernightFutureOption PRODUCT2 = ResolvedOvernightFutureOptionTest.sut2();
  private static final LocalDate TRADE_DATE = date(2014, 6, 30);
  private static final TradeInfo TRADE_INFO = TradeInfo.of(TRADE_DATE);
  private static final TradeInfo TRADE_INFO2 = TradeInfo.of(date(2014, 7, 1));
  private static final double QUANTITY = 100;
  private static final double QUANTITY2 = 200;
  private static final double PRICE = 0.99;
  private static final double PRICE2 = 0.98;

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    ResolvedOvernightFutureOptionTrade test = sut();
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
    assertThat(test.getQuantity()).isEqualTo(QUANTITY);
    assertThat(test.getTradedPrice()).isEqualTo(Optional.of(TradedPrice.of(TRADE_DATE, PRICE)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  @Test
  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static ResolvedOvernightFutureOptionTrade sut() {
    return ResolvedOvernightFutureOptionTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .tradedPrice(TradedPrice.of(TRADE_DATE, PRICE))
        .build();
  }

  static ResolvedOvernightFutureOptionTrade sut2() {
    return ResolvedOvernightFutureOptionTrade.builder()
        .info(TRADE_INFO2)
        .product(PRODUCT2)
        .quantity(QUANTITY2)
        .tradedPrice(TradedPrice.of(TRADE_DATE, PRICE2))
        .build();
  }

}
