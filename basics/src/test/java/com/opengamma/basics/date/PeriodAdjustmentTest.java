/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrows;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link PeriodAdjustment}.
 */
@Test
public class PeriodAdjustmentTest {

  private static final HolidayCalendar HOLCAL_NONE = HolidayCalendar.NONE;
  private static final HolidayCalendar HOLCAL_WEEKENDS = HolidayCalendar.WEEKENDS;
  private static final HolidayCalendar HOLCAL_WED_THU = HolidayCalendar.of(ImmutableList.of(), WEDNESDAY, THURSDAY);
  private static final BusinessDayAdjustment BDA_NONE = BusinessDayAdjustment.NONE;
  private static final BusinessDayAdjustment BDA_FOLLOW_WEEKENDS =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, HolidayCalendar.WEEKENDS);
  private static final BusinessDayAdjustment BDA_FOLLOW_WED_THU =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, HOLCAL_WED_THU);

  //-------------------------------------------------------------------------
  public void test_ofCalendarDays1_oneDay() {
    PeriodAdjustment test = PeriodAdjustment.ofCalendarDays(1);
    assertEquals(test.getDays(), 1);
    assertEquals(test.getCalendar(), HOLCAL_NONE);
    assertEquals(test.getAdjustment(), BDA_NONE);
    assertEquals(test.toString(), "1 calendar day");
  }

  public void test_ofCalendarDays1_threeDays() {
    PeriodAdjustment test = PeriodAdjustment.ofCalendarDays(3);
    assertEquals(test.getDays(), 3);
    assertEquals(test.getCalendar(), HOLCAL_NONE);
    assertEquals(test.getAdjustment(), BDA_NONE);
    assertEquals(test.toString(), "3 calendar days");
  }

  public void test_ofCalendarDays1_adjust() {
    PeriodAdjustment test = PeriodAdjustment.ofCalendarDays(2);
    LocalDate base = date(2014, 8, 15);  // Fri
    assertEquals(test.adjust(base), date(2014, 8, 17));  // Sun
  }

  public void test_ofCalendarDays2_oneDay() {
    PeriodAdjustment test = PeriodAdjustment.ofCalendarDays(1, BDA_FOLLOW_WEEKENDS);
    assertEquals(test.getDays(), 1);
    assertEquals(test.getCalendar(), HOLCAL_NONE);
    assertEquals(test.getAdjustment(), BDA_FOLLOW_WEEKENDS);
    assertEquals(test.toString(), "1 calendar day then apply Following using calendar Weekends");
  }

  public void test_ofCalendarDays2_fourDays() {
    PeriodAdjustment test = PeriodAdjustment.ofCalendarDays(4, BDA_FOLLOW_WEEKENDS);
    assertEquals(test.getDays(), 4);
    assertEquals(test.getCalendar(), HOLCAL_NONE);
    assertEquals(test.getAdjustment(), BDA_FOLLOW_WEEKENDS);
    assertEquals(test.toString(), "4 calendar days then apply Following using calendar Weekends");
  }

  public void test_ofCalendarDays2_adjust() {
    PeriodAdjustment test = PeriodAdjustment.ofCalendarDays(2, BDA_FOLLOW_WEEKENDS);
    LocalDate base = date(2014, 8, 15);  // Fri
    assertEquals(test.adjust(base), date(2014, 8, 18));  // Mon
  }

  public void test_ofCalendarDays2_null() {
    assertThrows(() -> PeriodAdjustment.ofCalendarDays(2, null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_ofBusinessDays2_oneDay() {
    PeriodAdjustment test = PeriodAdjustment.ofBusinessDays(1, HOLCAL_WEEKENDS);
    assertEquals(test.getDays(), 1);
    assertEquals(test.getCalendar(), HOLCAL_WEEKENDS);
    assertEquals(test.getAdjustment(), BDA_NONE);
    assertEquals(test.toString(), "1 business day using calendar Weekends");
  }

  public void test_ofBusinessDays2_threeDays() {
    PeriodAdjustment test = PeriodAdjustment.ofBusinessDays(3, HOLCAL_WEEKENDS);
    assertEquals(test.getDays(), 3);
    assertEquals(test.getCalendar(), HOLCAL_WEEKENDS);
    assertEquals(test.getAdjustment(), BDA_NONE);
    assertEquals(test.toString(), "3 business days using calendar Weekends");
  }

  public void test_ofBusinessDays2_adjust() {
    PeriodAdjustment test = PeriodAdjustment.ofBusinessDays(2, HOLCAL_WEEKENDS);
    LocalDate base = date(2014, 8, 15);  // Fri
    assertEquals(test.adjust(base), date(2014, 8, 19));  // Tue
  }

  public void test_ofBusinessDays2_null() {
    assertThrows(() -> PeriodAdjustment.ofBusinessDays(2, null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_ofBusinessDays3_oneDay() {
    PeriodAdjustment test = PeriodAdjustment.ofBusinessDays(1, HOLCAL_WEEKENDS, BDA_FOLLOW_WED_THU);
    assertEquals(test.getDays(), 1);
    assertEquals(test.getCalendar(), HOLCAL_WEEKENDS);
    assertEquals(test.getAdjustment(), BDA_FOLLOW_WED_THU);
    assertEquals(test.toString(), "1 business day using calendar Weekends then apply Following using " +
        "calendar Wed/Thu weekends");
  }

  public void test_ofBusinessDays3_fourDays() {
    PeriodAdjustment test = PeriodAdjustment.ofBusinessDays(4, HOLCAL_WEEKENDS, BDA_FOLLOW_WED_THU);
    assertEquals(test.getDays(), 4);
    assertEquals(test.getCalendar(), HOLCAL_WEEKENDS);
    assertEquals(test.getAdjustment(), BDA_FOLLOW_WED_THU);
    assertEquals(test.toString(), "4 business days using calendar Weekends then apply Following using " +
        "calendar Wed/Thu weekends");
  }

  public void test_ofBusinessDays3_adjust() {
    PeriodAdjustment test = PeriodAdjustment.ofBusinessDays(3, HOLCAL_WEEKENDS, BDA_FOLLOW_WED_THU);
    LocalDate base = date(2014, 8, 15);  // Fri
    assertEquals(test.adjust(base), date(2014, 8, 22));  // Fri (3 days gives Wed, following moves to Fri)
  }

  public void test_ofBusinessDays3_null() {
    assertThrows(() -> PeriodAdjustment.ofBusinessDays(3, null, BDA_FOLLOW_WEEKENDS), IllegalArgumentException.class);
    assertThrows(() -> PeriodAdjustment.ofBusinessDays(3, HOLCAL_WEEKENDS, null), IllegalArgumentException.class);
    assertThrows(() -> PeriodAdjustment.ofBusinessDays(3, null, null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void equals() {
    PeriodAdjustment a = PeriodAdjustment.ofBusinessDays(3, HOLCAL_NONE, BDA_FOLLOW_WEEKENDS);
    PeriodAdjustment b = PeriodAdjustment.ofBusinessDays(4, HOLCAL_NONE, BDA_FOLLOW_WEEKENDS);
    PeriodAdjustment c = PeriodAdjustment.ofBusinessDays(3, HOLCAL_WED_THU, BDA_FOLLOW_WEEKENDS);
    PeriodAdjustment d = PeriodAdjustment.ofBusinessDays(3, HOLCAL_NONE, BDA_FOLLOW_WED_THU);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(c), false);
    assertEquals(a.equals(d), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(PeriodAdjustment.ofCalendarDays(4, BDA_FOLLOW_WEEKENDS));
  }

  public void test_serialization() {
    assertSerialization(PeriodAdjustment.ofCalendarDays(4, BDA_FOLLOW_WEEKENDS));
  }

}
