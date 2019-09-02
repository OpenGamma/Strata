/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.function.DoubleBinaryOperator;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class LognormalFisherKurtosisFromVolatilityCalculatorTest {
  private static final DoubleBinaryOperator F = new LognormalFisherKurtosisFromVolatilityCalculator();
  private static final double SIGMA = 0.3;
  private static final double T = 0.25;

  @Test
  public void test() {
    assertThat(F.applyAsDouble(SIGMA, T)).isCloseTo(0.3719, offset(1e-4));
  }
}
