/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.math.impl.matrix;

import static com.opengamma.strata.collect.DoubleArrayMath.EMPTY_DOUBLE_ARRAY;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link DoubleMatrix1D}.
 */
@Test
public class DoubleMatrix1DTest {

  public void test_EMPTY() {
    assertMatrix(DoubleMatrix1D.EMPTY);
  }

  public void test_of() {
    assertMatrix(DoubleMatrix1D.of());
    assertMatrix(DoubleMatrix1D.of(1d), 1d);
    assertMatrix(DoubleMatrix1D.of(1d, 2d), 1d, 2d);
    assertMatrix(DoubleMatrix1D.of(1d, 2d, 3d), 1d, 2d, 3d);
    assertMatrix(DoubleMatrix1D.of(1d, 2d, 3d, 4d), 1d, 2d, 3d, 4d);
    assertMatrix(DoubleMatrix1D.of(1d, 2d, 3d, 4d, 5d), 1d, 2d, 3d, 4d, 5d);
    assertMatrix(DoubleMatrix1D.of(1d, 2d, 3d, 4d, 5d, 6d), 1d, 2d, 3d, 4d, 5d, 6d);
    assertMatrix(DoubleMatrix1D.of(1d, 2d, 3d, 4d, 5d, 6d, 7d), 1d, 2d, 3d, 4d, 5d, 6d, 7d);
    assertMatrix(DoubleMatrix1D.of(1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d), 1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d);
  }

  public void test_of_lambda() {
    assertMatrix(DoubleMatrix1D.of(0, i -> {
      throw new AssertionError();
    }));
    AtomicInteger counter = new AtomicInteger(2);
    assertMatrix(DoubleMatrix1D.of(1, i -> counter.getAndIncrement()), 2d);
    assertMatrix(DoubleMatrix1D.of(2, i -> counter.getAndIncrement()), 3d, 4d);
  }

  public void test_ofUnsafe() {
    double[] base = {1d, 2d, 3d};
    DoubleMatrix1D test = DoubleMatrix1D.ofUnsafe(base);
    assertMatrix(test, 1d, 2d, 3d);
    base[0] = 4d;
    // internal state of object mutated - don't do this in application code!
    assertMatrix(test, 4d, 2d, 3d);
    // empty
    assertMatrix(DoubleMatrix1D.ofUnsafe(EMPTY_DOUBLE_ARRAY));
  }

  public void test_copyOf_List() {
    assertMatrix(DoubleMatrix1D.copyOf(ImmutableList.of(1d, 2d, 3d)), 1d, 2d, 3d);
    assertMatrix(DoubleMatrix1D.copyOf(ImmutableList.of()));
  }

  public void test_copyOf_array() {
    double[] base = new double[] {1d, 2d, 3d};
    DoubleMatrix1D test = DoubleMatrix1D.copyOf(base);
    assertMatrix(test, 1d, 2d, 3d);
    base[0] = 4d;
    // internal state of object is not mutated
    assertMatrix(test, 1d, 2d, 3d);
    // empty
    assertMatrix(DoubleMatrix1D.copyOf(EMPTY_DOUBLE_ARRAY));
  }

  public void test_copyOf_array_fromIndex() {
    assertMatrix(DoubleMatrix1D.copyOf(new double[] {1d, 2d, 3d}, 0), 1d, 2d, 3d);
    assertMatrix(DoubleMatrix1D.copyOf(new double[] {1d, 2d, 3d}, 1), 2d, 3d);
    assertMatrix(DoubleMatrix1D.copyOf(new double[] {1d, 2d, 3d}, 3));
    assertThrows(() -> DoubleMatrix1D.copyOf(new double[] {1d, 2d, 3d}, -1), IndexOutOfBoundsException.class);
    assertThrows(() -> DoubleMatrix1D.copyOf(new double[] {1d, 2d, 3d}, 4), IndexOutOfBoundsException.class);
  }

