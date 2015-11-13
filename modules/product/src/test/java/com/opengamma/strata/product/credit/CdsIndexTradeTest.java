/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.credit;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;

import org.testng.annotations.Test;

/**
 * Test.
 */
@Test
public class CdsIndexTradeTest {

  public void test_builder_notEnoughData() {
    assertThrowsIllegalArg(() -> CdsTrade.builder().build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(CdsTestUtils.indexTrade());
  }

  public void test_serialization() {
    assertSerialization(CdsTestUtils.indexTrade());
  }

}
