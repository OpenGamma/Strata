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

import org.junit.jupiter.api.Test;

import com.opengamma.strata.math.MathException;
import com.opengamma.strata.math.impl.function.RealPolynomialFunction1D;

/**
 * Test.
 */
public class QuadraticRealRootFinderTest {
  private static final double EPS = 1e-9;
  private static final RealPolynomialFunction1D F = new RealPolynomialFunction1D(12., 7., 1.);
  private static final Polynomial1DRootFinder<Double> FINDER = new QuadraticRealRootFinder();

  @Test
  public void test() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FINDER.getRoots(null));
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FINDER.getRoots(new RealPolynomialFunction1D(1., 2., 3., 4.)));
    assertThatExceptionOfType(MathException.class)
        .isThrownBy(() -> FINDER.getRoots(new RealPolynomialFunction1D(12., 1., 12.)));
    final Double[] roots = FINDER.getRoots(F);
    assertThat(roots[0]).isCloseTo(-4.0, offset(EPS));
    assertThat(roots[1]).isCloseTo(-3.0, offset(EPS));
  }
}
