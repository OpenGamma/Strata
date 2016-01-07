/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.key;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.market.curve.CurveGroupName;
import com.opengamma.strata.market.curve.CurveInputs;
import com.opengamma.strata.market.curve.CurveName;

/**
 * Test {@link CurveInputsKey}.
 */
@Test
public class CurveInputsKeyTest {

  //-------------------------------------------------------------------------
  public void test_of() {
    CurveInputsKey test = CurveInputsKey.of(CurveGroupName.of("foo"), CurveName.of("bar"));
    assertEquals(test.getCurveGroupName(), CurveGroupName.of("foo"));
    assertEquals(test.getCurveName(), CurveName.of("bar"));
    assertEquals(test.getMarketDataType(), CurveInputs.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveInputsKey test = CurveInputsKey.of(CurveGroupName.of("foo"), CurveName.of("bar"));
    coverImmutableBean(test);
    CurveInputsKey test2 = CurveInputsKey.of(CurveGroupName.of("bar"), CurveName.of("foo"));
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveInputsKey test = CurveInputsKey.of(CurveGroupName.of("foo"), CurveName.of("bar"));
    assertSerialization(test);
  }

}
