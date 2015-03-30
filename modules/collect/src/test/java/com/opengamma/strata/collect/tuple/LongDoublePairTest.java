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
public class LongDoublePairTest {

  private static final double TOLERANCE = 0.00001d;

  //-------------------------------------------------------------------------
  @DataProvider(name = "factory")
  Object[][] data_factory() {
    return new Object[][] {
        {1L, 2.5d},
        {-100L, 200.2d},
        {-1L, -2.5d},
        {0L, 0d},
    };
  }

  @Test(dataProvider = "factory")
  public void test_of_getters(long first, double second) {
    LongDoublePair test = LongDoublePair.of(first, second);
    assertEquals(test.getFirst(), first, TOLERANCE);
    assertEquals(test.getSecond(), second, TOLERANCE);
  }

  @Test(dataProvider = "factory")
  public void test_ofPair(long first, double second) {
    Pair<Long, Double> pair = Pair.of(first, second);
    LongDoublePair test = LongDoublePair.ofPair(pair);
    assertEquals(test.getFirst(), first);
    assertEquals(test.getSecond(), second, TOLERANCE);
  }

  @Test(dataProvider = "factory")
  public void test_sizeElements(long first, double second) {
    LongDoublePair test = LongDoublePair.of(first, second);
    assertEquals(test.size(), 2);
    assertEquals(test.elements(), ImmutableList.of(first, second));
  }

  @Test(dataProvider = "factory")
  public void test_toString(long first, double second) {
    LongDoublePair test = LongDoublePair.of(first, second);
    String str = "[" + first + ", " + second + "]";
    assertEquals(test.toString(), str);
    assertEquals(LongDoublePair.parse(str), test);
  }

  @Test(dataProvider = "factory")
  public void test_toPair(long first, double second) {
    LongDoublePair test = LongDoublePair.of(first, second);
    assertEquals(test.toPair(), Pair.of(first, second));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "parseGood")
  Object[][] data_parseGood() {
    return new Object[][] {
        {"[1, 2.5]", 1L, 2.5d},
        {"[1,2.5]", 1L, 2.5d},
        {"[ 1, 2.5 ]", 1L, 2.5d},
        {"[-1, -2.5]", -1L, -2.5d},
        {"[0,4]", 0L, 4d},
        {"[1,201d]", 1L, 201d},
    };
  }

  @Test(dataProvider = "parseGood")
  public void test_parse_good(String text, long first, double second) {
    LongDoublePair test = LongDoublePair.parse(text);
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
    LongDoublePair.parse(text);
  }

  //-------------------------------------------------------------------------
  public void test_compareTo() {
    LongDoublePair p12 = LongDoublePair.of(1L, 2d);
    LongDoublePair p13 = LongDoublePair.of(1L, 3d);
    LongDoublePair p21 = LongDoublePair.of(2L, 1d);

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
    LongDoublePair a = LongDoublePair.of(1L, 2.0d);
    LongDoublePair a2 = LongDoublePair.of(1L, 2.0d);
    LongDoublePair b = LongDoublePair.of(1L, 3.0d);
    LongDoublePair c = LongDoublePair.of(2L, 2.0d);
    LongDoublePair d = LongDoublePair.of(2L, 3.0d);

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
    LongDoublePair a = LongDoublePair.of(1L, 1.7d);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals(""), false);
    assertEquals(a.equals(Pair.of(Long.valueOf(1L), Double.valueOf(1.7d))), false);
  }

  public void test_hashCode() {
    LongDoublePair a1 = LongDoublePair.of(1L, 1.7d);
    LongDoublePair a2 = LongDoublePair.of(1L, 1.7d);
    assertEquals(a1.hashCode(), a2.hashCode());
  }

  public void coverage() {
    LongDoublePair test = LongDoublePair.of(1L, 1.7d);
    TestHelper.coverImmutableBean(test);
  }

  public void test_serialization() {
    assertSerialization(LongDoublePair.of(1L, 1.7d));
  }

  public void test_jodaConvert() {
    assertJodaConvert(LongDoublePair.class, LongDoublePair.of(1L, 1.7d));
  }

}
