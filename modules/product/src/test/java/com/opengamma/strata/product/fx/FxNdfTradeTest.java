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
 * Test {@link FxNdfTrade}.
 */
public class FxNdfTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final FxNdf PRODUCT = FxNdfTest.sut();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2015, 1, 15));

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    FxNdfTrade test = FxNdfTrade.of(TRADE_INFO, PRODUCT);
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
    assertThat(test.getProduct().getCurrencyPair()).isEqualTo(PRODUCT.getCurrencyPair());
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.withInfo(TRADE_INFO).getInfo()).isEqualTo(TRADE_INFO);
  }

  @Test
  public void test_builder() {
    FxNdfTrade test = sut();
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    FxNdfTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.FX_NDF)
        .currencies(Currency.GBP, Currency.USD)
        .description("Rec GBP 100mm @ GBP/USD 1.5 NDF : 19Mar15")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    FxNdfTrade test = sut();
    ResolvedFxNdfTrade expected = ResolvedFxNdfTrade.of(TRADE_INFO, PRODUCT.resolve(REF_DATA));
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
  static FxNdfTrade sut() {
    return FxNdfTrade.builder()
        .product(PRODUCT)
        .info(TRADE_INFO)
        .build();
  }

  static FxNdfTrade sut2() {
    return FxNdfTrade.builder()
        .product(PRODUCT)
        .build();
  }

}
