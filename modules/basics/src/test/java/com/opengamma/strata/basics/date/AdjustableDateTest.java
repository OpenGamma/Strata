/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link AdjustableDate}.
 */
@Test
public class AdjustableDateTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final BusinessDayAdjustment BDA_NONE = BusinessDayAdjustment.NONE;
  private static final BusinessDayAdjustment BDA_FOLLOW_SAT_SUN =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, HolidayCalendarIds.SAT_SUN);

  private static final LocalDate THU_2014_07_10 = LocalDate.of(2014, 7, 10);
  private static final LocalDate FRI_2014_07_11 = LocalDate.of(2014, 7, 11);
  private static final LocalDate SAT_2014_07_12 = LocalDate.of(2014, 7, 12);
  private static final LocalDate SUN_2014_07_13 = LocalDate.of(2014, 7, 13);
  private static final LocalDate MON_2014_07_14 = LocalDate.of(2014, 7, 14);
  private static final LocalDate TUE_2014_07_15 = LocalDate.of(2014, 7, 15);

  //-------------------------------------------------------------------------
  public void test_of_1arg() {
    AdjustableDate test = AdjustableDate.of(FRI_2014_07_11);
    assertEquals(test.getUnadjusted(), FRI_2014_07_11);
    assertEquals(test.getAdjustment(), BDA_NONE);
    assertEquals(test.toString(), "2014-07-11");
    assertEquals(test.adjusted(REF_DATA), FRI_2014_07_11);
  }

  public void test_of_2args_withAdjustment() {
    AdjustableDate test = AdjustableDate.of(FRI_2014_07_11, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.getUnadjusted(), FRI_2014_07_11);
    assertEquals(test.getAdjustment(), BDA_FOLLOW_SAT_SUN);
    assertEquals(test.toString(), "2014-07-11 adjusted by Following using calendar Sat/Sun");
    assertEquals(test.adjusted(REF_DATA), FRI_2014_07_11);
  }

  public void test_of_2args_withNoAdjustment() {
    AdjustableDate test = AdjustableDate.of(FRI_2014_07_11, BDA_NONE);
    assertEquals(test.getUnadjusted(), FRI_2014_07_11);
    assertEquals(test.getAdjustment(), BDA_NONE);
    assertEquals(test.toString(), "2014-07-11");
    assertEquals(test.adjusted(REF_DATA), FRI_2014_07_11);
  }

  public void test_of_null() {
    assertThrowsIllegalArg(() -> AdjustableDate.of(null));
    assertThrowsIllegalArg(() -> AdjustableDate.of(null, BDA_FOLLOW_SAT_SUN));
    assertThrowsIllegalArg(() -> AdjustableDate.of(FRI_2014_07_11, null));
    assertThrowsIllegalArg(() -> AdjustableDate.of(null, null));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "adjusted")
  static Object[][] data_adjusted() {
    return new Object[][] {
        {THU_2014_07_10, THU_2014_07_10},
        {FRI_2014_07_11, FRI_2014_07_11},
        {SAT_2014_07_12, MON_2014_07_14},
        {SUN_2014_07_13, MON_2014_07_14},
        {MON_2014_07_14, MON_2014_07_14},
        {TUE_2014_07_15, TUE_2014_07_15},
    };
  }

  @Test(dataProvider = "adjusted")
  public void test_adjusted(LocalDate date, LocalDate expected) {
    AdjustableDate test = AdjustableDate.of(date, BDA_FOLLOW_SAT_SUN);
    assertEquals(test.adjusted(REF_DATA), expected);
  }

  //-------------------------------------------------------------------------
  public void equals() {
    AdjustableDate a1 = AdjustableDate.of(FRI_2014_07_11, BDA_FOLLOW_SAT_SUN);
    AdjustableDate a2 = AdjustableDate.of(FRI_2014_07_11, BDA_FOLLOW_SAT_SUN);
    AdjustableDate b = AdjustableDate.of(SAT_2014_07_12, BDA_FOLLOW_SAT_SUN);
    AdjustableDate c = AdjustableDate.of(FRI_2014_07_11, BDA_NONE);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(c), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(AdjustableDate.of(FRI_2014_07_11, BDA_FOLLOW_SAT_SUN));
  }

  public void test_serialization() {
    assertSerialization(AdjustableDate.of(FRI_2014_07_11, BDA_FOLLOW_SAT_SUN));
  }

}
