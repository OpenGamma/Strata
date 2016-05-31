/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link SecurityId}.
 */
@Test
public class SecurityIdTest {

  private static final StandardId STANDARD_ID = StandardId.of("A", "1");

  //-------------------------------------------------------------------------
  public void test_of_strings() {
    SecurityId test = SecurityId.of("A", "1");
    assertEquals(test.getStandardId(), STANDARD_ID);
    assertEquals(test.getReferenceDataType(), Security.class);
    assertEquals(test.toString(), STANDARD_ID.toString());
  }

  public void test_of_standardId() {
    SecurityId test = SecurityId.of(STANDARD_ID);
    assertEquals(test.getStandardId(), STANDARD_ID);
    assertEquals(test.getReferenceDataType(), Security.class);
    assertEquals(test.toString(), STANDARD_ID.toString());
  }

  public void test_parse() {
    SecurityId test = SecurityId.parse(STANDARD_ID.toString());
    assertEquals(test.getStandardId(), STANDARD_ID);
    assertEquals(test.getReferenceDataType(), Security.class);
    assertEquals(test.toString(), STANDARD_ID.toString());
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    SecurityId a = SecurityId.of("A", "1");
    SecurityId a2 = SecurityId.of("A", "1");
    SecurityId b = SecurityId.of("B", "1");
    assertEquals(a.equals(a), true);
    assertEquals(a.equals(a2), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals(""), false);
  }

}
