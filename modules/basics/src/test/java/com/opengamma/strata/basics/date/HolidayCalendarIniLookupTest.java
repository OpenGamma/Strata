/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.TestHelper.caputureLog;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;

/**
 * Test {@link HolidayCalendar}.
 */
public class HolidayCalendarIniLookupTest {

  @Test
  public void test_valid1() {
    ImmutableMap<String, HolidayCalendar> lookup = HolidayCalendarIniLookup.loadFromIni("HolidayCalendarDataValid1.ini");
    assertThat(lookup.size()).isEqualTo(1);

    HolidayCalendar test = lookup.get("TEST-VALID");
    assertThat(test.isHoliday(date(2015, 1, 1))).isTrue();
    assertThat(test.isHoliday(date(2015, 1, 6))).isTrue();
    assertThat(test.isHoliday(date(2015, 4, 5))).isTrue();
    assertThat(test.isHoliday(date(2015, 12, 25))).isTrue();
    assertThat(test.isHoliday(date(2016, 1, 1))).isTrue();
    assertThat(test.getName()).isEqualTo("TEST-VALID");
    assertThat(test.toString()).isEqualTo("HolidayCalendar[TEST-VALID]");
  }

  @Test
  public void test_workingDays() {
    ImmutableMap<String, HolidayCalendar> lookup =
        HolidayCalendarIniLookup.loadFromIni("HolidayCalendarDataWorkingDays.ini");
    assertThat(lookup.size()).isEqualTo(1);

    HolidayCalendar test = lookup.get("TEST-WORKINGDAYS");
    assertThat(test.isHoliday(date(2015, 1, 1))).isTrue();
    assertThat(test.isHoliday(date(2015, 1, 6))).isTrue();
    assertThat(test.isHoliday(date(2015, 4, 5))).isTrue();
    assertThat(test.isHoliday(date(2015, 12, 25))).isTrue();
    assertThat(test.isHoliday(date(2016, 1, 1))).isTrue();
    assertThat(test.isHoliday(date(2016, 1, 2))).isFalse();
    assertThat(test.isHoliday(date(2016, 1, 3))).isFalse();
    assertThat(test.getName()).isEqualTo("TEST-WORKINGDAYS");
    assertThat(test.toString()).isEqualTo("HolidayCalendar[TEST-WORKINGDAYS]");
  }

  @Test
  public void test_valid2() {
    ImmutableMap<String, HolidayCalendar> lookup = HolidayCalendarIniLookup.loadFromIni("HolidayCalendarDataValid2.ini");
    assertThat(lookup.size()).isEqualTo(1);

    HolidayCalendar test = lookup.get("TEST-VALID");
    assertThat(test.isHoliday(date(2015, 1, 1))).isTrue();
    assertThat(test.isHoliday(date(2015, 1, 6))).isTrue();
    assertThat(test.isHoliday(date(2015, 4, 5))).isTrue();
    assertThat(test.isHoliday(date(2015, 12, 25))).isTrue();
    assertThat(test.isHoliday(date(2016, 1, 1))).isTrue();
    assertThat(test.getName()).isEqualTo("TEST-VALID");
    assertThat(test.toString()).isEqualTo("HolidayCalendar[TEST-VALID]");
  }

  @Test
  public void test_valid1equals2() {
    ImmutableMap<String, HolidayCalendar> lookup1 = HolidayCalendarIniLookup.loadFromIni("HolidayCalendarDataValid1.ini");
    ImmutableMap<String, HolidayCalendar> lookup2 = HolidayCalendarIniLookup.loadFromIni("HolidayCalendarDataValid2.ini");
    assertThat(lookup1).isEqualTo(lookup2);
  }

  @Test
  public synchronized void test_invalid1_invalidYear() {
    List<LogRecord> captured = caputureLog(
        HolidayCalendarIniLookup.class,
        () -> HolidayCalendarIniLookup.loadFromIni("HolidayCalendarDataInvalid1.ini"));
    assertThat(captured.size()).isEqualTo(1);
    LogRecord record = captured.get(0);
    assertThat(record.getLevel()).isEqualTo(Level.SEVERE);
    assertThat(record.getThrown().getMessage().contains("Parsed date had incorrect year")).isTrue();
  }

  @Test
  public synchronized void test_invalid1_invalidDayOfWeek() {
    List<LogRecord> captured = caputureLog(
        HolidayCalendarIniLookup.class,
        () -> HolidayCalendarIniLookup.loadFromIni("HolidayCalendarDataInvalid2.ini"));
    assertThat(captured.size()).isEqualTo(1);
    LogRecord record = captured.get(0);
    assertThat(record.getLevel()).isEqualTo(Level.SEVERE);
    assertThat(record.getThrown() instanceof DateTimeParseException).isTrue();
    assertThat(record.getThrown().getMessage().contains("'Bob'")).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public synchronized void test_defaultByCurrency_valid() {
    ImmutableMap<Currency, HolidayCalendarId> test =
        HolidayCalendarIniLookup.loadDefaultsFromIni("HolidayCalendarDefaultDataValid.ini");
    assertThat(test.size()).isEqualTo(2);

    assertThat(test.get(Currency.GBP)).isEqualTo(HolidayCalendarIds.GBLO);
    assertThat(test.get(Currency.USD)).isEqualTo(HolidayCalendarIds.NYSE);
  }

  @Test
  public synchronized void test_defaultByCurrency_invalid() {
    List<LogRecord> captured = caputureLog(
        HolidayCalendarIniLookup.class,
        () -> HolidayCalendarIniLookup.loadFromIni("HolidayCalendarDefaultDataInvalid.ini"));
    assertThat(captured.size()).isEqualTo(1);
    LogRecord record = captured.get(0);
    assertThat(record.getLevel()).isEqualTo(Level.SEVERE);
    assertThat(record.getMessage().contains("Error processing resource")).isTrue();
  }

}
