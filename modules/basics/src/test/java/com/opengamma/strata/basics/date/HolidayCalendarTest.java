/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.TestHelper.date;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.Month.JULY;
import static java.time.Month.JUNE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link HolidayCalendar}.
 */
public class HolidayCalendarTest {

  private static final LocalDate MON_2014_07_07 = LocalDate.of(2014, 7, 7);
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
  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  @Test
  public void test_NO_HOLIDAYS() {
    HolidayCalendar test = HolidayCalendars.NO_HOLIDAYS;
    LocalDateUtils.stream(LocalDate.of(2011, 1, 1), LocalDate.of(2015, 1, 31)).forEach(date -> {
      assertThat(test.isBusinessDay(date)).isEqualTo(true);
      assertThat(test.isHoliday(date)).isEqualTo(false);
    });
    assertThat(test.getName()).isEqualTo("NoHolidays");
    assertThat(test.toString()).isEqualTo("HolidayCalendar[NoHolidays]");
  }

  @Test
  public void test_NO_HOLIDAYS_of() {
    HolidayCalendar test = HolidayCalendars.of("NoHolidays");
    assertThat(test).isEqualTo(HolidayCalendars.NO_HOLIDAYS);
  }

  @Test
  public void test_NO_HOLIDAYS_shift() {
    assertThat(HolidayCalendars.NO_HOLIDAYS.shift(FRI_2014_07_11, 2)).isEqualTo(SUN_2014_07_13);
    assertThat(HolidayCalendars.NO_HOLIDAYS.shift(SUN_2014_07_13, -2)).isEqualTo(FRI_2014_07_11);
  }

  @Test
  public void test_NO_HOLIDAYS_next() {
    assertThat(HolidayCalendars.NO_HOLIDAYS.next(FRI_2014_07_11)).isEqualTo(SAT_2014_07_12);
    assertThat(HolidayCalendars.NO_HOLIDAYS.next(SAT_2014_07_12)).isEqualTo(SUN_2014_07_13);
  }

  @Test
  public void test_NO_HOLIDAYS_nextOrSame() {
    assertThat(HolidayCalendars.NO_HOLIDAYS.nextOrSame(FRI_2014_07_11)).isEqualTo(FRI_2014_07_11);
    assertThat(HolidayCalendars.NO_HOLIDAYS.nextOrSame(SAT_2014_07_12)).isEqualTo(SAT_2014_07_12);
  }

  @Test
  public void test_NO_HOLIDAYS_previous() {
    assertThat(HolidayCalendars.NO_HOLIDAYS.previous(SAT_2014_07_12)).isEqualTo(FRI_2014_07_11);
    assertThat(HolidayCalendars.NO_HOLIDAYS.previous(SUN_2014_07_13)).isEqualTo(SAT_2014_07_12);
  }

  @Test
  public void test_NO_HOLIDAYS_previousOrSame() {
    assertThat(HolidayCalendars.NO_HOLIDAYS.previousOrSame(SAT_2014_07_12)).isEqualTo(SAT_2014_07_12);
    assertThat(HolidayCalendars.NO_HOLIDAYS.previousOrSame(SUN_2014_07_13)).isEqualTo(SUN_2014_07_13);
  }

  @Test
  public void test_NO_HOLIDAYS_nextSameOrLastInMonth() {
    assertThat(HolidayCalendars.NO_HOLIDAYS.nextSameOrLastInMonth(FRI_2014_07_11)).isEqualTo(FRI_2014_07_11);
    assertThat(HolidayCalendars.NO_HOLIDAYS.nextSameOrLastInMonth(SAT_2014_07_12)).isEqualTo(SAT_2014_07_12);
  }

  @Test
  public void test_NO_HOLIDAYS_daysBetween_LocalDateLocalDate() {
    assertThat(HolidayCalendars.NO_HOLIDAYS.daysBetween(FRI_2014_07_11, MON_2014_07_14)).isEqualTo(3);
  }

