/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.basics.currency.Currency.BRL;
import static com.opengamma.strata.basics.currency.Currency.DKK;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.PLN;
import static com.opengamma.strata.basics.currency.Currency.SEK;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.AUSY;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.BRBD;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.DKCO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.PLWA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SEST;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;

/**
 * Test Overnight Index.
 */
@Test
public class OvernightIndexTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  public void test_gbpSonia() {
    OvernightIndex test = OvernightIndex.of("GBP-SONIA");
    assertEquals(test.getName(), "GBP-SONIA");
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.isActive(), true);
    assertEquals(test.getFixingCalendar(), GBLO);
    assertEquals(test.getPublicationDateOffset(), 0);
    assertEquals(test.getEffectiveDateOffset(), 0);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.toString(), "GBP-SONIA");
  }

  public void test_gbpSonia_dates() {
    OvernightIndex test = OvernightIndex.of("GBP-SONIA");
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 13), REF_DATA), date(2014, 10, 13));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 13), REF_DATA), date(2014, 10, 13));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 13), REF_DATA), date(2014, 10, 14));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 13), REF_DATA), date(2014, 10, 13));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 13), REF_DATA), date(2014, 10, 14));
    // weekend
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 10), REF_DATA), date(2014, 10, 10));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 10), REF_DATA), date(2014, 10, 10));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 10), REF_DATA), date(2014, 10, 13));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 10), REF_DATA), date(2014, 10, 10));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 10), REF_DATA), date(2014, 10, 13));
    // input date is Sunday
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 12), REF_DATA), date(2014, 10, 13));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 12), REF_DATA), date(2014, 10, 13));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 12), REF_DATA), date(2014, 10, 14));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 12), REF_DATA), date(2014, 10, 13));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 12), REF_DATA), date(2014, 10, 14));
  }

  public void test_usdFedFund3m() {
    OvernightIndex test = OvernightIndex.of("USD-FED-FUND");
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getName(), "USD-FED-FUND");
    assertEquals(test.getFixingCalendar(), USNY);
    assertEquals(test.getPublicationDateOffset(), 1);
    assertEquals(test.getEffectiveDateOffset(), 0);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.toString(), "USD-FED-FUND");
  }

  public void test_usdFedFund_dates() {
    OvernightIndex test = OvernightIndex.of("USD-FED-FUND");
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 27), REF_DATA), date(2014, 10, 28));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 27), REF_DATA), date(2014, 10, 27));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 27), REF_DATA), date(2014, 10, 28));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 27), REF_DATA), date(2014, 10, 27));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 27), REF_DATA), date(2014, 10, 28));
    // weekend and US holiday
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 10), REF_DATA), date(2014, 10, 14));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 10), REF_DATA), date(2014, 10, 10));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 10), REF_DATA), date(2014, 10, 14));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 10), REF_DATA), date(2014, 10, 10));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 10), REF_DATA), date(2014, 10, 14));
    // input date is Sunday, 13th is US holiday
    assertEquals(test.calculatePublicationFromFixing(date(2014, 10, 12), REF_DATA), date(2014, 10, 15));
    assertEquals(test.calculateEffectiveFromFixing(date(2014, 10, 12), REF_DATA), date(2014, 10, 14));
    assertEquals(test.calculateMaturityFromFixing(date(2014, 10, 12), REF_DATA), date(2014, 10, 15));
    assertEquals(test.calculateFixingFromEffective(date(2014, 10, 12), REF_DATA), date(2014, 10, 14));
    assertEquals(test.calculateMaturityFromEffective(date(2014, 10, 12), REF_DATA), date(2014, 10, 15));
  }

  public void test_audAonia() {
    OvernightIndex test = OvernightIndex.of("AUD-AONIA");
    assertEquals(test.getName(), "AUD-AONIA");
    assertEquals(test.getCurrency(), AUD);
    assertEquals(test.isActive(), true);
    assertEquals(test.getFixingCalendar(), AUSY);
    assertEquals(test.getPublicationDateOffset(), 0);
    assertEquals(test.getEffectiveDateOffset(), 0);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.toString(), "AUD-AONIA");
  }

  public void test_brlCdi() {
    OvernightIndex test = OvernightIndex.of("BRL-CDI");
    assertEquals(test.getName(), "BRL-CDI");
    assertEquals(test.getCurrency(), BRL);
    assertEquals(test.isActive(), true);
    assertEquals(test.getFixingCalendar(), BRBD);
    assertEquals(test.getPublicationDateOffset(), 1);
    assertEquals(test.getEffectiveDateOffset(), 0);
    assertEquals(test.getDayCount(), DayCount.ofBus252(BRBD));
    assertEquals(test.toString(), "BRL-CDI");
  }

  public void test_dkkOis() {
    OvernightIndex test = OvernightIndex.of("DKK-TNR");
    assertEquals(test.getName(), "DKK-TNR");
    assertEquals(test.getCurrency(), DKK);
    assertEquals(test.isActive(), true);
    assertEquals(test.getFixingCalendar(), DKCO);
    assertEquals(test.getPublicationDateOffset(), 1);
    assertEquals(test.getEffectiveDateOffset(), 1);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.toString(), "DKK-TNR");
  }

  public void test_plnOis() {
    OvernightIndex test = OvernightIndex.of("PLN-POLONIA");
    assertEquals(test.getName(), "PLN-POLONIA");
    assertEquals(test.getCurrency(), PLN);
    assertEquals(test.isActive(), true);
    assertEquals(test.getFixingCalendar(), PLWA);
    assertEquals(test.getPublicationDateOffset(), 0);
    assertEquals(test.getEffectiveDateOffset(), 0);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.toString(), "PLN-POLONIA");
  }

  public void test_sekOis() {
    OvernightIndex test = OvernightIndex.of("SEK-SIOR");
    assertEquals(test.getName(), "SEK-SIOR");
    assertEquals(test.getCurrency(), SEK);
    assertEquals(test.isActive(), true);
    assertEquals(test.getFixingCalendar(), SEST);
    assertEquals(test.getPublicationDateOffset(), 0);
    assertEquals(test.getEffectiveDateOffset(), 1);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.toString(), "SEK-SIOR");
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {OvernightIndices.GBP_SONIA, "GBP-SONIA"},
        {OvernightIndices.CHF_TOIS, "CHF-TOIS"},
        {OvernightIndices.EUR_EONIA, "EUR-EONIA"},
        {OvernightIndices.JPY_TONAR, "JPY-TONAR"},
        {OvernightIndices.USD_FED_FUND, "USD-FED-FUND"},
        {OvernightIndices.AUD_AONIA, "AUD-AONIA"},
        {OvernightIndices.BRL_CDI, "BRL-CDI"},
        {OvernightIndices.DKK_TNR, "DKK-TNR"},
    };
  }

  @Test(dataProvider = "name")
  public void test_name(OvernightIndex convention, String name) {
    assertEquals(convention.getName(), name);
  }

  @Test(dataProvider = "name")
  public void test_toString(OvernightIndex convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(OvernightIndex convention, String name) {
    assertEquals(OvernightIndex.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_extendedEnum(OvernightIndex convention, String name) {
    ImmutableMap<String, OvernightIndex> map = OvernightIndex.extendedEnum().lookupAll();
    assertEquals(map.get(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> OvernightIndex.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> OvernightIndex.of(null));
  }

  //-------------------------------------------------------------------------
  public void test_equals() {
    ImmutableOvernightIndex a = ImmutableOvernightIndex.builder()
        .name("Test")
        .currency(Currency.GBP)
        .fixingCalendar(GBLO)
        .publicationDateOffset(0)
        .effectiveDateOffset(0)
        .dayCount(ACT_360)
        .build();
    OvernightIndex b = a.toBuilder().name("Rubbish").build();
    assertEquals(a.equals(b), false);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ImmutableOvernightIndex index = ImmutableOvernightIndex.builder()
        .name("Test")
        .currency(Currency.GBP)
        .fixingCalendar(GBLO)
        .publicationDateOffset(0)
        .effectiveDateOffset(0)
        .dayCount(ACT_360)
        .build();
    coverImmutableBean(index);
    coverPrivateConstructor(OvernightIndices.class);
  }

  public void test_jodaConvert() {
    assertJodaConvert(OvernightIndex.class, OvernightIndices.GBP_SONIA);
  }

  public void test_serialization() {
    OvernightIndex index = ImmutableOvernightIndex.builder()
        .name("Test")
        .currency(Currency.GBP)
        .fixingCalendar(GBLO)
        .publicationDateOffset(0)
        .effectiveDateOffset(0)
        .dayCount(ACT_360)
        .build();
    assertSerialization(index);
  }

}
