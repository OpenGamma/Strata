/*
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
import static java.time.temporal.ChronoUnit.CENTURIES;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.YEARS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.UnsupportedTemporalTypeException;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests for the frequency class.
 */
public class FrequencyTest {

  @DataProvider(name = "create")
  public static Object[][] data_create() {
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
    assertThat(test.getPeriod()).isEqualTo(period);
    assertThat(test.toString()).isEqualTo(toString);
    assertThat(test.isTerm()).isFalse();
  }

  @Test(dataProvider = "create")
  public void test_of_Period(Frequency test, Period period, String toString) {
    assertThat(Frequency.of(period)).isEqualTo(test);
    assertThat(Frequency.of(period).getPeriod()).isEqualTo(period);
  }

  @Test(dataProvider = "create")
  public void test_parse(Frequency test, Period period, String toString) {
    assertThat(Frequency.parse(toString)).isEqualTo(test);
    assertThat(Frequency.parse(toString).getPeriod()).isEqualTo(period);
  }

  @Test
  public void test_term() {
    assertThat(TERM.getPeriod()).isEqualTo(Period.ofYears(10_000));
    assertThat(TERM.isTerm()).isEqualTo(true);
    assertThat(TERM.toString()).isEqualTo("Term");
    assertThat(Frequency.parse("Term")).isEqualTo(TERM);
    assertThat(Frequency.parse("0T")).isEqualTo(TERM);
    assertThat(Frequency.parse("1T")).isEqualTo(TERM);
    assertThat(Frequency.parse("T")).isEqualTo(TERM);
  }

