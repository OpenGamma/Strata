/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.array;

import static com.opengamma.strata.collect.DoubleArrayMath.EMPTY_DOUBLE_ARRAY;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.within;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.DoubleStream;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link DoubleArray}.
 */
public class DoubleArrayTest {

  private static final Object ANOTHER_TYPE = "";

  private static final Offset<Double> DELTA = within(1e-14);

  @Test
  public void test_EMPTY() {
    assertContent(DoubleArray.EMPTY);
  }

  @Test
  public void test_of() {
    assertContent(DoubleArray.of());
    assertContent(DoubleArray.of(1d), 1d);
    assertContent(DoubleArray.of(1d, 2d), 1d, 2d);
    assertContent(DoubleArray.of(1d, 2d, 3d), 1d, 2d, 3d);
    assertContent(DoubleArray.of(1d, 2d, 3d, 4d), 1d, 2d, 3d, 4d);
    assertContent(DoubleArray.of(1d, 2d, 3d, 4d, 5d), 1d, 2d, 3d, 4d, 5d);
    assertContent(DoubleArray.of(1d, 2d, 3d, 4d, 5d, 6d), 1d, 2d, 3d, 4d, 5d, 6d);
    assertContent(DoubleArray.of(1d, 2d, 3d, 4d, 5d, 6d, 7d), 1d, 2d, 3d, 4d, 5d, 6d, 7d);
    assertContent(DoubleArray.of(1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d), 1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d);
    assertContent(DoubleArray.of(1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d), 1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d);
  }

  @Test
  public void test_of_lambda() {
    assertContent(DoubleArray.of(0, i -> {
      throw new AssertionError();
    }));
    AtomicInteger counter = new AtomicInteger(2);
    assertContent(DoubleArray.of(1, i -> counter.getAndIncrement()), 2d);
    assertContent(DoubleArray.of(2, i -> counter.getAndIncrement()), 3d, 4d);
  }

  @Test
  public void test_of_stream() {
    assertContent(DoubleArray.of(DoubleStream.empty()));
    assertContent(DoubleArray.of(DoubleStream.of(1d, 2d, 3d)), 1d, 2d, 3d);
  }

  @Test
  public void test_ofUnsafe() {
    double[] base = {1d, 2d, 3d};
    DoubleArray test = DoubleArray.ofUnsafe(base);
    assertContent(test, 1d, 2d, 3d);
    base[0] = 4d;
    // internal state of object mutated - don't do this in application code!
    assertContent(test, 4d, 2d, 3d);
    // empty
    assertContent(DoubleArray.ofUnsafe(EMPTY_DOUBLE_ARRAY));
  }

  @Test
  public void test_copyOf_List() {
    assertContent(DoubleArray.copyOf(ImmutableList.of(1d, 2d, 3d)), 1d, 2d, 3d);
    assertContent(DoubleArray.copyOf(ImmutableList.of()));
  }

  @Test
  public void test_copyOf_array() {
    double[] base = new double[] {1d, 2d, 3d};
    DoubleArray test = DoubleArray.copyOf(base);
    assertContent(test, 1d, 2d, 3d);
    base[0] = 4d;
    // internal state of object is not mutated
    assertContent(test, 1d, 2d, 3d);
    // empty
    assertContent(DoubleArray.copyOf(EMPTY_DOUBLE_ARRAY));
  }

  @Test
  public void test_copyOf_array_fromIndex() {
    assertContent(DoubleArray.copyOf(new double[] {1d, 2d, 3d}, 0), 1d, 2d, 3d);
    assertContent(DoubleArray.copyOf(new double[] {1d, 2d, 3d}, 1), 2d, 3d);
    assertContent(DoubleArray.copyOf(new double[] {1d, 2d, 3d}, 3));
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> DoubleArray.copyOf(new double[] {1d, 2d, 3d}, -1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> DoubleArray.copyOf(new double[] {1d, 2d, 3d}, 4));
  }

  @Test
  public void test_copyOf_array_fromToIndex() {
    assertContent(DoubleArray.copyOf(new double[] {1d, 2d, 3d}, 0, 3), 1d, 2d, 3d);
    assertContent(DoubleArray.copyOf(new double[] {1d, 2d, 3d}, 1, 2), 2d);
    assertContent(DoubleArray.copyOf(new double[] {1d, 2d, 3d}, 1, 1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> DoubleArray.copyOf(new double[] {1d, 2d, 3d}, -1, 3));
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> DoubleArray.copyOf(new double[] {1d, 2d, 3d}, 0, 5));
  }

