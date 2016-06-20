/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_364;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365L;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365_25;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365_ACTUAL;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_AFB;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ICMA;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_YEAR;
import static com.opengamma.strata.basics.date.DayCounts.NL_365;
import static com.opengamma.strata.basics.date.DayCounts.ONE_ONE;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_360_ISDA;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_360_PSA;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_EPLUS_360;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_E_360;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_E_360_ISDA;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360_EOM;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.assertThrowsRuntime;
import static com.opengamma.strata.collect.TestHelper.coverEnum;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.date.DayCount.ScheduleInfo;
import com.opengamma.strata.basics.schedule.Frequency;

/**
 * Test {@link DayCount}.
 */
@Test
public class DayCountTest {

  private static final LocalDate JAN_01 = LocalDate.of(2010, 1, 1);
  private static final LocalDate JAN_02 = LocalDate.of(2010, 1, 2);
  private static final LocalDate JUL_01 = LocalDate.of(2010, 7, 1);
  private static final LocalDate JAN_01_NEXT = LocalDate.of(2011, 1, 1);

  private static final double TOLERANCE_ZERO = 0d;

  //-------------------------------------------------------------------------
  @DataProvider(name = "types")
  static Object[][] data_types() {
    StandardDayCounts[] conv = StandardDayCounts.values();
    Object[][] result = new Object[conv.length][];
    for (int i = 0; i < conv.length; i++) {
      result[i] = new Object[] {conv[i]};
    }
    return result;
  }

  @Test(dataProvider = "types")
  public void test_null(DayCount type) {
    assertThrowsRuntime(() -> type.yearFraction(null, JAN_01));
    assertThrowsRuntime(() -> type.yearFraction(JAN_01, null));
    assertThrowsRuntime(() -> type.yearFraction(null, null));
    assertThrowsRuntime(() -> type.days(null, JAN_01));
    assertThrowsRuntime(() -> type.days(JAN_01, null));
    assertThrowsRuntime(() -> type.days(null, null));
  }

  @Test(dataProvider = "types")
  public void test_wrongOrder(DayCount type) {
    assertThrowsIllegalArg(() -> type.yearFraction(JAN_02, JAN_01));
    assertThrowsIllegalArg(() -> type.days(JAN_02, JAN_01));
  }

  @Test(dataProvider = "types")
  public void test_same(DayCount type) {
    if (type != ONE_ONE) {
      assertEquals(type.yearFraction(JAN_02, JAN_02), 0d, TOLERANCE_ZERO);
      assertEquals(type.days(JAN_02, JAN_02), 0);
    }
  }

  @Test(dataProvider = "types")
  public void test_halfYear(DayCount type) {
    // sanity check to ensure that half year has fraction close to half
    if (type != ONE_ONE) {
      ScheduleInfo info = new Info(JAN_01, JAN_01_NEXT, JAN_01_NEXT, false, P12M);
      assertEquals(type.yearFraction(JAN_01, JUL_01, info), 0.5d, 0.01d);
      assertEquals(type.days(JAN_01, JUL_01), 182, 2);
    }
  }

  @Test(dataProvider = "types")
  public void test_wholeYear(DayCount type) {
    // sanity check to ensure that one year has fraction close to one
    if (type != ONE_ONE) {
      ScheduleInfo info = new Info(JAN_01, JAN_01_NEXT, JAN_01_NEXT, false, P12M);
      assertEquals(type.yearFraction(JAN_01, JAN_01_NEXT, info), 1d, 0.02d);
      assertEquals(type.days(JAN_01, JAN_01_NEXT), 365, 5);
    }
  }

  //-------------------------------------------------------------------------
  // use flag to make it clearer when an adjustment is happening
  private static Double SIMPLE_30_360 = new Double(Double.NaN);

  private static int SIMPLE_30_360Days = 0;

