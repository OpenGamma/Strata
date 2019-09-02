/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.statistics.descriptive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class GeometricMeanCalculatorTest {
  private static final Function<double[], Double> ARITHMETIC = new MeanCalculator();
  private static final Function<double[], Double> GEOMETRIC = new GeometricMeanCalculator();
  private static final int N = 100;
  private static final double[] FLAT = new double[N];
  private static final double[] X = new double[N];
  private static final double[] LN_X = new double[N];

  static {
    for (int i = 0; i < N; i++) {
      FLAT[i] = 2;
      X[i] = Math.random();
      LN_X[i] = Math.log(X[i]);
    }
  }

  @Test
  public void testNullArray() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> GEOMETRIC.apply(null));
  }

  @Test
  public void testEmptyArray() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> GEOMETRIC.apply(new double[0]));
  }

  @Test
  public void test() {
    assertThat(GEOMETRIC.apply(FLAT)).isEqualTo(2);
    assertThat(GEOMETRIC.apply(X)).isCloseTo(Math.exp(ARITHMETIC.apply(LN_X)), offset(1e-15));
  }
}
