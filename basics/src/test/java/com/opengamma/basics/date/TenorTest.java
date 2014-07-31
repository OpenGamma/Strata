/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import static com.opengamma.basics.date.Tenor.TENOR_10M;
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
import static com.opengamma.collect.TestHelper.assertJodaConvert;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrows;
import static java.time.temporal.ChronoUnit.CENTURIES;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.YEARS;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.UnsupportedTemporalTypeException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Tests for the tenor class.
 */
@Test
public class TenorTest {

  public void test_of_Period() {
    assertEquals(Tenor.of(Period.ofDays(1)), TENOR_1D);
    assertEquals(Tenor.of(Period.ofDays(7)), TENOR_1W);
    assertEquals(Tenor.of(Period.ofWeeks(2)), TENOR_2W);
    assertEquals(Tenor.of(Period.ofMonths(1)), TENOR_1M);
    assertEquals(Tenor.of(Period.ofYears(1)), TENOR_1Y);
  }

  public void test_of_int() {
    assertEquals(Tenor.ofDays(1), TENOR_1D);
    assertEquals(Tenor.ofDays(7), TENOR_1W);
    assertEquals(Tenor.ofWeeks(2), TENOR_2W);
    assertEquals(Tenor.ofMonths(1), TENOR_1M);
    assertEquals(Tenor.ofYears(1), TENOR_1Y);
  }

  public void test_of_notNegative() {
    assertThrows(() -> Tenor.of(Period.ofDays(-1)), IllegalArgumentException.class);
    assertThrows(() -> Tenor.ofDays(-1), IllegalArgumentException.class);
    assertThrows(() -> Tenor.ofWeeks(-1), IllegalArgumentException.class);
    assertThrows(() -> Tenor.ofMonths(-1), IllegalArgumentException.class);
    assertThrows(() -> Tenor.ofYears(-1), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_parse_String_roundTrip() {
    assertEquals(Tenor.parse(TENOR_10M.toString()), TENOR_10M);
  }

  @DataProvider(name = "parseGood")
  Object[][] data_parseGood() {
    return new Object[][] {
        {"2D", TENOR_2D},
        {"2W", TENOR_2W},
        {"6W", TENOR_6W},
        {"2M", TENOR_2M},
        {"12M", TENOR_12M},
        {"1Y", TENOR_1Y},
        {"2Y", TENOR_2Y},
        {"P2D", TENOR_2D},
        {"P2W", TENOR_2W},
        {"P6W", TENOR_6W},
        {"P2M", TENOR_2M},
        {"P12M", TENOR_12M},
        {"P1Y", TENOR_1Y},
        {"P2Y", TENOR_2Y},
    };
  }

  @Test(dataProvider = "parseGood")
  public void test_parse_String_good(String input, Tenor expected) {
    assertEquals(Tenor.parse(input), expected);
  }

  @DataProvider(name = "parseBad")
  Object[][] data_parseBad() {
    return new Object[][] {
      {""},
      {"2"},
      {"2K"},
      {"-2D"},
      {null},
    };
  }

  @Test(dataProvider = "parseBad", expectedExceptions = IllegalArgumentException.class)
  public void test_parse_String_bad(String input) {
    Tenor.parse(input);
  }

  //-------------------------------------------------------------------------
  public void test_getPeriod() {
    assertEquals(TENOR_3D.getPeriod(), Period.ofDays(3));
    assertEquals(TENOR_3W.getPeriod(), Period.ofDays(21));
    assertEquals(TENOR_3M.getPeriod(), Period.ofMonths(3));
    assertEquals(TENOR_3Y.getPeriod(), Period.ofYears(3));
    assertThrows(() -> TENOR_10M.get(CENTURIES), UnsupportedTemporalTypeException.class);
  }

  //-------------------------------------------------------------------------
  public void test_temporalAmount() {
    assertEquals(TENOR_3D.getUnits(), ImmutableList.of(YEARS, MONTHS, DAYS));
    assertEquals(TENOR_3D.get(DAYS), 3);
    assertEquals(TENOR_3D.addTo(LocalDate.of(2014, 6, 30)), LocalDate.of(2014, 7, 3));
    assertEquals(TENOR_3D.subtractFrom(LocalDate.of(2014, 6, 30)), LocalDate.of(2014, 6, 27));
    assertEquals(LocalDate.of(2014, 6, 30).plus(TENOR_1W), LocalDate.of(2014, 7, 7));
    assertEquals(LocalDate.of(2014, 6, 30).minus(TENOR_1W), LocalDate.of(2014, 6, 23));
  }

  //-------------------------------------------------------------------------
  public void test_equals_hashCode() {
    Tenor a1 = TENOR_3D;
    Tenor a2 = Tenor.ofDays(3);
    Tenor b = TENOR_4M;
    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(a2), true);
    
    assertEquals(a2.equals(a1), true);
    assertEquals(a2.equals(a2), true);
    assertEquals(a2.equals(b), false);
    
    assertEquals(b.equals(a1), false);
    assertEquals(b.equals(a2), false);
    assertEquals(b.equals(b), true);
    
    assertEquals(a1.hashCode(), a2.hashCode());
  }

  public void test_equals_bad() {
    assertEquals(TENOR_3D.equals(null), false);
    assertEquals(TENOR_3D.equals("String"), false);
    assertEquals(TENOR_3D.equals(new Object()), false);
  }

  //-------------------------------------------------------------------------
  public void test_toString() {
    assertEquals(TENOR_3D.toString(), "3D");
    assertEquals(TENOR_2W.toString(), "2W");
    assertEquals(TENOR_4M.toString(), "4M");
    assertEquals(TENOR_1Y.toString(), "1Y");
    assertEquals(TENOR_12M.toString(), "12M");
    assertEquals(TENOR_18M.toString(), "18M");
    assertEquals(TENOR_4Y.toString(), "4Y");
  }

  //-----------------------------------------------------------------------
  public void test_serialization() {
    assertSerialization(TENOR_3D);
    assertSerialization(TENOR_4M);
    assertSerialization(TENOR_3Y);
  }

  public void test_jodaConvert() {
    assertJodaConvert(Tenor.class, TENOR_3D);
    assertJodaConvert(Tenor.class, TENOR_4M);
    assertJodaConvert(Tenor.class, TENOR_3Y);
  }

}

