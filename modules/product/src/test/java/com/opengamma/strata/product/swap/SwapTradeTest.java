/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

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
 * Test.
 */
public class SwapTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2014, 6, 30));
  private static final Swap SWAP1 = Swap.of(MockSwapLeg.MOCK_GBP1, MockSwapLeg.MOCK_USD1);
  private static final Swap SWAP2 = Swap.of(MockSwapLeg.MOCK_GBP1);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    SwapTrade test = SwapTrade.of(TRADE_INFO, SWAP1);
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.getProduct()).isEqualTo(SWAP1);
    assertThat(test.withInfo(TRADE_INFO).getInfo()).isEqualTo(TRADE_INFO);
  }

  @Test
  public void test_builder() {
    SwapTrade test = SwapTrade.builder()
        .product(SWAP1)
        .build();
    assertThat(test.getInfo()).isEqualTo(TradeInfo.empty());
    assertThat(test.getProduct()).isEqualTo(SWAP1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    SwapTrade trade = SwapTrade.of(TRADE_INFO, SWAP1);
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.SWAP)
        .currencies(Currency.GBP, Currency.EUR, Currency.USD)
        .description(
            "7M Pay [GBP-LIBOR-3M, EUR/GBP-ECB, EUR-EONIA] / Rec [GBP-LIBOR-3M, EUR/GBP-ECB, EUR-EONIA] : 15Jan12-15Aug12")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    SwapTrade test = SwapTrade.of(TRADE_INFO, SWAP1);
    assertThat(test.resolve(REF_DATA).getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.resolve(REF_DATA).getProduct()).isEqualTo(SWAP1.resolve(REF_DATA));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    SwapTrade test = SwapTrade.builder()
        .info(TRADE_INFO)
        .product(SWAP1)
        .build();
    coverImmutableBean(test);
    SwapTrade test2 = SwapTrade.builder()
        .product(SWAP2)
        .build();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    SwapTrade test = SwapTrade.builder()
        .info(TRADE_INFO)
        .product(SWAP1)
        .build();
    assertSerialization(test);
  }

}
