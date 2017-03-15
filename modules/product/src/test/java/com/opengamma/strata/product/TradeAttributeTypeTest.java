/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link TradeAttributeType}.
 */
@Test
public class TradeAttributeTypeTest {

  //-------------------------------------------------------------------------
  public void test_constants() {
    TradeAttributeType<String> test = TradeAttributeType.DESCRIPTION;
    assertEquals(test.toString(), "description");
  }

  //-------------------------------------------------------------------------
  public void test_of() {
    TradeAttributeType<String> test = TradeAttributeType.of("test");
    assertEquals(test.toString(), "test");
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    TradeAttributeType<String> a = TradeAttributeType.of("test");
    TradeAttributeType<String> a2 = TradeAttributeType.of("test");
    TradeAttributeType<String> b = TradeAttributeType.of("test2");
    assertEquals(a.equals(a), true);
    assertEquals(a.equals(a2), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals(""), false);
  }

}
