/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.market.param;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import org.testng.annotations.Test;

/**
 * Test {@link ParameterizedData}.
 */
@Test
public class ParameterizedDataTest {

  private static final ParameterizedData CURVE = new TestingParameterizedData(1d);

  public void test_withPerturbation() {
    assertSame(CURVE.withPerturbation((i, v, m) -> v), CURVE);
    assertEquals(CURVE.withPerturbation((i, v, m) -> v + 2d).getParameter(0), 3d);
  }

}
