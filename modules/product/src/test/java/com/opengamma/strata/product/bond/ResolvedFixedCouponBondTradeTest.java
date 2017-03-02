/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Test {@link ResolvedFixedCouponBondTrade}.
 */
@Test
public class ResolvedFixedCouponBondTradeTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  public void test_builder() {
    ResolvedFixedCouponBondTrade test = sut();
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
  static ResolvedFixedCouponBondTrade sut() {
    return FixedCouponBondTradeTest.sut().resolve(REF_DATA);
  }

  static ResolvedFixedCouponBondTrade sut2() {
    return FixedCouponBondTradeTest.sut2().resolve(REF_DATA);
  }

}
