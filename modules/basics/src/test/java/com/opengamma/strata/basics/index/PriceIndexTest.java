/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.location.Country.GB;
import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import org.joda.beans.ImmutableBean;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.location.Country;
import com.opengamma.strata.basics.schedule.Frequency;

/**
 * Test {@link PriceIndex}.
 */
@Test
public class PriceIndexTest {

  public void test_gbpHicp() {
    PriceIndex test = PriceIndex.of("GB-HICP");
    assertEquals(test.getName(), "GB-HICP");
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getRegion(), GB);
    assertEquals(test.isActive(), true);
    assertEquals(test.getPublicationFrequency(), Frequency.P1M);
    assertEquals(test.getFloatingRateName(), FloatingRateName.of("GB-HICP"));
    assertEquals(test.toString(), "GB-HICP");
  }

  public void test_getFloatingRateName() {
    for (PriceIndex index : PriceIndex.extendedEnum().lookupAll().values()) {
      assertEquals(index.getFloatingRateName(), FloatingRateName.of(index.getName()));
    }
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {PriceIndices.GB_HICP, "GB-HICP"},
        {PriceIndices.GB_RPI, "GB-RPI"},
        {PriceIndices.GB_RPIX, "GB-RPIX"},
        {PriceIndices.CH_CPI, "CH-CPI"},
        {PriceIndices.EU_AI_CPI, "EU-AI-CPI"},
        {PriceIndices.EU_EXT_CPI, "EU-EXT-CPI"},
        {PriceIndices.JP_CPI_EXF, "JP-CPI-EXF"},
        {PriceIndices.US_CPI_U, "US-CPI-U"},
        {PriceIndices.FR_EXT_CPI, "FR-EXT-CPI"},
    };
  }

  @Test(dataProvider = "name")
  public void test_name(PriceIndex convention, String name) {
    assertEquals(convention.getName(), name);
  }

  @Test(dataProvider = "name")
  public void test_toString(PriceIndex convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(PriceIndex convention, String name) {
    assertEquals(PriceIndex.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_extendedEnum(PriceIndex convention, String name) {
    ImmutableMap<String, PriceIndex> map = PriceIndex.extendedEnum().lookupAll();
    assertEquals(map.get(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrowsIllegalArg(() -> PriceIndex.of("Rubbish"));
  }

  public void test_of_lookup_null() {
    assertThrowsIllegalArg(() -> PriceIndex.of(null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(PriceIndices.class);
    coverImmutableBean((ImmutableBean) PriceIndices.US_CPI_U);
    coverBeanEquals((ImmutableBean) PriceIndices.US_CPI_U, ImmutablePriceIndex.builder()
        .name("Test")
        .region(Country.AR)
        .currency(Currency.ARS)
        .publicationFrequency(Frequency.P6M)
        .build());
  }

  public void test_jodaConvert() {
    assertJodaConvert(PriceIndex.class, PriceIndices.US_CPI_U);
  }

  public void test_serialization() {
    assertSerialization(PriceIndices.US_CPI_U);
  }

}
