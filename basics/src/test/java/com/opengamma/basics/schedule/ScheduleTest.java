/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.schedule;

import static com.opengamma.basics.schedule.Frequency.P1M;
import static com.opengamma.basics.schedule.Frequency.P2M;
import static com.opengamma.basics.schedule.RollConventions.DAY_17;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrows;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static java.time.Month.AUGUST;
import static java.time.Month.JULY;
import static java.time.Month.SEPTEMBER;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link Schedule}.
 */
@Test
public class ScheduleTest {

  private static final LocalDate JUL_04 = date(2014, JULY, 4);
  private static final LocalDate JUL_17 = date(2014, JULY, 17);
  private static final LocalDate AUG_17 = date(2014, AUGUST, 17);
  private static final LocalDate SEP_17 = date(2014, SEPTEMBER, 17);
  private static final SchedulePeriod PERIOD1 =
      SchedulePeriod.of(SchedulePeriodType.INITIAL, JUL_04, JUL_17, JUL_04, JUL_17, P1M, DAY_17);
  private static final SchedulePeriod PERIOD2 =
      SchedulePeriod.of(SchedulePeriodType.NORMAL, JUL_17, AUG_17, JUL_17, AUG_17, P1M, DAY_17);
  private static final SchedulePeriod PERIOD3 =
      SchedulePeriod.of(SchedulePeriodType.FINAL, AUG_17, SEP_17, AUG_17, SEP_17, P1M, DAY_17);
  private static final SchedulePeriod PERIOD_TERM =
      SchedulePeriod.of(SchedulePeriodType.TERM, JUL_17, SEP_17, JUL_17, SEP_17, P2M, DAY_17);

  //-------------------------------------------------------------------------
  public void test_of_size0() {
    assertThrows(() -> Schedule.of(ImmutableList.of()), IllegalArgumentException.class);
  }

  public void test_of_size1() {
    Schedule test = Schedule.of(ImmutableList.of(PERIOD1));
    assertEquals(test.size(), 1);
    assertEquals(test.getPeriods(), ImmutableList.of(PERIOD1));
    assertEquals(test.getPeriod(0), PERIOD1);
    assertEquals(test.getFirstPeriod(), PERIOD1);
    assertEquals(test.getLastPeriod(), PERIOD1);
    assertThrows(() -> test.getPeriod(1), IndexOutOfBoundsException.class);
  }

  public void test_of_size2() {
    Schedule test = Schedule.of(ImmutableList.of(PERIOD1, PERIOD2));
    assertEquals(test.size(), 2);
    assertEquals(test.getPeriods(), ImmutableList.of(PERIOD1, PERIOD2));
    assertEquals(test.getPeriod(0), PERIOD1);
    assertEquals(test.getPeriod(1), PERIOD2);
    assertEquals(test.getFirstPeriod(), PERIOD1);
    assertEquals(test.getLastPeriod(), PERIOD2);
    assertThrows(() -> test.getPeriod(2), IndexOutOfBoundsException.class);
  }

  public void test_of_size3() {
    Schedule test = Schedule.of(ImmutableList.of(PERIOD1, PERIOD2, PERIOD3));
    assertEquals(test.size(), 3);
    assertEquals(test.getPeriods(), ImmutableList.of(PERIOD1, PERIOD2, PERIOD3));
    assertEquals(test.getPeriod(0), PERIOD1);
    assertEquals(test.getPeriod(1), PERIOD2);
    assertEquals(test.getPeriod(2), PERIOD3);
    assertEquals(test.getFirstPeriod(), PERIOD1);
    assertEquals(test.getLastPeriod(), PERIOD3);
    assertThrows(() -> test.getPeriod(3), IndexOutOfBoundsException.class);
  }

  //-------------------------------------------------------------------------
  public void test_getFrequency() {
    Schedule testNormal = Schedule.of(ImmutableList.of(PERIOD1, PERIOD2, PERIOD3));
    assertEquals(testNormal.getFrequency(), P1M);
    Schedule testTerm = Schedule.of(ImmutableList.of(PERIOD_TERM));
    assertEquals(testTerm.getFrequency(), P2M);
  }

  //-------------------------------------------------------------------------
  public void coverage_builder() {
    Schedule.Builder builder = Schedule.builder();
    builder
      .periods(ImmutableList.of(PERIOD1))
      .build();
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Schedule test = Schedule.of(ImmutableList.of(PERIOD1, PERIOD2));
    coverImmutableBean(test);
  }

  public void test_serialization() {
    Schedule test = Schedule.of(ImmutableList.of(PERIOD1, PERIOD2));
    assertSerialization(test);
  }

}
