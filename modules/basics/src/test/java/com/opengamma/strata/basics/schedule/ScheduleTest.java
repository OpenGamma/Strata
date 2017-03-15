/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import static com.opengamma.strata.basics.schedule.Frequency.P1M;
import static com.opengamma.strata.basics.schedule.Frequency.P2M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.TERM;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_17;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static java.time.Month.AUGUST;
import static java.time.Month.DECEMBER;
import static java.time.Month.JULY;
import static java.time.Month.JUNE;
import static java.time.Month.NOVEMBER;
import static java.time.Month.OCTOBER;
import static java.time.Month.SEPTEMBER;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link Schedule}.
 */
@Test
public class ScheduleTest {

  private static final LocalDate JUN_15 = date(2014, JUNE, 15);
  private static final LocalDate JUN_16 = date(2014, JUNE, 16);
  private static final LocalDate JUL_04 = date(2014, JULY, 4);
  private static final LocalDate JUL_16 = date(2014, JULY, 16);
  private static final LocalDate JUL_17 = date(2014, JULY, 17);
  private static final LocalDate AUG_17 = date(2014, AUGUST, 17);
  private static final LocalDate SEP_17 = date(2014, SEPTEMBER, 17);
  private static final LocalDate SEP_30 = date(2014, SEPTEMBER, 30);
  private static final LocalDate OCT_15 = date(2014, OCTOBER, 15);
  private static final LocalDate OCT_17 = date(2014, OCTOBER, 17);
  private static final LocalDate NOV_17 = date(2014, NOVEMBER, 17);
  private static final LocalDate DEC_17 = date(2014, DECEMBER, 17);

  private static final SchedulePeriod P1_STUB = SchedulePeriod.of(JUL_04, JUL_17);
  private static final SchedulePeriod P2_NORMAL = SchedulePeriod.of(JUL_17, AUG_17);
  private static final SchedulePeriod P3_NORMAL = SchedulePeriod.of(AUG_17, SEP_17);
  private static final SchedulePeriod P4_STUB = SchedulePeriod.of(SEP_17, SEP_30);
  private static final SchedulePeriod P4_NORMAL = SchedulePeriod.of(SEP_17, OCT_17);
  private static final SchedulePeriod P5_NORMAL = SchedulePeriod.of(OCT_17, NOV_17);
  private static final SchedulePeriod P6_NORMAL = SchedulePeriod.of(NOV_17, DEC_17);

  private static final SchedulePeriod P1_3 = SchedulePeriod.of(JUL_04, SEP_17);
  private static final SchedulePeriod P2_3 = SchedulePeriod.of(JUL_17, SEP_17);
  private static final SchedulePeriod P3_4 = SchedulePeriod.of(AUG_17, OCT_17);
  private static final SchedulePeriod P4_5 = SchedulePeriod.of(SEP_17, NOV_17);
  private static final SchedulePeriod P5_6 = SchedulePeriod.of(OCT_17, DEC_17);

  private static final SchedulePeriod P2_4 = SchedulePeriod.of(JUL_17, OCT_17);
  private static final SchedulePeriod P4_6 = SchedulePeriod.of(SEP_17, DEC_17);

  //-------------------------------------------------------------------------
  public void test_of_size0() {
    assertThrowsIllegalArg(() -> Schedule.builder().periods(ImmutableList.of()));
  }

  public void test_ofTerm() {
    Schedule test = Schedule.ofTerm(P1_STUB);
    assertEquals(test.size(), 1);
    assertEquals(test.isTerm(), true);
    assertEquals(test.getFrequency(), TERM);
    assertEquals(test.getRollConvention(), RollConventions.NONE);
    assertEquals(test.isEndOfMonthConvention(), false);
    assertEquals(test.getPeriods(), ImmutableList.of(P1_STUB));
    assertEquals(test.getPeriod(0), P1_STUB);
    assertEquals(test.getStartDate(), P1_STUB.getStartDate());
    assertEquals(test.getEndDate(), P1_STUB.getEndDate());
    assertEquals(test.getFirstPeriod(), P1_STUB);
    assertEquals(test.getLastPeriod(), P1_STUB);
    assertEquals(test.getInitialStub(), Optional.empty());
    assertEquals(test.getFinalStub(), Optional.empty());
    assertEquals(test.getRegularPeriods(), ImmutableList.of(P1_STUB));
    assertThrows(() -> test.getPeriod(1), IndexOutOfBoundsException.class);
  }

