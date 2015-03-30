/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test ObjIntPredicate.
 */
@Test
public class ObjIntPredicateTest {

  public void test_and() {
    ObjIntPredicate<String> fn1 = (a, b) -> b > 3;
    ObjIntPredicate<String> fn2 = (a, b) -> a.length() > 3;
    ObjIntPredicate<String> and = fn1.and(fn2);
    assertEquals(fn1.test("a", 2), false);
    assertEquals(fn1.test("a", 4), true);
    assertEquals(fn2.test("a", 4), false);
    assertEquals(fn2.test("abcd", 4), true);
    assertEquals(and.test("a", 2), false);
    assertEquals(and.test("a", 4), false);
    assertEquals(and.test("abcd", 2), false);
    assertEquals(and.test("abcd", 4), true);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void test_and_null() {
    ObjIntPredicate<String> fn1 = (a, b) -> b > 3;
    fn1.and(null);
  }

  //-------------------------------------------------------------------------
  public void test_or() {
    ObjIntPredicate<String> fn1 = (a, b) -> b > 3;
    ObjIntPredicate<String> fn2 = (a, b) -> a.length() > 3;
    ObjIntPredicate<String> or = fn1.or(fn2);
    assertEquals(fn1.test("a", 2), false);
    assertEquals(fn1.test("a", 4), true);
    assertEquals(fn2.test("a", 4), false);
    assertEquals(fn2.test("abcd", 4), true);
    assertEquals(or.test("a", 2), false);
    assertEquals(or.test("a", 4), true);
    assertEquals(or.test("abcd", 2), true);
    assertEquals(or.test("abcd", 4), true);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void test_or_null() {
    ObjIntPredicate<String> fn1 = (a, b) -> b > 3;
    fn1.or(null);
  }

  //-------------------------------------------------------------------------
  public void test_negate() {
    ObjIntPredicate<String> fn1 = (a, b) -> b > 3;
    ObjIntPredicate<String> negate = fn1.negate();
    assertEquals(fn1.test("a", 2), false);
    assertEquals(fn1.test("a", 4), true);
    assertEquals(negate.test("a", 2), true);
    assertEquals(negate.test("a", 4), false);
  }

}
