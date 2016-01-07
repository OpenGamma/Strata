/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.key;

import static com.opengamma.strata.basics.index.PriceIndices.EUR_AI_CPI;
import static com.opengamma.strata.basics.index.PriceIndices.UK_RPI;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.market.curve.Curve;

/**
 * Test {@link PriceIndexCurveKey}.
 */
@Test
public class PriceIndexCurveKeyTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    PriceIndexCurveKey test = PriceIndexCurveKey.of(UK_RPI);
    assertEquals(test.getIndex(), UK_RPI);
    assertEquals(test.getMarketDataType(), Curve.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    PriceIndexCurveKey test = PriceIndexCurveKey.of(UK_RPI);
    coverImmutableBean(test);
    PriceIndexCurveKey test2 = PriceIndexCurveKey.of(EUR_AI_CPI);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    PriceIndexCurveKey test = PriceIndexCurveKey.of(UK_RPI);
    assertSerialization(test);
  }

}
