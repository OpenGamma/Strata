/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.dsf;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.TradedPrice;

/**
 * Test {@link DsfTrade}.
 */
@Test
public class DsfTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  static final Dsf PRODUCT = DsfTest.sut();
  static final Dsf PRODUCT2 = DsfTest.sut2();
  private static final LocalDate TRADE_DATE = LocalDate.of(2014, 6, 12);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder()
      .tradeDate(TRADE_DATE)
      .settlementDate(LocalDate.of(2014, 6, 14))
      .build();
  private static final double QUANTITY = 100L;
  private static final double QUANTITY2 = 200L;
  private static final double PRICE = 0.99;
  private static final double PRICE2 = 0.98;

  //-------------------------------------------------------------------------
  public void test_builder() {
    DsfTrade test = sut();
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getPrice(), PRICE);
    assertEquals(test.getSecurityId(), PRODUCT.getSecurityId());
    assertEquals(test.getCurrency(), PRODUCT.getCurrency());
    assertEquals(test.withInfo(TRADE_INFO).getInfo(), TRADE_INFO);
    assertEquals(test.withQuantity(129).getQuantity(), 129d, 0d);
    assertEquals(test.withPrice(129).getPrice(), 129d, 0d);
  }

  //-------------------------------------------------------------------------
  public void test_summarize() {
    DsfTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.DSF)
        .currencies(Currency.USD)
        .description("DSF x 100")
        .build();
    assertEquals(trade.summarize(), expected);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    DsfTrade test = sut();
    ResolvedDsfTrade expected = ResolvedDsfTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT.resolve(REF_DATA))
        .quantity(QUANTITY)
        .tradedPrice(TradedPrice.of(TRADE_DATE, PRICE))
        .build();
    assertEquals(test.resolve(REF_DATA), expected);
  }

  //-------------------------------------------------------------------------
  public void test_withQuantity() {
    DsfTrade base = sut();
    double quantity = 6423d;
    DsfTrade computed = base.withQuantity(quantity);
    DsfTrade expected = DsfTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(quantity)
        .price(PRICE)
        .build();
    assertEquals(computed, expected);
  }

  public void test_withPrice() {
    DsfTrade base = sut();
    double price = 6423d;
    DsfTrade computed = base.withPrice(price);
    DsfTrade expected = DsfTrade.builder()
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
  static DsfTrade sut() {
    return DsfTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static DsfTrade sut2() {
    return DsfTrade.builder()
        .product(PRODUCT2)
        .quantity(QUANTITY2)
        .price(PRICE2)
        .build();
  }

}
