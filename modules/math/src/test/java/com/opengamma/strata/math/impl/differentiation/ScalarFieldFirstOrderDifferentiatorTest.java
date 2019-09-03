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

import com.opengamma.strata.collect.array.DoubleArray;

/**
 * Test.
 */
public class ScalarFieldFirstOrderDifferentiatorTest {

  private static final Function<DoubleArray, Double> F = new Function<DoubleArray, Double>() {

    @Override
    public Double apply(final DoubleArray x) {
      final double x1 = x.get(0);
      final double x2 = x.get(1);
      return x1 * x1 + 2 * x2 * x2 - x1 * x2 + x1 * Math.cos(x2) - x2 * Math.sin(x1);
    }
  };

  private static final Function<DoubleArray, Boolean> DOMAIN = new Function<DoubleArray, Boolean>() {

    @Override
    public Boolean apply(final DoubleArray x) {
      final double x1 = x.get(0);
      return x1 >= 0.0 && x1 <= Math.PI;
    }
  };

  private static final Function<DoubleArray, DoubleArray> G = new Function<DoubleArray, DoubleArray>() {

    @Override
    public DoubleArray apply(final DoubleArray x) {
      double x1 = x.get(0);
      double x2 = x.get(1);
      return DoubleArray.of(
          2 * x1 - x2 + Math.cos(x2) - x2 * Math.cos(x1),
          4 * x2 - x1 - x1 * Math.sin(x2) - Math.sin(x1));
    }

  };
  private static final double EPS = 1e-4;
  private static final ScalarFieldFirstOrderDifferentiator FORWARD =
      new ScalarFieldFirstOrderDifferentiator(FiniteDifferenceType.FORWARD, EPS);
  private static final ScalarFieldFirstOrderDifferentiator CENTRAL =
      new ScalarFieldFirstOrderDifferentiator(FiniteDifferenceType.CENTRAL, EPS);
  private static final ScalarFieldFirstOrderDifferentiator BACKWARD =
      new ScalarFieldFirstOrderDifferentiator(FiniteDifferenceType.BACKWARD, EPS);

  @Test
  public void testNullDifferenceType() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new ScalarFirstOrderDifferentiator(null));
  }

  @Test
  public void testNullFunction() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CENTRAL.differentiate((Function<DoubleArray, Double>) null));
  }

  @Test
  public void test() {
    final DoubleArray x = DoubleArray.of(.2245, -1.2344);
    final DoubleArray anGrad = G.apply(x);
    final DoubleArray fdFwdGrad = FORWARD.differentiate(F).apply(x);
    final DoubleArray fdCentGrad = CENTRAL.differentiate(F).apply(x);
    final DoubleArray fdBackGrad = BACKWARD.differentiate(F).apply(x);

    for (int i = 0; i < 2; i++) {
      assertThat(fdFwdGrad.get(i)).isCloseTo(anGrad.get(i), offset(10 * EPS));
      assertThat(fdCentGrad.get(i)).isCloseTo(anGrad.get(i), offset(EPS * EPS));
      assertThat(fdBackGrad.get(i)).isCloseTo(anGrad.get(i), offset(10 * EPS));
    }
  }

  @Test
  public void domainTest() {
    final DoubleArray[] x = new DoubleArray[3];
    x[0] = DoubleArray.of(0.2245, -1.2344);
    x[1] = DoubleArray.of(0.0, 12.6);
    x[2] = DoubleArray.of(Math.PI, 0.0);

    final Function<DoubleArray, DoubleArray> fdGradFunc = CENTRAL.differentiate(F, DOMAIN);

    for (int k = 0; k < 3; k++) {
      final DoubleArray fdRes = fdGradFunc.apply(x[k]);
      final DoubleArray alRes = G.apply(x[k]);
      for (int i = 0; i < 2; i++) {
        assertThat(fdRes.get(i)).isCloseTo(alRes.get(i), offset(1e-7));
      }
    }
  }

}
