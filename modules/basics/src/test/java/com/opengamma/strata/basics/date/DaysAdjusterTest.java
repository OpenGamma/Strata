/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Test {@link DaysAdjuster}.
 */
@Test
public class DaysAdjusterTest {

  private static final HolidayCalendar HOLCAL_NONE = HolidayCalendars.NO_HOLIDAYS;
  private static final HolidayCalendar HOLCAL_SAT_SUN = HolidayCalendars.SAT_SUN;
  private static final HolidayCalendar HOLCAL_WED_THU =
      ImmutableHolidayCalendar.of(HolidayCalendarId.of("WedThu"), ImmutableList.of(), WEDNESDAY, THURSDAY);
  private static final BusinessDayAdjuster BDA_NONE = BusinessDayAdjuster.NONE;
  private static final BusinessDayAdjuster BDA_FOLLOW_SAT_SUN =
      BusinessDayAdjuster.of(BusinessDayConventions.FOLLOWING, HolidayCalendars.SAT_SUN);
  private static final BusinessDayAdjuster BDA_FOLLOW_WED_THU =
      BusinessDayAdjuster.of(BusinessDayConventions.FOLLOWING, HOLCAL_WED_THU);

  //-------------------------------------------------------------------------
  public void test_NONE() {
    DaysAdjuster test = DaysAdjuster.NONE;
    assertEquals(test.getDays(), 0);
    assertEquals(test.getCalendar(), HOLCAL_NONE);
    assertEquals(test.getAdjuster(), BDA_NONE);
    assertEquals(test.toString(), "0 calendar days");
  }

  //-------------------------------------------------------------------------
  public void test_ofCalendarDays1_oneDay() {
    DaysAdjuster test = DaysAdjuster.ofCalendarDays(1);
    assertEquals(test.getDays(), 1);
    assertEquals(test.getCalendar(), HOLCAL_NONE);
    assertEquals(test.getAdjuster(), BDA_NONE);
    assertEquals(test.toString(), "1 calendar day");
  }

  public void test_ofCalendarDays1_threeDays() {
    DaysAdjuster test = DaysAdjuster.ofCalendarDays(3);
    assertEquals(test.getDays(), 3);
    assertEquals(test.getCalendar(), HOLCAL_NONE);
    assertEquals(test.getAdjuster(), BDA_NONE);
    assertEquals(test.toString(), "3 calendar days");
  }

  public void test_ofCalendarDays1_adjust() {
    DaysAdjuster test = DaysAdjuster.ofCalendarDays(2);
    LocalDate base = date(2014, 8, 15);  // Fri
    assertEquals(test.adjust(base), date(2014, 8, 17));  // Sun
  }

  public void test_ofCalendarDays2_oneDay() {
    DaysAdjuster test = DaysAdjuster.ofCalendarDays(1, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.getDays(), 1);
    assertEquals(test.getCalendar(), HOLCAL_NONE);
    assertEquals(test.getAdjuster(), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.toString(), "1 calendar day then apply Following using calendar Sat/Sun");
  }

  public void test_ofCalendarDays2_fourDays() {
    DaysAdjuster test = DaysAdjuster.ofCalendarDays(4, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.getDays(), 4);
    assertEquals(test.getCalendar(), HOLCAL_NONE);
    assertEquals(test.getAdjuster(), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.toString(), "4 calendar days then apply Following using calendar Sat/Sun");
  }

  public void test_ofCalendarDays2_adjust() {
    DaysAdjuster test = DaysAdjuster.ofCalendarDays(2, BDA_FOLLOW_SAT_SUN);
    LocalDate base = date(2014, 8, 15);  // Fri
    assertEquals(test.adjust(base), date(2014, 8, 18));  // Mon
  }

  public void test_ofCalendarDays2_null() {
    assertThrowsIllegalArg(() -> DaysAdjuster.ofCalendarDays(2, null));
  }

  //-------------------------------------------------------------------------
  public void test_ofBusinessDays2_oneDay() {
    DaysAdjuster test = DaysAdjuster.ofBusinessDays(1, HOLCAL_SAT_SUN);
    assertEquals(test.getDays(), 1);
    assertEquals(test.getCalendar(), HOLCAL_SAT_SUN);
    assertEquals(test.getAdjuster(), BDA_NONE);
    assertEquals(test.toString(), "1 business day using calendar Sat/Sun");
  }

  public void test_ofBusinessDays2_threeDays() {
    DaysAdjuster test = DaysAdjuster.ofBusinessDays(3, HOLCAL_SAT_SUN);
    assertEquals(test.getDays(), 3);
    assertEquals(test.getCalendar(), HOLCAL_SAT_SUN);
    assertEquals(test.getAdjuster(), BDA_NONE);
    assertEquals(test.toString(), "3 business days using calendar Sat/Sun");
  }

