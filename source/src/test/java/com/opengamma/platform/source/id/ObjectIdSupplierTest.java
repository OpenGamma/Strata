/**
 * Copyright (C) 2009 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.platform.source.id;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class ObjectIdSupplierTest {

  public void test_basics() {
    ObjectIdSupplier supplier = new ObjectIdSupplier("Scheme");
    assertEquals(supplier.getScheme(), "Scheme");
    assertEquals(supplier.get(), ObjectId.parse("Scheme~1"));
    assertEquals(supplier.get(), ObjectId.parse("Scheme~2"));
    assertEquals(supplier.get(), ObjectId.parse("Scheme~3"));
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
    assertTrue(test.toString().contains("Prefixing"));
  }

}
