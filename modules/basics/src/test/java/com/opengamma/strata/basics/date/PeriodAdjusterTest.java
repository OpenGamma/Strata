/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.PeriodAdditionConventions.LAST_BUSINESS_DAY;
import static com.opengamma.strata.basics.date.PeriodAdditionConventions.LAST_DAY;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test {@link PeriodAdjuster}.
 */
@Test
public class PeriodAdjusterTest {

  private static final PeriodAdditionConvention PAC_NONE = PeriodAdditionConventions.NONE;
  private static final BusinessDayAdjuster BDA_NONE = BusinessDayAdjuster.NONE;
  private static final BusinessDayAdjuster BDA_FOLLOW_SAT_SUN =
      BusinessDayAdjuster.of(BusinessDayConventions.FOLLOWING, HolidayCalendars.SAT_SUN);

  //-------------------------------------------------------------------------
  public void test_NONE() {
    PeriodAdjuster test = PeriodAdjuster.NONE;
    assertEquals(test.getPeriod(), Period.ZERO);
    assertEquals(test.getAdjuster(), BDA_NONE);
    assertEquals(test.toString(), "P0D");
  }

  //-------------------------------------------------------------------------
  public void test_of_additionConventionNone() {
    PeriodAdjuster test = PeriodAdjuster.of(Period.of(1, 2, 3), PAC_NONE, BDA_NONE);
    assertEquals(test.getPeriod(), Period.of(1, 2, 3));
    assertEquals(test.getAdditionConvention(), PAC_NONE);
    assertEquals(test.getAdjuster(), BDA_NONE);
    assertEquals(test.toString(), "P1Y2M3D");
  }

  public void test_of_additionConventionLastDay() {
    PeriodAdjuster test = PeriodAdjuster.of(Period.ofMonths(3), LAST_DAY, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.getPeriod(), Period.ofMonths(3));
    assertEquals(test.getAdditionConvention(), LAST_DAY);
    assertEquals(test.getAdjuster(), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.toString(), "P3M with LastDay then apply Following using calendar Sat/Sun");
  }

  public void test_ofLastDay() {
    PeriodAdjuster test = PeriodAdjuster.ofLastDay(Period.ofMonths(3), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.getPeriod(), Period.ofMonths(3));
    assertEquals(test.getAdditionConvention(), LAST_DAY);
    assertEquals(test.getAdjuster(), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.toString(), "P3M with LastDay then apply Following using calendar Sat/Sun");
  }

  public void test_ofLastBusinessDay() {
    PeriodAdjuster test = PeriodAdjuster.ofLastBusinessDay(Period.ofMonths(3), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.getPeriod(), Period.ofMonths(3));
    assertEquals(test.getAdditionConvention(), LAST_BUSINESS_DAY);
    assertEquals(test.getAdjuster(), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.toString(), "P3M with LastBusinessDay then apply Following using calendar Sat/Sun");
  }

  public void test_of_invalid_conventionForPeriod() {
    Period period = Period.of(1, 2, 3);
    assertThrowsIllegalArg(() -> PeriodAdjuster.of(period, LAST_DAY, BDA_NONE));
    assertThrowsIllegalArg(() -> PeriodAdjuster.of(period, LAST_BUSINESS_DAY, BDA_NONE));
    assertThrowsIllegalArg(() -> PeriodAdjuster.ofLastDay(period, BDA_NONE));
    assertThrowsIllegalArg(() -> PeriodAdjuster.ofLastBusinessDay(period, BDA_NONE));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "adjust")
  static Object[][] data_adjust() {
    return new Object[][] {
        // not last day
        {0, date(2014, 8, 15), date(2014, 8, 15)},
        {1, date(2014, 8, 15), date(2014, 9, 15)},
        {2, date(2014, 8, 15), date(2014, 10, 15)},
        {3, date(2014, 8, 15), date(2014, 11, 17)},
        {-1, date(2014, 8, 15), date(2014, 7, 15)},
        {-2, date(2014, 8, 15), date(2014, 6, 16)},
        // last day
        {1, date(2014, 2, 28), date(2014, 3, 31)},
        {1, date(2014, 6, 30), date(2014, 7, 31)},
    };
  }

  @Test(dataProvider = "adjust")
  public void test_adjust(int months, LocalDate date, LocalDate expected) {
    PeriodAdjuster test = PeriodAdjuster.of(Period.ofMonths(months), LAST_DAY, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.adjust(date), expected);
  }

  public void test_adjust_null() {
    PeriodAdjuster test = PeriodAdjuster.of(Period.ofMonths(3), LAST_DAY, BDA_FOLLOW_SAT_SUN);
    assertThrowsIllegalArg(() -> test.adjust(null));
  }

  //-------------------------------------------------------------------------
  public void equals() {
    PeriodAdjuster a = PeriodAdjuster.of(Period.ofMonths(3), LAST_DAY, BDA_FOLLOW_SAT_SUN);
    PeriodAdjuster b = PeriodAdjuster.of(Period.ofMonths(1), LAST_DAY, BDA_FOLLOW_SAT_SUN);
    PeriodAdjuster c = PeriodAdjuster.of(Period.ofMonths(3), PAC_NONE, BDA_FOLLOW_SAT_SUN);
    PeriodAdjuster d = PeriodAdjuster.of(Period.ofMonths(3), LAST_DAY, BDA_NONE);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(c), false);
    assertEquals(a.equals(d), false);
  }

  //-------------------------------------------------------------------------
  public void test_beanBuilder() {
    PeriodAdjuster test = PeriodAdjuster.builder()
        .period(Period.ofMonths(3))
        .additionConvention(LAST_DAY)
        .adjuster(BDA_FOLLOW_SAT_SUN)
        .build();
    assertEquals(test.getPeriod(), Period.ofMonths(3));
    assertEquals(test.getAdditionConvention(), LAST_DAY);
    assertEquals(test.getAdjuster(), BDA_FOLLOW_SAT_SUN);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(PeriodAdjuster.of(Period.ofMonths(3), LAST_DAY, BDA_FOLLOW_SAT_SUN));
  }

  public void test_serialization() {
    assertSerialization(PeriodAdjuster.of(Period.ofMonths(3), LAST_DAY, BDA_FOLLOW_SAT_SUN));
  }

}
