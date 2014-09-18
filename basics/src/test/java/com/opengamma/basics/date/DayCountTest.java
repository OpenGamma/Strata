/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import static com.opengamma.basics.date.DayCounts.ACT_360;
import static com.opengamma.basics.date.DayCounts.ACT_364;
import static com.opengamma.basics.date.DayCounts.ACT_365;
import static com.opengamma.basics.date.DayCounts.ACT_365_25;
import static com.opengamma.basics.date.DayCounts.ACT_365_ACTUAL;
import static com.opengamma.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.basics.date.DayCounts.NL_365;
import static com.opengamma.basics.date.DayCounts.ONE_ONE;
import static com.opengamma.basics.date.DayCounts.THIRTY_360_ISDA;
import static com.opengamma.basics.date.DayCounts.THIRTY_EPLUS_360;
import static com.opengamma.basics.date.DayCounts.THIRTY_E_360;
import static com.opengamma.basics.date.DayCounts.THIRTY_E_360_ISDA;
import static com.opengamma.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.basics.schedule.Frequency.P1Y;
import static com.opengamma.collect.TestHelper.assertJodaConvert;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrows;
import static com.opengamma.collect.TestHelper.coverEnum;
import static com.opengamma.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.opengamma.basics.date.DayCount.ScheduleInfo;
import com.opengamma.basics.schedule.Frequency;
import com.opengamma.basics.schedule.SchedulePeriodType;

/**
 * Test {@link DayCount}.
 */
@Test
public class DayCountTest {

  private static final LocalDate JAN_01 = LocalDate.of(2010, 1, 1);
  private static final LocalDate JAN_02 = LocalDate.of(2010, 1, 2);
  private static final LocalDate JUL_01 = LocalDate.of(2010, 7, 1);
  private static final LocalDate JAN_01_NEXT = LocalDate.of(2011, 1, 1);

  //-------------------------------------------------------------------------
  @DataProvider(name = "types")
  static Object[][] data_types() {
    DayCounts.Standard[] conv = DayCounts.Standard.values();
    Object[][] result = new Object[conv.length][];
    for (int i = 0; i < conv.length; i++) {
      result[i] = new Object[] {conv[i]};
    }
    return result;
  }

  @Test(dataProvider = "types")
  public void test_null(DayCount type) {
    assertThrows(() -> type.getDayCountFraction(null, JAN_01), IllegalArgumentException.class);
    assertThrows(() -> type.getDayCountFraction(JAN_01, null), IllegalArgumentException.class);
    assertThrows(() -> type.getDayCountFraction(null, null), IllegalArgumentException.class);
  }

  @Test(dataProvider = "types")
  public void test_wrongOrder(DayCount type) {
    assertThrows(() -> type.getDayCountFraction(JAN_02, JAN_01), IllegalArgumentException.class);
  }

  @Test(dataProvider = "types")
  public void test_same(DayCount type) {
    if (type != ONE_ONE) {
      assertEquals(type.getDayCountFraction(JAN_02, JAN_02), 0d, 0d);
    }
  }

  @Test(dataProvider = "types")
  public void test_halfYear(DayCount type) {
    // sanity check to ensure that half year has fraction close to half
    if (type != ONE_ONE) {
      ScheduleInfo info = new Info(false, false, P1Y, JAN_01_NEXT, SchedulePeriodType.NORMAL);
      assertEquals(type.getDayCountFraction(JAN_01, JUL_01, info), 0.5d, 0.01d);
    }
  }

  @Test(dataProvider = "types")
  public void test_wholeYear(DayCount type) {
    // sanity check to ensure that one year has fraction close to one
    if (type != ONE_ONE) {
      ScheduleInfo info = new Info(false, false, P1Y, JAN_01_NEXT, SchedulePeriodType.NORMAL);
      assertEquals(type.getDayCountFraction(JAN_01, JAN_01_NEXT, info), 1d, 0.02d);
    }
  }

  //-------------------------------------------------------------------------
  // use flag to make it clearer when an adjustment is happening
  private static Double SIMPLE_30_360 = new Double(Double.NaN);

