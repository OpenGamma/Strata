/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.curve;

import static org.testng.Assert.assertEquals;

import com.opengamma.strata.market.curve.CurveName;

import org.testng.annotations.Test;

/**
 * Test {@link CurveName}.
 */
@Test
public class CurveNameTest {

  public void coverage() {
    CurveName test = CurveName.of("Foo");
    assertEquals(test.toString(), "Foo");
  }

}
