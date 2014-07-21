/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import static com.opengamma.collect.TestHelper.assertThrows;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static org.testng.Assert.assertEquals;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;

import org.joda.beans.BeanBuilder;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.opengamma.collect.range.LocalDateRange;

/**
 * Test {@link BusinessDayCalendar}.
 */
@Test
public class BusinessDayCalendarTest {

  private static final LocalDateRange RANGE_2014 = LocalDateRange.closed(
      LocalDate.of(2014,  1, 1), LocalDate.of(2014,  12, 31));

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

  //-------------------------------------------------------------------------
  public void test_ALL() {
    BusinessDayCalendar test = BusinessDayCalendar.ALL;
    LocalDateRange range = LocalDateRange.closed(LocalDate.of(2011, 1, 1), LocalDate.of(2015, 1, 31));
    range.stream().forEach(date -> {
      assertEquals(test.isBusinessDay(date), true);
      assertEquals(test.isNonBusinessDay(date), false);
    });
  }

  public void test_WEEKENDS() {
    BusinessDayCalendar test = BusinessDayCalendar.WEEKENDS;
    LocalDateRange range = LocalDateRange.closed(LocalDate.of(2011, 1, 1), LocalDate.of(2015, 1, 31));
    range.stream().forEach(date -> {
      boolean isBusinessDay = date.getDayOfWeek() != SATURDAY && date.getDayOfWeek() != SUNDAY;
      assertEquals(test.isBusinessDay(date), isBusinessDay);
      assertEquals(test.isNonBusinessDay(date), !isBusinessDay);
    });
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "createSatSunWeekend")
  Object[][] data_createSatSunWeekend() {
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
  public void test_ofIterableDayOfWeekDayOfWeek_satSunWeekend(LocalDate date, boolean isBusinessDay) {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, FRI_2014_07_18);
    BusinessDayCalendar test = BusinessDayCalendar.of(holidays, SATURDAY, SUNDAY);
    assertEquals(test.isBusinessDay(date), isBusinessDay);
    assertEquals(test.isNonBusinessDay(date), !isBusinessDay);
    assertEquals(test.getHolidays(), ImmutableSortedSet.copyOf(holidays));
    assertEquals(test.getWeekendDays(), ImmutableSet.of(SATURDAY, SUNDAY));
    assertEquals(test.getRange(), RANGE_2014);
    assertEquals(test.toString(), "BusinessDayCalendar" + RANGE_2014);
  }

