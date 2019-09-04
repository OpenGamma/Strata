/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.math.impl.FunctionUtils;

/**
 * Test.
 */
public class BasisFunctionGeneratorTest {
  private static final BasisFunctionGenerator GENERATOR = new BasisFunctionGenerator();
  private static final double[] KNOTS;

  static {
    final int n = 10;
    KNOTS = new double[n + 1];
    for (int i = 0; i < n + 1; i++) {
      KNOTS[i] = 0 + i * 1.0;
    }

  }

  @Test
  public void testFunctionIndexOutOfRange1() {
    BasisFunctionKnots k = BasisFunctionKnots.fromKnots(KNOTS, 2);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> GENERATOR.generate(k, -1));
  }

  @Test
  public void testFunctionIndexOutOfRange2() {
    BasisFunctionKnots k = BasisFunctionKnots.fromKnots(KNOTS, 2);
    int nS = k.getNumSplines();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> GENERATOR.generate(k, nS));
  }

  @Test
  public void testZeroOrder() {
    BasisFunctionKnots knots = BasisFunctionKnots.fromInternalKnots(KNOTS, 0);
    final Function<Double, Double> func = GENERATOR.generate(knots, 4);
    assertThat(func.apply(3.5)).isEqualTo(0.0);
    assertThat(func.apply(4.78)).isEqualTo(1.0);
    assertThat(func.apply(4.0)).isEqualTo(1.0);
    assertThat(func.apply(5.0)).isEqualTo(0.0);
  }

  @Test
  public void testFirstOrder() {
    BasisFunctionKnots knots = BasisFunctionKnots.fromInternalKnots(KNOTS, 1);
    final Function<Double, Double> func = GENERATOR.generate(knots, 3);
    assertThat(func.apply(1.76)).isEqualTo(0.0);
    assertThat(func.apply(3.0)).isEqualTo(1.0);
    assertThat(func.apply(4.0)).isEqualTo(0);
    assertThat(func.apply(2.5)).isEqualTo(0.5);
  }

  @Test
  public void testSecondOrder() {
    BasisFunctionKnots knots = BasisFunctionKnots.fromInternalKnots(KNOTS, 2);
    final Function<Double, Double> func = GENERATOR.generate(knots, 3);
    assertThat(func.apply(0.76)).isEqualTo(0.0);
    assertThat(func.apply(1.5)).isEqualTo(0.125);
    assertThat(func.apply(2.0)).isEqualTo(0.5);
    assertThat(func.apply(2.5)).isEqualTo(0.75);
    assertThat(func.apply(4.0)).isEqualTo(0.0);
  }

  @Test
  public void testThirdOrder() {
    BasisFunctionKnots knots = BasisFunctionKnots.fromInternalKnots(KNOTS, 3);
    final Function<Double, Double> func = GENERATOR.generate(knots, 3);
    assertThat(func.apply(-0.1)).isEqualTo(0);
    assertThat(func.apply(1d)).isEqualTo(1d / 6d);
    assertThat(func.apply(2d)).isEqualTo(2d / 3d);
    assertThat(func.apply(3.5d)).isEqualTo(1d / 48d);
    assertThat(func.apply(4d)).isEqualTo(0);
  }

  @Test
  public void testTwoD() {

    BasisFunctionKnots knots1 = BasisFunctionKnots.fromInternalKnots(KNOTS, 2);
    BasisFunctionKnots knots2 = BasisFunctionKnots.fromInternalKnots(KNOTS, 3);
    List<Function<double[], Double>> set = GENERATOR.generateSet(new BasisFunctionKnots[] {knots1, knots2});

    //pick of one of the basis functions for testing 
    int index = FunctionUtils.toTensorIndex(new int[] {3, 3}, new int[] {knots1.getNumSplines(), knots2.getNumSplines()});
    Function<double[], Double> func = set.get(index);
    assertThat(func.apply(new double[] {2.0, 2.0})).isEqualTo(1d / 3d);
    assertThat(func.apply(new double[] {2.5, 2.0})).isEqualTo(1d / 2d);
    assertThat(func.apply(new double[] {1.5, 3.5})).isEqualTo(1d / 8d / 48d);
    assertThat(func.apply(new double[] {4.0, 2.5})).isEqualTo(0);
  }

  @Test
  public void testThreeD() {
    BasisFunctionKnots knots1 = BasisFunctionKnots.fromInternalKnots(KNOTS, 2);
    BasisFunctionKnots knots2 = BasisFunctionKnots.fromInternalKnots(KNOTS, 3);
    BasisFunctionKnots knots3 = BasisFunctionKnots.fromInternalKnots(KNOTS, 1);
    List<Function<double[], Double>> set = GENERATOR.generateSet(new BasisFunctionKnots[] {knots1, knots2, knots3});

    //pick of one of the basis functions for testing 
    int index = FunctionUtils.toTensorIndex(new int[] {3, 3, 3}, new int[] {knots1.getNumSplines(), knots2.getNumSplines(),
        knots3.getNumSplines()});
    Function<double[], Double> func = set.get(index);
    assertThat(func.apply(new double[] {2.0, 2.0, 3.0})).isEqualTo(1d / 3d);
  }

}
