/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.assertThrowsRuntime;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;

/**
 * Test {@link ImmutableHolidayCalendar}.
 */
@Test
public class ImmutableHolidayCalendarTest {

  private static final HolidayCalendarId TEST_ID = HolidayCalendarId.of("Test1");
  private static final HolidayCalendarId TEST_ID2 = HolidayCalendarId.of("Test2");

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

  private static final ImmutableHolidayCalendar HOLCAL_MON_WED = ImmutableHolidayCalendar.of(
      TEST_ID, ImmutableList.of(MON_2014_07_14, WED_2014_07_16), SATURDAY, SUNDAY);

  private static final LocalDate MON_2014_12_29 = LocalDate.of(2014, 12, 29);
  private static final LocalDate TUE_2014_12_30 = LocalDate.of(2014, 12, 30);
  private static final LocalDate WED_2014_12_31 = LocalDate.of(2014, 12, 31);
  private static final LocalDate THU_2015_01_01 = LocalDate.of(2015, 1, 1);
  private static final LocalDate FRI_2015_01_02 = LocalDate.of(2015, 1, 2);
  private static final LocalDate SAT_2015_01_03 = LocalDate.of(2015, 1, 3);
  private static final LocalDate MON_2015_01_05 = LocalDate.of(2015, 1, 5);
  private static final LocalDate TUE_2015_03_31 = LocalDate.of(2015, 3, 31);
  private static final LocalDate WED_2015_04_01 = LocalDate.of(2015, 4, 1);

  private static final ImmutableHolidayCalendar HOLCAL_YEAR_END = ImmutableHolidayCalendar.of(
      HolidayCalendarId.of("TestYearEnd"), ImmutableList.of(TUE_2014_12_30, THU_2015_01_01), SATURDAY, SUNDAY);

  private static final ImmutableHolidayCalendar HOLCAL_SAT_SUN = ImmutableHolidayCalendar.of(
      HolidayCalendarId.of("TestSatSun"), ImmutableList.of(), SATURDAY, SUNDAY);

  private static final LocalDate MON_2014_06_30 = LocalDate.of(2014, 6, 30);
  private static final LocalDate WED_2014_07_30 = LocalDate.of(2014, 7, 30);
  private static final LocalDate THU_2014_07_31 = LocalDate.of(2014, 7, 31);

  private static final ImmutableHolidayCalendar HOLCAL_END_MONTH = ImmutableHolidayCalendar.of(
      HolidayCalendarId.of("TestEndOfMonth"), ImmutableList.of(MON_2014_06_30, THU_2014_07_31), SATURDAY, SUNDAY);

  private static final LocalDate FRI_2015_02_27 = LocalDate.of(2015, 2, 27);
  private static final LocalDate SAT_2015_02_28 = LocalDate.of(2015, 2, 28);