  @Test
  public void test_of_notZero() {
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.of(Period.ofDays(0)));
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.ofDays(0));
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.ofWeeks(0));
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.ofMonths(0));
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.ofYears(0));
  }

  @Test
  public void test_of_notNegative() {
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.of(Period.ofDays(-1)));
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.of(Period.ofMonths(-1)));
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.of(Period.of(0, -1, -1)));
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.of(Period.of(0, -1, 1)));
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.of(Period.of(0, 1, -1)));
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.ofDays(-1));
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.ofWeeks(-1));
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.ofMonths(-1));
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.ofYears(-1));
  }

  @Test
  public void test_of_tooBig() {
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.of(Period.ofMonths(12001)));
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.of(Period.ofMonths(Integer.MAX_VALUE)));

    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.of(Period.ofYears(1001)));
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.of(Period.ofYears(Integer.MAX_VALUE)));

    assertThatIllegalArgumentException()
        .isThrownBy(() -> Frequency.ofMonths(12001))
        .withMessage("Months must not exceed 12,000");
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.ofMonths(Integer.MAX_VALUE));

    assertThatIllegalArgumentException()
        .isThrownBy(() -> Frequency.ofYears(1001))
        .withMessage("Years must not exceed 1,000");
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.ofYears(Integer.MAX_VALUE));

    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.of(Period.of(10000, 0, 1)));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "ofMonths")
  public static Object[][] data_ofMonths() {
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
    assertThat(Frequency.ofMonths(months).getPeriod()).isEqualTo(normalized);
    assertThat(Frequency.ofMonths(months).toString()).isEqualTo(str);
  }

  @DataProvider(name = "ofYears")
  public static Object[][] data_ofYears() {
    return new Object[][] {
        {1, Period.ofYears(1), "P1Y"},
        {2, Period.ofYears(2), "P2Y"},
        {3, Period.ofYears(3), "P3Y"},
    };
  }

  @Test(dataProvider = "ofYears")
  public void test_ofYears(int years, Period normalized, String str) {
    assertThat(Frequency.ofYears(years).getPeriod()).isEqualTo(normalized);
    assertThat(Frequency.ofYears(years).toString()).isEqualTo(str);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "normalized")
  public static Object[][] data_normalized() {
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
    assertThat(Frequency.of(period).normalized().getPeriod()).isEqualTo(normalized);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "based")
  public static Object[][] data_based() {
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
    assertThat(test.isWeekBased()).isEqualTo(weekBased);
  }

  @Test(dataProvider = "based")
  public void test_isMonthBased(Frequency test, boolean weekBased, boolean monthBased, boolean annual) {
    assertThat(test.isMonthBased()).isEqualTo(monthBased);
  }

  @Test(dataProvider = "based")
  public void test_isAnnual(Frequency test, boolean weekBased, boolean monthBased, boolean annual) {
    assertThat(test.isAnnual()).isEqualTo(annual);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "events")
  public static Object[][] data_events() {
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
    assertThat(test.eventsPerYear()).isEqualTo(expected);
  }

  @Test
  public void test_eventsPerYear_bad() {
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.ofDays(3).eventsPerYear());
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.ofWeeks(3).eventsPerYear());
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.ofWeeks(104).eventsPerYear());
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.ofMonths(5).eventsPerYear());
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.ofMonths(24).eventsPerYear());
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.of(Period.of(2, 2, 2)).eventsPerYear());
  }

  @Test(dataProvider = "events")
  public void test_eventsPerYearEstimate(Frequency test, int expected) {
    assertThat(test.eventsPerYearEstimate()).isEqualTo(expected, within(1e-8));
  }

  @Test
  public void test_eventsPerYearEstimate_bad() {
    assertThat(Frequency.ofDays(3).eventsPerYearEstimate()).isEqualTo(364d / 3, within(1e-8));
    assertThat(Frequency.ofWeeks(3).eventsPerYearEstimate()).isEqualTo(364d / 21, within(1e-8));
    assertThat(Frequency.ofWeeks(104).eventsPerYearEstimate()).isEqualTo(364d / 728, within(1e-8));
    assertThat(Frequency.ofMonths(5).eventsPerYearEstimate()).isEqualTo(12d / 5, within(1e-8));
    assertThat(Frequency.ofMonths(22).eventsPerYearEstimate()).isEqualTo(12d / 22, within(1e-8));
    assertThat(Frequency.ofMonths(24).eventsPerYearEstimate()).isEqualTo(12d / 24, within(1e-8));
    assertThat(Frequency.ofYears(2).eventsPerYearEstimate()).isEqualTo(0.5d, within(1e-8));
    assertThat(Frequency.of(Period.of(10, 0, 1)).eventsPerYearEstimate()).isEqualTo(0.1d, within(1e-3));
    assertThat(Frequency.of(Period.of(5, 0, 95)).eventsPerYearEstimate()).isEqualTo(0.19d, within(1e-3));
    assertThat(Frequency.of(Period.of(5, 0, 97)).eventsPerYearEstimate()).isEqualTo(0.19d, within(1e-3));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "exactDivide")
  public static Object[][] data_exactDivide() {
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
    assertThat(test.exactDivide(other)).isEqualTo(expected);
  }

  @Test(dataProvider = "exactDivide")
  public void test_exactDivide_reverse(Frequency test, Frequency other, int expected) {
    if (!test.equals(other)) {
      assertThatIllegalArgumentException().isThrownBy(() -> other.exactDivide(test));
    }
  }

  @Test
  public void test_exactDivide_bad() {
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.ofDays(5).exactDivide(Frequency.ofDays(2)));
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.ofMonths(5).exactDivide(Frequency.ofMonths(2)));
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.P1M.exactDivide(Frequency.P1W));
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.P1W.exactDivide(Frequency.P1M));
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.TERM.exactDivide(Frequency.P1W));
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.P12M.exactDivide(Frequency.TERM));
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.ofYears(1).exactDivide(Frequency.P1W));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parse_String_roundTrip() {
    assertThat(Frequency.parse(P6M.toString())).isEqualTo(P6M);
  }

  @DataProvider(name = "parseGood")
  public static Object[][] data_parseGood() {
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
    assertThat(Frequency.parse(input)).isEqualTo(expected);
  }

  @Test(dataProvider = "parseGood")
  public void test_parse_String_good_withP(String input, Frequency expected) {
    assertThat(Frequency.parse("P" + input)).isEqualTo(expected);
  }

  @Test
  public void test_parse_String_term() {
    assertThat(Frequency.parse("Term")).isEqualTo(Frequency.TERM);
    assertThat(Frequency.parse("TERM")).isEqualTo(Frequency.TERM);
  }

  @DataProvider(name = "parseBad")
  public static Object[][] data_parseBad() {
    return new Object[][] {
        {""},
        {"2"},
        {"2K"},
        {"-2D"},
        {"PTerm"},
        {null},
    };
  }

  @Test(dataProvider = "parseBad")
  public void test_parse_String_bad(String input) {
    assertThatIllegalArgumentException().isThrownBy(() -> Frequency.parse(input));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_addTo() {
    assertThat(P1D.addTo(LocalDate.of(2014, 6, 30))).isEqualTo(LocalDate.of(2014, 7, 1));
    assertThat(P1W.addTo(OffsetDateTime.of(2014, 6, 30, 0, 0, 0, 0, ZoneOffset.UTC)))
        .isEqualTo(OffsetDateTime.of(2014, 7, 7, 0, 0, 0, 0, ZoneOffset.UTC));
  }

  @Test
  public void test_subtractFrom() {
    assertThat(P1D.subtractFrom(LocalDate.of(2014, 6, 30))).isEqualTo(LocalDate.of(2014, 6, 29));
    assertThat(P1W.subtractFrom(OffsetDateTime.of(2014, 6, 30, 0, 0, 0, 0, ZoneOffset.UTC)))
        .isEqualTo(OffsetDateTime.of(2014, 6, 23, 0, 0, 0, 0, ZoneOffset.UTC));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_temporalAmount() {
    assertThat(P3M.getUnits()).containsExactly(YEARS, MONTHS, DAYS);
    assertThat(P3M.get(MONTHS)).isEqualTo(3);
    assertThat(LocalDate.of(2014, 6, 30).plus(P1W)).isEqualTo(LocalDate.of(2014, 7, 7));
    assertThat(LocalDate.of(2014, 6, 30).minus(P1W)).isEqualTo(LocalDate.of(2014, 6, 23));
    assertThatExceptionOfType(UnsupportedTemporalTypeException.class).isThrownBy(() -> P3M.get(CENTURIES));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equals_hashCode() {
    Frequency a1 = P1D;
    Frequency a2 = Frequency.ofDays(1);
    Frequency b = P3M;
    assertThat(a1)
        .isEqualTo(a1)
        .isEqualTo(a2)
        .isNotEqualTo(b)
        .isNotEqualTo("")
        .isNotEqualTo(null)
        .hasSameHashCodeAs(a2);
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_serialization() {
    assertSerialization(P1D);
    assertSerialization(P3M);
    assertSerialization(P12M);
    assertSerialization(TERM);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(Frequency.class, P1D);
    assertJodaConvert(Frequency.class, P3M);
    assertJodaConvert(Frequency.class, P12M);
    assertJodaConvert(Frequency.class, TERM);
  }

}
