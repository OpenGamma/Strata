/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.collect.validate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.testng.annotations.Test;

import com.opengamma.collect.TestHelper;

/**
 * Test.
 */
@Test
public class CompareUtilsTest {

  @SuppressWarnings("unchecked")
  public void test_constructor() throws Exception {
    Constructor<?>[] cons = CompareUtils.class.getDeclaredConstructors();
    assertEquals(cons.length, 1);
    assertEquals(cons[0].getParameterTypes().length, 0);
    assertEquals(Modifier.isPrivate(cons[0].getModifiers()), true);
    Constructor<CompareUtils> con = (Constructor<CompareUtils>) cons[0];
    con.setAccessible(true);
    con.newInstance();
  }

  //-------------------------------------------------------------------------
  public void test_max() {
    assertEquals(CompareUtils.<String>max(null, null), null);
    assertEquals(CompareUtils.max(null, "A"), "A");
    assertEquals(CompareUtils.max("A", null), "A");
    Integer a = new Integer(1); // need to use new, not autoboxing
    Integer b = new Integer(1); // need to use new, not autoboxing
    assertSame(CompareUtils.max(a, b), a);  // as we test for same here
    assertEquals(CompareUtils.max("A", "B"), "B");
    assertEquals(CompareUtils.max("B", "A"), "B");
  }

  public void test_min() {
    assertEquals(CompareUtils.<String>min(null, null), null);
    assertEquals(CompareUtils.min(null, "A"), "A");
    assertEquals(CompareUtils.min("A", null), "A");
    Integer a = new Integer(1); // need to use new, not autoboxing
    Integer b = new Integer(1); // need to use new, not autoboxing
    assertSame(CompareUtils.min(a, b), a);  // as we test for same here
    assertEquals(CompareUtils.min("A", "B"), "A");
    assertEquals(CompareUtils.min("B", "A"), "A");
  }

  public void test_compareWithNullLow() {
    assertTrue(CompareUtils.compareWithNullLow(null, null) == 0);
    assertTrue(CompareUtils.compareWithNullLow(null, "Test") < 0);
    assertTrue(CompareUtils.compareWithNullLow("Test", null) > 0);
    assertTrue(CompareUtils.compareWithNullLow("Test", "Test") == 0);
    assertTrue(CompareUtils.compareWithNullLow("AAAA", "BBBB") == "AAAA".compareTo("BBBB"));
  }

  public void test_compareWithNullHigh() {
    assertTrue(CompareUtils.compareWithNullHigh(null, null) == 0);
    assertTrue(CompareUtils.compareWithNullHigh(null, "Test") > 0);
    assertTrue(CompareUtils.compareWithNullHigh("Test", null) < 0);
    assertTrue(CompareUtils.compareWithNullHigh("Test", "Test") == 0);
    assertTrue(CompareUtils.compareWithNullHigh("AAAA", "BBBB") == "AAAA".compareTo("BBBB"));
  }

