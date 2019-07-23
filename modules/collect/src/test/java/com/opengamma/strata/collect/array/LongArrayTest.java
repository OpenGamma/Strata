/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
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
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link LongArray}.
 */
public class LongArrayTest {

  private static final long[] EMPTY_LONG_ARRAY = new long[0];
  private static final Object ANOTHER_TYPE = "";

  @Test
  public void test_EMPTY() {
    assertContent(LongArray.EMPTY);
  }

  @Test
  public void test_of() {
    assertContent(LongArray.of());
    assertContent(LongArray.of(1), 1);
    assertContent(LongArray.of(1, 2), 1, 2);
    assertContent(LongArray.of(1, 2, 3), 1, 2, 3);
    assertContent(LongArray.of(1, 2, 3, 4), 1, 2, 3, 4);
    assertContent(LongArray.of(1, 2, 3, 4, 5), 1, 2, 3, 4, 5);
    assertContent(LongArray.of(1, 2, 3, 4, 5, 6), 1, 2, 3, 4, 5, 6);
    assertContent(LongArray.of(1, 2, 3, 4, 5, 6, 7), 1, 2, 3, 4, 5, 6, 7);
    assertContent(LongArray.of(1, 2, 3, 4, 5, 6, 7, 8), 1, 2, 3, 4, 5, 6, 7, 8);
    assertContent(LongArray.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 1, 2, 3, 4, 5, 6, 7, 8, 9);
  }

  @Test
  public void test_of_lambda() {
    assertContent(LongArray.of(0, i -> {
      throw new AssertionError();
    }));
    AtomicInteger counter = new AtomicInteger(2);
    assertContent(LongArray.of(1, i -> counter.getAndIncrement()), 2);
    assertContent(LongArray.of(2, i -> counter.getAndIncrement()), 3, 4);
  }

  @Test
  public void test_of_stream() {
    assertContent(LongArray.of(LongStream.empty()));
    assertContent(LongArray.of(LongStream.of(1, 2, 3)), 1, 2, 3);
  }

  @Test
  public void test_ofUnsafe() {
    long[] base = {1, 2, 3};
    LongArray test = LongArray.ofUnsafe(base);
    assertContent(test, 1, 2, 3);
    base[0] = 4;
    // internal state of object mutated - don't do this in application code!
    assertContent(test, 4, 2, 3);
    // empty
    assertContent(LongArray.ofUnsafe(EMPTY_LONG_ARRAY));
  }

  @Test
  public void test_copyOf_List() {
    assertContent(LongArray.copyOf(ImmutableList.of(1L, 2L, 3L)), 1, 2, 3);
    assertContent(LongArray.copyOf(ImmutableList.of()));
  }

  @Test
  public void test_copyOf_array() {
    long[] base = new long[] {1, 2, 3};
    LongArray test = LongArray.copyOf(base);
    assertContent(test, 1, 2, 3);
    base[0] = 4;
    // internal state of object is not mutated
    assertContent(test, 1, 2, 3);
    // empty
    assertContent(LongArray.copyOf(EMPTY_LONG_ARRAY));
  }

  @Test
  public void test_copyOf_array_fromIndex() {
    assertContent(LongArray.copyOf(new long[] {1, 2, 3}, 0), 1, 2, 3);
    assertContent(LongArray.copyOf(new long[] {1, 2, 3}, 1), 2, 3);
    assertContent(LongArray.copyOf(new long[] {1, 2, 3}, 3));
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> LongArray.copyOf(new long[] {1, 2, 3}, -1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> LongArray.copyOf(new long[] {1, 2, 3}, 4));
  }

  @Test
  public void test_copyOf_array_fromToIndex() {
    assertContent(LongArray.copyOf(new long[] {1, 2, 3}, 0, 3), 1, 2, 3);
    assertContent(LongArray.copyOf(new long[] {1, 2, 3}, 1, 2), 2);
    assertContent(LongArray.copyOf(new long[] {1, 2, 3}, 1, 1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> LongArray.copyOf(new long[] {1, 2, 3}, -1, 3));
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> LongArray.copyOf(new long[] {1, 2, 3}, 0, 5));
  }

  @Test
  public void test_filled() {
    assertContent(LongArray.filled(0));
    assertContent(LongArray.filled(3), 0, 0, 0);
  }

