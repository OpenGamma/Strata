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
public class ObjDoublePairTest {

  private static final double TOLERANCE = 0.00001d;

  //-------------------------------------------------------------------------
  @DataProvider(name = "factory")
  Object[][] data_factory() {
    return new Object[][] {
        {"A", 2.5d},
        {"B", 200.2d},
        {"C", -2.5d},
        {"D", 0d},
    };
  }

  @Test(dataProvider = "factory")
  public void test_of_getters(String first, double second) {
    ObjDoublePair<String> test = ObjDoublePair.of(first, second);
    assertEquals(test.getFirst(), first);
    assertEquals(test.getSecond(), second, TOLERANCE);
  }

  @Test(dataProvider = "factory")
  public void test_ofPair(String first, double second) {
    Pair<String, Double> pair = Pair.of(first, second);
    ObjDoublePair<String> test = ObjDoublePair.ofPair(pair);
    assertEquals(test.getFirst(), first);
    assertEquals(test.getSecond(), second, TOLERANCE);
  }

  @Test(dataProvider = "factory")
  public void test_sizeElements(String first, double second) {
    ObjDoublePair<String> test = ObjDoublePair.of(first, second);
    assertEquals(test.size(), 2);
    assertEquals(test.elements(), ImmutableList.of(first, second));
  }

  @Test(dataProvider = "factory")
  public void test_toString(String first, double second) {
    ObjDoublePair<String> test = ObjDoublePair.of(first, second);
    String str = "[" + first + ", " + second + "]";
    assertEquals(test.toString(), str);
  }

  @Test(dataProvider = "factory")
  public void test_toPair(String first, double second) {
    ObjDoublePair<String> test = ObjDoublePair.of(first, second);
    assertEquals(test.toPair(), Pair.of(first, second));
  }

  //-------------------------------------------------------------------------
  public void test_compareTo() {
    ObjDoublePair<String> p12 = ObjDoublePair.of("1", 2d);
    ObjDoublePair<String> p13 = ObjDoublePair.of("1", 3d);
    ObjDoublePair<String> p21 = ObjDoublePair.of("2", 1d);

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
    ObjDoublePair<Runnable> test1 = ObjDoublePair.of(notComparable, 2d);
    ObjDoublePair<Runnable> test2 = ObjDoublePair.of(notComparable, 2d);
    test1.compareTo(test2);
  }

  //-------------------------------------------------------------------------
  public void test_equals() {
    ObjDoublePair<String> a = ObjDoublePair.of("1", 2.0d);
    ObjDoublePair<String> a2 = ObjDoublePair.of("1", 2.0d);
    ObjDoublePair<String> b = ObjDoublePair.of("1", 3.0d);
    ObjDoublePair<String> c = ObjDoublePair.of("2", 2.0d);
    ObjDoublePair<String> d = ObjDoublePair.of("2", 3.0d);

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
    ObjDoublePair<String> a = ObjDoublePair.of("1", 1.7d);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals(""), false);
    assertEquals(a.equals(Pair.of(Integer.valueOf(1), Double.valueOf(1.7d))), false);
  }

  public void test_hashCode() {
    ObjDoublePair<String> a1 = ObjDoublePair.of("1", 1.7d);
    ObjDoublePair<String> a2 = ObjDoublePair.of("1", 1.7d);
    assertEquals(a1.hashCode(), a2.hashCode());
  }

  public void coverage() {
    ObjDoublePair<String> test = ObjDoublePair.of("1", 1.7d);
    TestHelper.coverImmutableBean(test);
  }

}
