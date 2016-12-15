/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.opengamma.strata.collect.named.NamedLookup;

/**
 * Day count convention for 'Bus/252'.
 * <p>
 * This day count is based on a holiday calendar, which is stored in the day count
 * and referenced in the name.
 */
final class Business252DayCount implements NamedLookup<DayCount> {

  /**
   * The singleton instance of the lookup.
   */
  public static final Business252DayCount INSTANCE = new Business252DayCount();

  /**
   * The cache of day count by name.
   */
  private static final ConcurrentMap<String, DayCount> BY_NAME = new ConcurrentHashMap<>();
  /**
   * The cache of day count by calendar.
   */
  private static final ConcurrentMap<String, DayCount> BY_CALENDAR = new ConcurrentHashMap<>();

  /**
   * Restricted constructor.
   */
  private Business252DayCount() {
  }

  // obtains the day count
  DayCount of(HolidayCalendar calendar) {
    return BY_CALENDAR.computeIfAbsent(calendar.getName(), this::createByCalendarName);
  }

  private DayCount createByCalendarName(String calendarName) {
    return lookup("Bus/252 " + calendarName);
  }

  //-------------------------------------------------------------------------
  @Override
  public DayCount lookup(String name) {
    DayCount value = BY_NAME.get(name);
    if (value == null) {
      if (name.regionMatches(true, 0, "Bus/252 ", 0, 8)) {
        HolidayCalendar cal = HolidayCalendars.of(name.substring(8));  // load from standard calendars
        String correctName = "Bus/252 " + cal.getName();
        DayCount created = new Bus252(correctName, cal);
        value = BY_NAME.computeIfAbsent(correctName, k -> created);
        BY_NAME.putIfAbsent(correctName.toUpperCase(Locale.ENGLISH), created);
      }
    }
    return value;
  }

  @Override
  public Map<String, DayCount> lookupAll() {
    return BY_NAME;
  }

  //-------------------------------------------------------------------------
  /**
   * Implementation of the day-of-month roll convention.
   */
  static final class Bus252 implements DayCount, Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;
    private final transient HolidayCalendar calendar;

    Bus252(String name, HolidayCalendar calendar) {
      this.name = name;
      this.calendar = calendar;
    }

    // resolve instance
    private Object readResolve() {
      return DayCount.of(name);
    }

    @Override
    public double yearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
      if (secondDate.isBefore(firstDate)) {
        throw new IllegalArgumentException("Dates must be in time-line order");
      }
      return calendar.daysBetween(firstDate, secondDate) / 252d;
    }

    @Override
    public int days(LocalDate firstDate, LocalDate secondDate) {
      if (secondDate.isBefore(firstDate)) {
        throw new IllegalArgumentException("Dates must be in time-line order");
      }
      return calendar.daysBetween(firstDate, secondDate);
    }

    //-------------------------------------------------------------------------
    @Override
    public String getName() {
      return name;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Bus252) {
        return ((Bus252) obj).name.equals(name);
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
