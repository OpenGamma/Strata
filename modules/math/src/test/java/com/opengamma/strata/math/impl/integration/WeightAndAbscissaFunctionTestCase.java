/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.offset;

import org.junit.jupiter.api.Test;

/**
 * Abstract test.
 */
public abstract class WeightAndAbscissaFunctionTestCase {
  private static final double EPS = 1e-3;

  protected abstract QuadratureWeightAndAbscissaFunction getFunction();

  @Test
  public void testNullFunction() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> getFunction().generate(-1));
  }

  protected void assertResults(final GaussianQuadratureData f, final double[] x, final double[] w) {
    final double[] x1 = f.getAbscissas();
    final double[] w1 = f.getWeights();
    for (int i = 0; i < x.length; i++) {
      assertThat(x1[i]).isCloseTo(x[i], offset(EPS));
      assertThat(w1[i]).isCloseTo(w[i], offset(EPS));
    }
  }
}
