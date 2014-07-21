/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import static org.testng.Assert.assertEquals;

import java.time.Period;
import java.time.format.DateTimeParseException;

import org.testng.annotations.Test;

/**
 * Tests for the tenor class.
 */
public class TenorTest {

  @Test
  public void testPeriodStaticConstructor() {
    assertEquals(Tenor.of(Period.ofDays(1)), Tenor.ONE_DAY);
    assertEquals(Tenor.of(Period.ofDays(7)), Tenor.ONE_WEEK);
    assertEquals(Tenor.of(Period.ofMonths(1)), Tenor.ONE_MONTH);
    assertEquals(Tenor.of(Period.ofYears(1)), Tenor.ONE_YEAR);
    assertEquals(Tenor.ofDays(1), Tenor.ONE_DAY);
    assertEquals(Tenor.ofDays(7), Tenor.ONE_WEEK);
    assertEquals(Tenor.ofMonths(1), Tenor.ONE_MONTH);
    assertEquals(Tenor.ofYears(1), Tenor.ONE_YEAR);
  }

  @Test(expectedExceptions = DateTimeParseException.class)
  public void testParseFailure() {
    Tenor.parse("0D");
  }

  @Test
  public void testParsePeriod() {
    assertEquals(Tenor.parse("P2D"), Tenor.TWO_DAYS);
    assertEquals(Tenor.parse("P14D"), Tenor.TWO_WEEKS);
    assertEquals(Tenor.parse("P2M"), Tenor.TWO_MONTHS);
    assertEquals(Tenor.parse("P2Y"), Tenor.TWO_YEARS);
  }

  @Test
  public void testGetPeriod() {
    assertEquals(Tenor.THREE_DAYS.getPeriod(), Period.ofDays(3));
    assertEquals(Tenor.THREE_WEEKS.getPeriod(), Period.ofDays(21));
    assertEquals(Tenor.THREE_MONTHS.getPeriod(), Period.ofMonths(3));
    assertEquals(Tenor.THREE_YEARS.getPeriod(), Period.ofYears(3));
  }

  @Test
  public void testToFormattedString() {
    assertEquals(Tenor.THREE_DAYS.toFormattedString(), "P3D");
    assertEquals(Tenor.TWO_WEEKS.toFormattedString(), "P14D");
    assertEquals(Tenor.FOUR_MONTHS.toFormattedString(), "P4M");
    assertEquals(Tenor.FOUR_YEARS.toFormattedString(), "P4Y");
  }

  @Test
  public void testToString() {
    assertEquals(Tenor.FOUR_MONTHS.toString(), "Tenor[P4M]");
    assertEquals(Tenor.FOUR_YEARS.toString(), "Tenor[P4Y]");
  }

}

