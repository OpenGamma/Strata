/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link ResolvedCapitalIndexedBondTrade}. 
 */
@Test
public class ResolvedCapitalIndexedBondTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedCapitalIndexedBondTrade test = sut();
    assertEquals(test.getSettlementDate(), test.getInfo().getSettlementDate().get());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static ResolvedCapitalIndexedBondTrade sut() {
    return CapitalIndexedBondTradeTest.sut().resolve(REF_DATA);
  }

  static ResolvedCapitalIndexedBondTrade sut2() {
    return CapitalIndexedBondTradeTest.sut2().resolve(REF_DATA);
  }

}
