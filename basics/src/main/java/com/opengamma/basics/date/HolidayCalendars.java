/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;

import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;

import com.google.common.base.Splitter;
import com.opengamma.collect.ArgChecker;
import com.opengamma.collect.named.ExtendedEnum;
import com.opengamma.collect.range.LocalDateRange;

/**
 * Constants and implementations for standard holiday calendars.
 * <p>
 * The purpose of each holiday calendar is to define whether a date is a holiday or a business day.
 * The is of use in many calculations.
 */
public final class HolidayCalendars {

  /**
   * An instance declaring no holidays and no weekends.
   * <p>
   * This calendar has the effect of making every day a business day.
   * It is often used to indicate that a holiday calendar does not apply.
   */
  public static final HolidayCalendar NONE = Standard.NONE;
  /**
   * An instance declaring all days as business days except Saturday/Sunday weekends.
   * <p>
   * This calendar is mostly useful in testing scenarios.
   * Note that not all countries use Saturday and Sunday weekends.
   */
  public static final HolidayCalendar SAT_SUN = Standard.SAT_SUN;
  /**
   * An instance declaring all days as business days except Friday/Saturday weekends.
   * <p>
   * This calendar is mostly useful in testing scenarios.
   */
  public static final HolidayCalendar FRI_SAT = Standard.FRI_SAT;
  /**
   * An instance declaring all days as business days except Thursday/Friday weekends.
   * <p>
   * This calendar is mostly useful in testing scenarios.
   */
  public static final HolidayCalendar THU_FRI = Standard.THU_FRI;

  /**
   * The extended enum lookup from name to instance.
   */
  private static final ExtendedEnum<HolidayCalendar> ENUM_LOOKUP = ExtendedEnum.of(HolidayCalendar.class);

  //-------------------------------------------------------------------------
  /**
   * Obtains a {@code HolidayCalendar} from a unique name.
   * 
   * @param uniqueName  the unique name of the calendar
   * @return the holiday calendar
   */
  static HolidayCalendar of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    if (uniqueName.contains("+")) {
      return Splitter.on('+').splitToList(uniqueName).stream()
          .map(HolidayCalendars::of)
          .reduce(NONE, HolidayCalendar::combineWith);
    }
    return ENUM_LOOKUP.lookup(uniqueName);
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private HolidayCalendars() {
  }

  //-------------------------------------------------------------------------
  /**
   * Standard holiday calendars.
   */
  static enum Standard implements HolidayCalendar {

    // no holidays
    NONE("None") {
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
        return date.plusDays(amount);
      }
      @Override
      public LocalDate next(LocalDate date) {
        ArgChecker.notNull(date, "date");
        return date.plusDays(1);
      }
      @Override
      public LocalDate previous(LocalDate date) {
        ArgChecker.notNull(date, "date");
        return date.minusDays(1);
      }
      @Override
      public int daysBetween(LocalDate startInclusive, LocalDate endExclusive) {
        return Math.toIntExact(endExclusive.toEpochDay() - startInclusive.toEpochDay());
      }
      @Override
      public int daysBetween(LocalDateRange dateRange) {
        return daysBetween(dateRange.getStart(), dateRange.getEndExclusive());
      }
      @Override
      public HolidayCalendar combineWith(HolidayCalendar other) {
        return ArgChecker.notNull(other, "other");
      }
    },
    // Saturday and Sunday only
    SAT_SUN("Sat/Sun") {
      @Override
      public boolean isHoliday(LocalDate date) {
        ArgChecker.notNull(date, "date");
        DayOfWeek dow = date.getDayOfWeek();
        return dow == SATURDAY || dow == SUNDAY;
      }
    },
    // Friday and Saturday only
    FRI_SAT("Fri/Sat") {
      @Override
      public boolean isHoliday(LocalDate date) {
        ArgChecker.notNull(date, "date");
        DayOfWeek dow = date.getDayOfWeek();
        return dow == FRIDAY || dow == SATURDAY;
      }
    },
    // Thursday and Friday only
    THU_FRI("Thu/Fri") {
      @Override
      public boolean isHoliday(LocalDate date) {
        ArgChecker.notNull(date, "date");
        DayOfWeek dow = date.getDayOfWeek();
        return dow == THURSDAY || dow == FRIDAY;
      }
    };

    // name
    private final String name;

    // create
    private Standard(String name) {
      this.name = name;
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
    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return name;
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Implementation of the combined holiday calendar.
   */
  static final class Combined implements HolidayCalendar, Serializable {

    // Serialization version
    private static final long serialVersionUID = 1L;

    // calendar 1
    private final HolidayCalendar calendar1;
    // calendar 2
    private final HolidayCalendar calendar2;
    // name
    private final String name;

    private Object readResolve() {
      return new Combined(calendar1, calendar2);
    }

    // create
    Combined(HolidayCalendar calendar1, HolidayCalendar calendar2) {
      this.calendar1 = ArgChecker.notNull(calendar1, "calendar1");
      this.calendar2 = ArgChecker.notNull(calendar2, "calendar2");
      this.name = calendar1.getName() + "+" + calendar2.getName();
    }

    @Override
    public boolean isHoliday(LocalDate date) {
      return calendar1.isHoliday(date) || calendar2.isHoliday(date);
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Combined) {
        return ((Combined) obj).name.equals(name);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    @Override
    public String toString() {
      return name;
    }
  }

}
