/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.Month.JULY;
import static java.time.Month.JUNE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.time.LocalDate;

import org.joda.beans.ImmutableBean;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link HolidayCalendar}.
 */
@Test
public class HolidayCalendarTest {

  private static final LocalDate WED_2014_07_09 = LocalDate.of(2014, 7, 9);
  private static final LocalDate THU_2014_07_10 = LocalDate.of(2014, 7, 10);
  private static final LocalDate FRI_2014_07_11 = LocalDate.of(2014, 7, 11);
  private static final LocalDate SAT_2014_07_12 = LocalDate.of(2014, 7, 12);
  private static final LocalDate SUN_2014_07_13 = LocalDate.of(2014, 7, 13);
  private static final LocalDate MON_2014_07_14 = LocalDate.of(2014, 7, 14);
  private static final LocalDate TUE_2014_07_15 = LocalDate.of(2014, 7, 15);
  private static final LocalDate WED_2014_07_16 = LocalDate.of(2014, 7, 16);
  private static final LocalDate THU_2014_07_17 = LocalDate.of(2014, 7, 17);
  private static final LocalDate FRI_2014_07_18 = LocalDate.of(2014, 7, 18);
  private static final LocalDate SAT_2014_07_19 = LocalDate.of(2014, 7, 19);
  private static final LocalDate SUN_2014_07_20 = LocalDate.of(2014, 7, 20);
  private static final LocalDate MON_2014_07_21 = LocalDate.of(2014, 7, 21);
  private static final LocalDate TUE_2014_07_22 = LocalDate.of(2014, 7, 22);
  private static final LocalDate WED_2014_07_23 = LocalDate.of(2014, 7, 23);

  private static final LocalDate WED_2014_07_30 = LocalDate.of(2014, 7, 30);
  private static final LocalDate THU_2014_07_31 = LocalDate.of(2014, 7, 31);

  //-------------------------------------------------------------------------
  public void test_NO_HOLIDAYS() {
    HolidayCalendar test = HolidayCalendars.NO_HOLIDAYS;
    LocalDateUtils.stream(LocalDate.of(2011, 1, 1), LocalDate.of(2015, 1, 31)).forEach(date -> {
      assertEquals(test.isBusinessDay(date), true);
      assertEquals(test.isHoliday(date), false);
    });
    assertEquals(test.getName(), "NoHolidays");
    assertEquals(test.toString(), "HolidayCalendar[NoHolidays]");
  }

  public void test_NO_HOLIDAYS_of() {
    HolidayCalendar test = HolidayCalendars.of("NoHolidays");
    assertEquals(test, HolidayCalendars.NO_HOLIDAYS);
  }

  public void test_NO_HOLIDAYS_shift() {
    assertEquals(HolidayCalendars.NO_HOLIDAYS.shift(FRI_2014_07_11, 2), SUN_2014_07_13);
    assertEquals(HolidayCalendars.NO_HOLIDAYS.shift(SUN_2014_07_13, -2), FRI_2014_07_11);
  }

  public void test_NO_HOLIDAYS_next() {
    assertEquals(HolidayCalendars.NO_HOLIDAYS.next(FRI_2014_07_11), SAT_2014_07_12);
    assertEquals(HolidayCalendars.NO_HOLIDAYS.next(SAT_2014_07_12), SUN_2014_07_13);
  }

  public void test_NO_HOLIDAYS_nextOrSame() {
    assertEquals(HolidayCalendars.NO_HOLIDAYS.nextOrSame(FRI_2014_07_11), FRI_2014_07_11);
    assertEquals(HolidayCalendars.NO_HOLIDAYS.nextOrSame(SAT_2014_07_12), SAT_2014_07_12);
  }

  public void test_NO_HOLIDAYS_previous() {
    assertEquals(HolidayCalendars.NO_HOLIDAYS.previous(SAT_2014_07_12), FRI_2014_07_11);
    assertEquals(HolidayCalendars.NO_HOLIDAYS.previous(SUN_2014_07_13), SAT_2014_07_12);
  }

  public void test_NO_HOLIDAYS_previousOrSame() {
    assertEquals(HolidayCalendars.NO_HOLIDAYS.previousOrSame(SAT_2014_07_12), SAT_2014_07_12);
    assertEquals(HolidayCalendars.NO_HOLIDAYS.previousOrSame(SUN_2014_07_13), SUN_2014_07_13);
  }

