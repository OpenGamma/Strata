/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.Test;

/**
 * Test {@link Trade}.
 */
@Test
public class TradeTest {

  //-------------------------------------------------------------------------
  public void test_methods() {
    Trade test = sut();
    assertEquals(test.getId(), Optional.empty());
    assertEquals(test.getInfo(), TradeInfo.empty());
  }

  //-------------------------------------------------------------------------
  public void test_summarize() {
    Trade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.OTHER)
        .description("Unknown: MockTrade")
        .build();
    assertEquals(trade.summarize(), expected);
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
    public Trade withInfo(TradeInfo info) {
      return this;
    }
  }

}
