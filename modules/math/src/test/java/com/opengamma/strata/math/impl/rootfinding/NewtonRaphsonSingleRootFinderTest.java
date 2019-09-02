/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.math.impl.function.DoubleFunction1D;

/**
 * Test.
 */
public class NewtonRaphsonSingleRootFinderTest {
  private static final DoubleFunction1D F1 = new DoubleFunction1D() {

    @Override
    public double applyAsDouble(double x) {
      return x * x * x - 6 * x * x + 11 * x - 106;
    }

    @Override
    public DoubleFunction1D derivative() {
      return x -> 3 * x * x - 12 * x + 11;
    }

  };
  private static final Function<Double, Double> F2 = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return x * x * x - 6 * x * x + 11 * x - 106;
    }

  };
  private static final DoubleFunction1D DF1 = x -> 3 * x * x - 12 * x + 11;
  private static final Function<Double, Double> DF2 = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return 3 * x * x - 12 * x + 11;
    }

  };
  private static final NewtonRaphsonSingleRootFinder ROOT_FINDER = new NewtonRaphsonSingleRootFinder();
  private static final double X1 = 4;
  private static final double X2 = 10;
  private static final double X3 = -10;
  private static final double X = 6;
  private static final double ROOT;
  private static final double EPS = 1e-12;

  static {
    final double q = 1. / 3;
    final double r = -50;
    final double a = Math.pow(Math.abs(r) + Math.sqrt(r * r - q * q * q), 1. / 3);
    final double b = q / a;
    ROOT = a + b + 2;
  }

  @Test
  public void testNullFunction1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot((Function<Double, Double>) null, X1, X2));
  }

  @Test
  public void testNullLower1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot(F2, (Double) null, X2));
  }

  @Test
  public void testNullHigher1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot(F2, X1, (Double) null));
  }

  @Test
  public void testNullFunction2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot((DoubleFunction1D) null, X1, X2));
  }

  @Test
  public void testNullLower2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot(F1, (Double) null, X2));
  }

  @Test
  public void testNullHigher2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot(F1, X1, (Double) null));
  }

  @Test
  public void testNullFunction3() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot(null, DF2, X1, X2));
  }

  @Test
  public void testNullDerivative1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot(F2, null, X1, X2));
  }

  @Test
  public void testNullLower3() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot(F2, DF2, null, X2));
  }

  @Test
  public void testNullHigher3() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot(F2, DF2, X1, null));
  }

  @Test
  public void testNullFunction4() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot(null, DF1, X1, X2));
  }

  @Test
  public void testNullDerivative2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot(F1, null, X1, X2));
  }

  @Test
  public void testNullLower4() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot(F1, DF1, null, X2));
  }

  @Test
  public void testNullHigher4() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot(F1, DF1, X1, null));
  }

  @Test
  public void testEnclosedExtremum() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot(F2, DF2, X1, X3));
  }

  @Test
  public void testNullDerivative3() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot(F1, (DoubleFunction1D) null, X));
  }

  @Test
  public void testNullDerivative4() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot(F2, (Function<Double, Double>) null, X));
  }

  @Test
  public void testNullFunction5() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot((Function<Double, Double>) null, X));
  }

  @Test
  public void testNullFunction6() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot((DoubleFunction1D) null, X));
  }

  @Test
  public void testNullInitialGuess1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot(F1, (Double) null));
  }

  @Test
  public void testNullInitialGuess2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot(F2, (Double) null));
  }

  @Test
  public void testNullInitialGuess3() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot(F1, DF1, null));
  }

  @Test
  public void testNullInitialGuess4() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoot(F2, DF2, null));
  }

  @Test
  public void test() {
    assertThat(ROOT_FINDER.getRoot(F2, DF2, ROOT, X2)).isEqualTo(ROOT);
    assertThat(ROOT_FINDER.getRoot(F2, DF2, X1, ROOT)).isEqualTo(ROOT);
    assertThat(ROOT_FINDER.getRoot(F1, X1, X2)).isCloseTo(ROOT, offset(EPS));
    assertThat(ROOT_FINDER.getRoot(F1, DF1, X1, X2)).isCloseTo(ROOT, offset(EPS));
    assertThat(ROOT_FINDER.getRoot(F2, X1, X2)).isCloseTo(ROOT, offset(EPS));
    assertThat(ROOT_FINDER.getRoot(F2, DF2, X1, X2)).isCloseTo(ROOT, offset(EPS));
    assertThat(ROOT_FINDER.getRoot(F1, X)).isCloseTo(ROOT, offset(EPS));
    assertThat(ROOT_FINDER.getRoot(F1, DF1, X)).isCloseTo(ROOT, offset(EPS));
    assertThat(ROOT_FINDER.getRoot(F2, X)).isCloseTo(ROOT, offset(EPS));
    assertThat(ROOT_FINDER.getRoot(F2, DF2, X)).isCloseTo(ROOT, offset(EPS));
  }
}
