/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

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

/**
 * Test {@link FxSwapTrade}.
 */
@Test
public class FxSwapTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final FxSwap PRODUCT = FxSwapTest.sut();
  private static final FxSwap PRODUCT2 = FxSwapTest.sut2();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2011, 11, 14));

  //-------------------------------------------------------------------------
  public void test_of() {
    FxSwapTrade test = FxSwapTrade.of(TRADE_INFO, PRODUCT);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getProduct().getCurrencyPair(), PRODUCT.getCurrencyPair());
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.withInfo(TRADE_INFO).getInfo(), TRADE_INFO);
  }

  public void test_builder() {
    FxSwapTrade test = sut();
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), PRODUCT);
  }

  //-------------------------------------------------------------------------
  public void test_summarize() {
    FxSwapTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.FX_SWAP)
        .currencies(Currency.GBP, Currency.USD)
        .description("Rec GBP 1k @ GBP/USD 1.6 / Pay GBP 1k @ GBP/USD 1.55 : 21Nov11-21Dec11")
        .build();
    assertEquals(trade.summarize(), expected);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    FxSwapTrade test = sut();
    ResolvedFxSwapTrade expected = ResolvedFxSwapTrade.of(TRADE_INFO, PRODUCT.resolve(REF_DATA));
    assertEquals(test.resolve(REF_DATA), expected);
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
  static FxSwapTrade sut() {
    return FxSwapTrade.builder()
        .product(PRODUCT)
        .info(TRADE_INFO)
        .build();
  }

  static FxSwapTrade sut2() {
    return FxSwapTrade.builder()
        .product(PRODUCT2)
        .build();
  }

}