  @DataProvider(name = "yearFraction")
  static Object[][] data_yearFraction() {
    return new Object[][] {
        {ONE_ONE, 2011, 12, 28, 2012, 2, 28, 1d},
        {ONE_ONE, 2011, 12, 28, 2012, 2, 29, 1d},
        {ONE_ONE, 2011, 12, 28, 2012, 3, 1, 1d},
        {ONE_ONE, 2011, 12, 28, 2016, 2, 28, 1d},
        {ONE_ONE, 2011, 12, 28, 2016, 2, 29, 1d},
        {ONE_ONE, 2011, 12, 28, 2016, 3, 1, 1d},
        {ONE_ONE, 2012, 2, 29, 2012, 3, 29, 1d},
        {ONE_ONE, 2012, 2, 29, 2012, 3, 28, 1d},
        {ONE_ONE, 2012, 3, 1, 2012, 3, 28, 1d},

        //-------------------------------------------------------
        {ACT_ACT_ISDA, 2011, 12, 28, 2012, 2, 28, (4d / 365d + 58d / 366d)},
        {ACT_ACT_ISDA, 2011, 12, 28, 2012, 2, 29, (4d / 365d + 59d / 366d)},
        {ACT_ACT_ISDA, 2011, 12, 28, 2012, 3, 1, (4d / 365d + 60d / 366d)},
        {ACT_ACT_ISDA, 2011, 12, 28, 2016, 2, 28, (4d / 365d + 58d / 366d + 4)},
        {ACT_ACT_ISDA, 2011, 12, 28, 2016, 2, 29, (4d / 365d + 59d / 366d + 4)},
        {ACT_ACT_ISDA, 2011, 12, 28, 2016, 3, 1, (4d / 365d + 60d / 366d + 4)},
        {ACT_ACT_ISDA, 2012, 2, 29, 2012, 3, 29, 29d / 366d},
        {ACT_ACT_ISDA, 2012, 2, 29, 2012, 3, 28, 28d / 366d},
        {ACT_ACT_ISDA, 2012, 3, 1, 2012, 3, 28, 27d / 366d},

        //-------------------------------------------------------
        {ACT_ACT_AFB, 2011, 12, 28, 2012, 2, 28, (62d / 365d)},
        {ACT_ACT_AFB, 2011, 12, 28, 2012, 2, 29, (63d / 365d)},
        {ACT_ACT_AFB, 2011, 12, 28, 2012, 3, 1, (64d / 366d)},
        {ACT_ACT_AFB, 2011, 12, 28, 2016, 2, 28, (62d / 365d) + 4},
        {ACT_ACT_AFB, 2011, 12, 28, 2016, 2, 29, (63d / 365d) + 4},
        {ACT_ACT_AFB, 2011, 12, 28, 2016, 3, 1, (64d / 366d) + 4},
        {ACT_ACT_AFB, 2012, 2, 28, 2012, 3, 28, 29d / 366d},
        {ACT_ACT_AFB, 2012, 2, 29, 2012, 3, 28, 28d / 366d},
        {ACT_ACT_AFB, 2012, 3, 1, 2012, 3, 28, 27d / 365d},

        //-------------------------------------------------------
        {ACT_ACT_YEAR, 2011, 12, 28, 2012, 2, 28, (62d / 366d)},
        {ACT_ACT_YEAR, 2011, 12, 28, 2012, 2, 29, (63d / 366d)},
        {ACT_ACT_YEAR, 2011, 12, 28, 2012, 3, 1, (64d / 366d)},
        {ACT_ACT_YEAR, 2011, 12, 28, 2016, 2, 28, (62d / 366d) + 4},
        {ACT_ACT_YEAR, 2011, 12, 28, 2016, 2, 29, (63d / 366d) + 4},
        {ACT_ACT_YEAR, 2011, 12, 28, 2016, 3, 1, (64d / 366d) + 4},
        {ACT_ACT_YEAR, 2012, 2, 28, 2012, 3, 28, 29d / 366d},
        {ACT_ACT_YEAR, 2012, 2, 29, 2012, 3, 28, 28d / 365d},
        {ACT_ACT_YEAR, 2012, 3, 1, 2012, 3, 28, 27d / 365d},

        {ACT_ACT_YEAR, 2011, 2, 28, 2011, 3, 2, (2d / 365d)},
        {ACT_ACT_YEAR, 2011, 3, 1, 2011, 3, 2, (1d / 366d)},

        {ACT_ACT_YEAR, 2012, 2, 28, 2016, 3, 2, (3d / 366d) + 4},
        {ACT_ACT_YEAR, 2012, 2, 29, 2016, 3, 2, (2d / 365d) + 4},

        //-------------------------------------------------------
        {ACT_365_ACTUAL, 2011, 12, 28, 2012, 2, 28, (62d / 365d)},
        {ACT_365_ACTUAL, 2011, 12, 28, 2012, 2, 29, (63d / 366d)},
        {ACT_365_ACTUAL, 2011, 12, 28, 2012, 3, 1, (64d / 366d)},
        {ACT_365_ACTUAL, 2011, 12, 28, 2016, 2, 28, ((62d + 366d + 365d + 365d + 365d) / 366d)},
        {ACT_365_ACTUAL, 2011, 12, 28, 2016, 2, 29, ((63d + 366d + 365d + 365d + 365d) / 366d)},
        {ACT_365_ACTUAL, 2011, 12, 28, 2016, 3, 1, ((64d + 366d + 365d + 365d + 365d) / 366d)},
        {ACT_365_ACTUAL, 2012, 2, 28, 2012, 3, 28, 29d / 366d},
        {ACT_365_ACTUAL, 2012, 2, 29, 2012, 3, 28, 28d / 365d},
        {ACT_365_ACTUAL, 2012, 3, 1, 2012, 3, 28, 27d / 365d},

        //-------------------------------------------------------
        {ACT_360, 2011, 12, 28, 2012, 2, 28, (62d / 360d)},
        {ACT_360, 2011, 12, 28, 2012, 2, 29, (63d / 360d)},
        {ACT_360, 2011, 12, 28, 2012, 3, 1, (64d / 360d)},
        {ACT_360, 2011, 12, 28, 2016, 2, 28, ((62d + 366d + 365d + 365d + 365d) / 360d)},
        {ACT_360, 2011, 12, 28, 2016, 2, 29, ((63d + 366d + 365d + 365d + 365d) / 360d)},
        {ACT_360, 2011, 12, 28, 2016, 3, 1, ((64d + 366d + 365d + 365d + 365d) / 360d)},
        {ACT_360, 2012, 2, 28, 2012, 3, 28, 29d / 360d},
        {ACT_360, 2012, 2, 29, 2012, 3, 28, 28d / 360d},
        {ACT_360, 2012, 3, 1, 2012, 3, 28, 27d / 360d},

        //-------------------------------------------------------
        {ACT_364, 2011, 12, 28, 2012, 2, 28, (62d / 364d)},
        {ACT_364, 2011, 12, 28, 2012, 2, 29, (63d / 364d)},
        {ACT_364, 2011, 12, 28, 2012, 3, 1, (64d / 364d)},
        {ACT_364, 2011, 12, 28, 2016, 2, 28, ((62d + 366d + 365d + 365d + 365d) / 364d)},
        {ACT_364, 2011, 12, 28, 2016, 2, 29, ((63d + 366d + 365d + 365d + 365d) / 364d)},
        {ACT_364, 2011, 12, 28, 2016, 3, 1, ((64d + 366d + 365d + 365d + 365d) / 364d)},
        {ACT_364, 2012, 2, 28, 2012, 3, 28, 29d / 364d},
        {ACT_364, 2012, 2, 29, 2012, 3, 28, 28d / 364d},
        {ACT_364, 2012, 3, 1, 2012, 3, 28, 27d / 364d},

        //-------------------------------------------------------
        {ACT_365F, 2011, 12, 28, 2012, 2, 28, (62d / 365d)},
        {ACT_365F, 2011, 12, 28, 2012, 2, 29, (63d / 365d)},
        {ACT_365F, 2011, 12, 28, 2012, 3, 1, (64d / 365d)},
        {ACT_365F, 2011, 12, 28, 2016, 2, 28, ((62d + 366d + 365d + 365d + 365d) / 365d)},
        {ACT_365F, 2011, 12, 28, 2016, 2, 29, ((63d + 366d + 365d + 365d + 365d) / 365d)},
        {ACT_365F, 2011, 12, 28, 2016, 3, 1, ((64d + 366d + 365d + 365d + 365d) / 365d)},
        {ACT_365F, 2012, 2, 28, 2012, 3, 28, 29d / 365d},
        {ACT_365F, 2012, 2, 29, 2012, 3, 28, 28d / 365d},
        {ACT_365F, 2012, 3, 1, 2012, 3, 28, 27d / 365d},

        //-------------------------------------------------------
        {ACT_365_25, 2011, 12, 28, 2012, 2, 28, (62d / 365.25d)},
        {ACT_365_25, 2011, 12, 28, 2012, 2, 29, (63d / 365.25d)},
        {ACT_365_25, 2011, 12, 28, 2012, 3, 1, (64d / 365.25d)},
        {ACT_365_25, 2011, 12, 28, 2016, 2, 28, ((62d + 366d + 365d + 365d + 365d) / 365.25d)},
        {ACT_365_25, 2011, 12, 28, 2016, 2, 29, ((63d + 366d + 365d + 365d + 365d) / 365.25d)},
        {ACT_365_25, 2011, 12, 28, 2016, 3, 1, ((64d + 366d + 365d + 365d + 365d) / 365.25d)},
        {ACT_365_25, 2012, 2, 28, 2012, 3, 28, 29d / 365.25d},
        {ACT_365_25, 2012, 2, 29, 2012, 3, 28, 28d / 365.25d},
        {ACT_365_25, 2012, 3, 1, 2012, 3, 28, 27d / 365.25d},

        //-------------------------------------------------------
        {NL_365, 2011, 12, 28, 2012, 2, 28, (62d / 365d)},
        {NL_365, 2011, 12, 28, 2012, 2, 29, (62d / 365d)},
        {NL_365, 2011, 12, 28, 2012, 3, 1, (63d / 365d)},
        {NL_365, 2011, 12, 28, 2016, 2, 28, ((62d + 365d + 365d + 365d + 365d) / 365d)},
        {NL_365, 2011, 12, 28, 2016, 2, 29, ((62d + 365d + 365d + 365d + 365d) / 365d)},
        {NL_365, 2011, 12, 28, 2016, 3, 1, ((63d + 365d + 365d + 365d + 365d) / 365d)},
        {NL_365, 2012, 2, 28, 2012, 3, 28, 28d / 365d},
        {NL_365, 2012, 2, 29, 2012, 3, 28, 28d / 365d},
        {NL_365, 2012, 3, 1, 2012, 3, 28, 27d / 365d},
        {NL_365, 2011, 12, 1, 2012, 12, 1, 365d / 365d},

        //-------------------------------------------------------
        {THIRTY_360_ISDA, 2011, 12, 28, 2012, 2, 28, SIMPLE_30_360},
        {THIRTY_360_ISDA, 2011, 12, 28, 2012, 2, 29, SIMPLE_30_360},
        {THIRTY_360_ISDA, 2011, 12, 28, 2012, 3, 1, SIMPLE_30_360},
        {THIRTY_360_ISDA, 2011, 12, 28, 2016, 2, 28, SIMPLE_30_360},
        {THIRTY_360_ISDA, 2011, 12, 28, 2016, 2, 29, SIMPLE_30_360},
        {THIRTY_360_ISDA, 2011, 12, 28, 2016, 3, 1, SIMPLE_30_360},

        {THIRTY_360_ISDA, 2012, 2, 28, 2012, 3, 28, SIMPLE_30_360},
        {THIRTY_360_ISDA, 2012, 2, 29, 2012, 3, 28, SIMPLE_30_360},
        {THIRTY_360_ISDA, 2011, 2, 28, 2012, 2, 28, SIMPLE_30_360},
        {THIRTY_360_ISDA, 2011, 2, 28, 2012, 2, 29, SIMPLE_30_360},
        {THIRTY_360_ISDA, 2012, 2, 29, 2016, 2, 29, SIMPLE_30_360},

        {THIRTY_360_ISDA, 2012, 3, 1, 2012, 3, 28, SIMPLE_30_360},
        {THIRTY_360_ISDA, 2012, 5, 30, 2013, 8, 29, SIMPLE_30_360},
        {THIRTY_360_ISDA, 2012, 5, 29, 2013, 8, 30, SIMPLE_30_360},
        {THIRTY_360_ISDA, 2012, 5, 30, 2013, 8, 30, SIMPLE_30_360},
        {THIRTY_360_ISDA, 2012, 5, 29, 2013, 8, 31, SIMPLE_30_360},
        {THIRTY_360_ISDA, 2012, 5, 30, 2013, 8, 31, calc360(2012, 5, 30, 2013, 8, 30)},
        {THIRTY_360_ISDA, 2012, 5, 31, 2013, 8, 30, calc360(2012, 5, 30, 2013, 8, 30)},
        {THIRTY_360_ISDA, 2012, 5, 31, 2013, 8, 31, calc360(2012, 5, 30, 2013, 8, 30)},

        //-------------------------------------------------------
        {THIRTY_360_PSA, 2011, 12, 28, 2012, 2, 28, SIMPLE_30_360},
        {THIRTY_360_PSA, 2011, 12, 28, 2012, 2, 29, SIMPLE_30_360},
        {THIRTY_360_PSA, 2011, 12, 28, 2012, 3, 1, SIMPLE_30_360},
        {THIRTY_360_PSA, 2011, 12, 28, 2016, 2, 28, SIMPLE_30_360},
        {THIRTY_360_PSA, 2011, 12, 28, 2016, 2, 29, SIMPLE_30_360},
        {THIRTY_360_PSA, 2011, 12, 28, 2016, 3, 1, SIMPLE_30_360},

        {THIRTY_360_PSA, 2012, 2, 28, 2012, 3, 28, SIMPLE_30_360},
        {THIRTY_360_PSA, 2012, 2, 29, 2012, 3, 28, calc360(2012, 2, 30, 2012, 3, 28)},
        {THIRTY_360_PSA, 2011, 2, 28, 2012, 2, 28, calc360(2011, 2, 30, 2012, 2, 28)},
        {THIRTY_360_PSA, 2011, 2, 28, 2012, 2, 29, calc360(2011, 2, 30, 2012, 2, 29)},
        {THIRTY_360_PSA, 2012, 2, 29, 2016, 2, 29, calc360(2012, 2, 30, 2016, 2, 29)},

        {THIRTY_360_PSA, 2012, 3, 1, 2012, 3, 28, SIMPLE_30_360},
        {THIRTY_360_PSA, 2012, 5, 30, 2013, 8, 29, SIMPLE_30_360},
        {THIRTY_360_PSA, 2012, 5, 29, 2013, 8, 30, SIMPLE_30_360},
        {THIRTY_360_PSA, 2012, 5, 30, 2013, 8, 30, SIMPLE_30_360},
        {THIRTY_360_PSA, 2012, 5, 29, 2013, 8, 31, SIMPLE_30_360},
        {THIRTY_360_PSA, 2012, 5, 30, 2013, 8, 31, calc360(2012, 5, 30, 2013, 8, 30)},
        {THIRTY_360_PSA, 2012, 5, 31, 2013, 8, 30, calc360(2012, 5, 30, 2013, 8, 30)},
        {THIRTY_360_PSA, 2012, 5, 31, 2013, 8, 31, calc360(2012, 5, 30, 2013, 8, 30)},

        //-------------------------------------------------------
        {THIRTY_E_360, 2011, 12, 28, 2012, 2, 28, SIMPLE_30_360},
        {THIRTY_E_360, 2011, 12, 28, 2012, 2, 29, SIMPLE_30_360},
        {THIRTY_E_360, 2011, 12, 28, 2012, 3, 1, SIMPLE_30_360},
        {THIRTY_E_360, 2011, 12, 28, 2016, 2, 28, SIMPLE_30_360},
        {THIRTY_E_360, 2011, 12, 28, 2016, 2, 29, SIMPLE_30_360},
        {THIRTY_E_360, 2011, 12, 28, 2016, 3, 1, SIMPLE_30_360},

        {THIRTY_E_360, 2012, 2, 28, 2012, 3, 28, SIMPLE_30_360},
        {THIRTY_E_360, 2012, 2, 29, 2012, 3, 28, SIMPLE_30_360},
        {THIRTY_E_360, 2011, 2, 28, 2012, 2, 28, SIMPLE_30_360},
        {THIRTY_E_360, 2011, 2, 28, 2012, 2, 29, SIMPLE_30_360},
        {THIRTY_E_360, 2012, 2, 29, 2016, 2, 29, SIMPLE_30_360},

        {THIRTY_E_360, 2012, 3, 1, 2012, 3, 28, SIMPLE_30_360},
        {THIRTY_E_360, 2012, 5, 30, 2013, 8, 29, SIMPLE_30_360},
        {THIRTY_E_360, 2012, 5, 29, 2013, 8, 30, SIMPLE_30_360},
        {THIRTY_E_360, 2012, 5, 30, 2013, 8, 30, SIMPLE_30_360},
        {THIRTY_E_360, 2012, 5, 29, 2013, 8, 31, calc360(2012, 5, 29, 2013, 8, 30)},
        {THIRTY_E_360, 2012, 5, 30, 2013, 8, 31, calc360(2012, 5, 30, 2013, 8, 30)},
        {THIRTY_E_360, 2012, 5, 31, 2013, 8, 30, calc360(2012, 5, 30, 2013, 8, 30)},
        {THIRTY_E_360, 2012, 5, 31, 2013, 8, 31, calc360(2012, 5, 30, 2013, 8, 30)},

        //-------------------------------------------------------
        {THIRTY_EPLUS_360, 2011, 12, 28, 2012, 2, 28, SIMPLE_30_360},
        {THIRTY_EPLUS_360, 2011, 12, 28, 2012, 2, 29, SIMPLE_30_360},
        {THIRTY_EPLUS_360, 2011, 12, 28, 2012, 3, 1, SIMPLE_30_360},
        {THIRTY_EPLUS_360, 2011, 12, 28, 2016, 2, 28, SIMPLE_30_360},
        {THIRTY_EPLUS_360, 2011, 12, 28, 2016, 2, 29, SIMPLE_30_360},
        {THIRTY_EPLUS_360, 2011, 12, 28, 2016, 3, 1, SIMPLE_30_360},

        {THIRTY_EPLUS_360, 2012, 2, 28, 2012, 3, 28, SIMPLE_30_360},
        {THIRTY_EPLUS_360, 2012, 2, 29, 2012, 3, 28, SIMPLE_30_360},
        {THIRTY_EPLUS_360, 2012, 3, 1, 2012, 3, 28, SIMPLE_30_360},
        {THIRTY_EPLUS_360, 2011, 2, 28, 2012, 2, 28, SIMPLE_30_360},
        {THIRTY_EPLUS_360, 2011, 2, 28, 2012, 2, 29, SIMPLE_30_360},
        {THIRTY_EPLUS_360, 2012, 2, 29, 2016, 2, 29, SIMPLE_30_360},

        {THIRTY_EPLUS_360, 2012, 3, 1, 2012, 3, 28, SIMPLE_30_360},
        {THIRTY_EPLUS_360, 2012, 5, 30, 2013, 8, 29, SIMPLE_30_360},
        {THIRTY_EPLUS_360, 2012, 5, 29, 2013, 8, 30, SIMPLE_30_360},
        {THIRTY_EPLUS_360, 2012, 5, 30, 2013, 8, 30, SIMPLE_30_360},
        {THIRTY_EPLUS_360, 2012, 5, 29, 2013, 8, 31, calc360(2012, 5, 29, 2013, 9, 1)},
        {THIRTY_EPLUS_360, 2012, 5, 30, 2013, 8, 31, calc360(2012, 5, 30, 2013, 9, 1)},
        {THIRTY_EPLUS_360, 2012, 5, 31, 2013, 8, 30, calc360(2012, 5, 30, 2013, 8, 30)},
        {THIRTY_EPLUS_360, 2012, 5, 31, 2013, 8, 31, calc360(2012, 5, 30, 2013, 9, 1)},
    };
  }

