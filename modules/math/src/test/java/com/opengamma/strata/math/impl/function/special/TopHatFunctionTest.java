/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.function.special;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class TopHatFunctionTest {
  private static final double X1 = 2;
  private static final double X2 = 2.5;
  private static final double Y = 10;
  private static final Function<Double, Double> F = new TopHatFunction(X1, X2, Y);

  @Test
  public void testWrongOrder() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TopHatFunction(X2, X1, Y));
  }

  @Test
  public void testNull() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> F.apply((Double) null));
  }

  @Test
  public void testX1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> F.apply(X1));
  }

  @Test
  public void testX2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> F.apply(X2));
  }

  @Test
  public void test() {
    assertThat(F.apply(X1 - 1e-15)).isEqualTo(0);
    assertThat(F.apply(X2 + 1e-15)).isEqualTo(0);
    assertThat(F.apply((X1 + X2) / 2)).isEqualTo(Y);
  }
}
