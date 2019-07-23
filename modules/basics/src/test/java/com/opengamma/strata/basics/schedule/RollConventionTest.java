/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import static com.opengamma.strata.basics.schedule.Frequency.P1D;
import static com.opengamma.strata.basics.schedule.Frequency.P1M;
import static com.opengamma.strata.basics.schedule.Frequency.P1W;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_2;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_29;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_30;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_THU;
import static com.opengamma.strata.basics.schedule.RollConventions.EOM;
import static com.opengamma.strata.basics.schedule.RollConventions.IMM;
import static com.opengamma.strata.basics.schedule.RollConventions.IMMAUD;
import static com.opengamma.strata.basics.schedule.RollConventions.IMMCAD;
import static com.opengamma.strata.basics.schedule.RollConventions.IMMNZD;
import static com.opengamma.strata.basics.schedule.RollConventions.NONE;
import static com.opengamma.strata.basics.schedule.RollConventions.SFE;
import static com.opengamma.strata.basics.schedule.RollConventions.TBILL;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.Month.APRIL;
import static java.time.Month.AUGUST;
import static java.time.Month.FEBRUARY;
import static java.time.Month.JANUARY;
import static java.time.Month.JULY;
import static java.time.Month.JUNE;
import static java.time.Month.MARCH;
import static java.time.Month.NOVEMBER;
import static java.time.Month.OCTOBER;
import static java.time.Month.SEPTEMBER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.collect.named.ExtendedEnum;

/**
 * Test {@link RollConvention}.
 */
public class RollConventionTest {

  //-------------------------------------------------------------------------
  public static Object[][] data_types() {
    RollConvention[] conv = StandardRollConventions.values();
    Object[][] result = new Object[conv.length][];
    for (int i = 0; i < conv.length; i++) {
      result[i] = new Object[] {conv[i]};
    }
    return result;
  }

