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
public class SecurityInfoValueTest {

  SecurityInfoType<String> TYPE1 = SecurityInfoType.of("T1");
  SecurityInfoType<String> TYPE2 = SecurityInfoType.of("T2");

  //-------------------------------------------------------------------------
  public void test_of() {
    SecurityInfoValue<String> test = SecurityInfoValue.of(TYPE1, "A");
    assertEquals(test.getType(), TYPE1);
    assertEquals(test.getValue(), "A");
    assertEquals(test.toString(), "T1:A");
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    SecurityInfoValue<String> a = SecurityInfoValue.of(TYPE1, "A");
    SecurityInfoValue<String> a2 = SecurityInfoValue.of(TYPE1, "A");
    SecurityInfoValue<String> b = SecurityInfoValue.of(TYPE1, "B");
    SecurityInfoValue<String> c = SecurityInfoValue.of(TYPE2, "B");
    assertEquals(a.equals(a), true);
    assertEquals(a.equals(a2), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(c), false);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals(""), false);
    assertEquals(a.hashCode(), a2.hashCode());
  }

}
