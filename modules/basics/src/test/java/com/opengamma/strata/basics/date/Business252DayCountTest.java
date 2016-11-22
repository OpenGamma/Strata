/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link Business252DayCount}.
 */
@Test
public class Business252DayCountTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  public void test_factory_name() {
    DayCount test = DayCount.of("Bus/252 EUTA");
    assertEquals(test.getName(), "Bus/252 EUTA");
    assertEquals(test.toString(), "Bus/252 EUTA");

    assertSame(DayCount.of("Bus/252 EUTA"), test);
    assertSame(DayCount.ofBus252(EUTA), test);
  }

  public void test_factory_calendar() {
    DayCount test = DayCount.ofBus252(GBLO);
    assertEquals(test.getName(), "Bus/252 GBLO");
    assertEquals(test.toString(), "Bus/252 GBLO");

    assertSame(DayCount.of("Bus/252 GBLO"), test);
    assertSame(DayCount.ofBus252(GBLO), test);
  }

  //-------------------------------------------------------------------------
  public void test_yearFraction() {
    DayCount test = DayCount.of("Bus/252 EUTA");
    LocalDate date1 = date(2014, 12, 1);
    LocalDate date2 = date(2014, 12, 1);
    for (int i = 0; i < 366; i++) {
      assertEquals(test.yearFraction(date1, date2), EUTA.resolve(REF_DATA).daysBetween(date1, date2) / 252d);
      date2 = date2.plusDays(1);
    }
  }

  public void test_yearFraction_badOrder() {
    DayCount test = DayCount.of("Bus/252 EUTA");
    LocalDate date1 = date(2014, 12, 2);
    LocalDate date2 = date(2014, 12, 1);
    assertThrowsIllegalArg(() -> test.yearFraction(date1, date2));
  }

  public void test_days() {
    DayCount test = DayCount.of("Bus/252 EUTA");
    LocalDate date1 = date(2014, 12, 1);
    LocalDate date2 = date(2014, 12, 1);
    for (int i = 0; i < 366; i++) {
      assertEquals(test.days(date1, date2), EUTA.resolve(REF_DATA).daysBetween(date1, date2));
      date2 = date2.plusDays(1);
    }
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    DayCount a = DayCount.of("Bus/252 EUTA");
    DayCount b = DayCount.of("Bus/252 GBLO");
    assertEquals(a.equals(a), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals("Rubbish"), false);
    assertEquals(a.equals(null), false);
    assertEquals(a.hashCode(), a.hashCode());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(Business252DayCount.class);
  }

  public void test_serialization() {
    assertSerialization(DayCount.ofBus252(EUTA));
  }

  public void test_jodaConvert() {
    assertJodaConvert(DayCount.class, DayCount.ofBus252(EUTA));
  }

}
