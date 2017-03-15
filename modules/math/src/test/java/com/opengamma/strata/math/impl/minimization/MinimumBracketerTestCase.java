/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import java.util.function.Function;

import org.testng.annotations.Test;

/**
 * Abstract test.
 */
@Test
public abstract class MinimumBracketerTestCase {
  private static final Function<Double, Double> F = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
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
