/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.differentiation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class ScalarFirstOrderDifferentiatorTest {
  private static final Function<Double, Double> F = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return 3 * x * x + 4 * x - Math.sin(x);
    }
  };

  private static final Function<Double, Boolean> DOMAIN = new Function<Double, Boolean>() {
    @Override
    public Boolean apply(final Double x) {
      return x >= 0 && x <= Math.PI;
    }
  };

  private static final Function<Double, Double> DX_ANALYTIC = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return 6 * x + 4 - Math.cos(x);
    }

  };
  private static final double EPS = 1e-5;
  private static final ScalarFirstOrderDifferentiator FORWARD =
      new ScalarFirstOrderDifferentiator(FiniteDifferenceType.FORWARD, EPS);
  private static final ScalarFirstOrderDifferentiator CENTRAL =
      new ScalarFirstOrderDifferentiator(FiniteDifferenceType.CENTRAL, EPS);
  private static final ScalarFirstOrderDifferentiator BACKWARD =
      new ScalarFirstOrderDifferentiator(FiniteDifferenceType.BACKWARD, EPS);

  @Test
  public void testNullDifferenceType() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new ScalarFirstOrderDifferentiator(null));
  }

  @Test
  public void testNullFunction() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CENTRAL.differentiate((Function<Double, Double>) null));
  }

  @Test
  public void test() {
    final double x = 0.2245;
    assertThat(FORWARD.differentiate(F).apply(x)).isCloseTo(DX_ANALYTIC.apply(x), offset(10 * EPS));
    assertThat(CENTRAL.differentiate(F).apply(x)).isCloseTo(DX_ANALYTIC.apply(x), offset(EPS * EPS)); // This is why you use central difference
    assertThat(BACKWARD.differentiate(F).apply(x)).isCloseTo(DX_ANALYTIC.apply(x), offset(10 * EPS));
  }

  @Test
  public void domainTest() {
    final double[] x = new double[] {1.2, 0, Math.PI};
    final Function<Double, Double> alFunc = CENTRAL.differentiate(F, DOMAIN);
    for (int i = 0; i < 3; i++) {
      assertThat(alFunc.apply(x[i])).isCloseTo(DX_ANALYTIC.apply(x[i]), offset(1e-8));
    }
  }
}
