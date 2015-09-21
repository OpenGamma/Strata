/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.fx;

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
 * Test {@link FxNdf}.
 */
@Test
public class FxNdfTest {

  private static final FxRate FX_RATE = FxRate.of(GBP, USD, 1.5d);
  private static final double NOTIONAL = 100_000_000;
  private static final CurrencyAmount CURRENCY_NOTIONAL = CurrencyAmount.of(GBP, NOTIONAL);
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 3, 19);

  public void test_builder() {
    FxNdf test = FxNdf.builder()
        .agreedFxRate(FX_RATE)
        .index(WM_GBP_USD)
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .paymentDate(PAYMENT_DATE)
        .build();
    assertEquals(test.getAgreedFxRate(), FX_RATE);
    assertEquals(test.getIndex(), WM_GBP_USD);
    assertEquals(test.getNonDeliverableCurrency(), USD);
    assertEquals(test.getSettlementCurrencyNotional(), CURRENCY_NOTIONAL);
    assertEquals(test.getPaymentDate(), PAYMENT_DATE);
    assertEquals(test.getSettlementCurrency(), GBP);
  }

  public void test_builder_inverse() {
    FxRate fxRate = FxRate.of(USD, GBP, 0.7d);
    FxNdf test = FxNdf.builder()
        .agreedFxRate(fxRate)
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .index(WM_GBP_USD)
        .paymentDate(PAYMENT_DATE)
        .build();
    assertEquals(test.getAgreedFxRate(), fxRate);
    assertEquals(test.getIndex(), WM_GBP_USD);
    assertEquals(test.getNonDeliverableCurrency(), USD);
    assertEquals(test.getSettlementCurrencyNotional(), CURRENCY_NOTIONAL);
    assertEquals(test.getPaymentDate(), PAYMENT_DATE);
    assertEquals(test.getSettlementCurrency(), GBP);
  }

  public void test_builder_wrongRate() {
    FxRate fxRate = FxRate.of(GBP, EUR, 1.1d);
    assertThrowsIllegalArg(() -> FxNdf.builder()
        .agreedFxRate(fxRate)
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .index(WM_GBP_USD)
        .paymentDate(PAYMENT_DATE)
        .build());
  }

  public void test_builder_wrongCurrency() {
    assertThrowsIllegalArg(() -> FxNdf.builder()
        .agreedFxRate(FX_RATE)
        .settlementCurrencyNotional(CurrencyAmount.of(EUR, NOTIONAL))
        .index(WM_GBP_USD)
        .paymentDate(PAYMENT_DATE)
        .build());
  }

  public void test_expand() {
    FxNdf base = FxNdf.builder()
        .agreedFxRate(FX_RATE)
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .index(WM_GBP_USD)
        .paymentDate(PAYMENT_DATE)
        .build();
    ExpandedFxNdf expanded = base.expand();
    assertEquals(expanded.getAgreedFxRate(), FX_RATE);
    assertEquals(expanded.getIndex(), WM_GBP_USD);
    assertEquals(expanded.getNonDeliverableCurrency(), USD);
    assertEquals(expanded.getPaymentDate(), PAYMENT_DATE);
    assertEquals(expanded.getSettlementCurrency(), GBP);
    assertEquals(expanded.getSettlementCurrencyNotional(), CURRENCY_NOTIONAL);
    assertEquals(expanded.getSettlementNotional(), NOTIONAL);
  }

  public void coverage() {
    FxNdf test1 = FxNdf.builder()
        .agreedFxRate(FX_RATE)
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .index(WM_GBP_USD)
        .paymentDate(PAYMENT_DATE)
        .build();
    coverImmutableBean(test1);
    FxNdf test2 = FxNdf.builder()
        .agreedFxRate(FX_RATE)
        .settlementCurrencyNotional(CurrencyAmount.of(USD, -NOTIONAL))
        .index(WM_GBP_USD)
        .paymentDate(PAYMENT_DATE)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    FxNdf test = FxNdf.builder()
        .agreedFxRate(FX_RATE)
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .index(WM_GBP_USD)
        .paymentDate(PAYMENT_DATE)
        .build();
    assertSerialization(test);
  }

}
