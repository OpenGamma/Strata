/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;

/**
 * Test {@link PortfolioItemSummary}.
 */
@Test
public class PortfolioItemSummaryTest {

  private static final StandardId STANDARD_ID = StandardId.of("A", "B");

  //-------------------------------------------------------------------------
  public void test_of() {
    assertEquals(sut().getId(), Optional.of(STANDARD_ID));
    assertEquals(sut2().getId(), Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static PortfolioItemSummary sut() {
    return PortfolioItemSummary.builder()
        .id(STANDARD_ID)
        .portfolioItemType(PortfolioItemType.POSITION)
        .productType(ProductType.SECURITY)
        .currencies(Currency.GBP)
        .description("One")
        .build();
  }

  static PortfolioItemSummary sut2() {
    return PortfolioItemSummary.builder()
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.FRA)
        .currencies(Currency.USD)
        .description("Two")
        .build();
  }

}
