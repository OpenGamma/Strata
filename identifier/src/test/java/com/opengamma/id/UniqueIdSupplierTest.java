/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.id;

import static org.testng.AssertJUnit.assertEquals;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class UniqueIdSupplierTest {

  private static final ObjectId OBJECT_ID = ObjectId.of("A", "B");

  public void test_constructor_ObjectId() {
    UniqueIdSupplier supplier = new UniqueIdSupplier(OBJECT_ID);
    assertEquals(OBJECT_ID, supplier.getObjectId());
    UniqueId test1 = supplier.get();
    UniqueId test2 = supplier.get();
    UniqueId test3 = supplier.get();
    assertEquals(false, test1.equals(test2));
    assertEquals(false, test1.equals(test3));
    assertEquals(OBJECT_ID, test1.getObjectId());
    assertEquals(OBJECT_ID, test2.getObjectId());
    assertEquals(OBJECT_ID, test3.getObjectId());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_constructor_ObjectId_null() {
    new UniqueIdSupplier((ObjectId) null);
  }

  public void test_toString() {
    UniqueIdSupplier test = new UniqueIdSupplier(OBJECT_ID);
    assertEquals(true, test.toString().contains(OBJECT_ID.toString()));
  }

}
