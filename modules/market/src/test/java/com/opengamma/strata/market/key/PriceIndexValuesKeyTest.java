/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.key;

import static com.opengamma.strata.basics.index.PriceIndices.CH_CPI;
import static com.opengamma.strata.basics.index.PriceIndices.GB_RPI;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.market.curve.PriceIndexValues;
import com.opengamma.strata.market.key.PriceIndexValuesKey.Meta;

/**
 * Test {@link PriceIndexValuesKey}.
 */
@Test
public class PriceIndexValuesKeyTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    PriceIndexValuesKey test = PriceIndexValuesKey.of(GB_RPI);
    assertEquals(test.getIndex(), GB_RPI);
    assertEquals(test.getMarketDataType(), PriceIndexValues.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    PriceIndexValuesKey test = PriceIndexValuesKey.of(GB_RPI);
    coverImmutableBean(test);
    PriceIndexValuesKey test2 = PriceIndexValuesKey.of(CH_CPI);
    coverBeanEquals(test, test2);
  }

  public void coverage_builder() {
    Meta meta = PriceIndexValuesKey.meta();
    PriceIndexValuesKey test1 = meta.builder().setString(meta.index(), "GB-RPI").build();
    PriceIndexValuesKey test2 = meta.builder().setString(meta.index().name(), "CH-CPI").build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    PriceIndexValuesKey test = PriceIndexValuesKey.of(GB_RPI);
    assertSerialization(test);
  }

}
