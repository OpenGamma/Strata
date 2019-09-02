/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function.special;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class NaturalLogGammaFunctionTest {
  private static final Function<Double, Double> LN_GAMMA = new NaturalLogGammaFunction();
  private static final double EPS = 1e-9;

  @Test
  public void testNegativeNumber() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LN_GAMMA.apply(-0.1));
  }

  @Test
  public void testRecurrence() {
    double z = 12;
    double gamma = getGammaFunction(LN_GAMMA.apply(z));
    assertThat(getGammaFunction(LN_GAMMA.apply(z + 1))).isCloseTo(z * gamma, offset(gamma * EPS));
    z = 11.34;
    gamma = getGammaFunction(LN_GAMMA.apply(z));
    assertThat(getGammaFunction(LN_GAMMA.apply(z + 1))).isCloseTo(z * gamma, offset(gamma * EPS));
  }

  @Test
  public void testIntegerArgument() {
    final int x = 5;
    final double factorial = 24;
    assertThat(getGammaFunction(LN_GAMMA.apply(Double.valueOf(x)))).isCloseTo(factorial, offset(EPS));
  }

  private double getGammaFunction(final double x) {
    return Math.exp(x);
  }
}
