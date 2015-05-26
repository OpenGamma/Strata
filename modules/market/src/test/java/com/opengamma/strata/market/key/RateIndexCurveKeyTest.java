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

import com.opengamma.analytics.financial.model.interestrate.curve.YieldCurve;

/**
 * Test {@link RateIndexCurveKey}.
 */
@Test
public class RateIndexCurveKeyTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    RateIndexCurveKey test = RateIndexCurveKey.of(GBP_LIBOR_3M);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getMarketDataType(), YieldCurve.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    RateIndexCurveKey test = RateIndexCurveKey.of(GBP_LIBOR_3M);
    coverImmutableBean(test);
    RateIndexCurveKey test2 = RateIndexCurveKey.of(CHF_LIBOR_12M);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    RateIndexCurveKey test = RateIndexCurveKey.of(GBP_LIBOR_3M);
    assertSerialization(test);
  }

}
