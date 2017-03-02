/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test ObjDoublePredicate.
 */
@Test
public class ObjDoublePredicateTest {

  public void test_and() {
    ObjDoublePredicate<String> fn1 = (a, b) -> b > 3;
    ObjDoublePredicate<String> fn2 = (a, b) -> a.length() > 3;
    ObjDoublePredicate<String> and = fn1.and(fn2);
    assertEquals(fn1.test("a", 2.3d), false);
    assertEquals(fn1.test("a", 3.2d), true);
    assertEquals(fn2.test("a", 3.2d), false);
    assertEquals(fn2.test("abcd", 3.2d), true);
    assertEquals(and.test("a", 2.3d), false);
    assertEquals(and.test("a", 3.2d), false);
    assertEquals(and.test("abcd", 2.3d), false);
    assertEquals(and.test("abcd", 3.2d), true);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void test_and_null() {
    ObjDoublePredicate<String> fn1 = (a, b) -> b > 3;
    fn1.and(null);
  }

  //-------------------------------------------------------------------------
  public void test_or() {
    ObjDoublePredicate<String> fn1 = (a, b) -> b > 3;
    ObjDoublePredicate<String> fn2 = (a, b) -> a.length() > 3;
    ObjDoublePredicate<String> or = fn1.or(fn2);
    assertEquals(fn1.test("a", 2.3d), false);
    assertEquals(fn1.test("a", 3.2d), true);
    assertEquals(fn2.test("a", 3.2d), false);
    assertEquals(fn2.test("abcd", 3.2d), true);
    assertEquals(or.test("a", 2.3d), false);
    assertEquals(or.test("a", 3.2d), true);
    assertEquals(or.test("abcd", 2.3d), true);
    assertEquals(or.test("abcd", 3.2d), true);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void test_or_null() {
    ObjDoublePredicate<String> fn1 = (a, b) -> b > 3;
    fn1.or(null);
  }

  //-------------------------------------------------------------------------
  public void test_negate() {
    ObjDoublePredicate<String> fn1 = (a, b) -> b > 3;
    ObjDoublePredicate<String> negate = fn1.negate();
    assertEquals(fn1.test("a", 2.3d), false);
    assertEquals(fn1.test("a", 3.2d), true);
    assertEquals(negate.test("a", 2.3d), true);
    assertEquals(negate.test("a", 3.2d), false);
  }

}