  public void test_copyOf_array_fromToIndex() {
    assertMatrix(DoubleMatrix1D.copyOf(new double[] {1d, 2d, 3d}, 0, 3), 1d, 2d, 3d);
    assertMatrix(DoubleMatrix1D.copyOf(new double[] {1d, 2d, 3d}, 1, 2), 2d);
    assertMatrix(DoubleMatrix1D.copyOf(new double[] {1d, 2d, 3d}, 1, 1));
    assertThrows(() -> DoubleMatrix1D.copyOf(new double[] {1d, 2d, 3d}, -1, 3), IndexOutOfBoundsException.class);
    assertThrows(() -> DoubleMatrix1D.copyOf(new double[] {1d, 2d, 3d}, 0, 5), IndexOutOfBoundsException.class);
  }

  public void test_filled() {
    assertMatrix(DoubleMatrix1D.filled(0));
    assertMatrix(DoubleMatrix1D.filled(3), 0d, 0d, 0d);
  }

  public void test_filled_withValue() {
    assertMatrix(DoubleMatrix1D.filled(0, 1.5));
    assertMatrix(DoubleMatrix1D.filled(3, 1.5), 1.5, 1.5, 1.5);
  }

  //-------------------------------------------------------------------------
  public void test_get() {
    DoubleMatrix1D test = DoubleMatrix1D.of(1d, 2d, 3d, 3d, 4d);
    assertEquals(test.get(0), 1d);
    assertEquals(test.get(4), 4d);
    assertThrows(() -> test.get(-1), IndexOutOfBoundsException.class);
    assertThrows(() -> test.get(5), IndexOutOfBoundsException.class);
  }

  public void test_contains() {
    DoubleMatrix1D test = DoubleMatrix1D.of(1d, 2d, 3d, 3d, 4d);
    assertEquals(test.contains(1d), true);
    assertEquals(test.contains(3d), true);
    assertEquals(test.contains(5d), false);
    assertEquals(DoubleMatrix1D.EMPTY.contains(5d), false);
  }

  public void test_indexOf() {
    DoubleMatrix1D test = DoubleMatrix1D.of(1d, 2d, 3d, 3d, 4d);
    assertEquals(test.indexOf(2d), 1);
    assertEquals(test.indexOf(3d), 2);
    assertEquals(test.indexOf(5d), -1);
    assertEquals(DoubleMatrix1D.EMPTY.indexOf(5d), -1);
  }

  public void test_lastIndexOf() {
    DoubleMatrix1D test = DoubleMatrix1D.of(1d, 2d, 3d, 3d, 4d);
    assertEquals(test.lastIndexOf(2d), 1);
    assertEquals(test.lastIndexOf(3d), 3);
    assertEquals(test.lastIndexOf(5d), -1);
    assertEquals(DoubleMatrix1D.EMPTY.lastIndexOf(5d), -1);
  }

  //-------------------------------------------------------------------------
  public void test_copyInto() {
    DoubleMatrix1D test = DoubleMatrix1D.of(1d, 2d, 3d);
    double[] dest = new double[4];
    test.copyInto(dest, 0);
    assertTrue(Arrays.equals(dest, new double[] {1d, 2d, 3d, 0d}));

    double[] dest2 = new double[4];
    test.copyInto(dest2, 1);
    assertTrue(Arrays.equals(dest2, new double[] {0d, 1d, 2d, 3d}));

    double[] dest3 = new double[4];
    assertThrows(() -> test.copyInto(dest3, 2), IndexOutOfBoundsException.class);
    assertThrows(() -> test.copyInto(dest3, -1), IndexOutOfBoundsException.class);
  }

  //-------------------------------------------------------------------------
  public void test_subArray_from() {
    DoubleMatrix1D test = DoubleMatrix1D.of(1d, 2d, 3d);
    assertMatrix(test.subArray(0), 1d, 2d, 3d);
    assertMatrix(test.subArray(1), 2d, 3d);
    assertMatrix(test.subArray(2), 3d);
    assertMatrix(test.subArray(3));
    assertThrows(() -> test.subArray(4), IndexOutOfBoundsException.class);
    assertThrows(() -> test.subArray(-1), IndexOutOfBoundsException.class);
  }

