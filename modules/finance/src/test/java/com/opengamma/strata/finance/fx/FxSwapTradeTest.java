/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.fx;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.finance.TradeInfo;

/**
 * Test {@link FxSwapTrade}.
 */
@Test
public class FxSwapTradeTest {

  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final CurrencyAmount GBP_M1000 = CurrencyAmount.of(GBP, -1_000);
  private static final CurrencyAmount USD_P1550 = CurrencyAmount.of(USD, 1_550);
  private static final CurrencyAmount USD_M1600 = CurrencyAmount.of(USD, -1_600);
  private static final FxSingle NEAR_LEG = FxSingle.of(GBP_P1000, USD_M1600, date(2011, 11, 21));
  private static final FxSingle FAR_LEG = FxSingle.of(GBP_M1000, USD_P1550, date(2011, 12, 21));
  private static final FxSwap PRODUCT = FxSwap.of(NEAR_LEG, FAR_LEG);
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(date(2011, 11, 14)).build();

  //-------------------------------------------------------------------------
  public void test_builder() {
    FxSwapTrade test = FxSwapTrade.builder()
        .product(PRODUCT)
        .tradeInfo(TRADE_INFO)
        .build();
    assertEquals(test.getTradeInfo(), TRADE_INFO);
    assertEquals(test.getProduct(), PRODUCT);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxSwapTrade test1 = FxSwapTrade.builder()
        .product(PRODUCT)
        .tradeInfo(TRADE_INFO)
        .build();
    coverImmutableBean(test1);
    FxSwapTrade test2 = FxSwapTrade.builder()
        .product(FxSwap.ofForwardPoints(USD_M1600, GBP, 0.85, -0.05, date(2011, 11, 28), date(2011, 12, 28)))
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    FxSwapTrade test = FxSwapTrade.builder()
        .product(PRODUCT)
        .tradeInfo(TRADE_INFO)
        .build();
    assertSerialization(test);
  }

}
