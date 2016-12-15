/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.basics.currency.Currency.DKK;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.HUF;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.MXN;
import static com.opengamma.strata.basics.currency.Currency.PLN;
import static com.opengamma.strata.basics.currency.Currency.SEK;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.currency.Currency.ZAR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.AUSY;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.DKCO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.HUBU;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.JPTO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.MXMC;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.PLWA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SEST;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.ZAJO;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_4M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_4W;
import static com.opengamma.strata.basics.date.Tenor.TENOR_5M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalTime;
import java.time.ZoneId;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.PeriodAdditionConventions;
import com.opengamma.strata.basics.date.TenorAdjustment;

/**
 * Test Ibor Index.
 */
@Test
public class IborIndexTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  public void test_gbpLibor3m() {
    IborIndex test = IborIndex.of("GBP-LIBOR-3M");
    assertEquals(test.getName(), "GBP-LIBOR-3M");
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.isActive(), true);
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getFixingCalendar(), GBLO);
    assertEquals(test.getFixingDateOffset(),
        DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(PRECEDING, GBLO)));
    assertEquals(test.getEffectiveDateOffset(),
        DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, GBLO)));
    assertEquals(test.getMaturityDateOffset(),
        TenorAdjustment.ofLastBusinessDay(TENOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO)));
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getFloatingRateName(), FloatingRateName.of("GBP-LIBOR"));
    assertEquals(test.toString(), "GBP-LIBOR-3M");
  }

  public void test_gbpLibor3m_dates() {
    IborIndex test = IborIndex.of("GBP-LIBOR-3M");
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 13), REF_DATA), date(2014, 10, 13));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 13), REF_DATA), date(2015, 1, 13));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 13), REF_DATA), date(2014, 10, 13));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 13), REF_DATA), date(2015, 1, 13));
    // weekend
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 10), REF_DATA), date(2014, 10, 10));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 10), REF_DATA), date(2015, 1, 12));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 10), REF_DATA), date(2014, 10, 10));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 10), REF_DATA), date(2015, 1, 12));
    // input date is Sunday
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 12), REF_DATA), date(2014, 10, 13));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 12), REF_DATA), date(2015, 1, 13));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 12), REF_DATA), date(2014, 10, 13));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 12), REF_DATA), date(2015, 1, 13));
    // fixing time and zone
    assertEquals(test.calculateFixingDateTime(date(2014, 10, 13)),
        date(2014, 10, 13).atTime(LocalTime.of(11, 0)).atZone(ZoneId.of("Europe/London")));
    // resolve
    assertEquals(test.resolve(REF_DATA).apply(date(2014, 10, 13)), IborIndexObservation.of(test, date(2014, 10, 13), REF_DATA));
  }

  public void test_getFloatingRateName() {
    for (IborIndex index : IborIndex.extendedEnum().lookupAll().values()) {
      String name = index.getName().substring(0, index.getName().lastIndexOf('-'));
      assertEquals(index.getFloatingRateName(), FloatingRateName.of(name));
    }
  }

  //-------------------------------------------------------------------------
  public void test_usdLibor3m() {
    IborIndex test = IborIndex.of("USD-LIBOR-3M");
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getName(), "USD-LIBOR-3M");
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getFixingCalendar(), GBLO);
    assertEquals(test.getFixingDateOffset(), DaysAdjustment.ofBusinessDays(-2, GBLO));
    assertEquals(test.getEffectiveDateOffset(),
        DaysAdjustment.ofBusinessDays(2, GBLO, BusinessDayAdjustment.of(FOLLOWING, GBLO.combinedWith(USNY))));
    assertEquals(test.getMaturityDateOffset(),
        TenorAdjustment.ofLastBusinessDay(TENOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO.combinedWith(USNY))));
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getFloatingRateName(), FloatingRateName.of("USD-LIBOR"));
    assertEquals(test.toString(), "USD-LIBOR-3M");
  }

  public void test_usdLibor3m_dates() {
    IborIndex test = IborIndex.of("USD-LIBOR-3M");
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 27), REF_DATA), date(2014, 10, 29));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 27), REF_DATA), date(2015, 1, 29));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 29), REF_DATA), date(2014, 10, 27));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 29), REF_DATA), date(2015, 1, 29));
    // weekend
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 10), REF_DATA), date(2014, 10, 14));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 10), REF_DATA), date(2015, 1, 14));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 14), REF_DATA), date(2014, 10, 10));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 14), REF_DATA), date(2015, 1, 14));
    // effective date is US holiday
    assertEquals(test.calculateEffectiveFromFixing(date(2015, 1, 16), REF_DATA), date(2015, 1, 20));
    assertEquals(test.calculateMaturityFromFixing(date(2015, 1, 16), REF_DATA), date(2015, 4, 20));
    assertEquals(test.calculateFixingFromEffective(date(2015, 1, 20), REF_DATA), date(2015, 1, 16));
    assertEquals(test.calculateMaturityFromEffective(date(2015, 1, 20), REF_DATA), date(2015, 4, 20));
    // input date is Sunday, 13th is US holiday, but not UK holiday (can fix, but not be effective)
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 12), REF_DATA), date(2014, 10, 15));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 12), REF_DATA), date(2015, 1, 15));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 12), REF_DATA), date(2014, 10, 10));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 12), REF_DATA), date(2015, 1, 14));
    // fixing time and zone
    assertEquals(test.calculateFixingDateTime(date(2014, 10, 13)),
        date(2014, 10, 13).atTime(LocalTime.of(11, 0)).atZone(ZoneId.of("Europe/London")));
    // resolve
    assertEquals(test.resolve(REF_DATA).apply(date(2014, 10, 27)), IborIndexObservation.of(test, date(2014, 10, 27), REF_DATA));
  }

  public void test_euribor3m() {
    IborIndex test = IborIndex.of("EUR-EURIBOR-3M");
    assertEquals(test.getCurrency(), EUR);
    assertEquals(test.getName(), "EUR-EURIBOR-3M");
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getFixingCalendar(), EUTA);
    assertEquals(test.getFixingDateOffset(), DaysAdjustment.ofBusinessDays(-2, EUTA));
    assertEquals(test.getEffectiveDateOffset(), DaysAdjustment.ofBusinessDays(2, EUTA));
    assertEquals(test.getMaturityDateOffset(),
        TenorAdjustment.ofLastBusinessDay(TENOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)));
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getFloatingRateName(), FloatingRateName.of("EUR-EURIBOR"));
    assertEquals(test.toString(), "EUR-EURIBOR-3M");
  }

  public void test_euribor3m_dates() {
    IborIndex test = IborIndex.of("EUR-EURIBOR-3M");
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 27), REF_DATA), date(2014, 10, 29));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 27), REF_DATA), date(2015, 1, 29));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 29), REF_DATA), date(2014, 10, 27));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 29), REF_DATA), date(2015, 1, 29));
    // weekend
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 10), REF_DATA), date(2014, 10, 14));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 10), REF_DATA), date(2015, 1, 14));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 14), REF_DATA), date(2014, 10, 10));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 14), REF_DATA), date(2015, 1, 14));
    // input date is Sunday
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 12), REF_DATA), date(2014, 10, 15));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 12), REF_DATA), date(2015, 1, 15));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 12), REF_DATA), date(2014, 10, 9));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 12), REF_DATA), date(2015, 1, 13));
    // fixing time and zone
    assertEquals(test.calculateFixingDateTime(date(2014, 10, 13)),
        date(2014, 10, 13).atTime(LocalTime.of(11, 0)).atZone(ZoneId.of("Europe/Brussels")));
  }

  public void test_tibor_japan3m() {
    IborIndex test = IborIndex.of("JPY-TIBOR-JAPAN-3M");
    assertEquals(test.getCurrency(), JPY);
    assertEquals(test.getName(), "JPY-TIBOR-JAPAN-3M");
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getFixingCalendar(), JPTO);
    assertEquals(test.getFixingDateOffset(), DaysAdjustment.ofBusinessDays(-2, JPTO));
    assertEquals(test.getEffectiveDateOffset(), DaysAdjustment.ofBusinessDays(2, JPTO));
    assertEquals(test.getMaturityDateOffset(),
        TenorAdjustment.ofLastBusinessDay(TENOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, JPTO)));
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getFloatingRateName(), FloatingRateName.of("JPY-TIBOR-JAPAN"));
    assertEquals(test.toString(), "JPY-TIBOR-JAPAN-3M");
  }

  public void test_tibor_japan3m_dates() {
    IborIndex test = IborIndex.of("JPY-TIBOR-JAPAN-3M");
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 27), REF_DATA), date(2014, 10, 29));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 27), REF_DATA), date(2015, 1, 29));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 29), REF_DATA), date(2014, 10, 27));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 29), REF_DATA), date(2015, 1, 29));
    // weekend
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 10), REF_DATA), date(2014, 10, 15));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 10), REF_DATA), date(2015, 1, 15));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 15), REF_DATA), date(2014, 10, 10));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 15), REF_DATA), date(2015, 1, 15));
    // input date is Sunday
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 12), REF_DATA), date(2014, 10, 16));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 12), REF_DATA), date(2015, 1, 16));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 12), REF_DATA), date(2014, 10, 9));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 12), REF_DATA), date(2015, 1, 14));
    // fixing time and zone
    assertEquals(test.calculateFixingDateTime(date(2014, 10, 13)),
        date(2014, 10, 13).atTime(LocalTime.of(11, 50)).atZone(ZoneId.of("Asia/Tokyo")));
  }

  public void test_tibor_euroyen3m() {
    IborIndex test = IborIndex.of("JPY-TIBOR-EUROYEN-3M");
    assertEquals(test.getCurrency(), JPY);
    assertEquals(test.getName(), "JPY-TIBOR-EUROYEN-3M");
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getFixingCalendar(), JPTO);
    assertEquals(test.getFixingDateOffset(), DaysAdjustment.ofBusinessDays(-2, JPTO));
    assertEquals(test.getEffectiveDateOffset(), DaysAdjustment.ofBusinessDays(2, JPTO));
    assertEquals(test.getMaturityDateOffset(),
        TenorAdjustment.ofLastBusinessDay(TENOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, JPTO)));
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getFloatingRateName(), FloatingRateName.of("JPY-TIBOR-EUROYEN"));
    assertEquals(test.toString(), "JPY-TIBOR-EUROYEN-3M");
  }

  public void test_tibor_euroyen3m_dates() {
    IborIndex test = IborIndex.of("JPY-TIBOR-EUROYEN-3M");
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 27), REF_DATA), date(2014, 10, 29));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 27), REF_DATA), date(2015, 1, 29));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 29), REF_DATA), date(2014, 10, 27));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 29), REF_DATA), date(2015, 1, 29));
    // weekend
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 10), REF_DATA), date(2014, 10, 15));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 10), REF_DATA), date(2015, 1, 15));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 15), REF_DATA), date(2014, 10, 10));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 15), REF_DATA), date(2015, 1, 15));
    // input date is Sunday
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 12), REF_DATA), date(2014, 10, 16));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 12), REF_DATA), date(2015, 1, 16));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 12), REF_DATA), date(2014, 10, 9));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 12), REF_DATA), date(2015, 1, 14));
    // fixing time and zone
    assertEquals(test.calculateFixingDateTime(date(2014, 10, 13)),
        date(2014, 10, 13).atTime(LocalTime.of(11, 50)).atZone(ZoneId.of("Asia/Tokyo")));
  }

  public void test_usdLibor_all() {
    assertEquals(IborIndex.of("USD-LIBOR-1W").getName(), "USD-LIBOR-1W");
    assertEquals(IborIndex.of("USD-LIBOR-1W"), IborIndices.USD_LIBOR_1W);
    assertEquals(IborIndex.of("USD-LIBOR-1M").getName(), "USD-LIBOR-1M");
    assertEquals(IborIndex.of("USD-LIBOR-1M"), IborIndices.USD_LIBOR_1M);
    assertEquals(IborIndex.of("USD-LIBOR-2M").getName(), "USD-LIBOR-2M");
    assertEquals(IborIndex.of("USD-LIBOR-2M"), IborIndices.USD_LIBOR_2M);
    assertEquals(IborIndex.of("USD-LIBOR-3M").getName(), "USD-LIBOR-3M");
    assertEquals(IborIndex.of("USD-LIBOR-3M"), IborIndices.USD_LIBOR_3M);
    assertEquals(IborIndex.of("USD-LIBOR-6M").getName(), "USD-LIBOR-6M");
    assertEquals(IborIndex.of("USD-LIBOR-6M"), IborIndices.USD_LIBOR_6M);
    assertEquals(IborIndex.of("USD-LIBOR-12M").getName(), "USD-LIBOR-12M");
    assertEquals(IborIndex.of("USD-LIBOR-12M"), IborIndices.USD_LIBOR_12M);
  }

  public void test_bbsw1m() {
    IborIndex test = IborIndex.of("AUD-BBSW-1M");
    assertEquals(test.getCurrency(), AUD);
    assertEquals(test.getName(), "AUD-BBSW-1M");
    assertEquals(test.getTenor(), TENOR_1M);
    assertEquals(test.getFixingCalendar(), AUSY);
    assertEquals(test.getFixingDateOffset(), DaysAdjustment.ofBusinessDays(-1, AUSY));
    assertEquals(test.getEffectiveDateOffset(), DaysAdjustment.ofBusinessDays(1, AUSY));
    assertEquals(test.getMaturityDateOffset(),
        TenorAdjustment.of(TENOR_1M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(FOLLOWING, AUSY)));
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.toString(), "AUD-BBSW-1M");
  }

  public void test_bbsw2m() {
    IborIndex test = IborIndex.of("AUD-BBSW-2M");
    assertEquals(test.getCurrency(), AUD);
    assertEquals(test.getName(), "AUD-BBSW-2M");
    assertEquals(test.getTenor(), TENOR_2M);
    assertEquals(test.getFixingCalendar(), AUSY);
    assertEquals(test.getFixingDateOffset(), DaysAdjustment.ofBusinessDays(-1, AUSY));
    assertEquals(test.getEffectiveDateOffset(), DaysAdjustment.ofBusinessDays(1, AUSY));
    assertEquals(test.getMaturityDateOffset(),
        TenorAdjustment.of(TENOR_2M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(FOLLOWING, AUSY)));
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.toString(), "AUD-BBSW-2M");
  }

  public void test_bbsw3m() {
    IborIndex test = IborIndex.of("AUD-BBSW-3M");
    assertEquals(test.getCurrency(), AUD);
    assertEquals(test.getName(), "AUD-BBSW-3M");
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getFixingCalendar(), AUSY);
    assertEquals(test.getFixingDateOffset(), DaysAdjustment.ofBusinessDays(-1, AUSY));
    assertEquals(test.getEffectiveDateOffset(), DaysAdjustment.ofBusinessDays(1, AUSY));
    assertEquals(test.getMaturityDateOffset(),
        TenorAdjustment.of(TENOR_3M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(FOLLOWING, AUSY)));
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.toString(), "AUD-BBSW-3M");
  }

  public void test_bbsw4m() {
    IborIndex test = IborIndex.of("AUD-BBSW-4M");
    assertEquals(test.getCurrency(), AUD);
    assertEquals(test.getName(), "AUD-BBSW-4M");
    assertEquals(test.getTenor(), TENOR_4M);
    assertEquals(test.getFixingCalendar(), AUSY);
    assertEquals(test.getFixingDateOffset(), DaysAdjustment.ofBusinessDays(-1, AUSY));
    assertEquals(test.getEffectiveDateOffset(), DaysAdjustment.ofBusinessDays(1, AUSY));
    assertEquals(test.getMaturityDateOffset(),
        TenorAdjustment.of(TENOR_4M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(FOLLOWING, AUSY)));
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.toString(), "AUD-BBSW-4M");
  }

  public void test_bbsw5m() {
    IborIndex test = IborIndex.of("AUD-BBSW-5M");
    assertEquals(test.getCurrency(), AUD);
    assertEquals(test.getName(), "AUD-BBSW-5M");
    assertEquals(test.getTenor(), TENOR_5M);
    assertEquals(test.getFixingCalendar(), AUSY);
    assertEquals(test.getFixingDateOffset(), DaysAdjustment.ofBusinessDays(-1, AUSY));
    assertEquals(test.getEffectiveDateOffset(), DaysAdjustment.ofBusinessDays(1, AUSY));
    assertEquals(test.getMaturityDateOffset(),
        TenorAdjustment.of(TENOR_5M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(FOLLOWING, AUSY)));
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.toString(), "AUD-BBSW-5M");
  }

  public void test_bbsw6m() {
    IborIndex test = IborIndex.of("AUD-BBSW-6M");
    assertEquals(test.getCurrency(), AUD);
    assertEquals(test.getName(), "AUD-BBSW-6M");
    assertEquals(test.getTenor(), TENOR_6M);
    assertEquals(test.getFixingCalendar(), AUSY);
    assertEquals(test.getFixingDateOffset(), DaysAdjustment.ofBusinessDays(-1, AUSY));
    assertEquals(test.getEffectiveDateOffset(), DaysAdjustment.ofBusinessDays(1, AUSY));
    assertEquals(test.getMaturityDateOffset(),
        TenorAdjustment.of(TENOR_6M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(FOLLOWING, AUSY)));
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.toString(), "AUD-BBSW-6M");
  }

  public void test_dkk_cibor() {
    IborIndex test = IborIndex.of("DKK-CIBOR-3M");
    assertEquals(test.getCurrency(), DKK);
    assertEquals(test.getName(), "DKK-CIBOR-3M");
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getFixingCalendar(), DKCO);
    assertEquals(test.getFixingDateOffset(), DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(PRECEDING, DKCO)));
    assertEquals(test.getEffectiveDateOffset(), DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, DKCO)));
    assertEquals(test.getMaturityDateOffset(),
        TenorAdjustment.of(TENOR_3M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(FOLLOWING, DKCO)));
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.toString(), "DKK-CIBOR-3M");
  }

  public void test_huf_bubor() {
    IborIndex test = IborIndex.of("HUF-BUBOR-3M");
    assertEquals(test.getCurrency(), HUF);
    assertEquals(test.getName(), "HUF-BUBOR-3M");
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getFixingCalendar(), HUBU);
    assertEquals(test.getFixingDateOffset(), DaysAdjustment.ofBusinessDays(-2, HUBU));
    assertEquals(test.getEffectiveDateOffset(), DaysAdjustment.ofBusinessDays(2, HUBU));
    assertEquals(test.getMaturityDateOffset(),
        TenorAdjustment.of(TENOR_3M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(FOLLOWING, HUBU)));
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.toString(), "HUF-BUBOR-3M");
  }

  public void test_mxn_tiie() {
    IborIndex test = IborIndex.of("MXN-TIIE-4W");
    assertEquals(test.getCurrency(), MXN);
    assertEquals(test.getName(), "MXN-TIIE-4W");
    assertEquals(test.getTenor(), TENOR_4W);
    assertEquals(test.getFixingCalendar(), MXMC);
    assertEquals(test.getFixingDateOffset(), DaysAdjustment.ofBusinessDays(-1, MXMC));
    assertEquals(test.getEffectiveDateOffset(), DaysAdjustment.ofBusinessDays(1, MXMC));
    assertEquals(test.getMaturityDateOffset(),
        TenorAdjustment.of(TENOR_4W, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(FOLLOWING, MXMC)));
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.toString(), "MXN-TIIE-4W");
  }

  public void test_pln_wibor() {
    IborIndex test = IborIndex.of("PLN-WIBOR-3M");
    assertEquals(test.getCurrency(), PLN);
    assertEquals(test.getName(), "PLN-WIBOR-3M");
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getFixingCalendar(), PLWA);
    assertEquals(test.getFixingDateOffset(), DaysAdjustment.ofBusinessDays(-2, PLWA));
    assertEquals(test.getEffectiveDateOffset(), DaysAdjustment.ofBusinessDays(2, PLWA));
    assertEquals(test.getMaturityDateOffset(),
        TenorAdjustment.of(TENOR_3M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(FOLLOWING, PLWA)));
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.toString(), "PLN-WIBOR-3M");
  }

  public void test_sek_stibor() {
    IborIndex test = IborIndex.of("SEK-STIBOR-3M");
    assertEquals(test.getCurrency(), SEK);
    assertEquals(test.getName(), "SEK-STIBOR-3M");
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getFixingCalendar(), SEST);
    assertEquals(test.getFixingDateOffset(), DaysAdjustment.ofBusinessDays(-2, SEST));
    assertEquals(test.getEffectiveDateOffset(), DaysAdjustment.ofBusinessDays(2, SEST));
    assertEquals(test.getMaturityDateOffset(),
        TenorAdjustment.of(TENOR_3M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(FOLLOWING, SEST)));
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.toString(), "SEK-STIBOR-3M");
  }

  public void test_zar_jibar() {
    IborIndex test = IborIndex.of("ZAR-JIBAR-3M");
    assertEquals(test.getCurrency(), ZAR);
    assertEquals(test.getName(), "ZAR-JIBAR-3M");
    assertEquals(test.getTenor(), TENOR_3M);
    assertEquals(test.getFixingCalendar(), ZAJO);
    assertEquals(test.getFixingDateOffset(),
        DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(PRECEDING, ZAJO)));
    assertEquals(test.getEffectiveDateOffset(),
        DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, ZAJO)));
    assertEquals(test.getMaturityDateOffset(),
        TenorAdjustment.of(TENOR_3M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(FOLLOWING, ZAJO)));
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.toString(), "ZAR-JIBAR-3M");
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {IborIndices.GBP_LIBOR_6M, "GBP-LIBOR-6M"},
        {IborIndices.CHF_LIBOR_6M, "CHF-LIBOR-6M"},
        {IborIndices.EUR_LIBOR_6M, "EUR-LIBOR-6M"},
        {IborIndices.JPY_LIBOR_6M, "JPY-LIBOR-6M"},
        {IborIndices.USD_LIBOR_6M, "USD-LIBOR-6M"},
        {IborIndices.EUR_EURIBOR_1M, "EUR-EURIBOR-1M"},
        {IborIndices.JPY_TIBOR_JAPAN_2M, "JPY-TIBOR-JAPAN-2M"},
        {IborIndices.JPY_TIBOR_EUROYEN_6M, "JPY-TIBOR-EUROYEN-6M"},
        {IborIndices.AUD_BBSW_1M, "AUD-BBSW-1M"},
        {IborIndices.AUD_BBSW_2M, "AUD-BBSW-2M"},
        {IborIndices.AUD_BBSW_3M, "AUD-BBSW-3M"},
        {IborIndices.AUD_BBSW_4M, "AUD-BBSW-4M"},
        {IborIndices.AUD_BBSW_5M, "AUD-BBSW-5M"},
        {IborIndices.AUD_BBSW_6M, "AUD-BBSW-6M"},
    };
  }

  @Test(dataProvider = "name")
  public void test_name(IborIndex convention, String name) {
    assertEquals(convention.getName(), name);
  }

  @Test(dataProvider = "name")
  public void test_toString(IborIndex convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(IborIndex convention, String name) {
    assertEquals(IborIndex.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_extendedEnum(IborIndex convention, String name) {
    ImmutableMap<String, IborIndex> map = IborIndex.extendedEnum().lookupAll();
    assertEquals(map.get(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> IborIndex.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> IborIndex.of(null));
  }

  //-------------------------------------------------------------------------
  public void test_equals() {
    ImmutableIborIndex a = ImmutableIborIndex.builder()
        .name("Test-3M")
        .currency(Currency.GBP)
        .fixingCalendar(GBLO)
        .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
        .effectiveDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .maturityDateOffset(TenorAdjustment.ofLastBusinessDay(TENOR_3M, BusinessDayAdjustment.NONE))
        .dayCount(ACT_360)
        .fixingTime(LocalTime.NOON)
        .fixingZone(ZoneId.of("Europe/London"))
        .build();
    IborIndex b = a.toBuilder().name("Rubbish-3M").build();
    assertEquals(a.equals(b), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableIborIndex index = ImmutableIborIndex.builder()
        .name("Test-3M")
        .currency(Currency.GBP)
        .fixingCalendar(GBLO)
        .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
        .effectiveDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .maturityDateOffset(TenorAdjustment.ofLastBusinessDay(TENOR_3M, BusinessDayAdjustment.NONE))
        .dayCount(ACT_360)
        .fixingTime(LocalTime.NOON)
        .fixingZone(ZoneId.of("Europe/London"))
        .build();
    coverImmutableBean(index);
    coverPrivateConstructor(IborIndices.class);
  }

  public void test_jodaConvert() {
    assertJodaConvert(IborIndex.class, IborIndices.GBP_LIBOR_12M);
  }

  public void test_serialization() {
    IborIndex index = ImmutableIborIndex.builder()
        .name("Test-3M")
        .currency(Currency.GBP)
        .fixingCalendar(GBLO)
        .fixingDateOffset(DaysAdjustment.ofBusinessDays(-2, GBLO))
        .effectiveDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .maturityDateOffset(TenorAdjustment.ofLastBusinessDay(TENOR_3M, BusinessDayAdjustment.NONE))
        .dayCount(ACT_360)
        .fixingTime(LocalTime.NOON)
        .fixingZone(ZoneId.of("Europe/London"))
        .build();
    assertSerialization(index);
  }

}
