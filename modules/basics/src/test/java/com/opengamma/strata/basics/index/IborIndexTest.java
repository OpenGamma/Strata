/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.basics.currency.Currency.CZK;
import static com.opengamma.strata.basics.currency.Currency.DKK;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.HKD;
import static com.opengamma.strata.basics.currency.Currency.HUF;
import static com.opengamma.strata.basics.currency.Currency.JPY;
import static com.opengamma.strata.basics.currency.Currency.KRW;
import static com.opengamma.strata.basics.currency.Currency.MXN;
import static com.opengamma.strata.basics.currency.Currency.NZD;
import static com.opengamma.strata.basics.currency.Currency.PLN;
import static com.opengamma.strata.basics.currency.Currency.SEK;
import static com.opengamma.strata.basics.currency.Currency.SGD;
import static com.opengamma.strata.basics.currency.Currency.THB;
import static com.opengamma.strata.basics.currency.Currency.TWD;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.currency.Currency.ZAR;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING_BI_MONTHLY;
import static com.opengamma.strata.basics.date.BusinessDayConventions.PRECEDING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.date.DayCounts.THIRTY_U_360;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.AUSY;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.CZPR;
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
import static com.opengamma.strata.basics.date.Tenor.TENOR_13W;
import static com.opengamma.strata.basics.date.Tenor.TENOR_1M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_2M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_3M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_4M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_4W;
import static com.opengamma.strata.basics.date.Tenor.TENOR_5M;
import static com.opengamma.strata.basics.date.Tenor.TENOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;
import com.opengamma.strata.basics.date.PeriodAdditionConventions;
import com.opengamma.strata.basics.date.TenorAdjustment;

/**
 * Test Ibor Index.
 */
