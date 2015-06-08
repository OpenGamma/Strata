/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.fx;

import static com.opengamma.strata.basics.BuySell.BUY;
import static com.opengamma.strata.basics.BuySell.SELL;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.FxIndices.WM_GBP_USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;

/**
 * Test {@link FxNonDeliverableForward}.
 */
@Test
public class FxNonDeliverableForwardTest {
  private static final FxRate FX_RATE = FxRate.of(GBP, USD, 1.5d);
  private static final double NOTIONAL = 100_000_000;
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 3, 19);

  public void test_builder() {
    FxNonDeliverableForward test = FxNonDeliverableForward.builder()
        .agreedFxRate(FX_RATE)
        .buySell(SELL)
        .index(WM_GBP_USD)
        .notional(NOTIONAL)
        .paymentDate(PAYMENT_DATE)
        .settlementCurrency(GBP)
        .build();
    assertEquals(test.getAgreedFxRate(), FX_RATE);
    assertEquals(test.getBuySell(), SELL);
    assertEquals(test.getIndex(), WM_GBP_USD);
    assertEquals(test.getNonDeliverableCurrency(), USD);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getPaymentDate(), PAYMENT_DATE);
    assertEquals(test.getSettlementCurrency(), GBP);
  }

  public void test_builder_inverse() {
    FxRate fxRate = FxRate.of(USD, GBP, 0.7d);
    FxNonDeliverableForward test = FxNonDeliverableForward.builder()
        .agreedFxRate(fxRate)
        .buySell(SELL)
        .index(WM_GBP_USD)
        .notional(NOTIONAL)
        .paymentDate(PAYMENT_DATE)
        .settlementCurrency(GBP)
        .build();
    assertEquals(test.getAgreedFxRate(), fxRate);
    assertEquals(test.getBuySell(), SELL);
    assertEquals(test.getIndex(), WM_GBP_USD);
    assertEquals(test.getNonDeliverableCurrency(), USD);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getPaymentDate(), PAYMENT_DATE);
    assertEquals(test.getSettlementCurrency(), GBP);
  }

  public void test_builder_wrongRate() {
    FxRate fxRate = FxRate.of(GBP, EUR, 1.1d);
    assertThrowsIllegalArg(() -> FxNonDeliverableForward.builder()
        .agreedFxRate(fxRate)
        .buySell(SELL)
        .index(WM_GBP_USD)
        .notional(NOTIONAL)
        .paymentDate(PAYMENT_DATE)
        .settlementCurrency(GBP)
        .build());
  }

  public void test_builder_wrongCurrency() {
    assertThrowsIllegalArg(() -> FxNonDeliverableForward.builder()
        .agreedFxRate(FX_RATE)
        .buySell(SELL)
        .index(WM_GBP_USD)
        .notional(NOTIONAL)
        .paymentDate(PAYMENT_DATE)
        .settlementCurrency(EUR)
        .build());
  }

  public void test_expand() {
    FxNonDeliverableForward base = FxNonDeliverableForward.builder()
        .agreedFxRate(FX_RATE)
        .buySell(SELL)
        .index(WM_GBP_USD)
        .notional(NOTIONAL)
        .paymentDate(PAYMENT_DATE)
        .settlementCurrency(GBP)
        .build();
    ExpandedFxNonDeliverableForward expanded = base.expand();
    assertEquals(expanded.getAgreedFxRate(), FX_RATE);
    assertEquals(expanded.getIndex(), WM_GBP_USD);
    assertEquals(expanded.getNonDeliverableCurrency(), USD);
    assertEquals(expanded.getPaymentDate(), PAYMENT_DATE);
    assertEquals(expanded.getSettlementCurrency(), GBP);
    assertEquals(expanded.getSettlementCurrencyNotional(), CurrencyAmount.of(GBP, -NOTIONAL));
    assertEquals(expanded.getSettlementNotional(), -NOTIONAL);
  }

  public void coverage() {
    FxNonDeliverableForward test1 = FxNonDeliverableForward.builder()
        .agreedFxRate(FX_RATE)
        .buySell(SELL)
        .index(WM_GBP_USD)
        .notional(NOTIONAL)
        .paymentDate(PAYMENT_DATE)
        .settlementCurrency(GBP)
        .build();
    coverImmutableBean(test1);
    FxNonDeliverableForward test2 = FxNonDeliverableForward.builder()
        .agreedFxRate(FX_RATE)
        .buySell(BUY)
        .index(WM_GBP_USD)
        .notional(NOTIONAL)
        .paymentDate(PAYMENT_DATE)
        .settlementCurrency(USD)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    FxNonDeliverableForward test = FxNonDeliverableForward.builder()
        .agreedFxRate(FX_RATE)
        .buySell(SELL)
        .index(WM_GBP_USD)
        .notional(NOTIONAL)
        .paymentDate(PAYMENT_DATE)
        .settlementCurrency(GBP)
        .build();
    assertSerialization(test);
  }
}
