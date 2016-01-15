/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 * 
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.array;

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
 * Test {@link ObjArray}.
 */
@Test
public class ObjArrayTest {

  public void test_of() {
    assertContent(ObjArray.of());
    assertContent(ObjArray.of("1"), "1");
    assertContent(ObjArray.of("1", "2"), "1", "2");
    assertContent(ObjArray.of("1", "2", "3"), "1", "2", "3");
    assertContent(ObjArray.of("1", "2", "3", "4"), "1", "2", "3", "4");
    assertContent(ObjArray.of("1", "2", "3", "4", "5"), "1", "2", "3", "4", "5");
    assertContent(ObjArray.of("1", "2", "3", "4", "5", "6"), "1", "2", "3", "4", "5", "6");
    assertContent(ObjArray.of("1", "2", "3", "4", "5", "6", "7"), "1", "2", "3", "4", "5", "6", "7");
    assertContent(ObjArray.of("1", "2", "3", "4", "5", "6", "7", "8"), "1", "2", "3", "4", "5", "6", "7", "8");
    assertContent(ObjArray.of("1", "2", "3", "4", "5", "6", "7", "8", "9"), "1", "2", "3", "4", "5", "6", "7", "8", "9");
  }

  public void test_of_lambda() {
    assertContent(ObjArray.of(0, i -> {
      throw new AssertionError();
    }));
    AtomicInteger counter = new AtomicInteger(2);
    assertContent(ObjArray.of(1, i -> counter.getAndIncrement()), 2);
    assertContent(ObjArray.of(2, i -> counter.getAndIncrement()), 3, 4);
  }

  public void test_ofUnsafe() {
    String[] base = {"1", "2", "3"};
    ObjArray<String> test = ObjArray.ofUnsafe(base);
    assertContent(test, "1", "2", "3");
    base[0] = "4";
    // internal state of object mutated - don't do this in application code!
    assertContent(test, "4", "2", "3");
    // empty
    assertContent(ObjArray.ofUnsafe(new Object[0]));
  }

  public void test_copyOf_List() {
    assertContent(ObjArray.copyOf(ImmutableList.of("1", "2", "3")), "1", "2", "3");
    assertContent(ObjArray.copyOf(ImmutableList.of()));
  }

  public void test_copyOf_array() {
    String[] base = new String[] {"1", "2", "3"};
    ObjArray<String> test = ObjArray.copyOf(base);
    assertContent(test, "1", "2", "3");
    base[0] = "4";
    // internal state of object is not mutated
    assertContent(test, "1", "2", "3");
    // empty
    assertContent(ObjArray.copyOf(new Object[0]));
  }

  //-------------------------------------------------------------------------
  public void test_get() {
    ObjArray<String> test = ObjArray.of("1", "2", "3", "3", "4");
    assertEquals(test.get(0), "1");
    assertEquals(test.get(4), "4");
    assertThrows(() -> test.get(-1), IndexOutOfBoundsException.class);
    assertThrows(() -> test.get(5), IndexOutOfBoundsException.class);
  }

  public void test_contains() {
    ObjArray<String> test = ObjArray.of("1", "2", "3", "3", "4");
    assertEquals(test.contains("1"), true);
    assertEquals(test.contains("3"), true);
    assertEquals(test.contains("5"), false);
    assertEquals(ObjArray.of().contains("5"), false);
  }

  public void test_indexOf() {
    ObjArray<String> test = ObjArray.of("1", "2", "3", "3", "4");
    assertEquals(test.indexOf("2"), 1);
    assertEquals(test.indexOf("3"), 2);
    assertEquals(test.indexOf("5"), -1);
    assertEquals(ObjArray.of().indexOf("5"), -1);
  }

  public void test_lastIndexOf() {
    ObjArray<String> test = ObjArray.of("1", "2", "3", "3", "4");
    assertEquals(test.lastIndexOf("2"), 1);
    assertEquals(test.lastIndexOf("3"), 3);
    assertEquals(test.lastIndexOf("5"), -1);
    assertEquals(ObjArray.of().lastIndexOf("5"), -1);
  }

