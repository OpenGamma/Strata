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
public class HermitePolynomialFunctionTest {

  private static final DoubleFunction1D H0 = x -> 1d;
  private static final DoubleFunction1D H1 = x -> 2 * x;
  private static final DoubleFunction1D H2 = x -> 4 * x * x - 2;
  private static final DoubleFunction1D H3 = x -> x * (8 * x * x - 12);
  private static final DoubleFunction1D H4 = x -> 16 * x * x * x * x - 48 * x * x + 12;
  private static final DoubleFunction1D H5 = x -> x * (32 * x * x * x * x - 160 * x * x + 120);
  private static final DoubleFunction1D H6 = x -> 64 * x * x * x * x * x * x - 480 * x * x * x * x + 720 * x * x - 120;
  private static final DoubleFunction1D H7 =
      x -> x * (128 * x * x * x * x * x * x - 1344 * x * x * x * x + 3360 * x * x - 1680);
  private static final DoubleFunction1D H8 = x -> {
    double xSq = x * x;
    return 256 * xSq * xSq * xSq * xSq - 3584 * xSq * xSq * xSq + 13440 * xSq * xSq - 13440 * xSq + 1680;
  };
  private static final DoubleFunction1D H9 = x -> {
    double xSq = x * x;
    return x * (512 * xSq * xSq * xSq * xSq - 9216 * xSq * xSq * xSq + 48384 * xSq * xSq - 80640 * xSq + 30240);
  };
  private static final DoubleFunction1D H10 = x -> {
    double xSq = x * x;
    return 1024 * xSq * xSq * xSq * xSq * xSq - 23040 *
        xSq * xSq * xSq * xSq + 161280 * xSq * xSq * xSq - 403200 * xSq * xSq + 302400 * xSq - 30240;
  };
  private static final DoubleFunction1D[] H = new DoubleFunction1D[] {H0, H1, H2, H3, H4, H5, H6, H7, H8, H9, H10};
  private static final HermitePolynomialFunction HERMITE = new HermitePolynomialFunction();
  private static final double EPS = 1e-9;

  @Test
  public void testBadN() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HERMITE.getPolynomials(-3));
  }

  @Test
  public void testGetPolynomials() {
    assertThatExceptionOfType(UnsupportedOperationException.class)
        .isThrownBy(() -> HERMITE.getPolynomialsAndFirstDerivative(3));
  }

  @Test
  public void test() {
    DoubleFunction1D[] h = HERMITE.getPolynomials(0);
    assertThat(h.length).isEqualTo(1);
    final double x = 1.23;
    assertThat(h[0].applyAsDouble(x)).isCloseTo(1, offset(EPS));
    h = HERMITE.getPolynomials(1);
    assertThat(h.length).isEqualTo(2);
    assertThat(h[1].applyAsDouble(x)).isCloseTo(2 * x, offset(EPS));
    for (int i = 0; i <= 10; i++) {
      h = HERMITE.getPolynomials(i);
      for (int j = 0; j <= i; j++) {
        assertThat(H[j].applyAsDouble(x)).isCloseTo(h[j].applyAsDouble(x), offset(EPS));
      }
    }
  }
}
