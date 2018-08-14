/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.StandardId;

/**
 * Test {@link LegalEntityId}.
 */
@Test
public class LegalEntityIdTest {

  private static final StandardId STANDARD_ID = StandardId.of("A", "1");
  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  public void test_of_strings() {
    LegalEntityId test = LegalEntityId.of("A", "1");
    assertEquals(test.getStandardId(), STANDARD_ID);
    assertEquals(test.getReferenceDataType(), LegalEntity.class);
    assertEquals(test.toString(), STANDARD_ID.toString());
  }

  public void test_of_standardId() {
    LegalEntityId test = LegalEntityId.of(STANDARD_ID);
    assertEquals(test.getStandardId(), STANDARD_ID);
    assertEquals(test.getReferenceDataType(), LegalEntity.class);
    assertEquals(test.toString(), STANDARD_ID.toString());
  }

  public void test_parse() {
    LegalEntityId test = LegalEntityId.parse(STANDARD_ID.toString());
    assertEquals(test.getStandardId(), STANDARD_ID);
    assertEquals(test.getReferenceDataType(), LegalEntity.class);
    assertEquals(test.toString(), STANDARD_ID.toString());
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    LegalEntityId a = LegalEntityId.of("A", "1");
    LegalEntityId a2 = LegalEntityId.of("A", "1");
    LegalEntityId b = LegalEntityId.of("B", "1");
    assertEquals(a.equals(a), true);
    assertEquals(a.equals(a2), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals(ANOTHER_TYPE), false);
  }

}
