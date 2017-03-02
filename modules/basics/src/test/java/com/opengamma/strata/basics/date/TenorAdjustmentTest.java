/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
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

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link TenorAdjustment}.
 */
@Test
public class TenorAdjustmentTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final PeriodAdditionConvention PAC_NONE = PeriodAdditionConventions.NONE;
  private static final BusinessDayAdjustment BDA_NONE = BusinessDayAdjustment.NONE;
  private static final BusinessDayAdjustment BDA_FOLLOW_SAT_SUN =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, HolidayCalendarIds.SAT_SUN);

  //-------------------------------------------------------------------------
  public void test_of_additionConventionNone() {
    TenorAdjustment test = TenorAdjustment.of(Tenor.of(Period.of(1, 2, 3)), PAC_NONE, BDA_NONE);
    assertEquals(test.getTenor(), Tenor.of(Period.of(1, 2, 3)));
    assertEquals(test.getAdditionConvention(), PAC_NONE);
    assertEquals(test.getAdjustment(), BDA_NONE);
    assertEquals(test.toString(), "1Y2M3D");
  }

  public void test_of_additionConventionLastDay() {
    TenorAdjustment test = TenorAdjustment.of(TENOR_3M, LAST_DAY, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getAdditionConvention(), LAST_DAY);
    assertEquals(test.getAdjustment(), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.toString(), "3M with LastDay then apply Following using calendar Sat/Sun");
  }

  public void test_ofLastDay() {
    TenorAdjustment test = TenorAdjustment.ofLastDay(TENOR_3M, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getAdditionConvention(), LAST_DAY);
    assertEquals(test.getAdjustment(), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.toString(), "3M with LastDay then apply Following using calendar Sat/Sun");
  }

  public void test_ofLastBusinessDay() {
    TenorAdjustment test = TenorAdjustment.ofLastBusinessDay(TENOR_3M, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getAdditionConvention(), LAST_BUSINESS_DAY);
    assertEquals(test.getAdjustment(), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.toString(), "3M with LastBusinessDay then apply Following using calendar Sat/Sun");
  }

  public void test_of_invalid_conventionForPeriod() {
    assertThrowsIllegalArg(() -> TenorAdjustment.of(TENOR_1W, LAST_DAY, BDA_NONE));
    assertThrowsIllegalArg(() -> TenorAdjustment.of(TENOR_1W, LAST_BUSINESS_DAY, BDA_NONE));
    assertThrowsIllegalArg(() -> TenorAdjustment.ofLastDay(TENOR_1W, BDA_NONE));
    assertThrowsIllegalArg(() -> TenorAdjustment.ofLastBusinessDay(TENOR_1W, BDA_NONE));
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
    TenorAdjustment test = TenorAdjustment.of(Tenor.ofMonths(months), LAST_DAY, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.adjust(date, REF_DATA), expected);
    assertEquals(test.resolve(REF_DATA).adjust(date), expected);
  }

  //-------------------------------------------------------------------------
  public void equals() {
    TenorAdjustment a = TenorAdjustment.of(TENOR_3M, LAST_DAY, BDA_FOLLOW_SAT_SUN);
    TenorAdjustment b = TenorAdjustment.of(TENOR_1M, LAST_DAY, BDA_FOLLOW_SAT_SUN);
    TenorAdjustment c = TenorAdjustment.of(TENOR_3M, PAC_NONE, BDA_FOLLOW_SAT_SUN);
    TenorAdjustment d = TenorAdjustment.of(TENOR_3M, LAST_DAY, BDA_NONE);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(c), false);
    assertEquals(a.equals(d), false);
  }

  //-------------------------------------------------------------------------
  public void test_beanBuilder() {
    TenorAdjustment test = TenorAdjustment.builder()
        .tenor(TENOR_3M)
        .additionConvention(LAST_DAY)
        .adjustment(BDA_FOLLOW_SAT_SUN)
        .build();
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getAdditionConvention(), LAST_DAY);
    assertEquals(test.getAdjustment(), BDA_FOLLOW_SAT_SUN);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(TenorAdjustment.of(TENOR_3M, LAST_DAY, BDA_FOLLOW_SAT_SUN));
  }

  public void test_serialization() {
    assertSerialization(TenorAdjustment.of(TENOR_3M, LAST_DAY, BDA_FOLLOW_SAT_SUN));
  }

}
