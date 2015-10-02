/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation.data;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class InterpolationBoundedValuesTest {
  private static final int LOWER_BOUND_INDEX = 2;
  private static final double LOWER_BOUND_KEY = 3;
  private static final double HIGHER_BOUND_KEY = 4;
  private static final double LOWER_BOUND_VALUE = 56;
  private static final double HIGHER_BOUND_VALUE = 78;
  private static final InterpolationBoundedValues VALUES = new InterpolationBoundedValues(LOWER_BOUND_INDEX, LOWER_BOUND_KEY, LOWER_BOUND_VALUE, HIGHER_BOUND_KEY, HIGHER_BOUND_VALUE);

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNegativeLowerBoundIndex() {
    new InterpolationBoundedValues(-LOWER_BOUND_INDEX, LOWER_BOUND_KEY, LOWER_BOUND_VALUE, HIGHER_BOUND_KEY, HIGHER_BOUND_VALUE);
  }

  @Test
  public void testGetters() {
    assertEquals(VALUES.getLowerBoundIndex(), LOWER_BOUND_INDEX);
    assertEquals(VALUES.getLowerBoundKey(), LOWER_BOUND_KEY, 0);
    assertEquals(VALUES.getLowerBoundValue(), LOWER_BOUND_VALUE, 0);
    assertEquals(VALUES.getHigherBoundKey(), HIGHER_BOUND_KEY, 0);
    assertEquals(VALUES.getHigherBoundValue(), HIGHER_BOUND_VALUE, 0);
  }
}
