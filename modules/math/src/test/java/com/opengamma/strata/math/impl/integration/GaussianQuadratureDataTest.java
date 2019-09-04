/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * Test.
 */
public class GaussianQuadratureDataTest {
  private static final double[] X = new double[] {1, 2, 3, 4};
  private static final double[] W = new double[] {6, 7, 8, 9};
  private static final GaussianQuadratureData F = new GaussianQuadratureData(X, W);

  @Test
  public void testNullAbscissas() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new GaussianQuadratureData(null, W));
  }

  @Test
  public void testNullWeights() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new GaussianQuadratureData(X, null));
  }

  @Test
  public void testWrongLength() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new GaussianQuadratureData(X, new double[] {1, 2, 3}));
  }

  @Test
  public void test() {
    GaussianQuadratureData other = new GaussianQuadratureData(X, W);
    assertThat(F).isEqualTo(other);
    assertThat(F.hashCode()).isEqualTo(other.hashCode());
    other = new GaussianQuadratureData(W, W);
    assertThat(F.equals(other)).isFalse();
    other = new GaussianQuadratureData(X, X);
    assertThat(F.equals(other)).isFalse();
    assertThat(F.getAbscissas()).isEqualTo(X);
    assertThat(F.getWeights()).isEqualTo(W);
  }
}
