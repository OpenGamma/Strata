/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.temporal.TemporalAdjusters.firstInMonth;
import static java.time.temporal.TemporalAdjusters.lastInMonth;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of some common global holiday calendars.
 * <p>
 * The data provided here has been identified through direct research and is not
 * derived from a vendor of holiday calendar data.
 * This data may or may not be sufficient for your production needs.
 */
public final class GlobalHolidayCalendars {

  /**
   * The holiday calendar for London, United Kingdom, with code 'GBLO'.
   * <p>
   * This constant provides the calendar for London bank holidays.
   * The default implementation is based on original research and covers 1950 to 2099.
   * Future dates are an extrapolations of the latest known rules.
   */
  public static final HolidayCalendar GBLO = generateLondon();
  /**
   * The holiday calendar for the European Union TARGET system, with code 'EUTA'.
   * <p>
   * This constant provides the calendar for the TARGET interbank payment system holidays.
   * The default implementation is based on original research and covers 1997 to 2099.
   * Future dates are an extrapolations of the latest known rules.
   */
  public static final HolidayCalendar EUTA = generateEuropeanTarget();

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private GlobalHolidayCalendars() {
  }

  //-------------------------------------------------------------------------
  // generate GBLO
  // common law, good friday and christmas day
  // from 1871 easter monday, whit monday, first Mon in Aug and boxing day
  // from 1965 to 1970, first in Aug moved to Mon after last Sat in Aug
  // from 1971, whitsun moved to last Mon in May, last Mon in Aug
  // from 1974, added new year
  // from 1978, added first Mon in May
  // see Hansard for specific details
  // 1965, Whitsun, Last Mon Aug - http://hansard.millbanksystems.com/commons/1964/mar/04/staggered-holidays
  // 1966, Whitsun May - http://hansard.millbanksystems.com/commons/1964/mar/04/staggered-holidays
  // 1966, 29th Aug - http://hansard.millbanksystems.com/written_answers/1965/nov/25/august-bank-holiday
  // 1967, 29th May, 28th Aug - http://hansard.millbanksystems.com/written_answers/1965/jun/03/bank-holidays-1967-and-1968
  // 1968, 3rd Jun, 2nd Sep - http://hansard.millbanksystems.com/written_answers/1965/jun/03/bank-holidays-1967-and-1968
  // 1969, 26th May, 1st Sep - http://hansard.millbanksystems.com/written_answers/1967/mar/21/bank-holidays-1969-dates
  // 1970, 25th May, 31st Aug - http://hansard.millbanksystems.com/written_answers/1967/jul/28/bank-holidays
  static StandardHolidayCalendar generateLondon() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1950; year <= 2099; year++) {
      if (year < 1871) {
        holidays.add(easter(year).minusDays(2));
        holidays.add(date(year, 12, 25));
        continue;
      }
      // new year
      if (year >= 1974) {
        holidays.add(bumpToMon(first(year, 1)));
      }
      // easter
      holidays.add(easter(year).minusDays(2));
      holidays.add(easter(year).plusDays(1));
      // early May
      if (year == 1995) {
        // ve day
        holidays.add(date(1995, 5, 8));
      } else if (year >= 1978) {
        holidays.add(first(year, 5).with(firstInMonth(MONDAY)));
      }
      // spring
      if (year == 2002) {
        // golden jubilee
        holidays.add(date(2002, 6, 3));
        holidays.add(date(2002, 6, 4));
      } else if (year == 2012) {
        // diamond jubilee
        holidays.add(date(2012, 6, 4));
        holidays.add(date(2012, 6, 5));
      } else if (year == 1967 || year == 1970) {
        holidays.add(first(year, 5).with(lastInMonth(MONDAY)));
      } else if (year < 1971) {
        // whitsun
        holidays.add(easter(year).plusDays(50));
      } else {
        holidays.add(first(year, 5).with(lastInMonth(MONDAY)));
      }
      // summer
      if (year < 1965) {
        holidays.add(first(year, 8).with(firstInMonth(MONDAY)));
      } else if (year < 1971) {
        holidays.add(first(year, 8).with(lastInMonth(SATURDAY)).plusDays(2));
      } else {
        holidays.add(first(year, 8).with(lastInMonth(MONDAY)));
      }
      // christmas
      holidays.add(christmas(year));
      holidays.add(boxingDay(year));
      // royal wedding
      if (year == 2011) {
        holidays.add(date(2011, 4, 29));
      }
      // millenium
      if (year == 1999) {
        holidays.add(date(1999, 12, 31));
      }
    }
    return StandardHolidayCalendar.of("GBLO", holidays, SATURDAY, SUNDAY);
  }

  //-------------------------------------------------------------------------
  // generate EUTA
  // 1997 - 1998 (testing phase), Jan 1, christmas day
  // https://www.ecb.europa.eu/pub/pdf/other/tagien.pdf
  // in 1999, Jan 1, christmas day, Dec 26, Dec 31
  // http://www.ecb.europa.eu/press/pr/date/1999/html/pr990715_1.en.html
  // http://www.ecb.europa.eu/press/pr/date/1999/html/pr990331.en.html
  // in 2000, Jan 1, good friday, easter monday, May 1, christmas day, Dec 26
  // http://www.ecb.europa.eu/press/pr/date/1999/html/pr990715_1.en.html
  // in 2001, Jan 1, good friday, easter monday, May 1, christmas day, Dec 26, Dec 31
  // http://www.ecb.europa.eu/press/pr/date/2000/html/pr000525_2.en.html
  // from 2002, Jan 1, good friday, easter monday, May 1, christmas day, Dec 26
  // http://www.ecb.europa.eu/press/pr/date/2000/html/pr001214_4.en.html
  static StandardHolidayCalendar generateEuropeanTarget() {
    List<LocalDate> holidays = new ArrayList<>(2000);
    for (int year = 1997; year <= 2099; year++) {
      if (year >= 2000) {
        holidays.add(date(year, 1, 1));
        holidays.add(easter(year).minusDays(2));
        holidays.add(easter(year).plusDays(1));
        holidays.add(date(year, 5, 1));
        holidays.add(date(year, 12, 25));
        holidays.add(date(year, 12, 26));
      } else {  // 1997 to 1999
        holidays.add(date(year, 1, 1));
        holidays.add(date(year, 12, 25));
      }
      if (year == 1999 || year == 2001) {
        holidays.add(date(year, 12, 31));
      }
    }
    return StandardHolidayCalendar.of("EUTA", holidays, SATURDAY, SUNDAY);
  }

  //-------------------------------------------------------------------------
  // date
  private static LocalDate date(int year, int month, int day) {
    return LocalDate.of(year, month, day);
  }

  // bump to following Monday
  private static LocalDate bumpToMon(LocalDate date) {
    if (date.getDayOfWeek() == SATURDAY) {
      return date.plusDays(2);
    } else if (date.getDayOfWeek() == SUNDAY) {
      return date.plusDays(1);
    }
    return date;
  }

  // christmas
  private static LocalDate christmas(int year) {
    LocalDate base = LocalDate.of(year, 12, 25);
    if (base.getDayOfWeek() == SATURDAY || base.getDayOfWeek() == SUNDAY) {
      return LocalDate.of(year, 12, 27);
    }
    return base;
  }

  // boxing day
  private static LocalDate boxingDay(int year) {
    LocalDate base = LocalDate.of(year, 12, 26);
    if (base.getDayOfWeek() == SATURDAY || base.getDayOfWeek() == SUNDAY) {
      return LocalDate.of(year, 12, 28);
    }
    return base;
  }

  // first of a month
  private static LocalDate first(int year, int month) {
    return LocalDate.of(year, month, 1);
  }

  // calculate easter day by Delambre
  static LocalDate easter(int year) {
    int a = year % 19;
    int b = year / 100;
    int c = year % 100;
    int d = b / 4;
    int e = b % 4;
    int f = (b + 8) / 25;
    int g = (b - f + 1) / 3;
    int h = (19 * a + b - d - g + 15) % 30;
    int i = c / 4;
    int k = c % 4;
    int l = (32 + 2 * e + 2 * i - h - k) % 7;
    int m = (a + 11 * h+ 22 * l) / 451;
    int month =(h + l - 7 * m + 114) / 31;
    int day = ((h + l - 7 * m + 114) % 31) + 1;
    return LocalDate.of(year, month, day);
  }

}
