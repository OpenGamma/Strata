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

import org.apache.commons.math3.random.Well44497b;
import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class IncompleteBetaFunctionTest {

  private static final Well44497b RANDOM = new Well44497b(0L);
  private static final double EPS = 1e-9;
  private static final double A = 0.4;
  private static final double B = 0.2;
  private static final int MAX_ITER = 10000;
  private static final Function<Double, Double> BETA = new IncompleteBetaFunction(A, B);

  @Test
  public void testNegativeA1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new IncompleteBetaFunction(-A, B));
  }

  @Test
  public void testNegativeA2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new IncompleteBetaFunction(-A, B, EPS, MAX_ITER));
  }

  @Test
  public void testNegativeB1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new IncompleteBetaFunction(A, -B));
  }

  @Test
  public void testNegativeB2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new IncompleteBetaFunction(A, -B, EPS, MAX_ITER));
  }

  @Test
  public void testNegativeEps() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new IncompleteBetaFunction(A, B, -EPS, MAX_ITER));
  }

  @Test
  public void testNegativeIter() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new IncompleteBetaFunction(A, B, EPS, -MAX_ITER));
  }

  @Test
  public void testLow() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BETA.apply(-0.3));
  }

  @Test
  public void testHigh() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BETA.apply(1.5));
  }

  @Test
  public void test() {
    final double a = RANDOM.nextDouble();
    final double b = RANDOM.nextDouble();
    final double x = RANDOM.nextDouble();
    final Function<Double, Double> f1 = new IncompleteBetaFunction(a, b);
    final Function<Double, Double> f2 = new IncompleteBetaFunction(b, a);
    assertThat(f1.apply(0.)).isCloseTo(0, offset(EPS));
    assertThat(f1.apply(1.)).isCloseTo(1, offset(EPS));
    assertThat(f1.apply(x)).isCloseTo(1 - f2.apply(1 - x), offset(EPS));
  }
}
