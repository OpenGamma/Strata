/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.math.MathException;
import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;

/**
 * Test.
 */
public class LaguerrePolynomialRealRootFinderTest {
  private static final double EPS = 1e-12;
  private static final LaguerrePolynomialRealRootFinder ROOT_FINDER = new LaguerrePolynomialRealRootFinder();
  private static final RealPolynomialFunction1D TWO_REAL_ROOTS = new RealPolynomialFunction1D(12, 7, 1);
  private static final RealPolynomialFunction1D ONE_REAL_ROOT = new RealPolynomialFunction1D(9, -6, 1);
  private static final RealPolynomialFunction1D CLOSE_ROOTS = new RealPolynomialFunction1D(9 + 3 * 1e-6, -6 - 1e-6, 1);
  private static final RealPolynomialFunction1D NO_REAL_ROOTS = new RealPolynomialFunction1D(12, 0, 1);

  @Test
  public void testNullFunction() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ROOT_FINDER.getRoots(null));
  }

  @Test
  public void testNoRealRoots() {
    assertThatExceptionOfType(MathException.class)
        .isThrownBy(() -> ROOT_FINDER.getRoots(NO_REAL_ROOTS));
  }

  @Test
  public void test() {
    Double[] result = ROOT_FINDER.getRoots(TWO_REAL_ROOTS);
    Arrays.sort(result);
    assertThat(result.length).isEqualTo(2);
    assertThat(result[0]).isCloseTo(-4, offset(EPS));
    assertThat(result[1]).isCloseTo(-3, offset(EPS));
    result = ROOT_FINDER.getRoots(ONE_REAL_ROOT);
    assertThat(result.length).isEqualTo(2);
    assertThat(result[0]).isCloseTo(3, offset(EPS));
    assertThat(result[1]).isCloseTo(3, offset(EPS));
    result = ROOT_FINDER.getRoots(CLOSE_ROOTS);
    Arrays.sort(result);
    assertThat(result.length).isEqualTo(2);
    assertThat(result[1] - result[0]).isCloseTo(1e-6, offset(1e-8));
  }
}
