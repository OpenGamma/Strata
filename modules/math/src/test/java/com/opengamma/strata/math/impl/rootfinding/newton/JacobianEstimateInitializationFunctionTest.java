/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Test.
 */
public class JacobianEstimateInitializationFunctionTest {

  private static final JacobianEstimateInitializationFunction ESTIMATE = new JacobianEstimateInitializationFunction();
  private static final Function<DoubleArray, DoubleMatrix> J = new Function<DoubleArray, DoubleMatrix>() {
    @Override
    public DoubleMatrix apply(DoubleArray v) {
      double[] x = v.toArray();
      return DoubleMatrix.copyOf(new double[][] {{x[0] * x[0], x[0] * x[1]}, {x[0] - x[1], x[1] * x[1]}});
    }
  };

  private static final DoubleArray X = DoubleArray.of(1, 2);

  @Test
  public void testNullFunction() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ESTIMATE.getInitializedMatrix(null, X));
  }

  @Test
  public void testNullVector() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> ESTIMATE.getInitializedMatrix(J, null));
  }

  @Test
  public void test() {
    DoubleMatrix m1 = ESTIMATE.getInitializedMatrix(J, X);
    DoubleMatrix m2 = J.apply(X);
    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < 2; j++) {
        assertThat(m1.get(i, j)).isCloseTo(m2.get(i, j), offset(1e-9));
      }
    }
  }
}
