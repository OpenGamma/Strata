/*
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
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
public class LinearWeightingFunctionTest extends WeightingFunctionTestCase {

  @Override
  protected WeightingFunction getInstance() {
    return LinearWeightingFunction.INSTANCE;
  }

  public void testWeighting() {
    assertEquals(getInstance().getWeight(STRIKES, INDEX, STRIKE), 0.55, EPS);
    assertEquals(getInstance().getWeight(STRIKES, INDEX, STRIKES[3]), 1, EPS);
  }

  public void testName() {
    assertEquals(getInstance().getName(), "Linear");
  }

}
