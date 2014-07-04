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
public class ObjectIdSupplierTest {

  public void test_basics() {
    ObjectIdSupplier supplier = new ObjectIdSupplier("Scheme");
    assertEquals("Scheme", supplier.getScheme());
    assertEquals(ObjectId.parse("Scheme~1"), supplier.get());
    assertEquals(ObjectId.parse("Scheme~2"), supplier.get());
    assertEquals(ObjectId.parse("Scheme~3"), supplier.get());
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_constructor_nullScheme() {
    new ObjectIdSupplier((String) null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_constructor_emptyScheme() {
    new ObjectIdSupplier("");
  }

  public void test_toString() {
    ObjectIdSupplier test = new ObjectIdSupplier("Prefixing");
    assertEquals(true, test.toString().contains("Prefixing"));
  }

}
