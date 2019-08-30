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
 * Test {@link FxSingleTrade}.
 */
public class FxSingleTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final FxSingle PRODUCT = FxSingleTest.sut();
  private static final FxSingle PRODUCT2 = FxSingleTest.sut2();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2015, 1, 15));

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    FxSingleTrade test = FxSingleTrade.of(TRADE_INFO, PRODUCT);
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
    assertThat(test.getProduct().getCurrencyPair()).isEqualTo(PRODUCT.getCurrencyPair());
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.withInfo(TRADE_INFO).getInfo()).isEqualTo(TRADE_INFO);
  }

  @Test
  public void test_builder() {
    FxSingleTrade test = FxSingleTrade.builder()
        .product(PRODUCT)
        .build();
    assertThat(test.getInfo()).isEqualTo(TradeInfo.empty());
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    FxSingleTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.FX_SINGLE)
        .currencies(Currency.GBP, Currency.USD)
        .description("Rec GBP 1k @ GBP/USD 1.6 : 30Jun15")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    FxSingleTrade test = FxSingleTrade.builder()
        .product(PRODUCT)
        .info(TRADE_INFO)
        .build();
    ResolvedFxSingleTrade expected = ResolvedFxSingleTrade.of(TRADE_INFO, PRODUCT.resolve(REF_DATA));
    assertThat(test.resolve(REF_DATA)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FxSingleTrade test = sut();
    coverImmutableBean(test);
    FxSingleTrade test2 = sut2();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static FxSingleTrade sut() {
    return FxSingleTrade.builder()
        .info(TradeInfo.of(date(2014, 6, 30)))
        .product(PRODUCT)
        .build();
  }

  static FxSingleTrade sut2() {
    return FxSingleTrade.builder()
        .product(PRODUCT2)
        .build();
  }

}
