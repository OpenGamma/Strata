/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.HolidayCalendarIds.NO_HOLIDAYS;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;

/**
 * Test {@link HolidayCalendarId}.
 */
@Test
public class HolidayCalendarIdTest {

  public void test_of_single() {
    HolidayCalendarId test = HolidayCalendarId.of("GB");
    assertEquals(test.getName(), "GB");
    assertEquals(test.getReferenceDataType(), HolidayCalendar.class);
    assertEquals(test.toString(), "GB");
  }

  public void test_of_combined() {
    HolidayCalendarId test = HolidayCalendarId.of("GB+EU");
    assertEquals(test.getName(), "EU+GB");
    assertEquals(test.getReferenceDataType(), HolidayCalendar.class);
    assertEquals(test.toString(), "EU+GB");

    HolidayCalendarId test2 = HolidayCalendarId.of("EU+GB");
    assertSame(test, test2);
  }

  public void test_of_combined_NoHolidays() {
    HolidayCalendarId test = HolidayCalendarId.of("GB+NoHolidays+EU");
    assertEquals(test.getName(), "EU+GB");
    assertEquals(test.getReferenceDataType(), HolidayCalendar.class);
    assertEquals(test.toString(), "EU+GB");
  }

  //-------------------------------------------------------------------------
  public void test_resolve_single() {
    HolidayCalendarId gb = HolidayCalendarId.of("GB");
    HolidayCalendarId eu = HolidayCalendarId.of("EU");
    HolidayCalendar gbCal = HolidayCalendars.SAT_SUN;
    ReferenceData refData = ImmutableReferenceData.of(gb, gbCal);
    assertEquals(gb.resolve(refData), gbCal);
    assertThrows(() -> eu.resolve(refData), ReferenceDataNotFoundException.class);
    assertEquals(refData.getValue(gb), gbCal);
  }

  public void test_resolve_combined_direct() {
    HolidayCalendarId gb = HolidayCalendarId.of("GB");
    HolidayCalendar gbCal = HolidayCalendars.SAT_SUN;
    HolidayCalendarId eu = HolidayCalendarId.of("EU");
    HolidayCalendar euCal = HolidayCalendars.FRI_SAT;
    HolidayCalendarId combined = gb.combinedWith(eu);
    HolidayCalendar combinedCal = euCal.combinedWith(gbCal);
    ReferenceData refData = ImmutableReferenceData.of(ImmutableMap.of(combined, combinedCal));
    assertEquals(combined.resolve(refData), combinedCal);
    assertEquals(refData.getValue(combined), combinedCal);
  }

  public void test_resolve_combined_indirect() {
    HolidayCalendarId gb = HolidayCalendarId.of("GB");
    HolidayCalendar gbCal = HolidayCalendars.SAT_SUN;
    HolidayCalendarId eu = HolidayCalendarId.of("EU");
    HolidayCalendar euCal = HolidayCalendars.FRI_SAT;
    HolidayCalendarId combined = gb.combinedWith(eu);
    HolidayCalendar combinedCal = euCal.combinedWith(gbCal);
    ReferenceData refData = ImmutableReferenceData.of(ImmutableMap.of(gb, gbCal, eu, euCal));
    assertEquals(combined.resolve(refData), combinedCal);
    assertEquals(refData.getValue(combined), combinedCal);
  }

  @Test
  public void testImmutableReferenceDataWithMergedHolidays() {
    HolidayCalendar hc = HolidayCalendars.FRI_SAT.combinedWith(HolidayCalendars.SAT_SUN);
    ImmutableReferenceData referenceData = ImmutableReferenceData.of(hc.getId(), hc);
    LocalDate date =
        BusinessDayAdjustment.of(BusinessDayConventions.PRECEDING, hc.getId()).adjust(LocalDate.of(2016, 8, 20), referenceData);
    assertEquals(LocalDate.of(2016, 8, 18), date);
  }

  //-------------------------------------------------------------------------
  public void test_combinedWith() {
    HolidayCalendarId gb = HolidayCalendarId.of("GB");
    HolidayCalendarId eu = HolidayCalendarId.of("EU");
    HolidayCalendarId us = HolidayCalendarId.of("US");
    HolidayCalendarId combined1 = eu.combinedWith(us).combinedWith(gb);
    HolidayCalendarId combined2 = us.combinedWith(eu).combinedWith(gb.combinedWith(us));
    assertEquals(combined1.getName(), "EU+GB+US");
    assertEquals(combined1.toString(), "EU+GB+US");
    assertEquals(combined2.getName(), "EU+GB+US");
    assertEquals(combined2.toString(), "EU+GB+US");
    assertEquals(combined1.equals(combined2), true);
  }

  public void test_combinedWithSelf() {
    HolidayCalendarId gb = HolidayCalendarId.of("GB");
    assertEquals(gb.combinedWith(gb), gb);
    assertEquals(gb.combinedWith(NO_HOLIDAYS), gb);
    assertEquals(NO_HOLIDAYS.combinedWith(gb), gb);
    assertEquals(NO_HOLIDAYS.combinedWith(NO_HOLIDAYS), NO_HOLIDAYS);
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    HolidayCalendarId a = HolidayCalendarId.of("GB");
    HolidayCalendarId a2 = HolidayCalendarId.of("GB");
    HolidayCalendarId b = HolidayCalendarId.of("EU");
    assertEquals(a.hashCode(), a2.hashCode());
    assertEquals(a.equals(a), true);
    assertEquals(a.equals(a2), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals("Rubbish"), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(HolidayCalendarIds.class);
  }

  public void test_serialization() {
    HolidayCalendarId test = HolidayCalendarId.of("US");
    assertSerialization(test);
  }

}
