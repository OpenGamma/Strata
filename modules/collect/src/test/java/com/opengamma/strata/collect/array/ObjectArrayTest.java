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

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

/**
 * Test {@link ObjectArray}.
 */
@Test
public class ObjectArrayTest {

  public void test_of() {
    assertContent(ObjectArray.empty());
    assertContent(ObjectArray.of(ImmutableList.of("1")), "1");
  }

  //-------------------------------------------------------------------------
  public void test_get() {
    ObjectArray<String> test = ObjectArray.of(ImmutableList.of("1", "2", "3", "3", "4"));
    assertEquals(test.get(0), "1");
    assertEquals(test.get(4), "4");
    assertThrows(() -> test.get(-1), IndexOutOfBoundsException.class);
    assertThrows(() -> test.get(5), IndexOutOfBoundsException.class);
  }

  //-------------------------------------------------------------------------
  public void test_forEach() {
    ObjectArray<String> test = ObjectArray.of(ImmutableList.of("1", "2", "3"));
    double[] extracted = new double[3];
    test.forEach((i, v) -> extracted[i] = Double.parseDouble(v));
    assertTrue(Arrays.equals(extracted, new double[] {1, 2, 3}));
  }

  public void test_map() {
    ObjectArray<String> test = ObjectArray.of(ImmutableList.of("1", "2", "3"));
    assertContent(test.map(v -> ":" + v), ":1", ":2", ":3");
  }

  public void test_mapWithIndex() {
    ObjectArray<String> test = ObjectArray.of(ImmutableList.of("1", "2", "3"));
    assertContent(test.mapWithIndex((i, v) -> i + ":" + v), "0:1", "1:2", "2:3");
  }

  public void test_reduce() {
    assertEquals(ObjectArray.empty().reduce("2", (r, v) -> {
      throw new AssertionError();
    }), "2");
    assertEquals(ObjectArray.of(ImmutableList.of("2")).reduce("B", (r, v) -> r + ":" + v), "B:2");
    assertEquals(ObjectArray.of(ImmutableList.of("2", "1", "3")).reduce("B", (r, v) -> r + ":" + v), "B:2:1:3");
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(ObjectArray.of(ImmutableList.of("1", "2", "3")));
//    ObjectArray.of(ImmutableList.of("1", "2", "3")).metaBean().metaProperty("array").metaBean();
//    ObjectArray.of(ImmutableList.of("1", "2", "3")).metaBean().metaProperty("array").propertyGenericType();
//    ObjectArray.of(ImmutableList.of("1", "2", "3")).metaBean().metaProperty("array").annotations();
  }

  //-------------------------------------------------------------------------
  private void assertContent(Array<?> array, Object... expected) {
    if (expected.length == 0) {
      assertSame(array, ObjectArray.empty());
      assertEquals(array.isEmpty(), true);
    } else {
      assertEquals(array.size(), expected.length);
      assertEquals(array.get(0), expected[0]);
      assertArray(array.toList().toArray(), expected);
      assertArray(array.stream().toArray(), expected);
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
