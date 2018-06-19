/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Test {@link BillTrade}.
 */
@Test
public class BillTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate TRADE_DATE = date(2015, 3, 25);
  private static final LocalDate SETTLEMENT_DATE = date(2015, 3, 30);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder()
      .tradeDate(TRADE_DATE)
      .settlementDate(SETTLEMENT_DATE)
      .build();
  private static final TradeInfo TRADE_INFO2 = TradeInfo.builder()
      .tradeDate(TRADE_DATE)
      .settlementDate(SETTLEMENT_DATE.plusDays(1))
      .build();
  private static final double QUANTITY = 10;
  private static final double YIELD = 1.23;
  private static final double YIELD2 = 2.00;
  private static final Bill PRODUCT = BillTest.US_BILL;
  private static final Bill PRODUCT2 = BillTest.BILL_2;

  //-------------------------------------------------------------------------
  public void test_builder_resolved() {
    BillTrade test = sut();
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getPrice(), YIELD);
    assertEquals(test.withInfo(TRADE_INFO).getInfo(), TRADE_INFO);
    assertEquals(test.withQuantity(129).getQuantity(), 129d, 0d);
    assertEquals(test.withPrice(129).getPrice(), 129d, 0d);
  }

  //-------------------------------------------------------------------------
  public void test_summarize() {
    BillTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.BILL)
        .currencies(Currency.USD)
        .description("Bill2019-05-23 x 10")
        .build();
    assertEquals(trade.summarize(), expected);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    ResolvedBillTrade expected = ResolvedBillTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT.resolve(REF_DATA))
        .quantity(QUANTITY)
        .settlement(ResolvedBillSettlement.of(SETTLEMENT_DATE, YIELD))
        .build();
    assertEquals(sut().resolve(REF_DATA), expected);
  }

  public void test_resolve_noTradeOrSettlementDate() {
    assertThrows(() -> BillTrade.builder()
        .info(TradeInfo.empty())
        .product(PRODUCT)
        .quantity(QUANTITY)
        .yield(YIELD)
        .build(), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_withQuantity() {
    BillTrade base = sut();
    double quantity = 75343d;
    BillTrade computed = base.withQuantity(quantity);
    BillTrade expected = BillTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(quantity)
        .yield(YIELD)
        .build();
    assertEquals(computed, expected);
  }

  public void test_withPrice() {
    BillTrade base = sut();
    double price = 135d;
    BillTrade computed = base.withPrice(price);
    BillTrade expected = BillTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .yield(price)
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
  static BillTrade sut() {
    return BillTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .yield(YIELD)
        .build();
  }

  static BillTrade sut2() {
    return BillTrade.builder()
        .info(TRADE_INFO2)
        .product(PRODUCT2)
        .quantity(100L)
        .yield(YIELD2)
        .build();
  }

}
