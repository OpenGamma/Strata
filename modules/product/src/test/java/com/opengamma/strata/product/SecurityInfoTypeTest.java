/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link SecurityInfoType}.
 */
@Test
public class SecurityInfoTypeTest {

  //-------------------------------------------------------------------------
  public void test_constants() {
    SecurityInfoType<String> test = SecurityInfoType.NAME;
    assertEquals(test.toString(), "Name");
  }

  //-------------------------------------------------------------------------
  public void test_of() {
    SecurityInfoType<String> test = SecurityInfoType.of("Test");
    assertEquals(test.toString(), "Test");
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    SecurityInfoType<String> a = SecurityInfoType.of("Test");
    SecurityInfoType<String> a2 = SecurityInfoType.of("Test");
    SecurityInfoType<String> b = SecurityInfoType.of("Test2");
    assertEquals(a.equals(a), true);
    assertEquals(a.equals(a2), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals(""), false);
  }

}
