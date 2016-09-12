/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;

/**
 * Test {@link FxNdf}.
 */
@Test
public class FxNdfTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final FxRate FX_RATE = FxRate.of(GBP, USD, 1.5d);
  private static final double NOTIONAL = 100_000_000;
  private static final CurrencyAmount CURRENCY_NOTIONAL = CurrencyAmount.of(GBP, NOTIONAL);
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 3, 19);

  //-------------------------------------------------------------------------
  public void test_builder() {
    FxNdf test = FxNdf.builder()
        .agreedFxRate(FX_RATE)
        .index(GBP_USD_WM)
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .paymentDate(PAYMENT_DATE)
        .build();
    assertEquals(test.getAgreedFxRate(), FX_RATE);
    assertEquals(test.getIndex(), GBP_USD_WM);
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
        .index(GBP_USD_WM)
        .paymentDate(PAYMENT_DATE)
        .build();
    assertEquals(test.getAgreedFxRate(), fxRate);
    assertEquals(test.getIndex(), GBP_USD_WM);
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
        .index(GBP_USD_WM)
        .paymentDate(PAYMENT_DATE)
        .build());
  }

  public void test_builder_wrongCurrency() {
    assertThrowsIllegalArg(() -> FxNdf.builder()
        .agreedFxRate(FX_RATE)
        .settlementCurrencyNotional(CurrencyAmount.of(EUR, NOTIONAL))
        .index(GBP_USD_WM)
        .paymentDate(PAYMENT_DATE)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    FxNdf base = sut();
    ResolvedFxNdf resolved = base.resolve(REF_DATA);
    assertEquals(resolved.getAgreedFxRate(), FX_RATE);
    assertEquals(resolved.getIndex(), GBP_USD_WM);
    assertEquals(resolved.getNonDeliverableCurrency(), USD);
    assertEquals(resolved.getPaymentDate(), PAYMENT_DATE);
    assertEquals(resolved.getSettlementCurrency(), GBP);
    assertEquals(resolved.getSettlementCurrencyNotional(), CURRENCY_NOTIONAL);
    assertEquals(resolved.getSettlementNotional(), NOTIONAL);
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
  static FxNdf sut() {
    return FxNdf.builder()
        .agreedFxRate(FX_RATE)
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .index(GBP_USD_WM)
        .paymentDate(PAYMENT_DATE)
        .build();
  }

  FxNdf sut2() {
    return FxNdf.builder()
        .agreedFxRate(FX_RATE)
        .settlementCurrencyNotional(CurrencyAmount.of(USD, -NOTIONAL))
        .index(GBP_USD_WM)
        .paymentDate(PAYMENT_DATE)
        .build();
  }

}
