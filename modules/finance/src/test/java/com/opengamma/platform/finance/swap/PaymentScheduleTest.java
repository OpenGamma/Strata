/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.finance.swap;

import static com.opengamma.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.basics.schedule.Frequency.P1M;
import static com.opengamma.basics.schedule.Frequency.P2M;
import static com.opengamma.basics.schedule.Frequency.P3M;
import static com.opengamma.basics.schedule.Frequency.TERM;
import static com.opengamma.basics.schedule.RollConventions.DAY_5;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.coverBeanEquals;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static com.opengamma.platform.finance.swap.CompoundingMethod.NONE;
import static com.opengamma.platform.finance.swap.CompoundingMethod.STRAIGHT;
import static com.opengamma.platform.finance.swap.PaymentRelativeTo.PERIOD_END;
import static com.opengamma.platform.finance.swap.PaymentRelativeTo.PERIOD_START;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.basics.date.DaysAdjustment;
import com.opengamma.basics.schedule.RollConventions;
import com.opengamma.basics.schedule.Schedule;
import com.opengamma.basics.schedule.SchedulePeriod;

/**
 * Test.
 */
@Test
public class PaymentScheduleTest {

  private static final LocalDate DATE_01_05 = date(2014, 1, 5);
  private static final LocalDate DATE_01_06 = date(2014, 1, 6);
  private static final LocalDate DATE_01_08 = date(2014, 1, 8);
  private static final LocalDate DATE_02_05 = date(2014, 2, 5);
  private static final LocalDate DATE_03_05 = date(2014, 3, 5);
  private static final LocalDate DATE_04_04 = date(2014, 4, 4);
  private static final LocalDate DATE_04_05 = date(2014, 4, 5);
  private static final LocalDate DATE_04_07 = date(2014, 4, 7);
  private static final LocalDate DATE_05_05 = date(2014, 5, 5);
  private static final LocalDate DATE_05_06 = date(2014, 5, 6);

  private static final SchedulePeriod ACCRUAL1STUB = SchedulePeriod.of(DATE_01_08, DATE_02_05, DATE_01_08, DATE_02_05);
  private static final SchedulePeriod ACCRUAL1 = SchedulePeriod.of(DATE_01_06, DATE_02_05, DATE_01_05, DATE_02_05);
  private static final SchedulePeriod ACCRUAL2 = SchedulePeriod.of(DATE_02_05, DATE_03_05, DATE_02_05, DATE_03_05);
  private static final SchedulePeriod ACCRUAL3 = SchedulePeriod.of(DATE_03_05, DATE_04_07, DATE_03_05, DATE_04_05);
  private static final SchedulePeriod ACCRUAL4 = SchedulePeriod.of(DATE_04_07, DATE_05_06, DATE_04_05, DATE_05_05);
  private static final SchedulePeriod ACCRUAL3STUB = SchedulePeriod.of(DATE_03_05, DATE_04_04, DATE_03_05, DATE_04_04);
  private static final Schedule ACCRUAL_SCHEDULE_SINGLE = Schedule.builder()
      .periods(ACCRUAL1)
      .frequency(P1M)
      .rollConvention(RollConventions.DAY_5)
      .build();
  private static final Schedule ACCRUAL_SCHEDULE_TERM = Schedule.builder()
      .periods(SchedulePeriod.of(DATE_01_06, DATE_04_07, DATE_01_05, DATE_04_05))
      .frequency(TERM)
      .rollConvention(RollConventions.NONE)
      .build();
  private static final Schedule ACCRUAL_SCHEDULE = Schedule.builder()
      .periods(ACCRUAL1, ACCRUAL2, ACCRUAL3)
      .frequency(P1M)
      .rollConvention(DAY_5)
      .build();
  private static final Schedule ACCRUAL_SCHEDULE_STUBS = Schedule.builder()
      .periods(ACCRUAL1STUB, ACCRUAL2, ACCRUAL3STUB)
      .frequency(P1M)
      .rollConvention(DAY_5)
      .build();
  private static final Schedule ACCRUAL_SCHEDULE_INITIAL_STUB = Schedule.builder()
      .periods(ACCRUAL1STUB, ACCRUAL2, ACCRUAL3, ACCRUAL4)
      .frequency(P1M)
      .rollConvention(DAY_5)
      .build();
  private static final Schedule ACCRUAL_SCHEDULE_FINAL_STUB = Schedule.builder()
      .periods(ACCRUAL1, ACCRUAL2, ACCRUAL3STUB)
      .frequency(P1M)
      .rollConvention(DAY_5)
      .build();

  //-------------------------------------------------------------------------
  public void test_builder_ensureDefaults() {
    PaymentSchedule test = PaymentSchedule.builder()
        .paymentFrequency(P1M)
        .paymentOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .build();
    assertEquals(test.getPaymentFrequency(), P1M);
    assertEquals(test.getPaymentOffset(), DaysAdjustment.ofBusinessDays(2, GBLO));
    assertEquals(test.getPaymentRelativeTo(), PERIOD_END);
    assertEquals(test.getCompoundingMethod(), NONE);
  }

