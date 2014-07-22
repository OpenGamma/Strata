/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import static com.opengamma.basics.date.Tenor.TENOR_12M;
import static com.opengamma.basics.date.Tenor.TENOR_18M;
import static com.opengamma.basics.date.Tenor.TENOR_1D;
import static com.opengamma.basics.date.Tenor.TENOR_1M;
import static com.opengamma.basics.date.Tenor.TENOR_1W;
import static com.opengamma.basics.date.Tenor.TENOR_1Y;
import static com.opengamma.basics.date.Tenor.TENOR_2D;
import static com.opengamma.basics.date.Tenor.TENOR_2M;
import static com.opengamma.basics.date.Tenor.TENOR_2W;
import static com.opengamma.basics.date.Tenor.TENOR_2Y;
import static com.opengamma.basics.date.Tenor.TENOR_3D;
import static com.opengamma.basics.date.Tenor.TENOR_3M;
import static com.opengamma.basics.date.Tenor.TENOR_3W;
import static com.opengamma.basics.date.Tenor.TENOR_3Y;
import static com.opengamma.basics.date.Tenor.TENOR_4M;
import static com.opengamma.basics.date.Tenor.TENOR_4Y;
import static com.opengamma.basics.date.Tenor.TENOR_6W;
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
    assertEquals(Tenor.of(Period.ofDays(1)), TENOR_1D);
    assertEquals(Tenor.of(Period.ofDays(7)), TENOR_1W);
    assertEquals(Tenor.of(Period.ofMonths(1)), TENOR_1M);
    assertEquals(Tenor.of(Period.ofYears(1)), TENOR_1Y);
    assertEquals(Tenor.ofDays(1), TENOR_1D);
    assertEquals(Tenor.ofDays(7), TENOR_1W);
    assertEquals(Tenor.ofMonths(1), TENOR_1M);
    assertEquals(Tenor.ofYears(1), TENOR_1Y);
  }

  @Test(expectedExceptions = DateTimeParseException.class)
  public void testParseFailure() {
    Tenor.parse("2K");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativePeriodNotAllowed() {
    Tenor.ofDays(-1);
  }

  @Test
  public void testGetPeriod() {
    assertEquals(TENOR_3D.getPeriod(), Period.ofDays(3));
    assertEquals(TENOR_3W.getPeriod(), Period.ofDays(21));
    assertEquals(TENOR_3M.getPeriod(), Period.ofMonths(3));
    assertEquals(TENOR_3Y.getPeriod(), Period.ofYears(3));
  }

  @Test
  public void testParsePeriod() {
    assertEquals(Tenor.parse("2D"), TENOR_2D);
    assertEquals(Tenor.parse("2W"), TENOR_2W);
    assertEquals(Tenor.parse("6W"), TENOR_6W);
    assertEquals(Tenor.parse("2M"), TENOR_2M);
    assertEquals(Tenor.parse("1Y"), TENOR_1Y);
    assertEquals(Tenor.parse("12M"), TENOR_12M);
    assertEquals(Tenor.parse("18M"), TENOR_18M);
    assertEquals(Tenor.parse("2Y"), TENOR_2Y);
  }
  @Test
  public void testParsePrefixedPeriod() {
    assertEquals(Tenor.parse("P2D"), TENOR_2D);
    assertEquals(Tenor.parse("P2W"), TENOR_2W);
    assertEquals(Tenor.parse("P6W"), TENOR_6W);
    assertEquals(Tenor.parse("P2M"), TENOR_2M);
    assertEquals(Tenor.parse("P2Y"), TENOR_2Y);
  }

  @Test
  public void testToString() {
    assertEquals(TENOR_3D.toString(), "3D");
    assertEquals(TENOR_2W.toString(), "2W");
    assertEquals(TENOR_4M.toString(), "4M");
    assertEquals(TENOR_1Y.toString(), "1Y");
    assertEquals(TENOR_12M.toString(), "12M");
    assertEquals(TENOR_18M.toString(), "18M");
    assertEquals(TENOR_4Y.toString(), "4Y");
  }

}

