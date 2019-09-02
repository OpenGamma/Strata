/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function.special;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

import org.apache.commons.math3.random.Well44497b;
import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class GammaFunctionTest {

  private static final Well44497b RANDOM = new Well44497b(0L);
  private static final DoubleUnaryOperator GAMMA = new GammaFunction();
  private static final Function<Double, Double> LN_GAMMA = new NaturalLogGammaFunction();
  private static final double EPS = 1e-9;

  @Test
  public void test() {
    final double x = RANDOM.nextDouble();
    assertThat(Math.log(GAMMA.applyAsDouble(x))).isCloseTo(LN_GAMMA.apply(x), offset(EPS));
  }
}
