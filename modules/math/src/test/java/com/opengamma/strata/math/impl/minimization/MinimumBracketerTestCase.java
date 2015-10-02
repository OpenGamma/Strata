/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import org.testng.annotations.Test;

import com.opengamma.strata.math.impl.function.Function1D;

/**
 * Abstract test.
 */
@Test
public abstract class MinimumBracketerTestCase {
  private static final Function1D<Double, Double> F = new Function1D<Double, Double>() {

    @Override
    public Double evaluate(final Double x) {
      return null;
    }

  };

  protected abstract MinimumBracketer getBracketer();

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullFunction() {
    getBracketer().checkInputs(null, 1., 2.);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testInputs() {
    getBracketer().checkInputs(F, 1., 1.);
  }
}
