/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.Tenor.TENOR_10M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_12M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_18M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1D;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1W;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2D;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2W;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3D;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3W;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_4M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_4Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_6W;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static java.time.temporal.ChronoUnit.CENTURIES;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.YEARS;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Tests for the tenor class.
 */
@Test
public class TenorTest {

  @DataProvider(name = "ofPeriod")
  static Object[][] data_ofPeriod() {
    return new Object[][] {
        {Period.ofDays(1), Period.ofDays(1), "1D"},
        {Period.ofDays(7), Period.ofDays(7), "1W"},
        {Period.ofDays(10), Period.ofDays(10), "10D"},
        {Period.ofWeeks(2), Period.ofDays(14), "2W"},
        {Period.ofMonths(1), Period.ofMonths(1), "1M"},
        {Period.ofMonths(2), Period.ofMonths(2), "2M"},
        {Period.ofMonths(12), Period.ofMonths(12), "12M"},
        {Period.ofYears(1), Period.ofYears(1), "1Y"},
        {Period.ofMonths(20), Period.ofMonths(20), "20M"},
        {Period.ofMonths(24), Period.ofMonths(24), "24M"},
        {Period.ofYears(2), Period.ofYears(2), "2Y"},
        {Period.ofMonths(30), Period.ofMonths(30), "30M"},
        {Period.of(2, 6, 0), Period.of(2, 6, 0), "2Y6M"},
    };
  }

  @Test(dataProvider = "ofPeriod")
  public void test_ofPeriod(Period period, Period stored, String str) {
    assertEquals(Tenor.of(period).getPeriod(), stored);
    assertEquals(Tenor.of(period).toString(), str);
  }

  @DataProvider(name = "ofMonths")
  static Object[][] data_ofMonths() {
    return new Object[][] {
        {1, Period.ofMonths(1), "1M"},
        {2, Period.ofMonths(2), "2M"},
        {12, Period.ofMonths(12), "12M"},
        {20, Period.ofMonths(20), "20M"},
        {24, Period.ofMonths(24), "24M"},
        {30, Period.ofMonths(30), "30M"},
    };
  }

  @Test(dataProvider = "ofMonths")
  public void test_ofMonths(int months, Period stored, String str) {
    assertEquals(Tenor.ofMonths(months).getPeriod(), stored);
    assertEquals(Tenor.ofMonths(months).toString(), str);
  }

  @DataProvider(name = "ofYears")
  static Object[][] data_ofYears() {
    return new Object[][] {
        {1, Period.ofYears(1), "1Y"},
        {2, Period.ofYears(2), "2Y"},
        {3, Period.ofYears(3), "3Y"},
    };
  }

  @Test(dataProvider = "ofYears")
  public void test_ofYears(int years, Period stored, String str) {
    assertEquals(Tenor.ofYears(years).getPeriod(), stored);
    assertEquals(Tenor.ofYears(years).toString(), str);
  }

  public void test_of_int() {
    assertEquals(Tenor.ofDays(1), TENOR_1D);
    assertEquals(Tenor.ofDays(7), TENOR_1W);
    assertEquals(Tenor.ofWeeks(2), TENOR_2W);
    assertEquals(Tenor.ofMonths(1), TENOR_1M);
    assertEquals(Tenor.ofYears(1), TENOR_1Y);
  }

  public void test_of_notZero() {
    assertThrowsIllegalArg(() -> Tenor.of(Period.ofDays(0)));
    assertThrowsIllegalArg(() -> Tenor.ofDays(0));
    assertThrowsIllegalArg(() -> Tenor.ofWeeks(0));
    assertThrowsIllegalArg(() -> Tenor.ofMonths(0));
    assertThrowsIllegalArg(() -> Tenor.ofYears(0));
  }

  public void test_of_notNegative() {
    assertThrowsIllegalArg(() -> Tenor.of(Period.ofDays(-1)));
    assertThrowsIllegalArg(() -> Tenor.ofDays(-1));
    assertThrowsIllegalArg(() -> Tenor.ofWeeks(-1));
    assertThrowsIllegalArg(() -> Tenor.ofMonths(-1));
    assertThrowsIllegalArg(() -> Tenor.ofYears(-1));
  }

  //-------------------------------------------------------------------------
  public void test_parse_String_roundTrip() {
    assertEquals(Tenor.parse(TENOR_10M.toString()), TENOR_10M);
  }

  @DataProvider(name = "parseGood")
  static Object[][] data_parseGood() {
    return new Object[][] {
        {"2D", TENOR_2D},
        {"2W", TENOR_2W},
        {"6W", TENOR_6W},
        {"2M", TENOR_2M},
        {"12M", TENOR_12M},
        {"1Y", TENOR_1Y},
        {"2Y", TENOR_2Y},
    };
  }

  @Test(dataProvider = "parseGood")
  public void test_parse_String_good_noP(String input, Tenor expected) {
    assertEquals(Tenor.parse(input), expected);
  }

  @Test(dataProvider = "parseGood")
  public void test_parse_String_good_withP(String input, Tenor expected) {
    assertEquals(Tenor.parse("P" + input), expected);
  }

