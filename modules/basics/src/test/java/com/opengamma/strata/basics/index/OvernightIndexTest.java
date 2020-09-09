/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.basics.currency.Currency.AUD;
import static com.opengamma.strata.basics.currency.Currency.BRL;
import static com.opengamma.strata.basics.currency.Currency.CHF;
import static com.opengamma.strata.basics.currency.Currency.CLP;
import static com.opengamma.strata.basics.currency.Currency.DKK;
import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.HKD;
import static com.opengamma.strata.basics.currency.Currency.INR;
import static com.opengamma.strata.basics.currency.Currency.NZD;
import static com.opengamma.strata.basics.currency.Currency.PLN;
import static com.opengamma.strata.basics.currency.Currency.SEK;
import static com.opengamma.strata.basics.currency.Currency.SGD;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.currency.Currency.ZAR;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.AUSY;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.BRBD;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.CHZU;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.DKCO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.PLWA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.SEST;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USGS;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.ZAJO;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.HolidayCalendarId;

/**
 * Test Overnight Index.
 */
public class OvernightIndexTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  @Test
  public void test_gbpSonia() {
    OvernightIndex test = OvernightIndex.of("GBP-SONIA");
    assertThat(test.getName()).isEqualTo("GBP-SONIA");
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.isActive()).isEqualTo(true);
    assertThat(test.getFixingCalendar()).isEqualTo(GBLO);
    assertThat(test.getPublicationDateOffset()).isEqualTo(1);
    assertThat(test.getEffectiveDateOffset()).isEqualTo(0);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getFloatingRateName()).isEqualTo(FloatingRateName.of("GBP-SONIA"));
    assertThat(test.toString()).isEqualTo("GBP-SONIA");
  }

  @Test
  public void test_gbpSonia_dates() {
    OvernightIndex test = OvernightIndex.of("GBP-SONIA");
    assertThat(test.calculatePublicationFromFixing(date(2014, 10, 13), REF_DATA)).isEqualTo(date(2014, 10, 14));
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 13), REF_DATA)).isEqualTo(date(2014, 10, 13));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 13), REF_DATA)).isEqualTo(date(2014, 10, 14));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 13), REF_DATA)).isEqualTo(date(2014, 10, 13));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 13), REF_DATA)).isEqualTo(date(2014, 10, 14));
    // weekend
    assertThat(test.calculatePublicationFromFixing(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2014, 10, 13));
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2014, 10, 10));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2014, 10, 13));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2014, 10, 10));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2014, 10, 13));
    // input date is Sunday
    assertThat(test.calculatePublicationFromFixing(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2014, 10, 14));
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2014, 10, 13));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2014, 10, 14));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2014, 10, 13));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2014, 10, 14));
  }

  @Test
  public void test_chfSaron() {
    OvernightIndex test = OvernightIndex.of("CHF-SARON");
    assertThat(test.getName()).isEqualTo("CHF-SARON");
    assertThat(test.getCurrency()).isEqualTo(CHF);
    assertThat(test.isActive()).isEqualTo(true);
    assertThat(test.getFixingCalendar()).isEqualTo(CHZU);
    assertThat(test.getPublicationDateOffset()).isEqualTo(0);
    assertThat(test.getEffectiveDateOffset()).isEqualTo(0);
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_360);
    assertThat(test.getFloatingRateName()).isEqualTo(FloatingRateName.of("CHF-SARON"));
    assertThat(test.toString()).isEqualTo("CHF-SARON");
  }

  @Test
  public void test_getFloatingRateName() {
    for (OvernightIndex index : OvernightIndex.extendedEnum().lookupAll().values()) {
      assertThat(index.getFloatingRateName()).isEqualTo(FloatingRateName.of(index.getName()));
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_usdFedFund3m() {
    OvernightIndex test = OvernightIndex.of("USD-FED-FUND");
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getName()).isEqualTo("USD-FED-FUND");
    assertThat(test.getFixingCalendar()).isEqualTo(USNY);
    assertThat(test.getPublicationDateOffset()).isEqualTo(1);
    assertThat(test.getEffectiveDateOffset()).isEqualTo(0);
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_360);
    assertThat(test.toString()).isEqualTo("USD-FED-FUND");
  }

  @Test
  public void test_usdFedFund_dates() {
    OvernightIndex test = OvernightIndex.of("USD-FED-FUND");
    assertThat(test.calculatePublicationFromFixing(date(2014, 10, 27), REF_DATA)).isEqualTo(date(2014, 10, 28));
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 27), REF_DATA)).isEqualTo(date(2014, 10, 27));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 27), REF_DATA)).isEqualTo(date(2014, 10, 28));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 27), REF_DATA)).isEqualTo(date(2014, 10, 27));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 27), REF_DATA)).isEqualTo(date(2014, 10, 28));
    // weekend and US holiday
    assertThat(test.calculatePublicationFromFixing(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2014, 10, 14));
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2014, 10, 10));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2014, 10, 14));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2014, 10, 10));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 10), REF_DATA)).isEqualTo(date(2014, 10, 14));
    // input date is Sunday, 13th is US holiday
    assertThat(test.calculatePublicationFromFixing(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2014, 10, 15));
    assertThat(test.calculateEffectiveFromFixing(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2014, 10, 14));
    assertThat(test.calculateMaturityFromFixing(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2014, 10, 15));
    assertThat(test.calculateFixingFromEffective(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2014, 10, 14));
    assertThat(test.calculateMaturityFromEffective(date(2014, 10, 12), REF_DATA)).isEqualTo(date(2014, 10, 15));
  }

  @Test
  public void test_usdSofr() {
    OvernightIndex test = OvernightIndex.of("USD-SOFR");
    assertThat(test.getName()).isEqualTo("USD-SOFR");
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.isActive()).isEqualTo(true);
    assertThat(test.getFixingCalendar()).isEqualTo(USGS);
    assertThat(test.getPublicationDateOffset()).isEqualTo(1);
    assertThat(test.getEffectiveDateOffset()).isEqualTo(0);
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.toString()).isEqualTo("USD-SOFR");
  }

  //-------------------------------------------------------------------------

  @Test
  public void test_eurEonia() {
    OvernightIndex test = OvernightIndex.of("EUR-EONIA");
    assertThat(test.getName()).isEqualTo("EUR-EONIA");
    assertThat(test.getCurrency()).isEqualTo(EUR);
    assertThat(test.isActive()).isEqualTo(true);
    assertThat(test.getFixingCalendar()).isEqualTo(EUTA);
    assertThat(test.getPublicationDateOffset()).isEqualTo(0);
    assertThat(test.getEffectiveDateOffset()).isEqualTo(0);
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.toString()).isEqualTo("EUR-EONIA");
  }

  @Test
  public void test_eurEstr() {
    OvernightIndex test = OvernightIndex.of("EUR-ESTR");
    assertThat(test.getName()).isEqualTo("EUR-ESTR");
    assertThat(test.getCurrency()).isEqualTo(EUR);
    assertThat(test.isActive()).isEqualTo(true);
    assertThat(test.getFixingCalendar()).isEqualTo(EUTA);
    assertThat(test.getPublicationDateOffset()).isEqualTo(1);
    assertThat(test.getEffectiveDateOffset()).isEqualTo(0);
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.toString()).isEqualTo("EUR-ESTR");
    // old name
    assertThat(OvernightIndex.of("EUR-ESTER")).isEqualTo(test);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_audAonia() {
    OvernightIndex test = OvernightIndex.of("AUD-AONIA");
    assertThat(test.getName()).isEqualTo("AUD-AONIA");
    assertThat(test.getCurrency()).isEqualTo(AUD);
    assertThat(test.isActive()).isEqualTo(true);
    assertThat(test.getFixingCalendar()).isEqualTo(AUSY);
    assertThat(test.getPublicationDateOffset()).isEqualTo(0);
    assertThat(test.getEffectiveDateOffset()).isEqualTo(0);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("AUD-AONIA");
  }

  @Test
  public void test_brlCdi() {
    OvernightIndex test = OvernightIndex.of("BRL-CDI");
    assertThat(test.getName()).isEqualTo("BRL-CDI");
    assertThat(test.getCurrency()).isEqualTo(BRL);
    assertThat(test.isActive()).isEqualTo(true);
    assertThat(test.getFixingCalendar()).isEqualTo(BRBD);
    assertThat(test.getPublicationDateOffset()).isEqualTo(1);
    assertThat(test.getEffectiveDateOffset()).isEqualTo(0);
    assertThat(test.getDayCount()).isEqualTo(DayCount.ofBus252(BRBD));
    assertThat(test.toString()).isEqualTo("BRL-CDI");
  }

  @Test
  public void test_clpOis() {
    OvernightIndex test = OvernightIndex.of("CLP-TNA");
    assertThat(test.getName()).isEqualTo("CLP-TNA");
    assertThat(test.getCurrency()).isEqualTo(CLP);
    assertThat(test.isActive()).isEqualTo(true);
    assertThat(test.getFixingCalendar()).isEqualTo(HolidayCalendarId.of("CLSA"));
    assertThat(test.getPublicationDateOffset()).isEqualTo(0);
    assertThat(test.getEffectiveDateOffset()).isEqualTo(0);
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_360);
    assertThat(test.toString()).isEqualTo("CLP-TNA");
  }

  @Test
  public void test_dkkOis() {
    OvernightIndex test = OvernightIndex.of("DKK-TNR");
    assertThat(test.getName()).isEqualTo("DKK-TNR");
    assertThat(test.getCurrency()).isEqualTo(DKK);
    assertThat(test.isActive()).isEqualTo(true);
    assertThat(test.getFixingCalendar()).isEqualTo(DKCO);
    assertThat(test.getPublicationDateOffset()).isEqualTo(1);
    assertThat(test.getEffectiveDateOffset()).isEqualTo(1);
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_360);
    assertThat(test.toString()).isEqualTo("DKK-TNR");
  }

  @Test
  public void test_hkdOis() {
    OvernightIndex test = OvernightIndex.of("HKD-HONIA");
    assertThat(test.getName()).isEqualTo("HKD-HONIA");
    assertThat(test.getCurrency()).isEqualTo(HKD);
    assertThat(test.isActive()).isEqualTo(true);
    assertThat(test.getFixingCalendar()).isEqualTo(HolidayCalendarId.of("HKHK"));
    assertThat(test.getPublicationDateOffset()).isEqualTo(0);
    assertThat(test.getEffectiveDateOffset()).isEqualTo(0);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("HKD-HONIA");
    // alternative name
    assertThat(OvernightIndex.of("HKD-HONIX")).isEqualTo(test);
  }

  @Test
  public void test_inrOis() {
    OvernightIndex test = OvernightIndex.of("INR-OMIBOR");
    assertThat(test.getName()).isEqualTo("INR-OMIBOR");
    assertThat(test.getCurrency()).isEqualTo(INR);
    assertThat(test.isActive()).isEqualTo(true);
    assertThat(test.getFixingCalendar()).isEqualTo(HolidayCalendarId.of("INMU"));
    assertThat(test.getPublicationDateOffset()).isEqualTo(0);
    assertThat(test.getEffectiveDateOffset()).isEqualTo(0);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("INR-OMIBOR");
  }

  @Test
  public void test_nzdOis() {
    OvernightIndex test = OvernightIndex.of("NZD-NZIONA");
    assertThat(test.getName()).isEqualTo("NZD-NZIONA");
    assertThat(test.getCurrency()).isEqualTo(NZD);
    assertThat(test.isActive()).isEqualTo(true);
    assertThat(test.getFixingCalendar()).isEqualTo(HolidayCalendarId.of("NZBD"));
    assertThat(test.getPublicationDateOffset()).isEqualTo(0);
    assertThat(test.getEffectiveDateOffset()).isEqualTo(0);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("NZD-NZIONA");
  }

  @Test
  public void test_plnOis() {
    OvernightIndex test = OvernightIndex.of("PLN-POLONIA");
    assertThat(test.getName()).isEqualTo("PLN-POLONIA");
    assertThat(test.getCurrency()).isEqualTo(PLN);
    assertThat(test.isActive()).isEqualTo(true);
    assertThat(test.getFixingCalendar()).isEqualTo(PLWA);
    assertThat(test.getPublicationDateOffset()).isEqualTo(0);
    assertThat(test.getEffectiveDateOffset()).isEqualTo(0);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("PLN-POLONIA");
  }

  @Test
  public void test_sekOis() {
    OvernightIndex test = OvernightIndex.of("SEK-SIOR");
    assertThat(test.getName()).isEqualTo("SEK-SIOR");
    assertThat(test.getCurrency()).isEqualTo(SEK);
    assertThat(test.isActive()).isEqualTo(true);
    assertThat(test.getFixingCalendar()).isEqualTo(SEST);
    assertThat(test.getPublicationDateOffset()).isEqualTo(0);
    assertThat(test.getEffectiveDateOffset()).isEqualTo(1);
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_360);
    assertThat(test.toString()).isEqualTo("SEK-SIOR");
  }

  @Test
  public void test_sgdSonar() {
    HolidayCalendarId cal = HolidayCalendarId.of("SGSI");
    OvernightIndex test = OvernightIndex.of("SGD-SONAR");
    assertThat(test.getName()).isEqualTo("SGD-SONAR");
    assertThat(test.getCurrency()).isEqualTo(SGD);
    assertThat(test.isActive()).isEqualTo(true);
    assertThat(test.getFixingCalendar()).isEqualTo(cal);
    assertThat(test.getPublicationDateOffset()).isEqualTo(0);
    assertThat(test.getEffectiveDateOffset()).isEqualTo(0);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("SGD-SONAR");
  }

  @Test
  public void test_sgdSora() {
    HolidayCalendarId cal = HolidayCalendarId.of("SGSI");
    OvernightIndex test = OvernightIndex.of("SGD-SORA");
    assertThat(test.getName()).isEqualTo("SGD-SORA");
    assertThat(test.getCurrency()).isEqualTo(SGD);
    assertThat(test.isActive()).isEqualTo(true);
    assertThat(test.getFixingCalendar()).isEqualTo(cal);
    assertThat(test.getPublicationDateOffset()).isEqualTo(1);
    assertThat(test.getEffectiveDateOffset()).isEqualTo(0);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("SGD-SORA");
  }

  @Test
  public void test_zarSabor() {
    OvernightIndex test = OvernightIndex.of("ZAR-SABOR");
    assertThat(test.getName()).isEqualTo("ZAR-SABOR");
    assertThat(test.getCurrency()).isEqualTo(ZAR);
    assertThat(test.isActive()).isEqualTo(true);
    assertThat(test.getFixingCalendar()).isEqualTo(ZAJO);
    assertThat(test.getPublicationDateOffset()).isEqualTo(0);
    assertThat(test.getEffectiveDateOffset()).isEqualTo(0);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getDefaultFixedLegDayCount()).isEqualTo(ACT_365F);
    assertThat(test.toString()).isEqualTo("ZAR-SABOR");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_alternateNames() {
    assertThat(OvernightIndex.of("JPY-TONA")).isEqualTo(OvernightIndices.JPY_TONAR);
    assertThat(OvernightIndex.of("USD-FED-FUNDS")).isEqualTo(OvernightIndices.USD_FED_FUND);
    assertThat(OvernightIndex.of("USD-FEDFUNDS")).isEqualTo(OvernightIndices.USD_FED_FUND);
    assertThat(OvernightIndex.of("USD-FEDFUND")).isEqualTo(OvernightIndices.USD_FED_FUND);
  }

  //-------------------------------------------------------------------------
  public static Object[][] data_name() {
    return new Object[][] {
        {OvernightIndices.GBP_SONIA, "GBP-SONIA"},
        {OvernightIndices.CHF_SARON, "CHF-SARON"},
        {OvernightIndices.EUR_EONIA, "EUR-EONIA"},
        {OvernightIndices.JPY_TONAR, "JPY-TONAR"},
        {OvernightIndices.USD_FED_FUND, "USD-FED-FUND"},
        {OvernightIndices.AUD_AONIA, "AUD-AONIA"},
        {OvernightIndices.BRL_CDI, "BRL-CDI"},
        {OvernightIndices.DKK_TNR, "DKK-TNR"},
    };
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_name(OvernightIndex convention, String name) {
    assertThat(convention.getName()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_toString(OvernightIndex convention, String name) {
    assertThat(convention.toString()).isEqualTo(name);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_of_lookup(OvernightIndex convention, String name) {
    assertThat(OvernightIndex.of(name)).isEqualTo(convention);
  }

  @ParameterizedTest
  @MethodSource("data_name")
  public void test_extendedEnum(OvernightIndex convention, String name) {
    ImmutableMap<String, OvernightIndex> map = OvernightIndex.extendedEnum().lookupAll();
    assertThat(map.get(name)).isEqualTo(convention);
  }

  @Test
  public void test_of_lookup_notFound() {
    assertThatIllegalArgumentException().isThrownBy(() -> OvernightIndex.of("Rubbish"));
  }

  @Test
  public void test_of_lookup_null() {
    assertThatIllegalArgumentException().isThrownBy(() -> OvernightIndex.of(null));
  }

  //-------------------------------------------------------------------------
  @Test
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
    assertThat(a.equals(b)).isEqualTo(false);
  }

  //-------------------------------------------------------------------------
  @Test
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

  @Test
  public void test_jodaConvert() {
    assertJodaConvert(OvernightIndex.class, OvernightIndices.GBP_SONIA);
  }

  @Test
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
