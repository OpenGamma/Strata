/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.date;

import static com.opengamma.strata.basics.date.HolidayCalendarIds.NO_HOLIDAYS;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ImmutableReferenceData;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.ReferenceDataNotFoundException;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyPair;

/**
 * Test {@link HolidayCalendarId}.
 */
public class HolidayCalendarIdTest {

  private static final Object ANOTHER_TYPE = "";

  @Test
  public void test_of_single() {
    HolidayCalendarId test = HolidayCalendarId.of("GB");
    assertThat(test.getName()).isEqualTo("GB");
    assertThat(test.getReferenceDataType()).isEqualTo(HolidayCalendar.class);
    assertThat(test.toString()).isEqualTo("GB");
  }

  @Test
  public void test_of_combined() {
    HolidayCalendarId test = HolidayCalendarId.of("GB+EU");
    assertThat(test.getName()).isEqualTo("EU+GB");
    assertThat(test.getReferenceDataType()).isEqualTo(HolidayCalendar.class);
    assertThat(test.toString()).isEqualTo("EU+GB");

    HolidayCalendarId test2 = HolidayCalendarId.of("EU+GB");
    assertThat(test).isSameAs(test2);
  }

  @Test
  public void test_of_combined_NoHolidays() {
    HolidayCalendarId test = HolidayCalendarId.of("GB+NoHolidays+EU");
    assertThat(test.getName()).isEqualTo("EU+GB");
    assertThat(test.getReferenceDataType()).isEqualTo(HolidayCalendar.class);
    assertThat(test.toString()).isEqualTo("EU+GB");
  }

  @Test
  public void test_of_linked() {
    HolidayCalendarId test = HolidayCalendarId.of("GB~EU");
    assertThat(test.getName()).isEqualTo("EU~GB");
    assertThat(test.getReferenceDataType()).isEqualTo(HolidayCalendar.class);
    assertThat(test.toString()).isEqualTo("EU~GB");

    HolidayCalendarId test2 = HolidayCalendarId.of("EU~GB");
    assertThat(test).isSameAs(test2);
  }

  @Test
  public void test_of_linked_NoHolidays() {
    HolidayCalendarId test = HolidayCalendarId.of("GB~NoHolidays~EU");
    assertThat(test.getName()).isEqualTo("NoHolidays");
    assertThat(test.getReferenceDataType()).isEqualTo(HolidayCalendar.class);
    assertThat(test.toString()).isEqualTo("NoHolidays");
  }

  @Test
  public void test_of_linked_combined() {
    HolidayCalendarId test = HolidayCalendarId.of("GB~EU+Fri/Sat");
    assertThat(test.getName()).isEqualTo("EU+Fri/Sat~GB");
    assertThat(test.getReferenceDataType()).isEqualTo(HolidayCalendar.class);
    assertThat(test.toString()).isEqualTo("EU+Fri/Sat~GB");
  }

  @Test
  public void test_defaultByCurrency() {
    assertThat(HolidayCalendarId.defaultByCurrency(Currency.GBP)).isEqualTo(HolidayCalendarIds.GBLO);
    assertThat(HolidayCalendarId.defaultByCurrency(Currency.CZK)).isEqualTo(HolidayCalendarIds.CZPR);
    assertThat(HolidayCalendarId.defaultByCurrency(Currency.HKD)).isEqualTo(HolidayCalendarId.of("HKHK"));
    assertThatIllegalArgumentException().isThrownBy(() -> HolidayCalendarId.defaultByCurrency(Currency.XAG));
  }

  @Test
  public void test_findDefaultByCurrency() {
    assertThat(HolidayCalendarId.findDefaultByCurrency(Currency.GBP)).hasValue(HolidayCalendarIds.GBLO);
    assertThat(HolidayCalendarId.findDefaultByCurrency(Currency.CZK)).hasValue(HolidayCalendarIds.CZPR);
    assertThat(HolidayCalendarId.findDefaultByCurrency(Currency.HKD)).hasValue(HolidayCalendarId.of("HKHK"));
    assertThat(HolidayCalendarId.findDefaultByCurrency(Currency.XAG)).isEqualTo(Optional.empty());
  }

