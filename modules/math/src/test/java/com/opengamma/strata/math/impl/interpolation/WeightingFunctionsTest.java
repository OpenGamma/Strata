/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static com.opengamma.strata.collect.TestHelper.coverPrivateConstructor;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class WeightingFunctionsTest {

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testBadName() {
    WeightingFunction.of("Random");
  }

  public void test() {
    assertEquals(WeightingFunction.of("Linear"), WeightingFunctions.LINEAR);
    assertEquals(WeightingFunction.of("Sine"), WeightingFunctions.SINE);
  }

  public void coverage() {
    coverPrivateConstructor(WeightingFunctions.class);
  }

}
