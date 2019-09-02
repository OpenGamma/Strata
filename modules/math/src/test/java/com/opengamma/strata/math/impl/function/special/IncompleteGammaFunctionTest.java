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
public class IncompleteGammaFunctionTest {
  private static final double A = 1;
  private static final Function<Double, Double> FUNCTION = new IncompleteGammaFunction(A);
  private static final double EPS = 1e-9;
  private static final int MAX_ITER = 10000;

  @Test
  public void testNegativeA1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new IncompleteGammaFunction(-A));
  }

  @Test
  public void testNegativeA2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new IncompleteGammaFunction(-A, MAX_ITER, EPS));
  }

  @Test
  public void testNegativeIter() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new IncompleteGammaFunction(A, -MAX_ITER, EPS));
  }

  @Test
  public void testNegativeEps() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new IncompleteGammaFunction(A, MAX_ITER, -EPS));
  }

  @Test
  public void testLimits() {
    assertThat(FUNCTION.apply(0.)).isCloseTo(0, offset(EPS));
    assertThat(FUNCTION.apply(100.)).isCloseTo(1, offset(EPS));
  }

  @Test
  public void test() {
    final Function<Double, Double> f = new Function<Double, Double>() {

      @Override
      public Double apply(final Double x) {
        return 1 - Math.exp(-x);
      }

    };
    final double x = 4.6;
    assertThat(f.apply(x)).isCloseTo(FUNCTION.apply(x), offset(EPS));
  }
}
