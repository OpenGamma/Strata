/**
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
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_THU;
import static com.opengamma.strata.basics.schedule.RollConventions.EOM;
import static com.opengamma.strata.basics.schedule.RollConventions.IMM;
import static com.opengamma.strata.basics.schedule.RollConventions.IMMAUD;
import static com.opengamma.strata.basics.schedule.RollConventions.IMMNZD;
import static com.opengamma.strata.basics.schedule.RollConventions.NONE;
import static com.opengamma.strata.basics.schedule.RollConventions.SFE;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
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
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableMap;

/**
 * Test {@link RollConvention}.
 */
@Test
public class RollConventionTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "types")
  static Object[][] data_types() {
    RollConvention[] conv = StandardRollConventions.values();
    Object[][] result = new Object[conv.length][];
    for (int i = 0; i < conv.length; i++) {
      result[i] = new Object[] {conv[i]};
    }
    return result;
  }

  @Test(dataProvider = "types")
  public void test_null(RollConvention type) {
    assertThrowsIllegalArg(() -> type.adjust(null));
    assertThrowsIllegalArg(() -> type.matches(null));
    assertThrowsIllegalArg(() -> type.next(date(2014, JULY, 1), null));
    assertThrowsIllegalArg(() -> type.next(null, P3M));
    assertThrowsIllegalArg(() -> type.previous(date(2014, JULY, 1), null));
    assertThrowsIllegalArg(() -> type.previous(null, P3M));
  }

  //-------------------------------------------------------------------------
  public void test_noAdjust() {
    LocalDate date = date(2014, AUGUST, 17);
    assertEquals(NONE.adjust(date), date);
    assertEquals(NONE.matches(date), true);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "adjust")
  static Object[][] data_adjust() {
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
    };
  }

  @Test(dataProvider = "adjust")
  public void test_adjust(RollConvention conv, LocalDate input, LocalDate expected) {
    assertEquals(conv.adjust(input), expected);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "matches")
  static Object[][] data_matches() {
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

  @Test(dataProvider = "matches")
  public void test_matches(RollConvention conv, LocalDate input, boolean expected) {
    assertEquals(conv.matches(input), expected);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "next")
  static Object[][] data_next() {
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

  @Test(dataProvider = "next")
  public void test_next(RollConvention conv, LocalDate input, Frequency freq, LocalDate expected) {
    assertEquals(conv.next(input, freq), expected);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "previous")
  static Object[][] data_previous() {
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

  @Test(dataProvider = "previous")
  public void test_previous(RollConvention conv, LocalDate input, Frequency freq, LocalDate expected) {
    assertEquals(conv.previous(input, freq), expected);
  }

  //-------------------------------------------------------------------------
  public void test_dayOfMonth_constants() {
    assertEquals(RollConventions.DAY_1.adjust(date(2014, JULY, 30)), date(2014, JULY, 1));
    assertEquals(RollConventions.DAY_2.adjust(date(2014, JULY, 30)), date(2014, JULY, 2));
    assertEquals(RollConventions.DAY_3.adjust(date(2014, JULY, 30)), date(2014, JULY, 3));
    assertEquals(RollConventions.DAY_4.adjust(date(2014, JULY, 30)), date(2014, JULY, 4));
    assertEquals(RollConventions.DAY_5.adjust(date(2014, JULY, 30)), date(2014, JULY, 5));
    assertEquals(RollConventions.DAY_6.adjust(date(2014, JULY, 30)), date(2014, JULY, 6));
    assertEquals(RollConventions.DAY_7.adjust(date(2014, JULY, 30)), date(2014, JULY, 7));
    assertEquals(RollConventions.DAY_8.adjust(date(2014, JULY, 30)), date(2014, JULY, 8));
    assertEquals(RollConventions.DAY_9.adjust(date(2014, JULY, 30)), date(2014, JULY, 9));
    assertEquals(RollConventions.DAY_10.adjust(date(2014, JULY, 30)), date(2014, JULY, 10));
    assertEquals(RollConventions.DAY_11.adjust(date(2014, JULY, 30)), date(2014, JULY, 11));
    assertEquals(RollConventions.DAY_12.adjust(date(2014, JULY, 30)), date(2014, JULY, 12));
    assertEquals(RollConventions.DAY_13.adjust(date(2014, JULY, 30)), date(2014, JULY, 13));
    assertEquals(RollConventions.DAY_14.adjust(date(2014, JULY, 30)), date(2014, JULY, 14));
    assertEquals(RollConventions.DAY_15.adjust(date(2014, JULY, 30)), date(2014, JULY, 15));
    assertEquals(RollConventions.DAY_16.adjust(date(2014, JULY, 30)), date(2014, JULY, 16));
    assertEquals(RollConventions.DAY_17.adjust(date(2014, JULY, 30)), date(2014, JULY, 17));
    assertEquals(RollConventions.DAY_18.adjust(date(2014, JULY, 30)), date(2014, JULY, 18));
    assertEquals(RollConventions.DAY_19.adjust(date(2014, JULY, 30)), date(2014, JULY, 19));
    assertEquals(RollConventions.DAY_20.adjust(date(2014, JULY, 30)), date(2014, JULY, 20));
    assertEquals(RollConventions.DAY_21.adjust(date(2014, JULY, 30)), date(2014, JULY, 21));
    assertEquals(RollConventions.DAY_22.adjust(date(2014, JULY, 30)), date(2014, JULY, 22));
    assertEquals(RollConventions.DAY_23.adjust(date(2014, JULY, 30)), date(2014, JULY, 23));
    assertEquals(RollConventions.DAY_24.adjust(date(2014, JULY, 30)), date(2014, JULY, 24));
    assertEquals(RollConventions.DAY_25.adjust(date(2014, JULY, 30)), date(2014, JULY, 25));
    assertEquals(RollConventions.DAY_26.adjust(date(2014, JULY, 30)), date(2014, JULY, 26));
    assertEquals(RollConventions.DAY_27.adjust(date(2014, JULY, 30)), date(2014, JULY, 27));
    assertEquals(RollConventions.DAY_28.adjust(date(2014, JULY, 30)), date(2014, JULY, 28));
    assertEquals(RollConventions.DAY_29.adjust(date(2014, JULY, 30)), date(2014, JULY, 29));
    assertEquals(RollConventions.DAY_30.adjust(date(2014, JULY, 30)), date(2014, JULY, 30));
  }

  //-------------------------------------------------------------------------
  public void test_ofDayOfMonth() {
    for (int i = 1; i < 30; i++) {
      RollConvention test = RollConvention.ofDayOfMonth(i);
      assertEquals(test.adjust(date(2014, JULY, 1)), date(2014, JULY, i));
      assertEquals(test.getName(), "Day" + i);
      assertEquals(test.toString(), "Day" + i);
      assertSame(RollConvention.of(test.getName()), test);
      assertSame(RollConvention.of("DAY" + i), test);
    }
  }

  public void test_ofDayOfMonth_31() {
    assertEquals(RollConvention.ofDayOfMonth(31), EOM);
  }

  public void test_ofDayOfMonth_invalid() {
    assertThrowsIllegalArg(() -> RollConvention.ofDayOfMonth(0));
    assertThrowsIllegalArg(() -> RollConvention.ofDayOfMonth(32));
  }

  public void test_ofDayOfMonth_adjust_Day29() {
    assertEquals(RollConvention.ofDayOfMonth(29).adjust(date(2014, FEBRUARY, 2)), date(2014, FEBRUARY, 28));
    assertEquals(RollConvention.ofDayOfMonth(29).adjust(date(2016, FEBRUARY, 2)), date(2016, FEBRUARY, 29));
  }

  public void test_ofDayOfMonth_adjust_Day30() {
    assertEquals(RollConvention.ofDayOfMonth(30).adjust(date(2014, FEBRUARY, 2)), date(2014, FEBRUARY, 28));
    assertEquals(RollConvention.ofDayOfMonth(30).adjust(date(2016, FEBRUARY, 2)), date(2016, FEBRUARY, 29));
  }

  public void test_ofDayOfMonth_matches_Day29() {
    assertEquals(RollConvention.ofDayOfMonth(29).matches(date(2016, JANUARY, 30)), false);
    assertEquals(RollConvention.ofDayOfMonth(29).matches(date(2016, JANUARY, 29)), true);
    assertEquals(RollConvention.ofDayOfMonth(29).matches(date(2016, JANUARY, 30)), false);

    assertEquals(RollConvention.ofDayOfMonth(29).matches(date(2016, FEBRUARY, 28)), false);
    assertEquals(RollConvention.ofDayOfMonth(29).matches(date(2016, FEBRUARY, 29)), true);

    assertEquals(RollConvention.ofDayOfMonth(29).matches(date(2015, FEBRUARY, 27)), false);
    assertEquals(RollConvention.ofDayOfMonth(29).matches(date(2015, FEBRUARY, 28)), true);
  }

  public void test_ofDayOfMonth_matches_Day30() {
    assertEquals(RollConvention.ofDayOfMonth(30).matches(date(2016, JANUARY, 29)), false);
    assertEquals(RollConvention.ofDayOfMonth(30).matches(date(2016, JANUARY, 30)), true);
    assertEquals(RollConvention.ofDayOfMonth(30).matches(date(2016, JANUARY, 31)), false);

    assertEquals(RollConvention.ofDayOfMonth(30).matches(date(2016, FEBRUARY, 28)), false);
    assertEquals(RollConvention.ofDayOfMonth(30).matches(date(2016, FEBRUARY, 29)), true);

    assertEquals(RollConvention.ofDayOfMonth(30).matches(date(2015, FEBRUARY, 27)), false);
    assertEquals(RollConvention.ofDayOfMonth(30).matches(date(2015, FEBRUARY, 28)), true);
  }

  public void test_ofDayOfMonth_next_oneMonth() {
    for (int start = 1; start <= 5; start++) {
      for (int i = 1; i <= 30; i++) {
        RollConvention test = RollConvention.ofDayOfMonth(i);
        LocalDate expected = date(2014, AUGUST, i);
        assertEquals(test.next(date(2014, JULY, start), P1M), expected);
      }
    }
  }

  public void test_ofDayOfMonth_next_oneDay() {
    for (int start = 1; start <= 5; start++) {
      for (int i = 1; i <= 30; i++) {
        RollConvention test = RollConvention.ofDayOfMonth(i);
        LocalDate expected = date(2014, JULY, i);
        if (i <= start) {
          expected = expected.plusMonths(1);
        }
        assertEquals(test.next(date(2014, JULY, start), P1D), expected);
      }
    }
  }

  public void test_ofDayOfMonth_previous_oneMonth() {
    for (int start = 1; start <= 5; start++) {
      for (int i = 1; i <= 30; i++) {
        RollConvention test = RollConvention.ofDayOfMonth(i);
        LocalDate expected = date(2014, JUNE, i);
        assertEquals(test.previous(date(2014, JULY, start), P1M), expected);
      }
    }
  }

  public void test_ofDayOfMonth_previous_oneDay() {
    for (int start = 1; start <= 5; start++) {
      for (int i = 1; i <= 30; i++) {
        RollConvention test = RollConvention.ofDayOfMonth(i);
        LocalDate expected = date(2014, JULY, i);
        if (i >= start) {
          expected = expected.minusMonths(1);
        }
        assertEquals(test.previous(date(2014, JULY, start), P1D), expected);
      }
    }
  }

  //-------------------------------------------------------------------------
  public void test_dayOfWeek_constants() {
    assertEquals(RollConventions.DAY_MON.adjust(date(2014, AUGUST, 11)), date(2014, AUGUST, 11));
    assertEquals(RollConventions.DAY_TUE.adjust(date(2014, AUGUST, 11)), date(2014, AUGUST, 12));
    assertEquals(RollConventions.DAY_WED.adjust(date(2014, AUGUST, 11)), date(2014, AUGUST, 13));
    assertEquals(RollConventions.DAY_THU.adjust(date(2014, AUGUST, 11)), date(2014, AUGUST, 14));
    assertEquals(RollConventions.DAY_FRI.adjust(date(2014, AUGUST, 11)), date(2014, AUGUST, 15));
    assertEquals(RollConventions.DAY_SAT.adjust(date(2014, AUGUST, 11)), date(2014, AUGUST, 16));
    assertEquals(RollConventions.DAY_SUN.adjust(date(2014, AUGUST, 11)), date(2014, AUGUST, 17));
  }

  //-------------------------------------------------------------------------
  public void test_ofDayOfWeek() {
    for (DayOfWeek dow : DayOfWeek.values()) {
      RollConvention test = RollConvention.ofDayOfWeek(dow);
      assertEquals(test.getName(), "Day" +
          CaseFormat.UPPER_UNDERSCORE.converterTo(CaseFormat.UPPER_CAMEL).convert(dow.toString()).substring(0, 3));
      assertEquals(test.toString(), "Day" +
          CaseFormat.UPPER_UNDERSCORE.converterTo(CaseFormat.UPPER_CAMEL).convert(dow.toString()).substring(0, 3));
      assertSame(RollConvention.of(test.getName()), test);
      assertSame(RollConvention.of("DAY" + dow.toString().substring(0, 3)), test);
    }
  }

  public void test_ofDayOfWeek_adjust() {
    for (DayOfWeek dow : DayOfWeek.values()) {
      RollConvention test = RollConvention.ofDayOfWeek(dow);
      assertEquals(
          test.adjust(date(2014, AUGUST, 14)),
          date(2014, AUGUST, 14).with(TemporalAdjusters.nextOrSame(dow)));
    }
  }

  public void test_ofDayOfWeek_matches() {
    assertEquals(RollConvention.ofDayOfWeek(TUESDAY).matches(date(2014, SEPTEMBER, 1)), false);
    assertEquals(RollConvention.ofDayOfWeek(TUESDAY).matches(date(2014, SEPTEMBER, 2)), true);
    assertEquals(RollConvention.ofDayOfWeek(TUESDAY).matches(date(2014, SEPTEMBER, 3)), false);
  }

  public void test_ofDayOfWeek_next_oneMonth() {
    for (DayOfWeek dow : DayOfWeek.values()) {
      RollConvention test = RollConvention.ofDayOfWeek(dow);
      assertEquals(
          test.next(date(2014, AUGUST, 14), P1W),
          date(2014, AUGUST, 21).with(TemporalAdjusters.nextOrSame(dow)));
    }
  }

  public void test_ofDayOfWeek_next_oneDay() {
    for (DayOfWeek dow : DayOfWeek.values()) {
      RollConvention test = RollConvention.ofDayOfWeek(dow);
      assertEquals(
          test.next(date(2014, AUGUST, 14), P1D),
          date(2014, AUGUST, 15).with(TemporalAdjusters.nextOrSame(dow)));
    }
  }

  public void test_ofDayOfWeek_previous_oneMonth() {
    for (DayOfWeek dow : DayOfWeek.values()) {
      RollConvention test = RollConvention.ofDayOfWeek(dow);
      assertEquals(
          test.previous(date(2014, AUGUST, 14), P1W),
          date(2014, AUGUST, 7).with(TemporalAdjusters.previousOrSame(dow)));
    }
  }

  public void test_ofDayOfWeek_previous_oneDay() {
    for (DayOfWeek dow : DayOfWeek.values()) {
      RollConvention test = RollConvention.ofDayOfWeek(dow);
      assertEquals(
          test.previous(date(2014, AUGUST, 14), P1D),
          date(2014, AUGUST, 13).with(TemporalAdjusters.previousOrSame(dow)));
    }
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
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

  @Test(dataProvider = "name")
  public void test_name(RollConvention convention, String name) {
    assertEquals(convention.getName(), name);
  }

  @Test(dataProvider = "name")
  public void test_toString(RollConvention convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(RollConvention convention, String name) {
    assertEquals(RollConvention.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_extendedEnum(RollConvention convention, String name) {
    ImmutableMap<String, RollConvention> map = RollConvention.extendedEnum().lookupAll();
    assertEquals(map.get(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> RollConvention.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> RollConvention.of(null));
  }

  //-------------------------------------------------------------------------
  public void test_equals() {
    RollConvention a = RollConventions.EOM;
    RollConvention b = RollConventions.DAY_1;
    RollConvention c = RollConventions.DAY_WED;

    assertEquals(a.equals(a), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(c), false);

    assertEquals(b.equals(a), false);
    assertEquals(b.equals(b), true);
    assertEquals(b.equals(c), false);

    assertEquals(c.equals(a), false);
    assertEquals(c.equals(b), false);
    assertEquals(c.equals(c), true);

    assertEquals(a.hashCode(), a.hashCode());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(RollConventions.class);
    coverEnum(StandardRollConventions.class);
  }

  public void test_serialization() {
    assertSerialization(EOM);
    assertSerialization(DAY_2);
    assertSerialization(DAY_THU);
  }

  public void test_jodaConvert() {
    assertJodaConvert(RollConvention.class, NONE);
    assertJodaConvert(RollConvention.class, EOM);
  }

}
