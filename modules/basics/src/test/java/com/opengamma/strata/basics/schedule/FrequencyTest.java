/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.basics.schedule.Frequency.P1D;
import static com.opengamma.strata.basics.schedule.Frequency.P1W;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.basics.schedule.Frequency.TERM;
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

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Tests for the frequency class.
 */
@Test
public class FrequencyTest {

  @DataProvider(name = "create")
  static Object[][] data_create() {
    return new Object[][] {
        {Frequency.ofDays(1), Period.ofDays(1), "P1D"},
        {Frequency.ofDays(2), Period.ofDays(2), "P2D"},
        {Frequency.ofDays(6), Period.ofDays(6), "P6D"},
        {Frequency.ofDays(7), Period.ofDays(7), "P1W"},
        {Frequency.ofDays(91), Period.ofDays(91), "P13W"},
        {Frequency.ofWeeks(1), Period.ofDays(7), "P1W"},
        {Frequency.ofWeeks(3), Period.ofDays(21), "P3W"},
        {Frequency.ofMonths(8), Period.ofMonths(8), "P8M"},
        {Frequency.ofMonths(12), Period.ofMonths(12), "P12M"},
        {Frequency.ofMonths(18), Period.ofMonths(18), "P18M"},
        {Frequency.ofMonths(24), Period.ofMonths(24), "P24M"},
        {Frequency.ofMonths(30), Period.ofMonths(30), "P30M"},
        {Frequency.ofYears(1), Period.ofYears(1), "P1Y"},
        {Frequency.ofYears(2), Period.ofYears(2), "P2Y"},
        {Frequency.of(Period.of(1, 2, 3)), Period.of(1, 2, 3), "P1Y2M3D"},
        {Frequency.P1D, Period.ofDays(1), "P1D"},
        {Frequency.P1W, Period.ofWeeks(1), "P1W"},
        {Frequency.P2W, Period.ofWeeks(2), "P2W"},
        {Frequency.P4W, Period.ofWeeks(4), "P4W"},
        {Frequency.P13W, Period.ofWeeks(13), "P13W"},
        {Frequency.P26W, Period.ofWeeks(26), "P26W"},
        {Frequency.P52W, Period.ofWeeks(52), "P52W"},
        {Frequency.P1M, Period.ofMonths(1), "P1M"},
        {Frequency.P2M, Period.ofMonths(2), "P2M"},
        {Frequency.P3M, Period.ofMonths(3), "P3M"},
        {Frequency.P4M, Period.ofMonths(4), "P4M"},
        {Frequency.P6M, Period.ofMonths(6), "P6M"},
        {Frequency.P12M, Period.ofMonths(12), "P12M"},
    };
  }

  @Test(dataProvider = "create")
  public void test_of_int(Frequency test, Period period, String toString) {
    assertEquals(test.getPeriod(), period);
    assertEquals(test.toString(), toString);
    assertEquals(test.isTerm(), false);
  }

  @Test(dataProvider = "create")
  public void test_of_Period(Frequency test, Period period, String toString) {
    assertEquals(Frequency.of(period), test);
    assertEquals(Frequency.of(period).getPeriod(), period);
  }

  @Test(dataProvider = "create")
  public void test_parse(Frequency test, Period period, String toString) {
    assertEquals(Frequency.parse(toString), test);
    assertEquals(Frequency.parse(toString).getPeriod(), period);
  }

  public void test_term() {
    assertEquals(TERM.getPeriod(), Period.ofYears(10_000));
    assertEquals(TERM.isTerm(), true);
    assertEquals(TERM.toString(), "Term");
    assertEquals(Frequency.parse("Term"), TERM);
    assertEquals(Frequency.parse("0T"), TERM);
    assertEquals(Frequency.parse("1T"), TERM);
  }

  public void test_of_notZero() {
    assertThrowsIllegalArg(() -> Frequency.of(Period.ofDays(0)));
    assertThrowsIllegalArg(() -> Frequency.ofDays(0));
    assertThrowsIllegalArg(() -> Frequency.ofWeeks(0));
    assertThrowsIllegalArg(() -> Frequency.ofMonths(0));
    assertThrowsIllegalArg(() -> Frequency.ofYears(0));
  }

  public void test_of_notNegative() {
    assertThrowsIllegalArg(() -> Frequency.of(Period.ofDays(-1)));
    assertThrowsIllegalArg(() -> Frequency.of(Period.ofMonths(-1)));
    assertThrowsIllegalArg(() -> Frequency.of(Period.of(0, -1, -1)));
    assertThrowsIllegalArg(() -> Frequency.of(Period.of(0, -1, 1)));
    assertThrowsIllegalArg(() -> Frequency.of(Period.of(0, 1, -1)));
    assertThrowsIllegalArg(() -> Frequency.ofDays(-1));
    assertThrowsIllegalArg(() -> Frequency.ofWeeks(-1));
    assertThrowsIllegalArg(() -> Frequency.ofMonths(-1));
    assertThrowsIllegalArg(() -> Frequency.ofYears(-1));
  }

