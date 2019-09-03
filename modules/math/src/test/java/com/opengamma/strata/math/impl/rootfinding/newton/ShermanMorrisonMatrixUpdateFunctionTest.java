/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.rootfinding.newton;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;

/**
 * Test.
 */
public class ShermanMorrisonMatrixUpdateFunctionTest {
  private static final MatrixAlgebra ALGEBRA = new OGMatrixAlgebra();
  private static final ShermanMorrisonMatrixUpdateFunction UPDATE = new ShermanMorrisonMatrixUpdateFunction(ALGEBRA);
  private static final DoubleArray V = DoubleArray.of(1, 2);
  private static final DoubleMatrix M = DoubleMatrix.copyOf(new double[][] {{3, 4}, {5, 6}});
  private static final Function<DoubleArray, DoubleMatrix> J = new Function<DoubleArray, DoubleMatrix>() {
    @SuppressWarnings("synthetic-access")
    @Override
    public DoubleMatrix apply(DoubleArray x) {
      return ALGEBRA.getOuterProduct(x, x);
    }
  };

  @Test
  public void testNull() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new ShermanMorrisonMatrixUpdateFunction(null));
  }

  @Test
  public void testNullDeltaX() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> UPDATE.getUpdatedMatrix(J, V, null, V, M));
  }

  @Test
  public void testNullDeltaY() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> UPDATE.getUpdatedMatrix(J, V, V, null, M));
  }

  @Test
  public void testNullMatrix() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> UPDATE.getUpdatedMatrix(J, V, V, V, null));
  }
}
