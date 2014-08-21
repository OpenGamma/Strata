/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrows;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link PeriodAdjustment}.
 */
@Test
public class PeriodAdjustmentTest {

  private static final BusinessDayAdjustment BDA_NONE = BusinessDayAdjustment.NONE;
  private static final BusinessDayAdjustment BDA_FOLLOW_SAT_SUN =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, HolidayCalendars.SAT_SUN);

  //-------------------------------------------------------------------------
  public void test_of() {
    PeriodAdjustment test = PeriodAdjustment.of(Period.of(1, 2, 3), BDA_NONE);
    assertEquals(test.getPeriod(), Period.of(1, 2, 3));
    assertEquals(test.getAdjustment(), BDA_NONE);
    assertEquals(test.toString(), "P1Y2M3D");
  }

  public void test_ofYears() {
    PeriodAdjustment test = PeriodAdjustment.ofYears(3, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.getPeriod(), Period.ofYears(3));
    assertEquals(test.getAdjustment(), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.toString(), "P3Y then apply Following using calendar Sat/Sun");
  }

  public void test_ofMonths() {
    PeriodAdjustment test = PeriodAdjustment.ofMonths(3, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.getPeriod(), Period.ofMonths(3));
    assertEquals(test.getAdjustment(), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.toString(), "P3M then apply Following using calendar Sat/Sun");
  }

  public void test_ofDays() {
    PeriodAdjustment test = PeriodAdjustment.ofDays(3, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.getPeriod(), Period.ofDays(3));
    assertEquals(test.getAdjustment(), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.toString(), "P3D then apply Following using calendar Sat/Sun");
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "adjust")
  static Object[][] data_adjust() {
      return new Object[][] {
          {0, date(2014, 8, 15), date(2014, 8, 15)},
          {1, date(2014, 8, 15), date(2014, 9, 15)},
          {2, date(2014, 8, 15), date(2014, 10, 15)},
          {3, date(2014, 8, 15), date(2014, 11, 17)},
          {-1, date(2014, 8, 15), date(2014, 7, 15)},
          {-2, date(2014, 8, 15), date(2014, 6, 16)},
      };
  }

  @Test(dataProvider = "adjust")
  public void test_adjust(int months, LocalDate date, LocalDate expected) {
    PeriodAdjustment test = PeriodAdjustment.ofMonths(months, BDA_FOLLOW_SAT_SUN);
    LocalDate base = date(2014, 8, 15);
    assertEquals(test.adjust(base), expected);
  }

  public void test_adjust_null() {
    PeriodAdjustment test = PeriodAdjustment.ofMonths(3, BDA_FOLLOW_SAT_SUN);
    assertThrows(() -> test.adjust(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void equals() {
    PeriodAdjustment a = PeriodAdjustment.of(Period.ofDays(1), BDA_FOLLOW_SAT_SUN);
    PeriodAdjustment b = PeriodAdjustment.of(Period.ofDays(2), BDA_FOLLOW_SAT_SUN);
    PeriodAdjustment c = PeriodAdjustment.of(Period.ofDays(1), BDA_NONE);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(c), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(PeriodAdjustment.of(Period.ofDays(1), BDA_FOLLOW_SAT_SUN));
  }

  public void test_serialization() {
    assertSerialization(PeriodAdjustment.of(Period.ofDays(1), BDA_FOLLOW_SAT_SUN));
  }

}
