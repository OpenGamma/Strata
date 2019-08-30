/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

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
 * Test {@link FxVanillaOptionTrade}.
 */
public class FxVanillaOptionTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final double NOTIONAL = 1.0e6;
  private static final FxVanillaOption PRODUCT = FxVanillaOptionTest.sut();
  private static final FxVanillaOption PRODUCT2 = FxVanillaOptionTest.sut2();
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2014, 11, 12));
  private static final AdjustablePayment PREMIUM =
      AdjustablePayment.of(CurrencyAmount.of(EUR, NOTIONAL * 0.05), date(2014, 11, 14));

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    FxVanillaOptionTrade test = sut();
    assertThat(test.getProduct()).isEqualTo(PRODUCT);
    assertThat(test.getProduct().getCurrencyPair()).isEqualTo(PRODUCT.getCurrencyPair());
    assertThat(test.getInfo()).isEqualTo(TRADE_INFO);
    assertThat(test.getPremium()).isEqualTo(PREMIUM);
    assertThat(test.withInfo(TRADE_INFO).getInfo()).isEqualTo(TRADE_INFO);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_summarize() {
    FxVanillaOptionTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.FX_VANILLA_OPTION)
        .currencies(Currency.USD, Currency.EUR)
        .description("Long Rec EUR 1mm @ EUR/USD 1.35 Premium EUR 50k : 14Feb15")
        .build();
    assertThat(trade.summarize()).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    FxVanillaOptionTrade test = sut();
    ResolvedFxVanillaOptionTrade expected = ResolvedFxVanillaOptionTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT.resolve(REF_DATA))
        .premium(PREMIUM.resolve(REF_DATA))
        .build();
    assertThat(test.resolve(REF_DATA)).isEqualTo(expected);
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
  static FxVanillaOptionTrade sut() {
    return FxVanillaOptionTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .premium(PREMIUM)
        .build();
  }

  static FxVanillaOptionTrade sut2() {
    AdjustablePayment premium = AdjustablePayment.of(CurrencyAmount.of(EUR, NOTIONAL * 0.01), date(2014, 11, 13));
    return FxVanillaOptionTrade.builder()
        .product(PRODUCT2)
        .premium(premium)
        .build();
  }

}