  public void test_subArray_fromTo() {
    DoubleMatrix1D test = DoubleMatrix1D.of(1d, 2d, 3d);
    assertMatrix(test.subArray(0, 3), 1d, 2d, 3d);
    assertMatrix(test.subArray(1, 3), 2d, 3d);
    assertMatrix(test.subArray(2, 3), 3d);
    assertMatrix(test.subArray(3, 3));
    assertMatrix(test.subArray(1, 2), 2d);
    assertThrows(() -> test.subArray(0, 4), IndexOutOfBoundsException.class);
    assertThrows(() -> test.subArray(-1, 3), IndexOutOfBoundsException.class);
  }

  //-------------------------------------------------------------------------
  public void test_toList() {
    DoubleMatrix1D test = DoubleMatrix1D.of(1d, 2d, 3d);
    List<Double> list = test.toList();
    assertMatrix(DoubleMatrix1D.copyOf(list), 1d, 2d, 3d);
    assertEquals(list.size(), 3);
    assertEquals(list.isEmpty(), false);
    assertEquals(list.get(0), 1d);
    assertEquals(list.get(2), 3d);
    assertEquals(list.contains(2d), true);
    assertEquals(list.contains(5d), false);
    assertEquals(list.contains(""), false);
    assertEquals(list.indexOf(2d), 1);
    assertEquals(list.indexOf(5d), -1);
    assertEquals(list.indexOf(""), -1);
    assertEquals(list.lastIndexOf(3d), 2);
    assertEquals(list.lastIndexOf(5d), -1);
    assertEquals(list.lastIndexOf(""), -1);

    assertThrows(() -> list.clear(), UnsupportedOperationException.class);
    assertThrows(() -> list.set(0, 3d), UnsupportedOperationException.class);
  }

  public void test_toList_iterator() {
    DoubleMatrix1D test = DoubleMatrix1D.of(1d, 2d, 3d);
    List<Double> list = test.toList();
    Iterator<Double> it = list.iterator();
    assertEquals(it.hasNext(), true);
    assertEquals(it.next(), 1d);
    assertEquals(it.hasNext(), true);
    assertEquals(it.next(), 2d);
    assertEquals(it.hasNext(), true);
    assertEquals(it.next(), 3d);
    assertEquals(it.hasNext(), false);

    assertThrows(() -> it.remove(), UnsupportedOperationException.class);
  }

  public void test_toList_listIterator() {
    DoubleMatrix1D test = DoubleMatrix1D.of(1d, 2d, 3d);
    List<Double> list = test.toList();
    ListIterator<Double> lit = list.listIterator();
    assertEquals(lit.nextIndex(), 0);
    assertEquals(lit.previousIndex(), -1);
    assertEquals(lit.hasNext(), true);
    assertEquals(lit.hasPrevious(), false);
    assertThrows(() -> lit.previous(), NoSuchElementException.class);

    assertEquals(lit.next(), 1d);
    assertEquals(lit.nextIndex(), 1);
    assertEquals(lit.previousIndex(), 0);
    assertEquals(lit.hasNext(), true);
    assertEquals(lit.hasPrevious(), true);

    assertEquals(lit.next(), 2d);
    assertEquals(lit.nextIndex(), 2);
    assertEquals(lit.previousIndex(), 1);
    assertEquals(lit.hasNext(), true);
    assertEquals(lit.hasPrevious(), true);

    assertEquals(lit.next(), 3d);
    assertEquals(lit.nextIndex(), 3);
    assertEquals(lit.previousIndex(), 2);
    assertEquals(lit.hasNext(), false);
    assertEquals(lit.hasPrevious(), true);
    assertThrows(() -> lit.next(), NoSuchElementException.class);

    assertEquals(lit.previous(), 3d);
    assertEquals(lit.nextIndex(), 2);
    assertEquals(lit.previousIndex(), 1);
    assertEquals(lit.hasNext(), true);
    assertEquals(lit.hasPrevious(), true);

    assertThrows(() -> lit.remove(), UnsupportedOperationException.class);
    assertThrows(() -> lit.set(2d), UnsupportedOperationException.class);
    assertThrows(() -> lit.add(2d), UnsupportedOperationException.class);
  }

  //-------------------------------------------------------------------------
  public void test_stream() {
    DoubleMatrix1D test = DoubleMatrix1D.of(1d, 2d, 3d);
    double[] streamed = test.stream().toArray();
    assertTrue(Arrays.equals(streamed, new double[] {1d, 2d, 3d}));
  }

