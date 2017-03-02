/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.NO_HOLIDAYS;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link BusinessDayAdjustment}.
 */
@Test
public class BusinessDayAdjustmentTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  public void test_basics() {
    BusinessDayAdjustment test = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN);
    assertEquals(test.getConvention(), MODIFIED_FOLLOWING);
    assertEquals(test.getCalendar(), SAT_SUN);
    assertEquals(test.toString(), "ModifiedFollowing using calendar Sat/Sun");
  }

  @Test(dataProvider = "convention", dataProviderClass = BusinessDayConventionTest.class)
  public void test_adjustDate(BusinessDayConvention convention, LocalDate input, LocalDate expected) {
    BusinessDayAdjustment test = BusinessDayAdjustment.of(convention, SAT_SUN);
    assertEquals(test.adjust(input, REF_DATA), expected);
    assertEquals(test.resolve(REF_DATA).adjust(input), expected);
  }

  public void test_noAdjust_constant() {
    BusinessDayAdjustment test = BusinessDayAdjustment.NONE;
    assertEquals(test.getConvention(), BusinessDayConventions.NO_ADJUST);
    assertEquals(test.getCalendar(), NO_HOLIDAYS);
    assertEquals(test.toString(), "NoAdjust");
  }

  public void test_noAdjust_factory() {
    BusinessDayAdjustment test = BusinessDayAdjustment.of(BusinessDayConventions.NO_ADJUST, NO_HOLIDAYS);
    assertEquals(test.getConvention(), BusinessDayConventions.NO_ADJUST);
    assertEquals(test.getCalendar(), NO_HOLIDAYS);
    assertEquals(test.toString(), "NoAdjust");
  }

  public void test_noAdjust_normalized() {
    BusinessDayAdjustment test = BusinessDayAdjustment.of(BusinessDayConventions.NO_ADJUST, SAT_SUN);
    assertEquals(test.getConvention(), BusinessDayConventions.NO_ADJUST);
    assertEquals(test.getCalendar(), SAT_SUN);
    assertEquals(test.toString(), "NoAdjust using calendar Sat/Sun");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN));
  }

  public void coverage_builder() {
    BusinessDayAdjustment test = BusinessDayAdjustment.builder()
        .convention(MODIFIED_FOLLOWING)
        .calendar(SAT_SUN)
        .build();
    assertEquals(test.getConvention(), MODIFIED_FOLLOWING);
    assertEquals(test.getCalendar(), SAT_SUN);
  }

  public void test_serialization() {
    assertSerialization(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN));
  }

}
