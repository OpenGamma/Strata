/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.HolidayCalendarIds.NO_HOLIDAYS;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SAT_SUN;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link DaysAdjustment}.
 */
public class DaysAdjustmentTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final HolidayCalendarId WED_THU = HolidayCalendarId.of("WedThu");
  private static final BusinessDayAdjustment BDA_NONE = BusinessDayAdjustment.NONE;
  private static final BusinessDayAdjustment BDA_FOLLOW_SAT_SUN =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, SAT_SUN);
  private static final BusinessDayAdjustment BDA_FOLLOW_WED_THU =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, WED_THU);

  //-------------------------------------------------------------------------
  @Test
  public void test_NONE() {
    DaysAdjustment test = DaysAdjustment.NONE;
    assertThat(test.getDays()).isEqualTo(0);
    assertThat(test.getCalendar()).isEqualTo(NO_HOLIDAYS);
    assertThat(test.getAdjustment()).isEqualTo(BDA_NONE);
    assertThat(test.toString()).isEqualTo("0 calendar days");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_ofCalendarDays1_oneDay() {
    DaysAdjustment test = DaysAdjustment.ofCalendarDays(1);
    assertThat(test.getDays()).isEqualTo(1);
    assertThat(test.getCalendar()).isEqualTo(NO_HOLIDAYS);
    assertThat(test.getAdjustment()).isEqualTo(BDA_NONE);
    assertThat(test.toString()).isEqualTo("1 calendar day");
  }

  @Test
  public void test_ofCalendarDays1_threeDays() {
    DaysAdjustment test = DaysAdjustment.ofCalendarDays(3);
    assertThat(test.getDays()).isEqualTo(3);
    assertThat(test.getCalendar()).isEqualTo(NO_HOLIDAYS);
    assertThat(test.getAdjustment()).isEqualTo(BDA_NONE);
    assertThat(test.toString()).isEqualTo("3 calendar days");
  }

  @Test
  public void test_ofCalendarDays1_adjust() {
    DaysAdjustment test = DaysAdjustment.ofCalendarDays(2);
    LocalDate base = date(2014, 8, 15);  // Fri
    assertThat(test.adjust(base, REF_DATA)).isEqualTo(date(2014, 8, 17));  // Sun
    assertThat(test.resolve(REF_DATA).adjust(base)).isEqualTo(date(2014, 8, 17));  // Sun
  }

  @Test
  public void test_ofCalendarDays2_oneDay() {
    DaysAdjustment test = DaysAdjustment.ofCalendarDays(1, BDA_FOLLOW_SAT_SUN);
    assertThat(test.getDays()).isEqualTo(1);
    assertThat(test.getCalendar()).isEqualTo(NO_HOLIDAYS);
    assertThat(test.getAdjustment()).isEqualTo(BDA_FOLLOW_SAT_SUN);
    assertThat(test.toString()).isEqualTo("1 calendar day then apply Following using calendar Sat/Sun");
  }

  @Test
  public void test_ofCalendarDays2_fourDays() {
    DaysAdjustment test = DaysAdjustment.ofCalendarDays(4, BDA_FOLLOW_SAT_SUN);
    assertThat(test.getDays()).isEqualTo(4);
    assertThat(test.getCalendar()).isEqualTo(NO_HOLIDAYS);
    assertThat(test.getAdjustment()).isEqualTo(BDA_FOLLOW_SAT_SUN);
    assertThat(test.toString()).isEqualTo("4 calendar days then apply Following using calendar Sat/Sun");
  }

  @Test
  public void test_ofCalendarDays2_adjust() {
    DaysAdjustment test = DaysAdjustment.ofCalendarDays(2, BDA_FOLLOW_SAT_SUN);
    LocalDate base = date(2014, 8, 15);  // Fri
    assertThat(test.adjust(base, REF_DATA)).isEqualTo(date(2014, 8, 18));  // Mon
    assertThat(test.resolve(REF_DATA).adjust(base)).isEqualTo(date(2014, 8, 18));  // Mon
  }

  @Test
  public void test_ofCalendarDays2_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> DaysAdjustment.ofCalendarDays(2, null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_ofBusinessDays2_oneDay() {
    DaysAdjustment test = DaysAdjustment.ofBusinessDays(1, SAT_SUN);
    assertThat(test.getDays()).isEqualTo(1);
    assertThat(test.getCalendar()).isEqualTo(SAT_SUN);
    assertThat(test.getAdjustment()).isEqualTo(BDA_NONE);
    assertThat(test.toString()).isEqualTo("1 business day using calendar Sat/Sun");
  }

  @Test
  public void test_ofBusinessDays2_threeDays() {
    DaysAdjustment test = DaysAdjustment.ofBusinessDays(3, SAT_SUN);
    assertThat(test.getDays()).isEqualTo(3);
    assertThat(test.getCalendar()).isEqualTo(SAT_SUN);
    assertThat(test.getAdjustment()).isEqualTo(BDA_NONE);
    assertThat(test.toString()).isEqualTo("3 business days using calendar Sat/Sun");
  }

  @Test
  public void test_ofBusinessDays2_adjust() {
    DaysAdjustment test = DaysAdjustment.ofBusinessDays(2, SAT_SUN);
    LocalDate base = date(2014, 8, 15);  // Fri
    assertThat(test.adjust(base, REF_DATA)).isEqualTo(date(2014, 8, 19));  // Tue
    assertThat(test.resolve(REF_DATA).adjust(base)).isEqualTo(date(2014, 8, 19));  // Tue
  }

  @Test
  public void test_ofBusinessDays2_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> DaysAdjustment.ofBusinessDays(2, null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_ofBusinessDays3_oneDay() {
    DaysAdjustment test = DaysAdjustment.ofBusinessDays(1, SAT_SUN, BDA_FOLLOW_WED_THU);
    assertThat(test.getDays()).isEqualTo(1);
    assertThat(test.getCalendar()).isEqualTo(SAT_SUN);
    assertThat(test.getAdjustment()).isEqualTo(BDA_FOLLOW_WED_THU);
    assertThat(test.toString())
        .isEqualTo("1 business day using calendar Sat/Sun then apply Following using calendar WedThu");
  }

  @Test
  public void test_ofBusinessDays3_fourDays() {
    DaysAdjustment test = DaysAdjustment.ofBusinessDays(4, SAT_SUN, BDA_FOLLOW_WED_THU);
    assertThat(test.getDays()).isEqualTo(4);
    assertThat(test.getCalendar()).isEqualTo(SAT_SUN);
    assertThat(test.getAdjustment()).isEqualTo(BDA_FOLLOW_WED_THU);
    assertThat(test.toString())
        .isEqualTo("4 business days using calendar Sat/Sun then apply Following using calendar WedThu");
  }

  @Test
  public void test_ofBusinessDays3_adjust() {
    ImmutableHolidayCalendar cal = ImmutableHolidayCalendar.of(WED_THU, ImmutableList.of(), WEDNESDAY, THURSDAY);
    ReferenceData refData = ImmutableReferenceData.of(ImmutableMap.of(WED_THU, cal)).combinedWith(REF_DATA);
    DaysAdjustment test = DaysAdjustment.ofBusinessDays(3, SAT_SUN, BDA_FOLLOW_WED_THU);
    LocalDate base = date(2014, 8, 15);  // Fri
    assertThat(test.adjust(base, refData)).isEqualTo(date(2014, 8, 22));  // Fri (3 days gives Wed, following moves to Fri)
    assertThat(test.resolve(refData).adjust(base)).isEqualTo(date(2014, 8, 22));  // Fri (3 days gives Wed, following moves to Fri)
  }

  @Test
  public void test_ofBusinessDays3_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> DaysAdjustment.ofBusinessDays(3, null, BDA_FOLLOW_SAT_SUN));
    assertThatIllegalArgumentException().isThrownBy(() -> DaysAdjustment.ofBusinessDays(3, SAT_SUN, null));
    assertThatIllegalArgumentException().isThrownBy(() -> DaysAdjustment.ofBusinessDays(3, null, null));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_getResultCalendar1() {
    DaysAdjustment test = DaysAdjustment.ofBusinessDays(3, SAT_SUN);
    assertThat(test.getResultCalendar()).isEqualTo(SAT_SUN);
  }

  @Test
  public void test_getResultCalendar2() {
    DaysAdjustment test = DaysAdjustment.ofBusinessDays(3, SAT_SUN, BDA_FOLLOW_WED_THU);
    assertThat(test.getResultCalendar()).isEqualTo(WED_THU);
  }

  @Test
  public void test_getResultCalendar3() {
    DaysAdjustment test = DaysAdjustment.ofCalendarDays(3);
    assertThat(test.getResultCalendar()).isEqualTo(NO_HOLIDAYS);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_normalized() {
    DaysAdjustment zeroDays = DaysAdjustment.ofCalendarDays(0, BDA_FOLLOW_SAT_SUN);
    DaysAdjustment zeroDaysWithCalendar = DaysAdjustment.ofBusinessDays(0, WED_THU, BDA_FOLLOW_SAT_SUN);
    DaysAdjustment twoDays = DaysAdjustment.ofCalendarDays(2, BDA_FOLLOW_SAT_SUN);
    DaysAdjustment twoDaysWithCalendar = DaysAdjustment.ofBusinessDays(2, WED_THU, BDA_FOLLOW_SAT_SUN);
    DaysAdjustment twoDaysWithSameCalendar = DaysAdjustment.ofBusinessDays(2, SAT_SUN, BDA_FOLLOW_SAT_SUN);
    DaysAdjustment twoDaysWithNoAdjust = DaysAdjustment.ofBusinessDays(2, SAT_SUN);
    assertThat(zeroDays.normalized()).isEqualTo(zeroDays);
    assertThat(zeroDaysWithCalendar.normalized()).isEqualTo(zeroDays);
    assertThat(twoDays.normalized()).isEqualTo(twoDays);
    assertThat(twoDaysWithCalendar.normalized()).isEqualTo(twoDaysWithCalendar);
    assertThat(twoDaysWithSameCalendar.normalized()).isEqualTo(twoDaysWithNoAdjust);
    assertThat(twoDaysWithNoAdjust.normalized()).isEqualTo(twoDaysWithNoAdjust);
  }

  //-------------------------------------------------------------------------
  @Test
  public void equals() {
    DaysAdjustment a = DaysAdjustment.ofBusinessDays(3, NO_HOLIDAYS, BDA_FOLLOW_SAT_SUN);
    DaysAdjustment b = DaysAdjustment.ofBusinessDays(4, NO_HOLIDAYS, BDA_FOLLOW_SAT_SUN);
    DaysAdjustment c = DaysAdjustment.ofBusinessDays(3, WED_THU, BDA_FOLLOW_SAT_SUN);
    DaysAdjustment d = DaysAdjustment.ofBusinessDays(3, NO_HOLIDAYS, BDA_FOLLOW_WED_THU);
    assertThat(a.equals(b)).isEqualTo(false);
    assertThat(a.equals(c)).isEqualTo(false);
    assertThat(a.equals(d)).isEqualTo(false);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(DaysAdjustment.ofCalendarDays(4, BDA_FOLLOW_SAT_SUN));
  }

  @Test
  public void coverage_builder() {
    DaysAdjustment test = DaysAdjustment.builder()
        .days(1)
        .calendar(SAT_SUN)
        .adjustment(BDA_FOLLOW_WED_THU)
        .build();
    assertThat(test.getDays()).isEqualTo(1);
    assertThat(test.getCalendar()).isEqualTo(SAT_SUN);
    assertThat(test.getAdjustment()).isEqualTo(BDA_FOLLOW_WED_THU);
  }

  @Test
  public void test_serialization() {
    assertSerialization(DaysAdjustment.ofCalendarDays(4, BDA_FOLLOW_SAT_SUN));
  }

}