  //-------------------------------------------------------------------------
  public void test_copyInto() {
    ObjArray<String> test = ObjArray.of("1", "2", "3");
    String[] dest = new String[4];
    test.copyInto(dest, 0);
    assertTrue(Arrays.equals(dest, new String[] {"1", "2", "3", null}));

    String[] dest2 = new String[4];
    test.copyInto(dest2, 1);
    assertTrue(Arrays.equals(dest2, new String[] {null, "1", "2", "3"}));

    String[] dest3 = new String[4];
    assertThrows(() -> test.copyInto(dest3, 2), IndexOutOfBoundsException.class);
    assertThrows(() -> test.copyInto(dest3, -1), IndexOutOfBoundsException.class);
    assertThrows(() -> test.copyInto(new Number[4], 0), ArrayStoreException.class);
  }

  //-------------------------------------------------------------------------
  public void test_subArray_from() {
    ObjArray<String> test = ObjArray.of("1", "2", "3");
    assertContent(test.subArray(0), "1", "2", "3");
    assertContent(test.subArray(1), "2", "3");
    assertContent(test.subArray(2), "3");
    assertContent(test.subArray(3));
    assertThrows(() -> test.subArray(4), IndexOutOfBoundsException.class);
    assertThrows(() -> test.subArray(-1), IndexOutOfBoundsException.class);
  }

  public void test_subArray_fromTo() {
    ObjArray<String> test = ObjArray.of("1", "2", "3");
    assertContent(test.subArray(0, 3), "1", "2", "3");
    assertContent(test.subArray(1, 3), "2", "3");
    assertContent(test.subArray(2, 3), "3");
    assertContent(test.subArray(3, 3));
    assertContent(test.subArray(1, 2), "2");
    assertThrows(() -> test.subArray(0, 4), IndexOutOfBoundsException.class);
    assertThrows(() -> test.subArray(-1, 3), IndexOutOfBoundsException.class);
  }

  //-------------------------------------------------------------------------
  public void test_toList() {
    ObjArray<String> test = ObjArray.of("1", "2", "3");
    List<String> list = test.toList();
    assertContent(ObjArray.copyOf(list), "1", "2", "3");
    assertEquals(list.size(), 3);
    assertEquals(list.isEmpty(), false);
    assertEquals(list.get(0), "1");
    assertEquals(list.get(2), "3");
    assertEquals(list.contains("2"), true);
    assertEquals(list.contains("5"), false);
    assertEquals(list.contains(""), false);
    assertEquals(list.indexOf("2"), 1);
    assertEquals(list.indexOf("5"), -1);
    assertEquals(list.indexOf(""), -1);
    assertEquals(list.lastIndexOf("3"), 2);
    assertEquals(list.lastIndexOf("5"), -1);
    assertEquals(list.lastIndexOf(""), -1);

    assertThrows(() -> list.clear(), UnsupportedOperationException.class);
    assertThrows(() -> list.set(0, "3"), UnsupportedOperationException.class);
  }

  public void test_toList_iterator() {
    ObjArray<String> test = ObjArray.of("1", "2", "3");
    List<String> list = test.toList();
    Iterator<String> it = list.iterator();
    assertEquals(it.hasNext(), true);
    assertEquals(it.next(), "1");
    assertEquals(it.hasNext(), true);
    assertEquals(it.next(), "2");
    assertEquals(it.hasNext(), true);
    assertEquals(it.next(), "3");
    assertEquals(it.hasNext(), false);

    assertThrows(() -> it.remove(), UnsupportedOperationException.class);
  }

  public void test_toList_listIterator() {
    ObjArray<String> test = ObjArray.of("1", "2", "3");
    List<String> list = test.toList();
    ListIterator<String> lit = list.listIterator();
    assertEquals(lit.nextIndex(), 0);
    assertEquals(lit.previousIndex(), -1);
    assertEquals(lit.hasNext(), true);
    assertEquals(lit.hasPrevious(), false);
    assertThrows(() -> lit.previous(), NoSuchElementException.class);

    assertEquals(lit.next(), "1");
    assertEquals(lit.nextIndex(), 1);
    assertEquals(lit.previousIndex(), 0);
    assertEquals(lit.hasNext(), true);
    assertEquals(lit.hasPrevious(), true);

    assertEquals(lit.next(), "2");
    assertEquals(lit.nextIndex(), 2);
    assertEquals(lit.previousIndex(), 1);
    assertEquals(lit.hasNext(), true);
    assertEquals(lit.hasPrevious(), true);

    assertEquals(lit.next(), "3");
    assertEquals(lit.nextIndex(), 3);
    assertEquals(lit.previousIndex(), 2);
    assertEquals(lit.hasNext(), false);
    assertEquals(lit.hasPrevious(), true);
    assertThrows(() -> lit.next(), NoSuchElementException.class);

    assertEquals(lit.previous(), "3");
    assertEquals(lit.nextIndex(), 2);
    assertEquals(lit.previousIndex(), 1);
    assertEquals(lit.hasNext(), true);
    assertEquals(lit.hasPrevious(), true);

    assertThrows(() -> lit.remove(), UnsupportedOperationException.class);
    assertThrows(() -> lit.set("2"), UnsupportedOperationException.class);
    assertThrows(() -> lit.add("2"), UnsupportedOperationException.class);
  }