  @Test
  public void test_filled() {
    assertContent(DoubleArray.filled(0));
    assertContent(DoubleArray.filled(3), 0d, 0d, 0d);
  }

  @Test
  public void test_filled_withValue() {
    assertContent(DoubleArray.filled(0, 1.5));
    assertContent(DoubleArray.filled(3, 1.5), 1.5, 1.5, 1.5);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_get() {
    DoubleArray test = DoubleArray.of(1d, 2d, 3d, 3d, 4d);
    assertThat(test.get(0)).isEqualTo(1d);
    assertThat(test.get(4)).isEqualTo(4d);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.get(-1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.get(5));
  }

  @Test
  public void test_contains() {
    DoubleArray test = DoubleArray.of(1d, 2d, 3d, 3d, 4d);
    assertThat(test.contains(1d)).isEqualTo(true);
    assertThat(test.contains(3d)).isEqualTo(true);
    assertThat(test.contains(5d)).isEqualTo(false);
    assertThat(DoubleArray.EMPTY.contains(5d)).isEqualTo(false);
  }

  @Test
  public void test_indexOf() {
    DoubleArray test = DoubleArray.of(1d, 2d, 3d, 3d, 4d);
    assertThat(test.indexOf(2d)).isEqualTo(1);
    assertThat(test.indexOf(3d)).isEqualTo(2);
    assertThat(test.indexOf(5d)).isEqualTo(-1);
    assertThat(DoubleArray.EMPTY.indexOf(5d)).isEqualTo(-1);
  }

  @Test
  public void test_lastIndexOf() {
    DoubleArray test = DoubleArray.of(1d, 2d, 3d, 3d, 4d);
    assertThat(test.lastIndexOf(2d)).isEqualTo(1);
    assertThat(test.lastIndexOf(3d)).isEqualTo(3);
    assertThat(test.lastIndexOf(5d)).isEqualTo(-1);
    assertThat(DoubleArray.EMPTY.lastIndexOf(5d)).isEqualTo(-1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_copyInto() {
    DoubleArray test = DoubleArray.of(1d, 2d, 3d);
    double[] dest = new double[4];
    test.copyInto(dest, 0);
    assertThat(dest).containsExactly(1d, 2d, 3d, 0d);

    double[] dest2 = new double[4];
    test.copyInto(dest2, 1);
    assertThat(dest2).containsExactly(0d, 1d, 2d, 3d);

    double[] dest3 = new double[4];
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.copyInto(dest3, 2));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.copyInto(dest3, -1));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_subArray_from() {
    DoubleArray test = DoubleArray.of(1d, 2d, 3d);
    assertContent(test.subArray(0), 1d, 2d, 3d);
    assertContent(test.subArray(1), 2d, 3d);
    assertContent(test.subArray(2), 3d);
    assertContent(test.subArray(3));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.subArray(4));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.subArray(-1));
  }

