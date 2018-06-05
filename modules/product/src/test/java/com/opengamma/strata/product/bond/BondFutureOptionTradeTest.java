/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.TradedPrice;

/**
 * Test {@link BondFutureOptionTrade}. 
 */
@Test
public class BondFutureOptionTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final BondFutureOption OPTION_PRODUCT = BondFutureOptionTest.sut();
  private static final BondFutureOption OPTION_PRODUCT2 = BondFutureOptionTest.sut2();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2014, 3, 31));
  private static final TradeInfo TRADE_INFO2 = TradeInfo.of(date(2014, 4, 1));
  private static final double QUANTITY = 1234;
  private static final double QUANTITY2 = 100;
  private static final Double PRICE = 0.01;
  private static final Double PRICE2 = 0.02;

  //-------------------------------------------------------------------------
  public void test_builder() {
    BondFutureOptionTrade test = sut();
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), OPTION_PRODUCT);
    assertEquals(test.getQuantity(), QUANTITY);
    assertEquals(test.getPrice(), PRICE);
    assertEquals(test.withInfo(TRADE_INFO).getInfo(), TRADE_INFO);
    assertEquals(test.withQuantity(129).getQuantity(), 129d, 0d);
    assertEquals(test.withPrice(129).getPrice(), 129d, 0d);
  }

  //-------------------------------------------------------------------------
  public void test_summarize() {
    BondFutureOptionTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.BOND_FUTURE_OPTION)
        .currencies(Currency.USD)
        .description("BondFutureOption x 1234")
        .build();
    assertEquals(trade.summarize(), expected);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    ResolvedBondFutureOptionTrade expected = ResolvedBondFutureOptionTrade.builder()
        .info(TRADE_INFO)
        .product(OPTION_PRODUCT.resolve(REF_DATA))
        .quantity(QUANTITY)
        .tradedPrice(TradedPrice.of(TRADE_INFO.getTradeDate().get(), PRICE))
        .build();
    assertEquals(sut().resolve(REF_DATA), expected);
  }

  //-------------------------------------------------------------------------
  public void test_withQuantity() {
    BondFutureOptionTrade base = sut();
    double quantity = 5432d;
    BondFutureOptionTrade computed = base.withQuantity(quantity);
    BondFutureOptionTrade expected = BondFutureOptionTrade.builder()
        .info(TRADE_INFO)
        .product(OPTION_PRODUCT)
        .quantity(quantity)
        .price(PRICE)
        .build();
    assertEquals(computed, expected);
  }

  public void test_withPrice() {
    BondFutureOptionTrade base = sut();
    double price = 0.05d;
    BondFutureOptionTrade computed = base.withPrice(price);
    BondFutureOptionTrade expected = BondFutureOptionTrade.builder()
        .info(TRADE_INFO)
        .product(OPTION_PRODUCT)
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
  static BondFutureOptionTrade sut() {
    return BondFutureOptionTrade.builder()
        .info(TRADE_INFO)
        .product(OPTION_PRODUCT)
        .quantity(QUANTITY)
        .price(PRICE)
        .build();
  }

  static BondFutureOptionTrade sut2() {
    return BondFutureOptionTrade.builder()
        .info(TRADE_INFO2)
        .product(OPTION_PRODUCT2)
        .quantity(QUANTITY2)
        .price(PRICE2)
        .build();
  }

}
