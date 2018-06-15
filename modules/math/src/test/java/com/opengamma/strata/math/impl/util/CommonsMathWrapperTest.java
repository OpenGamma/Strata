/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.util;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.internal.junit.ArrayAsserts.assertArrayEquals;

import java.util.function.Function;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.testng.annotations.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Test {@link CommonsMathWrapper}.
 */
@Test
public class CommonsMathWrapperTest {

  private static final DoubleArray OG_VECTOR = DoubleArray.of(1, 2, 3);
  private static final DoubleMatrix OG_MATRIX = DoubleMatrix.copyOf(
      new double[][] { {1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
  private static final Function<Double, Double> OG_FUNCTION_1D = new Function<Double, Double>() {
    @Override
    public Double apply(final Double x) {
      return x * x + 7 * x + 12;
    }

  };

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull1DMatrix() {
    CommonsMathWrapper.wrap((DoubleArray) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullVector() {
    CommonsMathWrapper.unwrap((RealVector) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNull1DFunction() {
    CommonsMathWrapper.wrapUnivariate((Function<Double, Double>) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullMatrix() {
    CommonsMathWrapper.wrap((DoubleMatrix) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testNullRealMatrix() {
    CommonsMathWrapper.unwrap((RealMatrix) null);
  }

  @Test
  public void testVector() {
    RealVector commons = CommonsMathWrapper.wrap(OG_VECTOR);
    assertEquals(CommonsMathWrapper.unwrap(commons), OG_VECTOR);
  }

  @Test
  public void testVectorAsMatrix() {
    RealMatrix commons = CommonsMathWrapper.wrapAsMatrix(OG_VECTOR);
    double[][] data = commons.getData();
    assertEquals(data.length, OG_VECTOR.size());
    assertEquals(data[0].length, 1);
  }

  @Test
  public void test1DFunction() {
    UnivariateFunction commons = CommonsMathWrapper.wrapUnivariate(OG_FUNCTION_1D);
    for (int i = 0; i < 100; i++) {
      assertEquals(OG_FUNCTION_1D.apply((double) i), commons.value(i), 1e-15);
    }
  }

  @Test
  public void testMatrix() {
    RealMatrix commons = CommonsMathWrapper.wrap(OG_MATRIX);
    double[][] unwrapped = CommonsMathWrapper.unwrap(commons).toArray();
    double[][] ogData = OG_MATRIX.toArray();
    int n = unwrapped.length;
    assertEquals(n, ogData.length);
    for (int i = 0; i < n; i++) {
      assertArrayEquals(unwrapped[i], ogData[i], 1e-15);
    }
  }

}
