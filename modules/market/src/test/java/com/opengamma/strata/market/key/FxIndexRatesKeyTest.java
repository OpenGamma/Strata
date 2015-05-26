/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.key;

import static com.opengamma.strata.basics.index.FxIndices.ECB_EUR_GBP;
import static com.opengamma.strata.basics.index.FxIndices.WM_GBP_USD;
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
    FxIndexRatesKey test = FxIndexRatesKey.of(ECB_EUR_GBP);
    assertEquals(test.getIndex(), ECB_EUR_GBP);
    assertEquals(test.getMarketDataType(), FxIndexRates.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FxIndexRatesKey test = FxIndexRatesKey.of(ECB_EUR_GBP);
    coverImmutableBean(test);
    FxIndexRatesKey test2 = FxIndexRatesKey.of(WM_GBP_USD);
    coverBeanEquals(test, test2);
  }

  public void coverage_builder() {
    Meta meta = FxIndexRatesKey.meta();
    FxIndexRatesKey test1 = meta.builder().setString(meta.index(), "ECB-EUR-GBP").build();
    FxIndexRatesKey test2 = meta.builder().setString(meta.index().name(), "WM-GBP-USD").build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    FxIndexRatesKey test = FxIndexRatesKey.of(ECB_EUR_GBP);
    assertSerialization(test);
  }

}
