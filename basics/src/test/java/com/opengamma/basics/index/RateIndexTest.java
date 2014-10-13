/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.basics.index;

import static com.opengamma.collect.TestHelper.assertThrows;
import static com.opengamma.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

/**
 * Test {@link RateIndex}.
 */
@Test
public class RateIndexTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "name")
  static Object[][] data_name() {
      return new Object[][] {
          {RateIndices.GBP_LIBOR_6M, "GBP-LIBOR-6M"},
          {RateIndices.CHF_LIBOR_6M, "CHF-LIBOR-6M"},
          {RateIndices.EUR_LIBOR_6M, "EUR-LIBOR-6M"},
          {RateIndices.JPY_LIBOR_6M, "JPY-LIBOR-6M"},
          {RateIndices.USD_LIBOR_6M, "USD-LIBOR-6M"},
          {RateIndices.EURIBOR_1M, "EURIBOR-1M"},
          {RateIndices.JPY_TIBOR_JAPAN_2M, "JPY-TIBOR-JAPAN-2M"},
          {RateIndices.JPY_TIBOR_EUROYEN_6M, "JPY-TIBOR-EUROYEN-6M"},
      };
  }

  @Test(dataProvider = "name")
  public void test_name(RateIndex convention, String name) {
    assertEquals(convention.getName(), name);
  }

  @Test(dataProvider = "name")
  public void test_toString(RateIndex convention, String name) {
    assertEquals(convention.toString(), name);
  }

  @Test(dataProvider = "name")
  public void test_of_lookup(RateIndex convention, String name) {
    assertEquals(RateIndex.of(name), convention);
  }

  @Test(dataProvider = "name")
  public void test_extendedEnum(RateIndex convention, String name) {
    ImmutableMap<String, RateIndex> map = RateIndex.extendedEnum().lookupAll();
    assertEquals(map.get(name), convention);
  }

  public void test_of_lookup_notFound() {
    assertThrows(() -> RateIndex.of("Rubbish"), IllegalArgumentException.class);
  }

  public void test_of_lookup_null() {
    assertThrows(() -> RateIndex.of(null), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverPrivateConstructor(RateIndices.class);
  }

}
