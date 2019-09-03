/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.Index;

/**
 * Test {@link FixedCouponBondPaymentPeriod}.
 */
public class FixedCouponBondPaymentPeriodTest {

  private static final LocalDate START = LocalDate.of(2015, 2, 2);
  private static final LocalDate END = LocalDate.of(2015, 8, 2);
  private static final LocalDate START_ADJUSTED = LocalDate.of(2015, 2, 2);
  private static final LocalDate END_ADJUSTED = LocalDate.of(2015, 8, 3);
  private static final LocalDate DETACHMENT_DATE = LocalDate.of(2015, 7, 27);
  private static final double FIXED_RATE = 0.025;
  private static final double NOTIONAL = 1.0e7;
  private static final double YEAR_FRACTION = 0.5;

  @Test
  public void test_of() {
    FixedCouponBondPaymentPeriod test = FixedCouponBondPaymentPeriod.builder()
        .currency(USD)
        .startDate(START_ADJUSTED)
        .unadjustedStartDate(START)
        .endDate(END_ADJUSTED)
        .unadjustedEndDate(END)
        .detachmentDate(DETACHMENT_DATE)
        .notional(NOTIONAL)
        .fixedRate(FIXED_RATE)
        .yearFraction(YEAR_FRACTION)
        .build();
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getUnadjustedStartDate()).isEqualTo(START);
    assertThat(test.getStartDate()).isEqualTo(START_ADJUSTED);
    assertThat(test.getUnadjustedEndDate()).isEqualTo(END);
    assertThat(test.getEndDate()).isEqualTo(END_ADJUSTED);
    assertThat(test.getPaymentDate()).isEqualTo(END_ADJUSTED);
    assertThat(test.getDetachmentDate()).isEqualTo(DETACHMENT_DATE);
    assertThat(test.getFixedRate()).isEqualTo(FIXED_RATE);
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getYearFraction()).isEqualTo(YEAR_FRACTION);
    assertThat(test.hasExCouponPeriod()).isTrue();

    // the object is not changed
    assertThat(test.adjustPaymentDate(TemporalAdjusters.ofDateAdjuster(d -> d.plusDays(2)))).isEqualTo(test);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getUnadjustedStartDate()).isEqualTo(START);
    assertThat(test.getStartDate()).isEqualTo(START_ADJUSTED);
    assertThat(test.getUnadjustedEndDate()).isEqualTo(END);
    assertThat(test.getEndDate()).isEqualTo(END_ADJUSTED);
    assertThat(test.getPaymentDate()).isEqualTo(END_ADJUSTED);
    assertThat(test.getDetachmentDate()).isEqualTo(DETACHMENT_DATE);
    assertThat(test.getFixedRate()).isEqualTo(FIXED_RATE);
    assertThat(test.getNotional()).isEqualTo(NOTIONAL);
    assertThat(test.getYearFraction()).isEqualTo(YEAR_FRACTION);
    assertThat(test.hasExCouponPeriod()).isTrue();
  }

  @Test
  public void test_of_noExCoupon() {
    FixedCouponBondPaymentPeriod test = FixedCouponBondPaymentPeriod.builder()
        .currency(USD)
        .startDate(START_ADJUSTED)
        .unadjustedStartDate(START)
        .endDate(END_ADJUSTED)
        .unadjustedEndDate(END)
        .detachmentDate(END_ADJUSTED)
        .notional(NOTIONAL)
        .fixedRate(FIXED_RATE)
        .yearFraction(YEAR_FRACTION)
        .build();
    assertThat(test.hasExCouponPeriod()).isFalse();
  }

  @Test
  public void test_of_wrongDates() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixedCouponBondPaymentPeriod.builder()
            .currency(USD)
            .startDate(START_ADJUSTED)
            .unadjustedStartDate(START)
            .endDate(LocalDate.of(2015, 2, 3))
            .unadjustedEndDate(LocalDate.of(2015, 2, 2))
            .notional(NOTIONAL)
            .fixedRate(FIXED_RATE)
            .yearFraction(YEAR_FRACTION)
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixedCouponBondPaymentPeriod.builder()
            .currency(USD)
            .startDate(LocalDate.of(2015, 8, 3))
            .unadjustedStartDate(LocalDate.of(2015, 8, 2))
            .endDate(LocalDate.of(2015, 8, 3))
            .unadjustedEndDate(LocalDate.of(2015, 8, 3))
            .notional(NOTIONAL)
            .fixedRate(FIXED_RATE)
            .yearFraction(YEAR_FRACTION)
            .build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixedCouponBondPaymentPeriod.builder()
            .currency(USD)
            .startDate(START_ADJUSTED)
            .unadjustedStartDate(START)
            .endDate(END_ADJUSTED)
            .unadjustedEndDate(END)
            .detachmentDate(LocalDate.of(2015, 8, 6))
            .notional(NOTIONAL)
            .fixedRate(FIXED_RATE)
            .yearFraction(YEAR_FRACTION)
            .build());
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_contains() {
    FixedCouponBondPaymentPeriod test = FixedCouponBondPaymentPeriod.builder()
        .currency(USD)
        .startDate(START_ADJUSTED)
        .unadjustedStartDate(START)
        .endDate(END_ADJUSTED)
        .unadjustedEndDate(END)
        .detachmentDate(DETACHMENT_DATE)
        .notional(NOTIONAL)
        .fixedRate(FIXED_RATE)
        .yearFraction(YEAR_FRACTION)
        .build();
    assertThat(test.contains(START.minusDays(1))).isFalse();
    assertThat(test.contains(START)).isTrue();
    assertThat(test.contains(START.plusDays(1))).isTrue();
    assertThat(test.contains(END.minusDays(1))).isTrue();
    assertThat(test.contains(END)).isFalse();
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FixedCouponBondPaymentPeriod test1 = FixedCouponBondPaymentPeriod.builder()
        .currency(USD)
        .startDate(START_ADJUSTED)
        .unadjustedStartDate(START)
        .endDate(END_ADJUSTED)
        .unadjustedEndDate(END)
        .detachmentDate(DETACHMENT_DATE)
        .notional(NOTIONAL)
        .fixedRate(FIXED_RATE)
        .yearFraction(YEAR_FRACTION)
        .build();
    coverImmutableBean(test1);
    FixedCouponBondPaymentPeriod test2 = FixedCouponBondPaymentPeriod.builder()
        .currency(GBP)
        .startDate(LocalDate.of(2014, 3, 4))
        .unadjustedStartDate(LocalDate.of(2014, 3, 2))
        .endDate(LocalDate.of(2015, 3, 4))
        .unadjustedEndDate(LocalDate.of(2015, 3, 3))
        .notional(1.0e8)
        .fixedRate(0.005)
        .yearFraction(1d)
        .build();
    coverBeanEquals(test1, test2);
  }

  @Test
  public void test_serialization() {
    FixedCouponBondPaymentPeriod test = FixedCouponBondPaymentPeriod.builder()
        .currency(USD)
        .startDate(START_ADJUSTED)
        .unadjustedStartDate(START)
        .endDate(END_ADJUSTED)
        .unadjustedEndDate(END)
        .detachmentDate(DETACHMENT_DATE)
        .notional(NOTIONAL)
        .fixedRate(FIXED_RATE)
        .yearFraction(YEAR_FRACTION)
        .build();
    assertSerialization(test);
  }

}
