/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swaption;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@code SwaptionTrade}.
 */
public class SwaptionTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final Swaption SWAPTION = SwaptionTest.sut();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2014, 3, 14));
  private static final AdjustablePayment PREMIUM =
      AdjustablePayment.of(CurrencyAmount.of(Currency.USD, -3150000d), date(2014, 3, 17));

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    SwaptionTrade test = SwaptionTrade.of(TRADE_INFO, SWAPTION, PREMIUM);
    assertThat(test.getPremium()).isEqualTo(PREMIUM);
    assertThat(test.getProduct()).isEqualTo(SWAPTION);
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.withInfo(TRADE_INFO).getInfo()).isEqualTo(TRADE_INFO);
  }

  @Test
  public void test_builder() {
    SwaptionTrade test = sut();
    assertThat(test.getPremium()).isEqualTo(PREMIUM);
    assertThat(test.getProduct()).isEqualTo(SWAPTION);
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    SwaptionTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .id(TRADE_INFO.getId().orElse(null))
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.SWAPTION)
        .currencies(Currency.USD)
        .description("Long 10Y USD 100mm Rec USD-LIBOR-3M / Pay 1.5% : 10Jun14")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    SwaptionTrade test = SwaptionTrade.of(TRADE_INFO, SWAPTION, PREMIUM);
    assertThat(test.resolve(REF_DATA).getPremium()).isEqualTo(PREMIUM.resolve(REF_DATA));
    assertThat(test.resolve(REF_DATA).getProduct()).isEqualTo(SWAPTION.resolve(REF_DATA));
    assertThat(test.resolve(REF_DATA).getInfo()).isEqualTo(TRADE_INFO);
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
  static SwaptionTrade sut() {
    return SwaptionTrade.builder()
        .premium(PREMIUM)
        .product(SWAPTION)
        .info(TRADE_INFO)
        .build();
  }

  static SwaptionTrade sut2() {
    return SwaptionTrade.builder()
        .premium(AdjustablePayment.of(CurrencyAmount.of(Currency.USD, -3050000d), LocalDate.of(2014, 3, 17)))
        .product(SwaptionTest.sut2())
        .build();
  }

}
