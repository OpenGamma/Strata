/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Identifiers for common holiday calendars.
 * <p>
 * The constants defined here are identifiers, used to locate instances of
 * {@link HolidayCalendar} from {@link ReferenceData}.
 * <p>
 * All the constants defined here will be available from {@link ReferenceData#standard()}.
 * The associated holiday data may or may not be sufficient for your production needs.
 * <p>
 * The standard holiday data was obtained by direct research - it was not derived from a vendor
 * of holiday calendar data. Two approaches are available to add or change the data.
 * Firstly, applications can provide their own {@code ReferenceData} implementation, mapping
 * the identifier to any data desired. Secondly, the standard data can be amended by following
 * the instructions in {@code HolidayCalendar.ini}.
 */
public final class HolidayCalendarIds {

  /**
   * An identifier for a calendar declaring no holidays and no weekends, with code 'NoHolidays'.
   * <p>
   * This calendar has the effect of making every day a business day.
   * It is often used to indicate that a holiday calendar does not apply.
   */
  public static final HolidayCalendarId NO_HOLIDAYS = HolidayCalendarId.of("NoHolidays");
  /**
   * An identifier for a calendar declaring all days as business days
   * except Saturday/Sunday weekends, with code 'SatSun'.
   * <p>
   * This calendar is mostly useful in testing scenarios.
   * Note that not all countries use Saturday and Sunday weekends.
   */
  public static final HolidayCalendarId SAT_SUN = HolidayCalendarId.of("Sat/Sun");
  /**
   * An identifier for a calendar declaring all days as business days
   * except Friday/Saturday weekends, with code 'FriSat'.
   * <p>
   * This calendar is mostly useful in testing scenarios.
   */
  public static final HolidayCalendarId FRI_SAT = HolidayCalendarId.of("Fri/Sat");
  /**
   * An identifier for a calendar declaring all days as business days
   * except Thursday/Friday weekends, with code 'ThuFri'.
   * <p>
   * This calendar is mostly useful in testing scenarios.
   */
  public static final HolidayCalendarId THU_FRI = HolidayCalendarId.of("Thu/Fri");

  /**
   * An identifier for the holiday calendar of London, United Kingdom, with code 'GBLO'.
   * <p>
   * This constant references the calendar for London bank holidays.
   */
  public static final HolidayCalendarId GBLO = HolidayCalendarId.of("GBLO");
  /**
   * An identifier for the holiday calendar of Paris, France, with code 'FRPA'.
   * <p>
   * This constant references the calendar for Paris public holidays.
   */
  public static final HolidayCalendarId FRPA = HolidayCalendarId.of("FRPA");
  /**
   * An identifier for the holiday calendar of Zurich, Switzerland, with code 'EUTA'.
   * <p>
   * This constant references the calendar for Zurich public holidays.
   */
  public static final HolidayCalendarId CHZU = HolidayCalendarId.of("CHZU");
  /**
   * An identifier for the holiday calendar of the European Union TARGET system, with code 'EUTA'.
   * <p>
   * This constant references the calendar for the TARGET interbank payment system holidays.
   * <p>
   * Referenced by the 2006 ISDA definitions 1.8.
   */
  public static final HolidayCalendarId EUTA = HolidayCalendarId.of("EUTA");
  /**
   * An identifier for the holiday calendar of United States Government Securities, with code 'USGS'.
   * <p>
   * This constant references the calendar for United States Government Securities as per SIFMA.
   * <p>
   * Referenced by the 2006 ISDA definitions 1.11.
   */
  public static final HolidayCalendarId USGS = HolidayCalendarId.of("USGS");
  /**
   * An identifier for the holiday calendar of New York, United States, with code 'USNY'.
   * <p>
   * This constant references the calendar for New York holidays.
   */
  public static final HolidayCalendarId USNY = HolidayCalendarId.of("USNY");
  /**
   * An identifier for the holiday calendar of the Federal Reserve Bank of New York, with code 'NYFD'.
   * <p>
   * This constant references the calendar for the Federal Reserve Bank of New York holidays.
   * <p>
   * Referenced by the 2006 ISDA definitions 1.9.
   */
  public static final HolidayCalendarId NYFD = HolidayCalendarId.of("NYFD");
  /**
   * An identifier for the holiday calendar of the New York Stock Exchange, with code 'NYSE'.
   * <p>
   * This constant references the calendar for the New York Stock Exchange.
   * <p>
   * Referenced by the 2006 ISDA definitions 1.10.
   */
  public static final HolidayCalendarId NYSE = HolidayCalendarId.of("NYSE");
  /**
   * An identifier for the holiday calendar of Tokyo, Japan, with code 'JPTO'.
   * <p>
   * This constant references the calendar for Tokyo bank holidays.
   */
  public static final HolidayCalendarId JPTO = HolidayCalendarId.of("JPTO");

  /**
   * An identifier for the holiday calendar of Sydney, Australia, with code 'AUSY'.
   * <p>
   * This constant references the calendar for Sydney bank holidays.
   */
  public static final HolidayCalendarId AUSY = HolidayCalendarId.of("AUSY");
  /**
   * An identifier for the holiday calendar of Brazil, with code 'BRBD'.
   * <p>
   * This constant references the combined calendar for Brazil bank holidays.
   * This unites city-level calendars.
   */
  public static final HolidayCalendarId BRBD = HolidayCalendarId.of("BRBD");
  /**
   * An identifier for the holiday calendar of Toronto, Canada, with code 'CATO'.
   * <p>
   * This constant references the calendar for Toronto bank holidays.
   */
  public static final HolidayCalendarId CATO = HolidayCalendarId.of("CATO");
  /**
   * An identifier for the holiday calendar of Copenhagen, Denmark, with code 'DKCO'.
   * <p>
   * This constant references the calendar for Copenhagen bank holidays.
   */
  public static final HolidayCalendarId DKCO = HolidayCalendarId.of("DKCO");
  /**
   * An identifier for the holiday calendar of Budapest, Hungary, with code 'HUBU'.
   * <p>
   * This constant references the calendar for Budapest bank holidays.
   */
  public static final HolidayCalendarId HUBU = HolidayCalendarId.of("HUBU");
  /**
   * An identifier for the holiday calendar of Mexico City, Mexico, with code 'MXMC'.
   * <p>
   * This constant references the calendar for Mexico City bank holidays.
   */
  public static final HolidayCalendarId MXMC = HolidayCalendarId.of("MXMC");
  /**
   * An identifier for the holiday calendar of Oslo, Norway, with code 'NOOS'.
   * <p>
   * This constant references the calendar for Oslo bank holidays.
   */
  public static final HolidayCalendarId NOOS = HolidayCalendarId.of("NOOS");
  /**
   * An identifier for the holiday calendar of Warsaw, Poland, with code 'PLWA'.
   * <p>
   * This constant references the calendar for Warsaw bank holidays.
   */
  public static final HolidayCalendarId PLWA = HolidayCalendarId.of("PLWA");
  /**
   * An identifier for the holiday calendar of Stockholm, Sweden, with code 'SEST'.
   * <p>
   * This constant references the calendar for Stockholm bank holidays.
   */
  public static final HolidayCalendarId SEST = HolidayCalendarId.of("SEST");
  /**
   * An identifier for the holiday calendar of Johannesburg, South Africa, with code 'ZAJO'.
   * <p>
   * This constant references the calendar for Johannesburg bank holidays.
   */
  public static final HolidayCalendarId ZAJO = HolidayCalendarId.of("ZAJO");

  //-------------------------------------------------------------------------
  /**
   * Restricted constructor.
   */
  private HolidayCalendarIds() {
  }

}
