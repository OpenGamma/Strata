/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.TestHelper.caputureLog;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test {@link HolidayCalendar}.
 */
@Test
public class HolidayCalendarIniLookupTest {

  public void test_valid1() {
    ImmutableMap<String, HolidayCalendar> lookup = HolidayCalendarIniLookup.loadFromIni("HolidayCalendarDataValid1.ini");
    assertEquals(lookup.size(), 1);

    HolidayCalendar test = lookup.get("TEST-VALID");
    assertTrue(test.isHoliday(date(2015, 1, 1)));
    assertTrue(test.isHoliday(date(2015, 1, 6)));
    assertTrue(test.isHoliday(date(2015, 4, 5)));
    assertTrue(test.isHoliday(date(2015, 12, 25)));
    assertTrue(test.isHoliday(date(2016, 1, 1)));
    assertEquals(test.getName(), "TEST-VALID");
    assertEquals(test.toString(), "HolidayCalendar[TEST-VALID]");
  }

  public void test_valid2() {
    ImmutableMap<String, HolidayCalendar> lookup = HolidayCalendarIniLookup.loadFromIni("HolidayCalendarDataValid2.ini");
    assertEquals(lookup.size(), 1);

    HolidayCalendar test = lookup.get("TEST-VALID");
    assertTrue(test.isHoliday(date(2015, 1, 1)));
    assertTrue(test.isHoliday(date(2015, 1, 6)));
    assertTrue(test.isHoliday(date(2015, 4, 5)));
    assertTrue(test.isHoliday(date(2015, 12, 25)));
    assertTrue(test.isHoliday(date(2016, 1, 1)));
    assertEquals(test.getName(), "TEST-VALID");
    assertEquals(test.toString(), "HolidayCalendar[TEST-VALID]");
  }

  public void test_valid1equals2() {
    ImmutableMap<String, HolidayCalendar> lookup1 = HolidayCalendarIniLookup.loadFromIni("HolidayCalendarDataValid1.ini");
    ImmutableMap<String, HolidayCalendar> lookup2 = HolidayCalendarIniLookup.loadFromIni("HolidayCalendarDataValid2.ini");
    assertEquals(lookup1, lookup2);
  }

  public synchronized void test_invalid1_invalidYear() {
    List<LogRecord> captured = caputureLog(
        HolidayCalendarIniLookup.class,
        () -> HolidayCalendarIniLookup.loadFromIni("HolidayCalendarDataInvalid1.ini"));
    assertEquals(captured.size(), 1);
    LogRecord record = captured.get(0);
    assertEquals(record.getLevel(), Level.SEVERE);
    assertTrue(record.getThrown().getMessage().contains("Parsed date had incorrect year"));
  }

  public synchronized void test_invalid1_invalidDayOfWeek() {
    List<LogRecord> captured = caputureLog(
        HolidayCalendarIniLookup.class,
        () -> HolidayCalendarIniLookup.loadFromIni("HolidayCalendarDataInvalid2.ini"));
    assertEquals(captured.size(), 1);
    LogRecord record = captured.get(0);
    assertEquals(record.getLevel(), Level.SEVERE);
    assertTrue(record.getThrown() instanceof DateTimeParseException);
    assertTrue(record.getThrown().getMessage().contains("'Bob'"));
  }

}
