/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function.special;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.math.impl.function.DoubleFunction1D;

/**
 * Test.
 */
public class JacobiPolynomialFunctionTest {
  private static final double ALPHA = 0.12;
  private static final double BETA = 0.34;
  private static final DoubleFunction1D P0 = x -> 1d;
  private static final DoubleFunction1D P1 = x -> 0.5 * (2 * (ALPHA + 1) + (ALPHA + BETA + 2) * (x - 1));
  private static final DoubleFunction1D P2 =
      x -> 0.125 * (4 * (ALPHA + 1) * (ALPHA + 2) + 4 * (ALPHA + BETA + 3) *
          (ALPHA + 2) * (x - 1) + (ALPHA + BETA + 3) * (ALPHA + BETA + 4) * (x - 1) * (x - 1));
  private static final DoubleFunction1D[] P = new DoubleFunction1D[] {P0, P1, P2};
  private static final JacobiPolynomialFunction JACOBI = new JacobiPolynomialFunction();
  private static final double EPS = 1e-9;

  @Test
  public void testNoAlphaBeta() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> JACOBI.getPolynomials(3));
  }

  @Test
  public void testNegativeN() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> JACOBI.getPolynomials(-3, ALPHA, BETA));
  }

  @Test
  public void testGetPolynomials() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> JACOBI.getPolynomialsAndFirstDerivative(3));
  }

  @Test
  public void test() {
    DoubleFunction1D[] p = JACOBI.getPolynomials(0, ALPHA, BETA);
    assertThat(p.length).isEqualTo(1);
    final double x = 1.23;
    assertThat(p[0].applyAsDouble(x)).isCloseTo(1, offset(EPS));
    for (int i = 0; i <= 2; i++) {
      p = JACOBI.getPolynomials(i, ALPHA, BETA);
      for (int j = 0; j <= i; j++) {
        assertThat(P[j].applyAsDouble(x)).isCloseTo(p[j].applyAsDouble(x), offset(EPS));
      }
    }
  }
}
