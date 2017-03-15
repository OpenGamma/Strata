/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SUNDAY;

import java.time.LocalDate;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Standard business day convention implementations.
 * <p>
 * See {@link BusinessDayConventions} for the description of each.
 */
enum StandardBusinessDayConventions implements BusinessDayConvention {

  // make no adjustment
  NO_ADJUST("NoAdjust") {
    @Override
    public LocalDate adjust(LocalDate date, HolidayCalendar calendar) {
      return ArgChecker.notNull(date, "date");
    }
  },

  // next business day
  FOLLOWING("Following") {
    @Override
    public LocalDate adjust(LocalDate date, HolidayCalendar calendar) {
      return calendar.nextOrSame(date);
    }
  },

  // next business day unless over a month end
  MODIFIED_FOLLOWING("ModifiedFollowing") {
    @Override
    public LocalDate adjust(LocalDate date, HolidayCalendar calendar) {
      return calendar.nextSameOrLastInMonth(date);
    }
  },

  // next business day unless over a month end or mid
  MODIFIED_FOLLOWING_BI_MONTHLY("ModifiedFollowingBiMonthly") {
    @Override
    public LocalDate adjust(LocalDate date, HolidayCalendar calendar) {
      LocalDate adjusted = calendar.nextOrSame(date);
      if (adjusted.getMonthValue() != date.getMonthValue() ||
          (adjusted.getDayOfMonth() > 15 && date.getDayOfMonth() <= 15)) {
        adjusted = calendar.previous(date);
      }
      return adjusted;
    }
  },

  // previous business day
  PRECEDING("Preceding") {
    @Override
    public LocalDate adjust(LocalDate date, HolidayCalendar calendar) {
      return calendar.previousOrSame(date);
    }
  },

  // previous business day unless over a month end
  MODIFIED_PRECEDING("ModifiedPreceding") {
    @Override
    public LocalDate adjust(LocalDate date, HolidayCalendar calendar) {
      LocalDate adjusted = calendar.previousOrSame(date);
      if (adjusted.getMonth() != date.getMonth()) {
        adjusted = calendar.next(date);
      }
      return adjusted;
    }
  },

  // next business day if Sun/Mon, otherwise previous
  NEAREST("Nearest") {
    @Override
    public LocalDate adjust(LocalDate date, HolidayCalendar calendar) {
      if (calendar.isBusinessDay(date)) {
        return date;
      }
      if (date.getDayOfWeek() == SUNDAY || date.getDayOfWeek() == MONDAY) {
        return calendar.next(date);
      } else {
        return calendar.previous(date);
      }
    }
  };

  // name
  private final String name;

  // create
  private StandardBusinessDayConventions(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

}
