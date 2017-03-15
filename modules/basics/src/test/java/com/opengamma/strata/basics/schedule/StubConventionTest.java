/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.schedule;

import static com.opengamma.strata.basics.schedule.Frequency.P1M;
import static com.opengamma.strata.basics.schedule.Frequency.P2W;
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
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.collect.TestHelper.date;
import static java.time.Month.APRIL;
import static java.time.Month.AUGUST;
import static java.time.Month.JANUARY;
import static java.time.Month.JULY;
import static java.time.Month.JUNE;
import static java.time.Month.OCTOBER;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link StubConvention}.
 */
@Test
public class StubConventionTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "types")
  static Object[][] data_types() {
    StubConvention[] conv = StubConvention.values();
    Object[][] result = new Object[conv.length][];
    for (int i = 0; i < conv.length; i++) {
      result[i] = new Object[] {conv[i]};
    }
    return result;
  }

  @Test(dataProvider = "types")
  public void test_null(StubConvention type) {
    assertThrowsIllegalArg(() -> type.toRollConvention(null, date(2014, JULY, 1), Frequency.P3M, true));
    assertThrowsIllegalArg(() -> type.toRollConvention(date(2014, JULY, 1), null, Frequency.P3M, true));
    assertThrowsIllegalArg(() -> type.toRollConvention(date(2014, JULY, 1), date(2014, OCTOBER, 1), null, true));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "roll")
  static Object[][] data_roll() {
    return new Object[][] {
        {NONE, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P1M, false, DAY_14},
        {NONE, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P1M, true, DAY_14},

        {NONE, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P2W, false, DAY_TUE},
        {NONE, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P2W, true, DAY_TUE},
        {NONE, date(2014, JANUARY, 14), date(2014, AUGUST, 16), TERM, false, RollConventions.NONE},
        {NONE, date(2014, JANUARY, 14), date(2014, AUGUST, 16), TERM, true, RollConventions.NONE},
        {NONE, date(2014, JANUARY, 31), date(2014, APRIL, 30), P1M, true, RollConventions.EOM},
        {NONE, date(2014, APRIL, 30), date(2014, AUGUST, 31), P1M, true, RollConventions.EOM},

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

        {BOTH, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P1M, false, DAY_14},
        {BOTH, date(2014, JANUARY, 14), date(2014, AUGUST, 16), P1M, true, DAY_14},
    };
  }

  @Test(dataProvider = "roll")
  public void test_toRollConvention(
      StubConvention conv, LocalDate start, LocalDate end, Frequency freq, boolean eom, RollConvention expected) {
    assertEquals(conv.toRollConvention(start, end, freq, eom), expected);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "implicit")
  static Object[][] data_implicit() {
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

        {SHORT_FINAL, false, false, SHORT_FINAL},
        {SHORT_FINAL, true, false, null},
        {SHORT_FINAL, false, true, NONE},
        {SHORT_FINAL, true, true, null},

        {LONG_FINAL, false, false, LONG_FINAL},
        {LONG_FINAL, true, false, null},
        {LONG_FINAL, false, true, NONE},
        {LONG_FINAL, true, true, null},

        {BOTH, false, false, null},
        {BOTH, true, false, null},
        {BOTH, false, true, null},
        {BOTH, true, true, NONE},
    };
  }

  @Test(dataProvider = "implicit")
  public void test_toImplicit(
      StubConvention conv, boolean initialStub, boolean finalStub, StubConvention expected) {
    if (expected == null) {
      assertThrowsIllegalArg(() -> conv.toImplicit(null, initialStub, finalStub));
    } else {
      assertEquals(conv.toImplicit(null, initialStub, finalStub), expected);
    }
  }

  //-------------------------------------------------------------------------
  public void test_NONE() {
    assertEquals(NONE.isCalculateForwards(), true);
    assertEquals(NONE.isCalculateBackwards(), false);
    assertEquals(NONE.isLong(), false);
    assertEquals(NONE.isShort(), false);
  }

  public void test_SHORT_INITIAL() {
    assertEquals(SHORT_INITIAL.isCalculateForwards(), false);
    assertEquals(SHORT_INITIAL.isCalculateBackwards(), true);
    assertEquals(SHORT_INITIAL.isLong(), false);
    assertEquals(SHORT_INITIAL.isShort(), true);
  }

  public void test_LONG_INITIAL() {
    assertEquals(LONG_INITIAL.isCalculateForwards(), false);
    assertEquals(LONG_INITIAL.isCalculateBackwards(), true);
    assertEquals(LONG_INITIAL.isLong(), true);
    assertEquals(LONG_INITIAL.isShort(), false);
  }

  public void test_SHORT_FINAL() {
    assertEquals(SHORT_FINAL.isCalculateForwards(), true);
    assertEquals(SHORT_FINAL.isCalculateBackwards(), false);
    assertEquals(SHORT_FINAL.isLong(), false);
    assertEquals(SHORT_FINAL.isShort(), true);
  }

  public void test_LONG_FINAL() {
    assertEquals(LONG_FINAL.isCalculateForwards(), true);
    assertEquals(LONG_FINAL.isCalculateBackwards(), false);
    assertEquals(LONG_FINAL.isLong(), true);
    assertEquals(LONG_FINAL.isShort(), false);
  }

  public void test_BOTH() {
    assertEquals(BOTH.isCalculateForwards(), false);
    assertEquals(BOTH.isCalculateBackwards(), false);
    assertEquals(BOTH.isLong(), false);
    assertEquals(BOTH.isShort(), false);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {NONE, "None"},
        {SHORT_INITIAL, "ShortInitial"},
        {LONG_INITIAL, "LongInitial"},
        {SHORT_FINAL, "ShortFinal"},
        {LONG_FINAL, "LongFinal"},
        {BOTH, "Both"},
    };
  }

  @Test(dataProvider = "name")
  public void test_toString(StubConvention convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(StubConvention convention, String name) {
    assertEquals(StubConvention.of(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> StubConvention.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> StubConvention.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverEnum(StubConvention.class);
  }

  public void test_serialization() {
    assertSerialization(NONE);
    assertSerialization(SHORT_FINAL);
  }

  public void test_jodaConvert() {
    assertJodaConvert(StubConvention.class, NONE);
    assertJodaConvert(StubConvention.class, SHORT_FINAL);
  }

}
