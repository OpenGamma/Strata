/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.array;

import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link IntArray}.
 */
public class IntArrayTest {

  private static final int[] EMPTY_INT_ARRAY = new int[0];
  private static final Object ANOTHER_TYPE = "";

  @Test
  public void test_EMPTY() {
    assertContent(IntArray.EMPTY);
  }

  @Test
  public void test_of() {
    assertContent(IntArray.of());
    assertContent(IntArray.of(1), 1);
    assertContent(IntArray.of(1, 2), 1, 2);
    assertContent(IntArray.of(1, 2, 3), 1, 2, 3);
    assertContent(IntArray.of(1, 2, 3, 4), 1, 2, 3, 4);
    assertContent(IntArray.of(1, 2, 3, 4, 5), 1, 2, 3, 4, 5);
    assertContent(IntArray.of(1, 2, 3, 4, 5, 6), 1, 2, 3, 4, 5, 6);
    assertContent(IntArray.of(1, 2, 3, 4, 5, 6, 7), 1, 2, 3, 4, 5, 6, 7);
    assertContent(IntArray.of(1, 2, 3, 4, 5, 6, 7, 8), 1, 2, 3, 4, 5, 6, 7, 8);
    assertContent(IntArray.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 1, 2, 3, 4, 5, 6, 7, 8, 9);
  }

  @Test
  public void test_of_lambda() {
    assertContent(IntArray.of(0, i -> {
      throw new AssertionError();
    }));
    AtomicInteger counter = new AtomicInteger(2);
    assertContent(IntArray.of(1, i -> counter.getAndIncrement()), 2);
    assertContent(IntArray.of(2, i -> counter.getAndIncrement()), 3, 4);
  }

  @Test
  public void test_of_stream() {
    assertContent(IntArray.of(IntStream.empty()));
    assertContent(IntArray.of(IntStream.of(1, 2, 3)), 1, 2, 3);
  }

  @Test
  public void test_ofUnsafe() {
    int[] base = {1, 2, 3};
    IntArray test = IntArray.ofUnsafe(base);
    assertContent(test, 1, 2, 3);
    base[0] = 4;
    // internal state of object mutated - don't do this in application code!
    assertContent(test, 4, 2, 3);
    // empty
    assertContent(IntArray.ofUnsafe(EMPTY_INT_ARRAY));
  }

  @Test
  public void test_copyOf_List() {
    assertContent(IntArray.copyOf(ImmutableList.of(1, 2, 3)), 1, 2, 3);
    assertContent(IntArray.copyOf(ImmutableList.of()));
  }

  @Test
  public void test_copyOf_array() {
    int[] base = new int[] {1, 2, 3};
    IntArray test = IntArray.copyOf(base);
    assertContent(test, 1, 2, 3);
    base[0] = 4;
    // internal state of object is not mutated
    assertContent(test, 1, 2, 3);
    // empty
    assertContent(IntArray.copyOf(EMPTY_INT_ARRAY));
  }

  @Test
  public void test_copyOf_array_fromIndex() {
    assertContent(IntArray.copyOf(new int[] {1, 2, 3}, 0), 1, 2, 3);
    assertContent(IntArray.copyOf(new int[] {1, 2, 3}, 1), 2, 3);
    assertContent(IntArray.copyOf(new int[] {1, 2, 3}, 3));
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> IntArray.copyOf(new int[] {1, 2, 3}, -1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> IntArray.copyOf(new int[] {1, 2, 3}, 4));
  }

  @Test
  public void test_copyOf_array_fromToIndex() {
    assertContent(IntArray.copyOf(new int[] {1, 2, 3}, 0, 3), 1, 2, 3);
    assertContent(IntArray.copyOf(new int[] {1, 2, 3}, 1, 2), 2);
    assertContent(IntArray.copyOf(new int[] {1, 2, 3}, 1, 1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> IntArray.copyOf(new int[] {1, 2, 3}, -1, 3));
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> IntArray.copyOf(new int[] {1, 2, 3}, 0, 5));
  }

  @Test
  public void test_filled() {
    assertContent(IntArray.filled(0));
    assertContent(IntArray.filled(3), 0, 0, 0);
  }