  //-------------------------------------------------------------------------
  public void test_forEach() {
    DoubleMatrix1D test = DoubleMatrix1D.of(1d, 2d, 3d);
    double[] extracted = new double[3];
    test.forEach((i, v) -> extracted[i] = v);
    assertTrue(Arrays.equals(extracted, new double[] {1d, 2d, 3d}));
  }

  //-------------------------------------------------------------------------
  public void test_with() {
    DoubleMatrix1D test = DoubleMatrix1D.of(1d, 2d, 3d);
    assertMatrix(test.with(0, 2.6d), 2.6d, 2d, 3d);
    assertMatrix(test.with(0, 1d), 1d, 2d, 3d);
    assertThrows(() -> test.with(-1, 2d), IndexOutOfBoundsException.class);
    assertThrows(() -> test.with(3, 2d), IndexOutOfBoundsException.class);
  }

  //-------------------------------------------------------------------------
  public void test_multipliedBy() {
    DoubleMatrix1D test = DoubleMatrix1D.of(1d, 2d, 3d);
    assertMatrix(test.multipliedBy(5), 5d, 10d, 15d);
    assertMatrix(test.multipliedBy(1), 1d, 2d, 3d);
  }

  public void test_map() {
    DoubleMatrix1D test = DoubleMatrix1D.of(1d, 2d, 3d);
    assertMatrix(test.map(v -> 1 / v), 1d, 1d / 2d, 1d / 3d);
  }

  public void test_mapWithIndex() {
    DoubleMatrix1D test = DoubleMatrix1D.of(1d, 2d, 3d);
    assertMatrix(test.mapWithIndex((i, v) -> i * v), 0d, 2d, 6d);
  }

  //-------------------------------------------------------------------------
  public void test_plus() {
    DoubleMatrix1D test1 = DoubleMatrix1D.of(1d, 2d, 3d);
    DoubleMatrix1D test2 = DoubleMatrix1D.of(0.5d, 0.6d, 0.7d);
    assertMatrix(test1.plus(test2), 1.5d, 2.6d, 3.7d);
    assertThrows(() -> test1.plus(DoubleMatrix1D.EMPTY), IllegalArgumentException.class);
  }

  public void test_minus() {
    DoubleMatrix1D test1 = DoubleMatrix1D.of(1d, 2d, 3d);
    DoubleMatrix1D test2 = DoubleMatrix1D.of(0.5d, 0.6d, 0.7d);
    assertMatrix(test1.minus(test2), 0.5d, 1.4d, 2.3d);
    assertThrows(() -> test1.minus(DoubleMatrix1D.EMPTY), IllegalArgumentException.class);
  }

  public void test_combine() {
    DoubleMatrix1D test1 = DoubleMatrix1D.of(1d, 2d, 3d);
    DoubleMatrix1D test2 = DoubleMatrix1D.of(0.5d, 0.6d, 0.7d);
    assertMatrix(test1.combine(test2, (a, b) -> a * b), 0.5d, 2d * 0.6d, 3d * 0.7d);
    assertThrows(() -> test1.combine(DoubleMatrix1D.EMPTY, (a, b) -> a * b), IllegalArgumentException.class);
  }

  public void test_combineReduce() {
    DoubleMatrix1D test1 = DoubleMatrix1D.of(1d, 2d, 3d);
    DoubleMatrix1D test2 = DoubleMatrix1D.of(0.5d, 0.6d, 0.7d);
    assertEquals(test1.combineReduce(test2, (r, a, b) -> r + a * b), 0.5d + 2d * 0.6d + 3d * 0.7d);
    assertThrows(() -> test1.combineReduce(DoubleMatrix1D.EMPTY, (r, a, b) -> r + a * b), IllegalArgumentException.class);
  }

  //-------------------------------------------------------------------------
  public void test_sorted() {
    assertMatrix(DoubleMatrix1D.of().sorted());
    assertMatrix(DoubleMatrix1D.of(2d).sorted(), 2d);
    assertMatrix(DoubleMatrix1D.of(2d, 1d, 3d, 0d).sorted(), 0d, 1d, 2d, 3d);
  }

