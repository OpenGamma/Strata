/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

/**
 * Test {@link DoubleMatrix2D}.
 */
@Test
public class DoubleMatrix2DTest {

  public void test_EMPTY() {
    assertMatrix(DoubleMatrix2D.EMPTY);
  }

  public void test_of() {
    assertMatrix(DoubleMatrix2D.of());
  }

  public void test_of_values() {
    assertMatrix(DoubleMatrix2D.of(2, 3, 1d, 2d, 3d, 4d, 5d, 6d), 1d, 2d, 3d, 4d, 5d, 6d);
    assertMatrix(DoubleMatrix2D.of(6, 1, 1d, 2d, 3d, 4d, 5d, 6d), 1d, 2d, 3d, 4d, 5d, 6d);
  }

  //-------------------------------------------------------------------------
  public void test_of_intintlambda() {
    assertMatrix(DoubleMatrix2D.of(0, 0, (i, j) -> {
      throw new AssertionError();
    }));
    assertMatrix(DoubleMatrix2D.of(0, 2, (i, j) -> {
      throw new AssertionError();
    }));
    assertMatrix(DoubleMatrix2D.of(2, 0, (i, j) -> {
      throw new AssertionError();
    }));
    AtomicInteger counter = new AtomicInteger(2);
    assertMatrix(DoubleMatrix2D.of(1, 2, (i, j) -> counter.getAndIncrement()), 2d, 3d);
    assertMatrix(DoubleMatrix2D.of(2, 2, (i, j) -> (i + 1) * (j + 1)), 1d, 2d, 2d, 4d);
  }

  public void test_ofArrayObjects() {
    assertMatrix(DoubleMatrix2D.ofArrayObjects(0, 0, i -> {
      throw new AssertionError();
    }));
    assertMatrix(DoubleMatrix2D.ofArrayObjects(0, 2, i -> {
      throw new AssertionError();
    }));
    assertMatrix(DoubleMatrix2D.ofArrayObjects(2, 0, i -> {
      throw new AssertionError();
    }));
    AtomicInteger counter = new AtomicInteger(2);
    assertMatrix(DoubleMatrix2D.ofArrayObjects(1, 2,
        i -> DoubleMatrix1D.of(counter.getAndIncrement(), counter.getAndIncrement())), 2d, 3d);
    assertThrowsIllegalArg(() -> DoubleMatrix2D.ofArrayObjects(1, 2, i -> DoubleMatrix1D.EMPTY));
  }

  public void test_ofArrays() {
    assertMatrix(DoubleMatrix2D.ofArrays(0, 0, i -> {
      throw new AssertionError();
    }));
    assertMatrix(DoubleMatrix2D.ofArrays(0, 2, i -> {
      throw new AssertionError();
    }));
    assertMatrix(DoubleMatrix2D.ofArrays(2, 0, i -> {
      throw new AssertionError();
    }));
    AtomicInteger counter = new AtomicInteger(2);
    assertMatrix(DoubleMatrix2D.ofArrays(1, 2,
        i -> new double[] {counter.getAndIncrement(), counter.getAndIncrement()}), 2d, 3d);
    assertThrowsIllegalArg(() -> DoubleMatrix2D.ofArrays(1, 2, i -> new double[0]));
  }

  public void test_ofUnsafe() {
    double[][] base = { {1d, 2d}, {3d, 4d}};
    DoubleMatrix2D test = DoubleMatrix2D.ofUnsafe(base);
    assertMatrix(test, 1d, 2d, 3d, 4d);
    base[0][0] = 7d;
    // internal state of object mutated - don't do this in application code!
    assertMatrix(test, 7d, 2d, 3d, 4d);
    // empty
    assertMatrix(DoubleMatrix2D.ofUnsafe(new double[0][0]));
    assertMatrix(DoubleMatrix2D.ofUnsafe(new double[0][2]));
    assertMatrix(DoubleMatrix2D.ofUnsafe(new double[2][0]));
  }

  public void test_copyOf_array() {
    double[][] base = { {1d, 2d}, {3d, 4d}};
    DoubleMatrix2D test = DoubleMatrix2D.copyOf(base);
    assertMatrix(test, 1d, 2d, 3d, 4d);
    base[0][0] = 7d;
    // internal state of object is not mutated
    assertMatrix(test, 1d, 2d, 3d, 4d);
    // empty
    assertMatrix(DoubleMatrix2D.copyOf(new double[0][0]));
    assertMatrix(DoubleMatrix2D.copyOf(new double[0][2]));
    assertMatrix(DoubleMatrix2D.copyOf(new double[2][0]));
  }

