/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class SineWeightingFunctionTest extends WeightingFunctionTestCase {

  @Override
  protected WeightingFunction getInstance() {
    return SineWeightingFunction.INSTANCE;
  }

  public void testName() {
    assertEquals(getInstance().getName(), "Sine");
  }

}