  @Test
  public void test_defaultByCurrencyPair() {
    assertThat(HolidayCalendarId.defaultByCurrencyPair(CurrencyPair.of(Currency.USD, Currency.GBP)))
        .isEqualTo(HolidayCalendarIds.USNY.combinedWith(HolidayCalendarIds.GBLO));
    assertThat(HolidayCalendarId.defaultByCurrencyPair(CurrencyPair.of(Currency.GBP, Currency.CZK)))
        .isEqualTo(HolidayCalendarId.of("CZPR+GBLO"));
    assertThat(HolidayCalendarId.defaultByCurrencyPair(CurrencyPair.of(Currency.USD, Currency.XAG)))
        .isEqualTo(HolidayCalendarIds.USNY);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_resolve_single() {
    HolidayCalendarId gb = HolidayCalendarId.of("GB");
    HolidayCalendarId eu = HolidayCalendarId.of("EU");
    HolidayCalendar gbCal = HolidayCalendars.SAT_SUN;
    ReferenceData refData = ImmutableReferenceData.of(gb, gbCal);
    assertThat(gb.resolve(refData)).isEqualTo(gbCal);
    assertThatExceptionOfType(ReferenceDataNotFoundException.class).isThrownBy(() -> eu.resolve(refData));
    assertThat(refData.getValue(gb)).isEqualTo(gbCal);
  }

  @Test
  public void test_resolve_combined_direct() {
    HolidayCalendarId gb = HolidayCalendarId.of("GB");
    HolidayCalendar gbCal = HolidayCalendars.SAT_SUN;
    HolidayCalendarId eu = HolidayCalendarId.of("EU");
    HolidayCalendar euCal = HolidayCalendars.FRI_SAT;
    HolidayCalendarId combined = gb.combinedWith(eu);
    HolidayCalendar combinedCal = euCal.combinedWith(gbCal);
    ReferenceData refData = ImmutableReferenceData.of(ImmutableMap.of(combined, combinedCal));
    assertThat(combined.resolve(refData)).isEqualTo(combinedCal);
    assertThat(refData.getValue(combined)).isEqualTo(combinedCal);
  }

  @Test
  public void test_resolve_combined_indirect() {
    HolidayCalendarId gb = HolidayCalendarId.of("GB");
    HolidayCalendar gbCal = HolidayCalendars.SAT_SUN;
    HolidayCalendarId eu = HolidayCalendarId.of("EU");
    HolidayCalendar euCal = HolidayCalendars.FRI_SAT;
    HolidayCalendarId combined = gb.combinedWith(eu);
    HolidayCalendar combinedCal = euCal.combinedWith(gbCal);
    ReferenceData refData = ImmutableReferenceData.of(ImmutableMap.of(gb, gbCal, eu, euCal));
    assertThat(combined.resolve(refData)).isEqualTo(combinedCal);
    assertThat(refData.getValue(combined)).isEqualTo(combinedCal);
  }

  @Test
  public void testImmutableReferenceDataWithMergedHolidays() {
    HolidayCalendar hc = HolidayCalendars.FRI_SAT.combinedWith(HolidayCalendars.SAT_SUN);
    ImmutableReferenceData referenceData = ImmutableReferenceData.of(hc.getId(), hc);
    LocalDate date =
        BusinessDayAdjustment.of(BusinessDayConventions.PRECEDING, hc.getId()).adjust(LocalDate.of(2016, 8, 20), referenceData);
    assertThat(LocalDate.of(2016, 8, 18)).isEqualTo(date);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_combinedWith() {
    HolidayCalendarId gb = HolidayCalendarId.of("GB");
    HolidayCalendarId eu = HolidayCalendarId.of("EU");
    HolidayCalendarId us = HolidayCalendarId.of("US");
    HolidayCalendarId combined1 = eu.combinedWith(us).combinedWith(gb);
    HolidayCalendarId combined2 = us.combinedWith(eu).combinedWith(gb.combinedWith(us));
    assertThat(combined1.getName()).isEqualTo("EU+GB+US");
    assertThat(combined1.toString()).isEqualTo("EU+GB+US");
    assertThat(combined2.getName()).isEqualTo("EU+GB+US");
    assertThat(combined2.toString()).isEqualTo("EU+GB+US");
    assertThat(combined1.equals(combined2)).isEqualTo(true);
  }

  @Test
  public void test_combinedWithSelf() {
    HolidayCalendarId gb = HolidayCalendarId.of("GB");
    assertThat(gb.combinedWith(gb)).isEqualTo(gb);
    assertThat(gb.combinedWith(NO_HOLIDAYS)).isEqualTo(gb);
    assertThat(NO_HOLIDAYS.combinedWith(gb)).isEqualTo(gb);
    assertThat(NO_HOLIDAYS.combinedWith(NO_HOLIDAYS)).isEqualTo(NO_HOLIDAYS);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCode() {
    HolidayCalendarId a = HolidayCalendarId.of("GB");
    HolidayCalendarId a2 = HolidayCalendarId.of("GB");
    HolidayCalendarId b = HolidayCalendarId.of("EU");
    assertThat(a.hashCode()).isEqualTo(a2.hashCode());
    assertThat(a.equals(a)).isEqualTo(true);
    assertThat(a.equals(a2)).isEqualTo(true);
    assertThat(a.equals(b)).isEqualTo(false);
    assertThat(a.equals(null)).isEqualTo(false);
    assertThat(a.equals(ANOTHER_TYPE)).isEqualTo(false);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(HolidayCalendarIds.class);
  }

  @Test
  public void test_serialization() {
    HolidayCalendarId test = HolidayCalendarId.of("US");
    assertSerialization(test);
  }

}
