/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.array;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

/**
 * Test {@link DoubleMatrix}.
 */
@Test
public class DoubleMatrixTest {

  public void test_EMPTY() {
    assertMatrix(DoubleMatrix.EMPTY);
  }

  public void test_of() {
    assertMatrix(DoubleMatrix.of());
  }

  public void test_of_values() {
    assertMatrix(DoubleMatrix.of(0, 0));
    assertMatrix(DoubleMatrix.of(1, 0));
    assertMatrix(DoubleMatrix.of(0, 1));
    assertMatrix(DoubleMatrix.of(2, 3, 1d, 2d, 3d, 4d, 5d, 6d), 1d, 2d, 3d, 4d, 5d, 6d);
    assertMatrix(DoubleMatrix.of(6, 1, 1d, 2d, 3d, 4d, 5d, 6d), 1d, 2d, 3d, 4d, 5d, 6d);
    assertThrowsIllegalArg(() -> DoubleMatrix.of(1, 2, 1d));
  }

  //-------------------------------------------------------------------------
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
    assertThrowsIllegalArg(() -> DoubleMatrix.ofArrayObjects(1, 2, i -> DoubleArray.EMPTY));
  }

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
    assertThrowsIllegalArg(() -> DoubleMatrix.ofArrays(1, 2, i -> new double[0]));
  }

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
  public void test_filled() {
    assertMatrix(DoubleMatrix.filled(0, 0));
    assertMatrix(DoubleMatrix.filled(0, 2));
    assertMatrix(DoubleMatrix.filled(2, 0));
    assertMatrix(DoubleMatrix.filled(3, 2), 0d, 0d, 0d, 0d, 0d, 0d);
  }

  public void test_filled_withValue() {
    assertMatrix(DoubleMatrix.filled(0, 0, 7d));
    assertMatrix(DoubleMatrix.filled(0, 2, 7d));
    assertMatrix(DoubleMatrix.filled(2, 0, 7d));
    assertMatrix(DoubleMatrix.filled(3, 2, 7d), 7d, 7d, 7d, 7d, 7d, 7d);
  }

  //-------------------------------------------------------------------------
  public void test_identity() {
    assertMatrix(DoubleMatrix.identity(0));
    assertMatrix(DoubleMatrix.identity(2), 1d, 0d, 0d, 1d);
  }

  //-------------------------------------------------------------------------
  public void test_diagonal() {
    assertMatrix(DoubleMatrix.diagonal(DoubleArray.EMPTY));
    assertMatrix(DoubleMatrix.diagonal(DoubleArray.of(2d, 3d, 4d)), 2d, 0d, 0d, 0d, 3d, 0d, 0d, 0d, 4d);
    assertEquals(DoubleMatrix.diagonal(DoubleArray.of(1d, 1d, 1d)), DoubleMatrix.identity(3));
  }

  //-------------------------------------------------------------------------
  public void test_get() {
    double[][] base = {{1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    assertEquals(test.get(0, 0), 1d);
    assertEquals(test.get(2, 1), 6d);
    assertThrows(() -> test.get(-1, 0), IndexOutOfBoundsException.class);
    assertThrows(() -> test.get(0, 4), IndexOutOfBoundsException.class);
  }

  public void test_row() {
    double[][] base = {{1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    assertEquals(test.row(0), DoubleArray.of(1d, 2d));
    assertEquals(test.row(1), DoubleArray.of(3d, 4d));
    assertEquals(test.row(2), DoubleArray.of(5d, 6d));
    assertThrows(() -> test.row(-1), IndexOutOfBoundsException.class);
    assertThrows(() -> test.row(4), IndexOutOfBoundsException.class);
  }

  public void test_rowArray() {
    double[][] base = {{1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    assertEquals(test.rowArray(0), new double[] {1d, 2d});
    assertEquals(test.rowArray(1), new double[] {3d, 4d});
    assertEquals(test.rowArray(2), new double[] {5d, 6d});
    assertThrows(() -> test.rowArray(-1), IndexOutOfBoundsException.class);
    assertThrows(() -> test.rowArray(4), IndexOutOfBoundsException.class);
  }

  public void test_column() {
    double[][] base = {{1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    assertEquals(test.column(0), DoubleArray.of(1d, 3d, 5d));
    assertEquals(test.column(1), DoubleArray.of(2d, 4d, 6d));
    assertThrows(() -> test.column(-1), IndexOutOfBoundsException.class);
    assertThrows(() -> test.column(4), IndexOutOfBoundsException.class);
  }

  public void test_columnArray() {
    double[][] base = {{1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    assertEquals(test.columnArray(0), new double[] {1d, 3d, 5d});
    assertEquals(test.columnArray(1), new double[] {2d, 4d, 6d});
    assertThrows(() -> test.columnArray(-1), IndexOutOfBoundsException.class);
    assertThrows(() -> test.columnArray(4), IndexOutOfBoundsException.class);
  }

  //-------------------------------------------------------------------------
  public void test_forEach() {
    double[][] base = {{1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    double[] extracted = new double[6];
    test.forEach((i, j, v) -> extracted[i * 2 + j] = v);
    assertTrue(Arrays.equals(extracted, new double[] {1d, 2d, 3d, 4d, 5d, 6d}));
  }

  //-------------------------------------------------------------------------
  public void test_with() {
    double[][] base = {{1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    assertMatrix(test.with(0, 0, 2.6d), 2.6d, 2d, 3d, 4d, 5d, 6d);
    assertMatrix(test.with(0, 0, 1d), 1d, 2d, 3d, 4d, 5d, 6d);
    assertThrows(() -> test.with(-1, 0, 2d), IndexOutOfBoundsException.class);
    assertThrows(() -> test.with(3, 0, 2d), IndexOutOfBoundsException.class);
    assertThrows(() -> test.with(0, -1, 2d), IndexOutOfBoundsException.class);
    assertThrows(() -> test.with(0, 3, 2d), IndexOutOfBoundsException.class);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    double[][] base = {{1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    assertMatrix(test.multipliedBy(5), 5d, 10d, 15d, 20d, 25d, 30d);
    assertMatrix(test.multipliedBy(1), 1d, 2d, 3d, 4d, 5d, 6d);
  }

  public void test_map() {
    double[][] base = {{1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    assertMatrix(test.map(v -> 1 / v), 1d, 1d / 2d, 1d / 3d, 1d / 4d, 1d / 5d, 1d / 6d);
  }

  public void test_mapWithIndex() {
    double[][] base = {{1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix test = DoubleMatrix.copyOf(base);
    assertMatrix(test.mapWithIndex((i, j, v) -> i * (j + 1) * v), 0d, 0d, 3d, 8d, 10d, 24d);
  }

  //-------------------------------------------------------------------------
  public void test_plus() {
    DoubleMatrix test1 = DoubleMatrix.of(2, 3, 1d, 2d, 3d, 4d, 5d, 6d);
    DoubleMatrix test2 = DoubleMatrix.of(2, 3, 0.5d, 0.6d, 0.7d, 0.5d, 0.6d, 0.7d);
    assertMatrix(test1.plus(test2), 1.5d, 2.6d, 3.7d, 4.5d, 5.6d, 6.7d);
    assertThrows(() -> test1.plus(DoubleMatrix.EMPTY), IllegalArgumentException.class);
  }

  public void test_minus() {
    DoubleMatrix test1 = DoubleMatrix.of(2, 3, 1d, 2d, 3d, 4d, 5d, 6d);
    DoubleMatrix test2 = DoubleMatrix.of(2, 3, 0.5d, 0.6d, 0.7d, 0.5d, 0.6d, 0.7d);
    assertMatrix(test1.minus(test2), 0.5d, 1.4d, 2.3d, 3.5d, 4.4d, 5.3d);
    assertThrows(() -> test1.minus(DoubleMatrix.EMPTY), IllegalArgumentException.class);
  }

  public void test_combine() {
    DoubleMatrix test1 = DoubleMatrix.of(2, 3, 1d, 2d, 3d, 4d, 5d, 6d);
    DoubleMatrix test2 = DoubleMatrix.of(2, 3, 0.5d, 0.6d, 0.7d, 0.5d, 0.6d, 0.7d);
    assertMatrix(test1.combine(test2, (a, b) -> a * b), 0.5d, 2d * 0.6d, 3d * 0.7d, 4d * 0.5d, 5d * 0.6d, 6d * 0.7d);
    assertThrows(() -> test1.combine(DoubleMatrix.EMPTY, (a, b) -> a * b), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_total() {
    assertEquals(DoubleMatrix.EMPTY.total(), 0d);
    assertEquals(DoubleMatrix.copyOf(new double[][] {{1d, 2d}, {3d, 4d}, {5d, 6d}}).total(), 21d);
  }

  public void test_reduce() {
    assertEquals(DoubleMatrix.EMPTY.reduce(2d, (r, v) -> {
      throw new AssertionError();
    }), 2d);
    assertEquals(DoubleMatrix.copyOf(new double[][] {{2d}}).reduce(1d, (r, v) -> r * v), 2d);
    assertEquals(DoubleMatrix.copyOf(new double[][] {{2d, 3d}}).reduce(1d, (r, v) -> r * v), 6d);
  }

  //-------------------------------------------------------------------------
  public void testTransposeMatrix() {
    DoubleMatrix m0 = DoubleMatrix.EMPTY;
    assertEquals(m0.transpose(), DoubleMatrix.EMPTY);

    DoubleMatrix m1 = DoubleMatrix.copyOf(new double[][] {
        {1, 2, 3},
        {4, 5, 6},
        {7, 8, 9}});
    assertEquals(m1.transpose(), DoubleMatrix.copyOf(new double[][] {
        {1, 4, 7},
        {2, 5, 8},
        {3, 6, 9}}));

    DoubleMatrix m2 = DoubleMatrix.copyOf(new double[][] {
        {1, 2, 3, 4, 5, 6},
        {7, 8, 9, 10, 11, 12},
        {13, 14, 15, 16, 17, 18}});
    assertEquals(m2.transpose(), DoubleMatrix.copyOf(new double[][] {
        {1, 7, 13},
        {2, 8, 14},
        {3, 9, 15},
        {4, 10, 16},
        {5, 11, 17},
        {6, 12, 18}}));
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    DoubleMatrix a1 = DoubleMatrix.copyOf(new double[][] {{2d, 3d}});
    DoubleMatrix a2 = DoubleMatrix.copyOf(new double[][] {{2d, 3d}});
    DoubleMatrix b = DoubleMatrix.copyOf(new double[][] {{3d, 3d}});
    DoubleMatrix c = DoubleMatrix.copyOf(new double[][] {{2d, 3d}, {4d, 5d}});
    DoubleMatrix d = DoubleMatrix.copyOf(new double[][] {{2d}});
    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(c), false);
    assertEquals(a1.equals(d), false);
    assertEquals(a1.equals(""), false);
    assertEquals(a1.equals(null), false);
    assertEquals(a1.hashCode(), a2.hashCode());
  }

  public void test_toString() {
    DoubleMatrix test = DoubleMatrix.copyOf(new double[][] {{1d, 2d}, {3d, 4d}, {5d, 6d}});
    assertEquals(test.toString(), "1.0 2.0\n3.0 4.0\n5.0 6.0\n");
  }

  //-------------------------------------------------------------------------
  private void assertMatrix(DoubleMatrix matrix, double... expected) {
    if (expected.length == 0) {
      assertSame(matrix, DoubleMatrix.EMPTY);
      assertEquals(matrix.isEmpty(), true);
    } else {
      assertEquals(matrix.size(), expected.length);
      int rowCount = matrix.rowCount();
      int colCount = matrix.columnCount();
      double[][] array = matrix.toArrayUnsafe();
      double[][] array2 = matrix.toArray();
      assertTrue(Arrays.deepEquals(array, array2));
      for (int i = 0; i < rowCount; i++) {
        for (int j = 0; j < colCount; j++) {
          assertEquals(matrix.get(i, j), expected[i * colCount + j]);
          assertEquals(array[i][j], expected[i * colCount + j]);
        }
      }
      assertEquals(matrix.dimensions(), 2);
      assertEquals(matrix.isEmpty(), false);
      assertEquals(matrix.isSquare(), matrix.rowCount() == matrix.columnCount());
    }
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(DoubleMatrix.EMPTY);
    coverImmutableBean(DoubleMatrix.of(2, 3, 1d, 2d, 3d, 4d, 5d, 6d));
  }

}
