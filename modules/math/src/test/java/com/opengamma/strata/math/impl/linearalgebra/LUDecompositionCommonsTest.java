/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.matrix.CommonsMatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.linearalgebra.Decomposition;
import com.opengamma.strata.math.linearalgebra.DecompositionResult;

/**
 * Test.
 */
public class LUDecompositionCommonsTest {
  private static final MatrixAlgebra ALGEBRA = new CommonsMatrixAlgebra();
  private static final Decomposition<LUDecompositionResult> LU = new LUDecompositionCommons();
  private static final DoubleMatrix A = DoubleMatrix.copyOf(
      new double[][] {{1, 2, -1}, {4, 3, 1}, {2, 2, 3}});
  private static final double EPS = 1e-9;

  @Test
  public void testNullObjectMatrix() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> LU.apply((DoubleMatrix) null));
  }

  @Test
  public void testRecoverOrginal() {
    final DecompositionResult result = LU.apply(A);
    assertThat(result instanceof LUDecompositionResult).isTrue();
    final LUDecompositionResult lu = (LUDecompositionResult) result;
    final DoubleMatrix a = (DoubleMatrix) ALGEBRA.multiply(lu.getL(), lu.getU());
    checkEquals((DoubleMatrix) ALGEBRA.multiply(lu.getP(), A), a);
  }

  private void checkEquals(final DoubleMatrix x, final DoubleMatrix y) {
    final int n = x.rowCount();
    final int m = x.columnCount();
    assertThat(n).isEqualTo(y.rowCount());
    assertThat(m).isEqualTo(y.columnCount());
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < m; j++) {
        assertThat(x.get(i, j)).isCloseTo(y.get(i, j), offset(EPS));
      }
    }
  }
}