  public void test_ofBusinessDays2_adjust() {
    DaysAdjuster test = DaysAdjuster.ofBusinessDays(2, HOLCAL_SAT_SUN);
    LocalDate base = date(2014, 8, 15);  // Fri
    assertEquals(test.adjust(base), date(2014, 8, 19));  // Tue
  }

  public void test_ofBusinessDays2_null() {
    assertThrowsIllegalArg(() -> DaysAdjuster.ofBusinessDays(2, null));
  }

  //-------------------------------------------------------------------------
  public void test_ofBusinessDays3_oneDay() {
    DaysAdjuster test = DaysAdjuster.ofBusinessDays(1, HOLCAL_SAT_SUN, BDA_FOLLOW_WED_THU);
    assertEquals(test.getDays(), 1);
    assertEquals(test.getCalendar(), HOLCAL_SAT_SUN);
    assertEquals(test.getAdjuster(), BDA_FOLLOW_WED_THU);
    assertEquals(test.toString(), "1 business day using calendar Sat/Sun then apply Following using " +
        "calendar WedThu");
  }

  public void test_ofBusinessDays3_fourDays() {
    DaysAdjuster test = DaysAdjuster.ofBusinessDays(4, HOLCAL_SAT_SUN, BDA_FOLLOW_WED_THU);
    assertEquals(test.getDays(), 4);
    assertEquals(test.getCalendar(), HOLCAL_SAT_SUN);
    assertEquals(test.getAdjuster(), BDA_FOLLOW_WED_THU);
    assertEquals(test.toString(), "4 business days using calendar Sat/Sun then apply Following using " +
        "calendar WedThu");
  }

  public void test_ofBusinessDays3_adjust() {
    DaysAdjuster test = DaysAdjuster.ofBusinessDays(3, HOLCAL_SAT_SUN, BDA_FOLLOW_WED_THU);
    LocalDate base = date(2014, 8, 15);  // Fri
    assertEquals(test.adjust(base), date(2014, 8, 22));  // Fri (3 days gives Wed, following moves to Fri)
  }

  public void test_ofBusinessDays3_null() {
    assertThrowsIllegalArg(() -> DaysAdjuster.ofBusinessDays(3, null, BDA_FOLLOW_SAT_SUN));
    assertThrowsIllegalArg(() -> DaysAdjuster.ofBusinessDays(3, HOLCAL_SAT_SUN, null));
    assertThrowsIllegalArg(() -> DaysAdjuster.ofBusinessDays(3, null, null));
  }

  //-------------------------------------------------------------------------
  public void test_getResultCalendar1() {
    DaysAdjuster test = DaysAdjuster.ofBusinessDays(3, HOLCAL_SAT_SUN);
    assertEquals(test.getResultCalendar(), HOLCAL_SAT_SUN);
  }

  public void test_getResultCalendar2() {
    DaysAdjuster test = DaysAdjuster.ofBusinessDays(3, HOLCAL_SAT_SUN, BDA_FOLLOW_WED_THU);
    assertEquals(test.getResultCalendar(), HOLCAL_WED_THU);
  }

  public void test_getResultCalendar3() {
    DaysAdjuster test = DaysAdjuster.ofCalendarDays(3);
    assertEquals(test.getResultCalendar(), HOLCAL_NONE);
  }

  //-------------------------------------------------------------------------
  public void equals() {
    DaysAdjuster a = DaysAdjuster.ofBusinessDays(3, HOLCAL_NONE, BDA_FOLLOW_SAT_SUN);
    DaysAdjuster b = DaysAdjuster.ofBusinessDays(4, HOLCAL_NONE, BDA_FOLLOW_SAT_SUN);
    DaysAdjuster c = DaysAdjuster.ofBusinessDays(3, HOLCAL_WED_THU, BDA_FOLLOW_SAT_SUN);
    DaysAdjuster d = DaysAdjuster.ofBusinessDays(3, HOLCAL_NONE, BDA_FOLLOW_WED_THU);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(c), false);
    assertEquals(a.equals(d), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(DaysAdjuster.ofCalendarDays(4, BDA_FOLLOW_SAT_SUN));
  }

  public void coverage_builder() {
    DaysAdjuster test = DaysAdjuster.builder()
        .days(1)
        .calendar(HOLCAL_SAT_SUN)
        .adjuster(BDA_FOLLOW_WED_THU)
        .build();
    assertEquals(test.getDays(), 1);
    assertEquals(test.getCalendar(), HOLCAL_SAT_SUN);
    assertEquals(test.getAdjuster(), BDA_FOLLOW_WED_THU);
  }

  public void test_serialization() {
    assertSerialization(DaysAdjuster.ofCalendarDays(4, BDA_FOLLOW_SAT_SUN));
  }

}
