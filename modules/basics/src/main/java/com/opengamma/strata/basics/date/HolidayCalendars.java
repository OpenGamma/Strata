/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import java.io.Serializable;
import java.time.LocalDate;

import com.google.common.base.Splitter;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Constants and implementations for standard holiday calendars.
 * <p>
 * The purpose of each holiday calendar is to define whether a date is a holiday or a business day.
 * The standard holiday calendar data is provided by direct research and is not derived
 * from a vendor of holiday calendar data. The implementation is defined by {@code HolidayCalendar.ini},
 * The data may or may not be sufficient for your production needs.
 * <p>
 * Applications should refer to holidays using {@link HolidayCalendarId}.
 * The identifier must be {@linkplain HolidayCalendarId#resolve(ReferenceData) resolved}
 * to a {@link HolidayCalendar} before holidays can be accessed.
 */
public final class HolidayCalendars {
  // constants are indirected via ENUM_LOOKUP to allow them to be replaced by config

  /**
   * The extended enum lookup from name to instance.
   */
  static final ExtendedEnum<HolidayCalendar> ENUM_LOOKUP = ExtendedEnum.of(HolidayCalendar.class);

  /**
   * An instance declaring no holidays and no weekends.
   * <p>
   * This calendar has the effect of making every day a business day.
   * It is often used to indicate that a holiday calendar does not apply.
   */
  public static final HolidayCalendar NO_HOLIDAYS = HolidayCalendar.of(StandardHolidayCalendars.NO_HOLIDAYS.getName());
  /**
   * An instance declaring all days as business days except Saturday/Sunday weekends.
   * <p>
   * This calendar is mostly useful in testing scenarios.
   * Note that not all countries use Saturday and Sunday weekends.
   */
  public static final HolidayCalendar SAT_SUN = HolidayCalendar.of(StandardHolidayCalendars.SAT_SUN.getName());
  /**
   * An instance declaring all days as business days except Friday/Saturday weekends.
   * <p>
   * This calendar is mostly useful in testing scenarios.
   */
  public static final HolidayCalendar FRI_SAT = HolidayCalendar.of(StandardHolidayCalendars.FRI_SAT.getName());
  /**
   * An instance declaring all days as business days except Thursday/Friday weekends.
   * <p>
   * This calendar is mostly useful in testing scenarios.
   */
  public static final HolidayCalendar THU_FRI = HolidayCalendar.of(StandardHolidayCalendars.THU_FRI.getName());

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the specified unique name.
   * 
   * @param uniqueName  the unique name of the calendar
   * @return the holiday calendar
   */
  static HolidayCalendar of(String uniqueName) {
    ArgChecker.notNull(uniqueName, "uniqueName");
    if (uniqueName.contains("+")) {
      return Splitter.on('+').splitToList(uniqueName).stream()
          .map(HolidayCalendars::of)
          .reduce(NO_HOLIDAYS, HolidayCalendar::combinedWith);
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
    private final HolidayCalendarId id;

    private Object readResolve() {
      return new Combined(calendar1, calendar2);
    }

    // create
    Combined(HolidayCalendar calendar1, HolidayCalendar calendar2) {
      this.calendar1 = ArgChecker.notNull(calendar1, "calendar1");
      this.calendar2 = ArgChecker.notNull(calendar2, "calendar2");
      this.id = calendar1.getId().combinedWith(calendar2.getId());
    }

    @Override
    public boolean isHoliday(LocalDate date) {
      return calendar1.isHoliday(date) || calendar2.isHoliday(date);
    }

    @Override
    public HolidayCalendarId getId() {
      return id;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Combined) {
        return ((Combined) obj).id.equals(id);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return id.hashCode();
    }

    @Override
    public String toString() {
      return getName();
    }
  }

}
