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
public class DoublesPairTest {

  private static final double TOLERANCE = 0.00001d;

  //-------------------------------------------------------------------------
  @DataProvider(name = "factory")
  Object[][] data_factory() {
    return new Object[][] {
        {1.2d, 2.5d},
        {-100.1d, 200.2d},
        {-1.2d, -2.5d},
        {0d, 0d},
    };
  }

  @Test(dataProvider = "factory")
  public void test_of_getters(double first, double second) {
    DoublesPair test = DoublesPair.of(first, second);
    assertEquals(test.getFirst(), first, TOLERANCE);
    assertEquals(test.getSecond(), second, TOLERANCE);
  }

  @Test(dataProvider = "factory")
  public void test_ofPair(double first, double second) {
    Pair<Double, Double> pair = Pair.of(first, second);
    DoublesPair test = DoublesPair.ofPair(pair);
    assertEquals(test.getFirst(), first, TOLERANCE);
    assertEquals(test.getSecond(), second, TOLERANCE);
  }

  @Test(dataProvider = "factory")
  public void test_sizeElements(double first, double second) {
    DoublesPair test = DoublesPair.of(first, second);
    assertEquals(test.size(), 2);
    assertEquals(test.elements(), ImmutableList.of(first, second));
  }

  @Test(dataProvider = "factory")
  public void test_toString(double first, double second) {
    DoublesPair test = DoublesPair.of(first, second);
    String str = "[" + first + ", " + second + "]";
    assertEquals(test.toString(), str);
    assertEquals(DoublesPair.parse(str), test);
  }

  @Test(dataProvider = "factory")
  public void test_toPair(double first, double second) {
    DoublesPair test = DoublesPair.of(first, second);
    assertEquals(test.toPair(), Pair.of(first, second));
  }

  //-------------------------------------------------------------------------
  @DataProvider(name = "parseGood")
  Object[][] data_parseGood() {
    return new Object[][] {
        {"[1.2, 2.5]", 1.2d, 2.5d},
        {"[1.2,2.5]", 1.2d, 2.5d},
        {"[ 1.2, 2.5 ]", 1.2d, 2.5d},
        {"[-1.2, -2.5]", -1.2d, -2.5d},
        {"[0,4]", 0d, 4d},
        {"[1d,201d]", 1d, 201d},
    };
  }

  @Test(dataProvider = "parseGood")
  public void test_parse_good(String text, double first, double second) {
    DoublesPair test = DoublesPair.parse(text);
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
    DoublesPair.parse(text);
  }

  //-------------------------------------------------------------------------
  public void test_compareTo() {
    DoublesPair p12 = DoublesPair.of(1d, 2d);
    DoublesPair p13 = DoublesPair.of(1d, 3d);
    DoublesPair p21 = DoublesPair.of(2d, 1d);

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
    DoublesPair a = DoublesPair.of(1d, 2.0d);
    DoublesPair a2 = DoublesPair.of(1d, 2.0d);
    DoublesPair b = DoublesPair.of(1d, 3.0d);
    DoublesPair c = DoublesPair.of(2d, 2.0d);
    DoublesPair d = DoublesPair.of(2d, 3.0d);

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
    DoublesPair a = DoublesPair.of(1.1d, 1.7d);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals(""), false);
    assertEquals(a.equals(Pair.of(Double.valueOf(1.1d), Double.valueOf(1.7d))), false);
  }

  public void test_hashCode() {
    DoublesPair a1 = DoublesPair.of(1d, 2.0d);
    DoublesPair a2 = DoublesPair.of(1d, 2.0d);
    assertEquals(a1.hashCode(), a2.hashCode());
  }

  public void coverage() {
    DoublesPair test = DoublesPair.of(1d, 2.0d);
    TestHelper.coverImmutableBean(test);
  }

  public void test_serialization() {
    assertSerialization(DoublesPair.of(1d, 1.7d));
  }

  public void test_jodaConvert() {
    assertJodaConvert(DoublesPair.class, DoublesPair.of(1d, 1.7d));
  }

}