  @Test
  public void test_filled_withValue() {
    assertContent(IntArray.filled(0, 1));
    assertContent(IntArray.filled(3, 1), 1, 1, 1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_get() {
    IntArray test = IntArray.of(1, 2, 3, 3, 4);
    assertThat(test.get(0)).isEqualTo(1);
    assertThat(test.get(4)).isEqualTo(4);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.get(-1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.get(5));
  }

  @Test
  public void test_contains() {
    IntArray test = IntArray.of(1, 2, 3, 3, 4);
    assertThat(test.contains(1)).isEqualTo(true);
    assertThat(test.contains(3)).isEqualTo(true);
    assertThat(test.contains(5)).isEqualTo(false);
    assertThat(IntArray.EMPTY.contains(5)).isEqualTo(false);
  }

  @Test
  public void test_indexOf() {
    IntArray test = IntArray.of(1, 2, 3, 3, 4);
    assertThat(test.indexOf(2)).isEqualTo(1);
    assertThat(test.indexOf(3)).isEqualTo(2);
    assertThat(test.indexOf(5)).isEqualTo(-1);
    assertThat(IntArray.EMPTY.indexOf(5)).isEqualTo(-1);
  }

  @Test
  public void test_lastIndexOf() {
    IntArray test = IntArray.of(1, 2, 3, 3, 4);
    assertThat(test.lastIndexOf(2)).isEqualTo(1);
    assertThat(test.lastIndexOf(3)).isEqualTo(3);
    assertThat(test.lastIndexOf(5)).isEqualTo(-1);
    assertThat(IntArray.EMPTY.lastIndexOf(5)).isEqualTo(-1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_copyInto() {
    IntArray test = IntArray.of(1, 2, 3);
    int[] dest = new int[4];
    test.copyInto(dest, 0);
    assertThat(dest).containsExactly(1, 2, 3, 0);

    int[] dest2 = new int[4];
    test.copyInto(dest2, 1);
    assertThat(dest2).containsExactly(0, 1, 2, 3);

    int[] dest3 = new int[4];
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.copyInto(dest3, 2));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.copyInto(dest3, -1));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_subArray_from() {
    IntArray test = IntArray.of(1, 2, 3);
    assertContent(test.subArray(0), 1, 2, 3);
    assertContent(test.subArray(1), 2, 3);
    assertContent(test.subArray(2), 3);
    assertContent(test.subArray(3));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.subArray(4));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.subArray(-1));
  }

  @Test
  public void test_subArray_fromTo() {
    IntArray test = IntArray.of(1, 2, 3);
    assertContent(test.subArray(0, 3), 1, 2, 3);
    assertContent(test.subArray(1, 3), 2, 3);
    assertContent(test.subArray(2, 3), 3);
    assertContent(test.subArray(3, 3));
    assertContent(test.subArray(1, 2), 2);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.subArray(0, 4));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.subArray(-1, 3));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toList() {
    IntArray test = IntArray.of(1, 2, 3);
    List<Integer> list = test.toList();
    assertContent(IntArray.copyOf(list), 1, 2, 3);
    assertThat(list.size()).isEqualTo(3);
    assertThat(list.isEmpty()).isEqualTo(false);
    assertThat(list.get(0).intValue()).isEqualTo(1);
    assertThat(list.get(2).intValue()).isEqualTo(3);
    assertThat(list.contains(2)).isEqualTo(true);
    assertThat(list.contains(5)).isEqualTo(false);
    assertThat(list.contains(ANOTHER_TYPE)).isEqualTo(false);
    assertThat(list.indexOf(2)).isEqualTo(1);
    assertThat(list.indexOf(5)).isEqualTo(-1);
    assertThat(list.indexOf(ANOTHER_TYPE)).isEqualTo(-1);
    assertThat(list.lastIndexOf(3)).isEqualTo(2);
    assertThat(list.lastIndexOf(5)).isEqualTo(-1);
    assertThat(list.lastIndexOf(ANOTHER_TYPE)).isEqualTo(-1);

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.clear());
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.set(0, 3));
  }

  @Test
  public void test_toList_iterator() {
    IntArray test = IntArray.of(1, 2, 3);
    List<Integer> list = test.toList();
    Iterator<Integer> it = list.iterator();
    assertThat(it.hasNext()).isEqualTo(true);
    assertThat(it.next().intValue()).isEqualTo(1);
    assertThat(it.hasNext()).isEqualTo(true);
    assertThat(it.next().intValue()).isEqualTo(2);
    assertThat(it.hasNext()).isEqualTo(true);
    assertThat(it.next().intValue()).isEqualTo(3);
    assertThat(it.hasNext()).isEqualTo(false);

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> it.remove());
  }

  @Test
  public void test_toList_listIterator() {
    IntArray test = IntArray.of(1, 2, 3);
    List<Integer> list = test.toList();
    ListIterator<Integer> lit = list.listIterator();
    assertThat(lit.nextIndex()).isEqualTo(0);
    assertThat(lit.previousIndex()).isEqualTo(-1);
    assertThat(lit.hasNext()).isEqualTo(true);
    assertThat(lit.hasPrevious()).isEqualTo(false);
    assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> lit.previous());

    assertThat(lit.next().intValue()).isEqualTo(1);
    assertThat(lit.nextIndex()).isEqualTo(1);
    assertThat(lit.previousIndex()).isEqualTo(0);
    assertThat(lit.hasNext()).isEqualTo(true);
    assertThat(lit.hasPrevious()).isEqualTo(true);

    assertThat(lit.next().intValue()).isEqualTo(2);
    assertThat(lit.nextIndex()).isEqualTo(2);
    assertThat(lit.previousIndex()).isEqualTo(1);
    assertThat(lit.hasNext()).isEqualTo(true);
    assertThat(lit.hasPrevious()).isEqualTo(true);

    assertThat(lit.next().intValue()).isEqualTo(3);
    assertThat(lit.nextIndex()).isEqualTo(3);
    assertThat(lit.previousIndex()).isEqualTo(2);
    assertThat(lit.hasNext()).isEqualTo(false);
    assertThat(lit.hasPrevious()).isEqualTo(true);
    assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> lit.next());

    assertThat(lit.previous().intValue()).isEqualTo(3);
    assertThat(lit.nextIndex()).isEqualTo(2);
    assertThat(lit.previousIndex()).isEqualTo(1);
    assertThat(lit.hasNext()).isEqualTo(true);
    assertThat(lit.hasPrevious()).isEqualTo(true);

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> lit.remove());
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> lit.set(2));
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> lit.add(2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_stream() {
    IntArray test = IntArray.of(1, 2, 3);
    int[] streamed = test.stream().toArray();
    assertThat(streamed).containsExactly(1, 2, 3);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forEach() {
    IntArray test = IntArray.of(1, 2, 3);
    int[] extracted = new int[3];
    test.forEach((i, v) -> extracted[i] = v);
    assertThat(extracted).containsExactly(1, 2, 3);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_with() {
    IntArray test = IntArray.of(1, 2, 3);
    assertContent(test.with(0, 4), 4, 2, 3);
    assertContent(test.with(0, 1), 1, 2, 3);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.with(-1, 2));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.with(3, 2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_plus() {
    IntArray test = IntArray.of(1, 2, 3);
    assertContent(test.plus(5), 6, 7, 8);
    assertContent(test.plus(0), 1, 2, 3);
    assertContent(test.plus(-5), -4, -3, -2);
  }

  @Test
  public void test_minus() {
    IntArray test = IntArray.of(1, 2, 3);
    assertContent(test.minus(5), -4, -3, -2);
    assertContent(test.minus(0), 1, 2, 3);
    assertContent(test.minus(-5), 6, 7, 8);
  }

  @Test
  public void test_multipliedBy() {
    IntArray test = IntArray.of(1, 2, 3);
    assertContent(test.multipliedBy(5), 5, 10, 15);
    assertContent(test.multipliedBy(1), 1, 2, 3);
  }

  @Test
  public void test_dividedBy() {
    IntArray test = IntArray.of(10, 20, 30);
    assertContent(test.dividedBy(5), 2, 4, 6);
    assertContent(test.dividedBy(1), 10, 20, 30);
  }

  @Test
  public void test_map() {
    IntArray test = IntArray.of(1, 2, 3);
    assertContent(test.map(v -> 1 / v), 1, 1 / 2, 1 / 3);
  }

  @Test
  public void test_mapWithIndex() {
    IntArray test = IntArray.of(1, 2, 3);
    assertContent(test.mapWithIndex((i, v) -> i * v), 0, 2, 6);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_plus_array() {
    IntArray test1 = IntArray.of(1, 2, 3);
    IntArray test2 = IntArray.of(5, 6, 7);
    assertContent(test1.plus(test2), 6, 8, 10);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.plus(IntArray.EMPTY));
  }

  @Test
  public void test_minus_array() {
    IntArray test1 = IntArray.of(1, 2, 3);
    IntArray test2 = IntArray.of(5, 6, 7);
    assertContent(test1.minus(test2), -4, -4, -4);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.minus(IntArray.EMPTY));
  }

  @Test
  public void test_multipliedBy_array() {
    IntArray test1 = IntArray.of(1, 2, 3);
    IntArray test2 = IntArray.of(5, 6, 7);
    assertContent(test1.multipliedBy(test2), 5, 12, 21);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.multipliedBy(IntArray.EMPTY));
  }

  @Test
  public void test_dividedBy_array() {
    IntArray test1 = IntArray.of(10, 20, 30);
    IntArray test2 = IntArray.of(2, 5, 10);
    assertContent(test1.dividedBy(test2), 5, 4, 3);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.dividedBy(IntArray.EMPTY));
  }

  @Test
  public void test_combine() {
    IntArray test1 = IntArray.of(1, 2, 3);
    IntArray test2 = IntArray.of(5, 6, 7);
    assertContent(test1.combine(test2, (a, b) -> a * b), 5, 12, 21);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.combine(IntArray.EMPTY, (a, b) -> a * b));
  }

  @Test
  public void test_combineReduce() {
    IntArray test1 = IntArray.of(1, 2, 3);
    IntArray test2 = IntArray.of(5, 6, 7);
    assertThat(test1.combineReduce(test2, (r, a, b) -> r + a * b)).isEqualTo(5 + 12 + 21);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.combineReduce(IntArray.EMPTY, (r, a, b) -> r + a * b));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_sorted() {
    assertContent(IntArray.of().sorted());
    assertContent(IntArray.of(2).sorted(), 2);
    assertContent(IntArray.of(2, 1, 3, 0).sorted(), 0, 1, 2, 3);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_min() {
    assertThat(IntArray.of(2).min()).isEqualTo(2);
    assertThat(IntArray.of(2, 1, 3).min()).isEqualTo(1);
    assertThatIllegalStateException().isThrownBy(() -> IntArray.EMPTY.min());
  }

  @Test
  public void test_max() {
    assertThat(IntArray.of(2).max()).isEqualTo(2);
    assertThat(IntArray.of(2, 1, 3).max()).isEqualTo(3);
    assertThatIllegalStateException().isThrownBy(() -> IntArray.EMPTY.max());
  }

  @Test
  public void test_sum() {
    assertThat(IntArray.EMPTY.sum()).isEqualTo(0);
    assertThat(IntArray.of(2).sum()).isEqualTo(2);
    assertThat(IntArray.of(2, 1, 3).sum()).isEqualTo(6);
  }

  @Test
  public void test_reduce() {
    assertThat(IntArray.EMPTY.reduce(2, (r, v) -> {
      throw new AssertionError();
    })).isEqualTo(2);
    assertThat(IntArray.of(2).reduce(1, (r, v) -> r * v)).isEqualTo(2);
    assertThat(IntArray.of(2, 1, 3).reduce(1, (r, v) -> r * v)).isEqualTo(6);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_concat_varargs() {
    IntArray test1 = IntArray.of(1, 2, 3);
    assertContent(test1.concat(5, 6, 7), 1, 2, 3, 5, 6, 7);
    assertContent(test1.concat(new int[] {5, 6, 7}), 1, 2, 3, 5, 6, 7);
    assertContent(test1.concat(EMPTY_INT_ARRAY), 1, 2, 3);
    assertContent(IntArray.EMPTY.concat(new int[] {1, 2, 3}), 1, 2, 3);
  }

  @Test
  public void test_concat_object() {
    IntArray test1 = IntArray.of(1, 2, 3);
    IntArray test2 = IntArray.of(5, 6, 7);
    assertContent(test1.concat(test2), 1, 2, 3, 5, 6, 7);
    assertContent(test1.concat(IntArray.EMPTY), 1, 2, 3);
    assertContent(IntArray.EMPTY.concat(test1), 1, 2, 3);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCode() {
    IntArray a1 = IntArray.of(1, 2);
    IntArray a2 = IntArray.of(1, 2);
    IntArray b = IntArray.of(1, 2, 3);
    assertThat(a1.equals(a1)).isEqualTo(true);
    assertThat(a1.equals(a2)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(ANOTHER_TYPE)).isEqualTo(false);
    assertThat(a1.equals(null)).isEqualTo(false);
    assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
  }

  @Test
  public void test_toString() {
    IntArray test = IntArray.of(1, 2);
    assertThat(test.toString()).isEqualTo("[1, 2]");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(IntArray.of(1, 2, 3));
    IntArray.of(1, 2, 3).metaBean().metaProperty("array").metaBean();
    IntArray.of(1, 2, 3).metaBean().metaProperty("array").propertyGenericType();
    IntArray.of(1, 2, 3).metaBean().metaProperty("array").annotations();
  }

  //-------------------------------------------------------------------------
  private void assertContent(IntArray array, int... expected) {
    if (expected.length == 0) {
      assertThat(array).isSameAs(IntArray.EMPTY);
      assertThat(array.isEmpty()).isEqualTo(true);
    } else {
      assertThat(array.size()).isEqualTo(expected.length);
      assertArray(array.toArray(), expected);
      assertArray(array.toArrayUnsafe(), expected);
      assertThat(array.dimensions()).isEqualTo(1);
      assertThat(array.isEmpty()).isEqualTo(false);
    }
  }

  private void assertArray(int[] array, int[] expected) {
    assertThat(array.length).isEqualTo(expected.length);

    for (int i = 0; i < array.length; i++) {
      assertThat(array[i])
          .withFailMessage("Unexpected value at index " + i + ",")
          .isEqualTo(expected[i]);
    }
  }
}