  public void test_of_tooBig() {
    assertThrowsIllegalArg(() -> Frequency.of(Period.ofMonths(12001)));
    assertThrowsIllegalArg(() -> Frequency.of(Period.ofMonths(Integer.MAX_VALUE)));

    assertThrowsIllegalArg(() -> Frequency.of(Period.ofYears(1001)));
    assertThrowsIllegalArg(() -> Frequency.of(Period.ofYears(Integer.MAX_VALUE)));

    assertThrowsIllegalArg(() -> Frequency.ofMonths(12001), "Months must not exceed 12,000");
    assertThrowsIllegalArg(() -> Frequency.ofMonths(Integer.MAX_VALUE));

    assertThrowsIllegalArg(() -> Frequency.ofYears(1001), "Years must not exceed 1,000");
    assertThrowsIllegalArg(() -> Frequency.ofYears(Integer.MAX_VALUE));

    assertThrowsIllegalArg(() -> Frequency.of(Period.of(10000, 0, 1)));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "ofMonths")
  static Object[][] data_ofMonths() {
    return new Object[][] {
        {1, Period.ofMonths(1), "P1M"},
        {2, Period.ofMonths(2), "P2M"},
        {3, Period.ofMonths(3), "P3M"},
        {4, Period.ofMonths(4), "P4M"},
        {6, Period.ofMonths(6), "P6M"},
        {12, Period.ofMonths(12), "P12M"},
        {20, Period.ofMonths(20), "P20M"},
        {24, Period.ofMonths(24), "P24M"},
        {30, Period.ofMonths(30), "P30M"},
    };
  }

  @Test(dataProvider = "ofMonths")
  public void test_ofMonths(int months, Period normalized, String str) {
    assertEquals(Frequency.ofMonths(months).getPeriod(), normalized);
    assertEquals(Frequency.ofMonths(months).toString(), str);
  }

  @DataProvider(name = "ofYears")
  static Object[][] data_ofYears() {
    return new Object[][] {
        {1, Period.ofYears(1), "P1Y"},
        {2, Period.ofYears(2), "P2Y"},
        {3, Period.ofYears(3), "P3Y"},
    };
  }

