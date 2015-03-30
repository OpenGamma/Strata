/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import java.time.LocalDate;
import java.time.Period;

import com.opengamma.strata.collect.ArgChecker;

/**
 * Standard period addition implementations.
 * <p>
 * See {@link PeriodAdditionConventions} for the description of each.
 */
enum StandardPeriodAdditionConventions implements PeriodAdditionConvention {

  // no specific addition rule
  NONE("None") {
    @Override
    public LocalDate adjust(LocalDate baseDate, Period period, HolidayCalendar calendar) {
      ArgChecker.notNull(baseDate, "baseDate");
      ArgChecker.notNull(period, "period");
      ArgChecker.notNull(calendar, "calendar");
      return baseDate.plus(period);
    }

    @Override
    public boolean isMonthBased() {
      return false;
    }
  },

  // last day of month
  LAST_DAY("LastDay") {
    @Override
    public LocalDate adjust(LocalDate baseDate, Period period, HolidayCalendar calendar) {
      ArgChecker.notNull(baseDate, "baseDate");
      ArgChecker.notNull(period, "period");
      ArgChecker.notNull(calendar, "calendar");
      LocalDate endDate = baseDate.plus(period);
      if (baseDate.getDayOfMonth() == baseDate.lengthOfMonth()) {
        return endDate.withDayOfMonth(endDate.lengthOfMonth());
      }
      return endDate;
    }

    @Override
    public boolean isMonthBased() {
      return true;
    }
  },

  // last business day of month
  LAST_BUSINESS_DAY("LastBusinessDay") {
    @Override
    public LocalDate adjust(LocalDate baseDate, Period period, HolidayCalendar calendar) {
      ArgChecker.notNull(baseDate, "baseDate");
      ArgChecker.notNull(period, "period");
      ArgChecker.notNull(calendar, "calendar");
      LocalDate endDate = baseDate.plus(period);
      if (calendar.isLastBusinessDayOfMonth(baseDate)) {
        return calendar.lastBusinessDayOfMonth(endDate);
      }
      return endDate;
    }

    @Override
    public boolean isMonthBased() {
      return true;
    }
  };

  // name
  private final String name;

  // create
  private StandardPeriodAdditionConventions(String name) {
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
