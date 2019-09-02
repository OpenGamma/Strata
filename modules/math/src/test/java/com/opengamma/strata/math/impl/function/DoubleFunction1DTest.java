/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.math.impl.differentiation.FiniteDifferenceType;

/**
 * Test.
 */
public class DoubleFunction1DTest {

  private static final DoubleFunction1D F1 = x -> x * x * x + 2 * x * x - 7 * x + 12;
  private static final DoubleFunction1D DF1 = x -> 3 * x * x + 4 * x - 7;
  private static final DoubleFunction1D F2 = x -> Math.sin(x);
  private static final DoubleFunction1D DF2 = x -> Math.cos(x);
  private static final DoubleFunction1D F3 = new DoubleFunction1D() {

    @Override
    public double applyAsDouble(double x) {
      return x * x * x + 2 * x * x - 7 * x + 12;
    }

    @SuppressWarnings("synthetic-access")
    @Override
    public DoubleFunction1D derivative() {
      return DF1;
    }
  };
  private static final DoubleFunction1D F4 = new DoubleFunction1D() {

    @Override
    public double applyAsDouble(double x) {
      return Math.sin(x);
    }

    @SuppressWarnings("synthetic-access")
    @Override
    public DoubleFunction1D derivative() {
      return DF2;
    }
  };
  private static final double X = 0.1234;
  private static final double A = 5.67;
  private static final double EPS = 1e-15;

  @Test
  public void testAddNull() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> F1.add(null));
  }

  @Test
  public void testDivideNull() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> F1.divide(null));
  }

  @Test
  public void testMultiplyNull() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> F1.multiply(null));
  }

  @Test
  public void testSubtractNull() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> F1.subtract(null));
  }

  @Test
  public void testConvertNull() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> DoubleFunction1D.from(null));
  }

  @Test
  public void testDerivativeNullType() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> F1.derivative(null, EPS));
  }

  @Test
  public void testAdd() {
    assertThat(F1.add(F2).applyAsDouble(X)).isCloseTo(F1.applyAsDouble(X) + F2.applyAsDouble(X), offset(EPS));
    assertThat(F1.add(A).applyAsDouble(X)).isCloseTo(F1.applyAsDouble(X) + A, offset(EPS));
  }

  @Test
  public void testDivide() {
    assertThat(F1.divide(F2).applyAsDouble(X)).isCloseTo(F1.applyAsDouble(X) / F2.applyAsDouble(X), offset(EPS));
    assertThat(F1.divide(A).applyAsDouble(X)).isCloseTo(F1.applyAsDouble(X) / A, offset(EPS));
  }

  @Test
  public void testMultiply() {
    assertThat(F1.multiply(F2).applyAsDouble(X)).isCloseTo(F1.applyAsDouble(X) * F2.applyAsDouble(X), offset(EPS));
    assertThat(F1.multiply(A).applyAsDouble(X)).isCloseTo(F1.applyAsDouble(X) * A, offset(EPS));
  }

  @Test
  public void testSubtract() {
    assertThat(F1.subtract(F2).applyAsDouble(X)).isCloseTo(F1.applyAsDouble(X) - F2.applyAsDouble(X), offset(EPS));
    assertThat(F1.subtract(A).applyAsDouble(X)).isCloseTo(F1.applyAsDouble(X) - A, offset(EPS));
  }

  @Test
  public void testDerivative() {
    assertThat(F1.derivative().applyAsDouble(X)).isCloseTo(DF1.applyAsDouble(X), offset(1e-3));
    assertThat(F2.derivative().applyAsDouble(X)).isCloseTo(DF2.applyAsDouble(X), offset(1e-3));
    assertThat(F1.derivative(FiniteDifferenceType.CENTRAL, 1e-5).applyAsDouble(X)).isCloseTo(DF1.applyAsDouble(X), offset(1e-3));
    assertThat(F2.derivative(FiniteDifferenceType.CENTRAL, 1e-5).applyAsDouble(X)).isCloseTo(DF2.applyAsDouble(X), offset(1e-3));
    assertThat(F1.derivative(FiniteDifferenceType.FORWARD, 1e-5).applyAsDouble(X)).isCloseTo(DF1.applyAsDouble(X), offset(1e-3));
    assertThat(F2.derivative(FiniteDifferenceType.FORWARD, 1e-5).applyAsDouble(X)).isCloseTo(DF2.applyAsDouble(X), offset(1e-3));
    assertThat(F1.derivative(FiniteDifferenceType.BACKWARD, 1e-5).applyAsDouble(X)).isCloseTo(DF1.applyAsDouble(X), offset(1e-3));
    assertThat(F2.derivative(FiniteDifferenceType.BACKWARD, 1e-5).applyAsDouble(X)).isCloseTo(DF2.applyAsDouble(X), offset(1e-3));
    assertThat(F3.derivative().applyAsDouble(X)).isCloseTo(DF1.applyAsDouble(X), offset(1e-15));
    assertThat(F4.derivative().applyAsDouble(X)).isCloseTo(DF2.applyAsDouble(X), offset(1e-15));
  }

  @Test
  public void testConversion() {
    final Function<Double, Double> f1 = x -> x * x * x + 2 * x * x - 7 * x + 12;
    final DoubleFunction1D f2 = DoubleFunction1D.from(f1);
    for (int i = 0; i < 100; i++) {
      final double x = Math.random();
      assertThat(f2.applyAsDouble(x)).isEqualTo(F1.applyAsDouble(x));
      assertThat(f2.derivative().applyAsDouble(x)).isEqualTo(F1.derivative().applyAsDouble(x));
    }
  }
}
