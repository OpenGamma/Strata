/*
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
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.FxRate;

/**
 * Test {@link FxNdf}.
 */
public class FxNdfTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final FxRate FX_RATE = FxRate.of(GBP, USD, 1.5d);
  private static final double NOTIONAL = 100_000_000;
  private static final CurrencyAmount CURRENCY_NOTIONAL = CurrencyAmount.of(GBP, NOTIONAL);
  private static final LocalDate PAYMENT_DATE = LocalDate.of(2015, 3, 19);

  //-------------------------------------------------------------------------
  @Test
  public void test_builder() {
    FxNdf test = FxNdf.builder()
        .agreedFxRate(FX_RATE)
        .index(GBP_USD_WM)
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .paymentDate(PAYMENT_DATE)
        .build();
    assertThat(test.getAgreedFxRate()).isEqualTo(FX_RATE);
    assertThat(test.getIndex()).isEqualTo(GBP_USD_WM);
    assertThat(test.getNonDeliverableCurrency()).isEqualTo(USD);
    assertThat(test.getSettlementCurrencyNotional()).isEqualTo(CURRENCY_NOTIONAL);
    assertThat(test.getPaymentDate()).isEqualTo(PAYMENT_DATE);
    assertThat(test.getSettlementCurrency()).isEqualTo(GBP);
    assertThat(test.isCrossCurrency()).isTrue();
    assertThat(test.allPaymentCurrencies()).containsOnly(GBP);
    assertThat(test.allCurrencies()).containsOnly(GBP, USD);
  }

  @Test
  public void test_builder_inverse() {
    FxRate fxRate = FxRate.of(USD, GBP, 0.7d);
    FxNdf test = FxNdf.builder()
        .agreedFxRate(fxRate)
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .index(GBP_USD_WM)
        .paymentDate(PAYMENT_DATE)
        .build();
    assertThat(test.getAgreedFxRate()).isEqualTo(fxRate);
    assertThat(test.getIndex()).isEqualTo(GBP_USD_WM);
    assertThat(test.getNonDeliverableCurrency()).isEqualTo(USD);
    assertThat(test.getSettlementCurrencyNotional()).isEqualTo(CURRENCY_NOTIONAL);
    assertThat(test.getPaymentDate()).isEqualTo(PAYMENT_DATE);
    assertThat(test.getSettlementCurrency()).isEqualTo(GBP);
  }

  @Test
  public void test_builder_wrongRate() {
    FxRate fxRate = FxRate.of(GBP, EUR, 1.1d);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxNdf.builder()
            .agreedFxRate(fxRate)
            .settlementCurrencyNotional(CURRENCY_NOTIONAL)
            .index(GBP_USD_WM)
            .paymentDate(PAYMENT_DATE)
            .build());
  }

  @Test
  public void test_builder_wrongCurrency() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxNdf.builder()
            .agreedFxRate(FX_RATE)
            .settlementCurrencyNotional(CurrencyAmount.of(EUR, NOTIONAL))
            .index(GBP_USD_WM)
            .paymentDate(PAYMENT_DATE)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve() {
    FxNdf base = sut();
    ResolvedFxNdf resolved = base.resolve(REF_DATA);
    assertThat(resolved.getAgreedFxRate()).isEqualTo(FX_RATE);
    assertThat(resolved.getIndex()).isEqualTo(GBP_USD_WM);
    assertThat(resolved.getNonDeliverableCurrency()).isEqualTo(USD);
    assertThat(resolved.getPaymentDate()).isEqualTo(PAYMENT_DATE);
    assertThat(resolved.getSettlementCurrency()).isEqualTo(GBP);
    assertThat(resolved.getSettlementCurrencyNotional()).isEqualTo(CURRENCY_NOTIONAL);
    assertThat(resolved.getSettlementNotional()).isEqualTo(NOTIONAL);
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
  static FxNdf sut() {
    return FxNdf.builder()
        .agreedFxRate(FX_RATE)
        .settlementCurrencyNotional(CURRENCY_NOTIONAL)
        .index(GBP_USD_WM)
        .paymentDate(PAYMENT_DATE)
        .build();
  }

  static FxNdf sut2() {
    return FxNdf.builder()
        .agreedFxRate(FX_RATE)
        .settlementCurrencyNotional(CurrencyAmount.of(USD, -NOTIONAL))
        .index(GBP_USD_WM)
        .paymentDate(PAYMENT_DATE)
        .build();
  }

}
