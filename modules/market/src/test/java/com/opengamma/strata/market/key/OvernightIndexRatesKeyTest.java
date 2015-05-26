/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.key;

import static com.opengamma.strata.basics.index.OvernightIndices.CHF_TOIS;
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.market.curve.OvernightIndexRates;
import com.opengamma.strata.market.key.OvernightIndexRatesKey.Meta;

/**
 * Test {@link OvernightIndexRatesKey}.
 */
@Test
public class OvernightIndexRatesKeyTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    OvernightIndexRatesKey test = OvernightIndexRatesKey.of(GBP_SONIA);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getMarketDataType(), OvernightIndexRates.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    OvernightIndexRatesKey test = OvernightIndexRatesKey.of(GBP_SONIA);
    coverImmutableBean(test);
    OvernightIndexRatesKey test2 = OvernightIndexRatesKey.of(CHF_TOIS);
    coverBeanEquals(test, test2);
  }

  public void coverage_builder() {
    Meta meta = OvernightIndexRatesKey.meta();
    OvernightIndexRatesKey test1 = meta.builder().setString(meta.index(), "GBP-SONIA").build();
    OvernightIndexRatesKey test2 = meta.builder().setString(meta.index().name(), "CHF-TOIS").build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    OvernightIndexRatesKey test = OvernightIndexRatesKey.of(GBP_SONIA);
    assertSerialization(test);
  }

}
