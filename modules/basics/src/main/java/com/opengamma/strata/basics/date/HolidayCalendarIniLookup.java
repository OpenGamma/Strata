/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.Guavate.toImmutableList;
import static com.opengamma.strata.collect.Guavate.toImmutableSet;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.DAY_OF_WEEK;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.collect.io.PropertySet;
import com.opengamma.strata.collect.io.ResourceConfig;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.named.NamedLookup;

/**
 * Loads holiday calendar implementations from CSV.
 * <p>
 * These will form the standard holiday calendars available in {@link ReferenceData#standard()}.
 */
final class HolidayCalendarIniLookup
    implements NamedLookup<HolidayCalendar> {

  /**
   * The logger.
   */
  private static final Logger log = Logger.getLogger(HolidayCalendarIniLookup.class.getName());
  /**
   * The singleton instance of the lookup.
   */
  public static final HolidayCalendarIniLookup INSTANCE = new HolidayCalendarIniLookup();

  /**
   * The Weekend key name.
   */
  private static final String WEEKEND_KEY = "Weekend";
  /**
   * The lenient day-of-week parser.
   */
  private static final DateTimeFormatter DOW_PARSER = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .parseLenient()
      .appendText(DAY_OF_WEEK)
      .toFormatter(Locale.ENGLISH);
  /**
   * The lenient month-day parser.
   */
  private static final DateTimeFormatter DAY_MONTH_PARSER = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .parseLenient()
      .appendText(MONTH_OF_YEAR)
      .appendOptional(new DateTimeFormatterBuilder().appendLiteral('-').toFormatter(Locale.ENGLISH))
      .appendValue(DAY_OF_MONTH)
      .toFormatter(Locale.ENGLISH);

  /**
   * The cache by name.
   */
  private static final ImmutableMap<String, HolidayCalendar> BY_NAME = loadFromIni("HolidayCalendarData.ini");

  /**
   * Restricted constructor.
   */
  private HolidayCalendarIniLookup() {
  }

  //-------------------------------------------------------------------------
  @Override
  public Map<String, HolidayCalendar> lookupAll() {
    return BY_NAME;
  }

  // accessible for testing
  static ImmutableMap<String, HolidayCalendar> loadFromIni(String filename) {
    List<ResourceLocator> resources = ResourceConfig.orderedResources(filename);
    Map<String, HolidayCalendar> map = new HashMap<>();
    for (ResourceLocator resource : resources) {
      try {
        IniFile ini = IniFile.of(resource.getCharSource());
        for (String sectionName : ini.sections()) {
          PropertySet section = ini.section(sectionName);
          HolidayCalendar parsed = parseHolidayCalendar(sectionName, section);
          map.put(parsed.getName(), parsed);
          map.putIfAbsent(parsed.getName().toUpperCase(Locale.ENGLISH), parsed);
        }
      } catch (RuntimeException ex) {
        log.log(Level.SEVERE, "Error processing resource as Holiday Calendar INI file: " + resource, ex);
        return ImmutableMap.of();
      }
    }
    return ImmutableMap.copyOf(map);
  }

  private static HolidayCalendar parseHolidayCalendar(String calendarName, PropertySet section) {
    String weekendStr = section.value(WEEKEND_KEY);
    Set<DayOfWeek> weekends = parseWeekends(weekendStr);
    List<LocalDate> holidays = new ArrayList<>();
    for (String key : section.keys()) {
      if (key.equals(WEEKEND_KEY)) {
        continue;
      }
      String value = section.value(key);
      if (key.length() == 4) {
        int year = Integer.parseInt(key);
        holidays.addAll(parseYearDates(year, value));
      } else {
        holidays.add(LocalDate.parse(key));
      }
    }
    // build result
    return ImmutableHolidayCalendar.of(HolidayCalendarId.of(calendarName), holidays, weekends);
  }

  // parse weekend format, such as 'Sat,Sun'
  private static Set<DayOfWeek> parseWeekends(String str) {
    List<String> split = Splitter.on(',').splitToList(str);
    return split.stream()
        .map(v -> DOW_PARSER.parse(v, DayOfWeek::from))
        .collect(toImmutableSet());
  }

  // parse year format, such as 'Jan1,Mar12,Dec25' or '2015-01-01,2015-03-12,2015-12-25'
  private static List<LocalDate> parseYearDates(int year, String str) {
    List<String> split = Splitter.on(',').splitToList(str);
    return split.stream()
        .map(v -> parseDate(year, v))
        .collect(toImmutableList());
  }

  private static LocalDate parseDate(int year, String str) {
    try {
      return MonthDay.parse(str, DAY_MONTH_PARSER).atYear(year);
    } catch (DateTimeParseException ex) {
      LocalDate date = LocalDate.parse(str);
      if (date.getYear() != year) {
        throw new IllegalArgumentException("Parsed date had incorrect year: " + str + ", but expected: " + year);
      }
      return date;
    }
  }

}
