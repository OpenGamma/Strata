/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.index.PriceIndices.GB_RPI;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.product.rate.FixedRateComputation;
import com.opengamma.strata.product.rate.InflationEndInterpolatedRateComputation;
import com.opengamma.strata.product.rate.InflationEndMonthRateComputation;

/**
 * Test {@link CapitalIndexedBondPaymentPeriod}.
 */
public class CapitalIndexedBondPaymentPeriodTest {

  private static final LocalDate START_UNADJ = LocalDate.of(2008, 1, 13);
  private static final LocalDate END_UNADJ = LocalDate.of(2008, 7, 13);
  private static final LocalDate START = LocalDate.of(2008, 1, 14);
  private static final LocalDate END = LocalDate.of(2008, 7, 14);
  private static final YearMonth REF_END = YearMonth.of(2008, 4);
  private static final double NOTIONAL = 10_000_000d;
  private static final double REAL_COUPON = 0.01d;
  private static final LocalDate DETACHMENT = LocalDate.of(2008, 1, 11);
  private static final double START_INDEX = 198.475;
  private static final InflationEndInterpolatedRateComputation COMPUTE_INTERP =
      InflationEndInterpolatedRateComputation.of(US_CPI_U, START_INDEX, REF_END, 0.25);
  private static final InflationEndMonthRateComputation COMPUTE_MONTH =
      InflationEndMonthRateComputation.of(US_CPI_U, START_INDEX, REF_END);

