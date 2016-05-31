/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import static com.opengamma.strata.basics.schedule.Frequency.P1M;
import static com.opengamma.strata.basics.schedule.Frequency.P2M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_18;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static java.time.Month.AUGUST;
import static java.time.Month.JULY;
import static java.time.Month.JUNE;
import static java.time.Month.SEPTEMBER;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCounts;

/**
 * Test {@link SchedulePeriod}.
 */
@Test
public class SchedulePeriodTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate JUN_15 = date(2014, JUNE, 15);  // Sunday
  private static final LocalDate JUN_16 = date(2014, JUNE, 16);
  private static final LocalDate JUN_17 = date(2014, JUNE, 17);
  private static final LocalDate JUN_18 = date(2014, JUNE, 18);
  private static final LocalDate JUL_04 = date(2014, JULY, 4);
  private static final LocalDate JUL_05 = date(2014, JULY, 5);
  private static final LocalDate JUL_17 = date(2014, JULY, 17);
  private static final LocalDate JUL_18 = date(2014, JULY, 18);
  private static final LocalDate AUG_17 = date(2014, AUGUST, 17);  // Sunday
  private static final LocalDate AUG_18 = date(2014, AUGUST, 18);  // Monday
  private static final LocalDate SEP_17 = date(2014, SEPTEMBER, 17);
  private static final double TOLERANCE = 1.0E-6;

  //-------------------------------------------------------------------------
  public void test_of_null() {
    assertThrowsIllegalArg(() -> SchedulePeriod.of(null, JUL_18, JUL_04, JUL_17));
    assertThrowsIllegalArg(() -> SchedulePeriod.of(JUL_05, null, JUL_04, JUL_17));
    assertThrowsIllegalArg(() -> SchedulePeriod.of(JUL_05, JUL_18, null, JUL_17));
    assertThrowsIllegalArg(() -> SchedulePeriod.of(JUL_05, JUL_18, JUL_04, null));
    assertThrowsIllegalArg(() -> SchedulePeriod.of(null, null, null, null));
  }

  public void test_of_all() {
    SchedulePeriod test = SchedulePeriod.of(JUL_05, JUL_18, JUL_04, JUL_17);
    assertEquals(test.getStartDate(), JUL_05);
    assertEquals(test.getEndDate(), JUL_18);
    assertEquals(test.getUnadjustedStartDate(), JUL_04);
    assertEquals(test.getUnadjustedEndDate(), JUL_17);
  }

  public void test_of_noUnadjusted() {
    SchedulePeriod test = SchedulePeriod.of(JUL_05, JUL_18);
    assertEquals(test.getStartDate(), JUL_05);
    assertEquals(test.getEndDate(), JUL_18);
    assertEquals(test.getUnadjustedStartDate(), JUL_05);
    assertEquals(test.getUnadjustedEndDate(), JUL_18);
  }

  public void test_builder_defaults() {
    SchedulePeriod test = SchedulePeriod.builder()
        .startDate(JUL_05)
        .endDate(JUL_18)
        .build();
    assertEquals(test.getStartDate(), JUL_05);
    assertEquals(test.getEndDate(), JUL_18);
    assertEquals(test.getUnadjustedStartDate(), JUL_05);
    assertEquals(test.getUnadjustedEndDate(), JUL_18);
  }

  //-------------------------------------------------------------------------
  public void test_yearFraction() {
    SchedulePeriod test = SchedulePeriod.of(JUN_16, JUL_18, JUN_16, JUL_17);
    Schedule schedule = Schedule.ofTerm(test);
    assertEquals(
        test.yearFraction(DayCounts.ACT_360, schedule),
        DayCounts.ACT_360.yearFraction(JUN_16, JUL_18, schedule),
        TOLERANCE);
  }

  public void test_yearFraction_null() {
    SchedulePeriod test = SchedulePeriod.of(JUN_16, JUL_18, JUN_16, JUL_17);
    Schedule schedule = Schedule.ofTerm(test);
    assertThrowsIllegalArg(() -> test.yearFraction(null, schedule));
    assertThrowsIllegalArg(() -> test.yearFraction(DayCounts.ACT_360, null));
    assertThrowsIllegalArg(() -> test.yearFraction(null, null));
  }

  //-------------------------------------------------------------------------
  public void test_length() {
    assertEquals(SchedulePeriod.of(JUN_16, JUN_18, JUN_16, JUN_18).length(), Period.between(JUN_16, JUN_18));
    assertEquals(SchedulePeriod.of(JUN_16, JUL_18, JUN_16, JUL_17).length(), Period.between(JUN_16, JUL_18));
  }

  //-------------------------------------------------------------------------
  public void test_lengthInDays() {
    assertEquals(SchedulePeriod.of(JUN_16, JUN_18, JUN_16, JUN_18).lengthInDays(), 2);
    assertEquals(SchedulePeriod.of(JUN_16, JUL_18, JUN_16, JUL_17).lengthInDays(), 32);
  }

  //-------------------------------------------------------------------------
  public void test_isRegular() {
    assertEquals(SchedulePeriod.of(JUN_18, JUL_18).isRegular(P1M, DAY_18), true);
    assertEquals(SchedulePeriod.of(JUN_18, JUL_05).isRegular(P1M, DAY_18), false);
    assertEquals(SchedulePeriod.of(JUL_05, JUL_18).isRegular(P1M, DAY_18), false);
    assertEquals(SchedulePeriod.of(JUN_18, JUL_05).isRegular(P2M, DAY_18), false);
  }

  public void test_isRegular_null() {
    SchedulePeriod test = SchedulePeriod.of(JUN_16, JUL_18);
    assertThrowsIllegalArg(() -> test.isRegular(null, DAY_18));
    assertThrowsIllegalArg(() -> test.isRegular(P1M, null));
    assertThrowsIllegalArg(() -> test.isRegular(null, null));
  }

  //-------------------------------------------------------------------------
  public void test_contains() {
    assertEquals(SchedulePeriod.of(JUN_16, JUL_18, JUN_16, JUL_17).contains(JUN_15), false);
    assertEquals(SchedulePeriod.of(JUN_16, JUL_18, JUN_16, JUL_17).contains(JUN_16), true);
    assertEquals(SchedulePeriod.of(JUN_16, JUL_18, JUN_16, JUL_17).contains(JUL_05), true);
    assertEquals(SchedulePeriod.of(JUN_16, JUL_18, JUN_16, JUL_17).contains(JUL_17), true);
    assertEquals(SchedulePeriod.of(JUN_16, JUL_18, JUN_16, JUL_17).contains(JUL_18), false);
  }

  public void test_contains_null() {
    SchedulePeriod test = SchedulePeriod.of(JUN_16, JUL_18);
    assertThrowsIllegalArg(() -> test.contains(null));
  }

  //-------------------------------------------------------------------------
  public void test_subSchedule_1monthIn3Month() {
    SchedulePeriod test = SchedulePeriod.of(JUN_17, SEP_17);
    Schedule schedule = test.subSchedule(P1M, RollConventions.DAY_17, StubConvention.NONE, BusinessDayAdjustment.NONE)
        .createSchedule(REF_DATA);
    assertEquals(schedule.size(), 3);
    assertEquals(schedule.getPeriod(0), SchedulePeriod.of(JUN_17, JUL_17));
    assertEquals(schedule.getPeriod(1), SchedulePeriod.of(JUL_17, AUG_17));
    assertEquals(schedule.getPeriod(2), SchedulePeriod.of(AUG_17, SEP_17));
    assertEquals(schedule.getFrequency(), P1M);
    assertEquals(schedule.getRollConvention(), RollConventions.DAY_17);
  }

  public void test_subSchedule_3monthIn3Month() {
    SchedulePeriod test = SchedulePeriod.of(JUN_17, SEP_17);
    Schedule schedule =
        test.subSchedule(P3M, RollConventions.DAY_17, StubConvention.NONE, BusinessDayAdjustment.NONE)
            .createSchedule(REF_DATA);
    assertEquals(schedule.size(), 1);
    assertEquals(schedule.getPeriod(0), SchedulePeriod.of(JUN_17, SEP_17));
  }

  public void test_subSchedule_2monthIn3Month_shortInitial() {
    SchedulePeriod test = SchedulePeriod.of(JUN_17, SEP_17);
    Schedule schedule =
        test.subSchedule(P2M, RollConventions.DAY_17, StubConvention.SHORT_INITIAL, BusinessDayAdjustment.NONE)
            .createSchedule(REF_DATA);
    assertEquals(schedule.size(), 2);
    assertEquals(schedule.getPeriod(0), SchedulePeriod.of(JUN_17, JUL_17));
    assertEquals(schedule.getPeriod(1), SchedulePeriod.of(JUL_17, SEP_17));
    assertEquals(schedule.getFrequency(), P2M);
    assertEquals(schedule.getRollConvention(), RollConventions.DAY_17);
  }

  public void test_subSchedule_2monthIn3Month_shortFinal() {
    SchedulePeriod test = SchedulePeriod.of(JUN_17, SEP_17);
    Schedule schedule =
        test.subSchedule(P2M, RollConventions.DAY_17, StubConvention.SHORT_FINAL, BusinessDayAdjustment.NONE)
            .createSchedule(REF_DATA);
    assertEquals(schedule.size(), 2);
    assertEquals(schedule.getPeriod(0), SchedulePeriod.of(JUN_17, AUG_17));
    assertEquals(schedule.getPeriod(1), SchedulePeriod.of(AUG_17, SEP_17));
    assertEquals(schedule.getFrequency(), P2M);
    assertEquals(schedule.getRollConvention(), RollConventions.DAY_17);
  }

  //-------------------------------------------------------------------------
  public void test_toAdjusted() {
    SchedulePeriod test1 = SchedulePeriod.of(JUN_15, SEP_17);
    assertEquals(test1.toAdjusted(date -> date), test1);
    assertEquals(test1.toAdjusted(date -> date.equals(JUN_15) ? JUN_16 : date),
        SchedulePeriod.of(JUN_16, SEP_17, JUN_15, SEP_17));
    SchedulePeriod test2 = SchedulePeriod.of(JUN_16, AUG_17);
    assertEquals(test2.toAdjusted(date -> date.equals(AUG_17) ? AUG_18 : date),
        SchedulePeriod.of(JUN_16, AUG_18, JUN_16, AUG_17));
  }

  public void test_toUnadjusted() {
    assertEquals(SchedulePeriod.of(JUN_15, SEP_17).toUnadjusted(), SchedulePeriod.of(JUN_15, SEP_17));
    assertEquals(SchedulePeriod.of(JUN_16, SEP_17, JUN_15, SEP_17).toUnadjusted(), SchedulePeriod.of(JUN_15, SEP_17));
    assertEquals(SchedulePeriod.of(JUN_16, JUL_18, JUN_16, JUL_17).toUnadjusted(), SchedulePeriod.of(JUN_16, JUL_17));
  }

  //-------------------------------------------------------------------------
  public void test_compareTo() {
    SchedulePeriod a = SchedulePeriod.of(JUL_05, JUL_18);
    SchedulePeriod b = SchedulePeriod.of(JUL_04, JUL_18);
    SchedulePeriod c = SchedulePeriod.of(JUL_05, JUL_17);
    assertEquals(a.compareTo(a) == 0, true);
    assertEquals(a.compareTo(b) > 0, true);
    assertEquals(a.compareTo(c) > 0, true);

    assertEquals(b.compareTo(a) < 0, true);
    assertEquals(b.compareTo(b) == 0, true);
    assertEquals(b.compareTo(c) < 0, true);

    assertEquals(c.compareTo(a) < 0, true);
    assertEquals(c.compareTo(b) > 0, true);
    assertEquals(c.compareTo(c) == 0, true);
  }

  //-------------------------------------------------------------------------
  public void coverage_equals() {
    SchedulePeriod a1 = SchedulePeriod.of(JUL_05, JUL_18, JUL_04, JUL_17);
    SchedulePeriod a2 = SchedulePeriod.of(JUL_05, JUL_18, JUL_04, JUL_17);
    SchedulePeriod b = SchedulePeriod.of(JUL_04, JUL_18, JUL_04, JUL_17);
    SchedulePeriod c = SchedulePeriod.of(JUL_05, JUL_17, JUL_04, JUL_17);
    SchedulePeriod d = SchedulePeriod.of(JUL_05, JUL_18, JUL_05, JUL_17);
    SchedulePeriod e = SchedulePeriod.of(JUL_05, JUL_18, JUL_04, JUL_18);
    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(c), false);
    assertEquals(a1.equals(d), false);
    assertEquals(a1.equals(e), false);
  }

  //-------------------------------------------------------------------------
  public void coverage_builder() {
    SchedulePeriod.Builder builder = SchedulePeriod.builder();
    builder
        .startDate(JUL_05)
        .endDate(JUL_18)
        .unadjustedStartDate(JUL_04)
        .unadjustedEndDate(JUL_17)
        .build();
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SchedulePeriod test = SchedulePeriod.of(JUL_05, JUL_18, JUL_04, JUL_17);
    coverImmutableBean(test);
  }

  public void test_serialization() {
    SchedulePeriod test = SchedulePeriod.of(JUL_05, JUL_18, JUL_04, JUL_17);
    assertSerialization(test);
  }

}
