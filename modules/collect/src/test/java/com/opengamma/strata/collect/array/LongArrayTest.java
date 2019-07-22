/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.array;

import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.LongStream;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link LongArray}.
 */
@Test
public class LongArrayTest {

  private static final long[] EMPTY_LONG_ARRAY = new long[0];
  private static final Object ANOTHER_TYPE = "";

  public void test_EMPTY() {
    assertContent(LongArray.EMPTY);
  }

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

  public void test_of_lambda() {
    assertContent(LongArray.of(0, i -> {
      throw new AssertionError();
    }));
    AtomicInteger counter = new AtomicInteger(2);
    assertContent(LongArray.of(1, i -> counter.getAndIncrement()), 2);
    assertContent(LongArray.of(2, i -> counter.getAndIncrement()), 3, 4);
  }

  public void test_of_stream() {
    assertContent(LongArray.of(LongStream.empty()));
    assertContent(LongArray.of(LongStream.of(1, 2, 3)), 1, 2, 3);
  }

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

  public void test_copyOf_List() {
    assertContent(LongArray.copyOf(ImmutableList.of(1L, 2L, 3L)), 1, 2, 3);
    assertContent(LongArray.copyOf(ImmutableList.of()));
  }

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

  public void test_copyOf_array_fromIndex() {
    assertContent(LongArray.copyOf(new long[] {1, 2, 3}, 0), 1, 2, 3);
    assertContent(LongArray.copyOf(new long[] {1, 2, 3}, 1), 2, 3);
    assertContent(LongArray.copyOf(new long[] {1, 2, 3}, 3));
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> LongArray.copyOf(new long[] {1, 2, 3}, -1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> LongArray.copyOf(new long[] {1, 2, 3}, 4));
  }

