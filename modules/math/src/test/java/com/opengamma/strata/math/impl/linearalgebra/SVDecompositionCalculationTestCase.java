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

import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.collect.array.DoubleMatrix;
import com.opengamma.strata.math.impl.matrix.MatrixAlgebra;
import com.opengamma.strata.math.linearalgebra.Decomposition;
import com.opengamma.strata.math.linearalgebra.DecompositionResult;

/**
 * Abstract test.
 */
public abstract class SVDecompositionCalculationTestCase {
  private static final double EPS = 1e-10;
  private static final DoubleMatrix A = DoubleMatrix.copyOf(
      new double[][] {{1, 2, 3}, {-3.4, -1, 4}, {1, 6, 1}});

  protected abstract Decomposition<SVDecompositionResult> getSVD();

  protected abstract MatrixAlgebra getAlgebra();

  @Test
  public void testNullObjectMatrix() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> getSVD().apply((DoubleMatrix) null));
  }

  @Test
  public void testRecoverOrginal() {
    final MatrixAlgebra algebra = getAlgebra();
    final DecompositionResult result = getSVD().apply(A);
    assertThat(result instanceof SVDecompositionResult).isTrue();
    final SVDecompositionResult svdResult = (SVDecompositionResult) result;
    final DoubleMatrix u = svdResult.getU();
    final DoubleMatrix w = DoubleMatrix.diagonal(DoubleArray.copyOf(svdResult.getSingularValues()));
    final DoubleMatrix vt = svdResult.getVT();
    final DoubleMatrix a = (DoubleMatrix) algebra.multiply(algebra.multiply(u, w), vt);
    checkEquals(A, a);
  }

  @Test
  public void testInvert() {
    final MatrixAlgebra algebra = getAlgebra();
    final SVDecompositionResult result = getSVD().apply(A);
    final DoubleMatrix ut = result.getUT();
    final DoubleMatrix v = result.getV();
    final double[] sv = result.getSingularValues();
    final int n = sv.length;
    final double[] svinv = new double[n];
    for (int i = 0; i < n; i++) {
      if (sv[i] == 0.0) {
        svinv[i] = 0.0;
      } else {
        svinv[i] = 1.0 / sv[i];
      }
    }
    final DoubleMatrix winv = DoubleMatrix.diagonal(DoubleArray.copyOf(svinv));
    final DoubleMatrix ainv = (DoubleMatrix) algebra.multiply(algebra.multiply(v, winv), ut);
    final DoubleMatrix identity = (DoubleMatrix) algebra.multiply(A, ainv);
    checkIdentity(identity);

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

  private void checkIdentity(final DoubleMatrix x) {
    final int n = x.rowCount();
    assertThat(x.columnCount()).isEqualTo(n);
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        if (i == j) {
          assertThat(1.0).isCloseTo(x.get(i, i), offset(EPS));
        } else {
          assertThat(0.0).isCloseTo(x.get(i, j), offset(EPS));
        }
      }
    }
  }
}
