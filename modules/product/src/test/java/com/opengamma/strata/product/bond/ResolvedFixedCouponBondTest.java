/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.schedule.RollConventions;

/**
 * Test {@link ResolvedFixedCouponBond}.
 */
public class ResolvedFixedCouponBondTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  @Test
  public void test_getters() {
    ResolvedFixedCouponBond test = sut();
    ImmutableList<FixedCouponBondPaymentPeriod> payments = test.getPeriodicPayments();
    assertThat(test.getStartDate()).isEqualTo(payments.get(0).getStartDate());
    assertThat(test.getEndDate()).isEqualTo(payments.get(payments.size() - 1).getEndDate());
    assertThat(test.getUnadjustedStartDate()).isEqualTo(payments.get(0).getUnadjustedStartDate());
    assertThat(test.getUnadjustedEndDate()).isEqualTo(payments.get(payments.size() - 1).getUnadjustedEndDate());
    assertThat(test.hasExCouponPeriod()).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_yearFraction() {
    ResolvedFixedCouponBond test = sut();
    FixedCouponBondPaymentPeriod period = test.getPeriodicPayments().get(0);
    assertThat(test.yearFraction(period.getUnadjustedStartDate(), period.getUnadjustedEndDate()))
        .isEqualTo(period.getYearFraction());
  }

  @Test
  public void test_yearFraction_scheduleInfo() {
    ResolvedFixedCouponBond base = sut();
    FixedCouponBondPaymentPeriod period = base.getPeriodicPayments().get(0);
    AtomicBoolean eom = new AtomicBoolean(false);
    DayCount dc = new DayCount() {
      @Override
      public double yearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
        assertThat(scheduleInfo.getStartDate()).isEqualTo(base.getUnadjustedStartDate());
        assertThat(scheduleInfo.getEndDate()).isEqualTo(base.getUnadjustedEndDate());
        assertThat(scheduleInfo.getPeriodEndDate(firstDate)).isEqualTo(period.getUnadjustedEndDate());
        assertThat(scheduleInfo.getFrequency()).isEqualTo(base.getFrequency());
        assertThat(scheduleInfo.isEndOfMonthConvention()).isEqualTo(eom.get());
        return 0.5;
      }

      @Override
      public int days(LocalDate firstDate, LocalDate secondDate) {
        return 182;
      }

      @Override
      public String getName() {
        return "";
      }
    };
    ResolvedFixedCouponBond test = base.toBuilder().dayCount(dc).build();
    assertThat(test.yearFraction(period.getUnadjustedStartDate(), period.getUnadjustedEndDate())).isEqualTo(0.5);
    // test with EOM=true
    ResolvedFixedCouponBond test2 = test.toBuilder().rollConvention(RollConventions.EOM).build();
    eom.set(true);
    assertThat(test2.yearFraction(period.getUnadjustedStartDate(), period.getUnadjustedEndDate())).isEqualTo(0.5);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_findPeriod() {
    ResolvedFixedCouponBond test = sut();
    ImmutableList<FixedCouponBondPaymentPeriod> payments = test.getPeriodicPayments();
    assertThat(test.findPeriod(test.getUnadjustedStartDate())).isEqualTo(Optional.of(payments.get(0)));
    assertThat(test.findPeriod(test.getUnadjustedEndDate().minusDays(1)))
        .isEqualTo(Optional.of(payments.get(payments.size() - 1)));
    assertThat(test.findPeriod(LocalDate.MIN)).isEqualTo(Optional.empty());
    assertThat(test.findPeriod(LocalDate.MAX)).isEqualTo(Optional.empty());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  @Test
  public void coverage_builder() {
    ResolvedFixedCouponBond test = sut();
    test.toBuilder().periodicPayments(test.getPeriodicPayments().toArray(new FixedCouponBondPaymentPeriod[0])).build();
  }

  @Test
  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static ResolvedFixedCouponBond sut() {
    return FixedCouponBondTest.sut().resolve(REF_DATA);
  }

  static ResolvedFixedCouponBond sut2() {
    return FixedCouponBondTest.sut2().resolve(REF_DATA);
  }

}
