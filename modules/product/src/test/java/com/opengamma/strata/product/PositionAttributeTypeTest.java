/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link PositionAttributeType}.
 */
@Test
public class PositionAttributeTypeTest {

  //-------------------------------------------------------------------------
  public void test_constants() {
    PositionAttributeType<String> test = PositionAttributeType.DESCRIPTION;
    assertEquals(test.toString(), "description");
  }

  //-------------------------------------------------------------------------
  public void test_of() {
    PositionAttributeType<String> test = PositionAttributeType.of("test");
    assertEquals(test.toString(), "test");
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    PositionAttributeType<String> a = PositionAttributeType.of("test");
    PositionAttributeType<String> a2 = PositionAttributeType.of("test");
    PositionAttributeType<String> b = PositionAttributeType.of("test2");
    assertEquals(a.equals(a), true);
    assertEquals(a.equals(a2), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals(""), false);
  }

}
