/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.math.impl.ComplexNumber;
import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;

/**
 * Test.
 */
public class CubicRootFindingTest {
  private static final CubicRootFinder CUBIC = new CubicRootFinder();
  private static final CubicRealRootFinder REAL_ONLY_CUBIC = new CubicRealRootFinder();
  private static final RealPolynomialFunction1D ONE_REAL_ROOT = new RealPolynomialFunction1D(-10, 10, -3, 3);
  private static final RealPolynomialFunction1D ONE_DISTINCT_ROOT = new RealPolynomialFunction1D(-1, 3, -3, 1);
  private static final RealPolynomialFunction1D THREE_ROOTS = new RealPolynomialFunction1D(-6, 11, -6, 1);
  private static final double EPS = 1e-12;

  @Test
  public void testNullFunction1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CUBIC.getRoots(null));
  }

  @Test
  public void testNonCubic1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CUBIC.getRoots(new RealPolynomialFunction1D(1, 1, 1, 1, 1)));
  }

  @Test
  public void testNullFunction2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> REAL_ONLY_CUBIC.getRoots(null));
  }

  @Test
  public void testNonCubic2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> REAL_ONLY_CUBIC.getRoots(new RealPolynomialFunction1D(1, 1, 1, 1, 1)));
  }

  @Test
  public void testCubic() {
    ComplexNumber[] result = CUBIC.getRoots(ONE_REAL_ROOT);
    assertThat(result.length).isEqualTo(3);
    assertComplexEquals(result[0], new ComplexNumber(1, 0));
    assertComplexEquals(result[1], new ComplexNumber(0, Math.sqrt(10 / 3.)));
    assertComplexEquals(result[2], new ComplexNumber(0, -Math.sqrt(10 / 3.)));
    result = CUBIC.getRoots(ONE_DISTINCT_ROOT);
    assertThat(result.length).isEqualTo(3);
    for (final ComplexNumber c : result) {
      assertComplexEquals(c, new ComplexNumber(1, 0));
    }
    result = CUBIC.getRoots(THREE_ROOTS);
    assertThat(result.length).isEqualTo(3);
    assertComplexEquals(result[0], new ComplexNumber(1, 0));
    assertComplexEquals(result[1], new ComplexNumber(3, 0));
    assertComplexEquals(result[2], new ComplexNumber(2, 0));
  }

  @Test
  public void testRealOnlyCubic() {
    Double[] result = REAL_ONLY_CUBIC.getRoots(ONE_REAL_ROOT);
    assertThat(result.length).isEqualTo(1);
    assertThat(result[0]).isEqualTo(1);
    result = REAL_ONLY_CUBIC.getRoots(ONE_DISTINCT_ROOT);
    assertThat(result.length).isEqualTo(3);
    for (final Double d : result) {
      assertThat(d).isCloseTo(1, offset(EPS));
    }
    result = REAL_ONLY_CUBIC.getRoots(THREE_ROOTS);
    assertThat(result.length).isEqualTo(3);
    assertThat(result[0]).isCloseTo(1, offset(EPS));
    assertThat(result[1]).isCloseTo(3, offset(EPS));
    assertThat(result[2]).isCloseTo(2, offset(EPS));
  }

  private void assertComplexEquals(final ComplexNumber c1, final ComplexNumber c2) {
    assertThat(c1.getReal()).isCloseTo(c2.getReal(), offset(EPS));
    assertThat(c1.getImaginary()).isCloseTo(c2.getImaginary(), offset(EPS));
  }
}
