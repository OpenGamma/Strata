/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test ObjLongPredicate.
 */
@Test
public class ObjLongPredicateTest {

  public void test_and() {
    ObjLongPredicate<String> fn1 = (a, b) -> b > 3;
    ObjLongPredicate<String> fn2 = (a, b) -> a.length() > 3;
    ObjLongPredicate<String> and = fn1.and(fn2);
    assertEquals(fn1.test("a", 2L), false);
    assertEquals(fn1.test("a", 4L), true);
    assertEquals(fn2.test("a", 4L), false);
    assertEquals(fn2.test("abcd", 4L), true);
    assertEquals(and.test("a", 2L), false);
    assertEquals(and.test("a", 4L), false);
    assertEquals(and.test("abcd", 2L), false);
    assertEquals(and.test("abcd", 4L), true);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void test_and_null() {
    ObjLongPredicate<String> fn1 = (a, b) -> b > 3;
    fn1.and(null);
  }

  //-------------------------------------------------------------------------
  public void test_or() {
    ObjLongPredicate<String> fn1 = (a, b) -> b > 3;
    ObjLongPredicate<String> fn2 = (a, b) -> a.length() > 3;
    ObjLongPredicate<String> or = fn1.or(fn2);
    assertEquals(fn1.test("a", 2L), false);
    assertEquals(fn1.test("a", 4L), true);
    assertEquals(fn2.test("a", 4L), false);
    assertEquals(fn2.test("abcd", 4L), true);
    assertEquals(or.test("a", 2L), false);
    assertEquals(or.test("a", 4L), true);
    assertEquals(or.test("abcd", 2L), true);
    assertEquals(or.test("abcd", 4L), true);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void test_or_null() {
    ObjLongPredicate<String> fn1 = (a, b) -> b > 3;
    fn1.or(null);
  }

  //-------------------------------------------------------------------------
  public void test_negate() {
    ObjLongPredicate<String> fn1 = (a, b) -> b > 3;
    ObjLongPredicate<String> negate = fn1.negate();
    assertEquals(fn1.test("a", 2L), false);
    assertEquals(fn1.test("a", 4L), true);
    assertEquals(negate.test("a", 2L), true);
    assertEquals(negate.test("a", 4L), false);
  }

}