  private static double calc360(int y1, int m1, int d1, int y2, int m2, int d2) {
    return ((y2 - y1) * 360 + (m2 - m1) * 30 + (d2 - d1)) / 360d;
  }

  private static int calc360Days(int y1, int m1, int d1, int y2, int m2, int d2) {
    return (y2 - y1) * 360 + (m2 - m1) * 30 + (d2 - d1);
  }

  @Test(dataProvider = "yearFraction")
  public void test_yearFraction(DayCount dayCount, int y1, int m1, int d1, int y2, int m2, int d2, Double value) {
    double expected = (value == SIMPLE_30_360 ? calc360(y1, m1, d1, y2, m2, d2) : value);
    LocalDate date1 = LocalDate.of(y1, m1, d1);
    LocalDate date2 = LocalDate.of(y2, m2, d2);
    assertEquals(dayCount.yearFraction(date1, date2), expected, TOLERANCE_ZERO);
  }

  @Test(dataProvider = "yearFraction")
  public void test_relativeYearFraction(
      DayCount dayCount, int y1, int m1, int d1, int y2, int m2, int d2, Double value) {
    double expected = (value == SIMPLE_30_360 ? calc360(y1, m1, d1, y2, m2, d2) : value);
    LocalDate date1 = LocalDate.of(y1, m1, d1);
    LocalDate date2 = LocalDate.of(y2, m2, d2);
    assertEquals(dayCount.relativeYearFraction(date1, date2), expected, TOLERANCE_ZERO);
  }

