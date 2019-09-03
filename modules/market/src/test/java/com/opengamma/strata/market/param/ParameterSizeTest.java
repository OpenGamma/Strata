/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.market.curve.CurveName;

/**
 * Test {@link ParameterSize}.
 */
public class ParameterSizeTest {

  private static final CurveName CURVE_NAME = CurveName.of("Test");

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    ParameterSize test = ParameterSize.of(CURVE_NAME, 3);
    assertThat(test.getName()).isEqualTo(CURVE_NAME);
    assertThat(test.getParameterCount()).isEqualTo(3);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    ParameterSize test = ParameterSize.of(CURVE_NAME, 3);
    coverImmutableBean(test);
    ParameterSize test2 = ParameterSize.of(CurveName.of("Foo"), 4);
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    ParameterSize test = ParameterSize.of(CURVE_NAME, 3);
    assertSerialization(test);
  }

}