  @Test(dataProvider = "ofYears")
  public void test_ofYears(int years, Period normalized, String str) {
    assertEquals(Frequency.ofYears(years).getPeriod(), normalized);
    assertEquals(Frequency.ofYears(years).toString(), str);
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
        {Period.ofMonths(12), Period.ofYears(1)},
        {Period.ofYears(1), Period.ofYears(1)},
        {Period.ofMonths(20), Period.of(1, 8, 0)},
        {Period.ofMonths(24), Period.ofYears(2)},
        {Period.ofYears(2), Period.ofYears(2)},
        {Period.ofMonths(30), Period.of(2, 6, 0)},
    };
  }

  @Test(dataProvider = "normalized")
  public void test_normalized(Period period, Period normalized) {
    assertEquals(Frequency.of(period).normalized().getPeriod(), normalized);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "based")
  static Object[][] data_based() {
    return new Object[][] {
        {Frequency.ofDays(1), false, false, false},
        {Frequency.ofDays(2), false, false, false},
        {Frequency.ofDays(6), false, false, false},
        {Frequency.ofDays(7), true, false, false},
        {Frequency.ofWeeks(1), true, false, false},
        {Frequency.ofWeeks(3), true, false, false},
        {Frequency.ofMonths(1), false, true, false},
        {Frequency.ofMonths(3), false, true, false},
        {Frequency.ofMonths(12), false, true, true},
        {Frequency.ofYears(1), false, true, true},
        {Frequency.ofYears(3), false, true, false},
        {Frequency.of(Period.of(1, 2, 3)), false, false, false},
        {Frequency.TERM, false, false, false},
    };
  }

  @Test(dataProvider = "based")
  public void test_isWeekBased(Frequency test, boolean weekBased, boolean monthBased, boolean annual) {
    assertEquals(test.isWeekBased(), weekBased);
  }

  @Test(dataProvider = "based")
  public void test_isMonthBased(Frequency test, boolean weekBased, boolean monthBased, boolean annual) {
    assertEquals(test.isMonthBased(), monthBased);
  }

  @Test(dataProvider = "based")
  public void test_isAnnual(Frequency test, boolean weekBased, boolean monthBased, boolean annual) {
    assertEquals(test.isAnnual(), annual);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "events")
  static Object[][] data_events() {
    return new Object[][] {
        {Frequency.P1D, 364},
        {Frequency.P1W, 52},
        {Frequency.P2W, 26},
        {Frequency.P4W, 13},
        {Frequency.P13W, 4},
        {Frequency.P26W, 2},
        {Frequency.P52W, 1},
        {Frequency.P1M, 12},
        {Frequency.P2M, 6},
        {Frequency.P3M, 4},
        {Frequency.P4M, 3},
        {Frequency.P6M, 2},
        {Frequency.P12M, 1},
        {Frequency.TERM, 0},
    };
  }

  @Test(dataProvider = "events")
  public void test_eventsPerYear(Frequency test, int expected) {
    assertEquals(test.eventsPerYear(), expected);
  }

  public void test_eventsPerYear_bad() {
    assertThrowsIllegalArg(() -> Frequency.ofDays(3).eventsPerYear());
    assertThrowsIllegalArg(() -> Frequency.ofWeeks(3).eventsPerYear());
    assertThrowsIllegalArg(() -> Frequency.ofWeeks(104).eventsPerYear());
    assertThrowsIllegalArg(() -> Frequency.ofMonths(5).eventsPerYear());
    assertThrowsIllegalArg(() -> Frequency.ofMonths(24).eventsPerYear());
    assertThrowsIllegalArg(() -> Frequency.of(Period.of(2, 2, 2)).eventsPerYear());
  }

  @Test(dataProvider = "events")
  public void test_eventsPerYearEstimate(Frequency test, int expected) {
    assertEquals(test.eventsPerYearEstimate(), expected, 1e-8);
  }

  public void test_eventsPerYearEstimate_bad() {
    assertEquals(Frequency.ofDays(3).eventsPerYearEstimate(), 364d / 3, 1e-8);
    assertEquals(Frequency.ofWeeks(3).eventsPerYearEstimate(), 364d / 21, 1e-8);
    assertEquals(Frequency.ofWeeks(104).eventsPerYearEstimate(), 364d / 728, 1e-8);
    assertEquals(Frequency.ofMonths(5).eventsPerYearEstimate(), 12d / 5, 1e-8);
    assertEquals(Frequency.ofMonths(22).eventsPerYearEstimate(), 12d / 22, 1e-8);
    assertEquals(Frequency.ofMonths(24).eventsPerYearEstimate(), 12d / 24, 1e-8);
    assertEquals(Frequency.ofYears(2).eventsPerYearEstimate(), 0.5d, 1e-8);
    assertEquals(Frequency.of(Period.of(10, 0, 1)).eventsPerYearEstimate(), 0.1d, 1e-3);
    assertEquals(Frequency.of(Period.of(5, 0, 95)).eventsPerYearEstimate(), 0.19d, 1e-3);
    assertEquals(Frequency.of(Period.of(5, 0, 97)).eventsPerYearEstimate(), 0.19d, 1e-3);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "exactDivide")
  static Object[][] data_exactDivide() {
    return new Object[][] {
        {Frequency.P1D, Frequency.P1D, 1},
        {Frequency.P1W, Frequency.P1D, 7},
        {Frequency.P2W, Frequency.P1D, 14},

        {Frequency.P1W, Frequency.P1W, 1},
        {Frequency.P2W, Frequency.P1W, 2},
        {Frequency.ofWeeks(3), Frequency.P1W, 3},
        {Frequency.P4W, Frequency.P1W, 4},
        {Frequency.P13W, Frequency.P1W, 13},
        {Frequency.P26W, Frequency.P1W, 26},
        {Frequency.P26W, Frequency.P2W, 13},
        {Frequency.P52W, Frequency.P1W, 52},
        {Frequency.P52W, Frequency.P2W, 26},

        {Frequency.P1M, Frequency.P1M, 1},
        {Frequency.P2M, Frequency.P1M, 2},
        {Frequency.P3M, Frequency.P1M, 3},
        {Frequency.P4M, Frequency.P1M, 4},
        {Frequency.P6M, Frequency.P1M, 6},
        {Frequency.P6M, Frequency.P2M, 3},
        {Frequency.P12M, Frequency.P1M, 12},
        {Frequency.P12M, Frequency.P2M, 6},
        {Frequency.ofYears(1), Frequency.P6M, 2},
        {Frequency.ofYears(1), Frequency.P3M, 4},
        {Frequency.ofYears(2), Frequency.P6M, 4},
    };
  }

  @Test(dataProvider = "exactDivide")
  public void test_exactDivide(Frequency test, Frequency other, int expected) {
    assertEquals(test.exactDivide(other), expected);
  }

  @Test(dataProvider = "exactDivide")
  public void test_exactDivide_reverse(Frequency test, Frequency other, int expected) {
    if (!test.equals(other)) {
      assertThrowsIllegalArg(() -> other.exactDivide(test));
    }
  }

  public void test_exactDivide_bad() {
    assertThrowsIllegalArg(() -> Frequency.ofDays(5).exactDivide(Frequency.ofDays(2)));
    assertThrowsIllegalArg(() -> Frequency.ofMonths(5).exactDivide(Frequency.ofMonths(2)));
    assertThrowsIllegalArg(() -> Frequency.P1M.exactDivide(Frequency.P1W));
    assertThrowsIllegalArg(() -> Frequency.P1W.exactDivide(Frequency.P1M));
    assertThrowsIllegalArg(() -> Frequency.TERM.exactDivide(Frequency.P1W));
    assertThrowsIllegalArg(() -> Frequency.P12M.exactDivide(Frequency.TERM));
    assertThrowsIllegalArg(() -> Frequency.ofYears(1).exactDivide(Frequency.P1W));
  }

  //-------------------------------------------------------------------------
  public void test_parse_String_roundTrip() {
    assertEquals(Frequency.parse(P6M.toString()), P6M);
  }

  @DataProvider(name = "parseGood")
  static Object[][] data_parseGood() {
    return new Object[][] {
        {"1D", Frequency.ofDays(1)},
        {"2D", Frequency.ofDays(2)},
        {"91D", Frequency.ofDays(91)},
        {"2W", Frequency.ofWeeks(2)},
        {"6W", Frequency.ofWeeks(6)},
        {"2M", Frequency.ofMonths(2)},
        {"12M", Frequency.ofMonths(12)},
        {"1Y", Frequency.ofYears(1)},
    };
  }

  @Test(dataProvider = "parseGood")
  public void test_parse_String_good_noP(String input, Frequency expected) {
    assertEquals(Frequency.parse(input), expected);
  }

  @Test(dataProvider = "parseGood")
  public void test_parse_String_good_withP(String input, Frequency expected) {
    assertEquals(Frequency.parse("P" + input), expected);
  }

  public void test_parse_String_term() {
    assertEquals(Frequency.parse("Term"), Frequency.TERM);
    assertEquals(Frequency.parse("TERM"), Frequency.TERM);
  }

  @DataProvider(name = "parseBad")
  static Object[][] data_parseBad() {
    return new Object[][] {
        {""},
        {"2"},
        {"2K"},
        {"-2D"},
        {"PTerm"},
        {null},
    };
  }

  @Test(dataProvider = "parseBad", expectedExceptions = IllegalArgumentException.class)
  public void test_parse_String_bad(String input) {
    Frequency.parse(input);
  }

  //-------------------------------------------------------------------------
  public void test_addTo() {
    assertEquals(P1D.addTo(LocalDate.of(2014, 6, 30)), LocalDate.of(2014, 7, 1));
    assertEquals(P1W.addTo(
        OffsetDateTime.of(2014, 6, 30, 0, 0, 0, 0, ZoneOffset.UTC)),
        OffsetDateTime.of(2014, 7, 7, 0, 0, 0, 0, ZoneOffset.UTC));
  }

  public void test_subtractFrom() {
    assertEquals(P1D.subtractFrom(LocalDate.of(2014, 6, 30)), LocalDate.of(2014, 6, 29));
    assertEquals(P1W.subtractFrom(
        OffsetDateTime.of(2014, 6, 30, 0, 0, 0, 0, ZoneOffset.UTC)),
        OffsetDateTime.of(2014, 6, 23, 0, 0, 0, 0, ZoneOffset.UTC));
  }

  //-------------------------------------------------------------------------
  public void test_temporalAmount() {
    assertEquals(P3M.getUnits(), ImmutableList.of(YEARS, MONTHS, DAYS));
    assertEquals(P3M.get(MONTHS), 3);
    assertEquals(LocalDate.of(2014, 6, 30).plus(P1W), LocalDate.of(2014, 7, 7));
    assertEquals(LocalDate.of(2014, 6, 30).minus(P1W), LocalDate.of(2014, 6, 23));
    assertThrows(() -> P3M.get(CENTURIES), UnsupportedTemporalTypeException.class);
  }

  //-------------------------------------------------------------------------
  public void test_equals_hashCode() {
    Frequency a1 = P1D;
    Frequency a2 = Frequency.ofDays(1);
    Frequency b = P3M;
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
    assertEquals(P3M.equals(null), false);
    assertEquals(P3M.equals("String"), false);
    assertEquals(P3M.equals(new Object()), false);
  }

  //-----------------------------------------------------------------------
  public void test_serialization() {
    assertSerialization(P1D);
    assertSerialization(P3M);
    assertSerialization(P12M);
    assertSerialization(TERM);
  }

  public void test_jodaConvert() {
    assertJodaConvert(Frequency.class, P1D);
    assertJodaConvert(Frequency.class, P3M);
    assertJodaConvert(Frequency.class, P12M);
    assertJodaConvert(Frequency.class, TERM);
  }

}