  @Test
  public void test_NO_HOLIDAYS_combineWith() {
    HolidayCalendar base = new MockHolCal();
    HolidayCalendar test = HolidayCalendars.NO_HOLIDAYS.combinedWith(base);
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_SAT_SUN() {
    HolidayCalendar test = HolidayCalendars.SAT_SUN;
    LocalDateUtils.stream(LocalDate.of(2011, 1, 1), LocalDate.of(2015, 1, 31)).forEach(date -> {
      boolean isBusinessDay = date.getDayOfWeek() != SATURDAY && date.getDayOfWeek() != SUNDAY;
      assertThat(test.isBusinessDay(date)).isEqualTo(isBusinessDay);
      assertThat(test.isHoliday(date)).isEqualTo(!isBusinessDay);
    });
    assertThat(test.getName()).isEqualTo("Sat/Sun");
    assertThat(test.toString()).isEqualTo("HolidayCalendar[Sat/Sun]");
  }

  @Test
  public void test_SAT_SUN_of() {
    HolidayCalendar test = HolidayCalendars.of("Sat/Sun");
    assertThat(test).isEqualTo(HolidayCalendars.SAT_SUN);
  }

  @Test
  public void test_SAT_SUN_shift() {
    ImmutableHolidayCalendar equivalent =
        ImmutableHolidayCalendar.of(HolidayCalendarId.of("TEST-SAT-SUN"), ImmutableList.of(), ImmutableList.of(SATURDAY, SUNDAY));
    assertSatSun(equivalent);
    assertSatSun(HolidayCalendars.SAT_SUN);
  }

  private void assertSatSun(HolidayCalendar test) {
    assertThat(test.shift(THU_2014_07_10, 2)).isEqualTo(MON_2014_07_14);
    assertThat(test.shift(FRI_2014_07_11, 2)).isEqualTo(TUE_2014_07_15);
    assertThat(test.shift(SUN_2014_07_13, 2)).isEqualTo(TUE_2014_07_15);
    assertThat(test.shift(MON_2014_07_14, 2)).isEqualTo(WED_2014_07_16);

    assertThat(test.shift(FRI_2014_07_11, -2)).isEqualTo(WED_2014_07_09);
    assertThat(test.shift(SAT_2014_07_12, -2)).isEqualTo(THU_2014_07_10);
    assertThat(test.shift(SUN_2014_07_13, -2)).isEqualTo(THU_2014_07_10);
    assertThat(test.shift(MON_2014_07_14, -2)).isEqualTo(THU_2014_07_10);
    assertThat(test.shift(TUE_2014_07_15, -2)).isEqualTo(FRI_2014_07_11);
    assertThat(test.shift(WED_2014_07_16, -2)).isEqualTo(MON_2014_07_14);

    assertThat(test.shift(FRI_2014_07_11, 5)).isEqualTo(FRI_2014_07_18);
    assertThat(test.shift(FRI_2014_07_11, 6)).isEqualTo(MON_2014_07_21);

    assertThat(test.shift(FRI_2014_07_18, -5)).isEqualTo(FRI_2014_07_11);
    assertThat(test.shift(MON_2014_07_21, -6)).isEqualTo(FRI_2014_07_11);

    assertThat(test.shift(SAT_2014_07_12, 5)).isEqualTo(FRI_2014_07_18);
    assertThat(test.shift(SAT_2014_07_12, -5)).isEqualTo(MON_2014_07_07);
  }

  @Test
  public void test_SAT_SUN_next() {
    assertThat(HolidayCalendars.SAT_SUN.next(THU_2014_07_10)).isEqualTo(FRI_2014_07_11);
    assertThat(HolidayCalendars.SAT_SUN.next(FRI_2014_07_11)).isEqualTo(MON_2014_07_14);
    assertThat(HolidayCalendars.SAT_SUN.next(SAT_2014_07_12)).isEqualTo(MON_2014_07_14);
    assertThat(HolidayCalendars.SAT_SUN.next(SAT_2014_07_12)).isEqualTo(MON_2014_07_14);
  }

  @Test
  public void test_SAT_SUN_previous() {
    assertThat(HolidayCalendars.SAT_SUN.previous(SAT_2014_07_12)).isEqualTo(FRI_2014_07_11);
    assertThat(HolidayCalendars.SAT_SUN.previous(SUN_2014_07_13)).isEqualTo(FRI_2014_07_11);
    assertThat(HolidayCalendars.SAT_SUN.previous(MON_2014_07_14)).isEqualTo(FRI_2014_07_11);
    assertThat(HolidayCalendars.SAT_SUN.previous(TUE_2014_07_15)).isEqualTo(MON_2014_07_14);
  }

  @Test
  public void test_SAT_SUN_daysBetween_LocalDateLocalDate() {
    assertThat(HolidayCalendars.SAT_SUN.daysBetween(FRI_2014_07_11, MON_2014_07_14)).isEqualTo(1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_FRI_SAT() {
    HolidayCalendar test = HolidayCalendars.FRI_SAT;
    LocalDateUtils.stream(LocalDate.of(2011, 1, 1), LocalDate.of(2015, 1, 31)).forEach(date -> {
      boolean isBusinessDay = date.getDayOfWeek() != FRIDAY && date.getDayOfWeek() != SATURDAY;
      assertThat(test.isBusinessDay(date)).isEqualTo(isBusinessDay);
      assertThat(test.isHoliday(date)).isEqualTo(!isBusinessDay);
    });
    assertThat(test.getName()).isEqualTo("Fri/Sat");
    assertThat(test.toString()).isEqualTo("HolidayCalendar[Fri/Sat]");
  }

  @Test
  public void test_FRI_SAT_of() {
    HolidayCalendar test = HolidayCalendars.of("Fri/Sat");
    assertThat(test).isEqualTo(HolidayCalendars.FRI_SAT);
  }

  @Test
  public void test_FRI_SAT_shift() {
    assertThat(HolidayCalendars.FRI_SAT.shift(THU_2014_07_10, 2)).isEqualTo(MON_2014_07_14);
    assertThat(HolidayCalendars.FRI_SAT.shift(FRI_2014_07_11, 2)).isEqualTo(MON_2014_07_14);
    assertThat(HolidayCalendars.FRI_SAT.shift(SUN_2014_07_13, 2)).isEqualTo(TUE_2014_07_15);
    assertThat(HolidayCalendars.FRI_SAT.shift(MON_2014_07_14, 2)).isEqualTo(WED_2014_07_16);

    assertThat(HolidayCalendars.FRI_SAT.shift(FRI_2014_07_11, -2)).isEqualTo(WED_2014_07_09);
    assertThat(HolidayCalendars.FRI_SAT.shift(SAT_2014_07_12, -2)).isEqualTo(WED_2014_07_09);
    assertThat(HolidayCalendars.FRI_SAT.shift(SUN_2014_07_13, -2)).isEqualTo(WED_2014_07_09);
    assertThat(HolidayCalendars.FRI_SAT.shift(MON_2014_07_14, -2)).isEqualTo(THU_2014_07_10);
    assertThat(HolidayCalendars.FRI_SAT.shift(TUE_2014_07_15, -2)).isEqualTo(SUN_2014_07_13);
    assertThat(HolidayCalendars.FRI_SAT.shift(WED_2014_07_16, -2)).isEqualTo(MON_2014_07_14);

    assertThat(HolidayCalendars.FRI_SAT.shift(THU_2014_07_10, 5)).isEqualTo(THU_2014_07_17);
    assertThat(HolidayCalendars.FRI_SAT.shift(THU_2014_07_10, 6)).isEqualTo(SUN_2014_07_20);

    assertThat(HolidayCalendars.FRI_SAT.shift(THU_2014_07_17, -5)).isEqualTo(THU_2014_07_10);
    assertThat(HolidayCalendars.FRI_SAT.shift(SUN_2014_07_20, -6)).isEqualTo(THU_2014_07_10);
  }

  @Test
  public void test_FRI_SAT_next() {
    assertThat(HolidayCalendars.FRI_SAT.next(WED_2014_07_09)).isEqualTo(THU_2014_07_10);
    assertThat(HolidayCalendars.FRI_SAT.next(THU_2014_07_10)).isEqualTo(SUN_2014_07_13);
    assertThat(HolidayCalendars.FRI_SAT.next(FRI_2014_07_11)).isEqualTo(SUN_2014_07_13);
    assertThat(HolidayCalendars.FRI_SAT.next(SAT_2014_07_12)).isEqualTo(SUN_2014_07_13);
    assertThat(HolidayCalendars.FRI_SAT.next(SUN_2014_07_13)).isEqualTo(MON_2014_07_14);
  }

  @Test
  public void test_FRI_SAT_previous() {
    assertThat(HolidayCalendars.FRI_SAT.previous(FRI_2014_07_11)).isEqualTo(THU_2014_07_10);
    assertThat(HolidayCalendars.FRI_SAT.previous(SAT_2014_07_12)).isEqualTo(THU_2014_07_10);
    assertThat(HolidayCalendars.FRI_SAT.previous(SUN_2014_07_13)).isEqualTo(THU_2014_07_10);
    assertThat(HolidayCalendars.FRI_SAT.previous(MON_2014_07_14)).isEqualTo(SUN_2014_07_13);
  }

  @Test
  public void test_FRI_SAT_daysBetween_LocalDateLocalDate() {
    assertThat(HolidayCalendars.FRI_SAT.daysBetween(FRI_2014_07_11, MON_2014_07_14)).isEqualTo(1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_THU_FRI() {
    HolidayCalendar test = HolidayCalendars.THU_FRI;
    LocalDateUtils.stream(LocalDate.of(2011, 1, 1), LocalDate.of(2015, 1, 31)).forEach(date -> {
      boolean isBusinessDay = date.getDayOfWeek() != THURSDAY && date.getDayOfWeek() != FRIDAY;
      assertThat(test.isBusinessDay(date)).isEqualTo(isBusinessDay);
      assertThat(test.isHoliday(date)).isEqualTo(!isBusinessDay);
    });
    assertThat(test.getName()).isEqualTo("Thu/Fri");
    assertThat(test.toString()).isEqualTo("HolidayCalendar[Thu/Fri]");
  }

  @Test
  public void test_THU_FRI_of() {
    HolidayCalendar test = HolidayCalendars.of("Thu/Fri");
    assertThat(test).isEqualTo(HolidayCalendars.THU_FRI);
  }

  @Test
  public void test_THU_FRI_shift() {
    assertThat(HolidayCalendars.THU_FRI.shift(WED_2014_07_09, 2)).isEqualTo(SUN_2014_07_13);
    assertThat(HolidayCalendars.THU_FRI.shift(THU_2014_07_10, 2)).isEqualTo(SUN_2014_07_13);
    assertThat(HolidayCalendars.THU_FRI.shift(FRI_2014_07_11, 2)).isEqualTo(SUN_2014_07_13);
    assertThat(HolidayCalendars.THU_FRI.shift(SAT_2014_07_12, 2)).isEqualTo(MON_2014_07_14);

    assertThat(HolidayCalendars.THU_FRI.shift(FRI_2014_07_18, -2)).isEqualTo(TUE_2014_07_15);
    assertThat(HolidayCalendars.THU_FRI.shift(SAT_2014_07_19, -2)).isEqualTo(TUE_2014_07_15);
    assertThat(HolidayCalendars.THU_FRI.shift(SUN_2014_07_20, -2)).isEqualTo(WED_2014_07_16);
    assertThat(HolidayCalendars.THU_FRI.shift(MON_2014_07_21, -2)).isEqualTo(SAT_2014_07_19);

    assertThat(HolidayCalendars.THU_FRI.shift(WED_2014_07_09, 5)).isEqualTo(WED_2014_07_16);
    assertThat(HolidayCalendars.THU_FRI.shift(WED_2014_07_09, 6)).isEqualTo(SAT_2014_07_19);

    assertThat(HolidayCalendars.THU_FRI.shift(WED_2014_07_16, -5)).isEqualTo(WED_2014_07_09);
    assertThat(HolidayCalendars.THU_FRI.shift(SAT_2014_07_19, -6)).isEqualTo(WED_2014_07_09);
  }

  @Test
  public void test_THU_FRI_next() {
    assertThat(HolidayCalendars.THU_FRI.next(WED_2014_07_09)).isEqualTo(SAT_2014_07_12);
    assertThat(HolidayCalendars.THU_FRI.next(THU_2014_07_10)).isEqualTo(SAT_2014_07_12);
    assertThat(HolidayCalendars.THU_FRI.next(FRI_2014_07_11)).isEqualTo(SAT_2014_07_12);
    assertThat(HolidayCalendars.THU_FRI.next(SAT_2014_07_12)).isEqualTo(SUN_2014_07_13);
  }

  @Test
  public void test_THU_FRI_previous() {
    assertThat(HolidayCalendars.THU_FRI.previous(THU_2014_07_10)).isEqualTo(WED_2014_07_09);
    assertThat(HolidayCalendars.THU_FRI.previous(FRI_2014_07_11)).isEqualTo(WED_2014_07_09);
    assertThat(HolidayCalendars.THU_FRI.previous(SAT_2014_07_12)).isEqualTo(WED_2014_07_09);
    assertThat(HolidayCalendars.THU_FRI.previous(SUN_2014_07_13)).isEqualTo(SAT_2014_07_12);
  }

  @Test
  public void test_THU_FRI_daysBetween_LocalDateLocalDate() {
    assertThat(HolidayCalendars.THU_FRI.daysBetween(FRI_2014_07_11, MON_2014_07_14)).isEqualTo(2);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_combined() {
    HolidayCalendar test = HolidayCalendars.of("Thu/Fri+Fri/Sat");
    assertThat(test.getName()).isEqualTo("Fri/Sat+Thu/Fri");
    assertThat(test.toString()).isEqualTo("HolidayCalendar[Fri/Sat+Thu/Fri]");

    HolidayCalendar test2 = HolidayCalendars.of("Thu/Fri+Fri/Sat");
    assertThat(test).isEqualTo(test2);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_shift() {
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

  @ParameterizedTest
  @MethodSource("data_shift")
  public void test_shift(LocalDate date, int amount, LocalDate expected) {
    // 16th, 18th, Sat, Sun
    HolidayCalendar test = new MockHolCal();
    assertThat(test.shift(date, amount)).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("data_shift")
  public void test_adjustBy(LocalDate date, int amount, LocalDate expected) {
    HolidayCalendar test = new MockHolCal();
    assertThat(date.with(test.adjustBy(amount))).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_next() {
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

  @ParameterizedTest
  @MethodSource("data_next")
  public void test_next(LocalDate date, LocalDate expectedNext) {
    HolidayCalendar test = new MockHolCal();
    assertThat(test.next(date)).isEqualTo(expectedNext);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_nextOrSame() {
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

  @ParameterizedTest
  @MethodSource("data_nextOrSame")
  public void test_nextOrSame(LocalDate date, LocalDate expectedNext) {
    HolidayCalendar test = new MockHolCal();
    assertThat(test.nextOrSame(date)).isEqualTo(expectedNext);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_previous() {
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

  @ParameterizedTest
  @MethodSource("data_previous")
  public void test_previous(LocalDate date, LocalDate expectedPrevious) {
    HolidayCalendar test = new MockHolCal();
    assertThat(test.previous(date)).isEqualTo(expectedPrevious);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_previousOrSame() {
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

  @ParameterizedTest
  @MethodSource("data_previousOrSame")
  public void test_previousOrSame(LocalDate date, LocalDate expectedPrevious) {
    HolidayCalendar test = new MockHolCal();
    assertThat(test.previousOrSame(date)).isEqualTo(expectedPrevious);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_nextSameOrLastInMonth() {
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

  @ParameterizedTest
  @MethodSource("data_nextSameOrLastInMonth")
  public void test_nextLastOrSame(LocalDate date, LocalDate expectedNext) {
    // mock calendar has Sat/Sun plus 16th, 18th and 31st as holidays
    HolidayCalendar test = new MockHolCal();
    assertThat(test.nextSameOrLastInMonth(date)).isEqualTo(expectedNext);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_lastBusinessDayOfMonth() {
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

  @ParameterizedTest
  @MethodSource("data_lastBusinessDayOfMonth")
  public void test_lastBusinessDayOfMonth(LocalDate date, LocalDate expectedEom) {
    HolidayCalendar test = new MockEomHolCal();
    assertThat(test.lastBusinessDayOfMonth(date)).isEqualTo(expectedEom);
  }

  @ParameterizedTest
  @MethodSource("data_lastBusinessDayOfMonth")
  public void test_isLastBusinessDayOfMonth(LocalDate date, LocalDate expectedEom) {
    HolidayCalendar test = new MockEomHolCal();
    assertThat(test.isLastBusinessDayOfMonth(date)).isEqualTo(date.equals(expectedEom));
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_daysBetween() {
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

  @ParameterizedTest
  @MethodSource("data_daysBetween")
  public void test_daysBetween_LocalDateLocalDate(LocalDate start, LocalDate end, int expected) {
    HolidayCalendar test = new MockHolCal();
    assertThat(test.daysBetween(start, end)).isEqualTo(expected);
  }

  @Test
  public void test_daysBetween_LocalDateLocalDate_endBeforeStart() {
    HolidayCalendar test = new MockHolCal();
    assertThatIllegalArgumentException().isThrownBy(() -> test.daysBetween(TUE_2014_07_15, MON_2014_07_14));
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_businessDays() {
    return new Object[][] {
        {FRI_2014_07_11, FRI_2014_07_11, ImmutableList.of()},
        {FRI_2014_07_11, SAT_2014_07_12, ImmutableList.of(FRI_2014_07_11)},
        {FRI_2014_07_11, SUN_2014_07_13, ImmutableList.of(FRI_2014_07_11)},
        {FRI_2014_07_11, MON_2014_07_14, ImmutableList.of(FRI_2014_07_11)},
        {FRI_2014_07_11, TUE_2014_07_15, ImmutableList.of(FRI_2014_07_11, MON_2014_07_14)},
        {FRI_2014_07_11, WED_2014_07_16, ImmutableList.of(FRI_2014_07_11, MON_2014_07_14, TUE_2014_07_15)},
        {FRI_2014_07_11, THU_2014_07_17, ImmutableList.of(FRI_2014_07_11, MON_2014_07_14, TUE_2014_07_15)},
        {FRI_2014_07_11, FRI_2014_07_18, ImmutableList.of(
            FRI_2014_07_11, MON_2014_07_14, TUE_2014_07_15, THU_2014_07_17)},
        {FRI_2014_07_11, SAT_2014_07_19, ImmutableList.of(
            FRI_2014_07_11, MON_2014_07_14, TUE_2014_07_15, THU_2014_07_17)},
        {FRI_2014_07_11, SUN_2014_07_20, ImmutableList.of(
            FRI_2014_07_11, MON_2014_07_14, TUE_2014_07_15, THU_2014_07_17)},
        {FRI_2014_07_11, MON_2014_07_21, ImmutableList.of(
            FRI_2014_07_11, MON_2014_07_14, TUE_2014_07_15, THU_2014_07_17)},
        {FRI_2014_07_11, TUE_2014_07_22, ImmutableList.of(
            FRI_2014_07_11, MON_2014_07_14, TUE_2014_07_15, THU_2014_07_17, MON_2014_07_21)},
    };
  }

  @ParameterizedTest
  @MethodSource("data_businessDays")
  public void test_businessDays_LocalDateLocalDate(LocalDate start, LocalDate end, ImmutableList<LocalDate> expected) {
    HolidayCalendar test = new MockHolCal();
    assertThat(test.businessDays(start, end).collect(toImmutableList())).isEqualTo(expected);
  }

  @Test
  public void test_businessDays_LocalDateLocalDate_endBeforeStart() {
    HolidayCalendar test = new MockHolCal();
    assertThatIllegalArgumentException().isThrownBy(() -> test.businessDays(TUE_2014_07_15, MON_2014_07_14));
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_holidays() {
    return new Object[][] {
        {FRI_2014_07_11, FRI_2014_07_11, ImmutableList.of()},
        {FRI_2014_07_11, SAT_2014_07_12, ImmutableList.of()},
        {FRI_2014_07_11, SUN_2014_07_13, ImmutableList.of(SAT_2014_07_12)},
        {FRI_2014_07_11, MON_2014_07_14, ImmutableList.of(SAT_2014_07_12, SUN_2014_07_13)},
        {FRI_2014_07_11, TUE_2014_07_15, ImmutableList.of(SAT_2014_07_12, SUN_2014_07_13)},
        {FRI_2014_07_11, WED_2014_07_16, ImmutableList.of(SAT_2014_07_12, SUN_2014_07_13)},
        {FRI_2014_07_11, THU_2014_07_17, ImmutableList.of(SAT_2014_07_12, SUN_2014_07_13, WED_2014_07_16)},
        {FRI_2014_07_11, FRI_2014_07_18, ImmutableList.of(SAT_2014_07_12, SUN_2014_07_13, WED_2014_07_16)},
        {FRI_2014_07_11, SAT_2014_07_19, ImmutableList.of(
            SAT_2014_07_12, SUN_2014_07_13, WED_2014_07_16, FRI_2014_07_18)},
        {FRI_2014_07_11, SUN_2014_07_20, ImmutableList.of(
            SAT_2014_07_12, SUN_2014_07_13, WED_2014_07_16, FRI_2014_07_18, SAT_2014_07_19)},
        {FRI_2014_07_11, MON_2014_07_21, ImmutableList.of(
            SAT_2014_07_12, SUN_2014_07_13, WED_2014_07_16, FRI_2014_07_18, SAT_2014_07_19, SUN_2014_07_20)},
        {FRI_2014_07_11, TUE_2014_07_22, ImmutableList.of(
            SAT_2014_07_12, SUN_2014_07_13, WED_2014_07_16, FRI_2014_07_18, SAT_2014_07_19, SUN_2014_07_20)},
    };
  }

  @ParameterizedTest
  @MethodSource("data_holidays")
  public void test_holidays_LocalDateLocalDate(LocalDate start, LocalDate end, ImmutableList<LocalDate> expected) {
    HolidayCalendar test = new MockHolCal();
    assertThat(test.holidays(start, end).collect(toImmutableList())).isEqualTo(expected);
  }

  @Test
  public void test_holidays_LocalDateLocalDate_endBeforeStart() {
    HolidayCalendar test = new MockHolCal();
    assertThatIllegalArgumentException().isThrownBy(() -> test.holidays(TUE_2014_07_15, MON_2014_07_14));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith() {
    HolidayCalendar base1 = new MockHolCal();
    HolidayCalendar base2 = HolidayCalendars.FRI_SAT;
    HolidayCalendar test = base1.combinedWith(base2);
    assertThat(test.toString()).isEqualTo("HolidayCalendar[Fri/Sat+Mock]");
    assertThat(test.getName()).isEqualTo("Fri/Sat+Mock");
    assertThat(test.equals(base1.combinedWith(base2))).isEqualTo(true);
    assertThat(test.equals(ANOTHER_TYPE)).isEqualTo(false);
    assertThat(test.equals(null)).isEqualTo(false);
    assertThat(test.hashCode()).isEqualTo(base1.combinedWith(base2).hashCode());

    assertThat(test.isHoliday(THU_2014_07_10)).isEqualTo(false);
    assertThat(test.isHoliday(FRI_2014_07_11)).isEqualTo(true);
    assertThat(test.isHoliday(SAT_2014_07_12)).isEqualTo(true);
    assertThat(test.isHoliday(SUN_2014_07_13)).isEqualTo(true);
    assertThat(test.isHoliday(MON_2014_07_14)).isEqualTo(false);
    assertThat(test.isHoliday(TUE_2014_07_15)).isEqualTo(false);
    assertThat(test.isHoliday(WED_2014_07_16)).isEqualTo(true);
    assertThat(test.isHoliday(THU_2014_07_17)).isEqualTo(false);
    assertThat(test.isHoliday(FRI_2014_07_18)).isEqualTo(true);
    assertThat(test.isHoliday(SAT_2014_07_19)).isEqualTo(true);
    assertThat(test.isHoliday(SUN_2014_07_20)).isEqualTo(true);
    assertThat(test.isHoliday(MON_2014_07_21)).isEqualTo(false);
  }

  @Test
  public void test_combineWith_same() {
    HolidayCalendar base = new MockHolCal();
    HolidayCalendar test = base.combinedWith(base);
    assertThat(test).isSameAs(base);
  }

  @Test
  public void test_combineWith_none() {
    HolidayCalendar base = new MockHolCal();
    HolidayCalendar test = base.combinedWith(HolidayCalendars.NO_HOLIDAYS);
    assertThat(test).isSameAs(base);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_linkedWith() {
    HolidayCalendar base1 = new MockHolCal();
    HolidayCalendar base2 = HolidayCalendars.FRI_SAT;
    HolidayCalendar test = base1.linkedWith(base2);
    assertThat(test.toString()).isEqualTo("HolidayCalendar[Fri/Sat~Mock]");
    assertThat(test.getName()).isEqualTo("Fri/Sat~Mock");
    assertThat(test.equals(base1.linkedWith(base2))).isEqualTo(true);
    assertThat(test.equals(ANOTHER_TYPE)).isEqualTo(false);
    assertThat(test.equals(null)).isEqualTo(false);
    assertThat(test.hashCode()).isEqualTo(base1.linkedWith(base2).hashCode());

    assertThat(test.isHoliday(THU_2014_07_10)).isEqualTo(false);
    assertThat(test.isHoliday(FRI_2014_07_11)).isEqualTo(false);
    assertThat(test.isHoliday(SAT_2014_07_12)).isEqualTo(true);
    assertThat(test.isHoliday(SUN_2014_07_13)).isEqualTo(false);
    assertThat(test.isHoliday(MON_2014_07_14)).isEqualTo(false);
    assertThat(test.isHoliday(TUE_2014_07_15)).isEqualTo(false);
    assertThat(test.isHoliday(WED_2014_07_16)).isEqualTo(false);
    assertThat(test.isHoliday(THU_2014_07_17)).isEqualTo(false);
    assertThat(test.isHoliday(FRI_2014_07_18)).isEqualTo(true);
    assertThat(test.isHoliday(SAT_2014_07_19)).isEqualTo(true);
    assertThat(test.isHoliday(SUN_2014_07_20)).isEqualTo(false);
    assertThat(test.isHoliday(MON_2014_07_21)).isEqualTo(false);
  }

  @Test
  public void test_linkedWith_same() {
    HolidayCalendar base = new MockHolCal();
    HolidayCalendar test = base.linkedWith(base);
    assertThat(test).isSameAs(base);
  }

  @Test
  public void test_linkedWith_none() {
    HolidayCalendar base = new MockHolCal();
    HolidayCalendar test = base.linkedWith(HolidayCalendars.NO_HOLIDAYS);
    assertThat(test).isSameAs(HolidayCalendars.NO_HOLIDAYS);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_extendedEnum() {
    assertThat(HolidayCalendars.extendedEnum().lookupAll().get("NoHolidays")).isEqualTo(HolidayCalendars.NO_HOLIDAYS);
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
