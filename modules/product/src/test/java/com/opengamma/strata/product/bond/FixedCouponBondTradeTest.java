/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link FixedCouponBondTrade}.
 */
@Test
public class FixedCouponBondTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate TRADE_DATE = date(2015, 3, 25);
  private static final LocalDate SETTLEMENT_DATE = date(2015, 3, 30);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder()
      .tradeDate(TRADE_DATE)
      .settlementDate(SETTLEMENT_DATE)
      .build();
  private static final TradeInfo TRADE_INFO2 = TradeInfo.builder()
      .tradeDate(TRADE_DATE)
      .build();
  private static final double QUANTITY = 10;
  private static final double PRICE = 123;
  private static final double PRICE2 = 200;
  private static final FixedCouponBond PRODUCT = FixedCouponBondTest.sut();
  private static final FixedCouponBond PRODUCT2 = FixedCouponBondTest.sut2();

  //-------------------------------------------------------------------------
  public void test_builder_resolved() {
    FixedCouponBondTrade test = sut();
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getPrice(), PRICE);
    assertEquals(test.withInfo(TRADE_INFO).getInfo(), TRADE_INFO);
    assertEquals(test.withQuantity(129).getQuantity(), 129d, 0d);
    assertEquals(test.withPrice(129).getPrice(), 129d, 0d);
  }

  //-------------------------------------------------------------------------
  public void test_summarize() {
    FixedCouponBondTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.BOND)
        .currencies(Currency.EUR)
        .description("Bond x 10")
        .build();
    assertEquals(trade.summarize(), expected);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    ResolvedFixedCouponBondTrade expected = ResolvedFixedCouponBondTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT.resolve(REF_DATA))
        .quantity(QUANTITY)
        .settlement(ResolvedFixedCouponBondSettlement.of(SETTLEMENT_DATE, PRICE))
        .build();
    assertEquals(sut().resolve(REF_DATA), expected);
  }

  public void test_resolve_noTradeOrSettlementDate() {
    FixedCouponBondTrade test = FixedCouponBondTrade.builder()
        .info(TradeInfo.empty())
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
    assertThrows(() -> test.resolve(REF_DATA), IllegalStateException.class);
  }

  //-------------------------------------------------------------------------
  public void test_withQuantity() {
    FixedCouponBondTrade base = sut();
    double quantity = 75343d;
    FixedCouponBondTrade computed = base.withQuantity(quantity);
    FixedCouponBondTrade expected = FixedCouponBondTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(quantity)
        .price(PRICE)
        .build();
    assertEquals(computed, expected);
  }

  public void test_withPrice() {
    FixedCouponBondTrade base = sut();
    double price = 135d;
    FixedCouponBondTrade computed = base.withPrice(price);
    FixedCouponBondTrade expected = FixedCouponBondTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(price)
        .build();
    assertEquals(computed, expected);
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
  static FixedCouponBondTrade sut() {
    return FixedCouponBondTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static FixedCouponBondTrade sut2() {
    return FixedCouponBondTrade.builder()
        .info(TRADE_INFO2)
        .product(PRODUCT2)
        .quantity(100L)
        .price(PRICE2)
        .build();
  }

}
