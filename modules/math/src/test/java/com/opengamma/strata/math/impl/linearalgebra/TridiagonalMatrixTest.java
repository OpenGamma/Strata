/*
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.linearalgebra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.collect.array.DoubleMatrix;

/**
 * Test.
 */
public class TridiagonalMatrixTest {
  private static final double[] A = new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
  private static final double[] B = new double[] {1, 2, 3, 4, 5, 6, 7, 8, 9};
  private static final double[] C = new double[] {2, 3, 4, 5, 6, 7, 8, 9, 10};
  private static final TridiagonalMatrix M = new TridiagonalMatrix(A, B, C);

  @Test
  public void testNullA() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TridiagonalMatrix(null, B, C));
  }

  @Test
  public void testNullB() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TridiagonalMatrix(A, null, C));
  }

  @Test
  public void testNullC() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TridiagonalMatrix(A, B, null));
  }

  @Test
  public void testWrongB() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TridiagonalMatrix(A, new double[] {1, 2, 3, 4, 5, 6, 7, 8}, C));
  }

  @Test
  public void testWrongC() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TridiagonalMatrix(A, B, new double[] {1, 2, 3, 4, 5, 6, 7}));
  }

  @Test
  public void testGetters() {
    assertThat(Arrays.equals(A, M.getDiagonalData())).isTrue();
    assertThat(Arrays.equals(B, M.getUpperSubDiagonalData())).isTrue();
    assertThat(Arrays.equals(C, M.getLowerSubDiagonalData())).isTrue();
    final int n = A.length;
    final DoubleMatrix matrix = M.toDoubleMatrix();
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        if (i == j) {
          assertThat(matrix.get(i, j)).isEqualTo(A[i]);
        } else if (j == i + 1) {
          assertThat(matrix.get(i, j)).isEqualTo(B[j - 1]);
        } else if (j == i - 1) {
          assertThat(matrix.get(i, j)).isEqualTo(C[j]);
        } else {
          assertThat(matrix.get(i, j)).isEqualTo(0);
        }
      }
    }
  }

  @Test
  public void testHashCodeAndEquals() {
    final double[] a = Arrays.copyOf(A, A.length);
    final double[] b = Arrays.copyOf(B, B.length);
    final double[] c = Arrays.copyOf(C, C.length);
    TridiagonalMatrix other = new TridiagonalMatrix(a, b, c);
    assertThat(other).isEqualTo(M);
    assertThat(other.hashCode()).isEqualTo(M.hashCode());
    a[1] = 1000;
    other = new TridiagonalMatrix(a, B, C);
    assertThat(other.equals(M)).isFalse();
    b[1] = 1000;
    other = new TridiagonalMatrix(A, b, C);
    assertThat(other.equals(M)).isFalse();
    c[1] = 1000;
    other = new TridiagonalMatrix(A, B, c);
    assertThat(other.equals(M)).isFalse();
  }
}