  @Test(dataProvider = "createSatSunWeekend")
  public void test_ofIterableIterable_satSunWeekend(LocalDate date, boolean isBusinessDay) {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, FRI_2014_07_18);
    Iterable<DayOfWeek> weekendDays = Arrays.asList(SATURDAY, SUNDAY);
    BusinessDayCalendar test = BusinessDayCalendar.of(holidays, weekendDays);
    assertEquals(test.isBusinessDay(date), isBusinessDay);
    assertEquals(test.isNonBusinessDay(date), !isBusinessDay);
    assertEquals(test.getHolidays(), ImmutableSortedSet.copyOf(holidays));
    assertEquals(test.getWeekendDays(), ImmutableSet.of(SATURDAY, SUNDAY));
    assertEquals(test.getRange(), RANGE_2014);
    assertEquals(test.toString(), "BusinessDayCalendar" + RANGE_2014);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "createThuFriWeekend")
  Object[][] data_createThuFriWeekend() {
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
  public void test_ofIterableDayOfWeekDayOfWeek_thuFriWeekend(LocalDate date, boolean isBusinessDay) {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, SAT_2014_07_19);
    BusinessDayCalendar test = BusinessDayCalendar.of(holidays, THURSDAY, FRIDAY);
    assertEquals(test.isBusinessDay(date), isBusinessDay);
    assertEquals(test.isNonBusinessDay(date), !isBusinessDay);
    assertEquals(test.getHolidays(), ImmutableSortedSet.copyOf(holidays));
    assertEquals(test.getWeekendDays(), ImmutableSet.of(THURSDAY, FRIDAY));
    assertEquals(test.getRange(), RANGE_2014);
  }

  @Test(dataProvider = "createThuFriWeekend")
  public void test_ofIterableIterable_thuFriWeekend(LocalDate date, boolean isBusinessDay) {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, SAT_2014_07_19);
    Iterable<DayOfWeek> weekendDays = Arrays.asList(THURSDAY, FRIDAY);
    BusinessDayCalendar test = BusinessDayCalendar.of(holidays, weekendDays);
    assertEquals(test.isBusinessDay(date), isBusinessDay);
    assertEquals(test.isNonBusinessDay(date), !isBusinessDay);
    assertEquals(test.getHolidays(), ImmutableSortedSet.copyOf(holidays));
    assertEquals(test.getWeekendDays(), ImmutableSet.of(THURSDAY, FRIDAY));
    assertEquals(test.getRange(), RANGE_2014);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "createSunWeekend")
  Object[][] data_createSunWeekend() {
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
  public void test_ofIterableDayOfWeekDayOfWeek_sunWeekend(LocalDate date, boolean isBusinessDay) {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, THU_2014_07_17);
    BusinessDayCalendar test = BusinessDayCalendar.of(holidays, SUNDAY, SUNDAY);
    assertEquals(test.isBusinessDay(date), isBusinessDay);
    assertEquals(test.isNonBusinessDay(date), !isBusinessDay);
    assertEquals(test.getHolidays(), ImmutableSortedSet.copyOf(holidays));
    assertEquals(test.getWeekendDays(), ImmutableSet.of(SUNDAY));
    assertEquals(test.getRange(), RANGE_2014);
  }

  @Test(dataProvider = "createSunWeekend")
  public void test_ofIterableIterable_sunWeekend(LocalDate date, boolean isBusinessDay) {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, THU_2014_07_17);
    Iterable<DayOfWeek> weekendDays = Arrays.asList(SUNDAY);
    BusinessDayCalendar test = BusinessDayCalendar.of(holidays, weekendDays);
    assertEquals(test.isBusinessDay(date), isBusinessDay);
    assertEquals(test.isNonBusinessDay(date), !isBusinessDay);
    assertEquals(test.getHolidays(), ImmutableSortedSet.copyOf(holidays));
    assertEquals(test.getWeekendDays(), ImmutableSet.of(SUNDAY));
    assertEquals(test.getRange(), RANGE_2014);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "createThuFriSatWeekend")
  Object[][] data_createThuFriSatWeekend() {
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
  public void test_ofIterableIterable_thuFriSatWeekend(LocalDate date, boolean isBusinessDay) {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, TUE_2014_07_15);
    Iterable<DayOfWeek> weekendDays = Arrays.asList(THURSDAY, FRIDAY, SATURDAY);
    BusinessDayCalendar test = BusinessDayCalendar.of(holidays, weekendDays);
    assertEquals(test.isBusinessDay(date), isBusinessDay);
    assertEquals(test.isNonBusinessDay(date), !isBusinessDay);
    assertEquals(test.getHolidays(), ImmutableSortedSet.copyOf(holidays));
    assertEquals(test.getWeekendDays(), ImmutableSet.of(THURSDAY, FRIDAY, SATURDAY));
    assertEquals(test.getRange(), RANGE_2014);
  }

  //-------------------------------------------------------------------------
  public void test_beanBuilder() {
    ImmutableSortedSet<LocalDate> holidays = ImmutableSortedSet.of(MON_2014_07_14, TUE_2014_07_15);
    ImmutableSortedSet<DayOfWeek> weekendDays = ImmutableSortedSet.of(SATURDAY, SUNDAY);
    BusinessDayCalendar test = BusinessDayCalendar.meta().builder()
        .set(BusinessDayCalendar.meta().holidays(), holidays)
        .set(BusinessDayCalendar.meta().weekendDays(), weekendDays)
        .set(BusinessDayCalendar.meta().range(), RANGE_2014)
        .build();
    assertEquals(test.getHolidays(), holidays);
    assertEquals(test.getWeekendDays(), weekendDays);
    assertEquals(test.getRange(), RANGE_2014);
  }

  public void test_beanBuilder_noHolidays() {
    ImmutableSortedSet<LocalDate> holidays = ImmutableSortedSet.of();
    ImmutableSortedSet<DayOfWeek> weekendDays = ImmutableSortedSet.of(SATURDAY, SUNDAY);
    BusinessDayCalendar test = BusinessDayCalendar.meta().builder()
        .set(BusinessDayCalendar.meta().holidays(), holidays)
        .set(BusinessDayCalendar.meta().weekendDays(), weekendDays)
        .set(BusinessDayCalendar.meta().range(), LocalDateRange.ALL)
        .build();
    assertEquals(test.getHolidays(), holidays);
    assertEquals(test.getWeekendDays(), weekendDays);
    assertEquals(test.getRange(), LocalDateRange.ALL);
  }

  public void test_beanBuilder_invalidRangeNotAllWhenNoHolidays() {
    ImmutableSortedSet<LocalDate> holidays = ImmutableSortedSet.of();
    ImmutableSortedSet<DayOfWeek> weekendDays = ImmutableSortedSet.of(SATURDAY, SUNDAY);
    BeanBuilder<? extends BusinessDayCalendar> builder = BusinessDayCalendar.meta().builder()
        .set(BusinessDayCalendar.meta().holidays(), holidays)
        .set(BusinessDayCalendar.meta().weekendDays(), weekendDays)
        .set(BusinessDayCalendar.meta().range(), RANGE_2014);
    assertThrows(() -> builder.build(), IllegalArgumentException.class);
  }

  public void test_beanBuilder_invalidRangeHolidayBeforeRangeStart() {
    ImmutableSortedSet<LocalDate> holidays = ImmutableSortedSet.of(MON_2014_07_14, FRI_2014_07_18);
    ImmutableSortedSet<DayOfWeek> weekendDays = ImmutableSortedSet.of(SATURDAY, SUNDAY);
    BeanBuilder<? extends BusinessDayCalendar> builder = BusinessDayCalendar.meta().builder()
        .set(BusinessDayCalendar.meta().holidays(), holidays)
        .set(BusinessDayCalendar.meta().weekendDays(), weekendDays)
        .set(BusinessDayCalendar.meta().range(), LocalDateRange.closed(TUE_2014_07_15, LocalDate.MAX));
    assertThrows(() -> builder.build(), IllegalArgumentException.class);
  }

  public void test_beanBuilder_invalidRangeHolidayAfterRangeEnd() {
    ImmutableSortedSet<LocalDate> holidays = ImmutableSortedSet.of(MON_2014_07_14, FRI_2014_07_18);
    ImmutableSortedSet<DayOfWeek> weekendDays = ImmutableSortedSet.of(SATURDAY, SUNDAY);
    BeanBuilder<? extends BusinessDayCalendar> builder = BusinessDayCalendar.meta().builder()
        .set(BusinessDayCalendar.meta().holidays(), holidays)
        .set(BusinessDayCalendar.meta().weekendDays(), weekendDays)
        .set(BusinessDayCalendar.meta().range(), LocalDateRange.closed(LocalDate.MIN, TUE_2014_07_15));
    assertThrows(() -> builder.build(), IllegalArgumentException.class);
  }

  public void test_beanBuilder_invalidRangeHolidayBeforeAndAfterRange() {
    ImmutableSortedSet<LocalDate> holidays = ImmutableSortedSet.of(MON_2014_07_14, FRI_2014_07_18);
    ImmutableSortedSet<DayOfWeek> weekendDays = ImmutableSortedSet.of(SATURDAY, SUNDAY);
    BeanBuilder<? extends BusinessDayCalendar> builder = BusinessDayCalendar.meta().builder()
        .set(BusinessDayCalendar.meta().holidays(), holidays)
        .set(BusinessDayCalendar.meta().weekendDays(), weekendDays)
        .set(BusinessDayCalendar.meta().range(), LocalDateRange.closed(TUE_2014_07_15, THU_2014_07_17));
    assertThrows(() -> builder.build(), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_isBusinessDay_outOfRange() {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, TUE_2014_07_15);
    BusinessDayCalendar test = BusinessDayCalendar.of(holidays, SATURDAY, SUNDAY);
    assertThrows(() -> test.isBusinessDay(LocalDate.of(2013, 12, 31)), IllegalArgumentException.class);
    assertThrows(() -> test.isBusinessDay(LocalDate.of(2015, 1, 1)), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "shift")
  Object[][] data_shift() {
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
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, WED_2014_07_16);
    BusinessDayCalendar test = BusinessDayCalendar.of(holidays, SATURDAY, SUNDAY);
    assertEquals(test.shift(date, amount), expected);
  }

  public void test_shift_null() {
    assertThrows(() -> BusinessDayCalendar.WEEKENDS.shift(null, 1), IllegalArgumentException.class);
  }

  @Test(dataProvider = "shift")
  public void test_adjustBy(LocalDate date, int amount, LocalDate expected) {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, WED_2014_07_16);
    BusinessDayCalendar test = BusinessDayCalendar.of(holidays, SATURDAY, SUNDAY);
    assertEquals(date.with(test.adjustBy(amount)), expected);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "next")
  Object[][] data_next() {
      return new Object[][] {
          {THU_2014_07_10, FRI_2014_07_11},
          {FRI_2014_07_11, TUE_2014_07_15},
          {SAT_2014_07_12, TUE_2014_07_15},
          {SUN_2014_07_13, TUE_2014_07_15},
          {MON_2014_07_14, TUE_2014_07_15},
          {TUE_2014_07_15, THU_2014_07_17},
          {WED_2014_07_16, THU_2014_07_17},
          {THU_2014_07_17, FRI_2014_07_18},
          {FRI_2014_07_18, MON_2014_07_21},
          {SAT_2014_07_19, MON_2014_07_21},
          {SUN_2014_07_20, MON_2014_07_21},
          {MON_2014_07_21, TUE_2014_07_22},
      };
  }

  @Test(dataProvider = "next")
  public void test_next(LocalDate date, LocalDate expectedNext) {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, WED_2014_07_16);
    BusinessDayCalendar test = BusinessDayCalendar.of(holidays, SATURDAY, SUNDAY);
    assertEquals(test.next(date), expectedNext);
  }

  public void test_next_null() {
    assertThrows(() -> BusinessDayCalendar.WEEKENDS.next(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "previous")
  Object[][] data_previous() {
      return new Object[][] {
          {FRI_2014_07_11, THU_2014_07_10},
          {SAT_2014_07_12, FRI_2014_07_11},
          {SUN_2014_07_13, FRI_2014_07_11},
          {MON_2014_07_14, FRI_2014_07_11},
          {TUE_2014_07_15, FRI_2014_07_11},
          {WED_2014_07_16, TUE_2014_07_15},
          {THU_2014_07_17, TUE_2014_07_15},
          {FRI_2014_07_18, THU_2014_07_17},
          {SAT_2014_07_19, FRI_2014_07_18},
          {SUN_2014_07_20, FRI_2014_07_18},
          {MON_2014_07_21, FRI_2014_07_18},
          {TUE_2014_07_22, MON_2014_07_21},
      };
  }

  @Test(dataProvider = "previous")
  public void test_previous(LocalDate date, LocalDate expectedPrevious) {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, WED_2014_07_16);
    BusinessDayCalendar test = BusinessDayCalendar.of(holidays, SATURDAY, SUNDAY);
    assertEquals(test.previous(date), expectedPrevious);
  }

  public void test_previous_null() {
    assertThrows(() -> BusinessDayCalendar.WEEKENDS.previous(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "ensure")
  Object[][] data_ensure() {
      return new Object[][] {
          {FRI_2014_07_11, FRI_2014_07_11},
          {SAT_2014_07_12, TUE_2014_07_15},
          {SUN_2014_07_13, TUE_2014_07_15},
          {MON_2014_07_14, TUE_2014_07_15},
          {TUE_2014_07_15, TUE_2014_07_15},
          {WED_2014_07_16, THU_2014_07_17},
          {THU_2014_07_17, THU_2014_07_17},
          {FRI_2014_07_18, FRI_2014_07_18},
          {SAT_2014_07_19, MON_2014_07_21},
          {SUN_2014_07_20, MON_2014_07_21},
          {MON_2014_07_21, MON_2014_07_21},
          {TUE_2014_07_22, TUE_2014_07_22},
      };
  }

  @Test(dataProvider = "ensure")
  public void test_ensure(LocalDate date, LocalDate expected) {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, WED_2014_07_16);
    BusinessDayCalendar test = BusinessDayCalendar.of(holidays, SATURDAY, SUNDAY);
    assertEquals(test.ensure(date, BusinessDayConvention.FOLLOWING), expected);
  }

  public void test_ensure_null() {
    BusinessDayCalendar test = BusinessDayCalendar.WEEKENDS;
    assertThrows(() -> test.ensure(null, BusinessDayConvention.NO_ADJUST), IllegalArgumentException.class);
    assertThrows(() -> test.ensure(THU_2014_07_10, null), IllegalArgumentException.class);
    assertThrows(() -> test.ensure(null, null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "daysBetween")
  Object[][] data_daysBetween() {
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
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, WED_2014_07_16);
    BusinessDayCalendar test = BusinessDayCalendar.of(holidays, SATURDAY, SUNDAY);
    assertEquals(test.daysBetween(start, end), expected);
  }

  @Test(dataProvider = "daysBetween")
  public void test_daysBetween_LocalDateRange(LocalDate start, LocalDate end, int expected) {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, WED_2014_07_16);
    BusinessDayCalendar test = BusinessDayCalendar.of(holidays, SATURDAY, SUNDAY);
    if (start.equals(end) == false) {
      // TODO: range allow empty?
      assertEquals(test.daysBetween(LocalDateRange.halfOpen(start, end)), expected);
    }
  }

  public void test_daysBetween_null() {
    BusinessDayCalendar test = BusinessDayCalendar.WEEKENDS;
    assertThrows(() -> test.daysBetween(null, WED_2014_07_16), IllegalArgumentException.class);
    assertThrows(() -> test.daysBetween(WED_2014_07_16, null), IllegalArgumentException.class);
    assertThrows(() -> test.daysBetween(null, null), IllegalArgumentException.class);
    assertThrows(() -> test.daysBetween(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_combineWith() {
    Iterable<LocalDate> holidays1 = Arrays.asList(WED_2014_07_16);
    BusinessDayCalendar base1 = BusinessDayCalendar.of(holidays1, SATURDAY, SUNDAY);
    Iterable<LocalDate> holidays2 = Arrays.asList(MON_2014_07_14);
    BusinessDayCalendar base2 = BusinessDayCalendar.of(holidays2, FRIDAY, SATURDAY);
    BusinessDayCalendar test = base1.combineWith(base2);
    assertEquals(test.getHolidays(), ImmutableSortedSet.of(MON_2014_07_14, WED_2014_07_16));
    assertEquals(test.getWeekendDays(), ImmutableSet.of(FRIDAY, SATURDAY, SUNDAY));
    assertEquals(test.getRange(), RANGE_2014);
  }

  public void test_combineWith_smae() {
    Iterable<LocalDate> holidays1 = Arrays.asList(WED_2014_07_16);
    BusinessDayCalendar base1 = BusinessDayCalendar.of(holidays1, SATURDAY, SUNDAY);
    Iterable<LocalDate> holidays2 = Arrays.asList(WED_2014_07_16);
    BusinessDayCalendar base2 = BusinessDayCalendar.of(holidays2, SATURDAY, SUNDAY);
    BusinessDayCalendar test = base1.combineWith(base2);
    assertEquals(test.getHolidays(), ImmutableSortedSet.of(WED_2014_07_16));
    assertEquals(test.getWeekendDays(), ImmutableSet.of(SATURDAY, SUNDAY));
    assertEquals(test.getRange(), RANGE_2014);
  }

  public void test_combineWith_null() {
    assertThrows(() -> BusinessDayCalendar.WEEKENDS.combineWith(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    Iterable<LocalDate> holidays = Arrays.asList(MON_2014_07_14, TUE_2014_07_15);
    coverImmutableBean(BusinessDayCalendar.of(holidays, SATURDAY, SUNDAY));
  }

}
