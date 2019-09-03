/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Test {@link CommonsMathWrapper}.
 */
public class CommonsMathWrapperTest {

  private static final DoubleArray OG_VECTOR = DoubleArray.of(1, 2, 3);
  private static final DoubleMatrix OG_MATRIX = DoubleMatrix.copyOf(
      new double[][] {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
  private static final Function<Double, Double> OG_FUNCTION_1D = new Function<Double, Double>() {
    @Override
    public Double apply(final Double x) {
      return x * x + 7 * x + 12;
    }

  };

  @Test
  public void testNull1DMatrix() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CommonsMathWrapper.wrap((DoubleArray) null));
  }

  @Test
  public void testNullVector() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CommonsMathWrapper.unwrap((RealVector) null));
  }

  @Test
  public void testNull1DFunction() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CommonsMathWrapper.wrapUnivariate((Function<Double, Double>) null));
  }

  @Test
  public void testNullMatrix() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CommonsMathWrapper.wrap((DoubleMatrix) null));
  }

  @Test
  public void testNullRealMatrix() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CommonsMathWrapper.unwrap((RealMatrix) null));
  }

  @Test
  public void testVector() {
    RealVector commons = CommonsMathWrapper.wrap(OG_VECTOR);
    assertThat(CommonsMathWrapper.unwrap(commons)).isEqualTo(OG_VECTOR);
  }

  @Test
  public void testVectorAsMatrix() {
    RealMatrix commons = CommonsMathWrapper.wrapAsMatrix(OG_VECTOR);
    double[][] data = commons.getData();
    assertThat(data.length).isEqualTo(OG_VECTOR.size());
    assertThat(data[0].length).isEqualTo(1);
  }

  @Test
  public void test1DFunction() {
    UnivariateFunction commons = CommonsMathWrapper.wrapUnivariate(OG_FUNCTION_1D);
    for (int i = 0; i < 100; i++) {
      assertThat(OG_FUNCTION_1D.apply((double) i)).isCloseTo(commons.value(i), offset(1e-15));
    }
  }

  @Test
  public void testMatrix() {
    RealMatrix commons = CommonsMathWrapper.wrap(OG_MATRIX);
    double[][] unwrapped = CommonsMathWrapper.unwrap(commons).toArray();
    double[][] ogData = OG_MATRIX.toArray();
    int n = unwrapped.length;
    assertThat(n).isEqualTo(ogData.length);
    for (int i = 0; i < n; i++) {
      double[] a = unwrapped[i];
      double[] b = ogData[i];
      assertThat(a).usingComparatorWithPrecision(1e-15).containsExactly(b);
    }
  }

}
