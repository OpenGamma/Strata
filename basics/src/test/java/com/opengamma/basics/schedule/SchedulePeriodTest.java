/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.schedule;

import static com.opengamma.basics.schedule.Frequency.P1M;
import static com.opengamma.basics.schedule.Frequency.P3M;
import static com.opengamma.basics.schedule.RollConventions.DAY_11;
import static com.opengamma.basics.schedule.RollConventions.DAY_17;
import static com.opengamma.basics.schedule.SchedulePeriodType.INITIAL;
import static com.opengamma.basics.schedule.SchedulePeriodType.NORMAL;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrows;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static java.time.Month.JULY;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

/**
 * Test {@link SchedulePeriod}.
 */
@Test
public class SchedulePeriodTest {

  private static final LocalDate JUL_04 = date(2014, JULY, 4);
  private static final LocalDate JUL_05 = date(2014, JULY, 5);
  private static final LocalDate JUL_17 = date(2014, JULY, 17);
  private static final LocalDate JUL_18 = date(2014, JULY, 18);

  //-------------------------------------------------------------------------
  public void test_of_null() {
    assertThrows(() -> SchedulePeriod.of(null, JUL_05, JUL_18, JUL_04, JUL_17, P1M, DAY_17), IllegalArgumentException.class);
    assertThrows(() -> SchedulePeriod.of(INITIAL, JUL_05, JUL_18, null, JUL_17, P1M, DAY_17), IllegalArgumentException.class);
    assertThrows(() -> SchedulePeriod.of(INITIAL, JUL_05, JUL_18, JUL_04, null, P1M, DAY_17), IllegalArgumentException.class);
    assertThrows(() -> SchedulePeriod.of(INITIAL, null, JUL_18, JUL_04, JUL_17, P1M, DAY_17), IllegalArgumentException.class);
    assertThrows(() -> SchedulePeriod.of(INITIAL, JUL_05, null, JUL_04, JUL_17, P1M, DAY_17), IllegalArgumentException.class);
    assertThrows(() -> SchedulePeriod.of(INITIAL, JUL_05, JUL_18, JUL_04, JUL_17, null, DAY_17), IllegalArgumentException.class);
    assertThrows(() -> SchedulePeriod.of(INITIAL, JUL_05, JUL_18, JUL_04, JUL_17, P1M, null), IllegalArgumentException.class);
  }

  public void test_of_all() {
    SchedulePeriod test = SchedulePeriod.of(INITIAL, JUL_05, JUL_18, JUL_04, JUL_17, P1M, DAY_17);
    assertEquals(test.getType(), INITIAL);
    assertEquals(test.getStartDate(), JUL_05);
    assertEquals(test.getEndDate(), JUL_18);
    assertEquals(test.getUnadjustedStartDate(), JUL_04);
    assertEquals(test.getUnadjustedEndDate(), JUL_17);
    assertEquals(test.getFrequency(), P1M);
    assertEquals(test.getRollConvention(), DAY_17);
  }

  public void test_of_noUnadjusted() {
    SchedulePeriod test = SchedulePeriod.of(INITIAL, JUL_05, JUL_18, P1M, DAY_17);
    assertEquals(test.getType(), INITIAL);
    assertEquals(test.getStartDate(), JUL_05);
    assertEquals(test.getEndDate(), JUL_18);
    assertEquals(test.getUnadjustedStartDate(), JUL_05);
    assertEquals(test.getUnadjustedEndDate(), JUL_18);
    assertEquals(test.getFrequency(), P1M);
    assertEquals(test.getRollConvention(), DAY_17);
  }

  //-------------------------------------------------------------------------
  public void coverage_equals() {
    SchedulePeriod a1 = SchedulePeriod.of(INITIAL, JUL_05, JUL_18, JUL_04, JUL_17, P1M, DAY_17);
    SchedulePeriod a2 = SchedulePeriod.of(INITIAL, JUL_05, JUL_18, JUL_04, JUL_17, P1M, DAY_17);
    SchedulePeriod b = SchedulePeriod.of(NORMAL, JUL_05, JUL_18, JUL_04, JUL_17, P1M, DAY_17);
    SchedulePeriod c = SchedulePeriod.of(INITIAL, JUL_05, JUL_18, JUL_05, JUL_17, P1M, DAY_17);
    SchedulePeriod d = SchedulePeriod.of(INITIAL, JUL_05, JUL_18, JUL_04, JUL_18, P1M, DAY_17);
    SchedulePeriod e = SchedulePeriod.of(INITIAL, JUL_04, JUL_18, JUL_04, JUL_17, P1M, DAY_17);
    SchedulePeriod f = SchedulePeriod.of(INITIAL, JUL_05, JUL_17, JUL_04, JUL_17, P1M, DAY_17);
    SchedulePeriod g = SchedulePeriod.of(INITIAL, JUL_05, JUL_18, JUL_04, JUL_17, P3M, DAY_17);
    SchedulePeriod h = SchedulePeriod.of(INITIAL, JUL_05, JUL_18, JUL_04, JUL_17, P1M, DAY_11);
    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(c), false);
    assertEquals(a1.equals(d), false);
    assertEquals(a1.equals(e), false);
    assertEquals(a1.equals(f), false);
    assertEquals(a1.equals(g), false);
    assertEquals(a1.equals(h), false);
  }

  //-------------------------------------------------------------------------
  public void coverage_builder() {
    SchedulePeriod.Builder builder = SchedulePeriod.builder();
    builder
      .type(INITIAL)
      .startDate(JUL_05)
      .endDate(JUL_18)
      .unadjustedStartDate(JUL_04)
      .unadjustedEndDate(JUL_17)
      .frequency(P1M)
      .rollConvention(DAY_17)
      .build();
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    SchedulePeriod test = SchedulePeriod.of(INITIAL, JUL_05, JUL_18, JUL_04, JUL_17, P1M, DAY_17);
    coverImmutableBean(test);
  }

  public void test_serialization() {
    SchedulePeriod test = SchedulePeriod.of(INITIAL, JUL_05, JUL_18, JUL_04, JUL_17, P1M, DAY_17);
    assertSerialization(test);
  }

}
