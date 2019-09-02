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
 * Abstract test.
 */
public abstract class RealSingleRootFinderTestCase {
  protected static final Function<Double, Double> F = new Function<Double, Double>() {
    @Override
    public Double apply(Double x) {
      return x * x * x - 4 * x * x + x + 6;
    }
  };
  protected static final double EPS = 1e-9;

  protected abstract RealSingleRootFinder getRootFinder();

  @Test
  public void testNullFunction() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> getRootFinder().checkInputs((DoubleFunction1D) null, 1., 2.));
  }

  @Test
  public void testNullLower() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> getRootFinder().checkInputs(F, null, 2.));
  }

  @Test
  public void testNullUpper() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> getRootFinder().checkInputs(F, 1., null));
  }

  @Test
  public void testOutsideRoots() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> getRootFinder().getRoot(F, 10., 100.));
  }

  @Test
  public void testBracketTwoRoots() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> getRootFinder().getRoot(F, 1.5, 3.5));
  }

  @Test
  public void test() {
    RealSingleRootFinder finder = getRootFinder();
    assertThat(finder.getRoot(F, 2.5, 3.5)).isCloseTo(3, offset(EPS));
    assertThat(finder.getRoot(F, 1.5, 2.5)).isCloseTo(2, offset(EPS));
    assertThat(finder.getRoot(F, -1.5, 0.5)).isCloseTo(-1, offset(EPS));
  }
}
