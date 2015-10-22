/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import java.io.Serializable;
import java.time.LocalDate;

import com.google.common.base.Splitter;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Constants and implementations for standard holiday calendars.
 * <p>
 * The purpose of each holiday calendar is to define whether a date is a holiday or a business day.
 * The is of use in many calculations.
 * <p>
 * The holiday calendar data provided here has been identified through direct research and is not
 * derived from a vendor of holiday calendar data.
 * This data may or may not be sufficient for your production needs.
 * To change the implementation, see {@code HolidayCalendar.ini}.
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

  /**
   * The holiday calendar for London, United Kingdom, with code 'GBLO'.
   * <p>
   * This constant provides the calendar for London bank holidays.
   * <p>
   * The default implementation is based on original research and covers 1950 to 2099.
   * Future dates are an extrapolations of the latest known rules.
   * To change the implementation, see {@code HolidayCalendar.ini}.
   */
  public static final HolidayCalendar GBLO = HolidayCalendar.of(GlobalHolidayCalendars.GBLO.getName());
  /**
   * The holiday calendar for Paris, France, with code 'FRPA'.
   * <p>
   * This constant provides the calendar for Paris public holidays.
   * <p>
   * The default implementation is based on original research and covers 1950 to 2099.
   * Future and past dates are an extrapolations of the latest known rules.
   * To change the implementation, see {@code HolidayCalendar.ini}.
   */
  public static final HolidayCalendar FRPA = HolidayCalendar.of(GlobalHolidayCalendars.FRPA.getName());
  /**
   * The holiday calendar for Zurich, Switzerland, with code 'EUTA'.
   * <p>
   * This constant provides the calendar for Zurich public holidays.
   * <p>
   * The default implementation is based on original research and covers 1950 to 2099.
   * Future and past dates are an extrapolations of the latest known rules.
   * To change the implementation, see {@code HolidayCalendar.ini}.
   */
  public static final HolidayCalendar CHZU = HolidayCalendar.of(GlobalHolidayCalendars.CHZU.getName());
  /**
   * The holiday calendar for the European Union TARGET system, with code 'EUTA'.
   * <p>
   * This constant provides the calendar for the TARGET interbank payment system holidays.
   * <p>
   * The default implementation is based on original research and covers 1997 to 2099.
   * Future dates are an extrapolations of the latest known rules.
   * To change the implementation, see {@code HolidayCalendar.ini}.
   * <p>
   * Referenced by the 2006 ISDA definitions 1.8.
   */
  public static final HolidayCalendar EUTA = HolidayCalendar.of(GlobalHolidayCalendars.EUTA.getName());
  /**
   * The holiday calendar for United States Government Securities, with code 'USGS'.
   * <p>
   * This constant provides the calendar for United States Government Securities as per SIFMA.
   * <p>
   * The default implementation is based on original research and covers 1950 to 2099.
   * Future and past dates are an extrapolations of the latest known rules.
   * To change the implementation, see {@code HolidayCalendar.ini}.
   * <p>
   * Referenced by the 2006 ISDA definitions 1.11.
   */
  public static final HolidayCalendar USGS = HolidayCalendar.of(GlobalHolidayCalendars.USGS.getName());
  /**
   * The holiday calendar for New York, United States, with code 'USNY'.
   * <p>
   * This constant provides the calendar for New York holidays.
   * <p>
   * The default implementation is based on original research and covers 1950 to 2099.
   * Future and past dates are an extrapolations of the latest known rules.
   * To change the implementation, see {@code HolidayCalendar.ini}.
   */
  public static final HolidayCalendar USNY = HolidayCalendar.of(GlobalHolidayCalendars.USNY.getName());
  /**
   * The holiday calendar for the Federal Reserve Bank of New York, with code 'NYFD'.
   * <p>
   * This constant provides the calendar for the Federal Reserve Bank of New York holidays.
   * <p>
   * The default implementation is based on original research and covers 1950 to 2099.
   * Future and past dates are an extrapolations of the latest known rules.
   * To change the implementation, see {@code HolidayCalendar.ini}.
   * <p>
   * Referenced by the 2006 ISDA definitions 1.9.
   */
  public static final HolidayCalendar NYFD = HolidayCalendar.of(GlobalHolidayCalendars.NYFD.getName());
  /**
   * The holiday calendar for the New York Stock Exchange, with code 'NYSE'.
   * <p>
   * This constant provides the calendar for the New York Stock Exchange.
   * <p>
   * The default implementation is based on original research and covers 1950 to 2099.
   * Future and past dates are an extrapolations of the latest known rules.
   * To change the implementation, see {@code HolidayCalendar.ini}.
   * <p>
   * Referenced by the 2006 ISDA definitions 1.10.
   */
  public static final HolidayCalendar NYSE = HolidayCalendar.of(GlobalHolidayCalendars.NYSE.getName());
  /**
   * The holiday calendar for Tokyo, Japan, with code 'JPTO'.
   * <p>
   * This constant provides the calendar for Tokyo bank holidays.
   * <p>
   * The default implementation is based on original research and covers 1950 to 2099.
   * Future and past dates are an extrapolations of the latest known rules.
   * To change the implementation, see {@code HolidayCalendar.ini}.
   */
  public static final HolidayCalendar JPTO = HolidayCalendar.of(GlobalHolidayCalendars.JPTO.getName());

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
          .reduce(NO_HOLIDAYS, HolidayCalendar::combineWith);
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
