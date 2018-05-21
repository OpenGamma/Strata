/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fxopt;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.product.PortfolioItemSummary;
import com.opengamma.strata.product.PortfolioItemType;
import com.opengamma.strata.product.ProductType;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.LongShort;
import com.opengamma.strata.product.fx.FxSingle;
import com.opengamma.strata.product.option.BarrierType;
import com.opengamma.strata.product.option.KnockType;
import com.opengamma.strata.product.option.SimpleConstantContinuousBarrier;

/**
 * Test {@link FxSingleBarrierOptionTrade}.
 */
@Test
public class FxSingleBarrierOptionTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate EXPIRY_DATE = LocalDate.of(2015, 2, 14);
  private static final LocalTime EXPIRY_TIME = LocalTime.of(12, 15);
  private static final ZoneId EXPIRY_ZONE = ZoneId.of("Z");
  private static final LongShort LONG = LongShort.LONG;
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 2, 16);
  private static final double NOTIONAL = 1.0e6;
  private static final CurrencyAmount EUR_AMOUNT = CurrencyAmount.of(EUR, NOTIONAL);
  private static final CurrencyAmount USD_AMOUNT = CurrencyAmount.of(USD, -NOTIONAL * 1.35);
  private static final FxSingle FX = FxSingle.of(EUR_AMOUNT, USD_AMOUNT, PAYMENT_DATE);
  private static final FxVanillaOption VANILLA_OPTION = FxVanillaOption.builder()
      .longShort(LONG)
      .expiryDate(EXPIRY_DATE)
      .expiryTime(EXPIRY_TIME)
      .expiryZone(EXPIRY_ZONE)
      .underlying(FX)
      .build();
  private static final SimpleConstantContinuousBarrier BARRIER =
      SimpleConstantContinuousBarrier.of(BarrierType.DOWN, KnockType.KNOCK_IN, 1.2);
  private static final CurrencyAmount REBATE = CurrencyAmount.of(USD, 5.0e4);
  private static final FxSingleBarrierOption PRODUCT = FxSingleBarrierOption.of(VANILLA_OPTION, BARRIER, REBATE);
  private static final TradeInfo TRADE_INFO = TradeInfo.of(date(2014, 11, 12));
  private static final AdjustablePayment PREMIUM =
      AdjustablePayment.of(CurrencyAmount.of(EUR, NOTIONAL * 0.05), date(2014, 11, 14));

  //-------------------------------------------------------------------------
  public void test_builder() {
    FxSingleBarrierOptionTrade test = sut();
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getProduct().getCurrencyPair(), PRODUCT.getCurrencyPair());
    assertEquals(test.getInfo(), TRADE_INFO);
    assertEquals(test.getPremium(), PREMIUM);
    assertEquals(test.withInfo(TRADE_INFO).getInfo(), TRADE_INFO);
  }

  //-------------------------------------------------------------------------
  public void test_summarize() {
    FxSingleBarrierOptionTrade trade = sut();
    PortfolioItemSummary expected = PortfolioItemSummary.builder()
        .portfolioItemType(PortfolioItemType.TRADE)
        .productType(ProductType.FX_SINGLE_BARRIER_OPTION)
        .currencies(Currency.USD, Currency.EUR)
        .description("Long Barrier Rec EUR 1mm @ EUR/USD 1.35 Premium EUR 50k : 14Feb15")
        .build();
    assertEquals(trade.summarize(), expected);
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    FxSingleBarrierOptionTrade base = sut();
    ResolvedFxSingleBarrierOptionTrade expected = ResolvedFxSingleBarrierOptionTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT.resolve(REF_DATA))
        .premium(PREMIUM.resolve(REF_DATA))
        .build();
    assertEquals(base.resolve(REF_DATA), expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxSingleBarrierOptionTrade test1 = sut();
    FxSingleBarrierOptionTrade test2 = FxSingleBarrierOptionTrade.builder()
        .product(FxSingleBarrierOption.of(VANILLA_OPTION, BARRIER))
        .premium(AdjustablePayment.of(CurrencyAmount.of(EUR, NOTIONAL * 0.01), date(2014, 11, 13)))
        .build();
    coverImmutableBean(test1);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    FxSingleBarrierOptionTrade test = sut();
    assertSerialization(test);
  }

  //-------------------------------------------------------------------------
  static FxSingleBarrierOptionTrade sut() {
    return FxSingleBarrierOptionTrade.builder()
        .info(TRADE_INFO)
        .product(PRODUCT)
        .premium(PREMIUM)
        .build();
  }

}
