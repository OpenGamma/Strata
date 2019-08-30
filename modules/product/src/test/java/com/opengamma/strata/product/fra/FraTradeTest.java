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
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link FraTrade}.
 */
public class FraTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final Fra PRODUCT = FraTest.sut();
  private static final Fra PRODUCT2 = FraTest.sut2();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2015, 3, 15));

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    FraTrade test = FraTrade.of(TRADE_INFO, PRODUCT);
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.withInfo(TRADE_INFO).getInfo()).isEqualTo(TRADE_INFO);
  }

  @Test
  public void test_builder() {
    FraTrade test = FraTrade.builder()
        .product(PRODUCT)
        .build();
    assertThat(test.getInfo()).isEqualTo(TradeInfo.empty());
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    FraTrade test = FraTrade.of(TRADE_INFO, PRODUCT);
    assertThat(test.resolve(REF_DATA).getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.resolve(REF_DATA).getProduct()).isEqualTo(PRODUCT.resolve(REF_DATA));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    FraTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.FRA)
        .currencies(Currency.GBP)
        .description("3x6 GBP 1mm Rec GBP-LIBOR / Pay 2.5% : 15Jun15-15Sep15")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
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