  //-------------------------------------------------------------------------
  public void test_closeEquals() {
    assertEquals(CompareUtils.closeEquals(0.2d, 0.2d), true);
    assertEquals(CompareUtils.closeEquals(0.2d, 0.3d), false);
    assertEquals(CompareUtils.closeEquals(0.2d, 0.1d), false);
    assertEquals(CompareUtils.closeEquals(0.2d, 0.2000000000000001d), true);
    assertEquals(CompareUtils.closeEquals(0.2d, 0.1999999999999999d), true);
    assertEquals(CompareUtils.closeEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), true);
    assertEquals(CompareUtils.closeEquals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), true);
    assertEquals(CompareUtils.closeEquals(Double.NaN, Double.NaN), false);
    assertEquals(CompareUtils.closeEquals(Double.POSITIVE_INFINITY, Double.NaN), false);
    assertEquals(CompareUtils.closeEquals(Double.NaN, Double.POSITIVE_INFINITY), false);
    assertEquals(CompareUtils.closeEquals(Double.NEGATIVE_INFINITY, Double.NaN), false);
    assertEquals(CompareUtils.closeEquals(Double.NaN, Double.NEGATIVE_INFINITY), false);
  }

  public void test_closeEquals_tolerance() {
    assertEquals(CompareUtils.closeEquals(0.2d, 0.2d, 0.0001d), true);
    assertEquals(CompareUtils.closeEquals(0.2d, 0.3d, 0.0001d), false);
    assertEquals(CompareUtils.closeEquals(0.2d, 0.1d, 0.0001d), false);
    assertEquals(CompareUtils.closeEquals(0.2d, 0.2002d, 0.0001d), false);
    assertEquals(CompareUtils.closeEquals(0.2d, 0.2001d, 0.0001d), true);
    assertEquals(CompareUtils.closeEquals(0.2d, 0.20009d, 0.0001d), true);
    assertEquals(CompareUtils.closeEquals(0.2d, 0.2000001d, 0.0001d), true);
    assertEquals(CompareUtils.closeEquals(0.2d, 0.1999999d, 0.0001d), true);
    assertEquals(CompareUtils.closeEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0001d), true);
    assertEquals(CompareUtils.closeEquals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0001d), true);
    assertEquals(CompareUtils.closeEquals(Double.NaN, Double.NaN, 0.0001d), false);
    assertEquals(CompareUtils.closeEquals(Double.POSITIVE_INFINITY, Double.NaN, 0.0001d), false);
    assertEquals(CompareUtils.closeEquals(Double.NaN, Double.POSITIVE_INFINITY, 0.0001d), false);
    assertEquals(CompareUtils.closeEquals(Double.NEGATIVE_INFINITY, Double.NaN, 0.0001d), false);
    assertEquals(CompareUtils.closeEquals(Double.NaN, Double.NEGATIVE_INFINITY, 0.0001d), false);
  }

  public void test_compareWithTolerance() {
    assertEquals(CompareUtils.compareWithTolerance(0.2d, 0.2d, 0.0001d), 0);
    assertEquals(CompareUtils.compareWithTolerance(0.2d, 0.3d, 0.0001d), -1);
    assertEquals(CompareUtils.compareWithTolerance(0.2d, 0.1d, 0.0001d), 1);
    assertEquals(CompareUtils.compareWithTolerance(0.2d, 0.2002d, 0.0001d), -1);
    assertEquals(CompareUtils.compareWithTolerance(0.2d, 0.2001d, 0.0001d), 0);
    assertEquals(CompareUtils.compareWithTolerance(0.2d, 0.20009d, 0.0001d), 0);
    assertEquals(CompareUtils.compareWithTolerance(0.2d, 0.2000001d, 0.0001d), 0);
    assertEquals(CompareUtils.compareWithTolerance(0.2d, 0.1999999d, 0.0001d), 0);
    assertEquals(CompareUtils.compareWithTolerance(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.0001d), 0);
    assertEquals(CompareUtils.compareWithTolerance(Double.POSITIVE_INFINITY, 1.0d, 0.0001d), 1);
    assertEquals(CompareUtils.compareWithTolerance(1.0d, Double.POSITIVE_INFINITY, 0.0001d), -1);
    assertEquals(CompareUtils.compareWithTolerance(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0001d), 0);
    assertEquals(CompareUtils.compareWithTolerance(Double.NEGATIVE_INFINITY, 1.0d, 0.0001d), -1);
    assertEquals(CompareUtils.compareWithTolerance(1.0d, Double.NEGATIVE_INFINITY, 0.0001d), 1);
    assertEquals(CompareUtils.compareWithTolerance(Double.NaN, Double.NaN, 0.0001d), 1);  // weird case
  }

  public void coverage() {
    TestHelper.coverPrivateConstructor(CompareUtils.class);
  }

}