  @Test(dataProvider = "yearFraction")
  public void test_relativeYearFraction_reverse(
      DayCount dayCount, int y1, int m1, int d1, int y2, int m2, int d2, Double value) {
    double expected = (value == SIMPLE_30_360 ? calc360(y1, m1, d1, y2, m2, d2) : value);
    LocalDate date1 = LocalDate.of(y1, m1, d1);
    LocalDate date2 = LocalDate.of(y2, m2, d2);
    assertEquals(dayCount.relativeYearFraction(date2, date1), -expected, TOLERANCE_ZERO);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "days")
  static Object[][] data_days() {
    return new Object[][] {
        {ONE_ONE, 2011, 12, 28, 2012, 2, 28, 1},
        {ONE_ONE, 2011, 12, 28, 2012, 2, 29, 1},
        {ONE_ONE, 2011, 12, 28, 2012, 3, 1, 1},
        {ONE_ONE, 2011, 12, 28, 2016, 2, 28, 1},
        {ONE_ONE, 2011, 12, 28, 2016, 2, 29, 1},
        {ONE_ONE, 2011, 12, 28, 2016, 3, 1, 1},
        {ONE_ONE, 2012, 2, 29, 2012, 3, 29, 1},
        {ONE_ONE, 2012, 2, 29, 2012, 3, 28, 1},
        {ONE_ONE, 2012, 3, 1, 2012, 3, 28, 1},

        //-------------------------------------------------------
        {ACT_ACT_ISDA, 2011, 12, 28, 2012, 2, 28, 62},
        {ACT_ACT_ISDA, 2011, 12, 28, 2012, 2, 29, 63},
        {ACT_ACT_ISDA, 2011, 12, 28, 2012, 3, 1, 64},
        {ACT_ACT_ISDA, 2011, 12, 28, 2016, 2, 28, 1523},
        {ACT_ACT_ISDA, 2011, 12, 28, 2016, 2, 29, 1524},
        {ACT_ACT_ISDA, 2011, 12, 28, 2016, 3, 1, 1525},

        //-------------------------------------------------------
        {ACT_ACT_AFB, 2011, 12, 28, 2012, 2, 28, 62},
        {ACT_ACT_AFB, 2011, 12, 28, 2012, 2, 29, 63},
        {ACT_ACT_AFB, 2011, 12, 28, 2012, 3, 1, 64},
        {ACT_ACT_AFB, 2011, 12, 28, 2016, 2, 28, 1523},
        {ACT_ACT_AFB, 2011, 12, 28, 2016, 2, 29, 1524},
        {ACT_ACT_AFB, 2011, 12, 28, 2016, 3, 1, 1525},

        //-------------------------------------------------------
        {ACT_ACT_YEAR, 2011, 12, 28, 2012, 2, 28, 62},
        {ACT_ACT_YEAR, 2011, 12, 28, 2012, 2, 29, 63},
        {ACT_ACT_YEAR, 2011, 12, 28, 2012, 3, 1, 64},
        {ACT_ACT_YEAR, 2011, 12, 28, 2016, 2, 28, 1523},
        {ACT_ACT_YEAR, 2011, 12, 28, 2016, 2, 29, 1524},
        {ACT_ACT_YEAR, 2011, 12, 28, 2016, 3, 1, 1525},

        //-------------------------------------------------------
        {ACT_365_ACTUAL, 2011, 12, 28, 2012, 2, 28, 62},
        {ACT_365_ACTUAL, 2011, 12, 28, 2012, 2, 29, 63},
        {ACT_365_ACTUAL, 2011, 12, 28, 2012, 3, 1, 64},
        {ACT_365_ACTUAL, 2011, 12, 28, 2016, 2, 28, 62 + 366 + 365 + 365 + 365},
        {ACT_365_ACTUAL, 2011, 12, 28, 2016, 2, 29, 63 + 366 + 365 + 365 + 365},
        {ACT_365_ACTUAL, 2011, 12, 28, 2016, 3, 1, 64 + 366 + 365 + 365 + 365},
        {ACT_365_ACTUAL, 2012, 2, 28, 2012, 3, 28, 29},
        {ACT_365_ACTUAL, 2012, 2, 29, 2012, 3, 28, 28},
        {ACT_365_ACTUAL, 2012, 3, 1, 2012, 3, 28, 27},

        //-------------------------------------------------------
        {ACT_360, 2011, 12, 28, 2012, 2, 28, 62},
        {ACT_360, 2011, 12, 28, 2012, 2, 29, 63},
        {ACT_360, 2011, 12, 28, 2012, 3, 1, 64},
        {ACT_360, 2011, 12, 28, 2016, 2, 28, 62 + 366 + 365 + 365 + 365},
        {ACT_360, 2011, 12, 28, 2016, 2, 29, 63 + 366 + 365 + 365 + 365},
        {ACT_360, 2011, 12, 28, 2016, 3, 1, 64 + 366 + 365 + 365 + 365},

        //-------------------------------------------------------
        {ACT_364, 2011, 12, 28, 2012, 2, 28, 62},
        {ACT_364, 2011, 12, 28, 2012, 2, 29, 63},
        {ACT_364, 2011, 12, 28, 2012, 3, 1, 64},
        {ACT_364, 2011, 12, 28, 2016, 2, 28, 62 + 366 + 365 + 365 + 365},
        {ACT_364, 2011, 12, 28, 2016, 2, 29, 63 + 366 + 365 + 365 + 365},
        {ACT_364, 2011, 12, 28, 2016, 3, 1, 64 + 366 + 365 + 365 + 365},
        {ACT_364, 2012, 2, 28, 2012, 3, 28, 29},
        {ACT_364, 2012, 2, 29, 2012, 3, 28, 28},
        {ACT_364, 2012, 3, 1, 2012, 3, 28, 27},

        //-------------------------------------------------------
        {ACT_365F, 2011, 12, 28, 2012, 2, 28, 62},
        {ACT_365F, 2011, 12, 28, 2012, 2, 29, 63},
        {ACT_365F, 2011, 12, 28, 2012, 3, 1, 64},
        {ACT_365F, 2011, 12, 28, 2016, 2, 28, 62 + 366 + 365 + 365 + 365},
        {ACT_365F, 2011, 12, 28, 2016, 2, 29, 63 + 366 + 365 + 365 + 365},
        {ACT_365F, 2011, 12, 28, 2016, 3, 1, 64 + 366 + 365 + 365 + 365},
        {ACT_365F, 2012, 2, 28, 2012, 3, 28, 29},
        {ACT_365F, 2012, 2, 29, 2012, 3, 28, 28},
        {ACT_365F, 2012, 3, 1, 2012, 3, 28, 27},

        //-------------------------------------------------------
        {ACT_365_25, 2011, 12, 28, 2012, 2, 28, 62},
        {ACT_365_25, 2011, 12, 28, 2012, 2, 29, 63},
        {ACT_365_25, 2011, 12, 28, 2012, 3, 1, 64},
        {ACT_365_25, 2011, 12, 28, 2016, 2, 28, 62 + 366 + 365 + 365 + 365},
        {ACT_365_25, 2011, 12, 28, 2016, 2, 29, 63 + 366 + 365 + 365 + 365},
        {ACT_365_25, 2011, 12, 28, 2016, 3, 1, 64 + 366 + 365 + 365 + 365},
        {ACT_365_25, 2012, 2, 28, 2012, 3, 28, 29},
        {ACT_365_25, 2012, 2, 29, 2012, 3, 28, 28},
        {ACT_365_25, 2012, 3, 1, 2012, 3, 28, 27},

        //-------------------------------------------------------
        {NL_365, 2011, 12, 28, 2012, 2, 28, 62},
        {NL_365, 2011, 12, 28, 2012, 2, 29, 62},
        {NL_365, 2011, 12, 28, 2012, 3, 1, 63},
        {NL_365, 2011, 12, 28, 2016, 2, 28, 62 + 365 + 365 + 365 + 365},
        {NL_365, 2011, 12, 28, 2016, 2, 29, 62 + 365 + 365 + 365 + 365},
        {NL_365, 2011, 12, 28, 2016, 3, 1, 63 + 365 + 365 + 365 + 365},
        {NL_365, 2012, 2, 28, 2012, 3, 28, 28},
        {NL_365, 2012, 2, 29, 2012, 3, 28, 28},
        {NL_365, 2012, 3, 1, 2012, 3, 28, 27},
        {NL_365, 2011, 12, 1, 2012, 12, 1, 365},

        //-------------------------------------------------------
        {THIRTY_360_ISDA, 2011, 12, 28, 2012, 2, 28, SIMPLE_30_360Days},
        {THIRTY_360_ISDA, 2011, 12, 28, 2012, 2, 29, SIMPLE_30_360Days},
        {THIRTY_360_ISDA, 2011, 12, 28, 2012, 3, 1, SIMPLE_30_360Days},
        {THIRTY_360_ISDA, 2011, 12, 28, 2016, 2, 28, SIMPLE_30_360Days},
        {THIRTY_360_ISDA, 2011, 12, 28, 2016, 2, 29, SIMPLE_30_360Days},
        {THIRTY_360_ISDA, 2011, 12, 28, 2016, 3, 1, SIMPLE_30_360Days},

        {THIRTY_360_ISDA, 2012, 2, 28, 2012, 3, 28, SIMPLE_30_360Days},
        {THIRTY_360_ISDA, 2012, 2, 29, 2012, 3, 28, SIMPLE_30_360Days},
        {THIRTY_360_ISDA, 2011, 2, 28, 2012, 2, 28, SIMPLE_30_360Days},
        {THIRTY_360_ISDA, 2011, 2, 28, 2012, 2, 29, SIMPLE_30_360Days},
        {THIRTY_360_ISDA, 2012, 2, 29, 2016, 2, 29, SIMPLE_30_360Days},

        {THIRTY_360_ISDA, 2012, 3, 1, 2012, 3, 28, SIMPLE_30_360Days},
        {THIRTY_360_ISDA, 2012, 5, 30, 2013, 8, 29, SIMPLE_30_360Days},
        {THIRTY_360_ISDA, 2012, 5, 29, 2013, 8, 30, SIMPLE_30_360Days},
        {THIRTY_360_ISDA, 2012, 5, 30, 2013, 8, 30, SIMPLE_30_360Days},
        {THIRTY_360_ISDA, 2012, 5, 29, 2013, 8, 31, SIMPLE_30_360Days},
        {THIRTY_360_ISDA, 2012, 5, 30, 2013, 8, 31, calc360Days(2012, 5, 30, 2013, 8, 30)},
        {THIRTY_360_ISDA, 2012, 5, 31, 2013, 8, 30, calc360Days(2012, 5, 30, 2013, 8, 30)},
        {THIRTY_360_ISDA, 2012, 5, 31, 2013, 8, 31, calc360Days(2012, 5, 30, 2013, 8, 30)},

        //-------------------------------------------------------
        {THIRTY_360_PSA, 2011, 12, 28, 2012, 2, 28, SIMPLE_30_360Days},
        {THIRTY_360_PSA, 2011, 12, 28, 2012, 2, 29, SIMPLE_30_360Days},
        {THIRTY_360_PSA, 2011, 12, 28, 2012, 3, 1, SIMPLE_30_360Days},
        {THIRTY_360_PSA, 2011, 12, 28, 2016, 2, 28, SIMPLE_30_360Days},
        {THIRTY_360_PSA, 2011, 12, 28, 2016, 2, 29, SIMPLE_30_360Days},
        {THIRTY_360_PSA, 2011, 12, 28, 2016, 3, 1, SIMPLE_30_360Days},

        {THIRTY_360_PSA, 2012, 2, 28, 2012, 3, 28, SIMPLE_30_360Days},
        {THIRTY_360_PSA, 2012, 2, 29, 2012, 3, 28, calc360Days(2012, 2, 30, 2012, 3, 28)},
        {THIRTY_360_PSA, 2011, 2, 28, 2012, 2, 28, calc360Days(2011, 2, 30, 2012, 2, 28)},
        {THIRTY_360_PSA, 2011, 2, 28, 2012, 2, 29, calc360Days(2011, 2, 30, 2012, 2, 29)},
        {THIRTY_360_PSA, 2012, 2, 29, 2016, 2, 29, calc360Days(2012, 2, 30, 2016, 2, 29)},

        {THIRTY_360_PSA, 2012, 3, 1, 2012, 3, 28, SIMPLE_30_360Days},
        {THIRTY_360_PSA, 2012, 5, 30, 2013, 8, 29, SIMPLE_30_360Days},
        {THIRTY_360_PSA, 2012, 5, 29, 2013, 8, 30, SIMPLE_30_360Days},
        {THIRTY_360_PSA, 2012, 5, 30, 2013, 8, 30, SIMPLE_30_360Days},
        {THIRTY_360_PSA, 2012, 5, 29, 2013, 8, 31, SIMPLE_30_360Days},
        {THIRTY_360_PSA, 2012, 5, 30, 2013, 8, 31, calc360Days(2012, 5, 30, 2013, 8, 30)},
        {THIRTY_360_PSA, 2012, 5, 31, 2013, 8, 30, calc360Days(2012, 5, 30, 2013, 8, 30)},
        {THIRTY_360_PSA, 2012, 5, 31, 2013, 8, 31, calc360Days(2012, 5, 30, 2013, 8, 30)},

        //-------------------------------------------------------
        {THIRTY_E_360, 2011, 12, 28, 2012, 2, 28, SIMPLE_30_360Days},
        {THIRTY_E_360, 2011, 12, 28, 2012, 2, 29, SIMPLE_30_360Days},
        {THIRTY_E_360, 2011, 12, 28, 2012, 3, 1, SIMPLE_30_360Days},
        {THIRTY_E_360, 2011, 12, 28, 2016, 2, 28, SIMPLE_30_360Days},
        {THIRTY_E_360, 2011, 12, 28, 2016, 2, 29, SIMPLE_30_360Days},
        {THIRTY_E_360, 2011, 12, 28, 2016, 3, 1, SIMPLE_30_360Days},

        {THIRTY_E_360, 2012, 2, 28, 2012, 3, 28, SIMPLE_30_360Days},
        {THIRTY_E_360, 2012, 2, 29, 2012, 3, 28, SIMPLE_30_360Days},
        {THIRTY_E_360, 2011, 2, 28, 2012, 2, 28, SIMPLE_30_360Days},
        {THIRTY_E_360, 2011, 2, 28, 2012, 2, 29, SIMPLE_30_360Days},
        {THIRTY_E_360, 2012, 2, 29, 2016, 2, 29, SIMPLE_30_360Days},

        {THIRTY_E_360, 2012, 3, 1, 2012, 3, 28, SIMPLE_30_360Days},
        {THIRTY_E_360, 2012, 5, 30, 2013, 8, 29, SIMPLE_30_360Days},
        {THIRTY_E_360, 2012, 5, 29, 2013, 8, 30, SIMPLE_30_360Days},
        {THIRTY_E_360, 2012, 5, 30, 2013, 8, 30, SIMPLE_30_360Days},
        {THIRTY_E_360, 2012, 5, 29, 2013, 8, 31, calc360Days(2012, 5, 29, 2013, 8, 30)},
        {THIRTY_E_360, 2012, 5, 30, 2013, 8, 31, calc360Days(2012, 5, 30, 2013, 8, 30)},
        {THIRTY_E_360, 2012, 5, 31, 2013, 8, 30, calc360Days(2012, 5, 30, 2013, 8, 30)},
        {THIRTY_E_360, 2012, 5, 31, 2013, 8, 31, calc360Days(2012, 5, 30, 2013, 8, 30)},

        //-------------------------------------------------------
        {THIRTY_EPLUS_360, 2011, 12, 28, 2012, 2, 28, SIMPLE_30_360Days},
        {THIRTY_EPLUS_360, 2011, 12, 28, 2012, 2, 29, SIMPLE_30_360Days},
        {THIRTY_EPLUS_360, 2011, 12, 28, 2012, 3, 1, SIMPLE_30_360Days},
        {THIRTY_EPLUS_360, 2011, 12, 28, 2016, 2, 28, SIMPLE_30_360Days},
        {THIRTY_EPLUS_360, 2011, 12, 28, 2016, 2, 29, SIMPLE_30_360Days},
        {THIRTY_EPLUS_360, 2011, 12, 28, 2016, 3, 1, SIMPLE_30_360Days},

        {THIRTY_EPLUS_360, 2012, 2, 28, 2012, 3, 28, SIMPLE_30_360Days},
        {THIRTY_EPLUS_360, 2012, 2, 29, 2012, 3, 28, SIMPLE_30_360Days},
        {THIRTY_EPLUS_360, 2012, 3, 1, 2012, 3, 28, SIMPLE_30_360Days},
        {THIRTY_EPLUS_360, 2011, 2, 28, 2012, 2, 28, SIMPLE_30_360Days},
        {THIRTY_EPLUS_360, 2011, 2, 28, 2012, 2, 29, SIMPLE_30_360Days},
        {THIRTY_EPLUS_360, 2012, 2, 29, 2016, 2, 29, SIMPLE_30_360Days},

        {THIRTY_EPLUS_360, 2012, 3, 1, 2012, 3, 28, SIMPLE_30_360Days},
        {THIRTY_EPLUS_360, 2012, 5, 30, 2013, 8, 29, SIMPLE_30_360Days},
        {THIRTY_EPLUS_360, 2012, 5, 29, 2013, 8, 30, SIMPLE_30_360Days},
        {THIRTY_EPLUS_360, 2012, 5, 30, 2013, 8, 30, SIMPLE_30_360Days},
        {THIRTY_EPLUS_360, 2012, 5, 29, 2013, 8, 31, calc360Days(2012, 5, 29, 2013, 9, 1)},
        {THIRTY_EPLUS_360, 2012, 5, 30, 2013, 8, 31, calc360Days(2012, 5, 30, 2013, 9, 1)},
        {THIRTY_EPLUS_360, 2012, 5, 31, 2013, 8, 30, calc360Days(2012, 5, 30, 2013, 8, 30)},
        {THIRTY_EPLUS_360, 2012, 5, 31, 2013, 8, 31, calc360Days(2012, 5, 30, 2013, 9, 1)},
    };
  }

