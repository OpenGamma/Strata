/*
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.io.IniFile;
import com.opengamma.strata.collect.io.PropertySet;
import com.opengamma.strata.collect.io.ResourceConfig;
import com.opengamma.strata.collect.io.ResourceLocator;
import com.opengamma.strata.collect.named.NamedLookup;

/**
 * Loads holiday calendar implementations from INI files.
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
   * The WorkingDays key name.
   */
  private static final String WORKING_DAYS_KEY = "WorkingDays";
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
   * The holiday calendars by name.
   */
  private static final ImmutableMap<String, HolidayCalendar> BY_NAME = loadFromIni("HolidayCalendarData.ini");
  /**
   * The default holiday calendars by currency.
   */
  private static final ImmutableMap<Currency, HolidayCalendarId> BY_CURRENCY =
      loadDefaultsFromIni("HolidayCalendarDefaultData.ini");

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

  // finds a default
  HolidayCalendarId defaultByCurrency(Currency currency) {
    Optional<HolidayCalendarId> calId = findDefaultByCurrency(currency);
    return calId.orElseThrow(() -> new IllegalArgumentException(
        "No default Holiday Calendar for currency " + currency));
  }

  // try to find a default
  Optional<HolidayCalendarId> findDefaultByCurrency(Currency currency) {
    return Optional.ofNullable(BY_CURRENCY.get(currency));
  }

  //-------------------------------------------------------------------------
  @VisibleForTesting
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

  // parses the holiday calendar
  private static HolidayCalendar parseHolidayCalendar(String calendarName, PropertySet section) {
    String weekendStr = section.value(WEEKEND_KEY);
    Set<DayOfWeek> weekends = parseWeekends(weekendStr);
    List<LocalDate> holidays = new ArrayList<>();
    Set<LocalDate> workingDays = new HashSet<>();
    for (String key : section.keys()) {
      if (key.equals(WEEKEND_KEY)) {
        continue;
      }
      String value = section.value(key);
      if (key.length() == 4) {
        int year = Integer.parseInt(key);
        holidays.addAll(parseYearDates(year, value));
      } else if (WORKING_DAYS_KEY.equals(key)) {
        workingDays.addAll(parseDates(value));
      } else {
        holidays.add(LocalDate.parse(key));
      }
    }
    // build result
    return ImmutableHolidayCalendar.of(HolidayCalendarId.of(calendarName), holidays, weekends, workingDays);
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

  // parse comma separated date format such as "2015-01-01,2015-03-12"
  private static List<LocalDate> parseDates(String str) {
    List<String> split = Splitter.on(',').splitToList(str);
    return split.stream()
        .map(LocalDate::parse)
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

  //-------------------------------------------------------------------------
  @VisibleForTesting
  static ImmutableMap<Currency, HolidayCalendarId> loadDefaultsFromIni(String filename) {
    List<ResourceLocator> resources = ResourceConfig.orderedResources(filename);
    Map<Currency, HolidayCalendarId> map = new HashMap<>();
    for (ResourceLocator resource : resources) {
      try {
        IniFile ini = IniFile.of(resource.getCharSource());
        PropertySet section = ini.section("defaultByCurrency");
        for (String currencyCode : section.keys()) {
          map.put(Currency.of(currencyCode), HolidayCalendarId.of(section.value(currencyCode)));
        }
      } catch (RuntimeException ex) {
        log.log(Level.SEVERE, "Error processing resource as Holiday Calendar Defaults INI file: " + resource, ex);
        return ImmutableMap.of();
      }
    }
    return ImmutableMap.copyOf(map);
  }

}
