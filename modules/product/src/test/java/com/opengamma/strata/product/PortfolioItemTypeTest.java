/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link PortfolioItemType}.
 */
public class PortfolioItemTypeTest {

  //-------------------------------------------------------------------------
  @Test
  public void test_constants() {
    assertThat(PortfolioItemType.POSITION.getName()).isEqualTo("Position");
    assertThat(PortfolioItemType.TRADE.getName()).isEqualTo("Trade");
    assertThat(PortfolioItemType.OTHER.getName()).isEqualTo("Other");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    assertThat(PortfolioItemType.of("Position")).isEqualTo(PortfolioItemType.POSITION);
    assertThat(PortfolioItemType.of("position")).isEqualTo(PortfolioItemType.POSITION);
    assertThat(PortfolioItemType.of("POSITION")).isEqualTo(PortfolioItemType.POSITION);
  }

}
