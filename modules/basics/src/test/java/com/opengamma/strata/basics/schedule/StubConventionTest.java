/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import static com.opengamma.strata.basics.schedule.Frequency.P1M;
import static com.opengamma.strata.basics.schedule.Frequency.P2W;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.basics.schedule.Frequency.TERM;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_14;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_16;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_30;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_SAT;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_TUE;
import static com.opengamma.strata.basics.schedule.RollConventions.EOM;
import static com.opengamma.strata.basics.schedule.StubConvention.BOTH;
import static com.opengamma.strata.basics.schedule.StubConvention.LONG_FINAL;
import static com.opengamma.strata.basics.schedule.StubConvention.LONG_INITIAL;
import static com.opengamma.strata.basics.schedule.StubConvention.NONE;
import static com.opengamma.strata.basics.schedule.StubConvention.SHORT_FINAL;
import static com.opengamma.strata.basics.schedule.StubConvention.SHORT_INITIAL;
import static com.opengamma.strata.basics.schedule.StubConvention.SMART_FINAL;
import static com.opengamma.strata.basics.schedule.StubConvention.SMART_INITIAL;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.collect.TestHelper.date;
import static java.time.Month.APRIL;
import static java.time.Month.AUGUST;
import static java.time.Month.FEBRUARY;
import static java.time.Month.JANUARY;
import static java.time.Month.JULY;
import static java.time.Month.JUNE;
import static java.time.Month.MARCH;
import static java.time.Month.OCTOBER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test {@link StubConvention}.
 */
