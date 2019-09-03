/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.fx;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.currency.FxRate;
import com.opengamma.strata.basics.currency.Payment;

/**
 * Test {@link ResolvedFxSingle}.
 */
public class ResolvedFxSingleTest {

  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1_000);
  private static final CurrencyAmount GBP_M1000 = CurrencyAmount.of(GBP, -1_000);
  private static final CurrencyAmount USD_P1600 = CurrencyAmount.of(USD, 1_600);
  private static final CurrencyAmount USD_M1600 = CurrencyAmount.of(USD, -1_600);
  private static final CurrencyAmount EUR_P1600 = CurrencyAmount.of(EUR, 1_800);
  private static final LocalDate DATE_2015_06_29 = date(2015, 6, 29);
  private static final LocalDate DATE_2015_06_30 = date(2015, 6, 30);
  private static final Payment PAYMENT_GBP_P1000 = Payment.of(GBP_P1000, DATE_2015_06_30);
  private static final Payment PAYMENT_GBP_M1000 = Payment.of(GBP_M1000, DATE_2015_06_30);
  private static final Payment PAYMENT_USD_P1600 = Payment.of(USD_P1600, DATE_2015_06_30);
  private static final Payment PAYMENT_USD_M1600 = Payment.of(USD_M1600, DATE_2015_06_30);

  //-------------------------------------------------------------------------
  @Test
  public void test_of_payments_rightOrder() {
    ResolvedFxSingle test = ResolvedFxSingle.of(PAYMENT_GBP_P1000, PAYMENT_USD_M1600);
    assertThat(test.getBaseCurrencyPayment()).isEqualTo(PAYMENT_GBP_P1000);
    assertThat(test.getCounterCurrencyPayment()).isEqualTo(PAYMENT_USD_M1600);
    assertThat(test.getPaymentDate()).isEqualTo(DATE_2015_06_30);
    assertThat(test.getCurrencyPair()).isEqualTo(CurrencyPair.of(GBP, USD));
    assertThat(test.getReceiveCurrencyAmount()).isEqualTo(GBP_P1000);
  }

  @Test
  public void test_of_payments_switchOrder() {
    ResolvedFxSingle test = ResolvedFxSingle.of(PAYMENT_USD_M1600, PAYMENT_GBP_P1000);
    assertThat(test.getBaseCurrencyPayment()).isEqualTo(PAYMENT_GBP_P1000);
    assertThat(test.getCounterCurrencyPayment()).isEqualTo(PAYMENT_USD_M1600);
    assertThat(test.getPaymentDate()).isEqualTo(DATE_2015_06_30);
    assertThat(test.getCurrencyPair()).isEqualTo(CurrencyPair.of(GBP, USD));
    assertThat(test.getReceiveCurrencyAmount()).isEqualTo(GBP_P1000);
  }

  @Test
  public void test_of_payments_sameCurrency() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedFxSingle.of(PAYMENT_GBP_P1000, PAYMENT_GBP_M1000));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_amounts_rightOrder() {
    ResolvedFxSingle test = sut();
    assertThat(test.getBaseCurrencyPayment()).isEqualTo(PAYMENT_GBP_P1000);
    assertThat(test.getCounterCurrencyPayment()).isEqualTo(PAYMENT_USD_M1600);
    assertThat(test.getPaymentDate()).isEqualTo(DATE_2015_06_30);
    assertThat(test.getCurrencyPair()).isEqualTo(CurrencyPair.of(GBP, USD));
    assertThat(test.getReceiveCurrencyAmount()).isEqualTo(GBP_P1000);
  }

  @Test
  public void test_of_amounts_switchOrder() {
    ResolvedFxSingle test = ResolvedFxSingle.of(USD_M1600, GBP_P1000, DATE_2015_06_30);
    assertThat(test.getBaseCurrencyPayment()).isEqualTo(PAYMENT_GBP_P1000);
    assertThat(test.getCounterCurrencyPayment()).isEqualTo(PAYMENT_USD_M1600);
    assertThat(test.getPaymentDate()).isEqualTo(DATE_2015_06_30);
    assertThat(test.getCurrencyPair()).isEqualTo(CurrencyPair.of(GBP, USD));
    assertThat(test.getReceiveCurrencyAmount()).isEqualTo(GBP_P1000);
  }

  @Test
  public void test_of_amounts_bothZero() {
    ResolvedFxSingle test = ResolvedFxSingle.of(CurrencyAmount.zero(GBP), CurrencyAmount.zero(USD), DATE_2015_06_30);
    assertThat(test.getBaseCurrencyPayment()).isEqualTo(Payment.of(CurrencyAmount.zero(GBP), DATE_2015_06_30));
    assertThat(test.getCounterCurrencyPayment()).isEqualTo(Payment.of(CurrencyAmount.zero(USD), DATE_2015_06_30));
    assertThat(test.getPaymentDate()).isEqualTo(DATE_2015_06_30);
    assertThat(test.getCurrencyPair()).isEqualTo(CurrencyPair.of(GBP, USD));
    assertThat(test.getReceiveCurrencyAmount()).isEqualTo(CurrencyAmount.zero(USD));
  }

  @Test
  public void test_of_amounts_positiveNegative() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedFxSingle.of(GBP_P1000, USD_P1600, DATE_2015_06_30));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedFxSingle.of(GBP_M1000, USD_M1600, DATE_2015_06_30));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedFxSingle.of(CurrencyAmount.zero(GBP), USD_M1600, DATE_2015_06_30));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedFxSingle.of(CurrencyAmount.zero(GBP), USD_P1600, DATE_2015_06_30));
  }

  @Test
  public void test_of_sameCurrency() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedFxSingle.of(GBP_P1000, GBP_M1000, DATE_2015_06_30));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_rate_rightOrder() {
    ResolvedFxSingle test = ResolvedFxSingle.of(GBP_P1000, FxRate.of(GBP, USD, 1.6d), DATE_2015_06_30);
    assertThat(test.getBaseCurrencyPayment()).isEqualTo(Payment.of(GBP_P1000, DATE_2015_06_30));
    assertThat(test.getCounterCurrencyPayment()).isEqualTo(Payment.of(USD_M1600, DATE_2015_06_30));
    assertThat(test.getPaymentDate()).isEqualTo(DATE_2015_06_30);
    assertThat(test.getCurrencyPair()).isEqualTo(CurrencyPair.of(GBP, USD));
    assertThat(test.getReceiveCurrencyAmount()).isEqualTo(GBP_P1000);
  }

  @Test
  public void test_of_rate_switchOrder() {
    ResolvedFxSingle test = ResolvedFxSingle.of(USD_M1600, FxRate.of(USD, GBP, 1d / 1.6d), DATE_2015_06_30);
    assertThat(test.getBaseCurrencyPayment()).isEqualTo(Payment.of(GBP_P1000, DATE_2015_06_30));
    assertThat(test.getCounterCurrencyPayment()).isEqualTo(Payment.of(USD_M1600, DATE_2015_06_30));
    assertThat(test.getPaymentDate()).isEqualTo(DATE_2015_06_30);
    assertThat(test.getCurrencyPair()).isEqualTo(CurrencyPair.of(GBP, USD));
    assertThat(test.getReceiveCurrencyAmount()).isEqualTo(GBP_P1000);
  }

  @Test
  public void test_of_rate_bothZero() {
    ResolvedFxSingle test = ResolvedFxSingle.of(CurrencyAmount.zero(GBP), FxRate.of(USD, GBP, 1.6d), DATE_2015_06_30);
    assertThat(test.getBaseCurrencyPayment().getValue()).isEqualTo(CurrencyAmount.zero(GBP));
    assertThat(test.getCounterCurrencyPayment().getValue().getAmount())
        .isCloseTo(CurrencyAmount.zero(USD).getAmount(), offset(1e-12));
    assertThat(test.getPaymentDate()).isEqualTo(DATE_2015_06_30);
    assertThat(test.getCurrencyPair()).isEqualTo(CurrencyPair.of(GBP, USD));
    assertThat(test.getReceiveCurrencyAmount()).isEqualTo(CurrencyAmount.of(USD, 0d));
  }

  @Test
  public void test_of_rate_wrongCurrency() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FxSingle.of(GBP_P1000, FxRate.of(USD, EUR, 1.45d), DATE_2015_06_30));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_rightOrder() {
    ResolvedFxSingle test = ResolvedFxSingle.meta().builder()
        .set(ResolvedFxSingle.meta().baseCurrencyPayment(), PAYMENT_GBP_P1000)
        .set(ResolvedFxSingle.meta().counterCurrencyPayment(), PAYMENT_USD_M1600)
        .build();
    assertThat(test.getBaseCurrencyPayment()).isEqualTo(PAYMENT_GBP_P1000);
    assertThat(test.getCounterCurrencyPayment()).isEqualTo(PAYMENT_USD_M1600);
    assertThat(test.getPaymentDate()).isEqualTo(DATE_2015_06_30);
  }

  @Test
  public void test_builder_switchOrder() {
    ResolvedFxSingle test = ResolvedFxSingle.meta().builder()
        .set(ResolvedFxSingle.meta().baseCurrencyPayment(), PAYMENT_USD_M1600)
        .set(ResolvedFxSingle.meta().counterCurrencyPayment(), PAYMENT_GBP_P1000)
        .build();
    assertThat(test.getBaseCurrencyPayment()).isEqualTo(PAYMENT_GBP_P1000);
    assertThat(test.getCounterCurrencyPayment()).isEqualTo(PAYMENT_USD_M1600);
    assertThat(test.getPaymentDate()).isEqualTo(DATE_2015_06_30);
  }

  @Test
  public void test_builder_bothPositive() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedFxSingle.meta().builder()
            .set(ResolvedFxSingle.meta().baseCurrencyPayment(), PAYMENT_GBP_P1000)
            .set(ResolvedFxSingle.meta().counterCurrencyPayment(), PAYMENT_USD_P1600)
            .build());
  }

  @Test
  public void test_builder_bothNegative() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedFxSingle.meta().builder()
            .set(ResolvedFxSingle.meta().baseCurrencyPayment(), PAYMENT_GBP_M1000)
            .set(ResolvedFxSingle.meta().counterCurrencyPayment(), PAYMENT_USD_M1600)
            .build());
  }

  @Test
  public void test_builder_sameCurrency() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ResolvedFxSingle.meta().builder()
            .set(ResolvedFxSingle.meta().baseCurrencyPayment(), PAYMENT_GBP_P1000)
            .set(ResolvedFxSingle.meta().counterCurrencyPayment(), PAYMENT_GBP_M1000)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_inverse() {
    ResolvedFxSingle test = sut();
    assertThat(test.inverse()).isEqualTo(ResolvedFxSingle.of(GBP_M1000, USD_P1600, DATE_2015_06_30));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
    coverBeanEquals(sut(), sut3());
    coverBeanEquals(sut2(), sut3());
  }

  @Test
  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static ResolvedFxSingle sut() {
    return ResolvedFxSingle.of(GBP_P1000, USD_M1600, DATE_2015_06_30);
  }

  static ResolvedFxSingle sut2() {
    return ResolvedFxSingle.of(GBP_M1000, EUR_P1600, DATE_2015_06_29);
  }

  static ResolvedFxSingle sut3() {
    return ResolvedFxSingle.of(USD_M1600, EUR_P1600, DATE_2015_06_30);
  }

}
