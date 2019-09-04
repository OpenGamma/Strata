/*
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Tests the Cholesky decomposition wrapping.
 */
public class CholeskyDecompositionCommonsTest {

  private static final MatrixAlgebra ALGEBRA = new CommonsMatrixAlgebra();
  private static final Decomposition<CholeskyDecompositionResult> CH = new CholeskyDecompositionCommons();
  private static final DoubleMatrix A = DoubleMatrix.copyOf(
      new double[][] {{10.0, 2.0, -1.0}, {2.0, 5.0, -2.0}, {-1.0, -2.0, 15.0}});
  private static final double EPS = 1e-9;

  @Test
  public void testNullObjectMatrix() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CH.apply((DoubleMatrix) null));
  }

  @Test
  public void testRecoverOrginal() {
    final DecompositionResult result = CH.apply(A);
    assertThat(result instanceof CholeskyDecompositionResult).isTrue();
    final CholeskyDecompositionResult ch = (CholeskyDecompositionResult) result;
    final DoubleMatrix a = (DoubleMatrix) ALGEBRA.multiply(ch.getL(), ch.getLT());
    checkEquals(A, a);
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
