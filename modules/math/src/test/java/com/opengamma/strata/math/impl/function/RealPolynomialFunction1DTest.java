/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.apache.commons.math3.random.Well44497b;
import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class RealPolynomialFunction1DTest {

  private static final Well44497b RANDOM = new Well44497b(0L);
  private static final double[] C = new double[] {3.4, 5.6, 1., -4.};
  private static final DoubleFunction1D F = new RealPolynomialFunction1D(C);
  private static final double EPS = 1e-12;

  @Test
  public void testNullCoefficients() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RealPolynomialFunction1D(null));
  }

  @Test
  public void testEmptyCoefficients() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new RealPolynomialFunction1D(new double[0]));
  }

  @Test
  public void testEvaluate() {
    final double x = RANDOM.nextDouble();
    assertThat(C[3] * Math.pow(x, 3) + C[2] * Math.pow(x, 2) + C[1] * x + C[0]).isCloseTo(F.applyAsDouble(x), offset(EPS));
  }

  @Test
  public void testDerivative() {
    final double x = RANDOM.nextDouble();
    assertThat(3 * C[3] * Math.pow(x, 2) + 2 * C[2] * x + C[1]).isCloseTo(F.derivative().applyAsDouble(x), offset(EPS));
  }
}
