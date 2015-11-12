/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.product.TradeInfo;

/**
 * Test {@link FxNdfTrade}.
 */
@Test
public class FxNdfTradeTest {

  private static final FxRate FX_RATE = FxRate.of(GBP, USD, 1.5d);
  private static final double NOTIONAL = 100_000_000;
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 3, 19);
  private static final CurrencyAmount CURRENCY_NOTIONAL = CurrencyAmount.of(GBP, NOTIONAL);
  private static final FxNdf PRODUCT = FxNdf.builder()
      .agreedFxRate(FX_RATE)
      .settlementCurrencyNotional(CURRENCY_NOTIONAL)
      .index(GBP_USD_WM)
      .paymentDate(PAYMENT_DATE)
      .build();
  private static final TradeInfo TRADE_INFO = TradeInfo.builder().tradeDate(date(2015, 1, 15)).build();

  public void test_builder() {
    FxNdfTrade test = FxNdfTrade.builder()
        .product(PRODUCT)
        .tradeInfo(TRADE_INFO)
        .build();
    assertEquals(test.getProduct(), PRODUCT);
    assertEquals(test.getTradeInfo(), TRADE_INFO);
  }

  public void coverage() {
    FxNdfTrade test1 = FxNdfTrade.builder()
        .product(PRODUCT)
        .tradeInfo(TRADE_INFO)
        .build();
    coverImmutableBean(test1);
    FxNdfTrade test2 = FxNdfTrade.builder()
        .product(PRODUCT)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    FxNdfTrade test = FxNdfTrade.builder()
        .product(PRODUCT)
        .tradeInfo(TRADE_INFO)
        .build();
    assertSerialization(test);
  }

}
