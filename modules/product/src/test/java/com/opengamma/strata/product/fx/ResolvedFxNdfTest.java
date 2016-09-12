/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.FxIndices.EUR_GBP_ECB;
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
import com.opengamma.strata.basics.index.FxIndexObservation;

/**
 * Test {@link ResolvedFxNdf}.
 */
@Test
public class ResolvedFxNdfTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final FxRate FX_RATE = FxRate.of(GBP, USD, 1.5d);
  private static final double NOTIONAL = 100_000_000;
  private static final CurrencyAmount CURRENCY_NOTIONAL = CurrencyAmount.of(GBP, NOTIONAL);
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 3, 19);
  private static final LocalDate FIXING_DATE = LocalDate.of(2015, 3, 17);

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedFxNdf test = sut();
    assertEquals(test.getAgreedFxRate(), FX_RATE);
    assertEquals(test.getIndex(), GBP_USD_WM);
    assertEquals(test.getNonDeliverableCurrency(), USD);
    assertEquals(test.getPaymentDate(), PAYMENT_DATE);
    assertEquals(test.getSettlementCurrency(), GBP);
    assertEquals(test.getSettlementCurrencyNotional(), CURRENCY_NOTIONAL);
    assertEquals(test.getSettlementNotional(), NOTIONAL);
  }

  public void test_builder_inverse() {
    CurrencyAmount currencyNotional = CurrencyAmount.of(USD, NOTIONAL);
    ResolvedFxNdf test = ResolvedFxNdf.builder()
        .agreedFxRate(FX_RATE)
        .observation(FxIndexObservation.of(GBP_USD_WM, FIXING_DATE, REF_DATA))
        .paymentDate(PAYMENT_DATE)
        .settlementCurrencyNotional(currencyNotional)
        .build();
    assertEquals(test.getAgreedFxRate(), FX_RATE);
    assertEquals(test.getIndex(), GBP_USD_WM);
    assertEquals(test.getNonDeliverableCurrency(), GBP);
    assertEquals(test.getPaymentDate(), PAYMENT_DATE);
    assertEquals(test.getSettlementCurrency(), USD);
    assertEquals(test.getSettlementCurrencyNotional(), currencyNotional);
    assertEquals(test.getSettlementNotional(), NOTIONAL);
  }

  public void test_builder_wrongCurrency() {
    CurrencyAmount currencyNotional = CurrencyAmount.of(EUR, NOTIONAL);
    assertThrowsIllegalArg(() -> ResolvedFxNdf.builder()
        .agreedFxRate(FX_RATE)
        .observation(FxIndexObservation.of(GBP_USD_WM, FIXING_DATE, REF_DATA))
        .paymentDate(PAYMENT_DATE)
        .settlementCurrencyNotional(currencyNotional)
        .build());
  }

  public void test_builder_wrongRate() {
    FxRate fxRate = FxRate.of(GBP, EUR, 1.1d);
    assertThrowsIllegalArg(() -> ResolvedFxNdf.builder()
        .agreedFxRate(fxRate)
        .observation(FxIndexObservation.of(GBP_USD_WM, FIXING_DATE, REF_DATA))
        .paymentDate(PAYMENT_DATE)
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .build());
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
  static ResolvedFxNdf sut() {
    return ResolvedFxNdf.builder()
        .agreedFxRate(FX_RATE)
        .observation(FxIndexObservation.of(GBP_USD_WM, FIXING_DATE, REF_DATA))
        .paymentDate(PAYMENT_DATE)
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .build();
  }

  static ResolvedFxNdf sut2() {
    FxRate fxRate = FxRate.of(GBP, EUR, 1.1d);
    return ResolvedFxNdf.builder()
        .agreedFxRate(fxRate)
        .observation(FxIndexObservation.of(EUR_GBP_ECB, FIXING_DATE, REF_DATA))
        .paymentDate(PAYMENT_DATE)
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .build();
  }

}
