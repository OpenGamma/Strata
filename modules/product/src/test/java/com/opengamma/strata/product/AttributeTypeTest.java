/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static org.testng.Assert.assertEquals;

import org.joda.convert.RenameHandler;
import org.testng.annotations.Test;

/**
 * Test {@link AttributeType}.
 */
@Test
public class AttributeTypeTest {

  private static final Object ANOTHER_TYPE = "";

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
    assertEquals(a.equals(ANOTHER_TYPE), false);
  }

  public void test_jodaConvert() throws Exception {
    assertEquals(RenameHandler.INSTANCE.lookupType("com.opengamma.strata.product.PositionAttributeType"), AttributeType.class);
    assertEquals(RenameHandler.INSTANCE.lookupType("com.opengamma.strata.product.SecurityAttributeType"), AttributeType.class);
    assertEquals(RenameHandler.INSTANCE.lookupType("com.opengamma.strata.product.TradeAttributeType"), AttributeType.class);
  }

}
