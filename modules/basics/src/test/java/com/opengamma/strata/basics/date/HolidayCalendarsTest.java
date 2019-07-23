/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static org.assertj.core.api.Assertions.assertThat;

import org.joda.beans.ImmutableBean;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.TestingReferenceDataId;

/**
 * Test {@link HolidayCalendars}.
 */
public class HolidayCalendarsTest {

  private static final ImmutableHolidayCalendar DEFAULTED_FRI_SAT =
      ImmutableHolidayCalendar.of(HolidayCalendarIds.FRI_SAT, ImmutableList.of(), ImmutableList.of(SATURDAY, SUNDAY));
  private static final ImmutableHolidayCalendar DEFAULTED_GBLO =
      ImmutableHolidayCalendar.of(HolidayCalendarIds.GBLO, ImmutableList.of(), ImmutableList.of(SATURDAY, SUNDAY));

  //-------------------------------------------------------------------------
  @Test
  public void test_defaulting() {
    ReferenceData base = ImmutableReferenceData.of(ImmutableMap.of(HolidayCalendarIds.FRI_SAT, HolidayCalendars.FRI_SAT));

    ReferenceData test = HolidayCalendars.defaultingReferenceData(base);
    assertThat(test.getValue(HolidayCalendarIds.FRI_SAT)).isEqualTo(HolidayCalendars.FRI_SAT);
    assertThat(test.getValue(HolidayCalendarIds.GBLO)).isEqualTo(DEFAULTED_GBLO);
    assertThat(test.containsValue(HolidayCalendarIds.FRI_SAT)).isEqualTo(true);
    assertThat(test.containsValue(HolidayCalendarIds.GBLO)).isEqualTo(true);
    assertThat(test.containsValue(new TestingReferenceDataId("1"))).isEqualTo(false);
  }

  @Test
  public void test_defaulting_combinedWith() {
    ReferenceData base1 = ImmutableReferenceData.of(ImmutableMap.of(HolidayCalendarIds.THU_FRI, HolidayCalendars.THU_FRI));
    ReferenceData base2 = ImmutableReferenceData.of(ImmutableMap.of(
        HolidayCalendarIds.THU_FRI, HolidayCalendars.FRI_SAT, HolidayCalendarIds.FRI_SAT, HolidayCalendars.FRI_SAT));

    ReferenceData testDefaulted = HolidayCalendars.defaultingReferenceData(base1);
    assertThat(testDefaulted.getValue(HolidayCalendarIds.THU_FRI)).isEqualTo(HolidayCalendars.THU_FRI);
    assertThat(testDefaulted.getValue(HolidayCalendarIds.FRI_SAT)).isEqualTo(DEFAULTED_FRI_SAT);
    assertThat(testDefaulted.getValue(HolidayCalendarIds.GBLO)).isEqualTo(DEFAULTED_GBLO);

    ReferenceData testCombined = testDefaulted.combinedWith(base2);
    assertThat(testCombined.getValue(HolidayCalendarIds.THU_FRI)).isEqualTo(HolidayCalendars.THU_FRI);  // test1 takes precedence
    assertThat(testCombined.getValue(HolidayCalendarIds.FRI_SAT)).isEqualTo(HolidayCalendars.FRI_SAT);  // from test2
    assertThat(testCombined.getValue(HolidayCalendarIds.GBLO)).isEqualTo(DEFAULTED_GBLO);  // from default

    ReferenceData testCombinedRevered = base2.combinedWith(testDefaulted);
    assertThat(testCombinedRevered.getValue(HolidayCalendarIds.THU_FRI)).isEqualTo(HolidayCalendars.FRI_SAT);  // test2 takes precedence
    assertThat(testCombinedRevered.getValue(HolidayCalendarIds.FRI_SAT)).isEqualTo(HolidayCalendars.FRI_SAT);  // from test2
    assertThat(testCombinedRevered.getValue(HolidayCalendarIds.GBLO)).isEqualTo(DEFAULTED_GBLO);  // from default
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(HolidayCalendars.class);
  }

  @Test
  public void coverage_combined() {
    HolidayCalendar test = HolidayCalendars.FRI_SAT.combinedWith(HolidayCalendars.SAT_SUN);
    coverImmutableBean((ImmutableBean) test);
  }

  @Test
  public void coverage_noHolidays() {
    HolidayCalendar test = HolidayCalendars.NO_HOLIDAYS;
    coverImmutableBean((ImmutableBean) test);
  }

  @Test
  public void coverage_weekend() {
    HolidayCalendar test = HolidayCalendars.FRI_SAT;
    coverImmutableBean((ImmutableBean) test);
  }

  @Test
  public void test_serialization() {
    assertSerialization(HolidayCalendars.NO_HOLIDAYS);
    assertSerialization(HolidayCalendars.SAT_SUN);
    assertSerialization(HolidayCalendars.FRI_SAT);
    assertSerialization(HolidayCalendars.THU_FRI);
    assertSerialization(HolidayCalendars.FRI_SAT.combinedWith(HolidayCalendars.SAT_SUN));
  }

}
