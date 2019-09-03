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
import com.opengamma.strata.math.impl.linearalgebra.DecompositionFactory;
import com.opengamma.strata.math.impl.matrix.CommonsMatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.linearalgebra.Decomposition;

/**
 * Test.
 */
public class InverseJacobianEstimateInitializationFunctionTest {

  private static final MatrixAlgebra ALGEBRA = new CommonsMatrixAlgebra();
  private static final Decomposition<?> SV = DecompositionFactory.SV_COMMONS;
  private static final InverseJacobianEstimateInitializationFunction ESTIMATE =
      new InverseJacobianEstimateInitializationFunction(SV);
  private static final Function<DoubleArray, DoubleMatrix> J = new Function<DoubleArray, DoubleMatrix>() {

    @Override
    public DoubleMatrix apply(DoubleArray v) {
      double[] x = v.toArray();
      return DoubleMatrix.copyOf(new double[][] {{x[0] * x[0], x[0] * x[1]}, {x[0] - x[1], x[1] * x[1]}});
    }

  };
  private static final DoubleArray X = DoubleArray.of(3, 4);

  @Test
  public void testNullDecomposition() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new InverseJacobianEstimateInitializationFunction(null));
  }

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
    DoubleMatrix m3 = (DoubleMatrix) (ALGEBRA.multiply(m1, m2));
    DoubleMatrix identity = DoubleMatrix.identity(2);
    for (int i = 0; i < 2; i++) {
      for (int j = 0; j < 2; j++) {
        assertThat(m3.get(i, j)).isCloseTo(identity.get(i, j), offset(1e-6));
      }
    }
  }

}
