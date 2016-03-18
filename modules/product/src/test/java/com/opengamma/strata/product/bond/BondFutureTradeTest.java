/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link BondFutureTrade}.
 */
@Test
public class BondFutureTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  // future
  private static final BondFuture FUTURE = BondFutureTest.sut();
  private static final BondFuture FUTURE2 = BondFutureTest.sut2();
  // trade
  private static final LocalDate TRADE_DATE = LocalDate.of(2011, 6, 20);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(TRADE_DATE).build();
  private static final long QUANTITY = 1234L;
  private static final long QUANTITY2 = 100L;
  private static final double PRICE = 1.2345;
  private static final double PRICE2 = 1.3;

  //-------------------------------------------------------------------------
  public void test_builder() {
    BondFutureTrade test = sut();
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), FUTURE);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getPrice(), PRICE);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    ResolvedBondFutureTrade expected = ResolvedBondFutureTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(FUTURE.resolve(REF_DATA))
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    assertEquals(sut().resolve(REF_DATA), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  static BondFutureTrade sut() {
    return BondFutureTrade.builder()
        .tradeInfo(TRADE_INFO)
        .product(FUTURE)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static BondFutureTrade sut2() {
    return BondFutureTrade.builder()
        .product(FUTURE2)
        .quantity(QUANTITY2)
        .price(PRICE2)
        .build();
  }

}