  @DataProvider(name = "parseBad")
  static Object[][] data_parseBad() {
    return new Object[][] {
        {""},
        {"2"},
        {"2K"},
        {"-2D"},
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
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "normalized")
  static Object[][] data_normalized() {
    return new Object[][] {
        {Period.ofDays(1), Period.ofDays(1)},
        {Period.ofDays(7), Period.ofDays(7)},
        {Period.ofDays(10), Period.ofDays(10)},
        {Period.ofWeeks(2), Period.ofDays(14)},
        {Period.ofMonths(1), Period.ofMonths(1)},
        {Period.ofMonths(2), Period.ofMonths(2)},
        {Period.ofMonths(12), Period.ofMonths(12)},
        {Period.ofYears(1), Period.ofMonths(12)},
        {Period.ofMonths(20), Period.of(1, 8, 0)},
        {Period.ofMonths(24), Period.ofYears(2)},
        {Period.ofYears(2), Period.ofYears(2)},
        {Period.ofMonths(30), Period.of(2, 6, 0)},
    };
  }

  @Test(dataProvider = "normalized")
  public void test_normalized(Period period, Period normalized) {
    assertEquals(Tenor.of(period).normalized().getPeriod(), normalized);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "based")
  static Object[][] data_based() {
    return new Object[][] {
        {Tenor.ofDays(1), false, false},
        {Tenor.ofDays(2), false, false},
        {Tenor.ofDays(6), false, false},
        {Tenor.ofDays(7), true, false},
        {Tenor.ofWeeks(1), true, false},
        {Tenor.ofWeeks(3), true, false},
        {Tenor.ofMonths(1), false, true},
        {Tenor.ofMonths(3), false, true},
        {Tenor.ofYears(1), false, true},
        {Tenor.ofYears(3), false, true},
        {Tenor.of(Period.of(1, 2, 3)), false, false},
    };
  }

  @Test(dataProvider = "based")
  public void test_isWeekBased(Tenor test, boolean weekBased, boolean monthBased) {
    assertEquals(test.isWeekBased(), weekBased);
  }

  @Test(dataProvider = "based")
  public void test_isMonthBased(Tenor test, boolean weekBased, boolean monthBased) {
    assertEquals(test.isMonthBased(), monthBased);
  }

  //-------------------------------------------------------------------------
  public void test_addTo() {
    assertEquals(TENOR_3D.addTo(LocalDate.of(2014, 6, 30)), LocalDate.of(2014, 7, 3));
    assertEquals(TENOR_1W.addTo(
        OffsetDateTime.of(2014, 6, 30, 0, 0, 0, 0, ZoneOffset.UTC)),
        OffsetDateTime.of(2014, 7, 7, 0, 0, 0, 0, ZoneOffset.UTC));
  }

  public void test_subtractFrom() {
    assertEquals(TENOR_3D.subtractFrom(LocalDate.of(2014, 6, 30)), LocalDate.of(2014, 6, 27));
    assertEquals(TENOR_1W.subtractFrom(
        OffsetDateTime.of(2014, 6, 30, 0, 0, 0, 0, ZoneOffset.UTC)),
        OffsetDateTime.of(2014, 6, 23, 0, 0, 0, 0, ZoneOffset.UTC));
  }

  //-------------------------------------------------------------------------
  public void test_temporalAmount() {
    assertEquals(TENOR_3D.getUnits(), ImmutableList.of(YEARS, MONTHS, DAYS));
    assertEquals(TENOR_3D.get(DAYS), 3);
    assertEquals(LocalDate.of(2014, 6, 30).plus(TENOR_1W), LocalDate.of(2014, 7, 7));
    assertEquals(LocalDate.of(2014, 6, 30).minus(TENOR_1W), LocalDate.of(2014, 6, 23));
    assertThrows(() -> TENOR_10M.get(CENTURIES), UnsupportedTemporalTypeException.class);
  }

  //-------------------------------------------------------------------------
  public void test_compare() {
    List<Tenor> tenors = ImmutableList.of(
        Tenor.ofDays(1),
        Tenor.ofDays(3),
        Tenor.ofDays(7),
        Tenor.ofWeeks(2),
        Tenor.ofWeeks(4),
        Tenor.ofDays(30),
        Tenor.ofMonths(1),
        Tenor.ofDays(31),
        Tenor.of(Period.of(0, 1, 1)),
        Tenor.ofDays(60),
        Tenor.ofMonths(2),
        Tenor.ofDays(61),
        Tenor.ofDays(91),
        Tenor.ofMonths(3),
        Tenor.ofDays(92),
        Tenor.ofDays(182),
        Tenor.ofMonths(6),
        Tenor.ofDays(183),
        Tenor.ofDays(365),
        Tenor.ofYears(1),
        Tenor.ofDays(366));

    List<Tenor> test = new ArrayList<>(tenors);
    Collections.shuffle(test);
    Collections.sort(test);
    assertEquals(test, tenors);
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
    assertEquals(TENOR_12M.toString(), "12M");
    assertEquals(TENOR_1Y.toString(), "1Y");
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
