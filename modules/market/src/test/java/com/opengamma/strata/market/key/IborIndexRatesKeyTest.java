/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.key;

import static com.opengamma.strata.basics.index.IborIndices.CHF_LIBOR_12M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.market.key.IborIndexRatesKey.Meta;
import com.opengamma.strata.market.value.IborIndexRates;

/**
 * Test {@link IborIndexRatesKey}.
 */
@Test
public class IborIndexRatesKeyTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    IborIndexRatesKey test = IborIndexRatesKey.of(GBP_LIBOR_3M);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getMarketDataType(), IborIndexRates.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborIndexRatesKey test = IborIndexRatesKey.of(GBP_LIBOR_3M);
    coverImmutableBean(test);
    IborIndexRatesKey test2 = IborIndexRatesKey.of(CHF_LIBOR_12M);
    coverBeanEquals(test, test2);
  }

  public void coverage_builder() {
    Meta meta = IborIndexRatesKey.meta();
    IborIndexRatesKey test1 = meta.builder().setString(meta.index(), "GBP-LIBOR-3M").build();
    IborIndexRatesKey test2 = meta.builder().setString(meta.index().name(), "GBP-LIBOR-6M").build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    IborIndexRatesKey test = IborIndexRatesKey.of(GBP_LIBOR_3M);
    assertSerialization(test);
  }

}
