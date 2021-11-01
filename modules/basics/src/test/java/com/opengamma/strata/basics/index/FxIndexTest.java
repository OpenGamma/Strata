/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.basics.currency.Currency.CAD;
import static com.opengamma.strata.basics.currency.Currency.COP;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.NO_HOLIDAYS;
import static com.opengamma.strata.basics.index.FxIndices.EUR_CHF_ECB;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.joda.beans.ImmutableBean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendarId;

/**
 * Test {@link FxIndex}.
 */
public class FxIndexTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {FxIndices.EUR_CHF_ECB, "EUR/CHF-ECB"},
        {FxIndices.EUR_GBP_ECB, "EUR/GBP-ECB"},
        {FxIndices.EUR_JPY_ECB, "EUR/JPY-ECB"},
        {FxIndices.EUR_USD_ECB, "EUR/USD-ECB"},
        {FxIndices.USD_CHF_WM, "USD/CHF-WM"},
        {FxIndices.EUR_USD_WM, "EUR/USD-WM"},
        {FxIndices.GBP_USD_WM, "GBP/USD-WM"},
        {FxIndices.USD_JPY_WM, "USD/JPY-WM"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_name(FxIndex convention, String name) {
    assertThat(convention.getName()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(FxIndex convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(FxIndex convention, String name) {
    assertThat(FxIndex.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_extendedEnum(FxIndex convention, String name) {
    ImmutableMap<String, FxIndex> map = FxIndex.extendedEnum().lookupAll();
    assertThat(map.get(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> FxIndex.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> FxIndex.of((String) null));
  }

  @Test
  public void test_of_lookup_parse_currency() {
    CurrencyPair usdCad = CurrencyPair.of(USD, CAD);
    HolidayCalendarId calendarId = HolidayCalendarId.defaultByCurrencyPair(usdCad);
    ImmutableFxIndex fxIndex = ImmutableFxIndex.builder()
        .name(usdCad.toString())
        .currencyPair(usdCad)
        .fixingCalendar(calendarId)
        .maturityDateOffset(DaysAdjustment.ofBusinessDays(2, calendarId))
        .build();
    assertThat(FxIndex.of(usdCad.toString())).usingRecursiveComparison().isEqualTo(fxIndex);
  }

  @Test
  public void test_of_lookup_currency_pair_from_extendedEnum() {
    ImmutableFxIndex fxIndex = ImmutableFxIndex.builder()
        .name("USD/COP-TRM-COP02")
        .currencyPair(CurrencyPair.of(USD, COP))
        .fixingCalendar(HolidayCalendarId.of("COBO"))
        .maturityDateOffset(DaysAdjustment.ofBusinessDays(0, HolidayCalendarId.of("COBO")))
        .build();
    assertThat(FxIndex.of(CurrencyPair.of(USD, COP))).usingRecursiveComparison()
        .isEqualTo(fxIndex);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_ecb_eur_gbp_dates() {
    FxIndex test = FxIndices.EUR_GBP_ECB;
    assertThat(test.getFixingDateOffset())
        .isEqualTo(DaysAdjustment.ofBusinessDays(-2, EUTA.combinedWith(GBLO)));
    assertThat(test.getMaturityDateOffset())
        .isEqualTo(DaysAdjustment.ofBusinessDays(2, EUTA.combinedWith(GBLO)));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 13), REF_DATA)).isEqualTo(date(2014, 10, 15));
    assertThat(test.calculateFixingFromMaturity(date(2014, 10, 15), REF_DATA)).isEqualTo(date(2014, 10, 13));
    // weekend
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 16), REF_DATA)).isEqualTo(date(2014, 10, 20));
    assertThat(test.calculateFixingFromMaturity(date(2014, 10, 20), REF_DATA)).isEqualTo(date(2014, 10, 16));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 17), REF_DATA)).isEqualTo(date(2014, 10, 21));
    assertThat(test.calculateFixingFromMaturity(date(2014, 10, 21), REF_DATA)).isEqualTo(date(2014, 10, 17));
    // input date is Sunday
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 19), REF_DATA)).isEqualTo(date(2014, 10, 22));
    assertThat(test.calculateFixingFromMaturity(date(2014, 10, 19), REF_DATA)).isEqualTo(date(2014, 10, 16));
    // skip maturity over EUR (1st May) and GBP (5th May) holiday
    assertThat(test.calculateMaturityFromFixing(date(2014, 4, 30), REF_DATA)).isEqualTo(date(2014, 5, 6));
    assertThat(test.calculateFixingFromMaturity(date(2014, 5, 6), REF_DATA)).isEqualTo(date(2014, 4, 30));
    // resolve
    assertThat(test.resolve(REF_DATA).apply(date(2014, 5, 6)))
        .isEqualTo(FxIndexObservation.of(test, date(2014, 5, 6), REF_DATA));
  }

  @Test
  public void test_dates() {
    FxIndex test = ImmutableFxIndex.builder()
        .name("Test")
        .currencyPair(CurrencyPair.of(EUR, GBP))
        .fixingCalendar(NO_HOLIDAYS)
        .maturityDateOffset(DaysAdjustment.ofCalendarDays(2))
        .build();
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 13), REF_DATA)).isEqualTo(date(2014, 10, 15));
    assertThat(test.calculateFixingFromMaturity(date(2014, 10, 15), REF_DATA)).isEqualTo(date(2014, 10, 13));
    // weekend
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 16), REF_DATA)).isEqualTo(date(2014, 10, 18));
    assertThat(test.calculateFixingFromMaturity(date(2014, 10, 18), REF_DATA)).isEqualTo(date(2014, 10, 16));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 17), REF_DATA)).isEqualTo(date(2014, 10, 19));
    assertThat(test.calculateFixingFromMaturity(date(2014, 10, 19), REF_DATA)).isEqualTo(date(2014, 10, 17));
    // input date is Sunday
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 19), REF_DATA)).isEqualTo(date(2014, 10, 21));
    assertThat(test.calculateFixingFromMaturity(date(2014, 10, 19), REF_DATA)).isEqualTo(date(2014, 10, 17));
  }

  @Test
  public void test_cny() {
    FxIndex test = FxIndex.of("USD/CNY-SAEC-CNY01");
    assertThat(test.getName()).isEqualTo("USD/CNY-SAEC-CNY01");
  }

  @Test
  public void test_inr() {
    FxIndex test = FxIndex.of("USD/INR-FBIL-INR01");
    assertThat(test.getName()).isEqualTo("USD/INR-FBIL-INR01");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equals() {
    ImmutableFxIndex a = ImmutableFxIndex.builder()
        .name("GBP-EUR")
        .currencyPair(CurrencyPair.of(GBP, EUR))
        .fixingCalendar(GBLO)
        .maturityDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .build();
    ImmutableFxIndex b = a.toBuilder().name("EUR-GBP").build();
    assertThat(a.equals(b)).isEqualTo(false);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverPrivateConstructor(FxIndices.class);
    coverImmutableBean((ImmutableBean) EUR_CHF_ECB);
  }

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(FxIndex.class, EUR_CHF_ECB);
  }

  @Test
  public void test_serialization() {
    assertSerialization(EUR_CHF_ECB);
  }

}
