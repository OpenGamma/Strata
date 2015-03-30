/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.source.id;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class UniqueIdSupplierTest {

  private static final ObjectId OBJECT_ID = ObjectId.of("A", "B");

  public void test_constructor_ObjectId() {
    UniqueIdSupplier supplier = new UniqueIdSupplier(OBJECT_ID);
    assertEquals(supplier.getObjectId(), OBJECT_ID);
    UniqueId test1 = supplier.get();
    UniqueId test2 = supplier.get();
    UniqueId test3 = supplier.get();
    assertFalse(test1.equals(test2));
    assertFalse(test1.equals(test3));
    assertEquals(test1.getObjectId(), OBJECT_ID);
    assertEquals(test2.getObjectId(), OBJECT_ID);
    assertEquals(test3.getObjectId(), OBJECT_ID);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_constructor_ObjectId_null() {
    new UniqueIdSupplier((ObjectId) null);
  }

  public void test_toString() {
    UniqueIdSupplier test = new UniqueIdSupplier(OBJECT_ID);
    assertTrue(test.toString().contains(OBJECT_ID.toString()));
  }

}
