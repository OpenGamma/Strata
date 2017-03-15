/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
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

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link PeriodAdjustment}.
 */
@Test
public class PeriodAdjustmentTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final PeriodAdditionConvention PAC_NONE = PeriodAdditionConventions.NONE;
  private static final BusinessDayAdjustment BDA_NONE = BusinessDayAdjustment.NONE;
  private static final BusinessDayAdjustment BDA_FOLLOW_SAT_SUN =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, HolidayCalendarIds.SAT_SUN);

  //-------------------------------------------------------------------------
  public void test_NONE() {
    PeriodAdjustment test = PeriodAdjustment.NONE;
    assertEquals(test.getPeriod(), Period.ZERO);
    assertEquals(test.getAdjustment(), BDA_NONE);
    assertEquals(test.toString(), "P0D");
  }

  //-------------------------------------------------------------------------
  public void test_of_additionConventionNone() {
    PeriodAdjustment test = PeriodAdjustment.of(Period.of(1, 2, 3), PAC_NONE, BDA_NONE);
    assertEquals(test.getPeriod(), Period.of(1, 2, 3));
    assertEquals(test.getAdditionConvention(), PAC_NONE);
    assertEquals(test.getAdjustment(), BDA_NONE);
    assertEquals(test.toString(), "P1Y2M3D");
  }

  public void test_of_additionConventionLastDay() {
    PeriodAdjustment test = PeriodAdjustment.of(Period.ofMonths(3), LAST_DAY, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.getPeriod(), Period.ofMonths(3));
    assertEquals(test.getAdditionConvention(), LAST_DAY);
    assertEquals(test.getAdjustment(), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.toString(), "P3M with LastDay then apply Following using calendar Sat/Sun");
  }

  public void test_ofLastDay() {
    PeriodAdjustment test = PeriodAdjustment.ofLastDay(Period.ofMonths(3), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.getPeriod(), Period.ofMonths(3));
    assertEquals(test.getAdditionConvention(), LAST_DAY);
    assertEquals(test.getAdjustment(), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.toString(), "P3M with LastDay then apply Following using calendar Sat/Sun");
  }

  public void test_ofLastBusinessDay() {
    PeriodAdjustment test = PeriodAdjustment.ofLastBusinessDay(Period.ofMonths(3), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.getPeriod(), Period.ofMonths(3));
    assertEquals(test.getAdditionConvention(), LAST_BUSINESS_DAY);
    assertEquals(test.getAdjustment(), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.toString(), "P3M with LastBusinessDay then apply Following using calendar Sat/Sun");
  }

  public void test_of_invalid_conventionForPeriod() {
    Period period = Period.of(1, 2, 3);
    assertThrowsIllegalArg(() -> PeriodAdjustment.of(period, LAST_DAY, BDA_NONE));
    assertThrowsIllegalArg(() -> PeriodAdjustment.of(period, LAST_BUSINESS_DAY, BDA_NONE));
    assertThrowsIllegalArg(() -> PeriodAdjustment.ofLastDay(period, BDA_NONE));
    assertThrowsIllegalArg(() -> PeriodAdjustment.ofLastBusinessDay(period, BDA_NONE));
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
    PeriodAdjustment test = PeriodAdjustment.of(Period.ofMonths(months), LAST_DAY, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.adjust(date, REF_DATA), expected);
    assertEquals(test.resolve(REF_DATA).adjust(date), expected);
  }

  //-------------------------------------------------------------------------
  public void equals() {
    PeriodAdjustment a = PeriodAdjustment.of(Period.ofMonths(3), LAST_DAY, BDA_FOLLOW_SAT_SUN);
    PeriodAdjustment b = PeriodAdjustment.of(Period.ofMonths(1), LAST_DAY, BDA_FOLLOW_SAT_SUN);
    PeriodAdjustment c = PeriodAdjustment.of(Period.ofMonths(3), PAC_NONE, BDA_FOLLOW_SAT_SUN);
    PeriodAdjustment d = PeriodAdjustment.of(Period.ofMonths(3), LAST_DAY, BDA_NONE);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(c), false);
    assertEquals(a.equals(d), false);
  }

  //-------------------------------------------------------------------------
  public void test_beanBuilder() {
    PeriodAdjustment test = PeriodAdjustment.builder()
        .period(Period.ofMonths(3))
        .additionConvention(LAST_DAY)
        .adjustment(BDA_FOLLOW_SAT_SUN)
        .build();
    assertEquals(test.getPeriod(), Period.ofMonths(3));
    assertEquals(test.getAdditionConvention(), LAST_DAY);
    assertEquals(test.getAdjustment(), BDA_FOLLOW_SAT_SUN);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(PeriodAdjustment.of(Period.ofMonths(3), LAST_DAY, BDA_FOLLOW_SAT_SUN));
  }

  public void test_serialization() {
    assertSerialization(PeriodAdjustment.of(Period.ofMonths(3), LAST_DAY, BDA_FOLLOW_SAT_SUN));
  }

}
