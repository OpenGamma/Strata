/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.minimization;

import static com.opengamma.strata.math.MathUtils.pow2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.math.MathException;

/**
 * Test.
 */
public class ParabolicMinimumBracketerTest extends MinimumBracketerTestCase {
  private static final MinimumBracketer BRACKETER = new ParabolicMinimumBracketer();
  private static final Function<Double, Double> LINEAR = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return 2 * x - 4;
    }

  };
  private static final Function<Double, Double> QUADRATIC = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return x * x + 7 * x + 12;
    }

  };
  private static final Function<Double, Double> MOD_QUADRATIC = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return Math.abs(x * x - 4);
    }
  };

  private static final Function<Double, Double> STRETCHED_QUADRATIC = new Function<Double, Double>() {

    @Override
    public Double apply(final Double x) {
      return pow2((x - 50) / 50.0);
    }
  };

  @Override
  protected MinimumBracketer getBracketer() {
    return BRACKETER;
  }

  @Test
  public void test() {
    assertThatExceptionOfType(MathException.class)
        .isThrownBy(() -> BRACKETER.getBracketedPoints(LINEAR, 0., 1.));
  }

  @Test
  public void testQuadratic() {
    assertFunction(QUADRATIC, -100, 100);
    assertFunction(QUADRATIC, 100, -100);
    assertFunction(QUADRATIC, 100, 50);
    assertFunction(QUADRATIC, -100, -50);
  }

  @Test
  public void testInitialGuessBracketsTwoMinima() {
    assertFunction(MOD_QUADRATIC, -3, -1);
    assertFunction(MOD_QUADRATIC, -3, 3.5);
  }

  @Test
  public void testStretchedQuadratic() {
    assertFunction(STRETCHED_QUADRATIC, 0, 1);
  }

  private void assertFunction(final Function<Double, Double> f, final double xLower, final double xUpper) {
    final double[] result = BRACKETER.getBracketedPoints(f, xLower, xUpper);
    if (result[0] < result[1]) {
      assertThat(result[1] < result[2]).isTrue();
    } else {
      assertThat(result[2] < result[1]).isTrue();
    }
    final double f2 = f.apply(result[1]);
    assertThat(f.apply(result[0]) > f2).isTrue();
    assertThat(f.apply(result[2]) > f2).isTrue();
  }
}
