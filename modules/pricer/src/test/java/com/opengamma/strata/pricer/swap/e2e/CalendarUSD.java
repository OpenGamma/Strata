/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.swap.e2e;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.ImmutableHolidayCalendar;

/**
 * Dummy calendar for end-to-end tests.
 * This exists primarily to match numbers against older ones.
 */
public class CalendarUSD {

  public static final HolidayCalendarId NYC = HolidayCalendarId.of("TestNYC");
  public static final HolidayCalendar NYC_CALENDAR;
  static {
    List<LocalDate> holidays = new ArrayList<>();
    int startYear = 2013;
    int endYear = 2063;
    for (int i = startYear; i <= endYear; i++) {
      holidays.add(LocalDate.of(i, 1, 1));
      holidays.add(LocalDate.of(i, 7, 4));
      holidays.add(LocalDate.of(i, 11, 11));
      holidays.add(LocalDate.of(i, 12, 25));
    }
    holidays.add(LocalDate.of(2014, 1, 20));
    holidays.add(LocalDate.of(2014, 2, 17));
    holidays.add(LocalDate.of(2014, 5, 26));
    holidays.add(LocalDate.of(2014, 9, 1));
    holidays.add(LocalDate.of(2014, 10, 13));
    holidays.add(LocalDate.of(2014, 11, 27));
    holidays.add(LocalDate.of(2015, 1, 19));
    holidays.add(LocalDate.of(2015, 2, 16));
    holidays.add(LocalDate.of(2015, 5, 25));
    holidays.add(LocalDate.of(2015, 9, 7));
    holidays.add(LocalDate.of(2015, 10, 12));
    holidays.add(LocalDate.of(2015, 11, 26));
    holidays.add(LocalDate.of(2016, 1, 18));
    holidays.add(LocalDate.of(2016, 2, 15));
    holidays.add(LocalDate.of(2016, 5, 30));
    holidays.add(LocalDate.of(2016, 9, 5));
    holidays.add(LocalDate.of(2016, 10, 10));
    holidays.add(LocalDate.of(2016, 11, 24));
    holidays.add(LocalDate.of(2016, 12, 26));
    holidays.add(LocalDate.of(2017, 1, 2));
    holidays.add(LocalDate.of(2017, 1, 16));
    holidays.add(LocalDate.of(2017, 2, 20));
    holidays.add(LocalDate.of(2017, 5, 29));
    holidays.add(LocalDate.of(2017, 9, 4));
    holidays.add(LocalDate.of(2017, 10, 9));
    holidays.add(LocalDate.of(2017, 11, 23));
    NYC_CALENDAR = ImmutableHolidayCalendar.of(NYC, holidays, SATURDAY, SUNDAY);
  }

}
