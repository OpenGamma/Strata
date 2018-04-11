/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link Position}.
 */
@Test
public class PositionTest {

  private static final StandardId STANDARD_ID = StandardId.of("A", "B");

  //-------------------------------------------------------------------------
  public void test_methods() {
    Position test = sut();
    assertEquals(test.getId(), Optional.empty());
    assertEquals(test.getInfo(), PositionInfo.empty());
    assertEquals(test.getQuantity(), 123d);
    assertEquals(test.getSecurityId(), SecurityId.of(STANDARD_ID));
  }

  //-------------------------------------------------------------------------
  public void test_summarize() {
    Position trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .portfolioItemType(PortfolioItemType.POSITION)
        .productType(ProductType.SECURITY)
        .description("B x 123")
        .build();
    assertEquals(trade.summarize(), expected);
  }

  //-------------------------------------------------------------------------
  static Position sut() {
    return new Position() {

      @Override
      public SecurityId getSecurityId() {
        return SecurityId.of(STANDARD_ID);
      }

      @Override
      public double getQuantity() {
        return 123d;
      }

      @Override
      public PositionInfo getInfo() {
        return PositionInfo.empty();
      }

      @Override
      public Position withInfo(PositionInfo info) {
        return this;
      }

      @Override
      public Position withQuantity(double quantity) {
        return this;
      }
    };
  }

}
