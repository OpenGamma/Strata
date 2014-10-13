/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.index;

import static com.opengamma.basics.currency.Currency.EUR;
import static com.opengamma.basics.currency.Currency.GBP;
import static com.opengamma.basics.currency.Currency.USD;
import static com.opengamma.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.basics.date.DayCounts.ACT_360;
import static com.opengamma.basics.date.DayCounts.ACT_365F;
import static com.opengamma.basics.date.HolidayCalendars.EUTA;
import static com.opengamma.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.basics.date.HolidayCalendars.USNY;
import static com.opengamma.basics.date.Tenor.TENOR_3M;
import static com.opengamma.basics.date.Tenor.TENOR_6M;
import static com.opengamma.collect.TestHelper.assertJodaConvert;
import static com.opengamma.collect.TestHelper.assertSerialization;
import static com.opengamma.collect.TestHelper.assertThrows;
import static com.opengamma.collect.TestHelper.coverImmutableBean;
import static com.opengamma.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.basics.currency.Currency;
import com.opengamma.basics.date.BusinessDayAdjustment;
import com.opengamma.basics.date.DaysAdjustment;
import com.opengamma.basics.date.TenorAdjustment;

/**
 * Test Ibor Index.
 */
@Test
public class IborIndexTest {

  public void test_null() {
    IborIndex test = IborIndex.of("GBP-LIBOR-3M");
    assertThrows(() -> test.calculatePublicationFromFixing(null), IllegalArgumentException.class);
    assertThrows(() -> test.calculateEffectiveFromFixing(null), IllegalArgumentException.class);
    assertThrows(() -> test.calculateFixingFromEffective(null), IllegalArgumentException.class);
    assertThrows(() -> test.calculateMaturityFromEffective(null), IllegalArgumentException.class);
  }

