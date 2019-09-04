/*
 * Copyright (C) 2011 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.offset;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.impl.matrix.OGMatrixAlgebra;
import com.opengamma.strata.math.linearalgebra.Decomposition;

/**
 * Tests the Cholesky decomposition OpenGamma implementation.
 */
public class CholeskyDecompositionOpenGammaTest {

  private static final MatrixAlgebra ALGEBRA = new OGMatrixAlgebra();
  private static final CholeskyDecompositionOpenGamma CDOG = new CholeskyDecompositionOpenGamma();
  private static final Decomposition<CholeskyDecompositionResult> CDC = new CholeskyDecompositionCommons();
  private static final DoubleMatrix A3 = DoubleMatrix.copyOf(
      new double[][] {{10.0, 2.0, -1.0}, {2.0, 5.0, -2.0}, {-1.0, -2.0, 15.0}});
  private static final DoubleMatrix A5 = DoubleMatrix.copyOf(
      new double[][] {
          {10.0, 2.0, -1.0, 1.0, 1.0},
          {2.0, 5.0, -2.0, 0.5, 0.5},
          {-1.0, -2.0, 15.0, 1.0, 0.5},
          {1.0, 0.5, 1.0, 10.0, -1.0},
          {1.0, 0.5, 0.5, -1.0, 25.0}});
  private static final double EPS = 1e-9;

  @Test
  public void testNullObjectMatrix() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> CDOG.apply((DoubleMatrix) null));
  }

  /**
   * Tests A = L L^T.
   */
  @Test
  public void recoverOrginal() {
    final CholeskyDecompositionResult result = CDOG.apply(A3);
    final DoubleMatrix a = (DoubleMatrix) ALGEBRA.multiply(result.getL(), result.getLT());
    checkEquals(A3, a);
  }

  /**
   * Tests solve Ax = b from A and b.
   */
  @Test
  public void solveVector() {
    final CholeskyDecompositionResult result = CDOG.apply(A5);
    double[] b = new double[] {1.0, 2.0, 3.0, 4.0, -1.0};
    double[] x = result.solve(b);
    DoubleArray ax = (DoubleArray) ALGEBRA.multiply(A5, DoubleArray.copyOf(x));
    assertThat(ax.toArray()).usingComparatorWithPrecision(1e-10).containsExactly(b);
  }

  /**
   * Tests solve AX = B from A and B.
   */
  @Test
  public void solveMatrix() {
    final CholeskyDecompositionResult result = CDOG.apply(A5);
    double[][] b = new double[][] {{1.0, 2.0}, {2.0, 3.0}, {3.0, 4.0}, {4.0, -2.0}, {-1.0, -1.0}};
    DoubleMatrix x = result.solve(DoubleMatrix.copyOf(b));
    DoubleMatrix ax = (DoubleMatrix) ALGEBRA.multiply(A5, x);
    assertThat(ax.rowArray(0)).usingComparatorWithPrecision(1e-10).containsExactly(b[0]);
    assertThat(ax.rowArray(1)).usingComparatorWithPrecision(1e-10).containsExactly(b[1]);
  }

  /**
   * Compare results with Common decomposition
   */
  @Test
  public void compareCommon() {
    final CholeskyDecompositionResult resultOG = CDOG.apply(A3);
    final CholeskyDecompositionResult resultC = CDC.apply(A3);
    checkEquals(resultC.getL(), resultOG.getL());
    checkEquals(ALGEBRA.getTranspose(resultC.getL()), resultOG.getLT());
    assertThat(resultOG.getDeterminant()).isCloseTo(resultC.getDeterminant(), offset(1e-10));
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