  //-------------------------------------------------------------------------
  public void test_stream() {
    ObjArray<String> test = ObjArray.of("1", "2", "3");
    String[] streamed = test.stream().toArray(String[]::new);
    assertTrue(Arrays.equals(streamed, new String[] {"1", "2", "3"}));
  }

  //-------------------------------------------------------------------------
  public void test_forEach() {
    ObjArray<String> test = ObjArray.of("1", "2", "3");
    double[] extracted = new double[3];
    test.forEach((i, v) -> extracted[i] = Double.parseDouble(v));
    assertTrue(Arrays.equals(extracted, new double[] {1, 2, 3}));
  }

  //-------------------------------------------------------------------------
  public void test_with() {
    ObjArray<String> test = ObjArray.of("1", "2", "3");
    assertContent(test.with(0, "1"), "1", "2", "3");
    assertContent(test.with(0, "X"), "X", "2", "3");
    assertThrows(() -> test.with(-1, "2"), IndexOutOfBoundsException.class);
    assertThrows(() -> test.with(3, "2"), IndexOutOfBoundsException.class);
  }

  public void test_map() {
    ObjArray<String> test = ObjArray.of("1", "2", "3");
    assertContent(test.map(v -> ":" + v), ":1", ":2", ":3");
  }

  public void test_mapWithIndex() {
    ObjArray<String> test = ObjArray.of("1", "2", "3");
    assertContent(test.mapWithIndex((i, v) -> i + ":" + v), "0:1", "1:2", "2:3");
  }

  public void test_reduce() {
    assertEquals(ObjArray.of().reduce("2", (r, v) -> {
      throw new AssertionError();
    }), "2");
    assertEquals(ObjArray.of("2").reduce("B", (r, v) -> r + ":" + v), "B:2");
    assertEquals(ObjArray.of("2", "1", "3").reduce("B", (r, v) -> r + ":" + v), "B:2:1:3");
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    ObjArray<String> a1 = ObjArray.of("1", "2");
    ObjArray<String> a2 = ObjArray.of("1", "2");
    ObjArray<String> b = ObjArray.of("1", "2", "3");
    assertEquals(a1.equals(a1), true);
    assertEquals(a1.equals(a2), true);
    assertEquals(a1.equals(b), false);
    assertEquals(a1.equals(""), false);
    assertEquals(a1.equals(null), false);
    assertEquals(a1.hashCode(), a2.hashCode());
  }

  public void test_toString() {
    ObjArray<String> test = ObjArray.of("1", "2");
    assertEquals(test.toString(), "[1, 2]");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(ObjArray.of("1", "2", "3"));
    ObjArray.of("1", "2", "3").metaBean().metaProperty("array").metaBean();
    ObjArray.of("1", "2", "3").metaBean().metaProperty("array").propertyGenericType();
    ObjArray.of("1", "2", "3").metaBean().metaProperty("array").annotations();
  }

  //-------------------------------------------------------------------------
  private void assertContent(ObjArray<?> array, Object... expected) {
    if (expected.length == 0) {
      assertSame(array, ObjArray.of());
      assertEquals(array.isEmpty(), true);
    } else {
      assertEquals(array.size(), expected.length);
      assertArray(array.toArray(), expected);
      assertArray(array.toArray(Object[]::new), expected);
      assertArray(array.toArrayUnsafe(), expected);
      assertEquals(array.isEmpty(), false);
    }
  }

  private void assertArray(Object[] array, Object[] expected) {
    assertEquals(array.length, expected.length);

    for (int i = 0; i < array.length; i++) {
      assertEquals(array[i], expected[i], "Unexpected value at index " + i + ",");
    }
  }
}
