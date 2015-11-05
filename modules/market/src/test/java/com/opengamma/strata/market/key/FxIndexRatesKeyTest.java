/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.key;

import static com.opengamma.strata.basics.index.FxIndices.EUR_GBP_ECB;
import static com.opengamma.strata.basics.index.FxIndices.GBP_USD_WM;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.market.key.FxIndexRatesKey.Meta;
import com.opengamma.strata.market.value.FxIndexRates;

/**
 * Test {@link FxIndexRatesKey}.
 */
@Test
public class FxIndexRatesKeyTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    FxIndexRatesKey test = FxIndexRatesKey.of(EUR_GBP_ECB);
    assertEquals(test.getIndex(), EUR_GBP_ECB);
    assertEquals(test.getMarketDataType(), FxIndexRates.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxIndexRatesKey test = FxIndexRatesKey.of(EUR_GBP_ECB);
    coverImmutableBean(test);
    FxIndexRatesKey test2 = FxIndexRatesKey.of(GBP_USD_WM);
    coverBeanEquals(test, test2);
  }

  public void coverage_builder() {
    Meta meta = FxIndexRatesKey.meta();
    FxIndexRatesKey test1 = meta.builder().setString(meta.index(), "EUR/GBP-ECB").build();
    FxIndexRatesKey test2 = meta.builder().setString(meta.index().name(), "GBP/USD-WM").build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    FxIndexRatesKey test = FxIndexRatesKey.of(EUR_GBP_ECB);
    assertSerialization(test);
  }

}