  //-------------------------------------------------------------------------
  public void test_filled() {
    assertMatrix(DoubleMatrix2D.filled(0, 0));
    assertMatrix(DoubleMatrix2D.filled(0, 2));
    assertMatrix(DoubleMatrix2D.filled(2, 0));
    assertMatrix(DoubleMatrix2D.filled(3, 2), 0d, 0d, 0d, 0d, 0d, 0d);
  }

  public void test_filled_withValue() {
    assertMatrix(DoubleMatrix2D.filled(0, 0, 7d));
    assertMatrix(DoubleMatrix2D.filled(0, 2, 7d));
    assertMatrix(DoubleMatrix2D.filled(2, 0, 7d));
    assertMatrix(DoubleMatrix2D.filled(3, 2, 7d), 7d, 7d, 7d, 7d, 7d, 7d);
  }

  //-------------------------------------------------------------------------
  public void test_get() {
    double[][] base = { {1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix2D test = DoubleMatrix2D.copyOf(base);
    assertEquals(test.get(0, 0), 1d);
    assertEquals(test.get(2, 1), 6d);
    assertThrows(() -> test.get(-1, 0), IndexOutOfBoundsException.class);
    assertThrows(() -> test.get(0, 4), IndexOutOfBoundsException.class);
  }

  public void test_row() {
    double[][] base = { {1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix2D test = DoubleMatrix2D.copyOf(base);
    assertEquals(test.row(0), DoubleMatrix1D.of(1d, 2d));
    assertEquals(test.row(1), DoubleMatrix1D.of(3d, 4d));
    assertEquals(test.row(2), DoubleMatrix1D.of(5d, 6d));
    assertThrows(() -> test.row(-1), IndexOutOfBoundsException.class);
    assertThrows(() -> test.row(4), IndexOutOfBoundsException.class);
  }

  public void test_rowArray() {
    double[][] base = { {1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix2D test = DoubleMatrix2D.copyOf(base);
    assertEquals(test.rowArray(0), new double[] {1d, 2d});
    assertEquals(test.rowArray(1), new double[] {3d, 4d});
    assertEquals(test.rowArray(2), new double[] {5d, 6d});
    assertThrows(() -> test.rowArray(-1), IndexOutOfBoundsException.class);
    assertThrows(() -> test.rowArray(4), IndexOutOfBoundsException.class);
  }

  public void test_column() {
    double[][] base = { {1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix2D test = DoubleMatrix2D.copyOf(base);
    assertEquals(test.column(0), DoubleMatrix1D.of(1d, 3d, 5d));
    assertEquals(test.column(1), DoubleMatrix1D.of(2d, 4d, 6d));
    assertThrows(() -> test.column(-1), IndexOutOfBoundsException.class);
    assertThrows(() -> test.column(4), IndexOutOfBoundsException.class);
  }

  public void test_columnArray() {
    double[][] base = { {1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix2D test = DoubleMatrix2D.copyOf(base);
    assertEquals(test.columnArray(0), new double[] {1d, 3d, 5d});
    assertEquals(test.columnArray(1), new double[] {2d, 4d, 6d});
    assertThrows(() -> test.columnArray(-1), IndexOutOfBoundsException.class);
    assertThrows(() -> test.columnArray(4), IndexOutOfBoundsException.class);
  }

  //-------------------------------------------------------------------------
  public void test_forEach() {
    double[][] base = { {1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix2D test = DoubleMatrix2D.copyOf(base);
    double[] extracted = new double[6];
    test.forEach((i, j, v) -> extracted[i * 2 + j] = v);
    assertTrue(Arrays.equals(extracted, new double[] {1d, 2d, 3d, 4d, 5d, 6d}));
  }

  //-------------------------------------------------------------------------
  public void test_with() {
    double[][] base = { {1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix2D test = DoubleMatrix2D.copyOf(base);
    assertMatrix(test.with(0, 0, 2.6d), 2.6d, 2d, 3d, 4d, 5d, 6d);
    assertMatrix(test.with(0, 0, 1d), 1d, 2d, 3d, 4d, 5d, 6d);
    assertThrows(() -> test.with(-1, 0, 2d), IndexOutOfBoundsException.class);
    assertThrows(() -> test.with(3, 0, 2d), IndexOutOfBoundsException.class);
    assertThrows(() -> test.with(0, -1, 2d), IndexOutOfBoundsException.class);
    assertThrows(() -> test.with(0, 3, 2d), IndexOutOfBoundsException.class);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    double[][] base = { {1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix2D test = DoubleMatrix2D.copyOf(base);
    assertMatrix(test.multipliedBy(5), 5d, 10d, 15d, 20d, 25d, 30d);
    assertMatrix(test.multipliedBy(1), 1d, 2d, 3d, 4d, 5d, 6d);
  }

  public void test_map() {
    double[][] base = { {1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix2D test = DoubleMatrix2D.copyOf(base);
    assertMatrix(test.map(v -> 1 / v), 1d, 1d / 2d, 1d / 3d, 1d / 4d, 1d / 5d, 1d / 6d);
  }

  public void test_mapWithIndex() {
    double[][] base = { {1d, 2d}, {3d, 4d}, {5d, 6d}};
    DoubleMatrix2D test = DoubleMatrix2D.copyOf(base);
    assertMatrix(test.mapWithIndex((i, j, v) -> i * (j + 1) * v), 0d, 0d, 3d, 8d, 10d, 24d);
  }

  //-------------------------------------------------------------------------
  public void test_plus() {
    DoubleMatrix2D test1 = DoubleMatrix2D.of(2, 3, 1d, 2d, 3d, 4d, 5d, 6d);
    DoubleMatrix2D test2 = DoubleMatrix2D.of(2, 3, 0.5d, 0.6d, 0.7d, 0.5d, 0.6d, 0.7d);
    assertMatrix(test1.plus(test2), 1.5d, 2.6d, 3.7d, 4.5d, 5.6d, 6.7d);
    assertThrows(() -> test1.plus(DoubleMatrix2D.EMPTY), IllegalArgumentException.class);
  }

  public void test_minus() {
    DoubleMatrix2D test1 = DoubleMatrix2D.of(2, 3, 1d, 2d, 3d, 4d, 5d, 6d);
    DoubleMatrix2D test2 = DoubleMatrix2D.of(2, 3, 0.5d, 0.6d, 0.7d, 0.5d, 0.6d, 0.7d);
    assertMatrix(test1.minus(test2), 0.5d, 1.4d, 2.3d, 3.5d, 4.4d, 5.3d);
    assertThrows(() -> test1.minus(DoubleMatrix2D.EMPTY), IllegalArgumentException.class);
  }

  public void test_combine() {
    DoubleMatrix2D test1 = DoubleMatrix2D.of(2, 3, 1d, 2d, 3d, 4d, 5d, 6d);
    DoubleMatrix2D test2 = DoubleMatrix2D.of(2, 3, 0.5d, 0.6d, 0.7d, 0.5d, 0.6d, 0.7d);
    assertMatrix(test1.combine(test2, (a, b) -> a * b), 0.5d, 2d * 0.6d, 3d * 0.7d, 4d * 0.5d, 5d * 0.6d, 6d * 0.7d);
    assertThrows(() -> test1.combine(DoubleMatrix2D.EMPTY, (a, b) -> a * b), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_total() {
    assertEquals(DoubleMatrix2D.EMPTY.total(), 0d);
    assertEquals(DoubleMatrix2D.copyOf(new double[][] { {1d, 2d}, {3d, 4d}, {5d, 6d}}).total(), 21d);
  }

  public void test_reduce() {
    assertEquals(DoubleMatrix2D.EMPTY.reduce(2d, (r, v) -> {
      throw new AssertionError();
    }), 2d);
    assertEquals(DoubleMatrix2D.copyOf(new double[][] {{2d}}).reduce(1d, (r, v) -> r * v), 2d);
    assertEquals(DoubleMatrix2D.copyOf(new double[][] {{2d, 3d}}).reduce(1d, (r, v) -> r * v), 6d);
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    DoubleMatrix2D a1 = DoubleMatrix2D.copyOf(new double[][] {{2d, 3d}});
    DoubleMatrix2D a2 = DoubleMatrix2D.copyOf(new double[][] {{2d, 3d}});
    DoubleMatrix2D b = DoubleMatrix2D.copyOf(new double[][] {{3d, 3d}});
    DoubleMatrix2D c = DoubleMatrix2D.copyOf(new double[][] { {2d, 3d}, {4d, 5d}});
    DoubleMatrix2D d = DoubleMatrix2D.copyOf(new double[][] {{2d}});
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
    DoubleMatrix2D test = DoubleMatrix2D.copyOf(new double[][] { {1d, 2d}, {3d, 4d}, {5d, 6d}});
    assertEquals(test.toString(), "1.0 2.0\n3.0 4.0\n5.0 6.0\n");
  }

  //-------------------------------------------------------------------------
  private void assertMatrix(DoubleMatrix2D matrix, double... expected) {
    if (expected.length == 0) {
      assertSame(matrix, DoubleMatrix2D.EMPTY);
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
    }
  }

}