  public void test_of_size2_initialStub() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB, P2_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertEquals(test.size(), 2);
    assertEquals(test.isTerm(), false);
    assertEquals(test.getFrequency(), P1M);
    assertEquals(test.getRollConvention(), DAY_17);
    assertEquals(test.isEndOfMonthConvention(), false);
    assertEquals(test.getPeriods(), ImmutableList.of(P1_STUB, P2_NORMAL));
    assertEquals(test.getPeriod(0), P1_STUB);
    assertEquals(test.getPeriod(1), P2_NORMAL);
    assertEquals(test.getStartDate(), P1_STUB.getStartDate());
    assertEquals(test.getEndDate(), P2_NORMAL.getEndDate());
    assertEquals(test.getFirstPeriod(), P1_STUB);
    assertEquals(test.getLastPeriod(), P2_NORMAL);
    assertEquals(test.getInitialStub(), Optional.of(P1_STUB));
    assertEquals(test.getFinalStub(), Optional.empty());
    assertEquals(test.getRegularPeriods(), ImmutableList.of(P2_NORMAL));
    assertThrows(() -> test.getPeriod(2), IndexOutOfBoundsException.class);
  }

  public void test_of_size2_noStub() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertEquals(test.size(), 2);
    assertEquals(test.isTerm(), false);
    assertEquals(test.getFrequency(), P1M);
    assertEquals(test.getRollConvention(), DAY_17);
    assertEquals(test.isEndOfMonthConvention(), false);
    assertEquals(test.getPeriods(), ImmutableList.of(P2_NORMAL, P3_NORMAL));
    assertEquals(test.getPeriod(0), P2_NORMAL);
    assertEquals(test.getPeriod(1), P3_NORMAL);
    assertEquals(test.getStartDate(), P2_NORMAL.getStartDate());
    assertEquals(test.getEndDate(), P3_NORMAL.getEndDate());
    assertEquals(test.getFirstPeriod(), P2_NORMAL);
    assertEquals(test.getLastPeriod(), P3_NORMAL);
    assertEquals(test.getInitialStub(), Optional.empty());
    assertEquals(test.getFinalStub(), Optional.empty());
    assertEquals(test.getRegularPeriods(), ImmutableList.of(P2_NORMAL, P3_NORMAL));
    assertThrows(() -> test.getPeriod(2), IndexOutOfBoundsException.class);
  }

  public void test_of_size2_finalStub() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P3_NORMAL, P4_STUB))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertEquals(test.size(), 2);
    assertEquals(test.isTerm(), false);
    assertEquals(test.getFrequency(), P1M);
    assertEquals(test.getRollConvention(), DAY_17);
    assertEquals(test.isEndOfMonthConvention(), false);
    assertEquals(test.getPeriods(), ImmutableList.of(P3_NORMAL, P4_STUB));
    assertEquals(test.getPeriod(0), P3_NORMAL);
    assertEquals(test.getPeriod(1), P4_STUB);
    assertEquals(test.getStartDate(), P3_NORMAL.getStartDate());
    assertEquals(test.getEndDate(), P4_STUB.getEndDate());
    assertEquals(test.getFirstPeriod(), P3_NORMAL);
    assertEquals(test.getLastPeriod(), P4_STUB);
    assertEquals(test.getInitialStub(), Optional.empty());
    assertEquals(test.getFinalStub(), Optional.of(P4_STUB));
    assertEquals(test.getRegularPeriods(), ImmutableList.of(P3_NORMAL));
    assertThrows(() -> test.getPeriod(2), IndexOutOfBoundsException.class);
  }

  public void test_of_size3_initialStub() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB, P2_NORMAL, P3_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertEquals(test.size(), 3);
    assertEquals(test.isTerm(), false);
    assertEquals(test.getFrequency(), P1M);
    assertEquals(test.getRollConvention(), DAY_17);
    assertEquals(test.isEndOfMonthConvention(), false);
    assertEquals(test.getPeriods(), ImmutableList.of(P1_STUB, P2_NORMAL, P3_NORMAL));
    assertEquals(test.getPeriod(0), P1_STUB);
    assertEquals(test.getPeriod(1), P2_NORMAL);
    assertEquals(test.getPeriod(2), P3_NORMAL);
    assertEquals(test.getStartDate(), P1_STUB.getStartDate());
    assertEquals(test.getEndDate(), P3_NORMAL.getEndDate());
    assertEquals(test.getFirstPeriod(), P1_STUB);
    assertEquals(test.getLastPeriod(), P3_NORMAL);
    assertEquals(test.getInitialStub(), Optional.of(P1_STUB));
    assertEquals(test.getFinalStub(), Optional.empty());
    assertEquals(test.getRegularPeriods(), ImmutableList.of(P2_NORMAL, P3_NORMAL));
    assertThrows(() -> test.getPeriod(3), IndexOutOfBoundsException.class);
  }

  public void test_of_size4_bothStubs() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB, P2_NORMAL, P3_NORMAL, P4_STUB))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertEquals(test.size(), 4);
    assertEquals(test.isTerm(), false);
    assertEquals(test.getFrequency(), P1M);
    assertEquals(test.getRollConvention(), DAY_17);
    assertEquals(test.isEndOfMonthConvention(), false);
    assertEquals(test.getPeriods(), ImmutableList.of(P1_STUB, P2_NORMAL, P3_NORMAL, P4_STUB));
    assertEquals(test.getPeriod(0), P1_STUB);
    assertEquals(test.getPeriod(1), P2_NORMAL);
    assertEquals(test.getPeriod(2), P3_NORMAL);
    assertEquals(test.getPeriod(3), P4_STUB);
    assertEquals(test.getStartDate(), P1_STUB.getStartDate());
    assertEquals(test.getEndDate(), P4_STUB.getEndDate());
    assertEquals(test.getFirstPeriod(), P1_STUB);
    assertEquals(test.getLastPeriod(), P4_STUB);
    assertEquals(test.getInitialStub(), Optional.of(P1_STUB));
    assertEquals(test.getFinalStub(), Optional.of(P4_STUB));
    assertEquals(test.getRegularPeriods(), ImmutableList.of(P2_NORMAL, P3_NORMAL));
    assertThrows(() -> test.getPeriod(4), IndexOutOfBoundsException.class);
  }

  //-------------------------------------------------------------------------
  public void test_isEndOfMonthConvention_eom() {
    // schedule doesn't make sense, but test only requires roll convention of EOM
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL))
        .frequency(P1M)
        .rollConvention(RollConventions.EOM)
        .build();
    assertEquals(test.isEndOfMonthConvention(), true);
  }

  //-------------------------------------------------------------------------
  public void test_getPeriodEndDate() {
    // schedule doesn't make sense, but test only requires roll convention of EOM
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertEquals(test.getPeriodEndDate(P2_NORMAL.getStartDate()), P2_NORMAL.getEndDate());
    assertEquals(test.getPeriodEndDate(P2_NORMAL.getStartDate().plusDays(1)), P2_NORMAL.getEndDate());
    assertEquals(test.getPeriodEndDate(P3_NORMAL.getStartDate()), P3_NORMAL.getEndDate());
    assertEquals(test.getPeriodEndDate(P3_NORMAL.getStartDate().plusDays(1)), P3_NORMAL.getEndDate());
    assertThrowsIllegalArg(() -> test.getPeriodEndDate(P2_NORMAL.getStartDate().minusDays(1)));
  }

  //-------------------------------------------------------------------------
  public void test_mergeToTerm() {
    Schedule testNormal = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB, P2_NORMAL, P3_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertEquals(testNormal.mergeToTerm(), Schedule.ofTerm(P1_3));
    assertEquals(testNormal.mergeToTerm().mergeToTerm(), Schedule.ofTerm(P1_3));
  }

  //-------------------------------------------------------------------------
  public void test_mergeRegular_group2_within2_initialStub() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB, P2_NORMAL, P3_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB, P2_3))
        .frequency(P2M)
        .rollConvention(DAY_17)
        .build();
    assertEquals(test.mergeRegular(2, true), expected);
    assertEquals(test.mergeRegular(2, false), expected);
  }

  public void test_mergeRegular_group2_within2_noStub() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(P2_3))
        .frequency(P2M)
        .rollConvention(DAY_17)
        .build();
    assertEquals(test.mergeRegular(2, true), expected);
    assertEquals(test.mergeRegular(2, false), expected);
  }

  public void test_mergeRegular_group2_within2_finalStub() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_STUB))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(P2_3, P4_STUB))
        .frequency(P2M)
        .rollConvention(DAY_17)
        .build();
    assertEquals(test.mergeRegular(2, true), expected);
    assertEquals(test.mergeRegular(2, false), expected);
  }

  public void test_mergeRegular_group2_within3_forwards() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(P2_3, P4_NORMAL))
        .frequency(P2M)
        .rollConvention(DAY_17)
        .build();
    assertEquals(test.mergeRegular(2, true), expected);
  }

  public void test_mergeRegular_group2_within3_backwards() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_4))
        .frequency(P2M)
        .rollConvention(DAY_17)
        .build();
    assertEquals(test.mergeRegular(2, false), expected);
  }

  public void test_mergeRegular_group2_within5_forwards() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(P2_3, P4_5, P6_NORMAL))
        .frequency(P2M)
        .rollConvention(DAY_17)
        .build();
    assertEquals(test.mergeRegular(2, true), expected);
  }

  public void test_mergeRegular_group2_within5_backwards() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_4, P5_6))
        .frequency(P2M)
        .rollConvention(DAY_17)
        .build();
    assertEquals(test.mergeRegular(2, false), expected);
  }

  public void test_mergeRegular_group3_within5_forwards() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(P2_4, P5_6))
        .frequency(P3M)
        .rollConvention(DAY_17)
        .build();
    assertEquals(test.mergeRegular(3, true), expected);
  }

  public void test_mergeRegular_group3_within5_backwards() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(P2_3, P4_6))
        .frequency(P3M)
        .rollConvention(DAY_17)
        .build();
    assertEquals(test.mergeRegular(3, false), expected);
  }

  public void test_mergeRegular_termNoChange() {
    Schedule test = Schedule.ofTerm(P1_STUB);
    assertEquals(test.mergeRegular(2, true), test);
    assertEquals(test.mergeRegular(2, false), test);
  }

  public void test_mergeRegular_groupSizeOneNoChange() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertEquals(test.mergeRegular(1, true), test);
    assertEquals(test.mergeRegular(1, false), test);
  }

  public void test_mergeRegular_groupSizeInvalid() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertThrowsIllegalArg(() -> test.mergeRegular(0, true));
    assertThrowsIllegalArg(() -> test.mergeRegular(0, false));
    assertThrowsIllegalArg(() -> test.mergeRegular(-1, true));
    assertThrowsIllegalArg(() -> test.mergeRegular(-1, false));
  }

  //-------------------------------------------------------------------------
  public void test_toAdjusted() {
    SchedulePeriod period1 = SchedulePeriod.of(JUN_15, SEP_17);
    SchedulePeriod period2 = SchedulePeriod.of(SEP_17, SEP_30);
    Schedule test = Schedule.builder()
        .periods(period1, period2)
        .frequency(P3M)
        .rollConvention(DAY_17)
        .build();
    assertEquals(test.toAdjusted(date -> date), test);
    assertEquals(test.toAdjusted(date -> date.equals(JUN_15) ? JUN_16 : date), Schedule.builder()
        .periods(SchedulePeriod.of(JUN_16, SEP_17, JUN_15, SEP_17), period2)
        .frequency(P3M)
        .rollConvention(DAY_17)
        .build());
  }

  public void test_toUnadjusted() {
    SchedulePeriod a = SchedulePeriod.of(JUL_17, OCT_17, JUL_16, OCT_15);
    SchedulePeriod b = SchedulePeriod.of(JUL_16, OCT_15, JUL_16, OCT_15);
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(a))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build()
        .toUnadjusted();
    Schedule expected = Schedule.builder()
        .periods(ImmutableList.of(b))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage_equals() {
    Schedule a = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule b = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    Schedule c = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P3M)
        .rollConvention(DAY_17)
        .build();
    Schedule d = Schedule.builder()
        .periods(ImmutableList.of(P2_NORMAL, P3_NORMAL, P4_NORMAL, P5_NORMAL, P6_NORMAL))
        .frequency(P1M)
        .rollConvention(RollConventions.DAY_1)
        .build();
    assertEquals(a.equals(a), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(c), false);
    assertEquals(a.equals(d), false);
  }

  //-------------------------------------------------------------------------
  public void coverage_builder() {
    Schedule.Builder builder = Schedule.builder();
    builder
        .periods(P1_STUB)
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB, P2_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    coverImmutableBean(test);
  }

  public void test_serialization() {
    Schedule test = Schedule.builder()
        .periods(ImmutableList.of(P1_STUB, P2_NORMAL))
        .frequency(P1M)
        .rollConvention(DAY_17)
        .build();
    assertSerialization(test);
  }

}
