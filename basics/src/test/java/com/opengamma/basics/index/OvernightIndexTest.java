/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.index;

import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.currency.Currency.USD;
import static com.opengamma.basics.date.DayCounts.ACT_360;
import static com.opengamma.basics.date.DayCounts.ACT_365F;
import static com.opengamma.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.basics.date.HolidayCalendars.NYFD;
import static com.opengamma.basics.date.HolidayCalendars.USNY;
import static com.opengamma.basics.date.Tenor.TENOR_1D;
import static com.opengamma.collect.TestHelper.assertJodaConvert;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrows;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.basics.currency.Currency;

/**
 * Test Overnight Index.
 */
@Test
public class OvernightIndexTest {

  public void test_null() {
    OvernightIndex test = OvernightIndex.of("GBP-SONIA");
    assertThrows(() -> test.calculatePublicationFromFixing(null), IllegalArgumentException.class);
    assertThrows(() -> test.calculateEffectiveFromFixing(null), IllegalArgumentException.class);
    assertThrows(() -> test.calculateFixingFromEffective(null), IllegalArgumentException.class);
    assertThrows(() -> test.calculateMaturityFromEffective(null), IllegalArgumentException.class);
  }

  public void test_gbpSonia() {
    OvernightIndex test = OvernightIndex.of("GBP-SONIA");
    assertEquals(test.getType(), RateIndexType.OVERNIGHT);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getName(), "GBP-SONIA");
    assertEquals(test.getTenor(), TENOR_1D);
    assertEquals(test.getCalendar(), GBLO);
    assertEquals(test.getPublicationDateOffset(), 0);
    assertEquals(test.getEffectiveDateOffset(), 0);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.toString(), "GBP-SONIA");
  }

  public void test_gbpSonia_dates() {
    OvernightIndex test = OvernightIndex.of("GBP-SONIA");
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 13)), date(2014, 10, 13));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 13)), date(2014, 10, 13));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 13)), date(2014, 10, 13));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 13)), date(2014, 10, 14));
    // weekend
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 10)), date(2014, 10, 10));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 10)), date(2014, 10, 10));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 10)), date(2014, 10, 10));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 10)), date(2014, 10, 13));
    // input date is Sunday
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 12)), date(2014, 10, 13));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 12)), date(2014, 10, 13));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 12)), date(2014, 10, 13));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 12)), date(2014, 10, 14));
  }

  public void test_usdFedFund3m() {
    OvernightIndex test = OvernightIndex.of("USD-FED-FUND");
    assertEquals(test.getType(), RateIndexType.OVERNIGHT);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getName(), "USD-FED-FUND");
    assertEquals(test.getTenor(), TENOR_1D);
    assertEquals(test.getCalendar(), NYFD);
    assertEquals(test.getPublicationDateOffset(), 1);
    assertEquals(test.getEffectiveDateOffset(), 0);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.toString(), "USD-FED-FUND");
  }

  public void test_usdFedFund_dates() {
    OvernightIndex test = OvernightIndex.of("USD-FED-FUND");
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 27)), date(2014, 10, 28));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 27)), date(2014, 10, 27));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 27)), date(2014, 10, 27));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 27)), date(2014, 10, 28));
    // weekend and US holiday
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 10)), date(2014, 10, 14));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 10)), date(2014, 10, 10));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 10)), date(2014, 10, 10));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 10)), date(2014, 10, 14));
    // input date is Sunday, 13th is US holiday
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 12)), date(2014, 10, 15));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 12)), date(2014, 10, 14));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 12)), date(2014, 10, 14));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 12)), date(2014, 10, 15));
  }

  //-------------------------------------------------------------------------
  public void test_equals() {
    OvernightIndex a = OvernightIndex.builder()
        .currency(Currency.GBP)
        .name("OGIBOR")
        .calendar(GBLO)
        .publicationDateOffset(0)
        .effectiveDateOffset(0)
        .dayCount(ACT_360)
        .build();
    OvernightIndex b = a.toBuilder().currency(Currency.USD).build();
    assertEquals(a.equals(b), false);
    b = a.toBuilder().name("Rubbish").build();
    assertEquals(a.equals(b), false);
    b = a.toBuilder().calendar(USNY).build();
    assertEquals(a.equals(b), false);
    b = a.toBuilder().publicationDateOffset(1).build();
    assertEquals(a.equals(b), false);
    b = a.toBuilder().effectiveDateOffset(1).build();
    assertEquals(a.equals(b), false);
    b = a.toBuilder().dayCount(ACT_365F).build();
    assertEquals(a.equals(b), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    OvernightIndex index = OvernightIndex.builder()
        .currency(Currency.GBP)
        .name("OGONIA")
        .calendar(GBLO)
        .publicationDateOffset(0)
        .effectiveDateOffset(0)
        .dayCount(ACT_360)
        .build();
    coverImmutableBean(index);
    coverPrivateConstructor(StandardOvernightIndices.class);
  }

  public void test_jodaConvert() {
    assertJodaConvert(RateIndex.class, RateIndices.GBP_SONIA);
  }

  public void test_serialization() {
    OvernightIndex index = OvernightIndex.builder()
        .currency(Currency.GBP)
        .name("OGONIA")
        .calendar(GBLO)
        .publicationDateOffset(0)
        .effectiveDateOffset(0)
        .dayCount(ACT_360)
        .build();
    assertSerialization(index);
  }

}
