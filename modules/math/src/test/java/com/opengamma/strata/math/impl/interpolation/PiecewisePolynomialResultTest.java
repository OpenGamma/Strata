/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.interpolation;

import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Test for {@link PiecewisePolynomialResult} and subclasses
 */
public class PiecewisePolynomialResultTest {

  /**
   * 
   */
  @Test
  public void hashCodeEqualsTest() {
    final double[] knots1 = new double[] {1., 2., 3., 4. };
    final double[] knots2 = new double[] {1., 2., 3., 4., 5., 6., 7. };
    final double[][] matrix1 = new double[][] { {3., 3., 3. }, {1., 1., 1. }, {2., 2., 2. }, {3., 3., 3. }, {1., 1., 1. }, {2., 2., 2. } };
    final double[][] matrix2 = new double[][] { {3., 3., 3. }, {1., 1., 1. }, {2., 2., 2. } };
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

    assertTrue(res1.equals(res1));

    assertTrue(res1.equals(res2));
    assertTrue(res2.equals(res1));
    assertTrue(res2.hashCode() == res1.hashCode());

    assertTrue(!(res3.hashCode() == res1.hashCode()));
    assertTrue(!(res1.equals(res3)));
    assertTrue(!(res3.equals(res1)));

    assertTrue(!(res4.hashCode() == res1.hashCode()));
    assertTrue(!(res1.equals(res4)));
    assertTrue(!(res4.equals(res1)));

    assertTrue(!(res5.hashCode() == res1.hashCode()));
    assertTrue(!(res1.equals(res5)));
    assertTrue(!(res5.equals(res1)));

    assertTrue(!(res6.hashCode() == res1.hashCode()));
    assertTrue(!(res1.equals(res6)));
    assertTrue(!(res6.equals(res1)));

    assertTrue(!(res1.equals(null)));
    assertTrue(!(res1.equals(DoubleArray.copyOf(knots1))));

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
    assertTrue(resSen1.equals(resSen1));

    assertTrue(!(resSen1.equals(DoubleArray.copyOf(knots1))));

    assertTrue(!(resSen1.equals(res5)));

    assertTrue(resSen1.equals(resSen2));
    assertTrue(resSen2.equals(resSen1));
    assertTrue(resSen1.hashCode() == resSen2.hashCode());

    assertTrue(!(resSen1.hashCode() == resSen3.hashCode()));
    assertTrue(!(resSen1.equals(resSen3)));
    assertTrue(!(resSen3.equals(resSen1)));

    try {
      @SuppressWarnings("unused")
      final PiecewisePolynomialResultsWithSensitivity resSen0 =
          new PiecewisePolynomialResultsWithSensitivity(
              DoubleArray.copyOf(knots1), DoubleMatrix.copyOf(matrix1), order, 2, sense1);
      throw new RuntimeException();
    } catch (Exception e) {
      assertTrue(e instanceof UnsupportedOperationException);
    }
  }
}
