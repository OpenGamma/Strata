/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link CurveParameterSize}.
 */
@Test
public class CurveParameterSizeTest {

  private static final CurveName CURVE_NAME = CurveName.of("Test");

  //-------------------------------------------------------------------------
  public void test_of() {
    CurveParameterSize test = CurveParameterSize.of(CURVE_NAME, 3);
    assertEquals(test.getName(), CURVE_NAME);
    assertEquals(test.getParameterCount(), 3);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    CurveParameterSize test = CurveParameterSize.of(CURVE_NAME, 3);
    coverImmutableBean(test);
    CurveParameterSize test2 = CurveParameterSize.of(CurveName.of("Foo"), 4);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    CurveParameterSize test = CurveParameterSize.of(CURVE_NAME, 3);
    assertSerialization(test);
  }

}
