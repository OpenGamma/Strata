/*
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public abstract class WeightingFunctionTestCase {

  static final double[] STRIKES = new double[] {1, 1.1, 1.2, 1.3, 1.4, 1.5};
  static final double STRIKE = 1.345;
  static final int INDEX = 3;
  static final double EPS = 1e-15;

  protected abstract WeightingFunction getInstance();

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullStrikes2() {
    getInstance().getWeight(null, INDEX, STRIKE);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeIndex() {
    getInstance().getWeight(STRIKES, -INDEX, STRIKE);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testHighIndex() {
    getInstance().getWeight(STRIKES, STRIKES.length, STRIKE);
  }

}
