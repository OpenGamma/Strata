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

  public static CdsTrade singleNameTrade() {
    return CdsTrade.builder()
        .product(CdsSingleNameTest.sut())
        .build();
  }

  public static CdsTrade indexTrade() {
    return CdsTrade.builder()
        .product(CdsIndexTest.sut())
        .build();
  }

}