  //-------------------------------------------------------------------------
  public void test_createSchedule_sameFrequency() {
    PaymentSchedule test = PaymentSchedule.builder()
        .paymentFrequency(P1M)
        .paymentOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .build();
    Schedule schedule = test.createSchedule(ACCRUAL_SCHEDULE);
    assertEquals(schedule, ACCRUAL_SCHEDULE);
  }

  public void test_createSchedule_singleAccrualPeriod() {
    PaymentSchedule test = PaymentSchedule.builder()
        .paymentFrequency(P1M)
        .paymentOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .build();
    Schedule schedule = test.createSchedule(ACCRUAL_SCHEDULE_SINGLE);
    assertEquals(schedule, ACCRUAL_SCHEDULE_SINGLE);
  }

  public void test_createSchedule_term() {
    PaymentSchedule test = PaymentSchedule.builder()
        .paymentFrequency(TERM)
        .paymentOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .build();
    Schedule schedule = test.createSchedule(ACCRUAL_SCHEDULE);
    assertEquals(schedule, ACCRUAL_SCHEDULE_TERM);
  }

  public void test_createSchedule_fullMerge() {
    PaymentSchedule test = PaymentSchedule.builder()
        .paymentFrequency(P3M)
        .paymentOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .build();
    Schedule schedule = test.createSchedule(ACCRUAL_SCHEDULE);
    Schedule expected = Schedule.builder()
        .periods(SchedulePeriod.of(DATE_01_06, DATE_04_07, DATE_01_05, DATE_04_05))
        .frequency(P3M)
        .rollConvention(DAY_5)
        .build();
    assertEquals(schedule, expected);
  }

  public void test_createSchedule_partMergeForwards() {
    PaymentSchedule test = PaymentSchedule.builder()
        .paymentFrequency(P2M)
        .paymentOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .build();
    Schedule schedule = test.createSchedule(ACCRUAL_SCHEDULE);
    Schedule expected = Schedule.builder()
        .periods(
            SchedulePeriod.of(DATE_01_06, DATE_03_05, DATE_01_05, DATE_03_05),
            SchedulePeriod.of(DATE_03_05, DATE_04_07, DATE_03_05, DATE_04_05))
        .frequency(P2M)
        .rollConvention(DAY_5)
        .build();
    assertEquals(schedule, expected);
  }

  public void test_createSchedule_initialStubPartMergeBackwards() {
    PaymentSchedule test = PaymentSchedule.builder()
        .paymentFrequency(P2M)
        .paymentOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .build();
    Schedule schedule = test.createSchedule(ACCRUAL_SCHEDULE_INITIAL_STUB);
    Schedule expected = Schedule.builder()
        .periods(
            ACCRUAL1STUB,
            SchedulePeriod.of(DATE_02_05, DATE_03_05, DATE_02_05, DATE_03_05),
            SchedulePeriod.of(DATE_03_05, DATE_05_06, DATE_03_05, DATE_05_05))
        .frequency(P2M)
        .rollConvention(DAY_5)
        .build();
    assertEquals(schedule, expected);
  }

  public void test_createSchedule_finalStubFullMerge() {
    PaymentSchedule test = PaymentSchedule.builder()
        .paymentFrequency(P2M)
        .paymentOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .build();
    Schedule schedule = test.createSchedule(ACCRUAL_SCHEDULE_FINAL_STUB);
    Schedule expected = Schedule.builder()
        .periods(
            SchedulePeriod.of(DATE_01_06, DATE_03_05, DATE_01_05, DATE_03_05),
            ACCRUAL3STUB)
        .frequency(P2M)
        .rollConvention(DAY_5)
        .build();
    assertEquals(schedule, expected);
  }

  public void test_createSchedule_dualStub() {
    PaymentSchedule test = PaymentSchedule.builder()
        .paymentFrequency(P2M)
        .paymentOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .build();
    Schedule schedule = test.createSchedule(ACCRUAL_SCHEDULE_STUBS);
    assertEquals(schedule, ACCRUAL_SCHEDULE_STUBS.toBuilder().frequency(P2M).build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    PaymentSchedule test = PaymentSchedule.builder()
        .paymentFrequency(P1M)
        .paymentOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .build();
    coverImmutableBean(test);
    PaymentSchedule test2 = PaymentSchedule.builder()
        .paymentFrequency(P3M)
        .paymentOffset(DaysAdjustment.ofBusinessDays(3, GBLO))
        .paymentRelativeTo(PERIOD_START)
        .compoundingMethod(STRAIGHT)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    PaymentSchedule test = PaymentSchedule.builder()
        .paymentFrequency(P3M)
        .paymentOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .build();
    assertSerialization(test);
  }

}
