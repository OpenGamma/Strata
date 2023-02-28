/*
 * Copyright (C) 2023 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.StandardSchemes;

/**
 * Test for {@link PortfolioItem}.
 */
public class PortfolioItemTest {

  private class TestPortfolioItem implements PortfolioItem {

    private final PortfolioItemInfo info;

    TestPortfolioItem(PortfolioItemInfo info) {
      this.info = info;
    }

    @Override
    public PortfolioItemInfo getInfo() {
      return info;
    }

    @Override
    public PortfolioItemSummary summarize() {
      return PortfolioItemSummary.of(
          getId().orElse(StandardId.of(StandardSchemes.OG_TRADE_SCHEME, "TestTrade")),
          PortfolioItemType.POSITION,
          ProductType.OTHER,
          ImmutableSet.of(),
          "Test Implementation");
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_getMissingAttribute() {
    TestPortfolioItem test = new TestPortfolioItem(PortfolioItemInfo.empty());
    assertThatIllegalArgumentException().isThrownBy(() -> test.getAttribute(AttributeType.NAME));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_getAttribute() {
    TestPortfolioItem test = new TestPortfolioItem(PortfolioItemInfo.of(AttributeType.NAME, "Test"));
    assertThat(test.getAttribute(AttributeType.NAME)).isEqualTo("Test");
  }

}
