/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

/**
 * Abstract test.
 */
public abstract class MinimumBracketerTestCase {
  private static final Function<Double, Double> F = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return null;
    }

  };

  protected abstract MinimumBracketer getBracketer();

  @Test
  public void testNullFunction() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> getBracketer().checkInputs(null, 1., 2.));
  }

  @Test
  public void testInputs() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> getBracketer().checkInputs(F, 1., 1.));
  }
}
