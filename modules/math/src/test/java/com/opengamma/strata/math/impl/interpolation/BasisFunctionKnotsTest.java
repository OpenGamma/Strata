/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

/**
 * 
 */
public class BasisFunctionKnotsTest {

  private static final double[] KNOTS;
  private static final double[] WRONG_ORDER_KNOTS;

  static {
    final int n = 10;
    KNOTS = new double[n + 1];

    for (int i = 0; i < n + 1; i++) {
      KNOTS[i] = 0 + i * 1.0;
    }
    WRONG_ORDER_KNOTS = KNOTS.clone();
    double a = WRONG_ORDER_KNOTS[6];
    WRONG_ORDER_KNOTS[6] = WRONG_ORDER_KNOTS[4];
    WRONG_ORDER_KNOTS[4] = a;
  }

  @Test
  public void testNullKnots() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BasisFunctionKnots.fromKnots(null, 2));
  }

  @Test
  public void testNullInternalKnots() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BasisFunctionKnots.fromInternalKnots(null, 2));
  }

  @Test
  public void testNegDegree() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BasisFunctionKnots.fromKnots(KNOTS, -1));
  }

  @Test
  public void testNegDegree2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BasisFunctionKnots.fromInternalKnots(KNOTS, -1));
  }

  @Test
  public void testWrongOrderUniform() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BasisFunctionKnots.fromUniform(2.0, 1.0, 10, 3));
  }

  @Test
  public void testWrongOrderKnots() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BasisFunctionKnots.fromKnots(WRONG_ORDER_KNOTS, 3));
  }

  @Test
  public void testWrongOrderInternalKnots() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BasisFunctionKnots.fromInternalKnots(WRONG_ORDER_KNOTS, 3));
  }

  @Test
  public void testDegreeToHigh1() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BasisFunctionKnots.fromUniform(0.0, 10.0, 11, 11));
  }

  @Test
  public void testDegreeToHigh2() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BasisFunctionKnots.fromInternalKnots(KNOTS, 11));
  }

  @Test
  public void testDegreeToHigh3() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> BasisFunctionKnots.fromKnots(KNOTS, 11));
  }

  @Test
  public void testUniform() {
    BasisFunctionKnots knots = BasisFunctionKnots.fromUniform(1.0, 2.0, 10, 3);
    assertThat(knots.getDegree()).isEqualTo(3);
    assertThat(knots.getNumKnots()).isEqualTo(16);
    assertThat(knots.getNumSplines()).isEqualTo(12);
  }

  @Test
  public void testInternalKnots() {
    BasisFunctionKnots knots = BasisFunctionKnots.fromInternalKnots(KNOTS, 2);
    assertThat(knots.getDegree()).isEqualTo(2);
    assertThat(knots.getNumKnots()).isEqualTo(15);
    assertThat(knots.getNumSplines()).isEqualTo(12);
  }

  @Test
  public void testKnots() {
    BasisFunctionKnots knots = BasisFunctionKnots.fromKnots(KNOTS, 3);
    assertThat(knots.getDegree()).isEqualTo(3);
    assertThat(knots.getNumKnots()).isEqualTo(11);
    assertThat(knots.getNumSplines()).isEqualTo(7);
    assertThat(knots.getKnots()).usingComparatorWithPrecision(1e-15).containsExactly(KNOTS);
  }

}
