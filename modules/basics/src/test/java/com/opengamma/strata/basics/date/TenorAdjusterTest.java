/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.PeriodAdditionConventions.LAST_BUSINESS_DAY;
import static com.opengamma.strata.basics.date.PeriodAdditionConventions.LAST_DAY;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1W;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3M;
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
 * Test {@link TenorAdjuster}.
 */
@Test
public class TenorAdjusterTest {

  private static final PeriodAdditionConvention PAC_NONE = PeriodAdditionConventions.NONE;
  private static final BusinessDayAdjuster BDA_NONE = BusinessDayAdjuster.NONE;
  private static final BusinessDayAdjuster BDA_FOLLOW_SAT_SUN =
      BusinessDayAdjuster.of(BusinessDayConventions.FOLLOWING, HolidayCalendars.SAT_SUN);

  //-------------------------------------------------------------------------
  public void test_of_additionConventionNone() {
    TenorAdjuster test = TenorAdjuster.of(Tenor.of(Period.of(1, 2, 3)), PAC_NONE, BDA_NONE);
    assertEquals(test.getTenor(), Tenor.of(Period.of(1, 2, 3)));
    assertEquals(test.getAdditionConvention(), PAC_NONE);
    assertEquals(test.getAdjuster(), BDA_NONE);
    assertEquals(test.toString(), "1Y2M3D");
  }

  public void test_of_additionConventionLastDay() {
    TenorAdjuster test = TenorAdjuster.of(TENOR_3M, LAST_DAY, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getAdditionConvention(), LAST_DAY);
    assertEquals(test.getAdjuster(), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.toString(), "3M with LastDay then apply Following using calendar Sat/Sun");
  }

  public void test_ofLastDay() {
    TenorAdjuster test = TenorAdjuster.ofLastDay(TENOR_3M, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getAdditionConvention(), LAST_DAY);
    assertEquals(test.getAdjuster(), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.toString(), "3M with LastDay then apply Following using calendar Sat/Sun");
  }

  public void test_ofLastBusinessDay() {
    TenorAdjuster test = TenorAdjuster.ofLastBusinessDay(TENOR_3M, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getAdditionConvention(), LAST_BUSINESS_DAY);
    assertEquals(test.getAdjuster(), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.toString(), "3M with LastBusinessDay then apply Following using calendar Sat/Sun");
  }

  public void test_of_invalid_conventionForPeriod() {
    assertThrowsIllegalArg(() -> TenorAdjuster.of(TENOR_1W, LAST_DAY, BDA_NONE));
    assertThrowsIllegalArg(() -> TenorAdjuster.of(TENOR_1W, LAST_BUSINESS_DAY, BDA_NONE));
    assertThrowsIllegalArg(() -> TenorAdjuster.ofLastDay(TENOR_1W, BDA_NONE));
    assertThrowsIllegalArg(() -> TenorAdjuster.ofLastBusinessDay(TENOR_1W, BDA_NONE));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "adjust")
  static Object[][] data_adjust() {
    return new Object[][] {
        // not last day
        {1, date(2014, 8, 15), date(2014, 9, 15)},
        {2, date(2014, 8, 15), date(2014, 10, 15)},
        {3, date(2014, 8, 15), date(2014, 11, 17)},
        // last day
        {1, date(2014, 2, 28), date(2014, 3, 31)},
        {1, date(2014, 6, 30), date(2014, 7, 31)},
    };
  }

  @Test(dataProvider = "adjust")
  public void test_adjust(int months, LocalDate date, LocalDate expected) {
    TenorAdjuster test = TenorAdjuster.of(Tenor.ofMonths(months), LAST_DAY, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.adjust(date), expected);
  }

  public void test_adjust_null() {
    TenorAdjuster test = TenorAdjuster.of(TENOR_3M, LAST_DAY, BDA_FOLLOW_SAT_SUN);
    assertThrowsIllegalArg(() -> test.adjust(null));
  }

  //-------------------------------------------------------------------------
  public void equals() {
    TenorAdjuster a = TenorAdjuster.of(TENOR_3M, LAST_DAY, BDA_FOLLOW_SAT_SUN);
    TenorAdjuster b = TenorAdjuster.of(TENOR_1M, LAST_DAY, BDA_FOLLOW_SAT_SUN);
    TenorAdjuster c = TenorAdjuster.of(TENOR_3M, PAC_NONE, BDA_FOLLOW_SAT_SUN);
    TenorAdjuster d = TenorAdjuster.of(TENOR_3M, LAST_DAY, BDA_NONE);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(c), false);
    assertEquals(a.equals(d), false);
  }

  //-------------------------------------------------------------------------
  public void test_beanBuilder() {
    TenorAdjuster test = TenorAdjuster.builder()
        .tenor(TENOR_3M)
        .additionConvention(LAST_DAY)
        .adjuster(BDA_FOLLOW_SAT_SUN)
        .build();
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getAdditionConvention(), LAST_DAY);
    assertEquals(test.getAdjuster(), BDA_FOLLOW_SAT_SUN);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(TenorAdjuster.of(TENOR_3M, LAST_DAY, BDA_FOLLOW_SAT_SUN));
  }

  public void test_serialization() {
    assertSerialization(TenorAdjuster.of(TENOR_3M, LAST_DAY, BDA_FOLLOW_SAT_SUN));
  }

}
