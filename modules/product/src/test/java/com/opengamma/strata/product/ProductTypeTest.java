/*
 * Copyright (C) 2017 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test {@link ProductType}.
 */
public class ProductTypeTest {

  private static final Object ANOTHER_TYPE = "";

  //-------------------------------------------------------------------------
  @Test
  public void test_constants() {
    assertThat(ProductType.SECURITY.toString()).isEqualTo("Security");
    assertThat(ProductType.SECURITY.getName()).isEqualTo("Security");
    assertThat(ProductType.SECURITY.getDescription()).isEqualTo("Security");
    assertThat(ProductType.FRA.toString()).isEqualTo("Fra");
    assertThat(ProductType.FRA.getName()).isEqualTo("Fra");
    assertThat(ProductType.FRA.getDescription()).isEqualTo("FRA");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    ProductType test = ProductType.of("test");
    assertThat(test.toString()).isEqualTo("test");
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_equalsHashCode() {
    ProductType a = ProductType.of("test");
    ProductType a2 = ProductType.of("test");
    ProductType b = ProductType.of("test2");
    assertThat(a.equals(a)).isTrue();
    assertThat(a.equals(a2)).isTrue();
    assertThat(a.equals(b)).isFalse();
    assertThat(a.equals(null)).isFalse();
    assertThat(a.equals(ANOTHER_TYPE)).isFalse();
  }

}
