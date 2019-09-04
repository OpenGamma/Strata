/*
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
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.index.FxIndexObservation;

/**
 * Test {@link ResolvedFxNdf}.
 */
public class ResolvedFxNdfTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final FxRate FX_RATE = FxRate.of(GBP, USD, 1.5d);
  private static final double NOTIONAL = 100_000_000;
  private static final CurrencyAmount CURRENCY_NOTIONAL = CurrencyAmount.of(GBP, NOTIONAL);
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 3, 19);
  private static final LocalDate FIXING_DATE = LocalDate.of(2015, 3, 17);

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    ResolvedFxNdf test = sut();
    assertThat(test.getAgreedFxRate()).isEqualTo(FX_RATE);
    assertThat(test.getIndex()).isEqualTo(GBP_USD_WM);
    assertThat(test.getNonDeliverableCurrency()).isEqualTo(USD);
    assertThat(test.getPaymentDate()).isEqualTo(PAYMENT_DATE);
    assertThat(test.getSettlementCurrency()).isEqualTo(GBP);
    assertThat(test.getSettlementCurrencyNotional()).isEqualTo(CURRENCY_NOTIONAL);
    assertThat(test.getSettlementNotional()).isEqualTo(NOTIONAL);
  }

  @Test
  public void test_builder_inverse() {
    CurrencyAmount currencyNotional = CurrencyAmount.of(USD, NOTIONAL);
    ResolvedFxNdf test = ResolvedFxNdf.builder()
        .agreedFxRate(FX_RATE)
        .observation(FxIndexObservation.of(GBP_USD_WM, FIXING_DATE, REF_DATA))
        .paymentDate(PAYMENT_DATE)
        .settlementCurrencyNotional(currencyNotional)
        .build();
    assertThat(test.getAgreedFxRate()).isEqualTo(FX_RATE);
    assertThat(test.getIndex()).isEqualTo(GBP_USD_WM);
    assertThat(test.getNonDeliverableCurrency()).isEqualTo(GBP);
    assertThat(test.getPaymentDate()).isEqualTo(PAYMENT_DATE);
    assertThat(test.getSettlementCurrency()).isEqualTo(USD);
    assertThat(test.getSettlementCurrencyNotional()).isEqualTo(currencyNotional);
    assertThat(test.getSettlementNotional()).isEqualTo(NOTIONAL);
  }

  @Test
  public void test_builder_wrongCurrency() {
    CurrencyAmount currencyNotional = CurrencyAmount.of(EUR, NOTIONAL);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedFxNdf.builder()
            .agreedFxRate(FX_RATE)
            .observation(FxIndexObservation.of(GBP_USD_WM, FIXING_DATE, REF_DATA))
            .paymentDate(PAYMENT_DATE)
            .settlementCurrencyNotional(currencyNotional)
            .build());
  }

  @Test
  public void test_builder_wrongRate() {
    FxRate fxRate = FxRate.of(GBP, EUR, 1.1d);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedFxNdf.builder()
            .agreedFxRate(fxRate)
            .observation(FxIndexObservation.of(GBP_USD_WM, FIXING_DATE, REF_DATA))
            .paymentDate(PAYMENT_DATE)
            .settlementCurrencyNotional(CURRENCY_NOTIONAL)
            .build());
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
