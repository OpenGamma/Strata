/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.index.FxIndexObservation;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.SchedulePeriod;

/**
 * Test {@link KnownAmountNotionalSwapPaymentPeriod}.
 */
public class KnownAmountNotionalSwapPaymentPeriodTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final CurrencyAmount GBP_P50000 = CurrencyAmount.of(GBP, 50000);
  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1000);
  private static final CurrencyAmount USD_P50000 = CurrencyAmount.of(USD, 50000);
  private static final LocalDate DATE_2014_03_30 = date(2014, 3, 30);
  private static final LocalDate DATE_2014_06_30 = date(2014, 6, 30);
  private static final LocalDate DATE_2014_09_30 = date(2014, 9, 30);
  private static final LocalDate DATE_2014_10_01 = date(2014, 10, 1);
  private static final LocalDate DATE_2014_10_03 = date(2014, 10, 3);
  private static final Payment PAYMENT_2014_10_01 = Payment.of(GBP_P1000, DATE_2014_10_01);
  private static final Payment PAYMENT_2014_10_03 = Payment.of(GBP_P1000, DATE_2014_10_03);
  private static final FxIndexObservation FX_RESET = FxIndexObservation.of(GBP_USD_WM, date(2014, 3, 28), REF_DATA);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    SchedulePeriod sched = SchedulePeriod.of(DATE_2014_03_30, DATE_2014_09_30);
    KnownAmountNotionalSwapPaymentPeriod test = KnownAmountNotionalSwapPaymentPeriod.of(PAYMENT_2014_10_03, sched, GBP_P50000);
    assertThat(test.getPayment()).isEqualTo(PAYMENT_2014_10_03);
    assertThat(test.getStartDate()).isEqualTo(DATE_2014_03_30);
    assertThat(test.getUnadjustedStartDate()).isEqualTo(DATE_2014_03_30);
    assertThat(test.getEndDate()).isEqualTo(DATE_2014_09_30);
    assertThat(test.getUnadjustedEndDate()).isEqualTo(DATE_2014_09_30);
    assertThat(test.getPaymentDate()).isEqualTo(DATE_2014_10_03);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getNotionalAmount()).isEqualTo(GBP_P50000);
    assertThat(test.getFxResetObservation()).isEqualTo(Optional.empty());
  }

  @Test
  public void test_of_fxReset() {
    SchedulePeriod sched = SchedulePeriod.of(DATE_2014_03_30, DATE_2014_09_30);
    KnownAmountNotionalSwapPaymentPeriod test = KnownAmountNotionalSwapPaymentPeriod.of(
        PAYMENT_2014_10_03, sched, USD_P50000, FX_RESET);
    assertThat(test.getPayment()).isEqualTo(PAYMENT_2014_10_03);
    assertThat(test.getStartDate()).isEqualTo(DATE_2014_03_30);
    assertThat(test.getUnadjustedStartDate()).isEqualTo(DATE_2014_03_30);
    assertThat(test.getEndDate()).isEqualTo(DATE_2014_09_30);
    assertThat(test.getUnadjustedEndDate()).isEqualTo(DATE_2014_09_30);
    assertThat(test.getPaymentDate()).isEqualTo(DATE_2014_10_03);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getNotionalAmount()).isEqualTo(USD_P50000);
    assertThat(test.getFxResetObservation()).isEqualTo(Optional.of(FX_RESET));
  }

  @Test
  public void test_builder_defaultDates() {
    KnownAmountNotionalSwapPaymentPeriod test = KnownAmountNotionalSwapPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_03)
        .startDate(DATE_2014_03_30)
        .endDate(DATE_2014_10_01)
        .notionalAmount(USD_P50000)
        .fxResetObservation(FX_RESET)
        .build();
    assertThat(test.getPayment()).isEqualTo(PAYMENT_2014_10_03);
    assertThat(test.getStartDate()).isEqualTo(DATE_2014_03_30);
    assertThat(test.getUnadjustedStartDate()).isEqualTo(DATE_2014_03_30);
    assertThat(test.getEndDate()).isEqualTo(DATE_2014_10_01);
    assertThat(test.getUnadjustedEndDate()).isEqualTo(DATE_2014_10_01);
    assertThat(test.getPaymentDate()).isEqualTo(DATE_2014_10_03);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getNotionalAmount()).isEqualTo(USD_P50000);
    assertThat(test.getFxResetObservation()).isEqualTo(Optional.of(FX_RESET));
  }

  @Test
  public void test_builder_invalid() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> KnownAmountNotionalSwapPaymentPeriod.builder()
            .payment(PAYMENT_2014_10_03)
            .endDate(DATE_2014_10_01)
            .notionalAmount(GBP_P50000)
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> KnownAmountNotionalSwapPaymentPeriod.builder()
            .payment(PAYMENT_2014_10_03)
            .startDate(DATE_2014_10_01)
            .notionalAmount(GBP_P50000)
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> KnownAmountNotionalSwapPaymentPeriod.builder()
            .payment(PAYMENT_2014_10_03)
            .startDate(DATE_2014_10_01)
            .endDate(DATE_2014_10_01)
            .notionalAmount(GBP_P50000)
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> KnownAmountNotionalSwapPaymentPeriod.builder()
            .payment(PAYMENT_2014_10_03)
            .startDate(DATE_2014_03_30)
            .endDate(DATE_2014_10_01)
            .notionalAmount(CurrencyAmount.of(USD, 1000d))
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> KnownAmountNotionalSwapPaymentPeriod.builder()
            .payment(PAYMENT_2014_10_03)
            .startDate(DATE_2014_03_30)
            .endDate(DATE_2014_10_01)
            .notionalAmount(CurrencyAmount.of(GBP, 1000d))
            .fxResetObservation(FX_RESET)
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> KnownAmountNotionalSwapPaymentPeriod.builder()
            .payment(PAYMENT_2014_10_03)
            .startDate(DATE_2014_03_30)
            .endDate(DATE_2014_10_01)
            .notionalAmount(CurrencyAmount.of(EUR, 1000d))
            .fxResetObservation(FX_RESET)
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> KnownAmountNotionalSwapPaymentPeriod.builder()
            .payment(Payment.of(CurrencyAmount.of(EUR, 1000d), DATE_2014_10_03))
            .startDate(DATE_2014_03_30)
            .endDate(DATE_2014_10_01)
            .notionalAmount(CurrencyAmount.of(USD, 1000d))
            .fxResetObservation(FX_RESET)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_adjustPaymentDate() {
    KnownAmountNotionalSwapPaymentPeriod test = KnownAmountNotionalSwapPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_01)
        .startDate(DATE_2014_03_30)
        .unadjustedStartDate(DATE_2014_03_30)
        .endDate(DATE_2014_10_01)
        .unadjustedEndDate(DATE_2014_09_30)
        .notionalAmount(GBP_P50000)
        .build();
    KnownAmountNotionalSwapPaymentPeriod expected = KnownAmountNotionalSwapPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_03)
        .startDate(DATE_2014_03_30)
        .unadjustedStartDate(DATE_2014_03_30)
        .endDate(DATE_2014_10_01)
        .unadjustedEndDate(DATE_2014_09_30)
        .notionalAmount(GBP_P50000)
        .build();
    assertThat(test.adjustPaymentDate(TemporalAdjusters.ofDateAdjuster(d -> d.plusDays(0)))).isEqualTo(test);
    assertThat(test.adjustPaymentDate(TemporalAdjusters.ofDateAdjuster(d -> d.plusDays(2)))).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_collectIndices_simple() {
    KnownAmountNotionalSwapPaymentPeriod test = KnownAmountNotionalSwapPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_03)
        .startDate(DATE_2014_03_30)
        .unadjustedStartDate(DATE_2014_03_30)
        .endDate(DATE_2014_10_01)
        .unadjustedEndDate(DATE_2014_09_30)
        .notionalAmount(GBP_P50000)
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertThat(builder.build()).isEmpty();
  }

  @Test
  public void test_collectIndices_fxReset() {
    SchedulePeriod sched = SchedulePeriod.of(DATE_2014_03_30, DATE_2014_09_30);
    KnownAmountNotionalSwapPaymentPeriod test = KnownAmountNotionalSwapPaymentPeriod.of(
        PAYMENT_2014_10_03, sched, USD_P50000, FX_RESET);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertThat(builder.build()).containsOnly(FX_RESET.getIndex());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    KnownAmountNotionalSwapPaymentPeriod test = KnownAmountNotionalSwapPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_03)
        .startDate(DATE_2014_03_30)
        .unadjustedStartDate(DATE_2014_03_30)
        .endDate(DATE_2014_10_01)
        .unadjustedEndDate(DATE_2014_09_30)
        .notionalAmount(GBP_P50000)
        .build();
    coverImmutableBean(test);
    KnownAmountNotionalSwapPaymentPeriod test2 = KnownAmountNotionalSwapPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_03.negated())
        .startDate(DATE_2014_06_30)
        .endDate(DATE_2014_09_30)
        .notionalAmount(GBP_P1000)
        .build();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    KnownAmountNotionalSwapPaymentPeriod test = KnownAmountNotionalSwapPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_03)
        .startDate(DATE_2014_03_30)
        .unadjustedStartDate(DATE_2014_03_30)
        .endDate(DATE_2014_10_01)
        .unadjustedEndDate(DATE_2014_09_30)
        .notionalAmount(GBP_P50000)
        .build();
    assertSerialization(test);
  }

}