  @Test(dataProvider = "days")
  public void test_days(DayCount dayCount, int y1, int m1, int d1, int y2, int m2, int d2, int value) {
    int expected = (value == SIMPLE_30_360Days ? calc360Days(y1, m1, d1, y2, m2, d2) : value);
    LocalDate date1 = LocalDate.of(y1, m1, d1);
    LocalDate date2 = LocalDate.of(y2, m2, d2);
    assertEquals(dayCount.days(date1, date2), expected, TOLERANCE_ZERO);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "30U360")
  static Object[][] data_30U360() {
    return new Object[][] {
        {2011, 12, 28, 2012, 2, 28, SIMPLE_30_360, SIMPLE_30_360},
        {2011, 12, 28, 2012, 2, 29, SIMPLE_30_360, SIMPLE_30_360},
        {2011, 12, 28, 2012, 3, 1, SIMPLE_30_360, SIMPLE_30_360},
        {2011, 12, 28, 2016, 2, 28, SIMPLE_30_360, SIMPLE_30_360},
        {2011, 12, 28, 2016, 2, 29, SIMPLE_30_360, SIMPLE_30_360},
        {2011, 12, 28, 2016, 3, 1, SIMPLE_30_360, SIMPLE_30_360},

        {2012, 2, 28, 2012, 3, 28, SIMPLE_30_360, SIMPLE_30_360},
        {2012, 2, 29, 2012, 3, 28, SIMPLE_30_360, calc360(2012, 2, 30, 2012, 3, 28)},
        {2012, 2, 29, 2012, 3, 30, SIMPLE_30_360, calc360(2012, 2, 30, 2012, 3, 30)},
        {2012, 2, 29, 2012, 3, 31, SIMPLE_30_360, calc360(2012, 2, 30, 2012, 3, 30)},
        {2012, 2, 29, 2013, 2, 28, SIMPLE_30_360, calc360(2012, 2, 30, 2013, 2, 30)},
        {2011, 2, 28, 2012, 2, 28, SIMPLE_30_360, calc360(2011, 2, 30, 2012, 2, 28)},
        {2011, 2, 28, 2012, 2, 29, SIMPLE_30_360, calc360(2011, 2, 30, 2012, 2, 30)},
        {2012, 2, 29, 2016, 2, 29, SIMPLE_30_360, calc360(2012, 2, 30, 2016, 2, 30)},

        {2012, 3, 1, 2012, 3, 28, SIMPLE_30_360, SIMPLE_30_360},
        {2012, 5, 30, 2013, 8, 29, SIMPLE_30_360, SIMPLE_30_360},
        {2012, 5, 29, 2013, 8, 30, SIMPLE_30_360, SIMPLE_30_360},
        {2012, 5, 30, 2013, 8, 30, SIMPLE_30_360, SIMPLE_30_360},
        {2012, 5, 29, 2013, 8, 31, SIMPLE_30_360, SIMPLE_30_360},
        {2012, 5, 30, 2013, 8, 31, calc360(2012, 5, 30, 2013, 8, 30), calc360(2012, 5, 30, 2013, 8, 30)},
        {2012, 5, 31, 2013, 8, 30, calc360(2012, 5, 30, 2013, 8, 30), calc360(2012, 5, 30, 2013, 8, 30)},
        {2012, 5, 31, 2013, 8, 31, calc360(2012, 5, 30, 2013, 8, 30), calc360(2012, 5, 30, 2013, 8, 30)},
    };
  }

  @Test(dataProvider = "30U360")
  public void test_yearFraction_30U360_notEom(
      int y1, int m1, int d1, int y2, int m2, int d2, Double valueNotEOM, Double valueEOM) {
    double expected = (valueNotEOM == SIMPLE_30_360 ? calc360(y1, m1, d1, y2, m2, d2) : valueNotEOM);
    LocalDate date1 = LocalDate.of(y1, m1, d1);
    LocalDate date2 = LocalDate.of(y2, m2, d2);
    ScheduleInfo info = new Info(false);
    assertEquals(THIRTY_U_360.yearFraction(date1, date2, info), expected, TOLERANCE_ZERO);
  }

  @Test(dataProvider = "30U360")
  public void test_yearFraction_30U360_eom(
      int y1, int m1, int d1, int y2, int m2, int d2, Double valueNotEOM, Double valueEOM) {
    double expected = (valueEOM == SIMPLE_30_360 ? calc360(y1, m1, d1, y2, m2, d2) : valueEOM);
    LocalDate date1 = LocalDate.of(y1, m1, d1);
    LocalDate date2 = LocalDate.of(y2, m2, d2);
    ScheduleInfo info = new Info(true);
    assertEquals(THIRTY_U_360.yearFraction(date1, date2, info), expected, TOLERANCE_ZERO);
  }

  @Test(dataProvider = "30U360")
  public void test_yearFraction_30360ISDA(
      int y1, int m1, int d1, int y2, int m2, int d2, Double valueNotEOM, Double valueEOM) {
    double expected = (valueNotEOM == SIMPLE_30_360 ? calc360(y1, m1, d1, y2, m2, d2) : valueNotEOM);
    LocalDate date1 = LocalDate.of(y1, m1, d1);
    LocalDate date2 = LocalDate.of(y2, m2, d2);
    ScheduleInfo info = new Info(true);
    assertEquals(THIRTY_360_ISDA.yearFraction(date1, date2, info), expected, TOLERANCE_ZERO);
  }