  @ParameterizedTest
  @MethodSource("data_types")
  public void test_null(RollConvention type) {
    assertThatIllegalArgumentException().isThrownBy(() -> type.adjust(null));
    assertThatIllegalArgumentException().isThrownBy(() -> type.matches(null));
    assertThatIllegalArgumentException().isThrownBy(() -> type.next(date(2014, JULY, 1), null));
    assertThatIllegalArgumentException().isThrownBy(() -> type.next(null, P3M));
    assertThatIllegalArgumentException().isThrownBy(() -> type.previous(date(2014, JULY, 1), null));
    assertThatIllegalArgumentException().isThrownBy(() -> type.previous(null, P3M));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_noAdjust() {
    LocalDate date = date(2014, AUGUST, 17);
    assertThat(NONE.adjust(date)).isEqualTo(date);
    assertThat(NONE.matches(date)).isEqualTo(true);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_adjust() {
    return new Object[][] {
        {EOM, date(2014, AUGUST, 1), date(2014, AUGUST, 31)},
        {EOM, date(2014, AUGUST, 30), date(2014, AUGUST, 31)},
        {EOM, date(2014, SEPTEMBER, 1), date(2014, SEPTEMBER, 30)},
        {EOM, date(2014, SEPTEMBER, 30), date(2014, SEPTEMBER, 30)},
        {EOM, date(2014, FEBRUARY, 1), date(2014, FEBRUARY, 28)},

        {IMM, date(2014, AUGUST, 1), date(2014, AUGUST, 20)},
        {IMM, date(2014, AUGUST, 6), date(2014, AUGUST, 20)},
        {IMM, date(2014, AUGUST, 19), date(2014, AUGUST, 20)},
        {IMM, date(2014, AUGUST, 20), date(2014, AUGUST, 20)},
        {IMM, date(2014, AUGUST, 21), date(2014, AUGUST, 20)},
        {IMM, date(2014, AUGUST, 31), date(2014, AUGUST, 20)},
        {IMM, date(2014, SEPTEMBER, 1), date(2014, SEPTEMBER, 17)},

        {IMMCAD, date(2014, AUGUST, 1), date(2014, AUGUST, 18)},
        {IMMCAD, date(2014, AUGUST, 6), date(2014, AUGUST, 18)},
        {IMMCAD, date(2014, AUGUST, 7), date(2014, AUGUST, 18)},
        {IMMCAD, date(2014, AUGUST, 8), date(2014, AUGUST, 18)},
        {IMMCAD, date(2014, AUGUST, 31), date(2014, AUGUST, 18)},
        {IMMCAD, date(2014, SEPTEMBER, 1), date(2014, SEPTEMBER, 15)},

        {IMMAUD, date(2014, AUGUST, 1), date(2014, AUGUST, 7)},
        {IMMAUD, date(2014, AUGUST, 6), date(2014, AUGUST, 7)},
        {IMMAUD, date(2014, AUGUST, 7), date(2014, AUGUST, 7)},
        {IMMAUD, date(2014, AUGUST, 8), date(2014, AUGUST, 7)},
        {IMMAUD, date(2014, AUGUST, 31), date(2014, AUGUST, 7)},
        {IMMAUD, date(2014, SEPTEMBER, 1), date(2014, SEPTEMBER, 11)},
        {IMMAUD, date(2014, OCTOBER, 1), date(2014, OCTOBER, 9)},
        {IMMAUD, date(2014, NOVEMBER, 1), date(2014, NOVEMBER, 13)},

        {IMMNZD, date(2014, AUGUST, 1), date(2014, AUGUST, 13)},
        {IMMNZD, date(2014, AUGUST, 6), date(2014, AUGUST, 13)},
        {IMMNZD, date(2014, AUGUST, 12), date(2014, AUGUST, 13)},
        {IMMNZD, date(2014, AUGUST, 13), date(2014, AUGUST, 13)},
        {IMMNZD, date(2014, AUGUST, 14), date(2014, AUGUST, 13)},
        {IMMNZD, date(2014, AUGUST, 31), date(2014, AUGUST, 13)},
        {IMMNZD, date(2014, SEPTEMBER, 1), date(2014, SEPTEMBER, 10)},
        {IMMNZD, date(2014, OCTOBER, 1), date(2014, OCTOBER, 15)},
        {IMMNZD, date(2014, NOVEMBER, 1), date(2014, NOVEMBER, 12)},

        {SFE, date(2014, AUGUST, 1), date(2014, AUGUST, 8)},
        {SFE, date(2014, AUGUST, 6), date(2014, AUGUST, 8)},
        {SFE, date(2014, AUGUST, 7), date(2014, AUGUST, 8)},
        {SFE, date(2014, AUGUST, 8), date(2014, AUGUST, 8)},
        {SFE, date(2014, AUGUST, 31), date(2014, AUGUST, 8)},
        {SFE, date(2014, SEPTEMBER, 1), date(2014, SEPTEMBER, 12)},
        {SFE, date(2014, OCTOBER, 1), date(2014, OCTOBER, 10)},
        {SFE, date(2014, NOVEMBER, 1), date(2014, NOVEMBER, 14)},

        {TBILL, date(2014, AUGUST, 1), date(2014, AUGUST, 4)},
        {TBILL, date(2014, AUGUST, 2), date(2014, AUGUST, 4)},
        {TBILL, date(2014, AUGUST, 3), date(2014, AUGUST, 4)},
        {TBILL, date(2014, AUGUST, 4), date(2014, AUGUST, 4)},
        {TBILL, date(2014, AUGUST, 5), date(2014, AUGUST, 11)},
        {TBILL, date(2014, AUGUST, 7), date(2014, AUGUST, 11)},
        {TBILL, date(2018, AUGUST, 31), date(2018, SEPTEMBER, 4)},  // Tuesday due to holiday
        {TBILL, date(2018, SEPTEMBER, 1), date(2018, SEPTEMBER, 4)},  // Tuesday due to holiday
    };
  }

  @ParameterizedTest
  @MethodSource("data_adjust")
  public void test_adjust(RollConvention conv, LocalDate input, LocalDate expected) {
    assertThat(conv.adjust(input)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_matches() {
    return new Object[][] {
        {EOM, date(2014, AUGUST, 1), false},
        {EOM, date(2014, AUGUST, 30), false},
        {EOM, date(2014, AUGUST, 31), true},
        {EOM, date(2014, SEPTEMBER, 1), false},
        {EOM, date(2014, SEPTEMBER, 30), true},

        {IMM, date(2014, SEPTEMBER, 16), false},
        {IMM, date(2014, SEPTEMBER, 17), true},
        {IMM, date(2014, SEPTEMBER, 18), false},

        {IMMAUD, date(2014, SEPTEMBER, 10), false},
        {IMMAUD, date(2014, SEPTEMBER, 11), true},
        {IMMAUD, date(2014, SEPTEMBER, 12), false},

        {IMMNZD, date(2014, SEPTEMBER, 9), false},
        {IMMNZD, date(2014, SEPTEMBER, 10), true},
        {IMMNZD, date(2014, SEPTEMBER, 11), false},

        {SFE, date(2014, SEPTEMBER, 11), false},
        {SFE, date(2014, SEPTEMBER, 12), true},
        {SFE, date(2014, SEPTEMBER, 13), false},
    };
  }

  @ParameterizedTest
  @MethodSource("data_matches")
  public void test_matches(RollConvention conv, LocalDate input, boolean expected) {
    assertThat(conv.matches(input)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_next() {
    return new Object[][] {
        {EOM, date(2014, AUGUST, 1), P1M, date(2014, SEPTEMBER, 30)},
        {EOM, date(2014, AUGUST, 30), P1M, date(2014, SEPTEMBER, 30)},
        {EOM, date(2014, AUGUST, 31), P1M, date(2014, SEPTEMBER, 30)},
        {EOM, date(2014, SEPTEMBER, 1), P1M, date(2014, OCTOBER, 31)},
        {EOM, date(2014, SEPTEMBER, 30), P1M, date(2014, OCTOBER, 31)},
        {EOM, date(2014, JANUARY, 1), P1M, date(2014, FEBRUARY, 28)},
        {EOM, date(2014, FEBRUARY, 1), P1M, date(2014, MARCH, 31)},
        {EOM, date(2014, AUGUST, 1), P3M, date(2014, NOVEMBER, 30)},
        {EOM, date(2014, AUGUST, 1), P1D, date(2014, AUGUST, 31)},
        {EOM, date(2014, AUGUST, 30), P1D, date(2014, AUGUST, 31)},
        {EOM, date(2014, AUGUST, 31), P1D, date(2014, SEPTEMBER, 30)},
        {EOM, date(2014, JANUARY, 1), P1D, date(2014, JANUARY, 31)},
        {EOM, date(2014, JANUARY, 31), P1D, date(2014, FEBRUARY, 28)},
        {EOM, date(2014, FEBRUARY, 1), P1D, date(2014, FEBRUARY, 28)},

        {IMM, date(2014, AUGUST, 1), P1M, date(2014, SEPTEMBER, 17)},
        {IMM, date(2014, AUGUST, 31), P1M, date(2014, SEPTEMBER, 17)},
        {IMM, date(2014, SEPTEMBER, 1), P1M, date(2014, OCTOBER, 15)},
        {IMM, date(2014, SEPTEMBER, 30), P1M, date(2014, OCTOBER, 15)},
        {IMM, date(2014, AUGUST, 1), P1D, date(2014, AUGUST, 20)},
        {IMM, date(2014, AUGUST, 19), P1D, date(2014, AUGUST, 20)},
        {IMM, date(2014, AUGUST, 20), P1D, date(2014, SEPTEMBER, 17)},
        {IMM, date(2014, AUGUST, 31), P1D, date(2014, SEPTEMBER, 17)},
        {IMM, date(2014, SEPTEMBER, 1), P1D, date(2014, SEPTEMBER, 17)},
        {IMM, date(2014, SEPTEMBER, 16), P1D, date(2014, SEPTEMBER, 17)},
        {IMM, date(2014, SEPTEMBER, 17), P1D, date(2014, OCTOBER, 15)},
        {IMM, date(2014, SEPTEMBER, 30), P1D, date(2014, OCTOBER, 15)},

        {IMMAUD, date(2014, AUGUST, 1), P1M, date(2014, SEPTEMBER, 11)},
        {IMMAUD, date(2014, AUGUST, 31), P1M, date(2014, SEPTEMBER, 11)},
        {IMMAUD, date(2014, SEPTEMBER, 1), P1M, date(2014, OCTOBER, 9)},
        {IMMAUD, date(2014, SEPTEMBER, 30), P1M, date(2014, OCTOBER, 9)},
        {IMMAUD, date(2014, AUGUST, 1), P1D, date(2014, AUGUST, 7)},
        {IMMAUD, date(2014, AUGUST, 6), P1D, date(2014, AUGUST, 7)},
        {IMMAUD, date(2014, AUGUST, 7), P1D, date(2014, SEPTEMBER, 11)},
        {IMMAUD, date(2014, AUGUST, 31), P1D, date(2014, SEPTEMBER, 11)},
        {IMMAUD, date(2014, SEPTEMBER, 1), P1D, date(2014, SEPTEMBER, 11)},
        {IMMAUD, date(2014, SEPTEMBER, 10), P1D, date(2014, SEPTEMBER, 11)},
        {IMMAUD, date(2014, SEPTEMBER, 11), P1D, date(2014, OCTOBER, 9)},
        {IMMAUD, date(2014, SEPTEMBER, 30), P1D, date(2014, OCTOBER, 9)},

        {IMMNZD, date(2014, AUGUST, 1), P1M, date(2014, SEPTEMBER, 10)},
        {IMMNZD, date(2014, AUGUST, 31), P1M, date(2014, SEPTEMBER, 10)},
        {IMMNZD, date(2014, SEPTEMBER, 1), P1M, date(2014, OCTOBER, 15)},
        {IMMNZD, date(2014, SEPTEMBER, 30), P1M, date(2014, OCTOBER, 15)},
        {IMMNZD, date(2014, AUGUST, 1), P1D, date(2014, AUGUST, 13)},
        {IMMNZD, date(2014, AUGUST, 12), P1D, date(2014, AUGUST, 13)},
        {IMMNZD, date(2014, AUGUST, 13), P1D, date(2014, SEPTEMBER, 10)},
        {IMMNZD, date(2014, AUGUST, 31), P1D, date(2014, SEPTEMBER, 10)},
        {IMMNZD, date(2014, SEPTEMBER, 1), P1D, date(2014, SEPTEMBER, 10)},
        {IMMNZD, date(2014, SEPTEMBER, 9), P1D, date(2014, SEPTEMBER, 10)},
        {IMMNZD, date(2014, SEPTEMBER, 10), P1D, date(2014, OCTOBER, 15)},
        {IMMNZD, date(2014, SEPTEMBER, 30), P1D, date(2014, OCTOBER, 15)},

        {SFE, date(2014, AUGUST, 1), P1M, date(2014, SEPTEMBER, 12)},
        {SFE, date(2014, AUGUST, 31), P1M, date(2014, SEPTEMBER, 12)},
        {SFE, date(2014, SEPTEMBER, 1), P1M, date(2014, OCTOBER, 10)},
        {SFE, date(2014, SEPTEMBER, 30), P1M, date(2014, OCTOBER, 10)},
        {SFE, date(2014, AUGUST, 1), P1D, date(2014, AUGUST, 8)},
        {SFE, date(2014, AUGUST, 7), P1D, date(2014, AUGUST, 8)},
        {SFE, date(2014, AUGUST, 8), P1D, date(2014, SEPTEMBER, 12)},
        {SFE, date(2014, AUGUST, 31), P1D, date(2014, SEPTEMBER, 12)},
        {SFE, date(2014, SEPTEMBER, 1), P1D, date(2014, SEPTEMBER, 12)},
        {SFE, date(2014, SEPTEMBER, 11), P1D, date(2014, SEPTEMBER, 12)},
        {SFE, date(2014, SEPTEMBER, 12), P1D, date(2014, OCTOBER, 10)},
        {SFE, date(2014, SEPTEMBER, 30), P1D, date(2014, OCTOBER, 10)},
    };
  }

  @ParameterizedTest
  @MethodSource("data_next")
  public void test_next(RollConvention conv, LocalDate input, Frequency freq, LocalDate expected) {
    assertThat(conv.next(input, freq)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_previous() {
    return new Object[][] {
        {EOM, date(2014, OCTOBER, 1), P1M, date(2014, SEPTEMBER, 30)},
        {EOM, date(2014, OCTOBER, 31), P1M, date(2014, SEPTEMBER, 30)},
        {EOM, date(2014, NOVEMBER, 1), P1M, date(2014, OCTOBER, 31)},
        {EOM, date(2014, NOVEMBER, 30), P1M, date(2014, OCTOBER, 31)},
        {EOM, date(2014, MARCH, 1), P1M, date(2014, FEBRUARY, 28)},
        {EOM, date(2014, APRIL, 1), P1M, date(2014, MARCH, 31)},
        {EOM, date(2014, NOVEMBER, 1), P3M, date(2014, AUGUST, 31)},
        {EOM, date(2014, OCTOBER, 1), P1D, date(2014, SEPTEMBER, 30)},
        {EOM, date(2014, OCTOBER, 30), P1D, date(2014, SEPTEMBER, 30)},

        {IMM, date(2014, OCTOBER, 1), P1M, date(2014, SEPTEMBER, 17)},
        {IMM, date(2014, OCTOBER, 31), P1M, date(2014, SEPTEMBER, 17)},
        {IMM, date(2014, NOVEMBER, 1), P1M, date(2014, OCTOBER, 15)},
        {IMM, date(2014, NOVEMBER, 30), P1M, date(2014, OCTOBER, 15)},
        {IMM, date(2014, AUGUST, 1), P1D, date(2014, JULY, 16)},
        {IMM, date(2014, AUGUST, 20), P1D, date(2014, JULY, 16)},
        {IMM, date(2014, AUGUST, 21), P1D, date(2014, AUGUST, 20)},
        {IMM, date(2014, AUGUST, 31), P1D, date(2014, AUGUST, 20)},
        {IMM, date(2014, SEPTEMBER, 1), P1D, date(2014, AUGUST, 20)},
        {IMM, date(2014, SEPTEMBER, 17), P1D, date(2014, AUGUST, 20)},
        {IMM, date(2014, SEPTEMBER, 18), P1D, date(2014, SEPTEMBER, 17)},
        {IMM, date(2014, SEPTEMBER, 30), P1D, date(2014, SEPTEMBER, 17)},

        {IMMAUD, date(2014, OCTOBER, 1), P1M, date(2014, SEPTEMBER, 11)},
        {IMMAUD, date(2014, OCTOBER, 31), P1M, date(2014, SEPTEMBER, 11)},
        {IMMAUD, date(2014, NOVEMBER, 1), P1M, date(2014, OCTOBER, 9)},
        {IMMAUD, date(2014, NOVEMBER, 30), P1M, date(2014, OCTOBER, 9)},
        {IMMAUD, date(2014, SEPTEMBER, 1), P1D, date(2014, AUGUST, 7)},
        {IMMAUD, date(2014, SEPTEMBER, 11), P1D, date(2014, AUGUST, 7)},
        {IMMAUD, date(2014, SEPTEMBER, 12), P1D, date(2014, SEPTEMBER, 11)},
        {IMMAUD, date(2014, SEPTEMBER, 30), P1D, date(2014, SEPTEMBER, 11)},
        {IMMAUD, date(2014, OCTOBER, 1), P1D, date(2014, SEPTEMBER, 11)},
        {IMMAUD, date(2014, OCTOBER, 9), P1D, date(2014, SEPTEMBER, 11)},
        {IMMAUD, date(2014, OCTOBER, 10), P1D, date(2014, OCTOBER, 9)},
        {IMMAUD, date(2014, OCTOBER, 30), P1D, date(2014, OCTOBER, 9)},

        {IMMNZD, date(2014, OCTOBER, 1), P1M, date(2014, SEPTEMBER, 10)},
        {IMMNZD, date(2014, OCTOBER, 31), P1M, date(2014, SEPTEMBER, 10)},
        {IMMNZD, date(2014, NOVEMBER, 1), P1M, date(2014, OCTOBER, 15)},
        {IMMNZD, date(2014, NOVEMBER, 30), P1M, date(2014, OCTOBER, 15)},
        {IMMNZD, date(2014, SEPTEMBER, 1), P1D, date(2014, AUGUST, 13)},
        {IMMNZD, date(2014, SEPTEMBER, 10), P1D, date(2014, AUGUST, 13)},
        {IMMNZD, date(2014, SEPTEMBER, 11), P1D, date(2014, SEPTEMBER, 10)},
        {IMMNZD, date(2014, SEPTEMBER, 30), P1D, date(2014, SEPTEMBER, 10)},
        {IMMNZD, date(2014, OCTOBER, 1), P1D, date(2014, SEPTEMBER, 10)},
        {IMMNZD, date(2014, OCTOBER, 15), P1D, date(2014, SEPTEMBER, 10)},
        {IMMNZD, date(2014, OCTOBER, 16), P1D, date(2014, OCTOBER, 15)},
        {IMMNZD, date(2014, OCTOBER, 30), P1D, date(2014, OCTOBER, 15)},

        {SFE, date(2014, OCTOBER, 1), P1M, date(2014, SEPTEMBER, 12)},
        {SFE, date(2014, OCTOBER, 31), P1M, date(2014, SEPTEMBER, 12)},
        {SFE, date(2014, NOVEMBER, 1), P1M, date(2014, OCTOBER, 10)},
        {SFE, date(2014, NOVEMBER, 30), P1M, date(2014, OCTOBER, 10)},
        {SFE, date(2014, SEPTEMBER, 1), P1D, date(2014, AUGUST, 8)},
        {SFE, date(2014, SEPTEMBER, 12), P1D, date(2014, AUGUST, 8)},
        {SFE, date(2014, SEPTEMBER, 13), P1D, date(2014, SEPTEMBER, 12)},
        {SFE, date(2014, SEPTEMBER, 30), P1D, date(2014, SEPTEMBER, 12)},
        {SFE, date(2014, OCTOBER, 1), P1D, date(2014, SEPTEMBER, 12)},
        {SFE, date(2014, OCTOBER, 10), P1D, date(2014, SEPTEMBER, 12)},
        {SFE, date(2014, OCTOBER, 11), P1D, date(2014, OCTOBER, 10)},
        {SFE, date(2014, OCTOBER, 30), P1D, date(2014, OCTOBER, 10)},
    };
  }

  @ParameterizedTest
  @MethodSource("data_previous")
  public void test_previous(RollConvention conv, LocalDate input, Frequency freq, LocalDate expected) {
    assertThat(conv.previous(input, freq)).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_dayOfMonth_constants() {
    assertThat(RollConventions.DAY_1.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 1));
    assertThat(RollConventions.DAY_2.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 2));
    assertThat(RollConventions.DAY_3.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 3));
    assertThat(RollConventions.DAY_4.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 4));
    assertThat(RollConventions.DAY_5.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 5));
    assertThat(RollConventions.DAY_6.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 6));
    assertThat(RollConventions.DAY_7.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 7));
    assertThat(RollConventions.DAY_8.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 8));
    assertThat(RollConventions.DAY_9.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 9));
    assertThat(RollConventions.DAY_10.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 10));
    assertThat(RollConventions.DAY_11.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 11));
    assertThat(RollConventions.DAY_12.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 12));
    assertThat(RollConventions.DAY_13.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 13));
    assertThat(RollConventions.DAY_14.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 14));
    assertThat(RollConventions.DAY_15.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 15));
    assertThat(RollConventions.DAY_16.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 16));
    assertThat(RollConventions.DAY_17.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 17));
    assertThat(RollConventions.DAY_18.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 18));
    assertThat(RollConventions.DAY_19.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 19));
    assertThat(RollConventions.DAY_20.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 20));
    assertThat(RollConventions.DAY_21.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 21));
    assertThat(RollConventions.DAY_22.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 22));
    assertThat(RollConventions.DAY_23.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 23));
    assertThat(RollConventions.DAY_24.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 24));
    assertThat(RollConventions.DAY_25.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 25));
    assertThat(RollConventions.DAY_26.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 26));
    assertThat(RollConventions.DAY_27.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 27));
    assertThat(RollConventions.DAY_28.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 28));
    assertThat(RollConventions.DAY_29.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 29));
    assertThat(RollConventions.DAY_30.adjust(date(2014, JULY, 30))).isEqualTo(date(2014, JULY, 30));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_ofDayOfMonth() {
    for (int i = 1; i < 30; i++) {
      RollConvention test = RollConvention.ofDayOfMonth(i);
      assertThat(test.adjust(date(2014, JULY, 1))).isEqualTo(date(2014, JULY, i));
      assertThat(test.getName()).isEqualTo("Day" + i);
      assertThat(test.toString()).isEqualTo("Day" + i);
      assertThat(RollConvention.of(test.getName())).isSameAs(test);
      assertThat(RollConvention.of("DAY" + i)).isSameAs(test);
    }
  }

  @Test
  public void test_ofDayOfMonth_31() {
    assertThat(RollConvention.ofDayOfMonth(31)).isEqualTo(EOM);
  }

  @Test
  public void test_ofDayOfMonth_invalid() {
    assertThatIllegalArgumentException().isThrownBy(() -> RollConvention.ofDayOfMonth(0));
    assertThatIllegalArgumentException().isThrownBy(() -> RollConvention.ofDayOfMonth(32));
  }

  @Test
  public void test_ofDayOfMonth_adjust_Day29() {
    assertThat(RollConvention.ofDayOfMonth(29).adjust(date(2014, FEBRUARY, 2))).isEqualTo(date(2014, FEBRUARY, 28));
    assertThat(RollConvention.ofDayOfMonth(29).adjust(date(2016, FEBRUARY, 2))).isEqualTo(date(2016, FEBRUARY, 29));
  }

  @Test
  public void test_ofDayOfMonth_adjust_Day30() {
    assertThat(RollConvention.ofDayOfMonth(30).adjust(date(2014, FEBRUARY, 2))).isEqualTo(date(2014, FEBRUARY, 28));
    assertThat(RollConvention.ofDayOfMonth(30).adjust(date(2016, FEBRUARY, 2))).isEqualTo(date(2016, FEBRUARY, 29));
  }

  @Test
  public void test_ofDayOfMonth_matches_Day29() {
    assertThat(RollConvention.ofDayOfMonth(29).matches(date(2016, JANUARY, 30))).isEqualTo(false);
    assertThat(RollConvention.ofDayOfMonth(29).matches(date(2016, JANUARY, 29))).isEqualTo(true);
    assertThat(RollConvention.ofDayOfMonth(29).matches(date(2016, JANUARY, 30))).isEqualTo(false);

    assertThat(RollConvention.ofDayOfMonth(29).matches(date(2016, FEBRUARY, 28))).isEqualTo(false);
    assertThat(RollConvention.ofDayOfMonth(29).matches(date(2016, FEBRUARY, 29))).isEqualTo(true);

    assertThat(RollConvention.ofDayOfMonth(29).matches(date(2015, FEBRUARY, 27))).isEqualTo(false);
    assertThat(RollConvention.ofDayOfMonth(29).matches(date(2015, FEBRUARY, 28))).isEqualTo(true);
  }

  @Test
  public void test_ofDayOfMonth_matches_Day30() {
    assertThat(RollConvention.ofDayOfMonth(30).matches(date(2016, JANUARY, 29))).isEqualTo(false);
    assertThat(RollConvention.ofDayOfMonth(30).matches(date(2016, JANUARY, 30))).isEqualTo(true);
    assertThat(RollConvention.ofDayOfMonth(30).matches(date(2016, JANUARY, 31))).isEqualTo(false);

    assertThat(RollConvention.ofDayOfMonth(30).matches(date(2016, FEBRUARY, 28))).isEqualTo(false);
    assertThat(RollConvention.ofDayOfMonth(30).matches(date(2016, FEBRUARY, 29))).isEqualTo(true);

    assertThat(RollConvention.ofDayOfMonth(30).matches(date(2015, FEBRUARY, 27))).isEqualTo(false);
    assertThat(RollConvention.ofDayOfMonth(30).matches(date(2015, FEBRUARY, 28))).isEqualTo(true);
  }

  @Test
  public void test_ofDayOfMonth_next_oneMonth() {
    for (int start = 1; start <= 5; start++) {
      for (int i = 1; i <= 30; i++) {
        RollConvention test = RollConvention.ofDayOfMonth(i);
        LocalDate expected = date(2014, AUGUST, i);
        assertThat(test.next(date(2014, JULY, start), P1M)).isEqualTo(expected);
      }
    }
  }

  @Test
  public void test_ofDayOfMonth_next_oneDay() {
    for (int start = 1; start <= 5; start++) {
      for (int i = 1; i <= 30; i++) {
        RollConvention test = RollConvention.ofDayOfMonth(i);
        LocalDate expected = date(2014, JULY, i);
        if (i <= start) {
          expected = expected.plusMonths(1);
        }
        assertThat(test.next(date(2014, JULY, start), P1D)).isEqualTo(expected);
      }
    }
  }

  @Test
  public void test_ofDayOfMonth_previous_oneMonth() {
    for (int start = 1; start <= 5; start++) {
      for (int i = 1; i <= 30; i++) {
        RollConvention test = RollConvention.ofDayOfMonth(i);
        LocalDate expected = date(2014, JUNE, i);
        assertThat(test.previous(date(2014, JULY, start), P1M)).isEqualTo(expected);
      }
    }
  }

  @Test
  public void test_ofDayOfMonth_previous_oneDay() {
    for (int start = 1; start <= 5; start++) {
      for (int i = 1; i <= 30; i++) {
        RollConvention test = RollConvention.ofDayOfMonth(i);
        LocalDate expected = date(2014, JULY, i);
        if (i >= start) {
          expected = expected.minusMonths(1);
        }
        assertThat(test.previous(date(2014, JULY, start), P1D)).isEqualTo(expected);
      }
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_dayOfWeek_constants() {
    assertThat(RollConventions.DAY_MON.adjust(date(2014, AUGUST, 11))).isEqualTo(date(2014, AUGUST, 11));
    assertThat(RollConventions.DAY_TUE.adjust(date(2014, AUGUST, 11))).isEqualTo(date(2014, AUGUST, 12));
    assertThat(RollConventions.DAY_WED.adjust(date(2014, AUGUST, 11))).isEqualTo(date(2014, AUGUST, 13));
    assertThat(RollConventions.DAY_THU.adjust(date(2014, AUGUST, 11))).isEqualTo(date(2014, AUGUST, 14));
    assertThat(RollConventions.DAY_FRI.adjust(date(2014, AUGUST, 11))).isEqualTo(date(2014, AUGUST, 15));
    assertThat(RollConventions.DAY_SAT.adjust(date(2014, AUGUST, 11))).isEqualTo(date(2014, AUGUST, 16));
    assertThat(RollConventions.DAY_SUN.adjust(date(2014, AUGUST, 11))).isEqualTo(date(2014, AUGUST, 17));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_ofDayOfWeek() {
    for (DayOfWeek dow : DayOfWeek.values()) {
      RollConvention test = RollConvention.ofDayOfWeek(dow);
      assertThat(test.getName())
          .isEqualTo("Day" +
              CaseFormat.UPPER_UNDERSCORE.converterTo(CaseFormat.UPPER_CAMEL).convert(dow.toString()).substring(0, 3));
      assertThat(test.toString())
          .isEqualTo("Day" +
              CaseFormat.UPPER_UNDERSCORE.converterTo(CaseFormat.UPPER_CAMEL).convert(dow.toString()).substring(0, 3));
      assertThat(RollConvention.of(test.getName())).isSameAs(test);
      assertThat(RollConvention.of("DAY" + dow.toString().substring(0, 3))).isSameAs(test);
    }
  }

  @Test
  public void test_ofDayOfWeek_adjust() {
    for (DayOfWeek dow : DayOfWeek.values()) {
      RollConvention test = RollConvention.ofDayOfWeek(dow);
      assertThat(test.adjust(date(2014, AUGUST, 14)))
          .isEqualTo(date(2014, AUGUST, 14).with(TemporalAdjusters.nextOrSame(dow)));
    }
  }

  @Test
  public void test_ofDayOfWeek_matches() {
    assertThat(RollConvention.ofDayOfWeek(TUESDAY).matches(date(2014, SEPTEMBER, 1))).isEqualTo(false);
    assertThat(RollConvention.ofDayOfWeek(TUESDAY).matches(date(2014, SEPTEMBER, 2))).isEqualTo(true);
    assertThat(RollConvention.ofDayOfWeek(TUESDAY).matches(date(2014, SEPTEMBER, 3))).isEqualTo(false);
  }

  @Test
  public void test_ofDayOfWeek_next_oneMonth() {
    for (DayOfWeek dow : DayOfWeek.values()) {
      RollConvention test = RollConvention.ofDayOfWeek(dow);
      assertThat(test.next(date(2014, AUGUST, 14), P1W))
          .isEqualTo(date(2014, AUGUST, 21).with(TemporalAdjusters.nextOrSame(dow)));
    }
  }

  @Test
  public void test_ofDayOfWeek_next_oneDay() {
    for (DayOfWeek dow : DayOfWeek.values()) {
      RollConvention test = RollConvention.ofDayOfWeek(dow);
      assertThat(test.next(date(2014, AUGUST, 14), P1D))
          .isEqualTo(date(2014, AUGUST, 15).with(TemporalAdjusters.nextOrSame(dow)));
    }
  }

  @Test
  public void test_ofDayOfWeek_previous_oneMonth() {
    for (DayOfWeek dow : DayOfWeek.values()) {
      RollConvention test = RollConvention.ofDayOfWeek(dow);
      assertThat(test.previous(date(2014, AUGUST, 14), P1W))
          .isEqualTo(date(2014, AUGUST, 7).with(TemporalAdjusters.previousOrSame(dow)));
    }
  }

  @Test
  public void test_ofDayOfWeek_previous_oneDay() {
    for (DayOfWeek dow : DayOfWeek.values()) {
      RollConvention test = RollConvention.ofDayOfWeek(dow);
      assertThat(test.previous(date(2014, AUGUST, 14), P1D))
          .isEqualTo(date(2014, AUGUST, 13).with(TemporalAdjusters.previousOrSame(dow)));
    }
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {NONE, "None"},
        {EOM, "EOM"},
        {IMM, "IMM"},
        {IMMAUD, "IMMAUD"},
        {IMMNZD, "IMMNZD"},
        {SFE, "SFE"},
        {DAY_2, "Day2"},
        {DAY_THU, "DayThu"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_name(RollConvention convention, String name) {
    assertThat(convention.getName()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(RollConvention convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(RollConvention convention, String name) {
    assertThat(RollConvention.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_lenientLookup_standardNames(RollConvention convention, String name) {
    assertThat(RollConvention.extendedEnum().findLenient(name.toLowerCase(Locale.ENGLISH)).get()).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_extendedEnum(RollConvention convention, String name) {
    ImmutableMap<String, RollConvention> map = RollConvention.extendedEnum().lookupAll();
    assertThat(map.get(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> RollConvention.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> RollConvention.of(null));
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_lenient() {
    return new Object[][] {
        {"2", DAY_2},
        {"29", DAY_29},
        {"Day29", DAY_29},
        {"Day_29", DAY_29},
        {"30", DAY_30},
        {"Day30", DAY_30},
        {"Day_30", DAY_30},
        {"31", EOM},
        {"Day31", EOM},
        {"Day_31", EOM},
        {"THU", DAY_THU},
    };
  }

  @ParameterizedTest
  @MethodSource("data_lenient")
  public void test_lenientLookup_specialNames(String name, RollConvention convention) {
    assertThat(RollConvention.extendedEnum().findLenient(name.toLowerCase(Locale.ENGLISH))).isEqualTo(Optional.of(convention));
  }

  @Test
  public void test_lenientLookup_constants() throws IllegalAccessException {
    Field[] fields = RollConventions.class.getDeclaredFields();
    for (Field field : fields) {
      if (Modifier.isPublic(field.getModifiers()) &&
          Modifier.isStatic(field.getModifiers()) &&
          Modifier.isFinal(field.getModifiers())) {

        String name = field.getName();
        Object value = field.get(null);
        ExtendedEnum<RollConvention> ext = RollConvention.extendedEnum();
        assertThat(ext.findLenient(name)).isEqualTo(Optional.of(value));
        assertThat(ext.findLenient(name.toLowerCase(Locale.ENGLISH))).isEqualTo(Optional.of(value));
      }
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equals() {
    RollConvention a = RollConventions.EOM;
    RollConvention b = RollConventions.DAY_1;
    RollConvention c = RollConventions.DAY_WED;

    assertThat(a.equals(a)).isEqualTo(true);
    assertThat(a.equals(b)).isEqualTo(false);
    assertThat(a.equals(c)).isEqualTo(false);

    assertThat(b.equals(a)).isEqualTo(false);
    assertThat(b.equals(b)).isEqualTo(true);
    assertThat(b.equals(c)).isEqualTo(false);

    assertThat(c.equals(a)).isEqualTo(false);
    assertThat(c.equals(b)).isEqualTo(false);
    assertThat(c.equals(c)).isEqualTo(true);

    assertThat(a.hashCode()).isEqualTo(a.hashCode());
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(RollConventions.class);
    coverEnum(StandardRollConventions.class);
  }

  @Test
  public void test_serialization() {
    assertSerialization(EOM);
    assertSerialization(DAY_2);
    assertSerialization(DAY_THU);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(RollConvention.class, NONE);
    assertJodaConvert(RollConvention.class, EOM);
  }

}
