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
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link FxSwapTrade}.
 */
public class FxSwapTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final FxSwap PRODUCT = FxSwapTest.sut();
  private static final FxSwap PRODUCT2 = FxSwapTest.sut2();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2011, 11, 14));

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    FxSwapTrade test = FxSwapTrade.of(TRADE_INFO, PRODUCT);
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
    assertThat(test.getProduct().getCurrencyPair()).isEqualTo(PRODUCT.getCurrencyPair());
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.withInfo(TRADE_INFO).getInfo()).isEqualTo(TRADE_INFO);
  }

  @Test
  public void test_builder() {
    FxSwapTrade test = sut();
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    FxSwapTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.FX_SWAP)
        .currencies(Currency.GBP, Currency.USD)
        .description("Rec GBP 1k @ GBP/USD 1.6 / Pay GBP 1k @ GBP/USD 1.55 : 21Nov11-21Dec11")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    FxSwapTrade test = sut();
    ResolvedFxSwapTrade expected = ResolvedFxSwapTrade.of(TRADE_INFO, PRODUCT.resolve(REF_DATA));
    assertThat(test.resolve(REF_DATA)).isEqualTo(expected);
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
