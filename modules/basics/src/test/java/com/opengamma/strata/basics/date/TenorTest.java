/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.Tenor.TENOR_10M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_12M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_15M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_18M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1D;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1W;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_21M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2D;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2W;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_35Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3D;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3W;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_40Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_45Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_4M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_4Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_50Y;
import static com.opengamma.strata.basics.date.Tenor.TENOR_6W;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static java.time.temporal.ChronoUnit.CENTURIES;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.MONTHS;
import static java.time.temporal.ChronoUnit.YEARS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableList;

/**
 * Tests for the tenor class.
 */
public class TenorTest {

  private static final Object ANOTHER_TYPE = "";

  public static Object[][] data_ofPeriod() {
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

  @ParameterizedTest
  @MethodSource("data_ofPeriod")
  public void test_ofPeriod(Period period, Period stored, String str) {
    assertThat(Tenor.of(period).getPeriod()).isEqualTo(stored);
    assertThat(Tenor.of(period).toString()).isEqualTo(str);
  }

  public static Object[][] data_ofMonths() {
    return new Object[][] {
        {1, Period.ofMonths(1), "1M"},
        {2, Period.ofMonths(2), "2M"},
        {12, Period.ofMonths(12), "12M"},
        {20, Period.ofMonths(20), "20M"},
        {24, Period.ofMonths(24), "24M"},
        {30, Period.ofMonths(30), "30M"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_ofMonths")
  public void test_ofMonths(int months, Period stored, String str) {
    assertThat(Tenor.ofMonths(months).getPeriod()).isEqualTo(stored);
    assertThat(Tenor.ofMonths(months).toString()).isEqualTo(str);
  }

  public static Object[][] data_ofYears() {
    return new Object[][] {
        {1, Period.ofYears(1), "1Y"},
        {2, Period.ofYears(2), "2Y"},
        {3, Period.ofYears(3), "3Y"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_ofYears")
  public void test_ofYears(int years, Period stored, String str) {
    assertThat(Tenor.ofYears(years).getPeriod()).isEqualTo(stored);
    assertThat(Tenor.ofYears(years).toString()).isEqualTo(str);
  }

  @Test
  public void test_of_int() {
    assertThat(Tenor.ofDays(1)).isEqualTo(TENOR_1D);
    assertThat(Tenor.ofDays(7)).isEqualTo(TENOR_1W);
    assertThat(Tenor.ofWeeks(2)).isEqualTo(TENOR_2W);
    assertThat(Tenor.ofMonths(1)).isEqualTo(TENOR_1M);
    assertThat(Tenor.ofMonths(15)).isEqualTo(TENOR_15M);
    assertThat(Tenor.ofMonths(18)).isEqualTo(TENOR_18M);
    assertThat(Tenor.ofMonths(21)).isEqualTo(TENOR_21M);
    assertThat(Tenor.ofYears(1)).isEqualTo(TENOR_1Y);
    assertThat(Tenor.ofYears(35)).isEqualTo(TENOR_35Y);
    assertThat(Tenor.ofYears(40)).isEqualTo(TENOR_40Y);
    assertThat(Tenor.ofYears(45)).isEqualTo(TENOR_45Y);
    assertThat(Tenor.ofYears(50)).isEqualTo(TENOR_50Y);
  }

  @Test
  public void test_of_notZero() {
    assertThatIllegalArgumentException().isThrownBy(() -> Tenor.of(Period.ofDays(0)));
    assertThatIllegalArgumentException().isThrownBy(() -> Tenor.ofDays(0));
    assertThatIllegalArgumentException().isThrownBy(() -> Tenor.ofWeeks(0));
    assertThatIllegalArgumentException().isThrownBy(() -> Tenor.ofMonths(0));
    assertThatIllegalArgumentException().isThrownBy(() -> Tenor.ofYears(0));
  }

  @Test
  public void test_of_notNegative() {
    assertThatIllegalArgumentException().isThrownBy(() -> Tenor.of(Period.ofDays(-1)));
    assertThatIllegalArgumentException().isThrownBy(() -> Tenor.ofDays(-1));
    assertThatIllegalArgumentException().isThrownBy(() -> Tenor.ofWeeks(-1));
    assertThatIllegalArgumentException().isThrownBy(() -> Tenor.ofMonths(-1));
    assertThatIllegalArgumentException().isThrownBy(() -> Tenor.ofYears(-1));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_parse_String_roundTrip() {
    assertThat(Tenor.parse(TENOR_10M.toString())).isEqualTo(TENOR_10M);
  }

  public static Object[][] data_parseGood() {
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

  @ParameterizedTest
  @MethodSource("data_parseGood")
  public void test_parse_String_good_noP(String input, Tenor expected) {
    assertThat(Tenor.parse(input)).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("data_parseGood")
  public void test_parse_String_good_withP(String input, Tenor expected) {
    assertThat(Tenor.parse("P" + input)).isEqualTo(expected);
  }

  public static Object[][] data_parseBad() {
    return new Object[][] {
        {""},
        {"2"},
        {"2K"},
        {"-2D"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_parseBad")
  public void test_parse_String_bad(String input) {
    assertThatIllegalArgumentException().isThrownBy(() -> Tenor.parse(input));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_getPeriod() {
    assertThat(TENOR_3D.getPeriod()).isEqualTo(Period.ofDays(3));
    assertThat(TENOR_3W.getPeriod()).isEqualTo(Period.ofDays(21));
    assertThat(TENOR_3M.getPeriod()).isEqualTo(Period.ofMonths(3));
    assertThat(TENOR_3Y.getPeriod()).isEqualTo(Period.ofYears(3));
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_normalized() {
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

  @ParameterizedTest
  @MethodSource("data_normalized")
  public void test_normalized(Period period, Period normalized) {
    assertThat(Tenor.of(period).normalized().getPeriod()).isEqualTo(normalized);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_based() {
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

  @ParameterizedTest
  @MethodSource("data_based")
  public void test_isWeekBased(Tenor test, boolean weekBased, boolean monthBased) {
    assertThat(test.isWeekBased()).isEqualTo(weekBased);
  }

  @ParameterizedTest
  @MethodSource("data_based")
  public void test_isMonthBased(Tenor test, boolean weekBased, boolean monthBased) {
    assertThat(test.isMonthBased()).isEqualTo(monthBased);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_addTo() {
    assertThat(TENOR_3D.addTo(LocalDate.of(2014, 6, 30))).isEqualTo(LocalDate.of(2014, 7, 3));
    assertThat(TENOR_1W.addTo(OffsetDateTime.of(2014, 6, 30, 0, 0, 0, 0, ZoneOffset.UTC)))
        .isEqualTo(OffsetDateTime.of(2014, 7, 7, 0, 0, 0, 0, ZoneOffset.UTC));
  }

  @Test
  public void test_subtractFrom() {
    assertThat(TENOR_3D.subtractFrom(LocalDate.of(2014, 6, 30))).isEqualTo(LocalDate.of(2014, 6, 27));
    assertThat(TENOR_1W.subtractFrom(OffsetDateTime.of(2014, 6, 30, 0, 0, 0, 0, ZoneOffset.UTC)))
        .isEqualTo(OffsetDateTime.of(2014, 6, 23, 0, 0, 0, 0, ZoneOffset.UTC));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_temporalAmount() {
    assertThat(TENOR_3D.getUnits()).containsExactly(YEARS, MONTHS, DAYS);
    assertThat(TENOR_3D.get(DAYS)).isEqualTo(3);
    assertThat(LocalDate.of(2014, 6, 30).plus(TENOR_1W)).isEqualTo(LocalDate.of(2014, 7, 7));
    assertThat(LocalDate.of(2014, 6, 30).minus(TENOR_1W)).isEqualTo(LocalDate.of(2014, 6, 23));
    assertThatExceptionOfType(UnsupportedTemporalTypeException.class).isThrownBy(() -> TENOR_10M.get(CENTURIES));
  }

  //-------------------------------------------------------------------------
  @Test
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
        Tenor.ofDays(366),
        Tenor.ofDays(730),
        Tenor.ofYears(2),
        Tenor.ofDays(731),
        Tenor.ofDays(1095),
        Tenor.ofYears(3),
        Tenor.ofDays(1096),
        Tenor.ofDays(1460),
        Tenor.ofYears(4),
        Tenor.ofDays(1461));

    List<Tenor> test = new ArrayList<>(tenors);
    Collections.shuffle(test);
    Collections.sort(test);
    assertThat(test).isEqualTo(tenors);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equals_hashCode() {
    Tenor a1 = TENOR_3D;
    Tenor a2 = Tenor.ofDays(3);
    Tenor b = TENOR_4M;
    assertThat(a1.equals(a1)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(a2)).isEqualTo(true);

    assertThat(a2.equals(a1)).isEqualTo(true);
    assertThat(a2.equals(a2)).isEqualTo(true);
    assertThat(a2.equals(b)).isEqualTo(false);

    assertThat(b.equals(a1)).isEqualTo(false);
    assertThat(b.equals(a2)).isEqualTo(false);
    assertThat(b.equals(b)).isEqualTo(true);

    assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
  }

  @Test
  public void test_equals_bad() {
    assertThat(TENOR_3D.equals(null)).isEqualTo(false);
    assertThat(TENOR_3D.equals(ANOTHER_TYPE)).isEqualTo(false);
    assertThat(TENOR_3D.equals(new Object())).isEqualTo(false);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toString() {
    assertThat(TENOR_3D.toString()).isEqualTo("3D");
    assertThat(TENOR_2W.toString()).isEqualTo("2W");
    assertThat(TENOR_4M.toString()).isEqualTo("4M");
    assertThat(TENOR_12M.toString()).isEqualTo("12M");
    assertThat(TENOR_1Y.toString()).isEqualTo("1Y");
    assertThat(TENOR_18M.toString()).isEqualTo("18M");
    assertThat(TENOR_4Y.toString()).isEqualTo("4Y");
  }

  //-----------------------------------------------------------------------
  @Test
  public void test_serialization() {
    assertSerialization(TENOR_3D);
    assertSerialization(TENOR_4M);
    assertSerialization(TENOR_3Y);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(Tenor.class, TENOR_3D);
    assertJodaConvert(Tenor.class, TENOR_4M);
    assertJodaConvert(Tenor.class, TENOR_3Y);
  }

}
