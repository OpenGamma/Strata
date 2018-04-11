/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fra;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link FraTrade}.
 */
@Test
public class FraTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final Fra PRODUCT = FraTest.sut();
  private static final Fra PRODUCT2 = FraTest.sut2();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2015, 3, 15));

  //-------------------------------------------------------------------------
  public void test_of() {
    FraTrade test = FraTrade.of(TRADE_INFO, PRODUCT);
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.withInfo(TRADE_INFO).getInfo(), TRADE_INFO);
  }

  public void test_builder() {
    FraTrade test = FraTrade.builder()
        .product(PRODUCT)
        .build();
    assertEquals(test.getInfo(), TradeInfo.empty());
    assertEquals(test.getProduct(), PRODUCT);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    FraTrade test = FraTrade.of(TRADE_INFO, PRODUCT);
    assertEquals(test.resolve(REF_DATA).getInfo(), TRADE_INFO);
    assertEquals(test.resolve(REF_DATA).getProduct(), PRODUCT.resolve(REF_DATA));
  }

  //-------------------------------------------------------------------------
  public void test_summarize() {
    FraTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.FRA)
        .currencies(Currency.GBP)
        .description("3x6 GBP 1mm Rec GBP-LIBOR / Pay 2.5% : 15Jun15-15Sep15")
        .build();
    assertEquals(trade.summarize(), expected);
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
  static FraTrade sut() {
    return FraTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .build();
  }

  static FraTrade sut2() {
    return FraTrade.builder()
        .product(PRODUCT2)
        .build();
  }

}