  @Test(dataProvider = "30U360")
  public void test_yearFraction_30U360EOM(
      int y1, int m1, int d1, int y2, int m2, int d2, Double valueNotEOM, Double valueEOM) {
    double expected = (valueEOM == SIMPLE_30_360 ? calc360(y1, m1, d1, y2, m2, d2) : valueEOM);
    LocalDate date1 = LocalDate.of(y1, m1, d1);
    LocalDate date2 = LocalDate.of(y2, m2, d2);
    ScheduleInfo info = new Info(true);
    assertEquals(THIRTY_U_360_EOM.yearFraction(date1, date2, info), expected, TOLERANCE_ZERO);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "30E360ISDA")
  static Object[][] data_30E360ISDA() {
    return new Object[][] {
        {2011, 12, 28, 2012, 2, 28, SIMPLE_30_360, SIMPLE_30_360},
        {2011, 12, 28, 2012, 2, 29, calc360(2011, 12, 28, 2012, 2, 30), SIMPLE_30_360},
        {2011, 12, 28, 2012, 3, 1, SIMPLE_30_360, SIMPLE_30_360},
        {2011, 12, 28, 2016, 2, 28, SIMPLE_30_360, SIMPLE_30_360},
        {2011, 12, 28, 2016, 2, 29, calc360(2011, 12, 28, 2016, 2, 30), SIMPLE_30_360},
        {2011, 12, 28, 2016, 3, 1, SIMPLE_30_360, SIMPLE_30_360},

        {2012, 2, 28, 2012, 3, 28, SIMPLE_30_360, SIMPLE_30_360},
        {2012, 2, 29, 2012, 3, 28, calc360(2012, 2, 30, 2012, 3, 28), calc360(2012, 2, 30, 2012, 3, 28)},
        {2011, 2, 28, 2012, 2, 28, calc360(2011, 2, 30, 2012, 2, 28), calc360(2011, 2, 30, 2012, 2, 28)},
        {2011, 2, 28, 2012, 2, 29, calc360(2011, 2, 30, 2012, 2, 30), calc360(2011, 2, 30, 2012, 2, 29)},
        {2012, 2, 29, 2016, 2, 29, calc360(2012, 2, 30, 2016, 2, 30), calc360(2012, 2, 30, 2016, 2, 29)},

        {2012, 3, 1, 2012, 3, 28, SIMPLE_30_360, SIMPLE_30_360},
        {2012, 5, 30, 2013, 8, 29, SIMPLE_30_360, SIMPLE_30_360},
        {2012, 5, 29, 2013, 8, 30, SIMPLE_30_360, SIMPLE_30_360},
        {2012, 5, 30, 2013, 8, 30, SIMPLE_30_360, SIMPLE_30_360},
        {2012, 5, 29, 2013, 8, 31, calc360(2012, 5, 29, 2013, 8, 30), calc360(2012, 5, 29, 2013, 8, 30)},
        {2012, 5, 30, 2013, 8, 31, calc360(2012, 5, 30, 2013, 8, 30), calc360(2012, 5, 30, 2013, 8, 30)},
        {2012, 5, 31, 2013, 8, 30, calc360(2012, 5, 30, 2013, 8, 30), calc360(2012, 5, 30, 2013, 8, 30)},
        {2012, 5, 31, 2013, 8, 31, calc360(2012, 5, 30, 2013, 8, 30), calc360(2012, 5, 30, 2013, 8, 30)},
    };
  }

  @Test(dataProvider = "30E360ISDA")
  public void test_yearFraction_30E360ISDA_notMaturity(
      int y1, int m1, int d1, int y2, int m2, int d2, Double valueNotMaturity, Double valueMaturity) {
    double expected = (valueNotMaturity == SIMPLE_30_360 ? calc360(y1, m1, d1, y2, m2, d2) : valueNotMaturity);
    LocalDate date1 = LocalDate.of(y1, m1, d1);
    LocalDate date2 = LocalDate.of(y2, m2, d2);
    ScheduleInfo info = new Info(false);
    assertEquals(THIRTY_E_360_ISDA.yearFraction(date1, date2, info), expected, TOLERANCE_ZERO);
  }

  @Test(dataProvider = "30E360ISDA")
  public void test_yearFraction_30E360ISDA_maturity(
      int y1, int m1, int d1, int y2, int m2, int d2, Double valueNotMaturity, Double valueMaturity) {
    double expected = (valueMaturity == SIMPLE_30_360 ? calc360(y1, m1, d1, y2, m2, d2) : valueMaturity);
    LocalDate date1 = LocalDate.of(y1, m1, d1);
    LocalDate date2 = LocalDate.of(y2, m2, d2);
    ScheduleInfo info = new Info(null, date2, null, false, P3M);
    assertEquals(THIRTY_E_360_ISDA.yearFraction(date1, date2, info), expected, TOLERANCE_ZERO);
  }

  //-------------------------------------------------------------------------
  // AFB day count is poorly defined, so tests were used to identify a sensible interpretation
  // 1) The ISDA use of "Calculation Period" is a translation of "Periode d'Application"
  // where the original simply meant the period the day count is applied over
  // and NOT the regular periodic schedule (ISDA's definition of "Calculation Period").
  // 2) The ISDA "clarification" for rolling backward does not appear in the original French.
  // The ISDA rule produce strange results (in comments below) which can be avoided.
  // OpenGamma interprets that February 29th should only be chosen if the end date of the period
  // is February 29th and the rolled back date is a leap year.
  // 3) No document indicates precisely when to stop rolling back and treat the remainder as a fraction
  // OpenGamma interprets that rolling back in whole years continues until the remainder
  // is less than one year, and possibly zero if two dates are an exact number of years apart
  // 4) In all cases, the rule has strange effects when interest through a period encounters
  // February 29th and the denominator suddenly changes from 365 to 366 for the rest of the year
  @DataProvider(name = "ACTACTAFB")
  static Object[][] data_ACTACTAFB() {
    return new Object[][] {
        // example from the original French specification
        {1994, 2, 10, 1997, 6, 30, 140d / 365d + 3},
        {1994, 2, 10, 1994, 6, 30, 140d / 365d},

        // simple examples that are less than one year long
        {2004, 2, 10, 2005, 2, 10, 1d},
        {2004, 2, 28, 2005, 2, 28, 1d},
        {2004, 2, 29, 2005, 2, 28, 365d / 366d},
        {2004, 3, 1, 2005, 3, 1, 1d},

        // examples over one year, from a fixed start date
        // from Feb28 2003
        {2003, 2, 28, 2005, 2, 27, 1d + (364d / 365d)},
        {2003, 2, 28, 2005, 2, 28, 2d},
        {2003, 2, 28, 2005, 3, 1, 2d + (1d / 365d)},
        {2003, 2, 28, 2008, 2, 27, 4d + (364d / 365d)},
        {2003, 2, 28, 2008, 2, 28, 5d},
        {2003, 2, 28, 2008, 2, 29, 5d},
        {2003, 2, 28, 2008, 3, 1, 5d + (1d / 365d)},
        // from Feb28 2004
        {2004, 2, 28, 2005, 2, 27, (365d / 366d)},
        {2004, 2, 28, 2005, 2, 28, 1d},
        {2004, 2, 28, 2005, 3, 1, 1d + (2d / 366d)},
        {2004, 2, 28, 2008, 2, 27, 3d + (365d / 366d)},
        {2004, 2, 28, 2008, 2, 28, 4d},                   // ISDA end-of-February would give (4d + (1d / 365d))
        {2004, 2, 28, 2008, 2, 29, 4d + (1d / 365d)},
        {2004, 2, 28, 2008, 3, 1, 4d + (2d / 366d)},
        // from Feb29 2004
        {2004, 2, 29, 2005, 2, 28, 365d / 366d},
        {2004, 2, 29, 2005, 3, 1, 1d + (1d / 366d)},
        {2004, 2, 29, 2008, 2, 27, 3d + (364d / 366d)},
        {2004, 2, 29, 2008, 2, 28, 3d + (365d / 366d)},   // ISDA end-of-February would give (4d)
        {2004, 2, 29, 2008, 2, 29, 4d},
        {2004, 2, 29, 2008, 3, 1, 4d + (1d / 366d)},
        // from Mar01 2004
        {2004, 3, 1, 2005, 2, 28, 364d / 365d},
        {2004, 3, 1, 2005, 3, 1, 1d},
        {2004, 3, 1, 2008, 2, 27, 3d + (363d / 365d)},
        {2004, 3, 1, 2008, 2, 28, 3d + (364d / 365d)},
        {2004, 3, 1, 2008, 2, 29, 3d + (364d / 365d)},
        {2004, 3, 1, 2008, 3, 1, 4d},
        // from Mar01 2003
        {2003, 3, 1, 2005, 2, 27, 1d + (363d / 365d)},
        {2003, 3, 1, 2005, 2, 28, 1d + (364d / 365d)},    // ISDA end-of-February would give (2d)
        {2003, 3, 1, 2005, 3, 1, 2d},
        {2003, 3, 1, 2008, 2, 27, 4d + (363d / 365d)},    // ISDA end-of-February would give (5d)
        {2003, 3, 1, 2008, 2, 28, 4d + (364d / 365d)},
        {2003, 3, 1, 2008, 2, 29, 5d},
        {2003, 3, 1, 2008, 3, 1, 5d},

        // examples over one year, up to a fixed end date (not relevant in real life)
        // up to Mar01 from leap year
        {2004, 2, 28, 2006, 3, 1, 2d + (2d / 366d)},
        {2004, 2, 29, 2006, 3, 1, 2d + (1d / 366d)},
        {2004, 3, 1, 2006, 3, 1, 2d},
        // up to Mar01 from non leap year
        {2005, 2, 28, 2007, 3, 1, 2d + (1d / 365d)},
        {2005, 3, 1, 2007, 3, 1, 2d},
        // up to Feb28 in leap year from leap year
        {2004, 2, 27, 2008, 2, 28, 4d + (1d / 365d)},     // ISDA end-of-February would give (4d + (2d / 365d))
        {2004, 2, 28, 2008, 2, 28, 4d},                   // ISDA end-of-February would give (4d + (1d / 365d))
        {2004, 2, 29, 2008, 2, 28, 3d + (365d / 366d)},   // ISDA end-of-February would give (4d)
        {2004, 3, 1, 2008, 2, 28, 3d + (364d / 365d)},
        // up to Feb28 in leap year from non leap year
        {2006, 2, 27, 2008, 2, 28, 2d + (1d / 365d)},
        {2006, 2, 28, 2008, 2, 28, 2d},
        {2006, 3, 1, 2008, 2, 28, 1d + (364d / 365d)},
        // up to Feb29 in leap year from leap year
        {2004, 2, 28, 2008, 2, 29, 4d + (1d / 365d)},
        {2004, 2, 29, 2008, 2, 29, 4d},
        {2004, 3, 1, 2008, 2, 29, 3d + (364d / 365d)},
        // up to Feb29 in leap year from non leap year
        {2006, 2, 27, 2008, 2, 29, 2d + (1d / 365d)},
        {2006, 2, 28, 2008, 2, 29, 2d},
        {2006, 3, 1, 2008, 2, 29, 1d + (364d / 365d)},
    };
  }

