/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import static com.opengamma.basics.date.DayCount.ACT_360;
import static com.opengamma.basics.date.DayCount.ACT_364;
import static com.opengamma.basics.date.DayCount.ACT_365F;
import static com.opengamma.basics.date.DayCount.ACT_365_25;
import static com.opengamma.basics.date.DayCount.ACT_ACT_ISDA;
import static com.opengamma.basics.date.DayCount.NL_365;
import static com.opengamma.basics.date.DayCount._30EPLUS_360;
import static com.opengamma.basics.date.DayCount._30E_360;
import static com.opengamma.basics.date.DayCount._30E_360_ISDA;
import static com.opengamma.basics.date.DayCount._30U_360;
import static com.opengamma.basics.date.DayCount._30_360_ISDA;
import static com.opengamma.collect.TestHelper.assertThrows;
import static com.opengamma.collect.TestHelper.coverEnum;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link DateAdjusters}.
 */
@Test
public class DayCountTest {

  private static final LocalDate D1 = LocalDate.of(2010, 1, 1);
  private static final LocalDate D2 = LocalDate.of(2010, 1, 2);

  //-------------------------------------------------------------------------
  @DataProvider(name = "types")
  Object[][] data_types() {
    DayCounts[] conv = DayCounts.values();
    Object[][] result = new Object[conv.length][];
    for (int i = 0; i < conv.length; i++) {
      result[i] = new Object[] {conv[i]};
    }
    return result;
  }

  @Test(dataProvider = "types")
  public void test_null(DayCount type) {
    assertThrows(() -> type.getDayCountFraction(null, D1), IllegalArgumentException.class);
    assertThrows(() -> type.getDayCountFraction(D1, null), IllegalArgumentException.class);
    assertThrows(() -> type.getDayCountFraction(null, null), IllegalArgumentException.class);
  }

  @Test(dataProvider = "types")
  public void test_wrongOrder(DayCount type) {
    assertThrows(() -> type.getDayCountFraction(D2, D1), IllegalArgumentException.class);
  }

  @Test(dataProvider = "types")
  public void test_same(DayCount type) {
    assertEquals(type.getDayCountFraction(D2, D2), 0d, 0d);
  }

  //-------------------------------------------------------------------------
  // use flag to make it clearer when an adjustment is happening
  private static Double SIMPLE_30_360 = new Double(Double.NaN);