  @DataProvider(name = "dayCountFraction")
  static Object[][] data_dayCountFraction() {
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
          {ACT_365, 2011, 12, 28, 2012, 2, 28, (62d / 365d)},
          {ACT_365, 2011, 12, 28, 2012, 2, 29, (63d / 365d)},
          {ACT_365, 2011, 12, 28, 2012, 3, 1, (64d / 365d)},
          {ACT_365, 2011, 12, 28, 2016, 2, 28, ((62d + 366d + 365d + 365d + 365d) / 365d)},
          {ACT_365, 2011, 12, 28, 2016, 2, 29, ((63d + 366d + 365d + 365d + 365d) / 365d)},
          {ACT_365, 2011, 12, 28, 2016, 3, 1, ((64d + 366d + 365d + 365d + 365d) / 365d)},
          {ACT_365, 2012, 2, 28, 2012, 3, 28, 29d / 365d},
          {ACT_365, 2012, 2, 29, 2012, 3, 28, 28d / 365d},
          {ACT_365, 2012, 3, 1, 2012, 3, 28, 27d / 365d},
          
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

  @Test(dataProvider = "dayCountFraction")
  public void test_dayCountFraction(DayCount dayCount, int y1, int m1, int d1, int y2, int m2, int d2, Double value) {
    double expected = (value == SIMPLE_30_360 ? calc360(y1, m1, d1, y2, m2, d2) : value);
    LocalDate date1 = LocalDate.of(y1, m1, d1);
    LocalDate date2 = LocalDate.of(y2, m2, d2);
    assertEquals(dayCount.getDayCountFraction(date1, date2), expected, 0d);
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
  public void test_dayCountFraction_30U360_notEom(
      int y1, int m1, int d1, int y2, int m2, int d2, Double valueNotEOM, Double valueEOM) {
    double expected = (valueNotEOM == SIMPLE_30_360 ? calc360(y1, m1, d1, y2, m2, d2) : valueNotEOM);
    LocalDate date1 = LocalDate.of(y1, m1, d1);
    LocalDate date2 = LocalDate.of(y2, m2, d2);
    ScheduleInfo info = new Info(false, false);
    assertEquals(THIRTY_U_360.getDayCountFraction(date1, date2, info), expected, 0d);
  }

  @Test(dataProvider = "30U360")
  public void test_dayCountFraction_30U360_eom(
      int y1, int m1, int d1, int y2, int m2, int d2, Double valueNotEOM, Double valueEOM) {
    double expected = (valueEOM == SIMPLE_30_360 ? calc360(y1, m1, d1, y2, m2, d2) : valueEOM);
    LocalDate date1 = LocalDate.of(y1, m1, d1);
    LocalDate date2 = LocalDate.of(y2, m2, d2);
    ScheduleInfo info = new Info(false, true);
    assertEquals(THIRTY_U_360.getDayCountFraction(date1, date2, info), expected, 0d);
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
  public void test_dayCountFraction_30E360ISDA_notMaturity(
      int y1, int m1, int d1, int y2, int m2, int d2, Double valueNotMaturity, Double valueMaturity) {
    double expected = (valueNotMaturity == SIMPLE_30_360 ? calc360(y1, m1, d1, y2, m2, d2) : valueNotMaturity);
    LocalDate date1 = LocalDate.of(y1, m1, d1);
    LocalDate date2 = LocalDate.of(y2, m2, d2);
    ScheduleInfo info = new Info(false, false);
    assertEquals(THIRTY_E_360_ISDA.getDayCountFraction(date1, date2, info), expected, 0d);
  }

  @Test(dataProvider = "30E360ISDA")
  public void test_dayCountFraction_30E360ISDA_maturity(
      int y1, int m1, int d1, int y2, int m2, int d2, Double valueNotMaturity, Double valueMaturity) {
    double expected = (valueMaturity == SIMPLE_30_360 ? calc360(y1, m1, d1, y2, m2, d2) : valueMaturity);
    LocalDate date1 = LocalDate.of(y1, m1, d1);
    LocalDate date2 = LocalDate.of(y2, m2, d2);
    ScheduleInfo info = new Info(true, false);
    assertEquals(THIRTY_E_360_ISDA.getDayCountFraction(date1, date2, info), expected, 0d);
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
      return new Object[][] {
          {ONE_ONE, "1/1"},
          {ACT_ACT_ISDA, "Act/Act ISDA"},
          {ACT_365_ACTUAL, "Act/365 Actual"},
          {ACT_360, "Act/360"},
          {ACT_364, "Act/364"},
          {ACT_365, "Act/365"},
          {ACT_365_25, "Act/365.25"},
          {NL_365, "NL/365"},
          {THIRTY_360_ISDA, "30/360 ISDA"},
          {THIRTY_U_360, "30U/360"},
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

  public void test_of_lookup_notFound() {
    assertThrows(() -> DayCount.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> DayCount.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_scheduleInfo() {
    ScheduleInfo test = new ScheduleInfo() {};
    assertEquals(test.isEndOfMonthConvention(), true);
    assertEquals(test.isScheduleEndDate(null), false);
    assertThrows(() -> test.getEndDate(), UnsupportedOperationException.class);
    assertThrows(() -> test.getFrequency(), UnsupportedOperationException.class);
    assertThrows(() -> test.getType(), UnsupportedOperationException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(DayCounts.class);
    coverEnum(DayCounts.Standard.class);
  }

  public void test_serialization() {
    assertSerialization(ACT_364);
  }

  public void test_jodaConvert() {
    assertJodaConvert(DayCount.class, THIRTY_360_ISDA);
    assertJodaConvert(DayCount.class, ACT_365);
  }

  //-------------------------------------------------------------------------
  static class Info implements ScheduleInfo {
    private final boolean maturity;
    private final boolean eom;
    private final Frequency frequency;
    private final LocalDate periodEnd;
    private final SchedulePeriodType type;
    
    public Info(boolean maturity, boolean eom) {
      this(maturity, eom, null, null, null);
    }
    public Info(boolean maturity, boolean eom, Frequency frequency, LocalDate periodEnd, SchedulePeriodType type) {
      super();
      this.maturity = maturity;
      this.eom = eom;
      this.frequency = frequency;
      this.periodEnd = periodEnd;
      this.type = type;
    }
    @Override
    public boolean isScheduleEndDate(LocalDate date) {
      return maturity;
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
    public LocalDate getEndDate() {
      return periodEnd;
    }
    @Override
    public SchedulePeriodType getType() {
      return type;
    }
  };

}
