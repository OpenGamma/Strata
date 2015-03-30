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
public class TripleTest {

  //-------------------------------------------------------------------------
  @DataProvider(name = "factory")
  Object[][] data_factory() {
    return new Object[][] {
        {"A", "B", "C"},
        {"A", 200.2d, 6L},
    };
  }

  @Test(dataProvider = "factory")
  public void test_of_getters(Object first, Object second, Object third) {
    Triple<Object, Object, Object> test = Triple.of(first, second, third);
    assertEquals(test.getFirst(), first);
    assertEquals(test.getSecond(), second);
  }

  @Test(dataProvider = "factory")
  public void test_sizeElements(Object first, Object second, Object third) {
    Triple<Object, Object, Object> test = Triple.of(first, second, third);
    assertEquals(test.size(), 3);
    assertEquals(test.elements(), ImmutableList.of(first, second, third));
  }

  @Test(dataProvider = "factory")
  public void test_toString(Object first, Object second, Object third) {
    Triple<Object, Object, Object> test = Triple.of(first, second, third);
    String str = "[" + first + ", " + second + ", " + third + "]";
    assertEquals(test.toString(), str);
  }

  @DataProvider(name = "factoryNull")
  Object[][] data_factoryNull() {
    return new Object[][] {
        {null, null, null},
        {null, "B", "C"},
        {"A", null, "C"},
        {"A", "B", null},
    };
  }

  @Test(dataProvider = "factoryNull", expectedExceptions = IllegalArgumentException.class)
  public void test_of_null(Object first, Object second, Object third) {
    Triple.of(first, second, third);
  }

  //-------------------------------------------------------------------------
  public void test_compareTo() {
    Triple<String, String, String> abc = Triple.of("A", "B", "C");
    Triple<String, String, String> adc = Triple.of("A", "D", "C");
    Triple<String, String, String> bac = Triple.of("B", "A", "C");
    Triple<String, String, String> bad = Triple.of("B", "A", "D");

    assertTrue(abc.compareTo(abc) == 0);
    assertTrue(abc.compareTo(adc) < 0);
    assertTrue(abc.compareTo(bac) < 0);

    assertTrue(adc.compareTo(abc) > 0);
    assertTrue(adc.compareTo(adc) == 0);
    assertTrue(adc.compareTo(bac) < 0);

    assertTrue(bac.compareTo(abc) > 0);
    assertTrue(bac.compareTo(adc) > 0);
    assertTrue(bac.compareTo(bac) == 0);

    assertTrue(bad.compareTo(abc) > 0);
    assertTrue(bad.compareTo(adc) > 0);
    assertTrue(bad.compareTo(bac) > 0);
    assertTrue(bad.compareTo(bad) == 0);
  }

  @Test(expectedExceptions = ClassCastException.class)
  public void test_compareTo_notComparable() {
    Runnable notComparable = () -> {};
    Triple<Integer, Runnable, String> test1 = Triple.of(1, notComparable, "A");
    Triple<Integer, Runnable, String> test2 = Triple.of(2, notComparable, "B");
    test1.compareTo(test2);
  }

  //-------------------------------------------------------------------------
  public void test_equals() {
    Triple<Integer, String, String> a = Triple.of(1, "Hello", "Triple");
    Triple<Integer, String, String> a2 = Triple.of(1, "Hello", "Triple");
    Triple<Integer, String, String> b = Triple.of(1, "Goodbye", "Triple");
    Triple<Integer, String, String> c = Triple.of(2, "Hello", "Triple");
    Triple<Integer, String, String> d = Triple.of(2, "Goodbye", "Triple");
    Triple<Integer, String, String> e = Triple.of(2, "Goodbye", "Other");

    assertEquals(a.equals(a), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(c), false);
    assertEquals(a.equals(d), false);
    assertEquals(a.equals(e), false);
    assertEquals(a.equals(a2), true);

    assertEquals(b.equals(a), false);
    assertEquals(b.equals(b), true);
    assertEquals(b.equals(c), false);
    assertEquals(b.equals(d), false);
    assertEquals(b.equals(e), false);

    assertEquals(c.equals(a), false);
    assertEquals(c.equals(b), false);
    assertEquals(c.equals(c), true);
    assertEquals(c.equals(d), false);
    assertEquals(c.equals(e), false);

    assertEquals(d.equals(a), false);
    assertEquals(d.equals(b), false);
    assertEquals(d.equals(c), false);
    assertEquals(d.equals(d), true);
    assertEquals(d.equals(e), false);

    assertEquals(e.equals(a), false);
    assertEquals(e.equals(b), false);
    assertEquals(e.equals(c), false);
    assertEquals(e.equals(d), false);
    assertEquals(e.equals(e), true);
  }

  public void test_equals_bad() {
    Triple<Integer, String, String> a = Triple.of(1, "Hello", "Triple");
    assertEquals(a.equals(null), false);
    assertEquals(a.equals(""), false);
  }

  public void test_hashCode() {
    Triple<Integer, String, String> a = Triple.of(1, "Hello", "Triple");
    assertEquals(a.hashCode(), a.hashCode());
  }

  public void test_toString() {
    Triple<String, String, String> test = Triple.of("A", "B", "C");
    assertEquals("[A, B, C]", test.toString());
  }

  public void coverage() {
    Triple<String, String, String> test = Triple.of("A", "B", "C");
    TestHelper.coverImmutableBean(test);
  }

}
