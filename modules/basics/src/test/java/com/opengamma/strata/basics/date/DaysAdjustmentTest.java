/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link DaysAdjustment}.
 */
@Test
public class DaysAdjustmentTest {

  private static final HolidayCalendar HOLCAL_NONE = HolidayCalendars.NO_HOLIDAYS;
  private static final HolidayCalendar HOLCAL_SAT_SUN = HolidayCalendars.SAT_SUN;
  private static final HolidayCalendar HOLCAL_WED_THU =
      ImmutableHolidayCalendar.of("WedThu", ImmutableList.of(), WEDNESDAY, THURSDAY);
  private static final BusinessDayAdjustment BDA_NONE = BusinessDayAdjustment.NONE;
  private static final BusinessDayAdjustment BDA_FOLLOW_SAT_SUN =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, HolidayCalendars.SAT_SUN);
  private static final BusinessDayAdjustment BDA_FOLLOW_WED_THU =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, HOLCAL_WED_THU);

  //-------------------------------------------------------------------------
  public void test_NONE() {
    DaysAdjustment test = DaysAdjustment.NONE;
    assertEquals(test.getDays(), 0);
    assertEquals(test.getCalendar(), HOLCAL_NONE);
    assertEquals(test.getAdjustment(), BDA_NONE);
    assertEquals(test.toString(), "0 calendar days");
  }

  //-------------------------------------------------------------------------
  public void test_ofCalendarDays1_oneDay() {
    DaysAdjustment test = DaysAdjustment.ofCalendarDays(1);
    assertEquals(test.getDays(), 1);
    assertEquals(test.getCalendar(), HOLCAL_NONE);
    assertEquals(test.getAdjustment(), BDA_NONE);
    assertEquals(test.toString(), "1 calendar day");
  }

  public void test_ofCalendarDays1_threeDays() {
    DaysAdjustment test = DaysAdjustment.ofCalendarDays(3);
    assertEquals(test.getDays(), 3);
    assertEquals(test.getCalendar(), HOLCAL_NONE);
    assertEquals(test.getAdjustment(), BDA_NONE);
    assertEquals(test.toString(), "3 calendar days");
  }

  public void test_ofCalendarDays1_adjust() {
    DaysAdjustment test = DaysAdjustment.ofCalendarDays(2);
    LocalDate base = date(2014, 8, 15);  // Fri
    assertEquals(test.adjust(base), date(2014, 8, 17));  // Sun
  }

  public void test_ofCalendarDays2_oneDay() {
    DaysAdjustment test = DaysAdjustment.ofCalendarDays(1, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.getDays(), 1);
    assertEquals(test.getCalendar(), HOLCAL_NONE);
    assertEquals(test.getAdjustment(), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.toString(), "1 calendar day then apply Following using calendar Sat/Sun");
  }

  public void test_ofCalendarDays2_fourDays() {
    DaysAdjustment test = DaysAdjustment.ofCalendarDays(4, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.getDays(), 4);
    assertEquals(test.getCalendar(), HOLCAL_NONE);
    assertEquals(test.getAdjustment(), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.toString(), "4 calendar days then apply Following using calendar Sat/Sun");
  }

  public void test_ofCalendarDays2_adjust() {
    DaysAdjustment test = DaysAdjustment.ofCalendarDays(2, BDA_FOLLOW_SAT_SUN);
    LocalDate base = date(2014, 8, 15);  // Fri
    assertEquals(test.adjust(base), date(2014, 8, 18));  // Mon
  }

  public void test_ofCalendarDays2_null() {
    assertThrowsIllegalArg(() -> DaysAdjustment.ofCalendarDays(2, null));
  }

  //-------------------------------------------------------------------------
  public void test_ofBusinessDays2_oneDay() {
    DaysAdjustment test = DaysAdjustment.ofBusinessDays(1, HOLCAL_SAT_SUN);
    assertEquals(test.getDays(), 1);
    assertEquals(test.getCalendar(), HOLCAL_SAT_SUN);
    assertEquals(test.getAdjustment(), BDA_NONE);
    assertEquals(test.toString(), "1 business day using calendar Sat/Sun");
  }

  public void test_ofBusinessDays2_threeDays() {
    DaysAdjustment test = DaysAdjustment.ofBusinessDays(3, HOLCAL_SAT_SUN);
    assertEquals(test.getDays(), 3);
    assertEquals(test.getCalendar(), HOLCAL_SAT_SUN);
    assertEquals(test.getAdjustment(), BDA_NONE);
    assertEquals(test.toString(), "3 business days using calendar Sat/Sun");
  }

  public void test_ofBusinessDays2_adjust() {
    DaysAdjustment test = DaysAdjustment.ofBusinessDays(2, HOLCAL_SAT_SUN);
    LocalDate base = date(2014, 8, 15);  // Fri
    assertEquals(test.adjust(base), date(2014, 8, 19));  // Tue
  }

  public void test_ofBusinessDays2_null() {
    assertThrowsIllegalArg(() -> DaysAdjustment.ofBusinessDays(2, null));
  }

  //-------------------------------------------------------------------------
  public void test_ofBusinessDays3_oneDay() {
    DaysAdjustment test = DaysAdjustment.ofBusinessDays(1, HOLCAL_SAT_SUN, BDA_FOLLOW_WED_THU);
    assertEquals(test.getDays(), 1);
    assertEquals(test.getCalendar(), HOLCAL_SAT_SUN);
    assertEquals(test.getAdjustment(), BDA_FOLLOW_WED_THU);
    assertEquals(test.toString(), "1 business day using calendar Sat/Sun then apply Following using " +
        "calendar WedThu");
  }

  public void test_ofBusinessDays3_fourDays() {
    DaysAdjustment test = DaysAdjustment.ofBusinessDays(4, HOLCAL_SAT_SUN, BDA_FOLLOW_WED_THU);
    assertEquals(test.getDays(), 4);
    assertEquals(test.getCalendar(), HOLCAL_SAT_SUN);
    assertEquals(test.getAdjustment(), BDA_FOLLOW_WED_THU);
    assertEquals(test.toString(), "4 business days using calendar Sat/Sun then apply Following using " +
        "calendar WedThu");
  }

  public void test_ofBusinessDays3_adjust() {
    DaysAdjustment test = DaysAdjustment.ofBusinessDays(3, HOLCAL_SAT_SUN, BDA_FOLLOW_WED_THU);
    LocalDate base = date(2014, 8, 15);  // Fri
    assertEquals(test.adjust(base), date(2014, 8, 22));  // Fri (3 days gives Wed, following moves to Fri)
  }

  public void test_ofBusinessDays3_null() {
    assertThrowsIllegalArg(() -> DaysAdjustment.ofBusinessDays(3, null, BDA_FOLLOW_SAT_SUN));
    assertThrowsIllegalArg(() -> DaysAdjustment.ofBusinessDays(3, HOLCAL_SAT_SUN, null));
    assertThrowsIllegalArg(() -> DaysAdjustment.ofBusinessDays(3, null, null));
  }

  //-------------------------------------------------------------------------
  public void test_toAdjustedDate1() {
    DaysAdjustment test = DaysAdjustment.ofBusinessDays(3, HOLCAL_SAT_SUN);
    // Wed +3 = Mon
    assertEquals(test.toAdjustedDate(date(2015, 6, 10)), AdjustableDate.of(date(2015, 6, 15), BDA_NONE));
    // Fri +3 = Wed
    assertEquals(test.toAdjustedDate(date(2015, 6, 12)), AdjustableDate.of(date(2015, 6, 17), BDA_NONE));
  }

  public void test_toAdjustedDate2() {
    DaysAdjustment test = DaysAdjustment.ofBusinessDays(3, HOLCAL_SAT_SUN, BDA_FOLLOW_WED_THU);
    // Wed +3 = Mon
    assertEquals(test.toAdjustedDate(date(2015, 6, 10)), AdjustableDate.of(date(2015, 6, 15), BDA_FOLLOW_WED_THU));
    // Fri +3 = Wed
    assertEquals(test.toAdjustedDate(date(2015, 6, 12)), AdjustableDate.of(date(2015, 6, 17), BDA_FOLLOW_WED_THU));
  }

  //-------------------------------------------------------------------------
  public void test_getEffectiveResultCalendar1() {
    DaysAdjustment test = DaysAdjustment.ofBusinessDays(3, HOLCAL_SAT_SUN);
    assertEquals(test.getEffectiveResultCalendar(), HOLCAL_SAT_SUN);
  }

  public void test_getEffectiveResultCalendar2() {
    DaysAdjustment test = DaysAdjustment.ofBusinessDays(3, HOLCAL_SAT_SUN, BDA_FOLLOW_WED_THU);
    assertEquals(test.getEffectiveResultCalendar(), HOLCAL_WED_THU);
  }

  public void test_getEffectiveResultCalendar3() {
    DaysAdjustment test = DaysAdjustment.ofCalendarDays(3);
    assertEquals(test.getEffectiveResultCalendar(), HOLCAL_NONE);
  }

  //-------------------------------------------------------------------------
  public void equals() {
    DaysAdjustment a = DaysAdjustment.ofBusinessDays(3, HOLCAL_NONE, BDA_FOLLOW_SAT_SUN);
    DaysAdjustment b = DaysAdjustment.ofBusinessDays(4, HOLCAL_NONE, BDA_FOLLOW_SAT_SUN);
    DaysAdjustment c = DaysAdjustment.ofBusinessDays(3, HOLCAL_WED_THU, BDA_FOLLOW_SAT_SUN);
    DaysAdjustment d = DaysAdjustment.ofBusinessDays(3, HOLCAL_NONE, BDA_FOLLOW_WED_THU);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(c), false);
    assertEquals(a.equals(d), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(DaysAdjustment.ofCalendarDays(4, BDA_FOLLOW_SAT_SUN));
  }

  public void coverage_builder() {
    DaysAdjustment test = DaysAdjustment.builder()
        .days(1)
        .calendar(HOLCAL_SAT_SUN)
        .adjustment(BDA_FOLLOW_WED_THU)
        .build();
    assertEquals(test.getDays(), 1);
    assertEquals(test.getCalendar(), HOLCAL_SAT_SUN);
    assertEquals(test.getAdjustment(), BDA_FOLLOW_WED_THU);
  }

  public void test_serialization() {
    assertSerialization(DaysAdjustment.ofCalendarDays(4, BDA_FOLLOW_SAT_SUN));
  }

}
