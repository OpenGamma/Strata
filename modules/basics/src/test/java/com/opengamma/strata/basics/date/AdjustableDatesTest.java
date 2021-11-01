/*
 * Copyright (C) 2021 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link AdjustableDates}.
 */
public class AdjustableDatesTest {

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
  @Test
  public void test_of_noAdjustment() {
    AdjustableDates test = AdjustableDates.of(FRI_2014_07_11, SUN_2014_07_13);
    assertThat(test.getUnadjusted()).containsExactly(FRI_2014_07_11, SUN_2014_07_13);
    assertThat(test.getAdjustment()).isEqualTo(BDA_NONE);
    assertThat(test.toAdjustableDateList())
        .containsExactly(AdjustableDate.of(FRI_2014_07_11), AdjustableDate.of(SUN_2014_07_13));
    assertThat(test.toString()).isEqualTo("[2014-07-11, 2014-07-13]");
    assertThat(test.adjusted(REF_DATA)).containsExactly(FRI_2014_07_11, SUN_2014_07_13);
    assertThat(test).isEqualTo(AdjustableDates.of(ImmutableList.of(FRI_2014_07_11, SUN_2014_07_13)));
  }

  @Test
  public void test_of_withAdjustment() {
    AdjustableDates test = AdjustableDates.of(BDA_FOLLOW_SAT_SUN, FRI_2014_07_11, SUN_2014_07_13);
    assertThat(test.getUnadjusted()).containsExactly(FRI_2014_07_11, SUN_2014_07_13);
    assertThat(test.getAdjustment()).isEqualTo(BDA_FOLLOW_SAT_SUN);
    assertThat(test.toAdjustableDateList())
        .containsExactly(
            AdjustableDate.of(FRI_2014_07_11, BDA_FOLLOW_SAT_SUN),
            AdjustableDate.of(SUN_2014_07_13, BDA_FOLLOW_SAT_SUN));
    assertThat(test.toString()).isEqualTo("[2014-07-11, 2014-07-13] adjusted by Following using calendar Sat/Sun");
    assertThat(test.adjusted(REF_DATA)).containsExactly(FRI_2014_07_11, MON_2014_07_14);
    assertThat(test)
        .isEqualTo(AdjustableDates.of(BDA_FOLLOW_SAT_SUN, ImmutableList.of(FRI_2014_07_11, SUN_2014_07_13)));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    AdjustableDates test = AdjustableDates.of(FRI_2014_07_11);
    AdjustableDates test2 = AdjustableDates.of(BDA_FOLLOW_SAT_SUN, FRI_2014_07_11, SUN_2014_07_13);
    coverImmutableBean(test);
    coverBeanEquals(test, test2);
    assertSerialization(test2);
  }

}