  @Test
  public void test_subArray_fromTo() {
    DoubleArray test = DoubleArray.of(1d, 2d, 3d);
    assertContent(test.subArray(0, 3), 1d, 2d, 3d);
    assertContent(test.subArray(1, 3), 2d, 3d);
    assertContent(test.subArray(2, 3), 3d);
    assertContent(test.subArray(3, 3));
    assertContent(test.subArray(1, 2), 2d);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.subArray(0, 4));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.subArray(-1, 3));
  }

  //-------------------------------------------------------------------------
  @SuppressWarnings("unlikely-arg-type")
  @Test
  public void test_toList() {
    DoubleArray test = DoubleArray.of(1d, 2d, 3d);
    List<Double> list = test.toList();
    assertContent(DoubleArray.copyOf(list), 1d, 2d, 3d);
    assertThat(list.size()).isEqualTo(3);
    assertThat(list.isEmpty()).isEqualTo(false);
    assertThat(list.get(0)).isEqualTo(1d);
    assertThat(list.get(2)).isEqualTo(3d);
    assertThat(list.contains(2d)).isEqualTo(true);
    assertThat(list.contains(5d)).isEqualTo(false);
    assertThat(list.contains("")).isEqualTo(false);
    assertThat(list.indexOf(2d)).isEqualTo(1);
    assertThat(list.indexOf(5d)).isEqualTo(-1);
    assertThat(list.indexOf("")).isEqualTo(-1);
    assertThat(list.lastIndexOf(3d)).isEqualTo(2);
    assertThat(list.lastIndexOf(5d)).isEqualTo(-1);
    assertThat(list.lastIndexOf("")).isEqualTo(-1);

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.clear());
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.set(0, 3d));
  }

  @Test
  public void test_toList_iterator() {
    DoubleArray test = DoubleArray.of(1d, 2d, 3d);
    List<Double> list = test.toList();
    Iterator<Double> it = list.iterator();
    assertThat(it.hasNext()).isEqualTo(true);
    assertThat(it.next()).isEqualTo(1d);
    assertThat(it.hasNext()).isEqualTo(true);
    assertThat(it.next()).isEqualTo(2d);
    assertThat(it.hasNext()).isEqualTo(true);
    assertThat(it.next()).isEqualTo(3d);
    assertThat(it.hasNext()).isEqualTo(false);

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> it.remove());
  }

  @Test
  public void test_toList_listIterator() {
    DoubleArray test = DoubleArray.of(1d, 2d, 3d);
    List<Double> list = test.toList();
    ListIterator<Double> lit = list.listIterator();
    assertThat(lit.nextIndex()).isEqualTo(0);
    assertThat(lit.previousIndex()).isEqualTo(-1);
    assertThat(lit.hasNext()).isEqualTo(true);
    assertThat(lit.hasPrevious()).isEqualTo(false);
    assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> lit.previous());

    assertThat(lit.next()).isEqualTo(1d);
    assertThat(lit.nextIndex()).isEqualTo(1);
    assertThat(lit.previousIndex()).isEqualTo(0);
    assertThat(lit.hasNext()).isEqualTo(true);
    assertThat(lit.hasPrevious()).isEqualTo(true);

    assertThat(lit.next()).isEqualTo(2d);
    assertThat(lit.nextIndex()).isEqualTo(2);
    assertThat(lit.previousIndex()).isEqualTo(1);
    assertThat(lit.hasNext()).isEqualTo(true);
    assertThat(lit.hasPrevious()).isEqualTo(true);

    assertThat(lit.next()).isEqualTo(3d);
    assertThat(lit.nextIndex()).isEqualTo(3);
    assertThat(lit.previousIndex()).isEqualTo(2);
    assertThat(lit.hasNext()).isEqualTo(false);
    assertThat(lit.hasPrevious()).isEqualTo(true);
    assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> lit.next());

    assertThat(lit.previous()).isEqualTo(3d);
    assertThat(lit.nextIndex()).isEqualTo(2);
    assertThat(lit.previousIndex()).isEqualTo(1);
    assertThat(lit.hasNext()).isEqualTo(true);
    assertThat(lit.hasPrevious()).isEqualTo(true);

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> lit.remove());
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> lit.set(2d));
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> lit.add(2d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_stream() {
    DoubleArray test = DoubleArray.of(1d, 2d, 3d);
    double[] streamed = test.stream().toArray();
    assertThat(streamed).containsExactly(1d, 2d, 3d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forEach() {
    DoubleArray test = DoubleArray.of(1d, 2d, 3d);
    double[] extracted = new double[3];
    test.forEach((i, v) -> extracted[i] = v);
    assertThat(extracted).containsExactly(1d, 2d, 3d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_with() {
    DoubleArray test = DoubleArray.of(1d, 2d, 3d);
    assertContent(test.with(0, 2.6d), 2.6d, 2d, 3d);
    assertContent(test.with(0, 1d), 1d, 2d, 3d);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.with(-1, 2d));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.with(3, 2d));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_plus() {
    DoubleArray test = DoubleArray.of(1d, 2d, 3d);
    assertContent(test.plus(5), 6d, 7d, 8d);
    assertContent(test.plus(0), 1d, 2d, 3d);
    assertContent(test.plus(-5), -4d, -3d, -2d);
  }

  @Test
  public void test_minus() {
    DoubleArray test = DoubleArray.of(1d, 2d, 3d);
    assertContent(test.minus(5), -4d, -3d, -2d);
    assertContent(test.minus(0), 1d, 2d, 3d);
    assertContent(test.minus(-5), 6d, 7d, 8d);
  }

  @Test
  public void test_multipliedBy() {
    DoubleArray test = DoubleArray.of(1d, 2d, 3d);
    assertContent(test.multipliedBy(5), 5d, 10d, 15d);
    assertContent(test.multipliedBy(1), 1d, 2d, 3d);
  }

  @Test
  public void test_dividedBy() {
    DoubleArray test = DoubleArray.of(10d, 20d, 30d);
    assertContent(test.dividedBy(5), 2d, 4d, 6d);
    assertContent(test.dividedBy(1), 10d, 20d, 30d);
  }

  @Test
  public void test_map() {
    DoubleArray test = DoubleArray.of(1d, 2d, 3d);
    assertContent(test.map(v -> 1 / v), 1d, 1d / 2d, 1d / 3d);
  }

  @Test
  public void test_mapWithIndex() {
    DoubleArray test = DoubleArray.of(1d, 2d, 3d);
    assertContent(test.mapWithIndex((i, v) -> i * v), 0d, 2d, 6d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_plus_array() {
    DoubleArray test1 = DoubleArray.of(1d, 2d, 3d);
    DoubleArray test2 = DoubleArray.of(0.5d, 0.6d, 0.7d);
    assertContent(test1.plus(test2), 1.5d, 2.6d, 3.7d);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.plus(DoubleArray.EMPTY));
  }

  @Test
  public void test_minus_array() {
    DoubleArray test1 = DoubleArray.of(1d, 2d, 3d);
    DoubleArray test2 = DoubleArray.of(0.5d, 0.6d, 0.7d);
    assertContent(test1.minus(test2), 0.5d, 1.4d, 2.3d);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.minus(DoubleArray.EMPTY));
  }

  @Test
  public void test_multipliedBy_array() {
    DoubleArray test1 = DoubleArray.of(1d, 2d, 3d);
    DoubleArray test2 = DoubleArray.of(0.5d, 0.6d, 0.7d);
    assertContent(test1.multipliedBy(test2), 0.5d, 1.2d, 2.1d);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.multipliedBy(DoubleArray.EMPTY));
  }

  @Test
  public void test_dividedBy_array() {
    DoubleArray test1 = DoubleArray.of(10d, 20d, 30d);
    DoubleArray test2 = DoubleArray.of(2d, 5d, 10d);
    assertContent(test1.dividedBy(test2), 5d, 4d, 3d);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.dividedBy(DoubleArray.EMPTY));
  }

  @Test
  public void test_combine() {
    DoubleArray test1 = DoubleArray.of(1d, 2d, 3d);
    DoubleArray test2 = DoubleArray.of(0.5d, 0.6d, 0.7d);
    assertContent(test1.combine(test2, (a, b) -> a * b), 0.5d, 2d * 0.6d, 3d * 0.7d);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test1.combine(DoubleArray.EMPTY, (a, b) -> a * b));
  }

  @Test
  public void test_combineReduce() {
    DoubleArray test1 = DoubleArray.of(1d, 2d, 3d);
    DoubleArray test2 = DoubleArray.of(0.5d, 0.6d, 0.7d);
    assertThat(test1.combineReduce(test2, (r, a, b) -> r + a * b)).isEqualTo(0.5d + 2d * 0.6d + 3d * 0.7d);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test1.combineReduce(DoubleArray.EMPTY, (r, a, b) -> r + a * b));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_sorted() {
    assertContent(DoubleArray.of().sorted());
    assertContent(DoubleArray.of(2d).sorted(), 2d);
    assertContent(DoubleArray.of(2d, 1d, 3d, 0d).sorted(), 0d, 1d, 2d, 3d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_min() {
    assertThat(DoubleArray.of(2d).min()).isEqualTo(2d);
    assertThat(DoubleArray.of(2d, 1d, 3d).min()).isEqualTo(1d);
    assertThatIllegalStateException().isThrownBy(() -> DoubleArray.EMPTY.min());
  }

  @Test
  public void test_max() {
    assertThat(DoubleArray.of(2d).max()).isEqualTo(2d);
    assertThat(DoubleArray.of(2d, 1d, 3d).max()).isEqualTo(3d);
    assertThatIllegalStateException().isThrownBy(() -> DoubleArray.EMPTY.max());
  }

  @Test
  public void test_sum() {
    assertThat(DoubleArray.EMPTY.sum()).isEqualTo(0d);
    assertThat(DoubleArray.of(2d).sum()).isEqualTo(2d);
    assertThat(DoubleArray.of(2d, 1d, 3d).sum()).isEqualTo(6d);
  }

  @Test
  public void test_reduce() {
    assertThat(DoubleArray.EMPTY.reduce(2d, (r, v) -> {
      throw new AssertionError();
    })).isEqualTo(2d);
    assertThat(DoubleArray.of(2d).reduce(1d, (r, v) -> r * v)).isEqualTo(2d);
    assertThat(DoubleArray.of(2d, 1d, 3d).reduce(1d, (r, v) -> r * v)).isEqualTo(6d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_concat_varargs() {
    DoubleArray test1 = DoubleArray.of(1d, 2d, 3d);
    assertContent(test1.concat(0.5d, 0.6d, 0.7d), 1d, 2d, 3d, 0.5d, 0.6d, 0.7d);
    assertContent(test1.concat(new double[] {0.5d, 0.6d, 0.7d}), 1d, 2d, 3d, 0.5d, 0.6d, 0.7d);
    assertContent(test1.concat(EMPTY_DOUBLE_ARRAY), 1d, 2d, 3d);
    assertContent(DoubleArray.EMPTY.concat(new double[] {1d, 2d, 3d}), 1d, 2d, 3d);
  }

  @Test
  public void test_concat_object() {
    DoubleArray test1 = DoubleArray.of(1d, 2d, 3d);
    DoubleArray test2 = DoubleArray.of(0.5d, 0.6d, 0.7d);
    assertContent(test1.concat(test2), 1d, 2d, 3d, 0.5d, 0.6d, 0.7d);
    assertContent(test1.concat(DoubleArray.EMPTY), 1d, 2d, 3d);
    assertContent(DoubleArray.EMPTY.concat(test1), 1d, 2d, 3d);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalWithTolerance() {
    DoubleArray a1 = DoubleArray.of(1d, 2d);
    DoubleArray a2 = DoubleArray.of(1d, 2.02d);
    DoubleArray a3 = DoubleArray.of(1d, 2.009d);
    DoubleArray b = DoubleArray.of(1d, 2d, 3d);
    assertThat(a1.equalWithTolerance(a2, 0.01d)).isFalse();
    assertThat(a1.equalWithTolerance(a3, 0.01d)).isTrue();
    assertThat(a1.equalWithTolerance(b, 0.01d)).isFalse();
  }

  @Test
  public void test_equalZeroWithTolerance() {
    DoubleArray a1 = DoubleArray.of(0d, 0d);
    DoubleArray a2 = DoubleArray.of(0d, 0.02d);
    DoubleArray a3 = DoubleArray.of(0d, 0.009d);
    DoubleArray b = DoubleArray.of(1d, 2d, 3d);
    assertThat(a1.equalZeroWithTolerance(0.01d)).isEqualTo(true);
    assertThat(a2.equalZeroWithTolerance(0.01d)).isEqualTo(false);
    assertThat(a3.equalZeroWithTolerance(0.01d)).isEqualTo(true);
    assertThat(b.equalZeroWithTolerance(0.01d)).isEqualTo(false);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCode() {
    DoubleArray a1 = DoubleArray.of(1d, 2d);
    DoubleArray a2 = DoubleArray.of(1d, 2d);
    DoubleArray b = DoubleArray.of(1d, 2d, 3d);
    assertThat(a1.equals(a1)).isEqualTo(true);
    assertThat(a1.equals(a2)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(ANOTHER_TYPE)).isEqualTo(false);
    assertThat(a1.equals(null)).isEqualTo(false);
    assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
  }

  @Test
  public void test_toString() {
    DoubleArray test = DoubleArray.of(1d, 2d);
    assertThat(test.toString()).isEqualTo("[1.0, 2.0]");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(DoubleArray.of(1d, 2d, 3d));
    DoubleArray.of(1d, 2d, 3d).metaBean().metaProperty("array").metaBean();
    DoubleArray.of(1d, 2d, 3d).metaBean().metaProperty("array").propertyGenericType();
    DoubleArray.of(1d, 2d, 3d).metaBean().metaProperty("array").annotations();
  }

  //-------------------------------------------------------------------------
  private void assertContent(DoubleArray array, double... expected) {
    if (expected.length == 0) {
      assertThat(array).isSameAs(DoubleArray.EMPTY);
      assertThat(array.isEmpty()).isEqualTo(true);
    } else {
      assertThat(array.size()).isEqualTo(expected.length);
      assertArray(array.toArray(), expected);
      assertArray(array.toArrayUnsafe(), expected);
      assertThat(array.dimensions()).isEqualTo(1);
      assertThat(array.isEmpty()).isEqualTo(false);
    }
  }

  private void assertArray(double[] array, double[] expected) {
    assertThat(array.length).isEqualTo(expected.length);

    for (int i = 0; i < array.length; i++) {
      assertThat(array[i])
          .withFailMessage("Unexpected value at index " + i + ",")
          .isEqualTo(expected[i], DELTA);
    }
  }
}
