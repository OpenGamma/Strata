/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * Test {@link ProductType}.
 */
@Test
public class ProductTypeTest {

  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  public void test_constants() {
    assertEquals(ProductType.SECURITY.toString(), "Security");
    assertEquals(ProductType.SECURITY.getName(), "Security");
    assertEquals(ProductType.SECURITY.getDescription(), "Security");
    assertEquals(ProductType.FRA.toString(), "Fra");
    assertEquals(ProductType.FRA.getName(), "Fra");
    assertEquals(ProductType.FRA.getDescription(), "FRA");
  }

  //-------------------------------------------------------------------------
  public void test_of() {
    ProductType test = ProductType.of("test");
    assertEquals(test.toString(), "test");
  }

  //-------------------------------------------------------------------------
  public void test_equalsHashCode() {
    ProductType a = ProductType.of("test");
    ProductType a2 = ProductType.of("test");
    ProductType b = ProductType.of("test2");
    assertEquals(a.equals(a), true);
    assertEquals(a.equals(a2), true);
    assertEquals(a.equals(b), false);
    assertEquals(a.equals(null), false);
    assertEquals(a.equals(ANOTHER_TYPE), false);
  }

}
