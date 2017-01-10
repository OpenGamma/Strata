/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.market.curve.CurveName;

/**
 * Test {@link ParameterSize}.
 */
@Test
public class ParameterSizeTest {

  private static final CurveName CURVE_NAME = CurveName.of("Test");

  //-------------------------------------------------------------------------
  public void test_of() {
    ParameterSize test = ParameterSize.of(CURVE_NAME, 3);
    assertEquals(test.getName(), CURVE_NAME);
    assertEquals(test.getParameterCount(), 3);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ParameterSize test = ParameterSize.of(CURVE_NAME, 3);
    coverImmutableBean(test);
    ParameterSize test2 = ParameterSize.of(CurveName.of("Foo"), 4);
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    ParameterSize test = ParameterSize.of(CURVE_NAME, 3);
    assertSerialization(test);
  }

}