  @Test(dataProvider = "ACTACTAFB")
  public void test_yearFraction_ACTACTAFB(
      int y1, int m1, int d1, int y2, int m2, int d2, double expected) {
    LocalDate date1 = LocalDate.of(y1, m1, d1);
    LocalDate date2 = LocalDate.of(y2, m2, d2);
    assertEquals(ACT_ACT_AFB.yearFraction(date1, date2), expected, TOLERANCE_ZERO);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "ACT365L")
  static Object[][] data_ACT365L() {
    return new Object[][] {
        {2011, 12, 28, 2012, 2, 28, P12M, 2012, 2, 28, 62d / 365d},
        {2011, 12, 28, 2012, 2, 28, P12M, 2012, 2, 29, 62d / 366d},
        {2011, 12, 28, 2012, 2, 28, P12M, 2012, 3, 1, 62d / 366d},

        {2011, 12, 28, 2012, 2, 29, P12M, 2012, 2, 29, 63d / 366d},
        {2011, 12, 28, 2012, 2, 29, P12M, 2012, 3, 1, 63d / 366d},

        {2011, 12, 28, 2012, 2, 28, P6M, 2012, 2, 28, 62d / 366d},
        {2011, 12, 28, 2012, 2, 28, P6M, 2012, 2, 29, 62d / 366d},
        {2011, 12, 28, 2012, 2, 28, P6M, 2012, 3, 1, 62d / 366d},

        {2011, 12, 28, 2012, 2, 29, P6M, 2012, 2, 29, 63d / 366d},
        {2011, 12, 28, 2012, 2, 29, P6M, 2012, 3, 1, 63d / 366d},

        {2010, 12, 28, 2011, 2, 28, P6M, 2011, 2, 28, 62d / 365d},
        {2010, 12, 28, 2011, 2, 28, P6M, 2011, 3, 1, 62d / 365d},
    };
  }

  @Test(dataProvider = "ACT365L")
  public void test_yearFraction_ACT365L(
      int y1, int m1, int d1, int y2, int m2, int d2, Frequency freq, int y3, int m3, int d3, double expected) {
    LocalDate date1 = LocalDate.of(y1, m1, d1);
    LocalDate date2 = LocalDate.of(y2, m2, d2);
    ScheduleInfo info = new Info(null, null, LocalDate.of(y3, m3, d3), false, freq);
    assertEquals(ACT_365L.yearFraction(date1, date2, info), expected, TOLERANCE_ZERO);
  }

  //-------------------------------------------------------------------------
  public void test_actActIcma_singlePeriod() {
    LocalDate start = LocalDate.of(2003, 11, 1);
    LocalDate end = LocalDate.of(2004, 5, 1);
    ScheduleInfo info = new Info(start, end, end, true, P6M);
    assertEquals(ACT_ACT_ICMA.yearFraction(start, end.minusDays(1), info), (181d / (182d * 2d)), TOLERANCE_ZERO);
    assertEquals(ACT_ACT_ICMA.yearFraction(start, end, info), (182d / (182d * 2d)), TOLERANCE_ZERO);
  }

  public void test_actActIcma_longInitialStub_eomFlagEom_short() {
    // nominals, 2011-08-31 (P91D) 2011-11-30 (P91D) 2012-02-29
    LocalDate start = LocalDate.of(2011, 10, 1);
    LocalDate periodEnd = LocalDate.of(2012, 2, 29);
    LocalDate end = LocalDate.of(2011, 11, 12);  // before first nominal
    ScheduleInfo info = new Info(start, periodEnd.plus(P3M), periodEnd, true, P3M);
    assertEquals(ACT_ACT_ICMA.yearFraction(start, end, info), (42d / (91d * 4d)), TOLERANCE_ZERO);
  }

  public void test_actActIcma_longInitialStub_eomFlagEom_long() {
    // nominals, 2011-08-31 (P91D) 2011-11-30 (P91D) 2012-02-29
    LocalDate start = LocalDate.of(2011, 10, 1);
    LocalDate periodEnd = LocalDate.of(2012, 2, 29);
    LocalDate end = LocalDate.of(2012, 1, 12);  // after first nominal
    ScheduleInfo info = new Info(start, periodEnd.plus(P3M), periodEnd, true, P3M);
    assertEquals(ACT_ACT_ICMA.yearFraction(start, end, info), (60d / (91d * 4d)) + (43d / (91d * 4d)), TOLERANCE_ZERO);
  }

  public void test_actActIcma_veryLongInitialStub_eomFlagEom_short() {
    // nominals, 2011-05-31 (P92D) 2011-08-31 (P91D) 2011-11-30 (P91D) 2012-02-29
    LocalDate start = LocalDate.of(2011, 7, 1);
    LocalDate periodEnd = LocalDate.of(2012, 2, 29);
    LocalDate end = LocalDate.of(2011, 8, 12);  // before first nominal
    ScheduleInfo info = new Info(start, periodEnd.plus(P3M), periodEnd, true, P3M);
    assertEquals(ACT_ACT_ICMA.yearFraction(start, end, info), (42d / (92d * 4d)), TOLERANCE_ZERO);
  }

  public void test_actActIcma_veryLongInitialStub_eomFlagEom_mid() {
    // nominals, 2011-05-31 (P92D) 2011-08-31 (P91D) 2011-11-30 (P91D) 2012-02-29
    LocalDate start = LocalDate.of(2011, 7, 1);
    LocalDate periodEnd = LocalDate.of(2012, 2, 29);
    LocalDate end = LocalDate.of(2011, 11, 12);
    ScheduleInfo info = new Info(start, periodEnd.plus(P3M), periodEnd, true, P3M);
    assertEquals(ACT_ACT_ICMA.yearFraction(start, end, info), (61d / (92d * 4d)) + (73d / (91d * 4d)), TOLERANCE_ZERO);
  }

  public void test_actActIcma_longInitialStub_notEomFlagEom_short() {
    // nominals, 2011-08-29 (P92D) 2011-11-29 (P92D) 2012-02-29
    LocalDate start = LocalDate.of(2011, 10, 1);
    LocalDate periodEnd = LocalDate.of(2012, 2, 29);
    LocalDate end = LocalDate.of(2011, 11, 12);  // before first nominal
    ScheduleInfo info = new Info(start, periodEnd.plus(P3M), periodEnd, false, P3M);
    assertEquals(ACT_ACT_ICMA.yearFraction(start, end, info), (42d / (92d * 4d)), TOLERANCE_ZERO);
  }

  public void test_actActIcma_longInitialStub_notEomFlagEom_long() {
    // nominals, 2011-08-29 (P92D) 2011-11-29 (P92D) 2012-02-29
    LocalDate start = LocalDate.of(2011, 10, 1);
    LocalDate periodEnd = LocalDate.of(2012, 2, 29);
    LocalDate end = LocalDate.of(2012, 1, 12);  // after first nominal
    ScheduleInfo info = new Info(start, periodEnd.plus(P3M), periodEnd, false, P3M);
    assertEquals(ACT_ACT_ICMA.yearFraction(start, end, info), (59d / (92d * 4d)) + (44d / (92d * 4d)), TOLERANCE_ZERO);
  }

  //-------------------------------------------------------------------------
  public void test_actActIcma_longFinalStub_eomFlagEom_short() {
    // nominals, 2011-08-31 (P91D) 2011-11-30 (P91D) 2012-02-29
    LocalDate start = LocalDate.of(2011, 8, 31);
    LocalDate periodEnd = LocalDate.of(2012, 1, 31);
    LocalDate end = LocalDate.of(2011, 11, 12);  // before first nominal
    ScheduleInfo info = new Info(start.minus(P3M), periodEnd, periodEnd, true, P3M);
    assertEquals(ACT_ACT_ICMA.yearFraction(start, end, info), (73d / (91d * 4d)), TOLERANCE_ZERO);
  }

  public void test_actActIcma_longFinalStub_eomFlagEom_long() {
    // nominals, 2011-08-31 (P91D) 2011-11-30 (P91D) 2012-02-29
    LocalDate start = LocalDate.of(2011, 8, 31);
    LocalDate periodEnd = LocalDate.of(2012, 1, 31);
    LocalDate end = LocalDate.of(2012, 1, 12);  // after first nominal
    ScheduleInfo info = new Info(start.minus(P3M), periodEnd, periodEnd, true, P3M);
    assertEquals(ACT_ACT_ICMA.yearFraction(start, end, info), (91d / (91d * 4d)) + (43d / (91d * 4d)), TOLERANCE_ZERO);
  }

  public void test_actActIcma_longFinalStub_notEomFlagEom_short() {
    // nominals, 2012-02-29 (P90D) 2012-05-29 (P92D) 2012-08-29
    LocalDate start = LocalDate.of(2012, 2, 29);
    LocalDate periodEnd = LocalDate.of(2012, 7, 31);
    LocalDate end = LocalDate.of(2012, 4, 1);  // before first nominal
    ScheduleInfo info = new Info(start.minus(P3M), periodEnd, periodEnd, false, P3M);
    assertEquals(ACT_ACT_ICMA.yearFraction(start, end, info), (32d / (90d * 4d)), TOLERANCE_ZERO);
  }

  public void test_actActIcma_longFinalStub_notEomFlagEom_long() {
    // nominals, 2012-02-29 (P90D) 2012-05-29 (P92D) 2012-08-29
    LocalDate start = LocalDate.of(2012, 2, 29);
    LocalDate periodEnd = LocalDate.of(2012, 7, 31);
    LocalDate end = LocalDate.of(2012, 6, 1);  // after first nominal
    ScheduleInfo info = new Info(start.minus(P3M), periodEnd, periodEnd, false, P3M);
    assertEquals(ACT_ACT_ICMA.yearFraction(start, end, info), (90d / (90d * 4d)) + (3d / (92d * 4d)), TOLERANCE_ZERO);
  }

  //-------------------------------------------------------------------------
  // test against official examples - http://www.isda.org/c_and_a/pdf/ACT-ACT-ISDA-1999.pdf
  // this version has an error http://www.isda.org/c_and_a/pdf/mktc1198.pdf
  public void test_actAct_isdaTestCase_normal() {
    LocalDate start = LocalDate.of(2003, 11, 1);
    LocalDate end = LocalDate.of(2004, 5, 1);
    ScheduleInfo info = new Info(start, end.plus(P6M), end, true, P6M);
    assertEquals(ACT_ACT_ISDA.yearFraction(start, end), (61d / 365d) + (121d / 366d), TOLERANCE_ZERO);
    assertEquals(ACT_ACT_ICMA.yearFraction(start, end, info), (182d / (182d * 2d)), TOLERANCE_ZERO);
    assertEquals(ACT_ACT_AFB.yearFraction(start, end), (182d / 366d), TOLERANCE_ZERO);
  }

