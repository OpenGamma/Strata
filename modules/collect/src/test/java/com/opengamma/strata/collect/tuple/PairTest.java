/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.tuple;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.collect.TestHelper;

/**
 * Test.
 */
@Test
public class PairTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "factory")
  Object[][] data_factory() {
    return new Object[][] {
        {"A", "B"},
        {"A", 200.2d},
    };
  }

  @Test(dataProvider = "factory")
  public void test_of_getters(Object first, Object second) {
    Pair<Object, Object> test = Pair.of(first, second);
    assertEquals(test.getFirst(), first);
    assertEquals(test.getSecond(), second);
  }

  @Test(dataProvider = "factory")
  public void test_sizeElements(Object first, Object second) {
    Pair<Object, Object> test = Pair.of(first, second);
    assertEquals(test.size(), 2);
    assertEquals(test.elements(), ImmutableList.of(first, second));
  }

  @Test(dataProvider = "factory")
  public void test_toString(Object first, Object second) {
    Pair<Object, Object> test = Pair.of(first, second);
    String str = "[" + first + ", " + second + "]";
    assertEquals(test.toString(), str);
  }

  @DataProvider(name = "factoryNull")
  Object[][] data_factoryNull() {
    return new Object[][] {
        {null, null},
        {null, "B"},
        {"A", null},
    };
  }

  @Test(dataProvider = "factoryNull", expectedExceptions = IllegalArgumentException.class)
  public void test_of_null(Object first, Object second) {
    Pair.of(first, second);
  }

  //-------------------------------------------------------------------------
  public void test_compareTo() {
    Pair<String, String> ab = Pair.of("A", "B");
    Pair<String, String> ad = Pair.of("A", "D");
    Pair<String, String> ba = Pair.of("B", "A");

    assertTrue(ab.compareTo(ab) == 0);
    assertTrue(ab.compareTo(ad) < 0);
    assertTrue(ab.compareTo(ba) < 0);

    assertTrue(ad.compareTo(ab) > 0);
    assertTrue(ad.compareTo(ad) == 0);
    assertTrue(ad.compareTo(ba) < 0);

    assertTrue(ba.compareTo(ab) > 0);
    assertTrue(ba.compareTo(ad) > 0);
    assertTrue(ba.compareTo(ba) == 0);
  }

  @Test(expectedExceptions = ClassCastException.class)
  public void test_compareTo_notComparable() {
    Runnable notComparable = () -> {};
    Pair<Runnable, String> test1 = Pair.of(notComparable, "A");
    Pair<Runnable, String> test2 = Pair.of(notComparable, "B");
    test1.compareTo(test2);
  }

  //-------------------------------------------------------------------------
  public void test_equals() {
    Pair<Integer, String> a = Pair.of(1, "Hello");
    Pair<Integer, String> a2 = Pair.of(1, "Hello");
    Pair<Integer, String> b = Pair.of(1, "Goodbye");
    Pair<Integer, String> c = Pair.of(2, "Hello");
    Pair<Integer, String> d = Pair.of(2, "Goodbye");

    assertEquals(a.equals(a), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(c), false);
    assertEquals(a.equals(d), false);
    assertEquals(a.equals(a2), true);

    assertEquals(b.equals(a), false);
    assertEquals(b.equals(b), true);
    assertEquals(b.equals(c), false);
    assertEquals(b.equals(d), false);

    assertEquals(c.equals(a), false);
    assertEquals(c.equals(b), false);
    assertEquals(c.equals(c), true);
    assertEquals(c.equals(d), false);

    assertEquals(d.equals(a), false);
    assertEquals(d.equals(b), false);
    assertEquals(d.equals(c), false);
    assertEquals(d.equals(d), true);
  }

  public void test_equals_bad() {
    Pair<Integer, String> a = Pair.of(1, "Hello");
    assertEquals(a.equals(null), false);
    assertEquals(a.equals(""), false);
  }

  public void test_hashCode() {
    Pair<Integer, String> a = Pair.of(1, "Hello");
    assertEquals(a.hashCode(), a.hashCode());
  }

  public void test_toString() {
    Pair<String, String> test = Pair.of("A", "B");
    assertEquals("[A, B]", test.toString());
  }

  public void coverage() {
    Pair<String, String> test = Pair.of("A", "B");
    TestHelper.coverImmutableBean(test);
  }

}
