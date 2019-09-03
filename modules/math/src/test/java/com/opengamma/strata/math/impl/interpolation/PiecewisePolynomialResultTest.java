/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Test for {@link PiecewisePolynomialResult} and subclasses
 */
public class PiecewisePolynomialResultTest {

  private static final Object ANOTHER_TYPE = "";

  @Test
  public void hashCodeEqualsTest() {
    final double[] knots1 = new double[] {1., 2., 3., 4.};
    final double[] knots2 = new double[] {1., 2., 3., 4., 5., 6., 7.};
    final double[][] matrix1 = new double[][] {
        {3., 3., 3.}, {1., 1., 1.}, {2., 2., 2.}, {3., 3., 3.}, {1., 1., 1.}, {2., 2., 2.}};
    final double[][] matrix2 = new double[][] {{3., 3., 3.}, {1., 1., 1.}, {2., 2., 2.}};
    final int order = 3;
    final int dim1 = 2;
    final int dim2 = 1;

    final PiecewisePolynomialResult res1 =
        new PiecewisePolynomialResult(DoubleArray.copyOf(knots1), DoubleMatrix.copyOf(matrix1), order, dim1);
    final PiecewisePolynomialResult res2 =
        new PiecewisePolynomialResult(DoubleArray.copyOf(knots1), DoubleMatrix.copyOf(matrix1), order, dim1);
    final PiecewisePolynomialResult res3 =
        new PiecewisePolynomialResult(DoubleArray.copyOf(knots2), DoubleMatrix.copyOf(matrix2), order, dim2);
    final PiecewisePolynomialResult res4 =
        new PiecewisePolynomialResult(DoubleArray.copyOf(knots1), DoubleMatrix.copyOf(matrix1), 2, dim1);
    final PiecewisePolynomialResult res5 =
        new PiecewisePolynomialResult(DoubleArray.copyOf(knots1), DoubleMatrix.copyOf(matrix1), order, dim1 - 1);
    final PiecewisePolynomialResult res6 =
        new PiecewisePolynomialResult(DoubleArray.of(1., 2., 3., 5.), DoubleMatrix.copyOf(matrix1), order, dim1);

    assertThat(res1.equals(res1)).isTrue();

    assertThat(res1.equals(res2)).isTrue();
    assertThat(res2.equals(res1)).isTrue();
    assertThat(res2.hashCode() == res1.hashCode()).isTrue();

    assertThat(!(res3.hashCode() == res1.hashCode())).isTrue();
    assertThat(!(res1.equals(res3))).isTrue();
    assertThat(!(res3.equals(res1))).isTrue();

    assertThat(!(res4.hashCode() == res1.hashCode())).isTrue();
    assertThat(!(res1.equals(res4))).isTrue();
    assertThat(!(res4.equals(res1))).isTrue();

    assertThat(!(res5.hashCode() == res1.hashCode())).isTrue();
    assertThat(!(res1.equals(res5))).isTrue();
    assertThat(!(res5.equals(res1))).isTrue();

    assertThat(!(res6.hashCode() == res1.hashCode())).isTrue();
    assertThat(!(res1.equals(res6))).isTrue();
    assertThat(!(res6.equals(res1))).isTrue();

    assertThat(!(res1.equals(null))).isTrue();
    assertThat(!(res1.equals(ANOTHER_TYPE))).isTrue();

    final DoubleMatrix[] sense1 = new DoubleMatrix[] {DoubleMatrix.copyOf(matrix1), DoubleMatrix.copyOf(matrix1)};
    final DoubleMatrix[] sense2 =
        new DoubleMatrix[] {DoubleMatrix.copyOf(matrix1), DoubleMatrix.copyOf(matrix1), DoubleMatrix.copyOf(matrix1)};

    final PiecewisePolynomialResultsWithSensitivity resSen1 =
        new PiecewisePolynomialResultsWithSensitivity(
            DoubleArray.copyOf(knots1), DoubleMatrix.copyOf(matrix1), order, 1, sense1);
    final PiecewisePolynomialResultsWithSensitivity resSen2 =
        new PiecewisePolynomialResultsWithSensitivity(
            DoubleArray.copyOf(knots1), DoubleMatrix.copyOf(matrix1), order, 1, sense1);
    final PiecewisePolynomialResultsWithSensitivity resSen3 =
        new PiecewisePolynomialResultsWithSensitivity(
            DoubleArray.copyOf(knots1), DoubleMatrix.copyOf(matrix1), order, 1, sense2);
    assertThat(resSen1.equals(resSen1)).isTrue();

    assertThat(!(resSen1.equals(ANOTHER_TYPE))).isTrue();

    assertThat(!(resSen1.equals(res5))).isTrue();

    assertThat(resSen1.equals(resSen2)).isTrue();
    assertThat(resSen2.equals(resSen1)).isTrue();
    assertThat(resSen1.hashCode() == resSen2.hashCode()).isTrue();

    assertThat(!(resSen1.hashCode() == resSen3.hashCode())).isTrue();
    assertThat(!(resSen1.equals(resSen3))).isTrue();
    assertThat(!(resSen3.equals(resSen1))).isTrue();

    try {
      @SuppressWarnings("unused")
      final PiecewisePolynomialResultsWithSensitivity resSen0 =
          new PiecewisePolynomialResultsWithSensitivity(
              DoubleArray.copyOf(knots1), DoubleMatrix.copyOf(matrix1), order, 2, sense1);
      throw new RuntimeException();
    } catch (Exception e) {
      assertThat(e instanceof UnsupportedOperationException).isTrue();
    }
  }
}
