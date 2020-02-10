/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link Position}.
 */
public class PositionTest {

  private static final StandardId STANDARD_ID = StandardId.of("A", "B");

  //-------------------------------------------------------------------------
  @Test
  public void test_methods() {
    Position test = sut();
    assertThat(test.getId()).isEqualTo(Optional.empty());
    assertThat(test.getInfo()).isEqualTo(PositionInfo.empty());
    assertThat(test.getQuantity()).isEqualTo(123d);
    assertThat(test.getSecurityId()).isEqualTo(SecurityId.of(STANDARD_ID));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    Position trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .portfolioItemType(PortfolioItemType.POSITION)
        .productType(ProductType.SECURITY)
        .description("B x 123")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
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
      public Position withInfo(PortfolioItemInfo info) {
        return this;
      }

      @Override
      public Position withQuantity(double quantity) {
        return this;
      }
    };
  }

}
