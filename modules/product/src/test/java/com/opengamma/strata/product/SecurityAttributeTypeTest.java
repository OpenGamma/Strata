/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link SecurityAttributeType}.
 */
@Test
public class SecurityAttributeTypeTest {

  //-------------------------------------------------------------------------
  public void test_constants() {
    SecurityAttributeType<String> test = SecurityAttributeType.NAME;
    assertEquals(test.toString(), "name");
  }

  //-------------------------------------------------------------------------
  public void test_of() {
    SecurityAttributeType<String> test = SecurityAttributeType.of("test");
    assertEquals(test.toString(), "test");
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    SecurityAttributeType<String> a = SecurityAttributeType.of("test");
    SecurityAttributeType<String> a2 = SecurityAttributeType.of("test");
    SecurityAttributeType<String> b = SecurityAttributeType.of("test2");
    assertEquals(a.equals(a), true);
    assertEquals(a.equals(a2), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals(""), false);
  }

}
