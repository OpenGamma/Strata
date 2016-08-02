/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import com.google.common.base.Splitter;
import com.opengamma.strata.basics.ReferenceData;
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

  /**
   * An instance declaring no holidays and no weekends.
   * <p>
   * This calendar has the effect of making every day a business day.
   * It is often used to indicate that a holiday calendar does not apply.
   */
  public static final HolidayCalendar NO_HOLIDAYS = NoHolidaysCalendar.INSTANCE;
  /**
   * An instance declaring all days as business days except Saturday/Sunday weekends.
   * <p>
   * This calendar is mostly useful in testing scenarios.
   * Note that not all countries use Saturday and Sunday weekends.
   */
  public static final HolidayCalendar SAT_SUN = WeekendHolidayCalendar.SAT_SUN;
  /**
   * An instance declaring all days as business days except Friday/Saturday weekends.
   * <p>
   * This calendar is mostly useful in testing scenarios.
   */
  public static final HolidayCalendar FRI_SAT = WeekendHolidayCalendar.FRI_SAT;
  /**
   * An instance declaring all days as business days except Thursday/Friday weekends.
   * <p>
   * This calendar is mostly useful in testing scenarios.
   */
  public static final HolidayCalendar THU_FRI = WeekendHolidayCalendar.THU_FRI;

  // This constant must be after the constants above in the source file.
  /**
   * The extended enum lookup from name to instance.
   */
  private static final ExtendedEnum<HolidayCalendar> ENUM_LOOKUP = ExtendedEnum.of(HolidayCalendar.class);

  //-------------------------------------------------------------------------
  /**
   * Obtains an instance from the set of standard holiday calendars.
   * <p>
   * The unique name identifies the calendar in the <i>standard</i> source of calendars.
   * The standard source is loaded at startup based on the {@code HolidayCalendar.ini} file.
   * <p>
   * Applications should generally avoid using this method.
   * Instead, applications should refer to holidays using {@link HolidayCalendarId},
   * resolving them using a {@link ReferenceData}.
   * <p>
   * It is possible to combine two or more calendars using the '+' symbol.
   * For example, 'GBLO+USNY' will combine the separate 'GBLO' and 'USNY' calendars.
   * 
   * @param uniqueName  the unique name of the calendar
   * @return the holiday calendar
   */
  public static HolidayCalendar of(String uniqueName) {
    if (uniqueName.contains("+")) {
      return Splitter.on('+').splitToList(uniqueName).stream()
          .map(HolidayCalendars::of)
          .reduce(NO_HOLIDAYS, HolidayCalendar::combinedWith);
    }
    return ENUM_LOOKUP.lookup(uniqueName);
  }

  /**
   * Gets the extended enum helper.
   * <p>
   * This helper allows instances of the calendar to be looked up.
   * It also provides the complete set of available instances.
   * 
   * @return the extended enum helper
   */
  public static ExtendedEnum<HolidayCalendar> extendedEnum() {
    return HolidayCalendars.ENUM_LOOKUP;
  }

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private HolidayCalendars() {
  }

}
