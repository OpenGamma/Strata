/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class GaussJacobiWeightAndAbscissaFunctionTest extends WeightAndAbscissaFunctionTestCase {
  private static final QuadratureWeightAndAbscissaFunction GAUSS_LEGENDRE = new GaussLegendreWeightAndAbscissaFunction();
  private static final QuadratureWeightAndAbscissaFunction GAUSS_JACOBI_GL_EQUIV = new GaussJacobiWeightAndAbscissaFunction(0, 0);
  private static final QuadratureWeightAndAbscissaFunction GAUSS_JACOBI_CHEBYSHEV_EQUIV =
      new GaussJacobiWeightAndAbscissaFunction(-0.5, -0.5);
  private static final double EPS = 1e-8;

  @Test
  public void test() {
    final int n = 12;
    final GaussianQuadratureData f1 = GAUSS_LEGENDRE.generate(n);
    final GaussianQuadratureData f2 = GAUSS_JACOBI_GL_EQUIV.generate(n);
    final GaussianQuadratureData f3 = GAUSS_JACOBI_CHEBYSHEV_EQUIV.generate(n);
    final double[] w1 = f1.getWeights();
    final double[] w2 = f2.getWeights();
    final double[] x1 = f1.getAbscissas();
    final double[] x2 = f2.getAbscissas();
    assertThat(w1.length == w2.length).isTrue();
    assertThat(x1.length == x2.length).isTrue();
    for (int i = 0; i < n; i++) {
      assertThat(w1[i]).isCloseTo(w2[i], offset(EPS));
      assertThat(x1[i]).isCloseTo(-x2[i], offset(EPS));
    }
    final double[] w3 = f3.getWeights();
    final double[] x3 = f3.getAbscissas();
    final double chebyshevWeight = Math.PI / n;
    final Function<Integer, Double> chebyshevAbscissa = new Function<Integer, Double>() {

      @Override
      public Double apply(final Integer x) {
        return -Math.cos(Math.PI * (x + 0.5) / n);
      }

    };
    for (int i = 0; i < n; i++) {
      assertThat(chebyshevWeight).isCloseTo(w3[i], offset(EPS));
      assertThat(chebyshevAbscissa.apply(i)).isCloseTo(-x3[i], offset(EPS));
    }
  }

  @Override
  protected QuadratureWeightAndAbscissaFunction getFunction() {
    return GAUSS_JACOBI_GL_EQUIV;
  }

}
