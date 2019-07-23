/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.array;

import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Test {@link DoubleMatrix}.
 */
public class DoubleMatrixTest {

  private static final Object ANOTHER_TYPE = "";

  @Test
  public void test_EMPTY() {
    assertMatrix(DoubleMatrix.EMPTY);
  }

  @Test
  public void test_of() {
    assertMatrix(DoubleMatrix.of());
  }

  @Test
  public void test_of_values() {
    assertMatrix(DoubleMatrix.of(0, 0));
    assertMatrix(DoubleMatrix.of(1, 0));
    assertMatrix(DoubleMatrix.of(0, 1));
    assertMatrix(DoubleMatrix.of(2, 3, 1d, 2d, 3d, 4d, 5d, 6d), 1d, 2d, 3d, 4d, 5d, 6d);
    assertMatrix(DoubleMatrix.of(6, 1, 1d, 2d, 3d, 4d, 5d, 6d), 1d, 2d, 3d, 4d, 5d, 6d);
    assertThatIllegalArgumentException().isThrownBy(() -> DoubleMatrix.of(1, 2, 1d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_intintlambda() {
    assertMatrix(DoubleMatrix.of(0, 0, (i, j) -> {
      throw new AssertionError();
    }));
    assertMatrix(DoubleMatrix.of(0, 2, (i, j) -> {
      throw new AssertionError();
    }));
    assertMatrix(DoubleMatrix.of(2, 0, (i, j) -> {
      throw new AssertionError();
    }));
    AtomicInteger counter = new AtomicInteger(2);
    assertMatrix(DoubleMatrix.of(1, 2, (i, j) -> counter.getAndIncrement()), 2d, 3d);
    assertMatrix(DoubleMatrix.of(2, 2, (i, j) -> (i + 1) * (j + 1)), 1d, 2d, 2d, 4d);
  }

  @Test
  public void test_ofArrayObjects() {
    assertMatrix(DoubleMatrix.ofArrayObjects(0, 0, i -> {
      throw new AssertionError();
    }));
    assertMatrix(DoubleMatrix.ofArrayObjects(0, 2, i -> {
      throw new AssertionError();
    }));
    assertMatrix(DoubleMatrix.ofArrayObjects(2, 0, i -> {
      throw new AssertionError();
    }));
    AtomicInteger counter = new AtomicInteger(2);
    assertMatrix(DoubleMatrix.ofArrayObjects(1, 2,
        i -> DoubleArray.of(counter.getAndIncrement(), counter.getAndIncrement())), 2d, 3d);
    assertThatIllegalArgumentException().isThrownBy(() -> DoubleMatrix.ofArrayObjects(1, 2, i -> DoubleArray.EMPTY));
  }

  @Test
  public void test_ofArrays() {
    assertMatrix(DoubleMatrix.ofArrays(0, 0, i -> {
      throw new AssertionError();
    }));
    assertMatrix(DoubleMatrix.ofArrays(0, 2, i -> {
      throw new AssertionError();
    }));
    assertMatrix(DoubleMatrix.ofArrays(2, 0, i -> {
      throw new AssertionError();
    }));
    AtomicInteger counter = new AtomicInteger(2);
    assertMatrix(DoubleMatrix.ofArrays(1, 2,
        i -> new double[] {counter.getAndIncrement(), counter.getAndIncrement()}), 2d, 3d);
    assertThatIllegalArgumentException().isThrownBy(() -> DoubleMatrix.ofArrays(1, 2, i -> new double[0]));
  }

  @Test
  public void test_ofUnsafe() {
    double[][] base = {{1d, 2d}, {3d, 4d}};
    DoubleMatrix test = DoubleMatrix.ofUnsafe(base);
    assertMatrix(test, 1d, 2d, 3d, 4d);
    base[0][0] = 7d;
    // internal state of object mutated - don't do this in application code!
    assertMatrix(test, 7d, 2d, 3d, 4d);
    // empty
    assertMatrix(DoubleMatrix.ofUnsafe(new double[0][0]));
    assertMatrix(DoubleMatrix.ofUnsafe(new double[0][2]));
    assertMatrix(DoubleMatrix.ofUnsafe(new double[2][0]));
  }

  @Test
  public void test_copyOf_array() {
    double[][] base = {{1d, 2d}, {3d, 4d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    assertMatrix(test, 1d, 2d, 3d, 4d);
    base[0][0] = 7d;
    // internal state of object is not mutated
    assertMatrix(test, 1d, 2d, 3d, 4d);
    // empty
    assertMatrix(DoubleMatrix.copyOf(new double[0][0]));
    assertMatrix(DoubleMatrix.copyOf(new double[0][2]));
    assertMatrix(DoubleMatrix.copyOf(new double[2][0]));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_filled() {
    assertMatrix(DoubleMatrix.filled(0, 0));
    assertMatrix(DoubleMatrix.filled(0, 2));
    assertMatrix(DoubleMatrix.filled(2, 0));
    assertMatrix(DoubleMatrix.filled(3, 2), 0d, 0d, 0d, 0d, 0d, 0d);
  }

  @Test
  public void test_filled_withValue() {
    assertMatrix(DoubleMatrix.filled(0, 0, 7d));
    assertMatrix(DoubleMatrix.filled(0, 2, 7d));
    assertMatrix(DoubleMatrix.filled(2, 0, 7d));
    assertMatrix(DoubleMatrix.filled(3, 2, 7d), 7d, 7d, 7d, 7d, 7d, 7d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_identity() {
    assertMatrix(DoubleMatrix.identity(0));
    assertMatrix(DoubleMatrix.identity(2), 1d, 0d, 0d, 1d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_diagonal() {
    assertMatrix(DoubleMatrix.diagonal(DoubleArray.EMPTY));
    assertMatrix(DoubleMatrix.diagonal(DoubleArray.of(2d, 3d, 4d)), 2d, 0d, 0d, 0d, 3d, 0d, 0d, 0d, 4d);
    assertThat(DoubleMatrix.diagonal(DoubleArray.of(1d, 1d, 1d))).isEqualTo(DoubleMatrix.identity(3));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_get() {
    double[][] base = {{1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    assertThat(test.get(0, 0)).isEqualTo(1d);
    assertThat(test.get(2, 1)).isEqualTo(6d);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.get(-1, 0));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.get(0, 4));
  }

  @Test
  public void test_row() {
    double[][] base = {{1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    assertThat(test.row(0)).isEqualTo(DoubleArray.of(1d, 2d));
    assertThat(test.row(1)).isEqualTo(DoubleArray.of(3d, 4d));
    assertThat(test.row(2)).isEqualTo(DoubleArray.of(5d, 6d));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.row(-1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.row(4));
  }

  @Test
  public void test_rowArray() {
    double[][] base = {{1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    assertThat(test.rowArray(0)).isEqualTo(new double[] {1d, 2d});
    assertThat(test.rowArray(1)).isEqualTo(new double[] {3d, 4d});
    assertThat(test.rowArray(2)).isEqualTo(new double[] {5d, 6d});
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.rowArray(-1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.rowArray(4));
  }

  @Test
  public void test_column() {
    double[][] base = {{1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    assertThat(test.column(0)).isEqualTo(DoubleArray.of(1d, 3d, 5d));
    assertThat(test.column(1)).isEqualTo(DoubleArray.of(2d, 4d, 6d));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.column(-1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.column(4));
  }

  @Test
  public void test_columnArray() {
    double[][] base = {{1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    assertThat(test.columnArray(0)).isEqualTo(new double[] {1d, 3d, 5d});
    assertThat(test.columnArray(1)).isEqualTo(new double[] {2d, 4d, 6d});
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.columnArray(-1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.columnArray(4));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forEach() {
    double[][] base = {{1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    double[] extracted = new double[6];
    test.forEach((i, j, v) -> extracted[i * 2 + j] = v);
    assertThat(extracted).containsExactly(1d, 2d, 3d, 4d, 5d, 6d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_with() {
    double[][] base = {{1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    assertMatrix(test.with(0, 0, 2.6d), 2.6d, 2d, 3d, 4d, 5d, 6d);
    assertMatrix(test.with(0, 0, 1d), 1d, 2d, 3d, 4d, 5d, 6d);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.with(-1, 0, 2d));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.with(3, 0, 2d));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.with(0, -1, 2d));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.with(0, 3, 2d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_multipliedBy() {
    double[][] base = {{1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    assertMatrix(test.multipliedBy(5), 5d, 10d, 15d, 20d, 25d, 30d);
    assertMatrix(test.multipliedBy(1), 1d, 2d, 3d, 4d, 5d, 6d);
  }

  @Test
  public void test_map() {
    double[][] base = {{1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    assertMatrix(test.map(v -> 1 / v), 1d, 1d / 2d, 1d / 3d, 1d / 4d, 1d / 5d, 1d / 6d);
  }

  @Test
  public void test_mapWithIndex() {
    double[][] base = {{1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    assertMatrix(test.mapWithIndex((i, j, v) -> i * (j + 1) * v), 0d, 0d, 3d, 8d, 10d, 24d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_plus() {
    DoubleMatrix test1 = DoubleMatrix.of(2, 3, 1d, 2d, 3d, 4d, 5d, 6d);
    DoubleMatrix test2 = DoubleMatrix.of(2, 3, 0.5d, 0.6d, 0.7d, 0.5d, 0.6d, 0.7d);
    assertMatrix(test1.plus(test2), 1.5d, 2.6d, 3.7d, 4.5d, 5.6d, 6.7d);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.plus(DoubleMatrix.EMPTY));
  }

  @Test
  public void test_minus() {
    DoubleMatrix test1 = DoubleMatrix.of(2, 3, 1d, 2d, 3d, 4d, 5d, 6d);
    DoubleMatrix test2 = DoubleMatrix.of(2, 3, 0.5d, 0.6d, 0.7d, 0.5d, 0.6d, 0.7d);
    assertMatrix(test1.minus(test2), 0.5d, 1.4d, 2.3d, 3.5d, 4.4d, 5.3d);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.minus(DoubleMatrix.EMPTY));
  }

  @Test
  public void test_combine() {
    DoubleMatrix test1 = DoubleMatrix.of(2, 3, 1d, 2d, 3d, 4d, 5d, 6d);
    DoubleMatrix test2 = DoubleMatrix.of(2, 3, 0.5d, 0.6d, 0.7d, 0.5d, 0.6d, 0.7d);
    assertMatrix(test1.combine(test2, (a, b) -> a * b), 0.5d, 2d * 0.6d, 3d * 0.7d, 4d * 0.5d, 5d * 0.6d, 6d * 0.7d);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.combine(DoubleMatrix.EMPTY, (a, b) -> a * b));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_total() {
    assertThat(DoubleMatrix.EMPTY.total()).isEqualTo(0d);
    assertThat(DoubleMatrix.copyOf(new double[][] {{1d, 2d}, {3d, 4d}, {5d, 6d}}).total()).isEqualTo(21d);
  }

  @Test
  public void test_reduce() {
    assertThat(DoubleMatrix.EMPTY.reduce(2d, (r, v) -> {
      throw new AssertionError();
    })).isEqualTo(2d);
    assertThat(DoubleMatrix.copyOf(new double[][] {{2d}}).reduce(1d, (r, v) -> r * v)).isEqualTo(2d);
    assertThat(DoubleMatrix.copyOf(new double[][] {{2d, 3d}}).reduce(1d, (r, v) -> r * v)).isEqualTo(6d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void testTransposeMatrix() {
    DoubleMatrix m0 = DoubleMatrix.EMPTY;
    assertThat(m0.transpose()).isEqualTo(DoubleMatrix.EMPTY);

    DoubleMatrix m1 = DoubleMatrix.copyOf(new double[][] {
        {1, 2, 3},
        {4, 5, 6},
        {7, 8, 9}});
    assertThat(m1.transpose()).isEqualTo(DoubleMatrix.copyOf(new double[][] {
        {1, 4, 7},
        {2, 5, 8},
        {3, 6, 9}}));

    DoubleMatrix m2 = DoubleMatrix.copyOf(new double[][] {
        {1, 2, 3, 4, 5, 6},
        {7, 8, 9, 10, 11, 12},
        {13, 14, 15, 16, 17, 18}});
    assertThat(m2.transpose()).isEqualTo(DoubleMatrix.copyOf(new double[][] {
        {1, 7, 13},
        {2, 8, 14},
        {3, 9, 15},
        {4, 10, 16},
        {5, 11, 17},
        {6, 12, 18}}));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCode() {
    DoubleMatrix a1 = DoubleMatrix.copyOf(new double[][] {{2d, 3d}});
    DoubleMatrix a2 = DoubleMatrix.copyOf(new double[][] {{2d, 3d}});
    DoubleMatrix b = DoubleMatrix.copyOf(new double[][] {{3d, 3d}});
    DoubleMatrix c = DoubleMatrix.copyOf(new double[][] {{2d, 3d}, {4d, 5d}});
    DoubleMatrix d = DoubleMatrix.copyOf(new double[][] {{2d}});
    assertThat(a1.equals(a1)).isEqualTo(true);
    assertThat(a1.equals(a2)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(c)).isEqualTo(false);
    assertThat(a1.equals(d)).isEqualTo(false);
    assertThat(a1.equals(ANOTHER_TYPE)).isEqualTo(false);
    assertThat(a1.equals(null)).isEqualTo(false);
    assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
  }

  @Test
  public void test_toString() {
    DoubleMatrix test = DoubleMatrix.copyOf(new double[][] {{1d, 2d}, {3d, 4d}, {5d, 6d}});
    assertThat(test.toString()).isEqualTo("1.0 2.0\n3.0 4.0\n5.0 6.0\n");
  }

  //-------------------------------------------------------------------------
  private void assertMatrix(DoubleMatrix matrix, double... expected) {
    if (expected.length == 0) {
      assertThat(matrix).isSameAs(DoubleMatrix.EMPTY);
      assertThat(matrix.isEmpty()).isEqualTo(true);
    } else {
      assertThat(matrix.size()).isEqualTo(expected.length);
      int rowCount = matrix.rowCount();
      int colCount = matrix.columnCount();
      double[][] array = matrix.toArrayUnsafe();
      double[][] array2 = matrix.toArray();
      assertThat(Arrays.deepEquals(array, array2)).isTrue();
      for (int i = 0; i < rowCount; i++) {
        for (int j = 0; j < colCount; j++) {
          assertThat(matrix.get(i, j)).isEqualTo(expected[i * colCount + j]);
          assertThat(array[i][j]).isEqualTo(expected[i * colCount + j]);
        }
      }
      assertThat(matrix.dimensions()).isEqualTo(2);
      assertThat(matrix.isEmpty()).isEqualTo(false);
      assertThat(matrix.isSquare()).isEqualTo(matrix.rowCount() == matrix.columnCount());
    }
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(DoubleMatrix.EMPTY);
    coverImmutableBean(DoubleMatrix.of(2, 3, 1d, 2d, 3d, 4d, 5d, 6d));
  }

}
