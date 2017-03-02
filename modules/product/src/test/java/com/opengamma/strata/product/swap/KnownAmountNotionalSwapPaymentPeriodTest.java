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
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import org.testng.annotations.Test;

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
@Test
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
  public void test_of() {
    SchedulePeriod sched = SchedulePeriod.of(DATE_2014_03_30, DATE_2014_09_30);
    KnownAmountNotionalSwapPaymentPeriod test = KnownAmountNotionalSwapPaymentPeriod.of(PAYMENT_2014_10_03, sched, GBP_P50000);
    assertEquals(test.getPayment(), PAYMENT_2014_10_03);
    assertEquals(test.getStartDate(), DATE_2014_03_30);
    assertEquals(test.getUnadjustedStartDate(), DATE_2014_03_30);
    assertEquals(test.getEndDate(), DATE_2014_09_30);
    assertEquals(test.getUnadjustedEndDate(), DATE_2014_09_30);
    assertEquals(test.getPaymentDate(), DATE_2014_10_03);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getNotionalAmount(), GBP_P50000);
    assertEquals(test.getFxResetObservation(), Optional.empty());
  }

  public void test_of_fxReset() {
    SchedulePeriod sched = SchedulePeriod.of(DATE_2014_03_30, DATE_2014_09_30);
    KnownAmountNotionalSwapPaymentPeriod test = KnownAmountNotionalSwapPaymentPeriod.of(
        PAYMENT_2014_10_03, sched, USD_P50000, FX_RESET);
    assertEquals(test.getPayment(), PAYMENT_2014_10_03);
    assertEquals(test.getStartDate(), DATE_2014_03_30);
    assertEquals(test.getUnadjustedStartDate(), DATE_2014_03_30);
    assertEquals(test.getEndDate(), DATE_2014_09_30);
    assertEquals(test.getUnadjustedEndDate(), DATE_2014_09_30);
    assertEquals(test.getPaymentDate(), DATE_2014_10_03);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getNotionalAmount(), USD_P50000);
    assertEquals(test.getFxResetObservation(), Optional.of(FX_RESET));
  }

  public void test_builder_defaultDates() {
    KnownAmountNotionalSwapPaymentPeriod test = KnownAmountNotionalSwapPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_03)
        .startDate(DATE_2014_03_30)
        .endDate(DATE_2014_10_01)
        .notionalAmount(USD_P50000)
        .fxResetObservation(FX_RESET)
        .build();
    assertEquals(test.getPayment(), PAYMENT_2014_10_03);
    assertEquals(test.getStartDate(), DATE_2014_03_30);
    assertEquals(test.getUnadjustedStartDate(), DATE_2014_03_30);
    assertEquals(test.getEndDate(), DATE_2014_10_01);
    assertEquals(test.getUnadjustedEndDate(), DATE_2014_10_01);
    assertEquals(test.getPaymentDate(), DATE_2014_10_03);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getNotionalAmount(), USD_P50000);
    assertEquals(test.getFxResetObservation(), Optional.of(FX_RESET));
  }

  public void test_builder_invalid() {
    assertThrowsIllegalArg(() -> KnownAmountNotionalSwapPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_03)
        .endDate(DATE_2014_10_01)
        .notionalAmount(GBP_P50000)
        .build());
    assertThrowsIllegalArg(() -> KnownAmountNotionalSwapPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_03)
        .startDate(DATE_2014_10_01)
        .notionalAmount(GBP_P50000)
        .build());
    assertThrowsIllegalArg(() -> KnownAmountNotionalSwapPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_03)
        .startDate(DATE_2014_10_01)
        .endDate(DATE_2014_10_01)
        .notionalAmount(GBP_P50000)
        .build());
    assertThrowsIllegalArg(() -> KnownAmountNotionalSwapPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_03)
        .startDate(DATE_2014_03_30)
        .endDate(DATE_2014_10_01)
        .notionalAmount(CurrencyAmount.of(USD, 1000d))
        .build());
    assertThrowsIllegalArg(() -> KnownAmountNotionalSwapPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_03)
        .startDate(DATE_2014_03_30)
        .endDate(DATE_2014_10_01)
        .notionalAmount(CurrencyAmount.of(GBP, 1000d))
        .fxResetObservation(FX_RESET)
        .build());
    assertThrowsIllegalArg(() -> KnownAmountNotionalSwapPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_03)
        .startDate(DATE_2014_03_30)
        .endDate(DATE_2014_10_01)
        .notionalAmount(CurrencyAmount.of(EUR, 1000d))
        .fxResetObservation(FX_RESET)
        .build());
    assertThrowsIllegalArg(() -> KnownAmountNotionalSwapPaymentPeriod.builder()
        .payment(Payment.of(CurrencyAmount.of(EUR, 1000d), DATE_2014_10_03))
        .startDate(DATE_2014_03_30)
        .endDate(DATE_2014_10_01)
        .notionalAmount(CurrencyAmount.of(USD, 1000d))
        .fxResetObservation(FX_RESET)
        .build());
  }

  //-------------------------------------------------------------------------
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
    assertEquals(test.adjustPaymentDate(TemporalAdjusters.ofDateAdjuster(d -> d.plusDays(0))), test);
    assertEquals(test.adjustPaymentDate(TemporalAdjusters.ofDateAdjuster(d -> d.plusDays(2))), expected);
  }

  //-------------------------------------------------------------------------
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
    assertEquals(builder.build(), ImmutableSet.of());
  }

  public void test_collectIndices_fxReset() {
    SchedulePeriod sched = SchedulePeriod.of(DATE_2014_03_30, DATE_2014_09_30);
    KnownAmountNotionalSwapPaymentPeriod test = KnownAmountNotionalSwapPaymentPeriod.of(
        PAYMENT_2014_10_03, sched, USD_P50000, FX_RESET);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(FX_RESET.getIndex()));
  }

  //-------------------------------------------------------------------------
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