  public void test_actAct_isdaTestCase_shortInitialStub() {
    LocalDate start = LocalDate.of(1999, 2, 1);
    LocalDate firstRegular = LocalDate.of(1999, 7, 1);
    LocalDate end = LocalDate.of(2000, 7, 1);
    ScheduleInfo info1 = new Info(start, end.plus(P12M), firstRegular, true, P12M);  // initial period
    ScheduleInfo info2 = new Info(start, end.plus(P12M), end, true, P12M);  // regular period
    assertEquals(ACT_ACT_ISDA.yearFraction(start, firstRegular), (150d / 365d), TOLERANCE_ZERO);
    assertEquals(ACT_ACT_ICMA.yearFraction(start, firstRegular, info1), (150d / (365d * 1d)), TOLERANCE_ZERO);
    assertEquals(ACT_ACT_AFB.yearFraction(start, firstRegular), (150d / (365d)), TOLERANCE_ZERO);

    assertEquals(ACT_ACT_ISDA.yearFraction(firstRegular, end), (184d / 365d) + (182d / 366d), TOLERANCE_ZERO);
    assertEquals(ACT_ACT_ICMA.yearFraction(firstRegular, end, info2), (366d / (366d * 1d)), TOLERANCE_ZERO);
    assertEquals(ACT_ACT_AFB.yearFraction(firstRegular, end), (366d / 366d), TOLERANCE_ZERO);
  }

  public void test_actAct_isdaTestCase_longInitialStub() {
    LocalDate start = LocalDate.of(2002, 8, 15);
    LocalDate firstRegular = LocalDate.of(2003, 7, 15);
    LocalDate end = LocalDate.of(2004, 1, 15);
    ScheduleInfo info1 = new Info(start, end, firstRegular, true, P6M);  // initial period
    ScheduleInfo info2 = new Info(start, end, end, true, P6M);  // regular period
    assertEquals(ACT_ACT_ISDA.yearFraction(start, firstRegular), (334d / 365d), TOLERANCE_ZERO);
    assertEquals(ACT_ACT_ICMA.yearFraction(start, firstRegular, info1),
        (181d / (181d * 2d)) + (153d / (184d * 2d)), TOLERANCE_ZERO);
    assertEquals(ACT_ACT_AFB.yearFraction(start, firstRegular), (334d / 365d), TOLERANCE_ZERO);
    // example is wrong in 1998 euro swap version
    assertEquals(ACT_ACT_ISDA.yearFraction(firstRegular, end), (170d / 365d) + (14d / 366d), TOLERANCE_ZERO);
    assertEquals(ACT_ACT_ICMA.yearFraction(firstRegular, end, info2), (184d / (184d * 2d)), TOLERANCE_ZERO);
    assertEquals(ACT_ACT_AFB.yearFraction(firstRegular, end), (184d / 365d), TOLERANCE_ZERO);
  }

  public void test_actAct_isdaTestCase_shortFinalStub() {
    LocalDate start = LocalDate.of(1999, 7, 30);
    LocalDate lastRegular = LocalDate.of(2000, 1, 30);
    LocalDate end = LocalDate.of(2000, 6, 30);
    ScheduleInfo info1 = new Info(start, end, lastRegular, true, P6M);  // regular period
    ScheduleInfo info2 = new Info(start, end, end, true, P6M);  // final period
    assertEquals(ACT_ACT_ISDA.yearFraction(start, lastRegular), (155d / 365d) + (29d / 366d), TOLERANCE_ZERO);
    assertEquals(ACT_ACT_ICMA.yearFraction(start, lastRegular, info1), (184d / (184d * 2d)), TOLERANCE_ZERO);
    assertEquals(ACT_ACT_AFB.yearFraction(start, lastRegular), (184d / 365d), TOLERANCE_ZERO);

    assertEquals(ACT_ACT_ISDA.yearFraction(lastRegular, end), (152d / 366d), TOLERANCE_ZERO);
    assertEquals(ACT_ACT_ICMA.yearFraction(lastRegular, end, info2), (152d / (182d * 2d)), TOLERANCE_ZERO);
    assertEquals(ACT_ACT_AFB.yearFraction(lastRegular, end), (152d / 366d), TOLERANCE_ZERO);
  }

  public void test_actAct_isdaTestCase_longFinalStub() {
    LocalDate start = LocalDate.of(1999, 11, 30);
    LocalDate end = LocalDate.of(2000, 4, 30);
    ScheduleInfo info = new Info(start.minus(P3M), end, end, true, P3M);
    assertEquals(ACT_ACT_ISDA.yearFraction(start, end), (32d / 365d) + (120d / 366d), TOLERANCE_ZERO);
    assertEquals(ACT_ACT_ICMA.yearFraction(start, end, info), (91d / (91d * 4d)) + (61d / (92d * 4)), TOLERANCE_ZERO);
    assertEquals(ACT_ACT_AFB.yearFraction(start, end), (152d / 366d), TOLERANCE_ZERO);
  }

  //-------------------------------------------------------------------------
  public void test_actActYearVsIcma() {
    LocalDate start = LocalDate.of(2011, 1, 1);
    for (int i = 0; i < 400; i++) {
      for (int j = 0; j < 365; j++) {
        LocalDate end = start.plusDays(j);
        ScheduleInfo info = new Info(start, end, start.plusYears(1), false, P12M);
        assertEquals(ACT_ACT_ICMA.yearFraction(start, end, info), ACT_ACT_YEAR.yearFraction(start, end), TOLERANCE_ZERO);
      }
      start = start.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {ONE_ONE, "1/1"},
        {ACT_ACT_ISDA, "Act/Act ISDA"},
        {ACT_ACT_ICMA, "Act/Act ICMA"},
        {ACT_ACT_AFB, "Act/Act AFB"},
        {ACT_ACT_YEAR, "Act/Act Year"},
        {ACT_365_ACTUAL, "Act/365 Actual"},
        {ACT_365L, "Act/365L"},
        {ACT_360, "Act/360"},
        {ACT_364, "Act/364"},
        {ACT_365F, "Act/365F"},
        {ACT_365_25, "Act/365.25"},
        {NL_365, "NL/365"},
        {THIRTY_360_ISDA, "30/360 ISDA"},
        {THIRTY_U_360, "30U/360"},
        {THIRTY_U_360_EOM, "30U/360 EOM"},
        {THIRTY_360_PSA, "30/360 PSA"},
        {THIRTY_E_360_ISDA, "30E/360 ISDA"},
        {THIRTY_E_360, "30E/360"},
        {THIRTY_EPLUS_360, "30E+/360"},
    };
  }

  @Test(dataProvider = "name")
  public void test_name(DayCount convention, String name) {
    assertEquals(convention.getName(), name);
  }

  @Test(dataProvider = "name")
  public void test_toString(DayCount convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(DayCount convention, String name) {
    assertEquals(DayCount.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_extendedEnum(DayCount convention, String name) {
    ImmutableMap<String, DayCount> map = DayCount.extendedEnum().lookupAll();
    assertEquals(map.get(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> DayCount.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsRuntime(() -> DayCount.of(null));
  }

  //-------------------------------------------------------------------------
  public void test_relativeYearFraction_defaultMethod() {
    DayCount dc = new DayCount() {
      @Override
      public double yearFraction(LocalDate firstDate, LocalDate secondDate, ScheduleInfo scheduleInfo) {
        return 1;
      }

      @Override
      public int days(LocalDate firstDate, LocalDate secondDate) {
        return 1;
      }

      @Override
      public String getName() {
        return "";
      }
    };
    LocalDate date1 = date(2015, 6, 1);
    LocalDate date2 = date(2015, 7, 1);
    assertEquals(dc.yearFraction(date1, date2), 1, TOLERANCE_ZERO);
    assertEquals(dc.relativeYearFraction(date1, date2), 1, TOLERANCE_ZERO);
    assertEquals(dc.relativeYearFraction(date2, date1), -1, TOLERANCE_ZERO);
  }

  //-------------------------------------------------------------------------
  public void test_scheduleInfo() {
    ScheduleInfo test = new ScheduleInfo() {};
    assertEquals(test.isEndOfMonthConvention(), true);
    assertThrows(() -> test.getStartDate(), UnsupportedOperationException.class);
    assertThrows(() -> test.getEndDate(), UnsupportedOperationException.class);
    assertThrows(() -> test.getFrequency(), UnsupportedOperationException.class);
    assertThrows(() -> test.getPeriodEndDate(JAN_01), UnsupportedOperationException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(DayCounts.class);
    coverEnum(StandardDayCounts.class);
  }

  public void test_serialization() {
    assertSerialization(ACT_364);
  }

  public void test_jodaConvert() {
    assertJodaConvert(DayCount.class, THIRTY_360_ISDA);
    assertJodaConvert(DayCount.class, ACT_365F);
  }

  //-------------------------------------------------------------------------
  static class Info implements ScheduleInfo {
    private final LocalDate start;
    private final LocalDate end;
    private final LocalDate periodEnd;
    private final boolean eom;
    private final Frequency frequency;

    public Info(boolean eom) {
      this(null, null, null, eom, null);
    }

    public Info(LocalDate start, LocalDate end, LocalDate periodEnd, boolean eom, Frequency frequency) {
      this.start = start;
      this.end = end;
      this.periodEnd = periodEnd;
      this.eom = eom;
      this.frequency = frequency;
    }

    @Override
    public boolean isEndOfMonthConvention() {
      return eom;
    }

    @Override
    public Frequency getFrequency() {
      return frequency;
    }

    @Override
    public LocalDate getStartDate() {
      return start;
    }

    @Override
    public LocalDate getEndDate() {
      return end;
    }

    @Override
    public LocalDate getPeriodEndDate(LocalDate date) {
      return periodEnd;
    }
  };

}
