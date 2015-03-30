/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test ObjLongFunction.
 */
@Test
public class ObjLongFunctionTest {

  public void test_andThen() {
    ObjLongFunction<Integer, String> fn1 = (a, b) -> a + "=" + b;
    ObjLongFunction<Integer, String> fn2 = fn1.andThen(str -> "[" + str + "]");
    assertEquals(fn1.apply(2, 3L), "2=3");
    assertEquals(fn2.apply(2, 3L), "[2=3]");
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void test_andThen_null() {
    ObjLongFunction<Integer, String> fn1 = (a, b) -> a + "=" + b;
    fn1.andThen(null);
  }

}
