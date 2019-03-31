/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.collect.function;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link ObjDoubleToDoubleFunction}.
 */
@Test
public class ObjDoubleToDoubleFunctionTest {

  public void test_andThen() {
    ObjDoubleToDoubleFunction<String> fn1 = (a, b) -> Double.parseDouble(a) + b;
    ObjDoubleToDoubleFunction<String> fn2 = fn1.andThen(val -> val + 4);
    assertEquals(fn1.apply("2", 3.2d), 5.2d, 0d);
    assertEquals(fn2.apply("2", 3.2d), 9.2d, 0d);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void test_andThen_null() {
    ObjDoubleToDoubleFunction<String> fn1 = (a, b) -> 6d;
    fn1.andThen(null);
  }

}
