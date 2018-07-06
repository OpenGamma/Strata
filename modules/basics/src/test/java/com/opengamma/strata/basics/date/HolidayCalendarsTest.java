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
import static org.testng.Assert.assertEquals;

import org.joda.beans.ImmutableBean;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.TestingReferenceDataId;

/**
 * Test {@link HolidayCalendars}.
 */
@Test
public class HolidayCalendarsTest {

  private static final ImmutableHolidayCalendar DEFAULTED_FRI_SAT =
      ImmutableHolidayCalendar.of(HolidayCalendarIds.FRI_SAT, ImmutableList.of(), ImmutableList.of(SATURDAY, SUNDAY));
  private static final ImmutableHolidayCalendar DEFAULTED_GBLO =
      ImmutableHolidayCalendar.of(HolidayCalendarIds.GBLO, ImmutableList.of(), ImmutableList.of(SATURDAY, SUNDAY));

  //-------------------------------------------------------------------------
  public void test_defaulting() {
    ReferenceData base = ImmutableReferenceData.of(ImmutableMap.of(HolidayCalendarIds.FRI_SAT, HolidayCalendars.FRI_SAT));

    ReferenceData test = HolidayCalendars.defaultingReferenceData(base);
    assertEquals(test.getValue(HolidayCalendarIds.FRI_SAT), HolidayCalendars.FRI_SAT);
    assertEquals(test.getValue(HolidayCalendarIds.GBLO), DEFAULTED_GBLO);
    assertEquals(test.containsValue(HolidayCalendarIds.FRI_SAT), true);
    assertEquals(test.containsValue(HolidayCalendarIds.GBLO), true);
    assertEquals(test.containsValue(new TestingReferenceDataId("1")), false);
  }

  public void test_defaulting_combinedWith() {
    ReferenceData base1 = ImmutableReferenceData.of(ImmutableMap.of(HolidayCalendarIds.THU_FRI, HolidayCalendars.THU_FRI));
    ReferenceData base2 = ImmutableReferenceData.of(ImmutableMap.of(
        HolidayCalendarIds.THU_FRI, HolidayCalendars.FRI_SAT, HolidayCalendarIds.FRI_SAT, HolidayCalendars.FRI_SAT));

    ReferenceData testDefaulted = HolidayCalendars.defaultingReferenceData(base1);
    assertEquals(testDefaulted.getValue(HolidayCalendarIds.THU_FRI), HolidayCalendars.THU_FRI);
    assertEquals(testDefaulted.getValue(HolidayCalendarIds.FRI_SAT), DEFAULTED_FRI_SAT);
    assertEquals(testDefaulted.getValue(HolidayCalendarIds.GBLO), DEFAULTED_GBLO);

    ReferenceData testCombined = testDefaulted.combinedWith(base2);
    assertEquals(testCombined.getValue(HolidayCalendarIds.THU_FRI), HolidayCalendars.THU_FRI);  // test1 takes precedence
    assertEquals(testCombined.getValue(HolidayCalendarIds.FRI_SAT), HolidayCalendars.FRI_SAT);  // from test2
    assertEquals(testCombined.getValue(HolidayCalendarIds.GBLO), DEFAULTED_GBLO);  // from default

    ReferenceData testCombinedRevered = base2.combinedWith(testDefaulted);
    assertEquals(testCombinedRevered.getValue(HolidayCalendarIds.THU_FRI), HolidayCalendars.FRI_SAT);  // test2 takes precedence
    assertEquals(testCombinedRevered.getValue(HolidayCalendarIds.FRI_SAT), HolidayCalendars.FRI_SAT);  // from test2
    assertEquals(testCombinedRevered.getValue(HolidayCalendarIds.GBLO), DEFAULTED_GBLO);  // from default
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(HolidayCalendars.class);
  }

  public void coverage_combined() {
    HolidayCalendar test = HolidayCalendars.FRI_SAT.combinedWith(HolidayCalendars.SAT_SUN);
    coverImmutableBean((ImmutableBean) test);
  }

  public void coverage_noHolidays() {
    HolidayCalendar test = HolidayCalendars.NO_HOLIDAYS;
    coverImmutableBean((ImmutableBean) test);
  }

  public void coverage_weekend() {
    HolidayCalendar test = HolidayCalendars.FRI_SAT;
    coverImmutableBean((ImmutableBean) test);
  }

  public void test_serialization() {
    assertSerialization(HolidayCalendars.NO_HOLIDAYS);
    assertSerialization(HolidayCalendars.SAT_SUN);
    assertSerialization(HolidayCalendars.FRI_SAT);
    assertSerialization(HolidayCalendars.THU_FRI);
    assertSerialization(HolidayCalendars.FRI_SAT.combinedWith(HolidayCalendars.SAT_SUN));
  }

}
