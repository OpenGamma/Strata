/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
public class ObjIntPairTest {

  private static final double TOLERANCE = 0.00001d;

  //-------------------------------------------------------------------------
  @DataProvider(name = "factory")
  Object[][] data_factory() {
    return new Object[][] {
        {"A", 2},
        {"B", 200},
        {"C", -2},
        {"D", 0},
    };
  }

  @Test(dataProvider = "factory")
  public void test_of_getters(String first, int second) {
    ObjIntPair<String> test = ObjIntPair.of(first, second);
    assertEquals(test.getFirst(), first);
    assertEquals(test.getSecond(), second, TOLERANCE);
  }

  @Test(dataProvider = "factory")
  public void test_ofPair(String first, int second) {
    Pair<String, Integer> pair = Pair.of(first, second);
    ObjIntPair<String> test = ObjIntPair.ofPair(pair);
    assertEquals(test.getFirst(), first);
    assertEquals(test.getSecond(), second, TOLERANCE);
  }

  @Test(dataProvider = "factory")
  public void test_sizeElements(String first, int second) {
    ObjIntPair<String> test = ObjIntPair.of(first, second);
    assertEquals(test.size(), 2);
    assertEquals(test.elements(), ImmutableList.of(first, second));
  }

  @Test(dataProvider = "factory")
  public void test_toString(String first, int second) {
    ObjIntPair<String> test = ObjIntPair.of(first, second);
    String str = "[" + first + ", " + second + "]";
    assertEquals(test.toString(), str);
  }

  @Test(dataProvider = "factory")
  public void test_toPair(String first, int second) {
    ObjIntPair<String> test = ObjIntPair.of(first, second);
    assertEquals(test.toPair(), Pair.of(first, second));
  }

  //-------------------------------------------------------------------------
  public void test_compareTo() {
    ObjIntPair<String> p12 = ObjIntPair.of("1", 2);
    ObjIntPair<String> p13 = ObjIntPair.of("1", 3);
    ObjIntPair<String> p21 = ObjIntPair.of("2", 1);

    assertTrue(p12.compareTo(p12) == 0);
    assertTrue(p12.compareTo(p13) < 0);
    assertTrue(p12.compareTo(p21) < 0);

    assertTrue(p13.compareTo(p12) > 0);
    assertTrue(p13.compareTo(p13) == 0);
    assertTrue(p13.compareTo(p21) < 0);

    assertTrue(p21.compareTo(p12) > 0);
    assertTrue(p21.compareTo(p13) > 0);
    assertTrue(p21.compareTo(p21) == 0);
  }

  @Test(expectedExceptions = ClassCastException.class)
  public void test_compareTo_notComparable() {
    Runnable notComparable = () -> {};
    ObjIntPair<Runnable> test1 = ObjIntPair.of(notComparable, 2);
    ObjIntPair<Runnable> test2 = ObjIntPair.of(notComparable, 2);
    test1.compareTo(test2);
  }

  //-------------------------------------------------------------------------
  public void test_equals() {
    ObjIntPair<String> a = ObjIntPair.of("1", 2);
    ObjIntPair<String> a2 = ObjIntPair.of("1", 2);
    ObjIntPair<String> b = ObjIntPair.of("1", 3);
    ObjIntPair<String> c = ObjIntPair.of("2", 2);
    ObjIntPair<String> d = ObjIntPair.of("2", 3);

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
    ObjIntPair<String> a = ObjIntPair.of("1", 1);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals(""), false);
    assertEquals(a.equals(Pair.of(Integer.valueOf(1), Integer.valueOf(1))), false);
  }

  public void test_hashCode() {
    ObjIntPair<String> a1 = ObjIntPair.of("1", 1);
    ObjIntPair<String> a2 = ObjIntPair.of("1", 1);
    assertEquals(a1.hashCode(), a2.hashCode());
  }

  public void coverage() {
    ObjIntPair<String> test = ObjIntPair.of("1", 1);
    TestHelper.coverImmutableBean(test);
  }

}
