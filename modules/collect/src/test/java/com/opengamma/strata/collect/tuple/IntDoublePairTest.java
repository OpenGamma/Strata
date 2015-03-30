/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.tuple;

import static com.opengamma.strata.collect.TestHelper.assertJodaConvert;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
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
public class IntDoublePairTest {

  private static final double TOLERANCE = 0.00001d;

  //-------------------------------------------------------------------------
  @DataProvider(name = "factory")
  Object[][] data_factory() {
    return new Object[][] {
        {1, 2.5d},
        {-100, 200.2d},
        {-1, -2.5d},
        {0, 0d},
    };
  }

  @Test(dataProvider = "factory")
  public void test_of_getters(int first, double second) {
    IntDoublePair test = IntDoublePair.of(first, second);
    assertEquals(test.getFirst(), first);
    assertEquals(test.getSecond(), second, TOLERANCE);
  }

  @Test(dataProvider = "factory")
  public void test_ofPair(int first, double second) {
    Pair<Integer, Double> pair = Pair.of(first, second);
    IntDoublePair test = IntDoublePair.ofPair(pair);
    assertEquals(test.getFirst(), first);
    assertEquals(test.getSecond(), second, TOLERANCE);
  }

  @Test(dataProvider = "factory")
  public void test_sizeElements(int first, double second) {
    IntDoublePair test = IntDoublePair.of(first, second);
    assertEquals(test.size(), 2);
    assertEquals(test.elements(), ImmutableList.of(first, second));
  }

  @Test(dataProvider = "factory")
  public void test_toString(int first, double second) {
    IntDoublePair test = IntDoublePair.of(first, second);
    String str = "[" + first + ", " + second + "]";
    assertEquals(test.toString(), str);
    assertEquals(IntDoublePair.parse(str), test);
  }

  @Test(dataProvider = "factory")
  public void test_toPair(int first, double second) {
    IntDoublePair test = IntDoublePair.of(first, second);
    assertEquals(test.toPair(), Pair.of(first, second));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "parseGood")
  Object[][] data_parseGood() {
    return new Object[][] {
        {"[1, 2.5]", 1, 2.5d},
        {"[1,2.5]", 1, 2.5d},
        {"[ 1, 2.5 ]", 1, 2.5d},
        {"[-1, -2.5]", -1, -2.5d},
        {"[0,4]", 0, 4d},
        {"[1,201d]", 1, 201d},
    };
  }

  @Test(dataProvider = "parseGood")
  public void test_parse_good(String text, int first, double second) {
    IntDoublePair test = IntDoublePair.parse(text);
    assertEquals(test.getFirst(), first, TOLERANCE);
    assertEquals(test.getSecond(), second, TOLERANCE);
  }

  @DataProvider(name = "parseBad")
  Object[][] data_parseBad() {
    return new Object[][] {
        {null},
        {""},
        {"[]"},
        {"[10]"},
        {"[10,20"},
        {"10,20]"},
        {"[10 20]"},
        {"[10,20,30]"},
    };
  }

  @Test(dataProvider = "parseBad", expectedExceptions = IllegalArgumentException.class)
  public void test_parse_bad(String text) {
    IntDoublePair.parse(text);
  }

  //-------------------------------------------------------------------------
  public void test_compareTo() {
    IntDoublePair p12 = IntDoublePair.of(1, 2d);
    IntDoublePair p13 = IntDoublePair.of(1, 3d);
    IntDoublePair p21 = IntDoublePair.of(2, 1d);

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

  //-------------------------------------------------------------------------
  public void test_equals() {
    IntDoublePair a = IntDoublePair.of(1, 2.0d);
    IntDoublePair a2 = IntDoublePair.of(1, 2.0d);
    IntDoublePair b = IntDoublePair.of(1, 3.0d);
    IntDoublePair c = IntDoublePair.of(2, 2.0d);
    IntDoublePair d = IntDoublePair.of(2, 3.0d);

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
    IntDoublePair a = IntDoublePair.of(1, 1.7d);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals(""), false);
    assertEquals(a.equals(Pair.of(Integer.valueOf(1), Double.valueOf(1.7d))), false);
  }

  public void test_hashCode() {
    IntDoublePair a1 = IntDoublePair.of(1, 1.7d);
    IntDoublePair a2 = IntDoublePair.of(1, 1.7d);
    assertEquals(a1.hashCode(), a2.hashCode());
  }

  public void coverage() {
    IntDoublePair test = IntDoublePair.of(1, 1.7d);
    TestHelper.coverImmutableBean(test);
  }

  public void test_serialization() {
    assertSerialization(IntDoublePair.of(1, 1.7d));
  }

  public void test_jodaConvert() {
    assertJodaConvert(IntDoublePair.class, IntDoublePair.of(1, 1.7d));
  }

}
