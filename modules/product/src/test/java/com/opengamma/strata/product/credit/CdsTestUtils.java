/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

/**
 * Test utilities.
 */
public class CdsTestUtils {
  // separate class to avoid methods being treated as tests by TestNG

  public static CdsTrade singleNameTrade() {
    return CdsTrade.builder()
        .product(CdsTest.sutSingleName())
        .build();
  }

  public static CdsTrade indexTrade() {
    return CdsTrade.builder()
        .product(CdsTest.sutIndex())
        .build();
  }

}
