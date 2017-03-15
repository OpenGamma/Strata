/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.NO_HOLIDAYS;
import static com.opengamma.strata.basics.index.FxIndices.EUR_CHF_ECB;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.joda.beans.ImmutableBean;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.CurrencyPair;
import com.opengamma.strata.basics.date.DaysAdjustment;

/**
 * Test {@link FxIndex}.
 */
@Test
public class FxIndexTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
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

  @Test(dataProvider = "name")
  public void test_name(FxIndex convention, String name) {
    assertEquals(convention.getName(), name);
  }

  @Test(dataProvider = "name")
  public void test_toString(FxIndex convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(FxIndex convention, String name) {
    assertEquals(FxIndex.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_extendedEnum(FxIndex convention, String name) {
    ImmutableMap<String, FxIndex> map = FxIndex.extendedEnum().lookupAll();
    assertEquals(map.get(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> FxIndex.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> FxIndex.of((String) null));
  }

  //-------------------------------------------------------------------------
  public void test_ecb_eur_gbp_dates() {
    FxIndex test = FxIndices.EUR_GBP_ECB;
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 13), REF_DATA), date(2014, 10, 15));
    assertEquals(test.calculateFixingFromMaturity(date(2014, 10, 15), REF_DATA), date(2014, 10, 13));
    // weekend
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 16), REF_DATA), date(2014, 10, 20));
    assertEquals(test.calculateFixingFromMaturity(date(2014, 10, 20), REF_DATA), date(2014, 10, 16));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 17), REF_DATA), date(2014, 10, 21));
    assertEquals(test.calculateFixingFromMaturity(date(2014, 10, 21), REF_DATA), date(2014, 10, 17));
    // input date is Sunday
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 19), REF_DATA), date(2014, 10, 22));
    assertEquals(test.calculateFixingFromMaturity(date(2014, 10, 19), REF_DATA), date(2014, 10, 16));
    // skip maturity over EUR (1st May) and GBP (5th May) holiday
    assertEquals(test.calculateMaturityFromFixing(date(2014, 4, 30), REF_DATA), date(2014, 5, 6));
    assertEquals(test.calculateFixingFromMaturity(date(2014, 5, 6), REF_DATA), date(2014, 4, 30));
    // resolve
    assertEquals(test.resolve(REF_DATA).apply(date(2014, 5, 6)), FxIndexObservation.of(test, date(2014, 5, 6), REF_DATA));
  }

  public void test_dates() {
    FxIndex test = ImmutableFxIndex.builder()
        .name("Test")
        .currencyPair(CurrencyPair.of(EUR, GBP))
        .fixingCalendar(NO_HOLIDAYS)
        .maturityDateOffset(DaysAdjustment.ofCalendarDays(2))
        .build();
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 13), REF_DATA), date(2014, 10, 15));
    assertEquals(test.calculateFixingFromMaturity(date(2014, 10, 15), REF_DATA), date(2014, 10, 13));
    // weekend
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 16), REF_DATA), date(2014, 10, 18));
    assertEquals(test.calculateFixingFromMaturity(date(2014, 10, 18), REF_DATA), date(2014, 10, 16));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 17), REF_DATA), date(2014, 10, 19));
    assertEquals(test.calculateFixingFromMaturity(date(2014, 10, 19), REF_DATA), date(2014, 10, 17));
    // input date is Sunday
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 19), REF_DATA), date(2014, 10, 21));
    assertEquals(test.calculateFixingFromMaturity(date(2014, 10, 19), REF_DATA), date(2014, 10, 17));
  }

  //-------------------------------------------------------------------------
  public void test_equals() {
    ImmutableFxIndex a = ImmutableFxIndex.builder()
        .name("GBP-EUR")
        .currencyPair(CurrencyPair.of(GBP, EUR))
        .fixingCalendar(GBLO)
        .maturityDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .build();
    ImmutableFxIndex b = a.toBuilder().name("EUR-GBP").build();
    assertEquals(a.equals(b), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(FxIndices.class);
    coverImmutableBean((ImmutableBean) EUR_CHF_ECB);
  }

  public void test_jodaConvert() {
    assertJodaConvert(FxIndex.class, EUR_CHF_ECB);
  }

  public void test_serialization() {
    assertSerialization(EUR_CHF_ECB);
  }

}
