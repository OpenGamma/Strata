/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link CurveName}.
 */
@Test
public class CurveNameTest {

  public void test_of() {
    CurveName test = CurveName.of("Foo");
    assertEquals(test.getName(), "Foo");
    assertEquals(test.getMarketDataType(), Curve.class);
    assertEquals(test.toString(), "Foo");
    assertEquals(test.compareTo(CurveName.of("Goo")) < 0, true);
  }

}