  @DataProvider(name = "dayCountFraction")
  Object[][] data_dayCountFraction() {
      return new Object[][] {
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
          
          //-------------------------------------------------------
          {_30_360_ISDA, 2011, 12, 28, 2012, 2, 28, SIMPLE_30_360},
          {_30_360_ISDA, 2011, 12, 28, 2012, 2, 29, SIMPLE_30_360},
          {_30_360_ISDA, 2011, 12, 28, 2012, 3, 1, SIMPLE_30_360},
          {_30_360_ISDA, 2011, 12, 28, 2016, 2, 28, SIMPLE_30_360},
          {_30_360_ISDA, 2011, 12, 28, 2016, 2, 29, SIMPLE_30_360},
          {_30_360_ISDA, 2011, 12, 28, 2016, 3, 1, SIMPLE_30_360},
          
          {_30_360_ISDA, 2012, 2, 28, 2012, 3, 28, SIMPLE_30_360},
          {_30_360_ISDA, 2012, 2, 29, 2012, 3, 28, SIMPLE_30_360},
          {_30_360_ISDA, 2011, 2, 28, 2012, 2, 28, SIMPLE_30_360},
          {_30_360_ISDA, 2011, 2, 28, 2012, 2, 29, SIMPLE_30_360},
          {_30_360_ISDA, 2012, 2, 29, 2016, 2, 29, SIMPLE_30_360},
          
          {_30_360_ISDA, 2012, 3, 1, 2012, 3, 28, SIMPLE_30_360},
          {_30_360_ISDA, 2012, 5, 30, 2013, 8, 29, SIMPLE_30_360},
          {_30_360_ISDA, 2012, 5, 29, 2013, 8, 30, SIMPLE_30_360},
          {_30_360_ISDA, 2012, 5, 30, 2013, 8, 30, SIMPLE_30_360},
          {_30_360_ISDA, 2012, 5, 29, 2013, 8, 31, SIMPLE_30_360},
          {_30_360_ISDA, 2012, 5, 30, 2013, 8, 31, calc360(2012, 5, 30, 2013, 8, 30)},
          {_30_360_ISDA, 2012, 5, 31, 2013, 8, 30, calc360(2012, 5, 30, 2013, 8, 30)},
          {_30_360_ISDA, 2012, 5, 31, 2013, 8, 31, calc360(2012, 5, 30, 2013, 8, 30)},
          
          //-------------------------------------------------------
          {_30U_360, 2011, 12, 28, 2012, 2, 28, SIMPLE_30_360},
          {_30U_360, 2011, 12, 28, 2012, 2, 29, SIMPLE_30_360},
          {_30U_360, 2011, 12, 28, 2012, 3, 1, SIMPLE_30_360},
          {_30U_360, 2011, 12, 28, 2016, 2, 28, SIMPLE_30_360},
          {_30U_360, 2011, 12, 28, 2016, 2, 29, SIMPLE_30_360},
          {_30U_360, 2011, 12, 28, 2016, 3, 1, SIMPLE_30_360},
          
          {_30U_360, 2012, 2, 28, 2012, 3, 28, SIMPLE_30_360},
          {_30U_360, 2012, 2, 29, 2012, 3, 28, calc360(2012, 2, 30, 2012, 3, 28)},
          {_30U_360, 2011, 2, 28, 2012, 2, 28, calc360(2011, 2, 30, 2012, 2, 28)},
          {_30U_360, 2011, 2, 28, 2012, 2, 29, calc360(2011, 2, 30, 2012, 2, 30)},
          {_30U_360, 2012, 2, 29, 2016, 2, 29, calc360(2012, 2, 30, 2016, 2, 30)},
          
          {_30U_360, 2012, 3, 1, 2012, 3, 28, SIMPLE_30_360},
          {_30U_360, 2012, 5, 30, 2013, 8, 29, SIMPLE_30_360},
          {_30U_360, 2012, 5, 29, 2013, 8, 30, SIMPLE_30_360},
          {_30U_360, 2012, 5, 30, 2013, 8, 30, SIMPLE_30_360},
          {_30U_360, 2012, 5, 29, 2013, 8, 31, SIMPLE_30_360},
          {_30U_360, 2012, 5, 30, 2013, 8, 31, calc360(2012, 5, 30, 2013, 8, 30)},
          {_30U_360, 2012, 5, 31, 2013, 8, 30, calc360(2012, 5, 30, 2013, 8, 30)},
          {_30U_360, 2012, 5, 31, 2013, 8, 31, calc360(2012, 5, 30, 2013, 8, 30)},
          
          //-------------------------------------------------------
          {_30E_360_ISDA, 2011, 12, 28, 2012, 2, 28, SIMPLE_30_360},
          {_30E_360_ISDA, 2011, 12, 28, 2012, 2, 29, calc360(2011, 12, 28, 2012, 2, 30)},
          {_30E_360_ISDA, 2011, 12, 28, 2012, 3, 1, SIMPLE_30_360},
          {_30E_360_ISDA, 2011, 12, 28, 2016, 2, 28, SIMPLE_30_360},
          {_30E_360_ISDA, 2011, 12, 28, 2016, 2, 29, calc360(2011, 12, 28, 2016, 2, 30)},
          {_30E_360_ISDA, 2011, 12, 28, 2016, 3, 1, SIMPLE_30_360},
          
          {_30E_360_ISDA, 2012, 2, 28, 2012, 3, 28, SIMPLE_30_360},
          {_30E_360_ISDA, 2012, 2, 29, 2012, 3, 28, calc360(2012, 2, 30, 2012, 3, 28)},
          {_30E_360_ISDA, 2011, 2, 28, 2012, 2, 28, calc360(2011, 2, 30, 2012, 2, 28)},
          {_30E_360_ISDA, 2011, 2, 28, 2012, 2, 29, calc360(2011, 2, 30, 2012, 2, 30)},
          {_30E_360_ISDA, 2012, 2, 29, 2016, 2, 29, calc360(2012, 2, 30, 2016, 2, 30)},
          
          {_30E_360_ISDA, 2012, 3, 1, 2012, 3, 28, SIMPLE_30_360},
          {_30E_360_ISDA, 2012, 5, 30, 2013, 8, 29, SIMPLE_30_360},
          {_30E_360_ISDA, 2012, 5, 29, 2013, 8, 30, SIMPLE_30_360},
          {_30E_360_ISDA, 2012, 5, 30, 2013, 8, 30, SIMPLE_30_360},
          {_30E_360_ISDA, 2012, 5, 29, 2013, 8, 31, calc360(2012, 5, 29, 2013, 8, 30)},
          {_30E_360_ISDA, 2012, 5, 30, 2013, 8, 31, calc360(2012, 5, 30, 2013, 8, 30)},
          {_30E_360_ISDA, 2012, 5, 31, 2013, 8, 30, calc360(2012, 5, 30, 2013, 8, 30)},
          {_30E_360_ISDA, 2012, 5, 31, 2013, 8, 31, calc360(2012, 5, 30, 2013, 8, 30)},
          
          //-------------------------------------------------------
          {_30E_360, 2011, 12, 28, 2012, 2, 28, SIMPLE_30_360},
          {_30E_360, 2011, 12, 28, 2012, 2, 29, SIMPLE_30_360},
          {_30E_360, 2011, 12, 28, 2012, 3, 1, SIMPLE_30_360},
          {_30E_360, 2011, 12, 28, 2016, 2, 28, SIMPLE_30_360},
          {_30E_360, 2011, 12, 28, 2016, 2, 29, SIMPLE_30_360},
          {_30E_360, 2011, 12, 28, 2016, 3, 1, SIMPLE_30_360},
          
          {_30E_360, 2012, 2, 28, 2012, 3, 28, SIMPLE_30_360},
          {_30E_360, 2012, 2, 29, 2012, 3, 28, SIMPLE_30_360},
          {_30E_360, 2011, 2, 28, 2012, 2, 28, SIMPLE_30_360},
          {_30E_360, 2011, 2, 28, 2012, 2, 29, SIMPLE_30_360},
          {_30E_360, 2012, 2, 29, 2016, 2, 29, SIMPLE_30_360},
          
          {_30E_360, 2012, 3, 1, 2012, 3, 28, SIMPLE_30_360},
          {_30E_360, 2012, 5, 30, 2013, 8, 29, SIMPLE_30_360},
          {_30E_360, 2012, 5, 29, 2013, 8, 30, SIMPLE_30_360},
          {_30E_360, 2012, 5, 30, 2013, 8, 30, SIMPLE_30_360},
          {_30E_360, 2012, 5, 29, 2013, 8, 31, calc360(2012, 5, 29, 2013, 8, 30)},
          {_30E_360, 2012, 5, 30, 2013, 8, 31, calc360(2012, 5, 30, 2013, 8, 30)},
          {_30E_360, 2012, 5, 31, 2013, 8, 30, calc360(2012, 5, 30, 2013, 8, 30)},
          {_30E_360, 2012, 5, 31, 2013, 8, 31, calc360(2012, 5, 30, 2013, 8, 30)},
          
          //-------------------------------------------------------
          {_30EPLUS_360, 2011, 12, 28, 2012, 2, 28, SIMPLE_30_360},
          {_30EPLUS_360, 2011, 12, 28, 2012, 2, 29, SIMPLE_30_360},
          {_30EPLUS_360, 2011, 12, 28, 2012, 3, 1, SIMPLE_30_360},
          {_30EPLUS_360, 2011, 12, 28, 2016, 2, 28, SIMPLE_30_360},
          {_30EPLUS_360, 2011, 12, 28, 2016, 2, 29, SIMPLE_30_360},
          {_30EPLUS_360, 2011, 12, 28, 2016, 3, 1, SIMPLE_30_360},
          
          {_30EPLUS_360, 2012, 2, 28, 2012, 3, 28, SIMPLE_30_360},
          {_30EPLUS_360, 2012, 2, 29, 2012, 3, 28, SIMPLE_30_360},
          {_30EPLUS_360, 2012, 3, 1, 2012, 3, 28, SIMPLE_30_360},
          {_30EPLUS_360, 2011, 2, 28, 2012, 2, 28, SIMPLE_30_360},
          {_30EPLUS_360, 2011, 2, 28, 2012, 2, 29, SIMPLE_30_360},
          {_30EPLUS_360, 2012, 2, 29, 2016, 2, 29, SIMPLE_30_360},
          
          {_30EPLUS_360, 2012, 3, 1, 2012, 3, 28, SIMPLE_30_360},
          {_30EPLUS_360, 2012, 5, 30, 2013, 8, 29, SIMPLE_30_360},
          {_30EPLUS_360, 2012, 5, 29, 2013, 8, 30, SIMPLE_30_360},
          {_30EPLUS_360, 2012, 5, 30, 2013, 8, 30, SIMPLE_30_360},
          {_30EPLUS_360, 2012, 5, 29, 2013, 8, 31, calc360(2012, 5, 29, 2013, 9, 1)},
          {_30EPLUS_360, 2012, 5, 30, 2013, 8, 31, calc360(2012, 5, 30, 2013, 9, 1)},
          {_30EPLUS_360, 2012, 5, 31, 2013, 8, 30, calc360(2012, 5, 30, 2013, 8, 30)},
          {_30EPLUS_360, 2012, 5, 31, 2013, 8, 31, calc360(2012, 5, 30, 2013, 9, 1)},
      };
  }

  private double calc360(int y1, int m1, int d1, int y2, int m2, int d2) {
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
  @DataProvider(name = "name")
  Object[][] data_name() {
      return new Object[][] {
          {ACT_ACT_ISDA, "Act/Act ISDA"},
          {ACT_360, "Act/360"},
          {ACT_364, "Act/364"},
          {ACT_365F, "Act/365F"},
          {ACT_365_25, "Act/365.25"},
          {NL_365, "NL/365"},
          {_30_360_ISDA, "30/360 ISDA"},
          {_30U_360, "30U/360"},
          {_30E_360_ISDA, "30/360 German"},
          {_30E_360, "30E/360"},
          {_30EPLUS_360, "30E+/360"},
      };
  }

  @Test(dataProvider = "name")
  public void test_name(DayCount convention, String name) {
    assertEquals(convention.getName(), name);
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
  public void coverage() {
    coverEnum(DayCounts.class);
  }

}
