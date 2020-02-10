/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Test {@link Trade}.
 */
public class TradeTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_methods() {
    Trade test = sut();
    assertThat(test.getId()).isEqualTo(Optional.empty());
    assertThat(test.getInfo()).isEqualTo(TradeInfo.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    Trade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.OTHER)
        .description("Unknown: MockTrade")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  static Trade sut() {
    return new MockTrade();
  }

  private static final class MockTrade implements Trade {
    @Override
    public TradeInfo getInfo() {
      return TradeInfo.empty();
    }

    @Override
    public Trade withInfo(PortfolioItemInfo info) {
      return this;
    }
  }

}
