/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.date;

import static com.opengamma.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.basics.date.HolidayCalendar.WEEKENDS;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

/**
 * Test {@link BusinessDayAdjustment}.
 */
@Test
public class BusinessDayAdjustmentTest {

  public void test_basics() {
    BusinessDayAdjustment test = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, WEEKENDS);
    assertEquals(test.getConvention(), MODIFIED_FOLLOWING);
    assertEquals(test.getCalendar(), WEEKENDS);
    assertEquals(test.toString(), "ModifiedFollowing using calendar Weekends");
  }

  @Test(dataProvider = "convention", dataProviderClass = BusinessDayConventionTest.class)
  public void test_convention(BusinessDayConvention convention, LocalDate input, LocalDate expected) {
    BusinessDayAdjustment test = BusinessDayAdjustment.of(convention, WEEKENDS);
    assertEquals(test.adjust(input), expected);
  }

  public void test_noAdjust_constant() {
    BusinessDayAdjustment test = BusinessDayAdjustment.NONE;
    assertEquals(test.getConvention(), BusinessDayConventions.NO_ADJUST);
    assertEquals(test.getCalendar(), HolidayCalendar.NONE);
    assertEquals(test.toString(), "NoAdjust");
  }

  public void test_noAdjust_factory() {
    BusinessDayAdjustment test = BusinessDayAdjustment.of(BusinessDayConventions.NO_ADJUST, HolidayCalendar.NONE);
    assertEquals(test.getConvention(), BusinessDayConventions.NO_ADJUST);
    assertEquals(test.getCalendar(), HolidayCalendar.NONE);
    assertEquals(test.toString(), "NoAdjust");
  }

  public void test_noAdjust_normalized() {
    BusinessDayAdjustment test = BusinessDayAdjustment.of(BusinessDayConventions.NO_ADJUST, WEEKENDS);
    assertEquals(test.getConvention(), BusinessDayConventions.NO_ADJUST);
    assertEquals(test.getCalendar(), HolidayCalendar.WEEKENDS);
    assertEquals(test.toString(), "NoAdjust using calendar Weekends");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, WEEKENDS));
  }

  public void test_serialization() {
    assertSerialization(BusinessDayAdjustment.of(MODIFIED_FOLLOWING, WEEKENDS));
  }

}
