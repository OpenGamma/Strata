/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendars.SAT_SUN;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

/**
 * Test {@link BusinessDayAdjuster}.
 */
@Test
public class BusinessDayAdjusterTest {

  public void test_basics() {
    BusinessDayAdjuster test = BusinessDayAdjuster.of(MODIFIED_FOLLOWING, SAT_SUN);
    assertEquals(test.getConvention(), MODIFIED_FOLLOWING);
    assertEquals(test.getCalendar(), SAT_SUN);
    assertEquals(test.toString(), "ModifiedFollowing using calendar Sat/Sun");
  }

  @Test(dataProvider = "convention", dataProviderClass = BusinessDayConventionTest.class)
  public void test_convention(BusinessDayConvention convention, LocalDate input, LocalDate expected) {
    BusinessDayAdjuster test = BusinessDayAdjuster.of(convention, SAT_SUN);
    assertEquals(test.adjust(input), expected);
  }

  public void test_noAdjust_constant() {
    BusinessDayAdjuster test = BusinessDayAdjuster.NONE;
    assertEquals(test.getConvention(), BusinessDayConventions.NO_ADJUST);
    assertEquals(test.getCalendar(), HolidayCalendars.NO_HOLIDAYS);
    assertEquals(test.toString(), "NoAdjust");
  }

  public void test_noAdjust_factory() {
    BusinessDayAdjuster test = BusinessDayAdjuster.of(BusinessDayConventions.NO_ADJUST, HolidayCalendars.NO_HOLIDAYS);
    assertEquals(test.getConvention(), BusinessDayConventions.NO_ADJUST);
    assertEquals(test.getCalendar(), HolidayCalendars.NO_HOLIDAYS);
    assertEquals(test.toString(), "NoAdjust");
  }

  public void test_noAdjust_normalized() {
    BusinessDayAdjuster test = BusinessDayAdjuster.of(BusinessDayConventions.NO_ADJUST, SAT_SUN);
    assertEquals(test.getConvention(), BusinessDayConventions.NO_ADJUST);
    assertEquals(test.getCalendar(), SAT_SUN);
    assertEquals(test.toString(), "NoAdjust using calendar Sat/Sun");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(BusinessDayAdjuster.of(MODIFIED_FOLLOWING, SAT_SUN));
  }

  public void coverage_builder() {
    BusinessDayAdjuster test = BusinessDayAdjuster.builder()
        .convention(MODIFIED_FOLLOWING)
        .calendar(SAT_SUN)
        .build();
    assertEquals(test.getConvention(), MODIFIED_FOLLOWING);
    assertEquals(test.getCalendar(), SAT_SUN);
  }

  public void test_serialization() {
    assertSerialization(BusinessDayAdjuster.of(MODIFIED_FOLLOWING, SAT_SUN));
  }

}