  //-------------------------------------------------------------------------
  public void test_min() {
    assertEquals(DoubleMatrix1D.of(2d).min(), 2d);
    assertEquals(DoubleMatrix1D.of(2d, 1d, 3d).min(), 1d);
    assertThrows(() -> DoubleMatrix1D.EMPTY.min(), IllegalStateException.class);
  }

  public void test_max() {
    assertEquals(DoubleMatrix1D.of(2d).max(), 2d);
    assertEquals(DoubleMatrix1D.of(2d, 1d, 3d).max(), 3d);
    assertThrows(() -> DoubleMatrix1D.EMPTY.max(), IllegalStateException.class);
  }

  public void test_total() {
    assertEquals(DoubleMatrix1D.EMPTY.total(), 0d);
    assertEquals(DoubleMatrix1D.of(2d).total(), 2d);
    assertEquals(DoubleMatrix1D.of(2d, 1d, 3d).total(), 6d);
  }

  public void test_reduce() {
    assertEquals(DoubleMatrix1D.EMPTY.reduce(2d, (r, v) -> {
      throw new AssertionError();
    }), 2d);
    assertEquals(DoubleMatrix1D.of(2d).reduce(1d, (r, v) -> r * v), 2d);
    assertEquals(DoubleMatrix1D.of(2d, 1d, 3d).reduce(1d, (r, v) -> r * v), 6d);
  }

  //-------------------------------------------------------------------------
  public void test_concat_array() {
    DoubleMatrix1D test1 = DoubleMatrix1D.of(1d, 2d, 3d);
    assertMatrix(test1.concat(new double[] {0.5d, 0.6d, 0.7d}), 1d, 2d, 3d, 0.5d, 0.6d, 0.7d);
    assertMatrix(test1.concat(EMPTY_DOUBLE_ARRAY), 1d, 2d, 3d);
    assertMatrix(DoubleMatrix1D.EMPTY.concat(new double[] {1d, 2d, 3d}), 1d, 2d, 3d);
  }

  public void test_concat_object() {
    DoubleMatrix1D test1 = DoubleMatrix1D.of(1d, 2d, 3d);
    DoubleMatrix1D test2 = DoubleMatrix1D.of(0.5d, 0.6d, 0.7d);
    assertMatrix(test1.concat(test2), 1d, 2d, 3d, 0.5d, 0.6d, 0.7d);
    assertMatrix(test1.concat(DoubleMatrix1D.EMPTY), 1d, 2d, 3d);
    assertMatrix(DoubleMatrix1D.EMPTY.concat(test1), 1d, 2d, 3d);
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    DoubleMatrix1D a1 = DoubleMatrix1D.of(1d, 2d);
    DoubleMatrix1D a2 = DoubleMatrix1D.of(1d, 2d);
    DoubleMatrix1D b = DoubleMatrix1D.of(1d, 2d, 3d);
    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(""), false);
    assertEquals(a1.equals(null), false);
    assertEquals(a1.hashCode(), a2.hashCode());
  }

  public void test_toString() {
    DoubleMatrix1D test = DoubleMatrix1D.of(1d, 2d);
    assertEquals(test.toString(), "[1.0, 2.0]");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(DoubleMatrix1D.of(1d, 2d, 3d));
    DoubleMatrix1D.of(1d, 2d, 3d).metaBean().metaProperty("array").metaBean();
    DoubleMatrix1D.of(1d, 2d, 3d).metaBean().metaProperty("array").propertyGenericType();
    DoubleMatrix1D.of(1d, 2d, 3d).metaBean().metaProperty("array").annotations();
  }

  //-------------------------------------------------------------------------
  private void assertMatrix(DoubleMatrix1D array, double... expected) {
    if (expected.length == 0) {
      assertSame(array, DoubleMatrix1D.EMPTY);
      assertEquals(array.isEmpty(), true);
    } else {
      assertEquals(array.size(), expected.length);
      assertTrue(Arrays.equals(array.toArray(), expected));
      assertTrue(Arrays.equals(array.toArrayUnsafe(), expected));
      assertEquals(array.dimensions(), 1);
      assertEquals(array.isEmpty(), false);
    }
  }

}
