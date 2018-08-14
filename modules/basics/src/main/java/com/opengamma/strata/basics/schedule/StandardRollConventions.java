/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.HolidayCalendarIds;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.collect.ArgChecker;

/**
 * Standard roll convention implementations.
 * <p>
 * See {@link RollConventions} for the description of each.
 */
enum StandardRollConventions implements RollConvention {

  // no adjustment
  NONE("None") {
    @Override
    public LocalDate adjust(LocalDate date) {
      return ArgChecker.notNull(date, "date");
    }
  },

  // last day of month
  EOM("EOM") {
    @Override
    public LocalDate adjust(LocalDate date) {
      ArgChecker.notNull(date, "date");
      return date.withDayOfMonth(date.lengthOfMonth());
    }

    @Override
    public int getDayOfMonth() {
      // EOM is equivalent to 31 in FpML in most cases
      // because roll conventions 30 and 29 also have to adjust to end of February
      return 31;
    }
  },

  // 3rd Wednesday
  IMM("IMM") {
    @Override
    public LocalDate adjust(LocalDate date) {
      ArgChecker.notNull(date, "date");
      return date.with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.WEDNESDAY));
    }
  },

  // 2nd London banking day before 3rd Wednesday, adjusted by Montreal & Toronto holiday
  IMMCAD("IMMCAD") {
    private final HolidayCalendar gblo = holidayCalendar(HolidayCalendarIds.GBLO);
    private final HolidayCalendar canada =
        holidayCalendar(HolidayCalendarIds.CATO).combinedWith(holidayCalendar(HolidayCalendarIds.CAMO));

    @Override
    public LocalDate adjust(LocalDate date) {
      ArgChecker.notNull(date, "date");
      LocalDate wed3 = date.with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.WEDNESDAY));
      return canada.previousOrSame(gblo.shift(wed3, -2));
    }
  },

  // 1 Sydney business day before 2nd Friday
  IMMAUD("IMMAUD") {
    private final HolidayCalendar ausy = holidayCalendar(HolidayCalendarIds.AUSY);

    @Override
    public LocalDate adjust(LocalDate date) {
      ArgChecker.notNull(date, "date");
      return ausy.previous(date.with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.FRIDAY)));
    }
  },

  // Wednesday on or after 9th
  IMMNZD("IMMNZD") {
    @Override
    public LocalDate adjust(LocalDate date) {
      ArgChecker.notNull(date, "date");
      return date.withDayOfMonth(9).with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));
    }
  },

  // 2nd Friday
  SFE("SFE") {
    @Override
    public LocalDate adjust(LocalDate date) {
      ArgChecker.notNull(date, "date");
      return date.with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.FRIDAY));
    }
  },

  // Each Monday, or Tuesday if USNY holiday
  TBILL("TBILL") {
    private final HolidayCalendar usny = holidayCalendar(HolidayCalendarIds.USNY);

    @Override
    public LocalDate adjust(LocalDate date) {
      ArgChecker.notNull(date, "date");
      return usny.nextOrSame(date.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)));
    }
  };

  // name
  private final String name;

  // create
  private StandardRollConventions(String name) {
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

  // safely find a holiday calendar
  // this exists because using a RollConvention should not result in a reference data exception
  private static HolidayCalendar holidayCalendar(HolidayCalendarId id) {
    return ReferenceData.standard().findValue(id).orElse(HolidayCalendars.SAT_SUN);
  }

}