  @Test
  public void test_filled_withValue() {
    assertContent(LongArray.filled(0, 1));
    assertContent(LongArray.filled(3, 1), 1, 1, 1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_get() {
    LongArray test = LongArray.of(1, 2, 3, 3, 4);
    assertThat(test.get(0)).isEqualTo(1);
    assertThat(test.get(4)).isEqualTo(4);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.get(-1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.get(5));
  }

  @Test
  public void test_contains() {
    LongArray test = LongArray.of(1, 2, 3, 3, 4);
    assertThat(test.contains(1)).isTrue();
    assertThat(test.contains(3)).isTrue();
    assertThat(test.contains(5)).isFalse();
    assertThat(LongArray.EMPTY.contains(5)).isFalse();
  }

  @Test
  public void test_indexOf() {
    LongArray test = LongArray.of(1, 2, 3, 3, 4);
    assertThat(test.indexOf(2)).isEqualTo(1);
    assertThat(test.indexOf(3)).isEqualTo(2);
    assertThat(test.indexOf(5)).isEqualTo(-1);
    assertThat(LongArray.EMPTY.indexOf(5)).isEqualTo(-1);
  }

  @Test
  public void test_lastIndexOf() {
    LongArray test = LongArray.of(1, 2, 3, 3, 4);
    assertThat(test.lastIndexOf(2)).isEqualTo(1);
    assertThat(test.lastIndexOf(3)).isEqualTo(3);
    assertThat(test.lastIndexOf(5)).isEqualTo(-1);
    assertThat(LongArray.EMPTY.lastIndexOf(5)).isEqualTo(-1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_copyInto() {
    LongArray test = LongArray.of(1, 2, 3);
    long[] dest = new long[4];
    test.copyInto(dest, 0);
    assertThat(dest).containsExactly(1, 2, 3, 0);

    long[] dest2 = new long[4];
    test.copyInto(dest2, 1);
    assertThat(dest2).containsExactly(0, 1, 2, 3);

    long[] dest3 = new long[4];
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.copyInto(dest3, 2));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.copyInto(dest3, -1));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_subArray_from() {
    LongArray test = LongArray.of(1, 2, 3);
    assertContent(test.subArray(0), 1, 2, 3);
    assertContent(test.subArray(1), 2, 3);
    assertContent(test.subArray(2), 3);
    assertContent(test.subArray(3));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.subArray(4));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.subArray(-1));
  }

  @Test
  public void test_subArray_fromTo() {
    LongArray test = LongArray.of(1, 2, 3);
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
    LongArray test = LongArray.of(1, 2, 3);
    List<Long> list = test.toList();
    assertContent(LongArray.copyOf(list), 1, 2, 3);
    assertThat(list.size()).isEqualTo(3);
    assertThat(list.isEmpty()).isEqualTo(false);
    assertThat(list.get(0).longValue()).isEqualTo(1L);
    assertThat(list.get(2).longValue()).isEqualTo(3L);
    assertThat(list.contains(2L)).isEqualTo(true);
    assertThat(list.contains(5L)).isEqualTo(false);
    assertThat(list.contains(ANOTHER_TYPE)).isEqualTo(false);
    assertThat(list.indexOf(2L)).isEqualTo(1);
    assertThat(list.indexOf(5L)).isEqualTo(-1);
    assertThat(list.indexOf(ANOTHER_TYPE)).isEqualTo(-1);
    assertThat(list.lastIndexOf(3L)).isEqualTo(2);
    assertThat(list.lastIndexOf(5L)).isEqualTo(-1);
    assertThat(list.lastIndexOf(ANOTHER_TYPE)).isEqualTo(-1);

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.clear());
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.set(0, 3L));
  }

  @Test
  public void test_toList_iterator() {
    LongArray test = LongArray.of(1, 2, 3);
    List<Long> list = test.toList();
    Iterator<Long> it = list.iterator();
    assertThat(it.hasNext()).isEqualTo(true);
    assertThat(it.next().longValue()).isEqualTo(1L);
    assertThat(it.hasNext()).isEqualTo(true);
    assertThat(it.next().longValue()).isEqualTo(2L);
    assertThat(it.hasNext()).isEqualTo(true);
    assertThat(it.next().longValue()).isEqualTo(3L);
    assertThat(it.hasNext()).isEqualTo(false);

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> it.remove());
  }

  @Test
  public void test_toList_listIterator() {
    LongArray test = LongArray.of(1, 2, 3);
    List<Long> list = test.toList();
    ListIterator<Long> lit = list.listIterator();
    assertThat(lit.nextIndex()).isEqualTo(0);
    assertThat(lit.previousIndex()).isEqualTo(-1);
    assertThat(lit.hasNext()).isEqualTo(true);
    assertThat(lit.hasPrevious()).isEqualTo(false);
    assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> lit.previous());

    assertThat(lit.next().longValue()).isEqualTo(1L);
    assertThat(lit.nextIndex()).isEqualTo(1);
    assertThat(lit.previousIndex()).isEqualTo(0);
    assertThat(lit.hasNext()).isEqualTo(true);
    assertThat(lit.hasPrevious()).isEqualTo(true);

    assertThat(lit.next().longValue()).isEqualTo(2L);
    assertThat(lit.nextIndex()).isEqualTo(2);
    assertThat(lit.previousIndex()).isEqualTo(1);
    assertThat(lit.hasNext()).isEqualTo(true);
    assertThat(lit.hasPrevious()).isEqualTo(true);

    assertThat(lit.next().longValue()).isEqualTo(3L);
    assertThat(lit.nextIndex()).isEqualTo(3);
    assertThat(lit.previousIndex()).isEqualTo(2);
    assertThat(lit.hasNext()).isEqualTo(false);
    assertThat(lit.hasPrevious()).isEqualTo(true);
    assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> lit.next());

    assertThat(lit.previous().longValue()).isEqualTo(3L);
    assertThat(lit.nextIndex()).isEqualTo(2);
    assertThat(lit.previousIndex()).isEqualTo(1);
    assertThat(lit.hasNext()).isEqualTo(true);
    assertThat(lit.hasPrevious()).isEqualTo(true);

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> lit.remove());
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> lit.set(2L));
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> lit.add(2L));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_stream() {
    LongArray test = LongArray.of(1, 2, 3);
    long[] streamed = test.stream().toArray();
    assertThat(streamed).containsExactly(1, 2, 3);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_forEach() {
    LongArray test = LongArray.of(1, 2, 3);
    long[] extracted = new long[3];
    test.forEach((i, v) -> extracted[i] = v);
    assertThat(extracted).containsExactly(1, 2, 3);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_with() {
    LongArray test = LongArray.of(1, 2, 3);
    assertContent(test.with(0, 4), 4, 2, 3);
    assertContent(test.with(0, 1), 1, 2, 3);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.with(-1, 2));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.with(3, 2));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_plus() {
    LongArray test = LongArray.of(1, 2, 3);
    assertContent(test.plus(5), 6, 7, 8);
    assertContent(test.plus(0), 1, 2, 3);
    assertContent(test.plus(-5), -4, -3, -2);
  }

  @Test
  public void test_minus() {
    LongArray test = LongArray.of(1, 2, 3);
    assertContent(test.minus(5), -4, -3, -2);
    assertContent(test.minus(0), 1, 2, 3);
    assertContent(test.minus(-5), 6, 7, 8);
  }

  @Test
  public void test_multipliedBy() {
    LongArray test = LongArray.of(1, 2, 3);
    assertContent(test.multipliedBy(5), 5, 10, 15);
    assertContent(test.multipliedBy(1), 1, 2, 3);
  }

  @Test
  public void test_dividedBy() {
    LongArray test = LongArray.of(10, 20, 30);
    assertContent(test.dividedBy(5), 2, 4, 6);
    assertContent(test.dividedBy(1), 10, 20, 30);
  }

  @Test
  public void test_map() {
    LongArray test = LongArray.of(1, 2, 3);
    assertContent(test.map(v -> 1 / v), 1, 1 / 2, 1 / 3);
  }

  @Test
  public void test_mapWithIndex() {
    LongArray test = LongArray.of(1, 2, 3);
    assertContent(test.mapWithIndex((i, v) -> i * v), 0, 2, 6);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_plus_array() {
    LongArray test1 = LongArray.of(1, 2, 3);
    LongArray test2 = LongArray.of(5, 6, 7);
    assertContent(test1.plus(test2), 6, 8, 10);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.plus(LongArray.EMPTY));
  }

  @Test
  public void test_minus_array() {
    LongArray test1 = LongArray.of(1, 2, 3);
    LongArray test2 = LongArray.of(5, 6, 7);
    assertContent(test1.minus(test2), -4, -4, -4);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.minus(LongArray.EMPTY));
  }

  @Test
  public void test_multipliedBy_array() {
    LongArray test1 = LongArray.of(1, 2, 3);
    LongArray test2 = LongArray.of(5, 6, 7);
    assertContent(test1.multipliedBy(test2), 5, 12, 21);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.multipliedBy(LongArray.EMPTY));
  }

  @Test
  public void test_dividedBy_array() {
    LongArray test1 = LongArray.of(10, 20, 30);
    LongArray test2 = LongArray.of(2, 5, 10);
    assertContent(test1.dividedBy(test2), 5, 4, 3);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.dividedBy(LongArray.EMPTY));
  }

  @Test
  public void test_combine() {
    LongArray test1 = LongArray.of(1, 2, 3);
    LongArray test2 = LongArray.of(5, 6, 7);
    assertContent(test1.combine(test2, (a, b) -> a * b), 5, 12, 21);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.combine(LongArray.EMPTY, (a, b) -> a * b));
  }

  @Test
  public void test_combineReduce() {
    LongArray test1 = LongArray.of(1, 2, 3);
    LongArray test2 = LongArray.of(5, 6, 7);
    assertThat(test1.combineReduce(test2, (r, a, b) -> r + a * b)).isEqualTo(5 + 12 + 21);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.combineReduce(LongArray.EMPTY, (r, a, b) -> r + a * b));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_sorted() {
    assertContent(LongArray.of().sorted());
    assertContent(LongArray.of(2).sorted(), 2);
    assertContent(LongArray.of(2, 1, 3, 0).sorted(), 0, 1, 2, 3);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_min() {
    assertThat(LongArray.of(2).min()).isEqualTo(2);
    assertThat(LongArray.of(2, 1, 3).min()).isEqualTo(1);
    assertThatIllegalStateException().isThrownBy(() -> LongArray.EMPTY.min());
  }

  @Test
  public void test_max() {
    assertThat(LongArray.of(2).max()).isEqualTo(2);
    assertThat(LongArray.of(2, 1, 3).max()).isEqualTo(3);
    assertThatIllegalStateException().isThrownBy(() -> LongArray.EMPTY.max());
  }

  @Test
  public void test_sum() {
    assertThat(LongArray.EMPTY.sum()).isEqualTo(0);
    assertThat(LongArray.of(2).sum()).isEqualTo(2);
    assertThat(LongArray.of(2, 1, 3).sum()).isEqualTo(6);
  }

  @Test
  public void test_reduce() {
    assertThat(LongArray.EMPTY.reduce(2, (r, v) -> {
      throw new AssertionError();
    })).isEqualTo(2);
    assertThat(LongArray.of(2).reduce(1, (r, v) -> r * v)).isEqualTo(2);
    assertThat(LongArray.of(2, 1, 3).reduce(1, (r, v) -> r * v)).isEqualTo(6);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_concat_varargs() {
    LongArray test1 = LongArray.of(1, 2, 3);
    assertContent(test1.concat(5, 6, 7), 1, 2, 3, 5, 6, 7);
    assertContent(test1.concat(5, 6, 7), 1, 2, 3, 5, 6, 7);
    assertContent(test1.concat(EMPTY_LONG_ARRAY), 1, 2, 3);
    assertContent(LongArray.EMPTY.concat(1, 2, 3), 1, 2, 3);
  }

  @Test
  public void test_concat_object() {
    LongArray test1 = LongArray.of(1, 2, 3);
    LongArray test2 = LongArray.of(5, 6, 7);
    assertContent(test1.concat(test2), 1, 2, 3, 5, 6, 7);
    assertContent(test1.concat(LongArray.EMPTY), 1, 2, 3);
    assertContent(LongArray.EMPTY.concat(test1), 1, 2, 3);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCode() {
    LongArray a1 = LongArray.of(1, 2);
    LongArray a2 = LongArray.of(1, 2);
    LongArray b = LongArray.of(1, 2, 3);
    assertThat(a1.equals(a1)).isEqualTo(true);
    assertThat(a1.equals(a2)).isEqualTo(true);
    assertThat(a1.equals(b)).isEqualTo(false);
    assertThat(a1.equals(ANOTHER_TYPE)).isEqualTo(false);
    assertThat(a1.equals(null)).isEqualTo(false);
    assertThat(a1.hashCode()).isEqualTo(a2.hashCode());
  }

  @Test
  public void test_toString() {
    LongArray test = LongArray.of(1, 2);
    assertThat(test.toString()).isEqualTo("[1, 2]");
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(LongArray.of(1, 2, 3));
    LongArray.of(1, 2, 3).metaBean().metaProperty("array").metaBean();
    LongArray.of(1, 2, 3).metaBean().metaProperty("array").propertyGenericType();
    LongArray.of(1, 2, 3).metaBean().metaProperty("array").annotations();
  }

  //-------------------------------------------------------------------------
  private void assertContent(LongArray array, long... expected) {
    if (expected.length == 0) {
      assertThat(array).isSameAs(LongArray.EMPTY);
      assertThat(array.isEmpty()).isEqualTo(true);
    } else {
      assertThat(array.size()).isEqualTo(expected.length);
      assertArray(array.toArray(), expected);
      assertArray(array.toArrayUnsafe(), expected);
      assertThat(array.dimensions()).isEqualTo(1);
      assertThat(array.isEmpty()).isEqualTo(false);
    }
  }

  private void assertArray(long[] array, long[] expected) {
    assertThat(array.length).isEqualTo(expected.length);

    for (int i = 0; i < array.length; i++) {
      assertThat(array[i])
          .withFailMessage("Unexpected value at index " + i + ",")
          .isEqualTo(expected[i]);
    }
  }
}
