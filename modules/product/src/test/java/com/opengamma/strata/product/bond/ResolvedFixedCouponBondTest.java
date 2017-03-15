/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.schedule.RollConventions;

/**
 * Test {@link ResolvedFixedCouponBond}.
 */
@Test
public class ResolvedFixedCouponBondTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  public void test_getters() {
    ResolvedFixedCouponBond test = sut();
    ImmutableList<FixedCouponBondPaymentPeriod> payments = test.getPeriodicPayments();
    assertEquals(test.getStartDate(), payments.get(0).getStartDate());
    assertEquals(test.getEndDate(), payments.get(payments.size() - 1).getEndDate());
    assertEquals(test.getUnadjustedStartDate(), payments.get(0).getUnadjustedStartDate());
    assertEquals(test.getUnadjustedEndDate(), payments.get(payments.size() - 1).getUnadjustedEndDate());
    assertEquals(test.hasExCouponPeriod(), true);
  }

  //-------------------------------------------------------------------------
  public void test_yearFraction() {
    ResolvedFixedCouponBond test = sut();
    FixedCouponBondPaymentPeriod period = test.getPeriodicPayments().get(0);
    assertEquals(test.yearFraction(period.getUnadjustedStartDate(), period.getUnadjustedEndDate()), period.getYearFraction());
  }

  public void test_yearFraction_scheduleInfo() {
    ResolvedFixedCouponBond base = sut();
    FixedCouponBondPaymentPeriod period = base.getPeriodicPayments().get(0);
    AtomicBoolean eom = new AtomicBoolean(false);
    DayCount dc = new DayCount() {
      @Override
      public double yearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
        assertEquals(scheduleInfo.getStartDate(), base.getUnadjustedStartDate());
        assertEquals(scheduleInfo.getEndDate(), base.getUnadjustedEndDate());
        assertEquals(scheduleInfo.getPeriodEndDate(firstDate), period.getUnadjustedEndDate());
        assertEquals(scheduleInfo.getFrequency(), base.getFrequency());
        assertEquals(scheduleInfo.isEndOfMonthConvention(), eom.get());
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
    assertEquals(test.yearFraction(period.getUnadjustedStartDate(), period.getUnadjustedEndDate()), 0.5);
    // test with EOM=true
    ResolvedFixedCouponBond test2 = test.toBuilder().rollConvention(RollConventions.EOM).build();
    eom.set(true);
    assertEquals(test2.yearFraction(period.getUnadjustedStartDate(), period.getUnadjustedEndDate()), 0.5);
  }

  //-------------------------------------------------------------------------
  public void test_findPeriod() {
    ResolvedFixedCouponBond test = sut();
    ImmutableList<FixedCouponBondPaymentPeriod> payments = test.getPeriodicPayments();
    assertEquals(test.findPeriod(test.getUnadjustedStartDate()), Optional.of(payments.get(0)));
    assertEquals(test.findPeriod(test.getUnadjustedEndDate().minusDays(1)), Optional.of(payments.get(payments.size() - 1)));
    assertEquals(test.findPeriod(LocalDate.MIN), Optional.empty());
    assertEquals(test.findPeriod(LocalDate.MAX), Optional.empty());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void coverage_builder() {
    ResolvedFixedCouponBond test = sut();
    test.toBuilder().periodicPayments(test.getPeriodicPayments().toArray(new FixedCouponBondPaymentPeriod[0])).build();
  }

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