  //-------------------------------------------------------------------------
  public void test_of_IterableDayOfWeekDayOfWeek_null() {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, FRI_2014_07_18);
    assertThrowsRuntime(() -> ImmutableHolidayCalendar.of(null, holidays, SATURDAY, SUNDAY));
    assertThrowsRuntime(() -> ImmutableHolidayCalendar.of(TEST_ID, null, SATURDAY, SUNDAY));
    assertThrowsRuntime(() -> ImmutableHolidayCalendar.of(TEST_ID, holidays, null, SUNDAY));
    assertThrowsRuntime(() -> ImmutableHolidayCalendar.of(TEST_ID, holidays, SATURDAY, null));
  }

  public void test_of_IterableIterable_null() {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, FRI_2014_07_18);
    Iterable<DayOfWeek> weekendDays = Arrays.asList(THURSDAY, FRIDAY);
    assertThrowsRuntime(() -> ImmutableHolidayCalendar.of(null, holidays, weekendDays));
    assertThrowsRuntime(() -> ImmutableHolidayCalendar.of(TEST_ID, null, weekendDays));
    assertThrowsRuntime(() -> ImmutableHolidayCalendar.of(TEST_ID, holidays, null));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "createSatSunWeekend")
  static Object[][] data_createSatSunWeekend() {
    return new Object[][] {
        {FRI_2014_07_11, true},
        {SAT_2014_07_12, false},
        {SUN_2014_07_13, false},
        {MON_2014_07_14, false},
        {TUE_2014_07_15, true},
        {WED_2014_07_16, true},
        {THU_2014_07_17, true},
        {FRI_2014_07_18, false},
        {SAT_2014_07_19, false},
        {SUN_2014_07_20, false},
        {MON_2014_07_21, true},
    };
  }

  @Test(dataProvider = "createSatSunWeekend")
  public void test_of_IterableDayOfWeekDayOfWeek_satSunWeekend(LocalDate date, boolean isBusinessDay) {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, FRI_2014_07_18);
    ImmutableHolidayCalendar test = ImmutableHolidayCalendar.of(TEST_ID, holidays, SATURDAY, SUNDAY);
    assertEquals(test.isBusinessDay(date), isBusinessDay);
    assertEquals(test.isHoliday(date), !isBusinessDay);
    assertEquals(test.getHolidays(), ImmutableSortedSet.copyOf(holidays));
    assertEquals(test.getWeekendDays(), ImmutableSet.of(SATURDAY, SUNDAY));
    assertEquals(test.toString(), "HolidayCalendar[" + TEST_ID.getName() + "]");
  }

  @Test(dataProvider = "createSatSunWeekend")
  public void test_of_IterableIterable_satSunWeekend(LocalDate date, boolean isBusinessDay) {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, FRI_2014_07_18);
    Iterable<DayOfWeek> weekendDays = Arrays.asList(SATURDAY, SUNDAY);
    ImmutableHolidayCalendar test = ImmutableHolidayCalendar.of(TEST_ID, holidays, weekendDays);
    assertEquals(test.isBusinessDay(date), isBusinessDay);
    assertEquals(test.isHoliday(date), !isBusinessDay);
    assertEquals(test.getHolidays(), ImmutableSortedSet.copyOf(holidays));
    assertEquals(test.getWeekendDays(), ImmutableSet.of(SATURDAY, SUNDAY));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "createThuFriWeekend")
  static Object[][] data_createThuFriWeekend() {
    return new Object[][] {
        {FRI_2014_07_11, false},
        {SAT_2014_07_12, true},
        {SUN_2014_07_13, true},
        {MON_2014_07_14, false},
        {TUE_2014_07_15, true},
        {WED_2014_07_16, true},
        {THU_2014_07_17, false},
        {FRI_2014_07_18, false},
        {SAT_2014_07_19, false},
        {SUN_2014_07_20, true},
        {MON_2014_07_21, true},
    };
  }

  @Test(dataProvider = "createThuFriWeekend")
  public void test_of_IterableDayOfWeekDayOfWeek_thuFriWeekend(LocalDate date, boolean isBusinessDay) {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, SAT_2014_07_19);
    ImmutableHolidayCalendar test = ImmutableHolidayCalendar.of(TEST_ID, holidays, THURSDAY, FRIDAY);
    assertEquals(test.isBusinessDay(date), isBusinessDay);
    assertEquals(test.isHoliday(date), !isBusinessDay);
    assertEquals(test.getHolidays(), ImmutableSortedSet.copyOf(holidays));
    assertEquals(test.getWeekendDays(), ImmutableSet.of(THURSDAY, FRIDAY));
    assertEquals(test.toString(), "HolidayCalendar[" + TEST_ID.getName() + "]");
  }

  @Test(dataProvider = "createThuFriWeekend")
  public void test_of_IterableIterable_thuFriWeekend(LocalDate date, boolean isBusinessDay) {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, SAT_2014_07_19);
    Iterable<DayOfWeek> weekendDays = Arrays.asList(THURSDAY, FRIDAY);
    ImmutableHolidayCalendar test = ImmutableHolidayCalendar.of(TEST_ID, holidays, weekendDays);
    assertEquals(test.isBusinessDay(date), isBusinessDay);
    assertEquals(test.isHoliday(date), !isBusinessDay);
    assertEquals(test.getHolidays(), ImmutableSortedSet.copyOf(holidays));
    assertEquals(test.getWeekendDays(), ImmutableSet.of(THURSDAY, FRIDAY));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "createSunWeekend")
  static Object[][] data_createSunWeekend() {
    return new Object[][] {
        {FRI_2014_07_11, true},
        {SAT_2014_07_12, true},
        {SUN_2014_07_13, false},
        {MON_2014_07_14, false},
        {TUE_2014_07_15, true},
        {WED_2014_07_16, true},
        {THU_2014_07_17, false},
        {FRI_2014_07_18, true},
        {SAT_2014_07_19, true},
        {SUN_2014_07_20, false},
        {MON_2014_07_21, true},
    };
  }

  @Test(dataProvider = "createSunWeekend")
  public void test_of_IterableDayOfWeekDayOfWeek_sunWeekend(LocalDate date, boolean isBusinessDay) {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, THU_2014_07_17);
    ImmutableHolidayCalendar test = ImmutableHolidayCalendar.of(TEST_ID, holidays, SUNDAY, SUNDAY);
    assertEquals(test.isBusinessDay(date), isBusinessDay);
    assertEquals(test.isHoliday(date), !isBusinessDay);
    assertEquals(test.getHolidays(), ImmutableSortedSet.copyOf(holidays));
    assertEquals(test.getWeekendDays(), ImmutableSet.of(SUNDAY));
    assertEquals(test.toString(), "HolidayCalendar[" + TEST_ID.getName() + "]");
  }

  @Test(dataProvider = "createSunWeekend")
  public void test_of_IterableIterable_sunWeekend(LocalDate date, boolean isBusinessDay) {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, THU_2014_07_17);
    Iterable<DayOfWeek> weekendDays = Arrays.asList(SUNDAY);
    ImmutableHolidayCalendar test = ImmutableHolidayCalendar.of(TEST_ID, holidays, weekendDays);
    assertEquals(test.isBusinessDay(date), isBusinessDay);
    assertEquals(test.isHoliday(date), !isBusinessDay);
    assertEquals(test.getHolidays(), ImmutableSortedSet.copyOf(holidays));
    assertEquals(test.getWeekendDays(), ImmutableSet.of(SUNDAY));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "createThuFriSatWeekend")
  static Object[][] data_createThuFriSatWeekend() {
    return new Object[][] {
        {FRI_2014_07_11, false},
        {SAT_2014_07_12, false},
        {SUN_2014_07_13, true},
        {MON_2014_07_14, false},
        {TUE_2014_07_15, false},
        {WED_2014_07_16, true},
        {THU_2014_07_17, false},
        {FRI_2014_07_18, false},
        {SAT_2014_07_19, false},
        {SUN_2014_07_20, true},
        {MON_2014_07_21, true},
    };
  }

  @Test(dataProvider = "createThuFriSatWeekend")
  public void test_of_IterableIterable_thuFriSatWeekend(LocalDate date, boolean isBusinessDay) {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, TUE_2014_07_15);
    Iterable<DayOfWeek> weekendDays = Arrays.asList(THURSDAY, FRIDAY, SATURDAY);
    ImmutableHolidayCalendar test = ImmutableHolidayCalendar.of(TEST_ID, holidays, weekendDays);
    assertEquals(test.isBusinessDay(date), isBusinessDay);
    assertEquals(test.isHoliday(date), !isBusinessDay);
    assertEquals(test.getHolidays(), ImmutableSortedSet.copyOf(holidays));
    assertEquals(test.getWeekendDays(), ImmutableSet.of(THURSDAY, FRIDAY, SATURDAY));
    assertEquals(test.toString(), "HolidayCalendar[" + TEST_ID.getName() + "]");
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "createNoWeekends")
  static Object[][] data_createNoWeekends() {
    return new Object[][] {
        {FRI_2014_07_11, true},
        {SAT_2014_07_12, true},
        {SUN_2014_07_13, true},
        {MON_2014_07_14, false},
        {TUE_2014_07_15, true},
        {WED_2014_07_16, true},
        {THU_2014_07_17, true},
        {FRI_2014_07_18, false},
        {SAT_2014_07_19, true},
        {SUN_2014_07_20, true},
        {MON_2014_07_21, true},
    };
  }

  @Test(dataProvider = "createNoWeekends")
  public void test_of_IterableIterable_noWeekends(LocalDate date, boolean isBusinessDay) {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, FRI_2014_07_18);
    Iterable<DayOfWeek> weekendDays = Arrays.asList();
    ImmutableHolidayCalendar test = ImmutableHolidayCalendar.of(TEST_ID, holidays, weekendDays);
    assertEquals(test.isBusinessDay(date), isBusinessDay);
    assertEquals(test.isHoliday(date), !isBusinessDay);
    assertEquals(test.getHolidays(), ImmutableSortedSet.copyOf(holidays));
    assertEquals(test.getWeekendDays(), ImmutableSet.of());
    assertEquals(test.toString(), "HolidayCalendar[" + TEST_ID.getName() + "]");
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "createNoHolidays")
  static Object[][] data_createNoHolidays() {
    return new Object[][] {
        {FRI_2014_07_11, false},
        {SAT_2014_07_12, false},
        {SUN_2014_07_13, true},
        {MON_2014_07_14, true},
        {TUE_2014_07_15, true},
        {WED_2014_07_16, true},
        {THU_2014_07_17, true},
        {FRI_2014_07_18, false},
        {SAT_2014_07_19, false},
        {SUN_2014_07_20, true},
        {MON_2014_07_21, true},
    };
  }

  @Test(dataProvider = "createNoHolidays")
  public void test_of_IterableIterable_noHolidays(LocalDate date, boolean isBusinessDay) {
    Iterable<LocalDate> holidays = Arrays.asList();
    Iterable<DayOfWeek> weekendDays = Arrays.asList(FRIDAY, SATURDAY);
    ImmutableHolidayCalendar test = ImmutableHolidayCalendar.of(TEST_ID, holidays, weekendDays);
    assertEquals(test.isBusinessDay(date), isBusinessDay);
    assertEquals(test.isHoliday(date), !isBusinessDay);
    assertEquals(test.getHolidays(), ImmutableSortedSet.copyOf(holidays));
    assertEquals(test.getWeekendDays(), ImmutableSet.of(FRIDAY, SATURDAY));
    assertEquals(test.toString(), "HolidayCalendar[" + TEST_ID.getName() + "]");
  }

  //-------------------------------------------------------------------------
  public void test_combined() {
    ImmutableHolidayCalendar base1 =
        ImmutableHolidayCalendar.of(TEST_ID, ImmutableList.of(MON_2014_07_14), SATURDAY, SUNDAY);
    ImmutableHolidayCalendar base2 =
        ImmutableHolidayCalendar.of(TEST_ID2, ImmutableList.of(WED_2014_07_16), FRIDAY, SATURDAY);

    ImmutableHolidayCalendar test = ImmutableHolidayCalendar.combined(base1, base2);
    assertEquals(test.getId(), base1.getId().combinedWith(base2.getId()));
    assertEquals(test.getName(), base1.getId().combinedWith(base2.getId()).getName());
    assertEquals(test.getHolidays(), ImmutableList.of(MON_2014_07_14, WED_2014_07_16));
    assertEquals(test.getWeekendDays(), ImmutableSet.of(FRIDAY, SATURDAY, SUNDAY));
  }

  public void test_combined_same() {
    ImmutableHolidayCalendar base =
        ImmutableHolidayCalendar.of(TEST_ID, ImmutableList.of(MON_2014_07_14), SATURDAY, SUNDAY);

    ImmutableHolidayCalendar test = ImmutableHolidayCalendar.combined(base, base);
    assertSame(test, base);
  }

  //-------------------------------------------------------------------------
  public void test_beanBuilder() {
    ImmutableSortedSet<LocalDate> holidays = ImmutableSortedSet.of(MON_2014_07_14, TUE_2014_07_15);
    ImmutableSortedSet<DayOfWeek> weekendDays = ImmutableSortedSet.of(SATURDAY, SUNDAY);
    ImmutableHolidayCalendar test = ImmutableHolidayCalendar.meta().builder()
        .set(ImmutableHolidayCalendar.meta().id(), TEST_ID)
        .set(ImmutableHolidayCalendar.meta().holidays(), holidays)
        .set(ImmutableHolidayCalendar.meta().weekendDays(), weekendDays)
        .build();
    assertEquals(test.getHolidays(), holidays);
    assertEquals(test.getWeekendDays(), weekendDays);
  }

  public void test_beanBuilder_noHolidays() {
    ImmutableSortedSet<LocalDate> holidays = ImmutableSortedSet.of();
    ImmutableSortedSet<DayOfWeek> weekendDays = ImmutableSortedSet.of(SATURDAY, SUNDAY);
    ImmutableHolidayCalendar test = ImmutableHolidayCalendar.meta().builder()
        .set(ImmutableHolidayCalendar.meta().id(), TEST_ID)
        .set(ImmutableHolidayCalendar.meta().holidays(), holidays)
        .set(ImmutableHolidayCalendar.meta().weekendDays(), weekendDays)
        .build();
    assertEquals(test.getHolidays(), holidays);
    assertEquals(test.getWeekendDays(), weekendDays);
  }

  //-------------------------------------------------------------------------
  public void test_isBusinessDay_outOfRange() {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, TUE_2014_07_15);
    ImmutableHolidayCalendar test = ImmutableHolidayCalendar.of(TEST_ID, holidays, SATURDAY, SUNDAY);
    assertEquals(test.isBusinessDay(LocalDate.of(2013, 12, 31)), true);
    assertEquals(test.isBusinessDay(LocalDate.of(2015, 1, 1)), true);
    assertThrowsIllegalArg(() -> test.isBusinessDay(LocalDate.MIN));
    assertThrowsIllegalArg(() -> test.isBusinessDay(LocalDate.MAX));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "shift")
  static Object[][] data_shift() {
    return new Object[][] {
        {THU_2014_07_10, 1, FRI_2014_07_11},
        {FRI_2014_07_11, 1, TUE_2014_07_15},
        {SAT_2014_07_12, 1, TUE_2014_07_15},
        {SUN_2014_07_13, 1, TUE_2014_07_15},
        {MON_2014_07_14, 1, TUE_2014_07_15},
        {TUE_2014_07_15, 1, THU_2014_07_17},
        {WED_2014_07_16, 1, THU_2014_07_17},
        {THU_2014_07_17, 1, FRI_2014_07_18},
        {FRI_2014_07_18, 1, MON_2014_07_21},
        {SAT_2014_07_19, 1, MON_2014_07_21},
        {SUN_2014_07_20, 1, MON_2014_07_21},
        {MON_2014_07_21, 1, TUE_2014_07_22},

        {THU_2014_07_10, 2, TUE_2014_07_15},
        {FRI_2014_07_11, 2, THU_2014_07_17},
        {SAT_2014_07_12, 2, THU_2014_07_17},
        {SUN_2014_07_13, 2, THU_2014_07_17},
        {MON_2014_07_14, 2, THU_2014_07_17},
        {TUE_2014_07_15, 2, FRI_2014_07_18},
        {WED_2014_07_16, 2, FRI_2014_07_18},
        {THU_2014_07_17, 2, MON_2014_07_21},
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
        {TUE_2014_07_15, -1, FRI_2014_07_11},
        {WED_2014_07_16, -1, TUE_2014_07_15},
        {THU_2014_07_17, -1, TUE_2014_07_15},
        {FRI_2014_07_18, -1, THU_2014_07_17},
        {SAT_2014_07_19, -1, FRI_2014_07_18},
        {SUN_2014_07_20, -1, FRI_2014_07_18},
        {MON_2014_07_21, -1, FRI_2014_07_18},
        {TUE_2014_07_22, -1, MON_2014_07_21},

        {FRI_2014_07_11, -2, WED_2014_07_09},
        {SAT_2014_07_12, -2, THU_2014_07_10},
        {SUN_2014_07_13, -2, THU_2014_07_10},
        {MON_2014_07_14, -2, THU_2014_07_10},
        {TUE_2014_07_15, -2, THU_2014_07_10},
        {WED_2014_07_16, -2, FRI_2014_07_11},
        {THU_2014_07_17, -2, FRI_2014_07_11},
        {FRI_2014_07_18, -2, TUE_2014_07_15},
        {SAT_2014_07_19, -2, THU_2014_07_17},
        {SUN_2014_07_20, -2, THU_2014_07_17},
        {MON_2014_07_21, -2, THU_2014_07_17},
        {TUE_2014_07_22, -2, FRI_2014_07_18},
    };
  }

  @Test(dataProvider = "shift")
  public void test_shift(LocalDate date, int amount, LocalDate expected) {
    assertEquals(HOLCAL_MON_WED.shift(date, amount), expected);
  }

  public void test_shift_SatSun() {
    assertEquals(HOLCAL_SAT_SUN.shift(SAT_2014_07_12, -2), THU_2014_07_10);
    assertEquals(HOLCAL_SAT_SUN.shift(SAT_2014_07_12, 2), TUE_2014_07_15);
  }

  public void test_shift_range() {
    assertEquals(HOLCAL_MON_WED.shift(date(2010, 1, 1), 1), date(2010, 1, 4));
    assertThrowsIllegalArg(() -> HOLCAL_MON_WED.shift(LocalDate.MIN, 1));
    assertThrowsIllegalArg(() -> HOLCAL_MON_WED.shift(LocalDate.MAX.minusDays(1), 1));
  }

  @Test(dataProvider = "shift")
  public void test_adjustBy(LocalDate date, int amount, LocalDate expected) {
    assertEquals(date.with(HOLCAL_MON_WED.adjustBy(amount)), expected);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "next")
  static Object[][] data_next() {
    return new Object[][] {
        {THU_2014_07_10, FRI_2014_07_11, HOLCAL_MON_WED},
        {FRI_2014_07_11, TUE_2014_07_15, HOLCAL_MON_WED},
        {SAT_2014_07_12, TUE_2014_07_15, HOLCAL_MON_WED},
        {SUN_2014_07_13, TUE_2014_07_15, HOLCAL_MON_WED},
        {MON_2014_07_14, TUE_2014_07_15, HOLCAL_MON_WED},
        {TUE_2014_07_15, THU_2014_07_17, HOLCAL_MON_WED},
        {WED_2014_07_16, THU_2014_07_17, HOLCAL_MON_WED},
        {THU_2014_07_17, FRI_2014_07_18, HOLCAL_MON_WED},
        {FRI_2014_07_18, MON_2014_07_21, HOLCAL_MON_WED},
        {SAT_2014_07_19, MON_2014_07_21, HOLCAL_MON_WED},
        {SUN_2014_07_20, MON_2014_07_21, HOLCAL_MON_WED},
        {MON_2014_07_21, TUE_2014_07_22, HOLCAL_MON_WED},

        {MON_2014_12_29, WED_2014_12_31, HOLCAL_YEAR_END},
        {TUE_2014_12_30, WED_2014_12_31, HOLCAL_YEAR_END},
        {WED_2014_12_31, FRI_2015_01_02, HOLCAL_YEAR_END},
        {THU_2015_01_01, FRI_2015_01_02, HOLCAL_YEAR_END},
        {FRI_2015_01_02, MON_2015_01_05, HOLCAL_YEAR_END},
        {SAT_2015_01_03, MON_2015_01_05, HOLCAL_YEAR_END},

        {TUE_2015_03_31, WED_2015_04_01, HOLCAL_YEAR_END},

        {SAT_2014_07_12, MON_2014_07_14, HOLCAL_SAT_SUN},
    };
  }

  @Test(dataProvider = "next")
  public void test_next(LocalDate date, LocalDate expectedNext, HolidayCalendar cal) {
    assertEquals(cal.next(date), expectedNext);
  }

  public void test_next_range() {
    assertEquals(HOLCAL_MON_WED.next(date(2010, 1, 1)), date(2010, 1, 4));
    assertThrowsIllegalArg(() -> HOLCAL_MON_WED.next(LocalDate.MIN));
    assertThrowsIllegalArg(() -> HOLCAL_MON_WED.next(LocalDate.MAX.minusDays(1)));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "nextOrSame")
  static Object[][] data_nextOrSame() {
    return new Object[][] {
        {THU_2014_07_10, THU_2014_07_10, HOLCAL_MON_WED},
        {FRI_2014_07_11, FRI_2014_07_11, HOLCAL_MON_WED},
        {SAT_2014_07_12, TUE_2014_07_15, HOLCAL_MON_WED},
        {SUN_2014_07_13, TUE_2014_07_15, HOLCAL_MON_WED},
        {MON_2014_07_14, TUE_2014_07_15, HOLCAL_MON_WED},
        {TUE_2014_07_15, TUE_2014_07_15, HOLCAL_MON_WED},
        {WED_2014_07_16, THU_2014_07_17, HOLCAL_MON_WED},
        {THU_2014_07_17, THU_2014_07_17, HOLCAL_MON_WED},
        {FRI_2014_07_18, FRI_2014_07_18, HOLCAL_MON_WED},
        {SAT_2014_07_19, MON_2014_07_21, HOLCAL_MON_WED},
        {SUN_2014_07_20, MON_2014_07_21, HOLCAL_MON_WED},
        {MON_2014_07_21, MON_2014_07_21, HOLCAL_MON_WED},

        {MON_2014_12_29, MON_2014_12_29, HOLCAL_YEAR_END},
        {TUE_2014_12_30, WED_2014_12_31, HOLCAL_YEAR_END},
        {WED_2014_12_31, WED_2014_12_31, HOLCAL_YEAR_END},
        {THU_2015_01_01, FRI_2015_01_02, HOLCAL_YEAR_END},
        {FRI_2015_01_02, FRI_2015_01_02, HOLCAL_YEAR_END},
        {SAT_2015_01_03, MON_2015_01_05, HOLCAL_YEAR_END},

        {TUE_2015_03_31, TUE_2015_03_31, HOLCAL_YEAR_END},
        {WED_2015_04_01, WED_2015_04_01, HOLCAL_YEAR_END},

        {SAT_2014_07_12, MON_2014_07_14, HOLCAL_SAT_SUN},
    };
  }

  @Test(dataProvider = "nextOrSame")
  public void test_nextOrSame(LocalDate date, LocalDate expectedNext, HolidayCalendar cal) {
    assertEquals(cal.nextOrSame(date), expectedNext);
  }

  public void test_nextOrSame_range() {
    assertEquals(HOLCAL_MON_WED.nextOrSame(date(2010, 1, 1)), date(2010, 1, 1));
    assertThrowsIllegalArg(() -> HOLCAL_MON_WED.nextOrSame(LocalDate.MIN));
    assertThrowsIllegalArg(() -> HOLCAL_MON_WED.nextOrSame(LocalDate.MAX));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "previous")
  static Object[][] data_previous() {
    return new Object[][] {
        {FRI_2014_07_11, THU_2014_07_10, HOLCAL_MON_WED},
        {SAT_2014_07_12, FRI_2014_07_11, HOLCAL_MON_WED},
        {SUN_2014_07_13, FRI_2014_07_11, HOLCAL_MON_WED},
        {MON_2014_07_14, FRI_2014_07_11, HOLCAL_MON_WED},
        {TUE_2014_07_15, FRI_2014_07_11, HOLCAL_MON_WED},
        {WED_2014_07_16, TUE_2014_07_15, HOLCAL_MON_WED},
        {THU_2014_07_17, TUE_2014_07_15, HOLCAL_MON_WED},
        {FRI_2014_07_18, THU_2014_07_17, HOLCAL_MON_WED},
        {SAT_2014_07_19, FRI_2014_07_18, HOLCAL_MON_WED},
        {SUN_2014_07_20, FRI_2014_07_18, HOLCAL_MON_WED},
        {MON_2014_07_21, FRI_2014_07_18, HOLCAL_MON_WED},
        {TUE_2014_07_22, MON_2014_07_21, HOLCAL_MON_WED},

        {TUE_2014_12_30, MON_2014_12_29, HOLCAL_YEAR_END},
        {WED_2014_12_31, MON_2014_12_29, HOLCAL_YEAR_END},
        {THU_2015_01_01, WED_2014_12_31, HOLCAL_YEAR_END},
        {FRI_2015_01_02, WED_2014_12_31, HOLCAL_YEAR_END},
        {SAT_2015_01_03, FRI_2015_01_02, HOLCAL_YEAR_END},
        {MON_2015_01_05, FRI_2015_01_02, HOLCAL_YEAR_END},

        {WED_2015_04_01, TUE_2015_03_31, HOLCAL_YEAR_END},

        {SAT_2014_07_12, FRI_2014_07_11, HOLCAL_SAT_SUN},
    };
  }

  @Test(dataProvider = "previous")
  public void test_previous(LocalDate date, LocalDate expectedPrevious, HolidayCalendar cal) {
    assertEquals(cal.previous(date), expectedPrevious);
  }

  public void test_previous_range() {
    assertEquals(HOLCAL_MON_WED.previous(date(2010, 1, 1)), date(2009, 12, 31));
    assertThrowsIllegalArg(() -> HOLCAL_MON_WED.previous(LocalDate.MIN.plusDays(1)));
    assertThrowsIllegalArg(() -> HOLCAL_MON_WED.previous(LocalDate.MAX));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "previousOrSame")
  static Object[][] data_previousOrSame() {
    return new Object[][] {
        {FRI_2014_07_11, FRI_2014_07_11, HOLCAL_MON_WED},
        {SAT_2014_07_12, FRI_2014_07_11, HOLCAL_MON_WED},
        {SUN_2014_07_13, FRI_2014_07_11, HOLCAL_MON_WED},
        {MON_2014_07_14, FRI_2014_07_11, HOLCAL_MON_WED},
        {TUE_2014_07_15, TUE_2014_07_15, HOLCAL_MON_WED},
        {WED_2014_07_16, TUE_2014_07_15, HOLCAL_MON_WED},
        {THU_2014_07_17, THU_2014_07_17, HOLCAL_MON_WED},
        {FRI_2014_07_18, FRI_2014_07_18, HOLCAL_MON_WED},
        {SAT_2014_07_19, FRI_2014_07_18, HOLCAL_MON_WED},
        {SUN_2014_07_20, FRI_2014_07_18, HOLCAL_MON_WED},
        {MON_2014_07_21, MON_2014_07_21, HOLCAL_MON_WED},
        {TUE_2014_07_22, TUE_2014_07_22, HOLCAL_MON_WED},

        {MON_2014_12_29, MON_2014_12_29, HOLCAL_YEAR_END},
        {TUE_2014_12_30, MON_2014_12_29, HOLCAL_YEAR_END},
        {WED_2014_12_31, WED_2014_12_31, HOLCAL_YEAR_END},
        {THU_2015_01_01, WED_2014_12_31, HOLCAL_YEAR_END},
        {FRI_2015_01_02, FRI_2015_01_02, HOLCAL_YEAR_END},
        {SAT_2015_01_03, FRI_2015_01_02, HOLCAL_YEAR_END},
        {MON_2015_01_05, MON_2015_01_05, HOLCAL_YEAR_END},

        {TUE_2015_03_31, TUE_2015_03_31, HOLCAL_YEAR_END},
        {WED_2015_04_01, WED_2015_04_01, HOLCAL_YEAR_END},

        {SAT_2014_07_12, FRI_2014_07_11, HOLCAL_SAT_SUN},
    };
  }

  @Test(dataProvider = "previousOrSame")
  public void test_previousOrSame(LocalDate date, LocalDate expectedPrevious, HolidayCalendar cal) {
    assertEquals(cal.previousOrSame(date), expectedPrevious);
  }

  public void test_previousOrSame_range() {
    assertEquals(HOLCAL_MON_WED.previousOrSame(date(2010, 1, 1)), date(2010, 1, 1));
    assertThrowsIllegalArg(() -> HOLCAL_MON_WED.previousOrSame(LocalDate.MIN));
    assertThrowsIllegalArg(() -> HOLCAL_MON_WED.previousOrSame(LocalDate.MAX));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "nextSameOrLastInMonth")
  static Object[][] data_nextSameOrLastInMonth() {
    return new Object[][] {
        {THU_2014_07_10, THU_2014_07_10, HOLCAL_MON_WED},
        {FRI_2014_07_11, FRI_2014_07_11, HOLCAL_MON_WED},
        {SAT_2014_07_12, TUE_2014_07_15, HOLCAL_MON_WED},
        {SUN_2014_07_13, TUE_2014_07_15, HOLCAL_MON_WED},
        {MON_2014_07_14, TUE_2014_07_15, HOLCAL_MON_WED},
        {TUE_2014_07_15, TUE_2014_07_15, HOLCAL_MON_WED},
        {WED_2014_07_16, THU_2014_07_17, HOLCAL_MON_WED},
        {THU_2014_07_17, THU_2014_07_17, HOLCAL_MON_WED},
        {FRI_2014_07_18, FRI_2014_07_18, HOLCAL_MON_WED},
        {SAT_2014_07_19, MON_2014_07_21, HOLCAL_MON_WED},
        {SUN_2014_07_20, MON_2014_07_21, HOLCAL_MON_WED},
        {MON_2014_07_21, MON_2014_07_21, HOLCAL_MON_WED},

        {MON_2014_12_29, MON_2014_12_29, HOLCAL_YEAR_END},
        {TUE_2014_12_30, WED_2014_12_31, HOLCAL_YEAR_END},
        {WED_2014_12_31, WED_2014_12_31, HOLCAL_YEAR_END},
        {THU_2015_01_01, FRI_2015_01_02, HOLCAL_YEAR_END},
        {FRI_2015_01_02, FRI_2015_01_02, HOLCAL_YEAR_END},
        {SAT_2015_01_03, MON_2015_01_05, HOLCAL_YEAR_END},

        {TUE_2015_03_31, TUE_2015_03_31, HOLCAL_YEAR_END},
        {WED_2015_04_01, WED_2015_04_01, HOLCAL_YEAR_END},

        {SAT_2014_07_12, MON_2014_07_14, HOLCAL_SAT_SUN},
        {SAT_2015_02_28, FRI_2015_02_27, HOLCAL_SAT_SUN},

        {WED_2014_07_30, WED_2014_07_30, HOLCAL_END_MONTH},
        {THU_2014_07_31, WED_2014_07_30, HOLCAL_END_MONTH},
    };
  }

  @Test(dataProvider = "nextSameOrLastInMonth")
  public void test_nextLastOrSame(LocalDate date, LocalDate expectedNext, HolidayCalendar cal) {
    assertEquals(cal.nextSameOrLastInMonth(date), expectedNext);
  }

  public void test_nextSameOrLastInMonth_range() {
    assertEquals(HOLCAL_MON_WED.nextSameOrLastInMonth(date(2010, 1, 1)), date(2010, 1, 1));
    assertThrowsIllegalArg(() -> HOLCAL_MON_WED.nextSameOrLastInMonth(LocalDate.MIN));
    assertThrowsIllegalArg(() -> HOLCAL_MON_WED.nextSameOrLastInMonth(LocalDate.MAX));
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
    assertEquals(HOLCAL_END_MONTH.lastBusinessDayOfMonth(date), expectedEom);
  }

  @Test(dataProvider = "lastBusinessDayOfMonth")
  public void test_isLastBusinessDayOfMonth(LocalDate date, LocalDate expectedEom) {
    assertEquals(HOLCAL_END_MONTH.isLastBusinessDayOfMonth(date), date.equals(expectedEom));
  }

  public void test_lastBusinessDayOfMonth_satSun() {
    assertEquals(HOLCAL_SAT_SUN.isLastBusinessDayOfMonth(MON_2014_06_30), true);
    assertEquals(HOLCAL_SAT_SUN.lastBusinessDayOfMonth(MON_2014_06_30), MON_2014_06_30);
  }

  public void test_lastBusinessDayOfMonth_range() {
    assertEquals(HOLCAL_END_MONTH.lastBusinessDayOfMonth(date(2010, 1, 1)), date(2010, 1, 29));
    assertThrowsIllegalArg(() -> HOLCAL_END_MONTH.lastBusinessDayOfMonth(LocalDate.MIN));
    assertThrowsIllegalArg(() -> HOLCAL_END_MONTH.lastBusinessDayOfMonth(LocalDate.MAX));
  }

  public void test_isLastBusinessDayOfMonth_range() {
    assertEquals(HOLCAL_END_MONTH.isLastBusinessDayOfMonth(date(2010, 1, 1)), false);
    assertThrowsIllegalArg(() -> HOLCAL_END_MONTH.isLastBusinessDayOfMonth(LocalDate.MIN));
    assertThrowsIllegalArg(() -> HOLCAL_END_MONTH.isLastBusinessDayOfMonth(LocalDate.MAX));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "daysBetween")
  static Object[][] data_daysBetween() {
    return new Object[][] {
        {FRI_2014_07_11, FRI_2014_07_11, 0},
        {FRI_2014_07_11, SAT_2014_07_12, 1},
        {FRI_2014_07_11, SUN_2014_07_13, 1},
        {FRI_2014_07_11, MON_2014_07_14, 1},
        {FRI_2014_07_11, TUE_2014_07_15, 1},
        {FRI_2014_07_11, WED_2014_07_16, 2},
        {FRI_2014_07_11, THU_2014_07_17, 2},
        {FRI_2014_07_11, FRI_2014_07_18, 3},
        {FRI_2014_07_11, SAT_2014_07_19, 4},
        {FRI_2014_07_11, SUN_2014_07_20, 4},
        {FRI_2014_07_11, MON_2014_07_21, 4},
        {FRI_2014_07_11, TUE_2014_07_22, 5},
    };
  }

  @Test(dataProvider = "daysBetween")
  public void test_daysBetween_LocalDateLocalDate(LocalDate start, LocalDate end, int expected) {
    assertEquals(HOLCAL_MON_WED.daysBetween(start, end), expected);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith() {
    Iterable<LocalDate> holidays1 = Arrays.asList(WED_2014_07_16);
    ImmutableHolidayCalendar base1 = ImmutableHolidayCalendar.of(TEST_ID, holidays1, SATURDAY, SUNDAY);
    Iterable<LocalDate> holidays2 = Arrays.asList(MON_2014_07_14);
    ImmutableHolidayCalendar base2 = ImmutableHolidayCalendar.of(TEST_ID2, holidays2, FRIDAY, SATURDAY);
    HolidayCalendar test = base1.combinedWith(base2);
    assertEquals(test.getName(), "Test1+Test2");

    assertEquals(test.isHoliday(THU_2014_07_10), false);
    assertEquals(test.isHoliday(FRI_2014_07_11), true);
    assertEquals(test.isHoliday(SAT_2014_07_12), true);
    assertEquals(test.isHoliday(SUN_2014_07_13), true);
    assertEquals(test.isHoliday(MON_2014_07_14), true);
    assertEquals(test.isHoliday(TUE_2014_07_15), false);
    assertEquals(test.isHoliday(WED_2014_07_16), true);
    assertEquals(test.isHoliday(THU_2014_07_17), false);
    assertEquals(test.isHoliday(FRI_2014_07_18), true);
    assertEquals(test.isHoliday(SAT_2014_07_19), true);
    assertEquals(test.isHoliday(SUN_2014_07_20), true);
    assertEquals(test.isHoliday(MON_2014_07_21), false);
  }

  public void test_combineWith_same() {
    Iterable<LocalDate> holidays = Arrays.asList(WED_2014_07_16);
    ImmutableHolidayCalendar base = ImmutableHolidayCalendar.of(TEST_ID, holidays, SATURDAY, SUNDAY);
    HolidayCalendar test = base.combinedWith(base);
    assertSame(test, base);
  }

  public void test_combineWith_none() {
    Iterable<LocalDate> holidays = Arrays.asList(WED_2014_07_16);
    ImmutableHolidayCalendar base = ImmutableHolidayCalendar.of(TEST_ID, holidays, SATURDAY, SUNDAY);
    HolidayCalendar test = base.combinedWith(HolidayCalendars.NO_HOLIDAYS);
    assertSame(test, base);
  }

  public void test_combineWith_satSun() {
    Iterable<LocalDate> holidays = Arrays.asList(WED_2014_07_16);
    ImmutableHolidayCalendar base = ImmutableHolidayCalendar.of(TEST_ID, holidays, SATURDAY, SUNDAY);
    HolidayCalendar test = base.combinedWith(HolidayCalendars.FRI_SAT);
    assertEquals(test.getName(), "Fri/Sat+Test1");

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

  //-------------------------------------------------------------------------
  public void test_broadCheck() {
    LocalDate start = LocalDate.of(2010, 1, 1);
    LocalDate end = LocalDate.of(2020, 1, 1);
    Random random = new Random(547698);
    for (int i = 0; i < 10; i++) {
      // create sample holiday dates
      LocalDate date = start;
      SortedSet<LocalDate> set = new TreeSet<>();
      while (date.isBefore(end)) {
        set.add(date);
        date = date.plusDays(random.nextInt(10) + 1);
      }
      // check holiday calendar works using simple algorithm
      ImmutableHolidayCalendar test = ImmutableHolidayCalendar.of(
          HolidayCalendarId.of("TestBroad" + i), set, SATURDAY, SUNDAY);
      LocalDate checkDate = start;
      while (checkDate.isBefore(end)) {
        DayOfWeek dow = checkDate.getDayOfWeek();
        assertEquals(test.isHoliday(checkDate), dow == SATURDAY || dow == SUNDAY || set.contains(checkDate));
        checkDate = checkDate.plusDays(1);
      }
    }
  }

  //-------------------------------------------------------------------------
  public void test_equals() {
    ImmutableHolidayCalendar a1 = ImmutableHolidayCalendar.of(TEST_ID, Arrays.asList(WED_2014_07_16), SATURDAY, SUNDAY);
    ImmutableHolidayCalendar a2 = ImmutableHolidayCalendar.of(TEST_ID, Arrays.asList(WED_2014_07_16), SATURDAY, SUNDAY);
    ImmutableHolidayCalendar b = ImmutableHolidayCalendar.of(TEST_ID2, Arrays.asList(WED_2014_07_16), SATURDAY, SUNDAY);
    ImmutableHolidayCalendar c = ImmutableHolidayCalendar.of(TEST_ID, Arrays.asList(THU_2014_07_10), SATURDAY, SUNDAY);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(c), true);  // only name compared
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(HOLCAL_MON_WED);
  }

  public void test_serialization() {
    assertSerialization(HOLCAL_MON_WED);
  }

}
