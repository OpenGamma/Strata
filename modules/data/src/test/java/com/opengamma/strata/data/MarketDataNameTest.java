/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.data;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link MarketDataName}.
 */
@Test
public class MarketDataNameTest {

  public void test_of() {
    TestingName test = new TestingName("Foo");
    assertEquals(test.getName(), "Foo");
    assertEquals(test.getMarketDataType(), String.class);
    assertEquals(test.toString(), "Foo");
  }

  public void test_comparison() {
    TestingName test = new TestingName("Foo");
    assertEquals(test.equals(test), true);
    assertEquals(test.hashCode(), test.hashCode());
    assertEquals(test.equals(new TestingName("Eoo")), false);
    assertEquals(test.equals(new TestingName("Foo")), true);
    assertEquals(test.equals("Foo"), false);
    assertEquals(test.equals(null), false);
    assertEquals(test.compareTo(new TestingName("Eoo")) > 0, true);
    assertEquals(test.compareTo(new TestingName("Foo")) == 0, true);
    assertEquals(test.compareTo(new TestingName("Goo")) < 0, true);
    assertEquals(test.compareTo(new TestingName2("Foo")) < 0, true);
  }

}