public class IborIndexTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final HolidayCalendarId NZBD = HolidayCalendarId.of("NZBD");  // no constant for this

  @Test
  public void test_gbpLibor3m() {
    IborIndex test = IborIndex.of("GBP-LIBOR-3M");
    assertThat(test.getName()).isEqualTo("GBP-LIBOR-3M");
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.isActive()).isEqualTo(true);
    assertThat(test.getTenor()).isEqualTo(TENOR_3M);
    assertThat(test.getFixingCalendar()).isEqualTo(GBLO);
    assertThat(test.getFixingDateOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(PRECEDING, GBLO)));
    assertThat(test.getEffectiveDateOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, GBLO)));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.ofLastBusinessDay(TENOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO)));
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getFloatingRateName()).isEqualTo(FloatingRateName.of("GBP-LIBOR"));
    assertThat(test.toString()).isEqualTo("GBP-LIBOR-3M");
  }

  @Test
  public void test_gbpLibor3m_dates() {
    IborIndex test = IborIndex.of("GBP-LIBOR-3M");
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 13), REF_DATA)).isEqualTo(date(2014, 10, 13));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 13), REF_DATA)).isEqualTo(date(2015, 1, 13));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 13), REF_DATA)).isEqualTo(date(2014, 10, 13));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 13), REF_DATA)).isEqualTo(date(2015, 1, 13));
    // weekend
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2014, 10, 10));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2015, 1, 12));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2014, 10, 10));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2015, 1, 12));
    // input date is Sunday
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2014, 10, 13));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2015, 1, 13));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2014, 10, 13));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2015, 1, 13));
    // fixing time and zone
    assertThat(test.calculateFixingDateTime(date(2014, 10, 13)))
        .isEqualTo(date(2014, 10, 13).atTime(LocalTime.of(11, 55)).atZone(ZoneId.of("Europe/London")));
    // resolve
    assertThat(test.resolve(REF_DATA).apply(date(2014, 10, 13)))
        .isEqualTo(IborIndexObservation.of(test, date(2014, 10, 13), REF_DATA));
  }

  @Test
  public void test_getFloatingRateName() {
    for (IborIndex index : IborIndex.extendedEnum().lookupAll().values()) {
      String name = index.getName().substring(0, index.getName().lastIndexOf('-'));
      assertThat(index.getFloatingRateName()).isEqualTo(FloatingRateName.of(name));
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_usdLibor3m() {
    IborIndex test = IborIndex.of("USD-LIBOR-3M");
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getName()).isEqualTo("USD-LIBOR-3M");
    assertThat(test.getTenor()).isEqualTo(TENOR_3M);
    assertThat(test.getFixingCalendar()).isEqualTo(GBLO);
    assertThat(test.getFixingDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(-2, GBLO));
    assertThat(test.getEffectiveDateOffset())
        .isEqualTo(DaysAdjustment.ofBusinessDays(2, GBLO, BusinessDayAdjustment.of(FOLLOWING, GBLO.combinedWith(USNY))));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.ofLastBusinessDay(
            TENOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO.combinedWith(USNY))));
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_360);
    assertThat(test.getFloatingRateName()).isEqualTo(FloatingRateName.of("USD-LIBOR"));
    assertThat(test.toString()).isEqualTo("USD-LIBOR-3M");
  }

  @Test
  public void test_usdLibor3m_dates() {
    IborIndex test = IborIndex.of("USD-LIBOR-3M");
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 27), REF_DATA)).isEqualTo(date(2014, 10, 29));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 27), REF_DATA)).isEqualTo(date(2015, 1, 29));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 29), REF_DATA)).isEqualTo(date(2014, 10, 27));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 29), REF_DATA)).isEqualTo(date(2015, 1, 29));
    // weekend
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2014, 10, 14));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2015, 1, 14));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 14), REF_DATA)).isEqualTo(date(2014, 10, 10));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 14), REF_DATA)).isEqualTo(date(2015, 1, 14));
    // effective date is US holiday
    assertThat(test.calculateEffectiveFromFixing(date(2015, 1, 16), REF_DATA)).isEqualTo(date(2015, 1, 20));
    assertThat(test.calculateMaturityFromFixing(date(2015, 1, 16), REF_DATA)).isEqualTo(date(2015, 4, 20));
    assertThat(test.calculateFixingFromEffective(date(2015, 1, 20), REF_DATA)).isEqualTo(date(2015, 1, 16));
    assertThat(test.calculateMaturityFromEffective(date(2015, 1, 20), REF_DATA)).isEqualTo(date(2015, 4, 20));
    // input date is Sunday, 13th is US holiday, but not UK holiday (can fix, but not be effective)
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2014, 10, 15));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2015, 1, 15));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2014, 10, 10));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2015, 1, 14));
    // fixing time and zone
    assertThat(test.calculateFixingDateTime(date(2014, 10, 13)))
        .isEqualTo(date(2014, 10, 13).atTime(LocalTime.of(11, 55)).atZone(ZoneId.of("Europe/London")));
    // resolve
    assertThat(test.resolve(REF_DATA).apply(date(2014, 10, 27)))
        .isEqualTo(IborIndexObservation.of(test, date(2014, 10, 27), REF_DATA));
  }

  @Test
  public void test_euribor3m() {
    IborIndex test = IborIndex.of("EUR-EURIBOR-3M");
    assertThat(test.getCurrency()).isEqualTo(EUR);
    assertThat(test.getName()).isEqualTo("EUR-EURIBOR-3M");
    assertThat(test.getTenor()).isEqualTo(TENOR_3M);
    assertThat(test.getFixingCalendar()).isEqualTo(EUTA);
    assertThat(test.getFixingDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(-2, EUTA));
    assertThat(test.getEffectiveDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(2, EUTA));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.ofLastBusinessDay(TENOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, EUTA)));
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(THIRTY_U_360);
    assertThat(test.getFloatingRateName()).isEqualTo(FloatingRateName.of("EUR-EURIBOR"));
    assertThat(test.toString()).isEqualTo("EUR-EURIBOR-3M");
  }

  @Test
  public void test_euribor3m_dates() {
    IborIndex test = IborIndex.of("EUR-EURIBOR-3M");
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 27), REF_DATA)).isEqualTo(date(2014, 10, 29));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 27), REF_DATA)).isEqualTo(date(2015, 1, 29));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 29), REF_DATA)).isEqualTo(date(2014, 10, 27));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 29), REF_DATA)).isEqualTo(date(2015, 1, 29));
    // weekend
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2014, 10, 14));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2015, 1, 14));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 14), REF_DATA)).isEqualTo(date(2014, 10, 10));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 14), REF_DATA)).isEqualTo(date(2015, 1, 14));
    // input date is Sunday
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2014, 10, 15));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2015, 1, 15));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2014, 10, 9));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2015, 1, 13));
    // fixing time and zone
    assertThat(test.calculateFixingDateTime(date(2014, 10, 13)))
        .isEqualTo(date(2014, 10, 13).atTime(LocalTime.of(11, 0)).atZone(ZoneId.of("Europe/Brussels")));
  }

  @Test
  public void test_tibor_japan3m() {
    IborIndex test = IborIndex.of("JPY-TIBOR-JAPAN-3M");
    assertThat(test.getCurrency()).isEqualTo(JPY);
    assertThat(test.getName()).isEqualTo("JPY-TIBOR-JAPAN-3M");
    assertThat(test.getTenor()).isEqualTo(TENOR_3M);
    assertThat(test.getFixingCalendar()).isEqualTo(JPTO);
    assertThat(test.getFixingDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(-2, JPTO));
    assertThat(test.getEffectiveDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(2, JPTO));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.ofLastBusinessDay(TENOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, JPTO)));
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getFloatingRateName()).isEqualTo(FloatingRateName.of("JPY-TIBOR-JAPAN"));
    assertThat(test.toString()).isEqualTo("JPY-TIBOR-JAPAN-3M");
  }

  @Test
  public void test_tibor_japan3m_dates() {
    IborIndex test = IborIndex.of("JPY-TIBOR-JAPAN-3M");
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 27), REF_DATA)).isEqualTo(date(2014, 10, 29));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 27), REF_DATA)).isEqualTo(date(2015, 1, 29));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 29), REF_DATA)).isEqualTo(date(2014, 10, 27));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 29), REF_DATA)).isEqualTo(date(2015, 1, 29));
    // weekend
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2014, 10, 15));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2015, 1, 15));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 15), REF_DATA)).isEqualTo(date(2014, 10, 10));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 15), REF_DATA)).isEqualTo(date(2015, 1, 15));
    // input date is Sunday
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2014, 10, 16));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2015, 1, 16));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2014, 10, 9));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2015, 1, 14));
    // fixing time and zone
    assertThat(test.calculateFixingDateTime(date(2014, 10, 13)))
        .isEqualTo(date(2014, 10, 13).atTime(LocalTime.of(13, 0)).atZone(ZoneId.of("Asia/Tokyo")));
  }

  @Test
  public void test_tibor_euroyen3m() {
    IborIndex test = IborIndex.of("JPY-TIBOR-EUROYEN-3M");
    assertThat(test.getCurrency()).isEqualTo(JPY);
    assertThat(test.getName()).isEqualTo("JPY-TIBOR-EUROYEN-3M");
    assertThat(test.getTenor()).isEqualTo(TENOR_3M);
    assertThat(test.getFixingCalendar()).isEqualTo(JPTO);
    assertThat(test.getFixingDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(-2, JPTO));
    assertThat(test.getEffectiveDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(2, JPTO));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.ofLastBusinessDay(TENOR_3M, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, JPTO)));
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getFloatingRateName()).isEqualTo(FloatingRateName.of("JPY-TIBOR-EUROYEN"));
    assertThat(test.toString()).isEqualTo("JPY-TIBOR-EUROYEN-3M");
  }

  @Test
  public void test_tibor_euroyen3m_dates() {
    IborIndex test = IborIndex.of("JPY-TIBOR-EUROYEN-3M");
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 27), REF_DATA)).isEqualTo(date(2014, 10, 29));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 27), REF_DATA)).isEqualTo(date(2015, 1, 29));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 29), REF_DATA)).isEqualTo(date(2014, 10, 27));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 29), REF_DATA)).isEqualTo(date(2015, 1, 29));
    // weekend
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2014, 10, 15));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2015, 1, 15));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 15), REF_DATA)).isEqualTo(date(2014, 10, 10));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 15), REF_DATA)).isEqualTo(date(2015, 1, 15));
    // input date is Sunday
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2014, 10, 16));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2015, 1, 16));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2014, 10, 9));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2015, 1, 14));
    // fixing time and zone
    assertThat(test.calculateFixingDateTime(date(2014, 10, 13)))
        .isEqualTo(date(2014, 10, 13).atTime(LocalTime.of(13, 0)).atZone(ZoneId.of("Asia/Tokyo")));
  }

  @Test
  public void test_usdLibor_all() {
    assertThat(IborIndex.of("USD-LIBOR-1W").getName()).isEqualTo("USD-LIBOR-1W");
    assertThat(IborIndex.of("USD-LIBOR-1W")).isEqualTo(IborIndices.USD_LIBOR_1W);
    assertThat(IborIndex.of("USD-LIBOR-1M").getName()).isEqualTo("USD-LIBOR-1M");
    assertThat(IborIndex.of("USD-LIBOR-1M")).isEqualTo(IborIndices.USD_LIBOR_1M);
    assertThat(IborIndex.of("USD-LIBOR-2M").getName()).isEqualTo("USD-LIBOR-2M");
    assertThat(IborIndex.of("USD-LIBOR-2M")).isEqualTo(IborIndices.USD_LIBOR_2M);
    assertThat(IborIndex.of("USD-LIBOR-3M").getName()).isEqualTo("USD-LIBOR-3M");
    assertThat(IborIndex.of("USD-LIBOR-3M")).isEqualTo(IborIndices.USD_LIBOR_3M);
    assertThat(IborIndex.of("USD-LIBOR-6M").getName()).isEqualTo("USD-LIBOR-6M");
    assertThat(IborIndex.of("USD-LIBOR-6M")).isEqualTo(IborIndices.USD_LIBOR_6M);
    assertThat(IborIndex.of("USD-LIBOR-12M").getName()).isEqualTo("USD-LIBOR-12M");
    assertThat(IborIndex.of("USD-LIBOR-12M")).isEqualTo(IborIndices.USD_LIBOR_12M);
  }

  @Test
  public void test_bbsw1m() {
    IborIndex test = IborIndex.of("AUD-BBSW-1M");
    assertThat(test.getCurrency()).isEqualTo(AUD);
    assertThat(test.getName()).isEqualTo("AUD-BBSW-1M");
    assertThat(test.getTenor()).isEqualTo(TENOR_1M);
    assertThat(test.getFixingCalendar()).isEqualTo(AUSY);
    assertThat(test.getFixingDateOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(PRECEDING, AUSY)));
    assertThat(test.getEffectiveDateOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, AUSY)));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.of(
            TENOR_1M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(MODIFIED_FOLLOWING_BI_MONTHLY, AUSY)));
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("AUD-BBSW-1M");
  }

  @Test
  public void test_bbsw2m() {
    IborIndex test = IborIndex.of("AUD-BBSW-2M");
    assertThat(test.getCurrency()).isEqualTo(AUD);
    assertThat(test.getName()).isEqualTo("AUD-BBSW-2M");
    assertThat(test.getTenor()).isEqualTo(TENOR_2M);
    assertThat(test.getFixingCalendar()).isEqualTo(AUSY);
    assertThat(test.getFixingDateOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(PRECEDING, AUSY)));
    assertThat(test.getEffectiveDateOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, AUSY)));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.of(
            TENOR_2M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(MODIFIED_FOLLOWING_BI_MONTHLY, AUSY)));
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("AUD-BBSW-2M");
  }

  @Test
  public void test_bbsw3m() {
    IborIndex test = IborIndex.of("AUD-BBSW-3M");
    assertThat(test.getCurrency()).isEqualTo(AUD);
    assertThat(test.getName()).isEqualTo("AUD-BBSW-3M");
    assertThat(test.getTenor()).isEqualTo(TENOR_3M);
    assertThat(test.getFixingCalendar()).isEqualTo(AUSY);
    assertThat(test.getFixingDateOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(PRECEDING, AUSY)));
    assertThat(test.getEffectiveDateOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, AUSY)));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.of(
            TENOR_3M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(MODIFIED_FOLLOWING_BI_MONTHLY, AUSY)));
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("AUD-BBSW-3M");
  }

  @Test
  public void test_bbsw4m() {
    IborIndex test = IborIndex.of("AUD-BBSW-4M");
    assertThat(test.getCurrency()).isEqualTo(AUD);
    assertThat(test.getName()).isEqualTo("AUD-BBSW-4M");
    assertThat(test.getTenor()).isEqualTo(TENOR_4M);
    assertThat(test.getFixingCalendar()).isEqualTo(AUSY);
    assertThat(test.getFixingDateOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(PRECEDING, AUSY)));
    assertThat(test.getEffectiveDateOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, AUSY)));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.of(
            TENOR_4M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(MODIFIED_FOLLOWING_BI_MONTHLY, AUSY)));
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("AUD-BBSW-4M");
  }

  @Test
  public void test_bbsw5m() {
    IborIndex test = IborIndex.of("AUD-BBSW-5M");
    assertThat(test.getCurrency()).isEqualTo(AUD);
    assertThat(test.getName()).isEqualTo("AUD-BBSW-5M");
    assertThat(test.getTenor()).isEqualTo(TENOR_5M);
    assertThat(test.getFixingCalendar()).isEqualTo(AUSY);
    assertThat(test.getFixingDateOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(PRECEDING, AUSY)));
    assertThat(test.getEffectiveDateOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, AUSY)));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.of(
            TENOR_5M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(MODIFIED_FOLLOWING_BI_MONTHLY, AUSY)));
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("AUD-BBSW-5M");
  }

  @Test
  public void test_bbsw6m() {
    IborIndex test = IborIndex.of("AUD-BBSW-6M");
    assertThat(test.getCurrency()).isEqualTo(AUD);
    assertThat(test.getName()).isEqualTo("AUD-BBSW-6M");
    assertThat(test.getTenor()).isEqualTo(TENOR_6M);
    assertThat(test.getFixingCalendar()).isEqualTo(AUSY);
    assertThat(test.getFixingDateOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(PRECEDING, AUSY)));
    assertThat(test.getEffectiveDateOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, AUSY)));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.of(
            TENOR_6M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(MODIFIED_FOLLOWING_BI_MONTHLY, AUSY)));
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("AUD-BBSW-6M");
  }

  @Test
  public void test_czk_pribor() {
    IborIndex test = IborIndex.of("CZK-PRIBOR-3M");
    assertThat(test.getCurrency()).isEqualTo(CZK);
    assertThat(test.getName()).isEqualTo("CZK-PRIBOR-3M");
    assertThat(test.getTenor()).isEqualTo(TENOR_3M);
    assertThat(test.getFixingCalendar()).isEqualTo(CZPR);
    assertThat(test.getFixingDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(-2, CZPR));
    assertThat(test.getEffectiveDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(2, CZPR));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.of(
            TENOR_3M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, CZPR)));
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_360);
    assertThat(test.toString()).isEqualTo("CZK-PRIBOR-3M");
  }

  @Test
  public void test_dkk_cibor() {
    IborIndex test = IborIndex.of("DKK-CIBOR-3M");
    assertThat(test.getCurrency()).isEqualTo(DKK);
    assertThat(test.getName()).isEqualTo("DKK-CIBOR-3M");
    assertThat(test.getTenor()).isEqualTo(TENOR_3M);
    assertThat(test.getFixingCalendar()).isEqualTo(DKCO);
    assertThat(test.getFixingDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(-2, DKCO));
    assertThat(test.getEffectiveDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(2, DKCO));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.of(
            TENOR_3M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, DKCO)));
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(THIRTY_U_360);
    assertThat(test.toString()).isEqualTo("DKK-CIBOR-3M");
  }

  @Test
  public void test_hkd_hibor() {
    HolidayCalendarId cal = HolidayCalendarId.of("HKHK");
    IborIndex test = IborIndex.of("HKD-HIBOR-3M");
    assertThat(test.getCurrency()).isEqualTo(HKD);
    assertThat(test.getName()).isEqualTo("HKD-HIBOR-3M");
    assertThat(test.getTenor()).isEqualTo(TENOR_3M);
    assertThat(test.getFixingCalendar()).isEqualTo(cal);
    assertThat(test.getFixingDateOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(PRECEDING, cal)));
    assertThat(test.getEffectiveDateOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, cal)));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.of(
            TENOR_3M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, cal)));
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("HKD-HIBOR-3M");
  }

  @Test
  public void test_huf_bubor() {
    IborIndex test = IborIndex.of("HUF-BUBOR-3M");
    assertThat(test.getCurrency()).isEqualTo(HUF);
    assertThat(test.getName()).isEqualTo("HUF-BUBOR-3M");
    assertThat(test.getTenor()).isEqualTo(TENOR_3M);
    assertThat(test.getFixingCalendar()).isEqualTo(HUBU);
    assertThat(test.getFixingDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(-2, HUBU));
    assertThat(test.getEffectiveDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(2, HUBU));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.of(
            TENOR_3M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, HUBU)));
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("HUF-BUBOR-3M");
  }

  @Test
  public void test_krw_cd() {
    HolidayCalendarId cal = HolidayCalendarId.of("KRSE");
    IborIndex test = IborIndex.of("KRW-CD-13W");
    assertThat(test.getCurrency()).isEqualTo(KRW);
    assertThat(test.getName()).isEqualTo("KRW-CD-13W");
    assertThat(test.getTenor()).isEqualTo(TENOR_13W);
    assertThat(test.getFixingCalendar()).isEqualTo(cal);
    assertThat(test.getFixingDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(-1, cal));
    assertThat(test.getEffectiveDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(1, cal));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.of(
            TENOR_13W, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(FOLLOWING, cal)));
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("KRW-CD-13W");

    IborIndex test2 = IborIndex.of("KRW-CD-3M");
    assertThat(test2.getName()).isEqualTo("KRW-CD-13W");
  }

  @Test
  public void test_mxn_tiie() {
    IborIndex test = IborIndex.of("MXN-TIIE-4W");
    assertThat(test.getCurrency()).isEqualTo(MXN);
    assertThat(test.getName()).isEqualTo("MXN-TIIE-4W");
    assertThat(test.getTenor()).isEqualTo(TENOR_4W);
    assertThat(test.getFixingCalendar()).isEqualTo(MXMC);
    assertThat(test.getFixingDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(-1, MXMC));
    assertThat(test.getEffectiveDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(1, MXMC));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.of(
            TENOR_4W, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(FOLLOWING, MXMC)));
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_360);
    assertThat(test.toString()).isEqualTo("MXN-TIIE-4W");
  }

  @Test
  public void test_nzd_bkbm() {
    IborIndex test = IborIndex.of("NZD-BKBM-3M");
    assertThat(test.getCurrency()).isEqualTo(NZD);
    assertThat(test.getName()).isEqualTo("NZD-BKBM-3M");
    assertThat(test.getTenor()).isEqualTo(TENOR_3M);
    assertThat(test.getFixingCalendar()).isEqualTo(NZBD);
    assertThat(test.getFixingDateOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(PRECEDING, NZBD)));
    assertThat(test.getEffectiveDateOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, NZBD)));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.of(
            TENOR_3M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, NZBD)));
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("NZD-BKBM-3M");
  }

  @Test
  public void test_pln_wibor() {
    IborIndex test = IborIndex.of("PLN-WIBOR-3M");
    assertThat(test.getCurrency()).isEqualTo(PLN);
    assertThat(test.getName()).isEqualTo("PLN-WIBOR-3M");
    assertThat(test.getTenor()).isEqualTo(TENOR_3M);
    assertThat(test.getFixingCalendar()).isEqualTo(PLWA);
    assertThat(test.getFixingDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(-2, PLWA));
    assertThat(test.getEffectiveDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(2, PLWA));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.of(
            TENOR_3M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, PLWA)));
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_ACT_ISDA);
    assertThat(test.toString()).isEqualTo("PLN-WIBOR-3M");
  }

  @Test
  public void test_sek_stibor() {
    IborIndex test = IborIndex.of("SEK-STIBOR-3M");
    assertThat(test.getCurrency()).isEqualTo(SEK);
    assertThat(test.getName()).isEqualTo("SEK-STIBOR-3M");
    assertThat(test.getTenor()).isEqualTo(TENOR_3M);
    assertThat(test.getFixingCalendar()).isEqualTo(SEST);
    assertThat(test.getFixingDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(-2, SEST));
    assertThat(test.getEffectiveDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(2, SEST));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.of(
            TENOR_3M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SEST)));
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(THIRTY_U_360);
    assertThat(test.toString()).isEqualTo("SEK-STIBOR-3M");
  }

  @Test
  public void test_sgd_sibor() {
    HolidayCalendarId cal = HolidayCalendarId.of("SGSI");
    IborIndex test = IborIndex.of("SGD-SIBOR-3M");
    assertThat(test.getCurrency()).isEqualTo(SGD);
    assertThat(test.getName()).isEqualTo("SGD-SIBOR-3M");
    assertThat(test.getTenor()).isEqualTo(TENOR_3M);
    assertThat(test.getFixingCalendar()).isEqualTo(cal);
    assertThat(test.getFixingDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(-2, cal));
    assertThat(test.getEffectiveDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(2, cal));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.of(
            TENOR_3M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, cal)));
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("SGD-SIBOR-3M");
  }

  @Test
  public void test_thb_thbfix() {
    HolidayCalendarId cal = HolidayCalendarId.of("THBA");
    IborIndex test = IborIndex.of("THB-THBFIX-6M");
    assertThat(test.getCurrency()).isEqualTo(THB);
    assertThat(test.getName()).isEqualTo("THB-THBFIX-6M");
    assertThat(test.getTenor()).isEqualTo(TENOR_6M);
    assertThat(test.getFixingCalendar()).isEqualTo(cal);
    assertThat(test.getFixingDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(-2, cal));
    assertThat(test.getEffectiveDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(2, cal));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.of(
            TENOR_6M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, cal)));
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("THB-THBFIX-6M");
  }

  @Test
  public void test_twd_taibor() {
    HolidayCalendarId cal = HolidayCalendarId.of("TWTA");
    IborIndex test = IborIndex.of("TWD-TAIBOR-6M");
    assertThat(test.getCurrency()).isEqualTo(TWD);
    assertThat(test.getName()).isEqualTo("TWD-TAIBOR-6M");
    assertThat(test.getTenor()).isEqualTo(TENOR_6M);
    assertThat(test.getFixingCalendar()).isEqualTo(cal);
    assertThat(test.getFixingDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(-2, cal));
    assertThat(test.getEffectiveDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(2, cal));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.of(
            TENOR_6M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, cal)));
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("TWD-TAIBOR-6M");
  }

  @Test
  public void test_zar_jibar() {
    IborIndex test = IborIndex.of("ZAR-JIBAR-3M");
    assertThat(test.getCurrency()).isEqualTo(ZAR);
    assertThat(test.getName()).isEqualTo("ZAR-JIBAR-3M");
    assertThat(test.getTenor()).isEqualTo(TENOR_3M);
    assertThat(test.getFixingCalendar()).isEqualTo(ZAJO);
    assertThat(test.getFixingDateOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(PRECEDING, ZAJO)));
    assertThat(test.getEffectiveDateOffset())
        .isEqualTo(DaysAdjustment.ofCalendarDays(0, BusinessDayAdjustment.of(FOLLOWING, ZAJO)));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(TenorAdjustment.of(
            TENOR_3M, PeriodAdditionConventions.NONE, BusinessDayAdjustment.of(MODIFIED_FOLLOWING, ZAJO)));
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("ZAR-JIBAR-3M");
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {IborIndices.GBP_LIBOR_6M, "GBP-LIBOR-6M"},
        {IborIndices.CHF_LIBOR_6M, "CHF-LIBOR-6M"},
        {IborIndices.EUR_LIBOR_6M, "EUR-LIBOR-6M"},
        {IborIndices.JPY_LIBOR_6M, "JPY-LIBOR-6M"},
        {IborIndices.USD_LIBOR_6M, "USD-LIBOR-6M"},
        {IborIndices.EUR_EURIBOR_1M, "EUR-EURIBOR-1M"},
        {IborIndices.JPY_TIBOR_JAPAN_3M, "JPY-TIBOR-JAPAN-3M"},
        {IborIndices.JPY_TIBOR_EUROYEN_6M, "JPY-TIBOR-EUROYEN-6M"},
        {IborIndices.AUD_BBSW_1M, "AUD-BBSW-1M"},
        {IborIndices.AUD_BBSW_2M, "AUD-BBSW-2M"},
        {IborIndices.AUD_BBSW_3M, "AUD-BBSW-3M"},
        {IborIndices.AUD_BBSW_4M, "AUD-BBSW-4M"},
        {IborIndices.AUD_BBSW_5M, "AUD-BBSW-5M"},
        {IborIndices.AUD_BBSW_6M, "AUD-BBSW-6M"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_name(IborIndex convention, String name) {
    assertThat(convention.getName()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(IborIndex convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(IborIndex convention, String name) {
    assertThat(IborIndex.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_extendedEnum(IborIndex convention, String name) {
    ImmutableMap<String, IborIndex> map = IborIndex.extendedEnum().lookupAll();
    assertThat(map.get(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> IborIndex.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> IborIndex.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
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
    assertThat(a.equals(b)).isEqualTo(false);
  }

  //-------------------------------------------------------------------------
  @Test
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

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(IborIndex.class, IborIndices.GBP_LIBOR_12M);
  }

  @Test
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
