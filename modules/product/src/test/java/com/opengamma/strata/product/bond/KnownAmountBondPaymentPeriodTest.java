/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.SchedulePeriod;

/**
 * Test {@link KnownAmountBondPaymentPeriod}.
 */
public class KnownAmountBondPaymentPeriodTest {

  private static final CurrencyAmount GBP_P1000 = CurrencyAmount.of(GBP, 1000);
  private static final LocalDate DATE_2014_03_30 = date(2014, 3, 30);
  private static final LocalDate DATE_2014_06_30 = date(2014, 6, 30);
  private static final LocalDate DATE_2014_09_30 = date(2014, 9, 30);
  private static final LocalDate DATE_2014_10_01 = date(2014, 10, 1);
  private static final LocalDate DATE_2014_10_03 = date(2014, 10, 3);
  private static final Payment PAYMENT_2014_10_01 = Payment.of(GBP_P1000, DATE_2014_10_01);
  private static final Payment PAYMENT_2014_10_03 = Payment.of(GBP_P1000, DATE_2014_10_03);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    SchedulePeriod sched = SchedulePeriod.of(DATE_2014_03_30, DATE_2014_09_30);
    KnownAmountBondPaymentPeriod test = KnownAmountBondPaymentPeriod.of(PAYMENT_2014_10_03, sched);
    assertThat(test.getPayment()).isEqualTo(PAYMENT_2014_10_03);
    assertThat(test.getStartDate()).isEqualTo(DATE_2014_03_30);
    assertThat(test.getUnadjustedStartDate()).isEqualTo(DATE_2014_03_30);
    assertThat(test.getEndDate()).isEqualTo(DATE_2014_09_30);
    assertThat(test.getUnadjustedEndDate()).isEqualTo(DATE_2014_09_30);
    assertThat(test.getPaymentDate()).isEqualTo(DATE_2014_10_03);
    assertThat(test.getCurrency()).isEqualTo(GBP);
  }

  @Test
  public void test_builder_defaultDates() {
    KnownAmountBondPaymentPeriod test = KnownAmountBondPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_03)
        .startDate(DATE_2014_03_30)
        .endDate(DATE_2014_10_01)
        .build();
    assertThat(test.getPayment()).isEqualTo(PAYMENT_2014_10_03);
    assertThat(test.getStartDate()).isEqualTo(DATE_2014_03_30);
    assertThat(test.getUnadjustedStartDate()).isEqualTo(DATE_2014_03_30);
    assertThat(test.getEndDate()).isEqualTo(DATE_2014_10_01);
    assertThat(test.getUnadjustedEndDate()).isEqualTo(DATE_2014_10_01);
    assertThat(test.getPaymentDate()).isEqualTo(DATE_2014_10_03);
    assertThat(test.getCurrency()).isEqualTo(GBP);
  }

  @Test
  public void test_builder_invalid() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> KnownAmountBondPaymentPeriod.builder()
            .payment(PAYMENT_2014_10_03)
            .endDate(DATE_2014_10_01)
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> KnownAmountBondPaymentPeriod.builder()
            .payment(PAYMENT_2014_10_03)
            .startDate(DATE_2014_10_01)
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> KnownAmountBondPaymentPeriod.builder()
            .payment(PAYMENT_2014_10_03)
            .startDate(DATE_2014_10_01)
            .endDate(DATE_2014_10_01)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_adjustPaymentDate() {
    KnownAmountBondPaymentPeriod test = KnownAmountBondPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_01)
        .startDate(DATE_2014_03_30)
        .unadjustedStartDate(DATE_2014_03_30)
        .endDate(DATE_2014_10_01)
        .unadjustedEndDate(DATE_2014_09_30)
        .build();
    KnownAmountBondPaymentPeriod expected = KnownAmountBondPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_03)
        .startDate(DATE_2014_03_30)
        .unadjustedStartDate(DATE_2014_03_30)
        .endDate(DATE_2014_10_01)
        .unadjustedEndDate(DATE_2014_09_30)
        .build();
    assertThat(test.adjustPaymentDate(TemporalAdjusters.ofDateAdjuster(d -> d.plusDays(0)))).isEqualTo(test);
    assertThat(test.adjustPaymentDate(TemporalAdjusters.ofDateAdjuster(d -> d.plusDays(2)))).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_collectIndices_simple() {
    KnownAmountBondPaymentPeriod test = KnownAmountBondPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_03)
        .startDate(DATE_2014_03_30)
        .unadjustedStartDate(DATE_2014_03_30)
        .endDate(DATE_2014_10_01)
        .unadjustedEndDate(DATE_2014_09_30)
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertThat(builder.build()).isEmpty();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    KnownAmountBondPaymentPeriod test = KnownAmountBondPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_03)
        .startDate(DATE_2014_03_30)
        .unadjustedStartDate(DATE_2014_03_30)
        .endDate(DATE_2014_10_01)
        .unadjustedEndDate(DATE_2014_09_30)
        .build();
    coverImmutableBean(test);
    KnownAmountBondPaymentPeriod test2 = KnownAmountBondPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_03.negated())
        .startDate(DATE_2014_06_30)
        .endDate(DATE_2014_09_30)
        .build();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    KnownAmountBondPaymentPeriod test = KnownAmountBondPaymentPeriod.builder()
        .payment(PAYMENT_2014_10_03)
        .startDate(DATE_2014_03_30)
        .unadjustedStartDate(DATE_2014_03_30)
        .endDate(DATE_2014_10_01)
        .unadjustedEndDate(DATE_2014_09_30)
        .build();
    assertSerialization(test);
  }

}