  public void test_copyOf_array_fromToIndex() {
    assertContent(LongArray.copyOf(new long[] {1, 2, 3}, 0, 3), 1, 2, 3);
    assertContent(LongArray.copyOf(new long[] {1, 2, 3}, 1, 2), 2);
    assertContent(LongArray.copyOf(new long[] {1, 2, 3}, 1, 1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> LongArray.copyOf(new long[] {1, 2, 3}, -1, 3));
    assertThatExceptionOfType(IndexOutOfBoundsException.class)
        .isThrownBy(() -> LongArray.copyOf(new long[] {1, 2, 3}, 0, 5));
  }

  public void test_filled() {
    assertContent(LongArray.filled(0));
    assertContent(LongArray.filled(3), 0, 0, 0);
  }

  public void test_filled_withValue() {
    assertContent(LongArray.filled(0, 1));
    assertContent(LongArray.filled(3, 1), 1, 1, 1);
  }

  //-------------------------------------------------------------------------
  public void test_get() {
    LongArray test = LongArray.of(1, 2, 3, 3, 4);
    assertEquals(test.get(0), 1);
    assertEquals(test.get(4), 4);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.get(-1));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.get(5));
  }

  public void test_contains() {
    LongArray test = LongArray.of(1, 2, 3, 3, 4);
    assertTrue(test.contains(1));
    assertTrue(test.contains(3));
    assertFalse(test.contains(5));
    assertFalse(LongArray.EMPTY.contains(5));
  }

  public void test_indexOf() {
    LongArray test = LongArray.of(1, 2, 3, 3, 4);
    assertEquals(test.indexOf(2), 1);
    assertEquals(test.indexOf(3), 2);
    assertEquals(test.indexOf(5), -1);
    assertEquals(LongArray.EMPTY.indexOf(5), -1);
  }

  public void test_lastIndexOf() {
    LongArray test = LongArray.of(1, 2, 3, 3, 4);
    assertEquals(test.lastIndexOf(2), 1);
    assertEquals(test.lastIndexOf(3), 3);
    assertEquals(test.lastIndexOf(5), -1);
    assertEquals(LongArray.EMPTY.lastIndexOf(5), -1);
  }

  //-------------------------------------------------------------------------
  public void test_copyInto() {
    LongArray test = LongArray.of(1, 2, 3);
    long[] dest = new long[4];
    test.copyInto(dest, 0);
    assertTrue(Arrays.equals(dest, new long[] {1, 2, 3, 0}));

    long[] dest2 = new long[4];
    test.copyInto(dest2, 1);
    assertTrue(Arrays.equals(dest2, new long[] {0, 1, 2, 3}));

    long[] dest3 = new long[4];
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.copyInto(dest3, 2));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.copyInto(dest3, -1));
  }

  //-------------------------------------------------------------------------
  public void test_subArray_from() {
    LongArray test = LongArray.of(1, 2, 3);
    assertContent(test.subArray(0), 1, 2, 3);
    assertContent(test.subArray(1), 2, 3);
    assertContent(test.subArray(2), 3);
    assertContent(test.subArray(3));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.subArray(4));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.subArray(-1));
  }

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
  public void test_toList() {
    LongArray test = LongArray.of(1, 2, 3);
    List<Long> list = test.toList();
    assertContent(LongArray.copyOf(list), 1, 2, 3);
    assertEquals(list.size(), 3);
    assertEquals(list.isEmpty(), false);
    assertEquals(list.get(0).longValue(), 1L);
    assertEquals(list.get(2).longValue(), 3L);
    assertEquals(list.contains(2L), true);
    assertEquals(list.contains(5L), false);
    assertEquals(list.contains(ANOTHER_TYPE), false);
    assertEquals(list.indexOf(2L), 1);
    assertEquals(list.indexOf(5L), -1);
    assertEquals(list.indexOf(ANOTHER_TYPE), -1);
    assertEquals(list.lastIndexOf(3L), 2);
    assertEquals(list.lastIndexOf(5L), -1);
    assertEquals(list.lastIndexOf(ANOTHER_TYPE), -1);

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.clear());
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.set(0, 3L));
  }

  public void test_toList_iterator() {
    LongArray test = LongArray.of(1, 2, 3);
    List<Long> list = test.toList();
    Iterator<Long> it = list.iterator();
    assertEquals(it.hasNext(), true);
    assertEquals(it.next().longValue(), 1L);
    assertEquals(it.hasNext(), true);
    assertEquals(it.next().longValue(), 2L);
    assertEquals(it.hasNext(), true);
    assertEquals(it.next().longValue(), 3L);
    assertEquals(it.hasNext(), false);

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> it.remove());
  }

  public void test_toList_listIterator() {
    LongArray test = LongArray.of(1, 2, 3);
    List<Long> list = test.toList();
    ListIterator<Long> lit = list.listIterator();
    assertEquals(lit.nextIndex(), 0);
    assertEquals(lit.previousIndex(), -1);
    assertEquals(lit.hasNext(), true);
    assertEquals(lit.hasPrevious(), false);
    assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> lit.previous());

    assertEquals(lit.next().longValue(), 1L);
    assertEquals(lit.nextIndex(), 1);
    assertEquals(lit.previousIndex(), 0);
    assertEquals(lit.hasNext(), true);
    assertEquals(lit.hasPrevious(), true);

    assertEquals(lit.next().longValue(), 2L);
    assertEquals(lit.nextIndex(), 2);
    assertEquals(lit.previousIndex(), 1);
    assertEquals(lit.hasNext(), true);
    assertEquals(lit.hasPrevious(), true);

    assertEquals(lit.next().longValue(), 3L);
    assertEquals(lit.nextIndex(), 3);
    assertEquals(lit.previousIndex(), 2);
    assertEquals(lit.hasNext(), false);
    assertEquals(lit.hasPrevious(), true);
    assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> lit.next());

    assertEquals(lit.previous().longValue(), 3L);
    assertEquals(lit.nextIndex(), 2);
    assertEquals(lit.previousIndex(), 1);
    assertEquals(lit.hasNext(), true);
    assertEquals(lit.hasPrevious(), true);

    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> lit.remove());
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> lit.set(2L));
    assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> lit.add(2L));
  }

  //-------------------------------------------------------------------------
  public void test_stream() {
    LongArray test = LongArray.of(1, 2, 3);
    long[] streamed = test.stream().toArray();
    assertTrue(Arrays.equals(streamed, new long[] {1, 2, 3}));
  }

  //-------------------------------------------------------------------------
  public void test_forEach() {
    LongArray test = LongArray.of(1, 2, 3);
    long[] extracted = new long[3];
    test.forEach((i, v) -> extracted[i] = v);
    assertTrue(Arrays.equals(extracted, new long[] {1, 2, 3}));
  }

  //-------------------------------------------------------------------------
  public void test_with() {
    LongArray test = LongArray.of(1, 2, 3);
    assertContent(test.with(0, 4), 4, 2, 3);
    assertContent(test.with(0, 1), 1, 2, 3);
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.with(-1, 2));
    assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> test.with(3, 2));
  }

  //-------------------------------------------------------------------------
  public void test_plus() {
    LongArray test = LongArray.of(1, 2, 3);
    assertContent(test.plus(5), 6, 7, 8);
    assertContent(test.plus(0), 1, 2, 3);
    assertContent(test.plus(-5), -4, -3, -2);
  }

  public void test_minus() {
    LongArray test = LongArray.of(1, 2, 3);
    assertContent(test.minus(5), -4, -3, -2);
    assertContent(test.minus(0), 1, 2, 3);
    assertContent(test.minus(-5), 6, 7, 8);
  }

  public void test_multipliedBy() {
    LongArray test = LongArray.of(1, 2, 3);
    assertContent(test.multipliedBy(5), 5, 10, 15);
    assertContent(test.multipliedBy(1), 1, 2, 3);
  }

  public void test_dividedBy() {
    LongArray test = LongArray.of(10, 20, 30);
    assertContent(test.dividedBy(5), 2, 4, 6);
    assertContent(test.dividedBy(1), 10, 20, 30);
  }

  public void test_map() {
    LongArray test = LongArray.of(1, 2, 3);
    assertContent(test.map(v -> 1 / v), 1, 1 / 2, 1 / 3);
  }

  public void test_mapWithIndex() {
    LongArray test = LongArray.of(1, 2, 3);
    assertContent(test.mapWithIndex((i, v) -> i * v), 0, 2, 6);
  }

  //-------------------------------------------------------------------------
  public void test_plus_array() {
    LongArray test1 = LongArray.of(1, 2, 3);
    LongArray test2 = LongArray.of(5, 6, 7);
    assertContent(test1.plus(test2), 6, 8, 10);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.plus(LongArray.EMPTY));
  }

  public void test_minus_array() {
    LongArray test1 = LongArray.of(1, 2, 3);
    LongArray test2 = LongArray.of(5, 6, 7);
    assertContent(test1.minus(test2), -4, -4, -4);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.minus(LongArray.EMPTY));
  }

  public void test_multipliedBy_array() {
    LongArray test1 = LongArray.of(1, 2, 3);
    LongArray test2 = LongArray.of(5, 6, 7);
    assertContent(test1.multipliedBy(test2), 5, 12, 21);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.multipliedBy(LongArray.EMPTY));
  }

  public void test_dividedBy_array() {
    LongArray test1 = LongArray.of(10, 20, 30);
    LongArray test2 = LongArray.of(2, 5, 10);
    assertContent(test1.dividedBy(test2), 5, 4, 3);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.dividedBy(LongArray.EMPTY));
  }

  public void test_combine() {
    LongArray test1 = LongArray.of(1, 2, 3);
    LongArray test2 = LongArray.of(5, 6, 7);
    assertContent(test1.combine(test2, (a, b) -> a * b), 5, 12, 21);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.combine(LongArray.EMPTY, (a, b) -> a * b));
  }

  public void test_combineReduce() {
    LongArray test1 = LongArray.of(1, 2, 3);
    LongArray test2 = LongArray.of(5, 6, 7);
    assertEquals(test1.combineReduce(test2, (r, a, b) -> r + a * b), 5 + 12 + 21);
    assertThatIllegalArgumentException().isThrownBy(() -> test1.combineReduce(LongArray.EMPTY, (r, a, b) -> r + a * b));
  }

  //-------------------------------------------------------------------------
  public void test_sorted() {
    assertContent(LongArray.of().sorted());
    assertContent(LongArray.of(2).sorted(), 2);
    assertContent(LongArray.of(2, 1, 3, 0).sorted(), 0, 1, 2, 3);
  }

  //-------------------------------------------------------------------------
  public void test_min() {
    assertEquals(LongArray.of(2).min(), 2);
    assertEquals(LongArray.of(2, 1, 3).min(), 1);
    assertThatIllegalStateException().isThrownBy(() -> LongArray.EMPTY.min());
  }

  public void test_max() {
    assertEquals(LongArray.of(2).max(), 2);
    assertEquals(LongArray.of(2, 1, 3).max(), 3);
    assertThatIllegalStateException().isThrownBy(() -> LongArray.EMPTY.max());
  }

  public void test_sum() {
    assertEquals(LongArray.EMPTY.sum(), 0);
    assertEquals(LongArray.of(2).sum(), 2);
    assertEquals(LongArray.of(2, 1, 3).sum(), 6);
  }

  public void test_reduce() {
    assertEquals(LongArray.EMPTY.reduce(2, (r, v) -> {
      throw new AssertionError();
    }), 2);
    assertEquals(LongArray.of(2).reduce(1, (r, v) -> r * v), 2);
    assertEquals(LongArray.of(2, 1, 3).reduce(1, (r, v) -> r * v), 6);
  }

  //-------------------------------------------------------------------------
  public void test_concat_varargs() {
    LongArray test1 = LongArray.of(1, 2, 3);
    assertContent(test1.concat(5, 6, 7), 1, 2, 3, 5, 6, 7);
    assertContent(test1.concat(5, 6, 7), 1, 2, 3, 5, 6, 7);
    assertContent(test1.concat(EMPTY_LONG_ARRAY), 1, 2, 3);
    assertContent(LongArray.EMPTY.concat(1, 2, 3), 1, 2, 3);
  }

  public void test_concat_object() {
    LongArray test1 = LongArray.of(1, 2, 3);
    LongArray test2 = LongArray.of(5, 6, 7);
    assertContent(test1.concat(test2), 1, 2, 3, 5, 6, 7);
    assertContent(test1.concat(LongArray.EMPTY), 1, 2, 3);
    assertContent(LongArray.EMPTY.concat(test1), 1, 2, 3);
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    LongArray a1 = LongArray.of(1, 2);
    LongArray a2 = LongArray.of(1, 2);
    LongArray b = LongArray.of(1, 2, 3);
    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(ANOTHER_TYPE), false);
    assertEquals(a1.equals(null), false);
    assertEquals(a1.hashCode(), a2.hashCode());
  }

  public void test_toString() {
    LongArray test = LongArray.of(1, 2);
    assertEquals(test.toString(), "[1, 2]");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(LongArray.of(1, 2, 3));
    LongArray.of(1, 2, 3).metaBean().metaProperty("array").metaBean();
    LongArray.of(1, 2, 3).metaBean().metaProperty("array").propertyGenericType();
    LongArray.of(1, 2, 3).metaBean().metaProperty("array").annotations();
  }

  //-------------------------------------------------------------------------
  private void assertContent(LongArray array, long... expected) {
    if (expected.length == 0) {
      assertSame(array, LongArray.EMPTY);
      assertEquals(array.isEmpty(), true);
    } else {
      assertEquals(array.size(), expected.length);
      assertArray(array.toArray(), expected);
      assertArray(array.toArrayUnsafe(), expected);
      assertEquals(array.dimensions(), 1);
      assertEquals(array.isEmpty(), false);
    }
  }

  private void assertArray(long[] array, long[] expected) {
    assertEquals(array.length, expected.length);

    for (int i = 0; i < array.length; i++) {
      assertEquals(array[i], expected[i], "Unexpected value at index " + i + ",");
    }
  }
}