  public void test_gbpLibor3m() {
    IborIndex test = IborIndex.of("GBP-LIBOR-3M");
    assertEquals(test.getType(), RateIndexType.TENOR);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getName(), "GBP-LIBOR-3M");
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getFixingCalendar(), GBLO);
    assertEquals(test.getEffectiveDateOffset(), DaysAdjustment.NONE);
    assertEquals(test.getMaturityDateOffset(),
        TenorAdjustment.ofLastBusinessDay(TENOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO)));
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.toString(), "GBP-LIBOR-3M");
  }

  public void test_gbpLibor3m_dates() {
    IborIndex test = IborIndex.of("GBP-LIBOR-3M");
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 13)), date(2014, 10, 13));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 13)), date(2014, 10, 13));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 13)), date(2014, 10, 13));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 13)), date(2015, 1, 13));
    // weekend
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 10)), date(2014, 10, 10));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 10)), date(2014, 10, 10));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 10)), date(2014, 10, 10));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 10)), date(2015, 1, 12));
    // input date is Sunday
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 12)), date(2014, 10, 13));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 12)), date(2014, 10, 13));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 12)), date(2014, 10, 13));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 12)), date(2015, 1, 13));
  }

  public void test_usdLibor3m() {
    IborIndex test = IborIndex.of("USD-LIBOR-3M");
    assertEquals(test.getType(), RateIndexType.TENOR);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getName(), "USD-LIBOR-3M");
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getFixingCalendar(), GBLO);
    assertEquals(test.getEffectiveDateOffset(),
        DaysAdjustment.ofBusinessDays(2, GBLO, BusinessDayAdjustment.of(FOLLOWING, GBLO.combineWith(USNY))));
    assertEquals(test.getMaturityDateOffset(),
        TenorAdjustment.ofLastBusinessDay(TENOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO.combineWith(USNY))));
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.toString(), "USD-LIBOR-3M");
  }

  public void test_usdLibor3m_dates() {
    IborIndex test = IborIndex.of("USD-LIBOR-3M");
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 27)), date(2014, 10, 27));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 27)), date(2014, 10, 29));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 29)), date(2014, 10, 27));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 29)), date(2015, 1, 29));
    // weekend
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 10)), date(2014, 10, 10));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 10)), date(2014, 10, 14));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 14)), date(2014, 10, 10));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 14)), date(2015, 1, 14));
    // effective date is US holiday
    assertEquals(test.calculatePublicationFromFixing(date(2015, 1, 16)), date(2015, 1, 16));
    assertEquals(test.calculateEffectiveFromFixing(date(2015, 1, 16)), date(2015, 1, 20));
    assertEquals(test.calculateFixingFromEffective(date(2015, 1, 20)), date(2015, 1, 16));
    assertEquals(test.calculateMaturityFromEffective(date(2015, 1, 20)), date(2015, 4, 20));
    // input date is Sunday, 13th is US holiday, but not UK holiday (can fix, but not be effective)
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 12)), date(2014, 10, 13));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 12)), date(2014, 10, 15));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 12)), date(2014, 10, 10));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 12)), date(2015, 1, 14));
  }

  public void test_euibor3m() {
    IborIndex test = IborIndex.of("EURIBOR-3M");
    assertEquals(test.getType(), RateIndexType.TENOR);
    assertEquals(test.getCurrency(), EUR);
    assertEquals(test.getName(), "EURIBOR-3M");
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getFixingCalendar(), EUTA);
    assertEquals(test.getEffectiveDateOffset(), DaysAdjustment.ofBusinessDays(2, EUTA));
    assertEquals(test.getMaturityDateOffset(),
        TenorAdjustment.ofLastBusinessDay(TENOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)));
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.toString(), "EURIBOR-3M");
  }

  public void test_euribor3m_dates() {
    IborIndex test = IborIndex.of("EURIBOR-3M");
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 27)), date(2014, 10, 27));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 27)), date(2014, 10, 29));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 29)), date(2014, 10, 27));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 29)), date(2015, 1, 29));
    // weekend
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 10)), date(2014, 10, 10));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 10)), date(2014, 10, 14));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 14)), date(2014, 10, 10));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 14)), date(2015, 1, 14));
    // input date is Sunday
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 12)), date(2014, 10, 13));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 12)), date(2014, 10, 15));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 12)), date(2014, 10, 9));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 12)), date(2015, 1, 13));
  }

  //-------------------------------------------------------------------------
  public void test_equals() {
    IborIndex a = IborIndex.builder()
        .name("OGIBOR")
        .currency(Currency.GBP)
        .fixingCalendar(GBLO)
        .effectiveDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .maturityDateOffset(TenorAdjustment.ofLastBusinessDay(TENOR_3M, BusinessDayAdjustment.NONE))
        .dayCount(ACT_360)
        .build();
    IborIndex b = a.toBuilder().currency(Currency.USD).build();
    assertEquals(a.equals(b), false);
    b = a.toBuilder().name("Rubbish").build();
    assertEquals(a.equals(b), false);
    b = a.toBuilder().fixingCalendar(USNY).build();
    assertEquals(a.equals(b), false);
    b = a.toBuilder().effectiveDateOffset(DaysAdjustment.ofBusinessDays(2, USNY)).build();
    assertEquals(a.equals(b), false);
    b = a.toBuilder().maturityDateOffset(TenorAdjustment.ofLastBusinessDay(TENOR_6M, BusinessDayAdjustment.NONE)).build();
    assertEquals(a.equals(b), false);
    b = a.toBuilder().dayCount(ACT_365F).build();
    assertEquals(a.equals(b), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborIndex index = IborIndex.builder()
        .currency(Currency.GBP)
        .name("OGIBOR")
        .fixingCalendar(GBLO)
        .effectiveDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .maturityDateOffset(TenorAdjustment.ofLastBusinessDay(TENOR_3M, BusinessDayAdjustment.NONE))
        .dayCount(ACT_360)
        .build();
    coverImmutableBean(index);
    coverPrivateConstructor(StandardIborIndices.class);
  }

  public void test_jodaConvert() {
    assertJodaConvert(RateIndex.class, RateIndices.GBP_LIBOR_12M);
  }

  public void test_serialization() {
    IborIndex index = IborIndex.builder()
        .currency(Currency.GBP)
        .name("OGIBOR")
        .fixingCalendar(GBLO)
        .effectiveDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .maturityDateOffset(TenorAdjustment.ofLastBusinessDay(TENOR_3M, BusinessDayAdjustment.NONE))
        .dayCount(ACT_360)
        .build();
    assertSerialization(index);
  }

}