public class StubConventionTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_types() {
    StubConvention[] conv = StubConvention.values();
    Object[][] result = new Object[conv.length][];
    for (int i = 0; i < conv.length; i++) {
      result[i] = new Object[] {conv[i]};
    }
    return result;
  }

  @ParameterizedTest
  @MethodSource("data_types")
  public void test_null(StubConvention type) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> type.toRollConvention(null, date(2014, JULY, 1), Frequency.P3M, true));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> type.toRollConvention(date(2014, JULY, 1), null, Frequency.P3M, true));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> type.toRollConvention(date(2014, JULY, 1), date(2014, OCTOBER, 1), null, true));
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_roll() {
    return new Object[][] {
        {NONE, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P1M, false, DAY_14},
        {NONE, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P1M, true, DAY_14},

        {NONE, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P2W, false, DAY_TUE},
        {NONE, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P2W, true, DAY_TUE},
        {NONE, date(2014, JANUARY, 14), date(2014, AUGUST, 16), TERM, false, RollConventions.NONE},
        {NONE, date(2014, JANUARY, 14), date(2014, AUGUST, 16), TERM, true, RollConventions.NONE},
        {NONE, date(2014, JANUARY, 31), date(2014, APRIL, 30), P1M, true, RollConventions.EOM},
        {NONE, date(2014, APRIL, 30), date(2014, AUGUST, 31), P1M, true, RollConventions.EOM},
        {NONE, date(2014, APRIL, 30), date(2014, FEBRUARY, 28), P1M, true, RollConventions.EOM},
        {NONE, date(2016, FEBRUARY, 29), date(2019, FEBRUARY, 28), P6M, true, RollConventions.EOM},
        {NONE, date(2015, FEBRUARY, 28), date(2016, FEBRUARY, 29), P6M, true, RollConventions.EOM},
        {NONE, date(2015, APRIL, 30), date(2016, FEBRUARY, 29), P1M, true, RollConventions.EOM},
        {NONE, date(2016, MARCH, 31), date(2017, MARCH, 27), P6M, true, RollConventions.EOM},
        {NONE, date(2016, MARCH, 16), date(2016, MARCH, 31), P6M, true, RollConvention.ofDayOfMonth(16)},
        {NONE, date(2016, MARCH, 16), date(2017, MARCH, 31), P6M, true, RollConventions.EOM},

        {SHORT_INITIAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P1M, false, DAY_16},
        {SHORT_INITIAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P1M, true, DAY_16},
        {SHORT_INITIAL, date(2014, JANUARY, 14), date(2014, JUNE, 30), P1M, false, DAY_30},
        {SHORT_INITIAL, date(2014, JANUARY, 14), date(2014, JUNE, 30), P1M, true, EOM},

        {SHORT_INITIAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P2W, true, DAY_SAT},
        {SHORT_INITIAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P2W, true, DAY_SAT},
        {SHORT_INITIAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), TERM, false, RollConventions.NONE},
        {SHORT_INITIAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), TERM, true, RollConventions.NONE},

        {LONG_INITIAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P1M, false, DAY_16},
        {LONG_INITIAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P1M, true, DAY_16},
        {LONG_INITIAL, date(2014, JANUARY, 14), date(2014, JUNE, 30), P1M, false, DAY_30},
        {LONG_INITIAL, date(2014, JANUARY, 14), date(2014, JUNE, 30), P1M, true, EOM},

        {LONG_INITIAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P2W, false, DAY_SAT},
        {LONG_INITIAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P2W, true, DAY_SAT},
        {LONG_INITIAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), TERM, false, RollConventions.NONE},
        {LONG_INITIAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), TERM, true, RollConventions.NONE},

        {SMART_INITIAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P1M, false, DAY_16},
        {SMART_INITIAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P1M, true, DAY_16},
        {SMART_INITIAL, date(2014, JANUARY, 14), date(2014, JUNE, 30), P1M, false, DAY_30},
        {SMART_INITIAL, date(2014, JANUARY, 14), date(2014, JUNE, 30), P1M, true, EOM},

        {SMART_INITIAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P2W, false, DAY_SAT},
        {SMART_INITIAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P2W, true, DAY_SAT},
        {SMART_INITIAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), TERM, false, RollConventions.NONE},
        {SMART_INITIAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), TERM, true, RollConventions.NONE},

        {SHORT_FINAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P1M, false, DAY_14},
        {SHORT_FINAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P1M, true, DAY_14},
        {SHORT_FINAL, date(2014, JUNE, 30), date(2014, AUGUST, 16), P1M, false, DAY_30},
        {SHORT_FINAL, date(2014, JUNE, 30), date(2014, AUGUST, 16), P1M, true, EOM},

        {SHORT_FINAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P2W, false, DAY_TUE},
        {SHORT_FINAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P2W, true, DAY_TUE},
        {SHORT_FINAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), TERM, false, RollConventions.NONE},
        {SHORT_FINAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), TERM, true, RollConventions.NONE},

        {LONG_FINAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P1M, false, DAY_14},
        {LONG_FINAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P1M, true, DAY_14},
        {LONG_FINAL, date(2014, JUNE, 30), date(2014, AUGUST, 16), P1M, false, DAY_30},
        {LONG_FINAL, date(2014, JUNE, 30), date(2014, AUGUST, 16), P1M, true, EOM},

        {LONG_FINAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P2W, false, DAY_TUE},
        {LONG_FINAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P2W, true, DAY_TUE},
        {LONG_FINAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), TERM, false, RollConventions.NONE},
        {LONG_FINAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), TERM, true, RollConventions.NONE},

        {SMART_FINAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P1M, false, DAY_14},
        {SMART_FINAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P1M, true, DAY_14},
        {SMART_FINAL, date(2014, JUNE, 30), date(2014, AUGUST, 16), P1M, false, DAY_30},
        {SMART_FINAL, date(2014, JUNE, 30), date(2014, AUGUST, 16), P1M, true, EOM},

        {SMART_FINAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P2W, false, DAY_TUE},
        {SMART_FINAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P2W, true, DAY_TUE},
        {SMART_FINAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), TERM, false, RollConventions.NONE},
        {SMART_FINAL, date(2014, JANUARY, 14), date(2014, AUGUST, 16), TERM, true, RollConventions.NONE},

        {BOTH, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P1M, false, DAY_14},
        {BOTH, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P1M, true, DAY_14},
    };
  }

  @ParameterizedTest
  @MethodSource("data_roll")
  public void test_toRollConvention(
      StubConvention conv, LocalDate start, LocalDate end, Frequency freq, boolean eom, RollConvention expected) {
    assertThat(conv.toRollConvention(start, end, freq, eom)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_implicit() {
    return new Object[][] {
        {NONE, false, false, NONE},
        {NONE, true, false, null},
        {NONE, false, true, null},
        {NONE, true, true, null},

        {SHORT_INITIAL, false, false, SHORT_INITIAL},
        {SHORT_INITIAL, true, false, NONE},
        {SHORT_INITIAL, false, true, null},
        {SHORT_INITIAL, true, true, null},

        {LONG_INITIAL, false, false, LONG_INITIAL},
        {LONG_INITIAL, true, false, NONE},
        {LONG_INITIAL, false, true, null},
        {LONG_INITIAL, true, true, null},

        {SMART_INITIAL, false, false, SMART_INITIAL},
        {SMART_INITIAL, true, false, NONE},
        {SMART_INITIAL, false, true, SMART_INITIAL},
        {SMART_INITIAL, true, true, BOTH},

        {SHORT_FINAL, false, false, SHORT_FINAL},
        {SHORT_FINAL, true, false, null},
        {SHORT_FINAL, false, true, NONE},
        {SHORT_FINAL, true, true, null},

        {LONG_FINAL, false, false, LONG_FINAL},
        {LONG_FINAL, true, false, null},
        {LONG_FINAL, false, true, NONE},
        {LONG_FINAL, true, true, null},

        {SMART_FINAL, false, false, SMART_FINAL},
        {SMART_FINAL, true, false, SMART_FINAL},
        {SMART_FINAL, false, true, NONE},
        {SMART_FINAL, true, true, BOTH},

        {BOTH, false, false, null},
        {BOTH, true, false, null},
        {BOTH, false, true, null},
        {BOTH, true, true, NONE},
    };
  }

  @ParameterizedTest
  @MethodSource("data_implicit")
  public void test_toImplicit(
      StubConvention conv, boolean initialStub, boolean finalStub, StubConvention expected) {
    if (expected == null) {
      assertThatIllegalArgumentException().isThrownBy(() -> conv.toImplicit(null, initialStub, finalStub));
    } else {
      assertThat(conv.toImplicit(null, initialStub, finalStub)).isEqualTo(expected);
    }
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_isStubLong() {
    return new Object[][] {
        {NONE, date(2018, 6, 1), date(2018, 6, 8), false},
        {SHORT_INITIAL, date(2018, 6, 1), date(2018, 6, 8), false},
        {LONG_INITIAL, date(2018, 6, 1), date(2018, 6, 8), true},
        {SHORT_FINAL, date(2018, 6, 1), date(2018, 6, 8), false},
        {LONG_FINAL, date(2018, 6, 1), date(2018, 6, 8), true},
        {BOTH, date(2018, 6, 1), date(2018, 6, 8), false},

        {SMART_INITIAL, date(2018, 6, 1), date(2018, 6, 2), true},
        {SMART_INITIAL, date(2018, 6, 1), date(2018, 6, 7), true},
        {SMART_INITIAL, date(2018, 6, 1), date(2018, 6, 8), false},
        {SMART_INITIAL, date(2018, 6, 1), date(2018, 6, 9), false},

        {SMART_FINAL, date(2018, 6, 1), date(2018, 6, 2), true},
        {SMART_FINAL, date(2018, 6, 1), date(2018, 6, 7), true},
        {SMART_FINAL, date(2018, 6, 1), date(2018, 6, 8), false},
        {SMART_FINAL, date(2018, 6, 1), date(2018, 6, 9), false},
    };
  }

  @ParameterizedTest
  @MethodSource("data_isStubLong")
  public void test_isStubLong(
      StubConvention conv, LocalDate date1, LocalDate date2, Boolean expected) {
    if (expected == null) {
      assertThatIllegalArgumentException().isThrownBy(() -> conv.isStubLong(date1, date2));
    } else {
      assertThat(conv.isStubLong(date1, date2)).isEqualTo(expected.booleanValue());
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_NONE() {
    assertThat(NONE.isCalculateForwards()).isEqualTo(true);
    assertThat(NONE.isCalculateBackwards()).isEqualTo(false);
    assertThat(NONE.isFinal()).isEqualTo(false);
    assertThat(NONE.isLong()).isEqualTo(false);
    assertThat(NONE.isShort()).isEqualTo(false);
    assertThat(NONE.isSmart()).isEqualTo(false);
  }

  @Test
  public void test_SHORT_INITIAL() {
    assertThat(SHORT_INITIAL.isCalculateForwards()).isEqualTo(false);
    assertThat(SHORT_INITIAL.isCalculateBackwards()).isEqualTo(true);
    assertThat(SHORT_INITIAL.isFinal()).isEqualTo(false);
    assertThat(SHORT_INITIAL.isLong()).isEqualTo(false);
    assertThat(SHORT_INITIAL.isShort()).isEqualTo(true);
    assertThat(SHORT_INITIAL.isSmart()).isEqualTo(false);
  }

  @Test
  public void test_LONG_INITIAL() {
    assertThat(LONG_INITIAL.isCalculateForwards()).isEqualTo(false);
    assertThat(LONG_INITIAL.isCalculateBackwards()).isEqualTo(true);
    assertThat(LONG_INITIAL.isFinal()).isEqualTo(false);
    assertThat(LONG_INITIAL.isLong()).isEqualTo(true);
    assertThat(LONG_INITIAL.isShort()).isEqualTo(false);
    assertThat(LONG_INITIAL.isSmart()).isEqualTo(false);
  }

  @Test
  public void test_SMART_INITIAL() {
    assertThat(SMART_INITIAL.isCalculateForwards()).isEqualTo(false);
    assertThat(SMART_INITIAL.isCalculateBackwards()).isEqualTo(true);
    assertThat(SMART_INITIAL.isFinal()).isEqualTo(false);
    assertThat(SMART_INITIAL.isLong()).isEqualTo(false);
    assertThat(SMART_INITIAL.isShort()).isEqualTo(false);
    assertThat(SMART_INITIAL.isSmart()).isEqualTo(true);
  }

  @Test
  public void test_SHORT_FINAL() {
    assertThat(SHORT_FINAL.isCalculateForwards()).isEqualTo(true);
    assertThat(SHORT_FINAL.isCalculateBackwards()).isEqualTo(false);
    assertThat(SHORT_FINAL.isFinal()).isEqualTo(true);
    assertThat(SHORT_FINAL.isLong()).isEqualTo(false);
    assertThat(SHORT_FINAL.isShort()).isEqualTo(true);
    assertThat(SHORT_FINAL.isSmart()).isEqualTo(false);
  }

  @Test
  public void test_LONG_FINAL() {
    assertThat(LONG_FINAL.isCalculateForwards()).isEqualTo(true);
    assertThat(LONG_FINAL.isCalculateBackwards()).isEqualTo(false);
    assertThat(LONG_FINAL.isFinal()).isEqualTo(true);
    assertThat(LONG_FINAL.isLong()).isEqualTo(true);
    assertThat(LONG_FINAL.isShort()).isEqualTo(false);
    assertThat(LONG_FINAL.isSmart()).isEqualTo(false);
  }

  @Test
  public void test_SMART_FINAL() {
    assertThat(SMART_FINAL.isCalculateForwards()).isEqualTo(true);
    assertThat(SMART_FINAL.isCalculateBackwards()).isEqualTo(false);
    assertThat(SMART_FINAL.isFinal()).isEqualTo(true);
    assertThat(SMART_FINAL.isLong()).isEqualTo(false);
    assertThat(SMART_FINAL.isShort()).isEqualTo(false);
    assertThat(SMART_FINAL.isSmart()).isEqualTo(true);
  }

  @Test
  public void test_BOTH() {
    assertThat(BOTH.isCalculateForwards()).isEqualTo(false);
    assertThat(BOTH.isCalculateBackwards()).isEqualTo(false);
    assertThat(BOTH.isFinal()).isEqualTo(false);
    assertThat(BOTH.isLong()).isEqualTo(false);
    assertThat(BOTH.isShort()).isEqualTo(false);
    assertThat(BOTH.isSmart()).isEqualTo(false);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {NONE, "None"},
        {SHORT_INITIAL, "ShortInitial"},
        {LONG_INITIAL, "LongInitial"},
        {SMART_INITIAL, "SmartInitial"},
        {SHORT_FINAL, "ShortFinal"},
        {LONG_FINAL, "LongFinal"},
        {SMART_FINAL, "SmartFinal"},
        {BOTH, "Both"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(StubConvention convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(StubConvention convention, String name) {
    assertThat(StubConvention.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupUpperCase(StubConvention convention, String name) {
    assertThat(StubConvention.of(name.toUpperCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookupLowerCase(StubConvention convention, String name) {
    assertThat(StubConvention.of(name.toLowerCase(Locale.ENGLISH))).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> StubConvention.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> StubConvention.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverEnum(StubConvention.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(NONE);
    assertSerialization(SHORT_FINAL);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(StubConvention.class, NONE);
    assertJodaConvert(StubConvention.class, SHORT_FINAL);
  }

}
