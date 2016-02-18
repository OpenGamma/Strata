/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.LocalDateUtils.plusDays;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;

import java.time.DayOfWeek;
import java.time.LocalDate;

import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.range.LocalDateRange;

/**
 * Standard holiday calendar implementations.
 * <p>
 * See {@link HolidayCalendars} for the description of each.
 */
enum StandardHolidayCalendars implements HolidayCalendar {

  // no holidays
  NO_HOLIDAYS(HolidayCalendarIds.NO_HOLIDAYS) {
    @Override
    public boolean isHoliday(LocalDate date) {
      ArgChecker.notNull(date, "date");
      return false;
    }

    @Override
    public boolean isBusinessDay(LocalDate date) {
      ArgChecker.notNull(date, "date");
      return true;
    }

    @Override
    public LocalDate shift(LocalDate date, int amount) {
      ArgChecker.notNull(date, "date");
      return plusDays(date, amount);
    }

    @Override
    public LocalDate next(LocalDate date) {
      ArgChecker.notNull(date, "date");
      return plusDays(date, 1);
    }

    @Override
    public LocalDate previous(LocalDate date) {
      ArgChecker.notNull(date, "date");
      return plusDays(date, -1);
    }

    @Override
    public int daysBetween(LocalDate startInclusive, LocalDate endExclusive) {
      return Math.toIntExact(LocalDateUtils.daysBetween(startInclusive, endExclusive));
    }

    @Override
    public int daysBetween(LocalDateRange dateRange) {
      return daysBetween(dateRange.getStart(), dateRange.getEndExclusive());
    }

    @Override
    public HolidayCalendar combinedWith(HolidayCalendar other) {
      return ArgChecker.notNull(other, "other");
    }
  },

  // Saturday and Sunday only
  SAT_SUN(HolidayCalendarIds.SAT_SUN) {
    @Override
    public boolean isHoliday(LocalDate date) {
      ArgChecker.notNull(date, "date");
      DayOfWeek dow = date.getDayOfWeek();
      return dow == SATURDAY || dow == SUNDAY;
    }
  },

  // Friday and Saturday only
  FRI_SAT(HolidayCalendarIds.FRI_SAT) {
    @Override
    public boolean isHoliday(LocalDate date) {
      ArgChecker.notNull(date, "date");
      DayOfWeek dow = date.getDayOfWeek();
      return dow == FRIDAY || dow == SATURDAY;
    }
  },

  // Thursday and Friday only
  THU_FRI(HolidayCalendarIds.THU_FRI) {
    @Override
    public boolean isHoliday(LocalDate date) {
      ArgChecker.notNull(date, "date");
      DayOfWeek dow = date.getDayOfWeek();
      return dow == THURSDAY || dow == FRIDAY;
    }
  };

  // name
  private final HolidayCalendarId id;

  // create
  private StandardHolidayCalendars(HolidayCalendarId id) {
    this.id = id;
  }

  @Override
  public LocalDate shift(LocalDate date, int amount) {
    // optimize because we know there are 5 business days in a week
    // method implemented here as cannot reach default method from enum subclass
    ArgChecker.notNull(date, "date");
    LocalDate weekAdjusted = date.plusWeeks(amount / 5);
    return HolidayCalendar.super.shift(weekAdjusted, amount % 5);
  }

  @Override
  public HolidayCalendarId getId() {
    return id;
  }

  @Override
  public String toString() {
    return getName();
  }

}
