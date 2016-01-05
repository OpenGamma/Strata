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

import com.opengamma.strata.market.curve.Curve;

/**
 * Test {@link OvernightIndexCurveKey}.
 */
@Test
public class OvernightIndexCurveKeyTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    OvernightIndexCurveKey test = OvernightIndexCurveKey.of(GBP_SONIA);
    assertEquals(test.getIndex(), GBP_SONIA);
    assertEquals(test.getMarketDataType(), Curve.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    OvernightIndexCurveKey test = OvernightIndexCurveKey.of(GBP_SONIA);
    coverImmutableBean(test);
    OvernightIndexCurveKey test2 = OvernightIndexCurveKey.of(CHF_TOIS);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    OvernightIndexCurveKey test = OvernightIndexCurveKey.of(GBP_SONIA);
    assertSerialization(test);
  }

}
