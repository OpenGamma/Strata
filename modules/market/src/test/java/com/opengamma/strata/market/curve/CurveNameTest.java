/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link CurveName}.
 */
public class CurveNameTest {

  @Test
  public void test_of() {
    CurveName test = CurveName.of("Foo");
    assertThat(test.getName()).isEqualTo("Foo");
    assertThat(test.getMarketDataType()).isEqualTo(Curve.class);
    assertThat(test.toString()).isEqualTo("Foo");
    assertThat(test.compareTo(CurveName.of("Goo")) < 0).isTrue();
  }

}