  public void test_NO_HOLIDAYS_nextSameOrLastInMonth() {
    assertEquals(HolidayCalendars.NO_HOLIDAYS.nextSameOrLastInMonth(FRI_2014_07_11), FRI_2014_07_11);
    assertEquals(HolidayCalendars.NO_HOLIDAYS.nextSameOrLastInMonth(SAT_2014_07_12), SAT_2014_07_12);
  }

  public void test_NO_HOLIDAYS_daysBetween_LocalDateLocalDate() {
    assertEquals(HolidayCalendars.NO_HOLIDAYS.daysBetween(FRI_2014_07_11, MON_2014_07_14), 3);
  }

  public void test_NO_HOLIDAYS_combineWith() {
    HolidayCalendar base = new MockHolCal();
    HolidayCalendar test = HolidayCalendars.NO_HOLIDAYS.combinedWith(base);
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void test_SAT_SUN() {
    HolidayCalendar test = HolidayCalendars.SAT_SUN;
    LocalDateUtils.stream(LocalDate.of(2011, 1, 1), LocalDate.of(2015, 1, 31)).forEach(date -> {
      boolean isBusinessDay = date.getDayOfWeek() != SATURDAY && date.getDayOfWeek() != SUNDAY;
      assertEquals(test.isBusinessDay(date), isBusinessDay);
      assertEquals(test.isHoliday(date), !isBusinessDay);
    });
    assertEquals(test.getName(), "Sat/Sun");
    assertEquals(test.toString(), "HolidayCalendar[Sat/Sun]");
  }

  public void test_SAT_SUN_of() {
    HolidayCalendar test = HolidayCalendars.of("Sat/Sun");
    assertEquals(test, HolidayCalendars.SAT_SUN);
  }

  public void test_SAT_SUN_shift() {
    assertEquals(HolidayCalendars.SAT_SUN.shift(THU_2014_07_10, 2), MON_2014_07_14);
    assertEquals(HolidayCalendars.SAT_SUN.shift(FRI_2014_07_11, 2), TUE_2014_07_15);
    assertEquals(HolidayCalendars.SAT_SUN.shift(SUN_2014_07_13, 2), TUE_2014_07_15);
    assertEquals(HolidayCalendars.SAT_SUN.shift(MON_2014_07_14, 2), WED_2014_07_16);

    assertEquals(HolidayCalendars.SAT_SUN.shift(FRI_2014_07_11, -2), WED_2014_07_09);
    assertEquals(HolidayCalendars.SAT_SUN.shift(SAT_2014_07_12, -2), THU_2014_07_10);
    assertEquals(HolidayCalendars.SAT_SUN.shift(SUN_2014_07_13, -2), THU_2014_07_10);
    assertEquals(HolidayCalendars.SAT_SUN.shift(MON_2014_07_14, -2), THU_2014_07_10);
    assertEquals(HolidayCalendars.SAT_SUN.shift(TUE_2014_07_15, -2), FRI_2014_07_11);
    assertEquals(HolidayCalendars.SAT_SUN.shift(WED_2014_07_16, -2), MON_2014_07_14);

    assertEquals(HolidayCalendars.SAT_SUN.shift(FRI_2014_07_11, 5), FRI_2014_07_18);
    assertEquals(HolidayCalendars.SAT_SUN.shift(FRI_2014_07_11, 6), MON_2014_07_21);

    assertEquals(HolidayCalendars.SAT_SUN.shift(FRI_2014_07_18, -5), FRI_2014_07_11);
    assertEquals(HolidayCalendars.SAT_SUN.shift(MON_2014_07_21, -6), FRI_2014_07_11);
  }

  public void test_SAT_SUN_next() {
    assertEquals(HolidayCalendars.SAT_SUN.next(THU_2014_07_10), FRI_2014_07_11);
    assertEquals(HolidayCalendars.SAT_SUN.next(FRI_2014_07_11), MON_2014_07_14);
    assertEquals(HolidayCalendars.SAT_SUN.next(SAT_2014_07_12), MON_2014_07_14);
    assertEquals(HolidayCalendars.SAT_SUN.next(SAT_2014_07_12), MON_2014_07_14);
  }

  public void test_SAT_SUN_previous() {
    assertEquals(HolidayCalendars.SAT_SUN.previous(SAT_2014_07_12), FRI_2014_07_11);
    assertEquals(HolidayCalendars.SAT_SUN.previous(SUN_2014_07_13), FRI_2014_07_11);
    assertEquals(HolidayCalendars.SAT_SUN.previous(MON_2014_07_14), FRI_2014_07_11);
    assertEquals(HolidayCalendars.SAT_SUN.previous(TUE_2014_07_15), MON_2014_07_14);
  }

  public void test_SAT_SUN_daysBetween_LocalDateLocalDate() {
    assertEquals(HolidayCalendars.SAT_SUN.daysBetween(FRI_2014_07_11, MON_2014_07_14), 1);
  }

  //-------------------------------------------------------------------------
  public void test_FRI_SAT() {
    HolidayCalendar test = HolidayCalendars.FRI_SAT;
    LocalDateUtils.stream(LocalDate.of(2011, 1, 1), LocalDate.of(2015, 1, 31)).forEach(date -> {
      boolean isBusinessDay = date.getDayOfWeek() != FRIDAY && date.getDayOfWeek() != SATURDAY;
      assertEquals(test.isBusinessDay(date), isBusinessDay);
      assertEquals(test.isHoliday(date), !isBusinessDay);
    });
    assertEquals(test.getName(), "Fri/Sat");
    assertEquals(test.toString(), "HolidayCalendar[Fri/Sat]");
  }

  public void test_FRI_SAT_of() {
    HolidayCalendar test = HolidayCalendars.of("Fri/Sat");
    assertEquals(test, HolidayCalendars.FRI_SAT);
  }

  public void test_FRI_SAT_shift() {
    assertEquals(HolidayCalendars.FRI_SAT.shift(THU_2014_07_10, 2), MON_2014_07_14);
    assertEquals(HolidayCalendars.FRI_SAT.shift(FRI_2014_07_11, 2), MON_2014_07_14);
    assertEquals(HolidayCalendars.FRI_SAT.shift(SUN_2014_07_13, 2), TUE_2014_07_15);
    assertEquals(HolidayCalendars.FRI_SAT.shift(MON_2014_07_14, 2), WED_2014_07_16);

    assertEquals(HolidayCalendars.FRI_SAT.shift(FRI_2014_07_11, -2), WED_2014_07_09);
    assertEquals(HolidayCalendars.FRI_SAT.shift(SAT_2014_07_12, -2), WED_2014_07_09);
    assertEquals(HolidayCalendars.FRI_SAT.shift(SUN_2014_07_13, -2), WED_2014_07_09);
    assertEquals(HolidayCalendars.FRI_SAT.shift(MON_2014_07_14, -2), THU_2014_07_10);
    assertEquals(HolidayCalendars.FRI_SAT.shift(TUE_2014_07_15, -2), SUN_2014_07_13);
    assertEquals(HolidayCalendars.FRI_SAT.shift(WED_2014_07_16, -2), MON_2014_07_14);

    assertEquals(HolidayCalendars.FRI_SAT.shift(THU_2014_07_10, 5), THU_2014_07_17);
    assertEquals(HolidayCalendars.FRI_SAT.shift(THU_2014_07_10, 6), SUN_2014_07_20);

    assertEquals(HolidayCalendars.FRI_SAT.shift(THU_2014_07_17, -5), THU_2014_07_10);
    assertEquals(HolidayCalendars.FRI_SAT.shift(SUN_2014_07_20, -6), THU_2014_07_10);
  }

  public void test_FRI_SAT_next() {
    assertEquals(HolidayCalendars.FRI_SAT.next(WED_2014_07_09), THU_2014_07_10);
    assertEquals(HolidayCalendars.FRI_SAT.next(THU_2014_07_10), SUN_2014_07_13);
    assertEquals(HolidayCalendars.FRI_SAT.next(FRI_2014_07_11), SUN_2014_07_13);
    assertEquals(HolidayCalendars.FRI_SAT.next(SAT_2014_07_12), SUN_2014_07_13);
    assertEquals(HolidayCalendars.FRI_SAT.next(SUN_2014_07_13), MON_2014_07_14);
  }

  public void test_FRI_SAT_previous() {
    assertEquals(HolidayCalendars.FRI_SAT.previous(FRI_2014_07_11), THU_2014_07_10);
    assertEquals(HolidayCalendars.FRI_SAT.previous(SAT_2014_07_12), THU_2014_07_10);
    assertEquals(HolidayCalendars.FRI_SAT.previous(SUN_2014_07_13), THU_2014_07_10);
    assertEquals(HolidayCalendars.FRI_SAT.previous(MON_2014_07_14), SUN_2014_07_13);
  }

  public void test_FRI_SAT_daysBetween_LocalDateLocalDate() {
    assertEquals(HolidayCalendars.FRI_SAT.daysBetween(FRI_2014_07_11, MON_2014_07_14), 1);
  }

  //-------------------------------------------------------------------------
  public void test_THU_FRI() {
    HolidayCalendar test = HolidayCalendars.THU_FRI;
    LocalDateUtils.stream(LocalDate.of(2011, 1, 1), LocalDate.of(2015, 1, 31)).forEach(date -> {
      boolean isBusinessDay = date.getDayOfWeek() != THURSDAY && date.getDayOfWeek() != FRIDAY;
      assertEquals(test.isBusinessDay(date), isBusinessDay);
      assertEquals(test.isHoliday(date), !isBusinessDay);
    });
    assertEquals(test.getName(), "Thu/Fri");
    assertEquals(test.toString(), "HolidayCalendar[Thu/Fri]");
  }

  public void test_THU_FRI_of() {
    HolidayCalendar test = HolidayCalendars.of("Thu/Fri");
    assertEquals(test, HolidayCalendars.THU_FRI);
  }

  public void test_THU_FRI_shift() {
    assertEquals(HolidayCalendars.THU_FRI.shift(WED_2014_07_09, 2), SUN_2014_07_13);
    assertEquals(HolidayCalendars.THU_FRI.shift(THU_2014_07_10, 2), SUN_2014_07_13);
    assertEquals(HolidayCalendars.THU_FRI.shift(FRI_2014_07_11, 2), SUN_2014_07_13);
    assertEquals(HolidayCalendars.THU_FRI.shift(SAT_2014_07_12, 2), MON_2014_07_14);

    assertEquals(HolidayCalendars.THU_FRI.shift(FRI_2014_07_18, -2), TUE_2014_07_15);
    assertEquals(HolidayCalendars.THU_FRI.shift(SAT_2014_07_19, -2), TUE_2014_07_15);
    assertEquals(HolidayCalendars.THU_FRI.shift(SUN_2014_07_20, -2), WED_2014_07_16);
    assertEquals(HolidayCalendars.THU_FRI.shift(MON_2014_07_21, -2), SAT_2014_07_19);

    assertEquals(HolidayCalendars.THU_FRI.shift(WED_2014_07_09, 5), WED_2014_07_16);
    assertEquals(HolidayCalendars.THU_FRI.shift(WED_2014_07_09, 6), SAT_2014_07_19);

    assertEquals(HolidayCalendars.THU_FRI.shift(WED_2014_07_16, -5), WED_2014_07_09);
    assertEquals(HolidayCalendars.THU_FRI.shift(SAT_2014_07_19, -6), WED_2014_07_09);
  }

  public void test_THU_FRI_next() {
    assertEquals(HolidayCalendars.THU_FRI.next(WED_2014_07_09), SAT_2014_07_12);
    assertEquals(HolidayCalendars.THU_FRI.next(THU_2014_07_10), SAT_2014_07_12);
    assertEquals(HolidayCalendars.THU_FRI.next(FRI_2014_07_11), SAT_2014_07_12);
    assertEquals(HolidayCalendars.THU_FRI.next(SAT_2014_07_12), SUN_2014_07_13);
  }

  public void test_THU_FRI_previous() {
    assertEquals(HolidayCalendars.THU_FRI.previous(THU_2014_07_10), WED_2014_07_09);
    assertEquals(HolidayCalendars.THU_FRI.previous(FRI_2014_07_11), WED_2014_07_09);
    assertEquals(HolidayCalendars.THU_FRI.previous(SAT_2014_07_12), WED_2014_07_09);
    assertEquals(HolidayCalendars.THU_FRI.previous(SUN_2014_07_13), SAT_2014_07_12);
  }

  public void test_THU_FRI_daysBetween_LocalDateLocalDate() {
    assertEquals(HolidayCalendars.THU_FRI.daysBetween(FRI_2014_07_11, MON_2014_07_14), 2);
  }

  //-------------------------------------------------------------------------
  public void test_of_combined() {
    HolidayCalendar test = HolidayCalendars.of("Thu/Fri+Fri/Sat");
    assertEquals(test.getName(), "Fri/Sat+Thu/Fri");
    assertEquals(test.toString(), "HolidayCalendar[Fri/Sat+Thu/Fri]");

    HolidayCalendar test2 = HolidayCalendars.of("Thu/Fri+Fri/Sat");
    assertEquals(test, test2);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "shift")
  static Object[][] data_shift() {
    return new Object[][] {
        {THU_2014_07_10, 1, FRI_2014_07_11},
        {FRI_2014_07_11, 1, MON_2014_07_14},
        {SAT_2014_07_12, 1, MON_2014_07_14},
        {SUN_2014_07_13, 1, MON_2014_07_14},
        {MON_2014_07_14, 1, TUE_2014_07_15},
        {TUE_2014_07_15, 1, THU_2014_07_17},
        {WED_2014_07_16, 1, THU_2014_07_17},
        {THU_2014_07_17, 1, MON_2014_07_21},
        {FRI_2014_07_18, 1, MON_2014_07_21},
        {SAT_2014_07_19, 1, MON_2014_07_21},
        {SUN_2014_07_20, 1, MON_2014_07_21},
        {MON_2014_07_21, 1, TUE_2014_07_22},

        {THU_2014_07_10, 2, MON_2014_07_14},
        {FRI_2014_07_11, 2, TUE_2014_07_15},
        {SAT_2014_07_12, 2, TUE_2014_07_15},
        {SUN_2014_07_13, 2, TUE_2014_07_15},
        {MON_2014_07_14, 2, THU_2014_07_17},
        {TUE_2014_07_15, 2, MON_2014_07_21},
        {WED_2014_07_16, 2, MON_2014_07_21},
        {THU_2014_07_17, 2, TUE_2014_07_22},
        {FRI_2014_07_18, 2, TUE_2014_07_22},
        {SAT_2014_07_19, 2, TUE_2014_07_22},
        {SUN_2014_07_20, 2, TUE_2014_07_22},
        {MON_2014_07_21, 2, WED_2014_07_23},

        {THU_2014_07_10, 0, THU_2014_07_10},
        {FRI_2014_07_11, 0, FRI_2014_07_11},
        {SAT_2014_07_12, 0, SAT_2014_07_12},
        {SUN_2014_07_13, 0, SUN_2014_07_13},
        {MON_2014_07_14, 0, MON_2014_07_14},
        {TUE_2014_07_15, 0, TUE_2014_07_15},
        {WED_2014_07_16, 0, WED_2014_07_16},
        {THU_2014_07_17, 0, THU_2014_07_17},
        {FRI_2014_07_18, 0, FRI_2014_07_18},
        {SAT_2014_07_19, 0, SAT_2014_07_19},
        {SUN_2014_07_20, 0, SUN_2014_07_20},
        {MON_2014_07_21, 0, MON_2014_07_21},

        {FRI_2014_07_11, -1, THU_2014_07_10},
        {SAT_2014_07_12, -1, FRI_2014_07_11},
        {SUN_2014_07_13, -1, FRI_2014_07_11},
        {MON_2014_07_14, -1, FRI_2014_07_11},
        {TUE_2014_07_15, -1, MON_2014_07_14},
        {WED_2014_07_16, -1, TUE_2014_07_15},
        {THU_2014_07_17, -1, TUE_2014_07_15},
        {FRI_2014_07_18, -1, THU_2014_07_17},
        {SAT_2014_07_19, -1, THU_2014_07_17},
        {SUN_2014_07_20, -1, THU_2014_07_17},
        {MON_2014_07_21, -1, THU_2014_07_17},
        {TUE_2014_07_22, -1, MON_2014_07_21},

        {FRI_2014_07_11, -2, WED_2014_07_09},
        {SAT_2014_07_12, -2, THU_2014_07_10},
        {SUN_2014_07_13, -2, THU_2014_07_10},
        {MON_2014_07_14, -2, THU_2014_07_10},
        {TUE_2014_07_15, -2, FRI_2014_07_11},
        {WED_2014_07_16, -2, MON_2014_07_14},
        {THU_2014_07_17, -2, MON_2014_07_14},
        {FRI_2014_07_18, -2, TUE_2014_07_15},
        {SAT_2014_07_19, -2, TUE_2014_07_15},
        {SUN_2014_07_20, -2, TUE_2014_07_15},
        {MON_2014_07_21, -2, TUE_2014_07_15},
        {TUE_2014_07_22, -2, THU_2014_07_17},
    };
  }

  @Test(dataProvider = "shift")
  public void test_shift(LocalDate date, int amount, LocalDate expected) {
    // 16th, 18th, Sat, Sun
    HolidayCalendar test = new MockHolCal();
    assertEquals(test.shift(date, amount), expected);
  }

  @Test(dataProvider = "shift")
  public void test_adjustBy(LocalDate date, int amount, LocalDate expected) {
    HolidayCalendar test = new MockHolCal();
    assertEquals(date.with(test.adjustBy(amount)), expected);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "next")
  static Object[][] data_next() {
    return new Object[][] {
        {THU_2014_07_10, FRI_2014_07_11},
        {FRI_2014_07_11, MON_2014_07_14},
        {SAT_2014_07_12, MON_2014_07_14},
        {SUN_2014_07_13, MON_2014_07_14},
        {MON_2014_07_14, TUE_2014_07_15},
        {TUE_2014_07_15, THU_2014_07_17},
        {WED_2014_07_16, THU_2014_07_17},
        {THU_2014_07_17, MON_2014_07_21},
        {FRI_2014_07_18, MON_2014_07_21},
        {SAT_2014_07_19, MON_2014_07_21},
        {SUN_2014_07_20, MON_2014_07_21},
        {MON_2014_07_21, TUE_2014_07_22},
    };
  }

  @Test(dataProvider = "next")
  public void test_next(LocalDate date, LocalDate expectedNext) {
    HolidayCalendar test = new MockHolCal();
    assertEquals(test.next(date), expectedNext);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "nextOrSame")
  static Object[][] data_nextOrSame() {
    return new Object[][] {
        {THU_2014_07_10, THU_2014_07_10},
        {FRI_2014_07_11, FRI_2014_07_11},
        {SAT_2014_07_12, MON_2014_07_14},
        {SUN_2014_07_13, MON_2014_07_14},
        {MON_2014_07_14, MON_2014_07_14},
        {TUE_2014_07_15, TUE_2014_07_15},
        {WED_2014_07_16, THU_2014_07_17},
        {THU_2014_07_17, THU_2014_07_17},
        {FRI_2014_07_18, MON_2014_07_21},
        {SAT_2014_07_19, MON_2014_07_21},
        {SUN_2014_07_20, MON_2014_07_21},
        {MON_2014_07_21, MON_2014_07_21},
    };
  }

  @Test(dataProvider = "nextOrSame")
  public void test_nextOrSame(LocalDate date, LocalDate expectedNext) {
    HolidayCalendar test = new MockHolCal();
    assertEquals(test.nextOrSame(date), expectedNext);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "previous")
  static Object[][] data_previous() {
    return new Object[][] {
        {FRI_2014_07_11, THU_2014_07_10},
        {SAT_2014_07_12, FRI_2014_07_11},
        {SUN_2014_07_13, FRI_2014_07_11},
        {MON_2014_07_14, FRI_2014_07_11},
        {TUE_2014_07_15, MON_2014_07_14},
        {WED_2014_07_16, TUE_2014_07_15},
        {THU_2014_07_17, TUE_2014_07_15},
        {FRI_2014_07_18, THU_2014_07_17},
        {SAT_2014_07_19, THU_2014_07_17},
        {SUN_2014_07_20, THU_2014_07_17},
        {MON_2014_07_21, THU_2014_07_17},
        {TUE_2014_07_22, MON_2014_07_21},
    };
  }

  @Test(dataProvider = "previous")
  public void test_previous(LocalDate date, LocalDate expectedPrevious) {
    HolidayCalendar test = new MockHolCal();
    assertEquals(test.previous(date), expectedPrevious);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "previousOrSame")
  static Object[][] data_previousOrSame() {
    return new Object[][] {
        {FRI_2014_07_11, FRI_2014_07_11},
        {SAT_2014_07_12, FRI_2014_07_11},
        {SUN_2014_07_13, FRI_2014_07_11},
        {MON_2014_07_14, MON_2014_07_14},
        {TUE_2014_07_15, TUE_2014_07_15},
        {WED_2014_07_16, TUE_2014_07_15},
        {THU_2014_07_17, THU_2014_07_17},
        {FRI_2014_07_18, THU_2014_07_17},
        {SAT_2014_07_19, THU_2014_07_17},
        {SUN_2014_07_20, THU_2014_07_17},
        {MON_2014_07_21, MON_2014_07_21},
        {TUE_2014_07_22, TUE_2014_07_22},
    };
  }

  @Test(dataProvider = "previousOrSame")
  public void test_previousOrSame(LocalDate date, LocalDate expectedPrevious) {
    HolidayCalendar test = new MockHolCal();
    assertEquals(test.previousOrSame(date), expectedPrevious);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "nextSameOrLastInMonth")
  static Object[][] data_nextSameOrLastInMonth() {
    return new Object[][] {
        {THU_2014_07_10, THU_2014_07_10},
        {FRI_2014_07_11, FRI_2014_07_11},
        {SAT_2014_07_12, MON_2014_07_14},
        {SUN_2014_07_13, MON_2014_07_14},
        {MON_2014_07_14, MON_2014_07_14},
        {TUE_2014_07_15, TUE_2014_07_15},
        {WED_2014_07_16, THU_2014_07_17},
        {THU_2014_07_17, THU_2014_07_17},
        {FRI_2014_07_18, MON_2014_07_21},
        {SAT_2014_07_19, MON_2014_07_21},
        {SUN_2014_07_20, MON_2014_07_21},
        {MON_2014_07_21, MON_2014_07_21},

        {THU_2014_07_31, WED_2014_07_30},
    };
  }

  @Test(dataProvider = "nextSameOrLastInMonth")
  public void test_nextLastOrSame(LocalDate date, LocalDate expectedNext) {
    // mock calendar has Sat/Sun plus 16th, 18th and 31st as holidays
    HolidayCalendar test = new MockHolCal();
    assertEquals(test.nextSameOrLastInMonth(date), expectedNext);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "lastBusinessDayOfMonth")
  static Object[][] data_lastBusinessDayOfMonth() {
    return new Object[][] {
        // June 30th is Monday holiday, June 28/29 is weekend
        {date(2014, 6, 26), date(2014, 6, 27)},
        {date(2014, 6, 27), date(2014, 6, 27)},
        {date(2014, 6, 28), date(2014, 6, 27)},
        {date(2014, 6, 29), date(2014, 6, 27)},
        {date(2014, 6, 30), date(2014, 6, 27)},
        // July 31st is Thursday holiday
        {date(2014, 7, 29), date(2014, 7, 30)},
        {date(2014, 7, 30), date(2014, 7, 30)},
        {date(2014, 7, 31), date(2014, 7, 30)},
        // August 31st is Sunday weekend
        {date(2014, 8, 28), date(2014, 8, 29)},
        {date(2014, 8, 29), date(2014, 8, 29)},
        {date(2014, 8, 30), date(2014, 8, 29)},
        {date(2014, 8, 31), date(2014, 8, 29)},
        // September 30th is Tuesday not holiday
        {date(2014, 9, 28), date(2014, 9, 30)},
        {date(2014, 9, 29), date(2014, 9, 30)},
        {date(2014, 9, 30), date(2014, 9, 30)},
    };
  }

  @Test(dataProvider = "lastBusinessDayOfMonth")
  public void test_lastBusinessDayOfMonth(LocalDate date, LocalDate expectedEom) {
    HolidayCalendar test = new MockEomHolCal();
    assertEquals(test.lastBusinessDayOfMonth(date), expectedEom);
  }

  @Test(dataProvider = "lastBusinessDayOfMonth")
  public void test_isLastBusinessDayOfMonth(LocalDate date, LocalDate expectedEom) {
    HolidayCalendar test = new MockEomHolCal();
    assertEquals(test.isLastBusinessDayOfMonth(date), date.equals(expectedEom));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "daysBetween")
  static Object[][] data_daysBetween() {
    return new Object[][] {
        {FRI_2014_07_11, FRI_2014_07_11, 0},
        {FRI_2014_07_11, SAT_2014_07_12, 1},
        {FRI_2014_07_11, SUN_2014_07_13, 1},
        {FRI_2014_07_11, MON_2014_07_14, 1},
        {FRI_2014_07_11, TUE_2014_07_15, 2},
        {FRI_2014_07_11, WED_2014_07_16, 3},
        {FRI_2014_07_11, THU_2014_07_17, 3},
        {FRI_2014_07_11, FRI_2014_07_18, 4},
        {FRI_2014_07_11, SAT_2014_07_19, 4},
        {FRI_2014_07_11, SUN_2014_07_20, 4},
        {FRI_2014_07_11, MON_2014_07_21, 4},
        {FRI_2014_07_11, TUE_2014_07_22, 5},
    };
  }

  @Test(dataProvider = "daysBetween")
  public void test_daysBetween_LocalDateLocalDate(LocalDate start, LocalDate end, int expected) {
    HolidayCalendar test = new MockHolCal();
    assertEquals(test.daysBetween(start, end), expected);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith() {
    HolidayCalendar base1 = new MockHolCal();
    HolidayCalendar base2 = HolidayCalendars.FRI_SAT;
    HolidayCalendar test = base1.combinedWith(base2);
    assertEquals(test.toString(), "HolidayCalendar[Fri/Sat+Mock]");
    assertEquals(test.getName(), "Fri/Sat+Mock");
    assertEquals(test.equals(base1.combinedWith(base2)), true);
    assertEquals(test.equals(""), false);
    assertEquals(test.equals(null), false);
    assertEquals(test.hashCode(), base1.combinedWith(base2).hashCode());

    assertEquals(test.isHoliday(THU_2014_07_10), false);
    assertEquals(test.isHoliday(FRI_2014_07_11), true);
    assertEquals(test.isHoliday(SAT_2014_07_12), true);
    assertEquals(test.isHoliday(SUN_2014_07_13), true);
    assertEquals(test.isHoliday(MON_2014_07_14), false);
    assertEquals(test.isHoliday(TUE_2014_07_15), false);
    assertEquals(test.isHoliday(WED_2014_07_16), true);
    assertEquals(test.isHoliday(THU_2014_07_17), false);
    assertEquals(test.isHoliday(FRI_2014_07_18), true);
    assertEquals(test.isHoliday(SAT_2014_07_19), true);
    assertEquals(test.isHoliday(SUN_2014_07_20), true);
    assertEquals(test.isHoliday(MON_2014_07_21), false);
  }

  public void test_combineWith_same() {
    HolidayCalendar base = new MockHolCal();
    HolidayCalendar test = base.combinedWith(base);
    assertSame(test, base);
  }

  public void test_combineWith_none() {
    HolidayCalendar base = new MockHolCal();
    HolidayCalendar test = base.combinedWith(HolidayCalendars.NO_HOLIDAYS);
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void test_extendedEnum() {
    assertEquals(HolidayCalendars.extendedEnum().lookupAll().get("NoHolidays"), HolidayCalendars.NO_HOLIDAYS);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(HolidayCalendars.class);
  }

  public void coverage_combined() {
    HolidayCalendar test = HolidayCalendars.FRI_SAT.combinedWith(HolidayCalendars.SAT_SUN);
    coverImmutableBean((ImmutableBean) test);
  }

  public void coverage_noHolidays() {
    HolidayCalendar test = HolidayCalendars.NO_HOLIDAYS;
    coverImmutableBean((ImmutableBean) test);
  }

  public void coverage_weekend() {
    HolidayCalendar test = HolidayCalendars.FRI_SAT;
    coverImmutableBean((ImmutableBean) test);
  }

  public void test_serialization() {
    assertSerialization(HolidayCalendars.NO_HOLIDAYS);
    assertSerialization(HolidayCalendars.SAT_SUN);
    assertSerialization(HolidayCalendars.FRI_SAT);
    assertSerialization(HolidayCalendars.THU_FRI);
    assertSerialization(HolidayCalendars.FRI_SAT.combinedWith(HolidayCalendars.SAT_SUN));
  }

  //-------------------------------------------------------------------------
  static class MockHolCal implements HolidayCalendar {
    @Override
    public boolean isHoliday(LocalDate date) {
      return date.getDayOfMonth() == 16 || date.getDayOfMonth() == 18 || date.getDayOfMonth() == 31 ||
          date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
    }

    @Override
    public HolidayCalendarId getId() {
      return HolidayCalendarId.of("Mock");
    }
  }

  static class MockEomHolCal implements HolidayCalendar {
    @Override
    public boolean isHoliday(LocalDate date) {
      return (date.getMonth() == JUNE && date.getDayOfMonth() == 30) ||
          (date.getMonth() == JULY && date.getDayOfMonth() == 31) ||
          date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
    }

    @Override
    public HolidayCalendarId getId() {
      return HolidayCalendarId.of("MockEom");
    }
  }

}
