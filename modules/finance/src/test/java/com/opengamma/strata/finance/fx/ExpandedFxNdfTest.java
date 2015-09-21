/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.FxIndices.ECB_EUR_GBP;
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
 * Test {@link ExpandedFxNdf}.
 */
@Test
public class ExpandedFxNdfTest {

  private static final FxRate FX_RATE = FxRate.of(GBP, USD, 1.5d);
  private static final double NOTIONAL = 100_000_000;
  private static final CurrencyAmount CURRENCY_NOTIONAL = CurrencyAmount.of(GBP, NOTIONAL);
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 3, 19);

  public void test_builder() {
    ExpandedFxNdf test = ExpandedFxNdf.builder()
        .agreedFxRate(FX_RATE)
        .index(WM_GBP_USD)
        .paymentDate(PAYMENT_DATE)
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .build();
    assertEquals(test.getAgreedFxRate(), FX_RATE);
    assertEquals(test.getIndex(), WM_GBP_USD);
    assertEquals(test.getNonDeliverableCurrency(), USD);
    assertEquals(test.getPaymentDate(), PAYMENT_DATE);
    assertEquals(test.getSettlementCurrency(), GBP);
    assertEquals(test.getSettlementCurrencyNotional(), CURRENCY_NOTIONAL);
    assertEquals(test.getSettlementNotional(), NOTIONAL);
  }

  public void test_builder_inverse() {
    CurrencyAmount currencyNotional = CurrencyAmount.of(USD, NOTIONAL);
    ExpandedFxNdf test = ExpandedFxNdf.builder()
        .agreedFxRate(FX_RATE)
        .index(WM_GBP_USD)
        .paymentDate(PAYMENT_DATE)
        .settlementCurrencyNotional(currencyNotional)
        .build();
    assertEquals(test.getAgreedFxRate(), FX_RATE);
    assertEquals(test.getIndex(), WM_GBP_USD);
    assertEquals(test.getNonDeliverableCurrency(), GBP);
    assertEquals(test.getPaymentDate(), PAYMENT_DATE);
    assertEquals(test.getSettlementCurrency(), USD);
    assertEquals(test.getSettlementCurrencyNotional(), currencyNotional);
    assertEquals(test.getSettlementNotional(), NOTIONAL);
  }

  public void test_builder_wrongCurrency() {
    CurrencyAmount currencyNotional = CurrencyAmount.of(EUR, NOTIONAL);
    assertThrowsIllegalArg(() -> ExpandedFxNdf.builder()
        .agreedFxRate(FX_RATE)
        .index(WM_GBP_USD)
        .paymentDate(PAYMENT_DATE)
        .settlementCurrencyNotional(currencyNotional)
        .build());
  }

  public void test_builder_wrongRate() {
    FxRate fxRate = FxRate.of(GBP, EUR, 1.1d);
    assertThrowsIllegalArg(() -> ExpandedFxNdf.builder()
        .agreedFxRate(fxRate)
        .index(WM_GBP_USD)
        .paymentDate(PAYMENT_DATE)
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .build());
  }

  public void test_expand() {
    ExpandedFxNdf base = ExpandedFxNdf.builder()
        .agreedFxRate(FX_RATE)
        .index(WM_GBP_USD)
        .paymentDate(PAYMENT_DATE)
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .build();
    ExpandedFxNdf test = base.expand();
    assertEquals(test, base);
  }

  public void coverage() {
    ExpandedFxNdf test1 = ExpandedFxNdf.builder()
        .agreedFxRate(FX_RATE)
        .index(WM_GBP_USD)
        .paymentDate(PAYMENT_DATE)
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .build();
    coverImmutableBean(test1);
    FxRate fxRate = FxRate.of(GBP, EUR, 1.1d);
    ExpandedFxNdf test2 = ExpandedFxNdf.builder()
        .agreedFxRate(fxRate)
        .index(ECB_EUR_GBP)
        .paymentDate(PAYMENT_DATE)
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ExpandedFxNdf test = ExpandedFxNdf.builder()
        .agreedFxRate(FX_RATE)
        .index(WM_GBP_USD)
        .paymentDate(PAYMENT_DATE)
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .build();
    assertSerialization(test);
  }

}
