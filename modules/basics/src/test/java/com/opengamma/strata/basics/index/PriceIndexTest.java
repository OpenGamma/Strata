/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.basics.index;

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

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
    return new Object[][] {
        {PriceIndices.UK_HICP, "UK-HICP"},
        {PriceIndices.UK_RPI, "UK-RPI"},
        {PriceIndices.UK_RPIX, "UK-RPIX"},
        {PriceIndices.SWF_CPI, "SWF-CPI"},
        {PriceIndices.EUR_AI_CPI, "EUR-AI-CPI"},
        {PriceIndices.JPY_CPI_EXF, "JPY-CPI-EXF"},
        {PriceIndices.USA_CPI_U, "USA-CPI-U"},
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
    coverImmutableBean((ImmutableBean) PriceIndices.USA_CPI_U);
    coverBeanEquals((ImmutableBean) PriceIndices.USA_CPI_U, ImmutablePriceIndex.builder()
        .name("Test")
        .region(Country.AR)
        .currency(Currency.ARS)
        .publicationFrequency(Frequency.P6M)
        .build());
  }

  public void test_jodaConvert() {
    assertJodaConvert(PriceIndex.class, PriceIndices.USA_CPI_U);
  }

  public void test_serialization() {
    assertSerialization(PriceIndices.USA_CPI_U);
  }

}