  @Test
  public void test_builder_full() {
    CapitalIndexedBondPaymentPeriod test = CapitalIndexedBondPaymentPeriod.builder()
        .currency(USD)
        .notional(NOTIONAL)
        .detachmentDate(DETACHMENT)
        .startDate(START)
        .endDate(END)
        .unadjustedStartDate(START_UNADJ)
        .unadjustedEndDate(END_UNADJ)
        .rateComputation(COMPUTE_INTERP)
        .realCoupon(REAL_COUPON)
        .build();
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getDetachmentDate()).isEqualTo(DETACHMENT);
    assertThat(test.getStartDate()).isEqualTo(START);
    assertThat(test.getEndDate()).isEqualTo(END);
    assertThat(test.getUnadjustedStartDate()).isEqualTo(START_UNADJ);
    assertThat(test.getUnadjustedEndDate()).isEqualTo(END_UNADJ);
    assertThat(test.getRateComputation()).isEqualTo(COMPUTE_INTERP);
    assertThat(test.getRealCoupon()).isEqualTo(REAL_COUPON);
  }

  @Test
  public void test_builder_min() {
    CapitalIndexedBondPaymentPeriod test = CapitalIndexedBondPaymentPeriod.builder()
        .currency(USD)
        .notional(NOTIONAL)
        .startDate(START)
        .endDate(END)
        .rateComputation(COMPUTE_MONTH)
        .realCoupon(REAL_COUPON)
        .build();
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getDetachmentDate()).isEqualTo(END);
    assertThat(test.getStartDate()).isEqualTo(START);
    assertThat(test.getEndDate()).isEqualTo(END);
    assertThat(test.getUnadjustedStartDate()).isEqualTo(START);
    assertThat(test.getUnadjustedEndDate()).isEqualTo(END);
    assertThat(test.getRateComputation()).isEqualTo(COMPUTE_MONTH);
    assertThat(test.getRealCoupon()).isEqualTo(REAL_COUPON);
  }

  @Test
  public void test_builder_fail() {
    // not inflation rate observation
    FixedRateComputation fixedRate = FixedRateComputation.of(0.01);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CapitalIndexedBondPaymentPeriod.builder()
            .currency(USD)
            .notional(NOTIONAL)
            .detachmentDate(DETACHMENT)
            .startDate(START)
            .endDate(END)
            .unadjustedStartDate(START_UNADJ)
            .unadjustedEndDate(END_UNADJ)
            .rateComputation(fixedRate)
            .realCoupon(REAL_COUPON)
            .build());
    // wrong start date and end date
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CapitalIndexedBondPaymentPeriod.builder()
            .currency(USD)
            .notional(NOTIONAL)
            .detachmentDate(DETACHMENT)
            .startDate(END.plusDays(1))
            .endDate(END)
            .unadjustedStartDate(START_UNADJ)
            .unadjustedEndDate(END_UNADJ)
            .rateComputation(COMPUTE_INTERP)
            .realCoupon(REAL_COUPON)
            .build());
    // wrong unadjusted start date and unadjusted end date
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CapitalIndexedBondPaymentPeriod.builder()
            .currency(USD)
            .notional(NOTIONAL)
            .detachmentDate(DETACHMENT)
            .startDate(START)
            .endDate(END)
            .unadjustedStartDate(START_UNADJ)
            .unadjustedEndDate(START_UNADJ.minusWeeks(1))
            .rateComputation(COMPUTE_INTERP)
            .realCoupon(REAL_COUPON)
            .build());
  }

  @Test
  public void test_methods() {
    CapitalIndexedBondPaymentPeriod test = CapitalIndexedBondPaymentPeriod.builder()
        .currency(USD)
        .notional(NOTIONAL)
        .detachmentDate(DETACHMENT)
        .startDate(START)
        .endDate(END)
        .unadjustedStartDate(START_UNADJ)
        .unadjustedEndDate(END_UNADJ)
        .rateComputation(COMPUTE_INTERP)
        .realCoupon(REAL_COUPON)
        .build();
    assertThat(test.getPaymentDate()).isEqualTo(END);
    assertThat(test.adjustPaymentDate(TemporalAdjusters.ofDateAdjuster(d -> d.plusDays(2)))).isEqualTo(test);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    ImmutableSet<Index> set = builder.build();
    assertThat(set).hasSize(1);
    assertThat(set.asList().get(0)).isEqualTo(US_CPI_U);

    LocalDate bondStart = LocalDate.of(2003, 1, 13);
    LocalDate bondStartUnadj = LocalDate.of(2003, 1, 12);
    CapitalIndexedBondPaymentPeriod expected = CapitalIndexedBondPaymentPeriod.builder()
        .currency(USD)
        .notional(NOTIONAL)
        .detachmentDate(END)
        .startDate(bondStart)
        .endDate(END)
        .unadjustedStartDate(bondStartUnadj)
        .unadjustedEndDate(END_UNADJ)
        .rateComputation(COMPUTE_INTERP)
        .realCoupon(1d)
        .build();
    assertThat(test.withUnitCoupon(bondStart, bondStartUnadj)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    CapitalIndexedBondPaymentPeriod test1 = CapitalIndexedBondPaymentPeriod.builder()
        .currency(USD)
        .notional(NOTIONAL)
        .detachmentDate(DETACHMENT)
        .startDate(START)
        .endDate(END)
        .unadjustedStartDate(START_UNADJ)
        .unadjustedEndDate(END_UNADJ)
        .rateComputation(COMPUTE_INTERP)
        .realCoupon(REAL_COUPON)
        .build();
    coverImmutableBean(test1);
    CapitalIndexedBondPaymentPeriod test2 = CapitalIndexedBondPaymentPeriod.builder()
        .currency(GBP)
        .notional(5.0e6)
        .startDate(LocalDate.of(2008, 1, 15))
        .endDate(LocalDate.of(2008, 7, 15))
        .rateComputation(InflationEndMonthRateComputation.of(GB_RPI, 155.32, REF_END))
        .realCoupon(1d)
        .build();
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    CapitalIndexedBondPaymentPeriod test = CapitalIndexedBondPaymentPeriod.builder()
        .currency(USD)
        .notional(NOTIONAL)
        .detachmentDate(DETACHMENT)
        .startDate(START)
        .endDate(END)
        .unadjustedStartDate(START_UNADJ)
        .unadjustedEndDate(END_UNADJ)
        .rateComputation(COMPUTE_INTERP)
        .realCoupon(REAL_COUPON)
        .build();
    assertSerialization(test);
  }

}
