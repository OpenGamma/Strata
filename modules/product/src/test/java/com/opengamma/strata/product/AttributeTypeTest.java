/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link AttributeType}.
 */
@Test
public class AttributeTypeTest {

  //-------------------------------------------------------------------------
  public void test_constant_description() {
    AttributeType<String> test = AttributeType.DESCRIPTION;
    assertEquals(test.toString(), "description");
  }

  public void test_constant_name() {
    AttributeType<String> test = AttributeType.NAME;
    assertEquals(test.toString(), "name");
  }

  //-------------------------------------------------------------------------
  public void test_of() {
    AttributeType<String> test = AttributeType.of("test");
    assertEquals(test.toString(), "test");
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    AttributeType<String> a = AttributeType.of("test");
    AttributeType<String> a2 = AttributeType.of("test");
    AttributeType<String> b = AttributeType.of("test2");
    assertEquals(a.equals(a), true);
    assertEquals(a.equals(a2), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals(""), false);
  }

}
