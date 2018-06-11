/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

/**
 * Test {@link ResolvedFixedCouponBondSettlement}. 
 */
@Test
public class ResolvedFixedCouponBondSettlementTest {

  private static final LocalDate SETTLE_DATE = date(2018, 6, 1);
  private static final LocalDate SETTLE_DATE2 = date(2018, 6, 2);
  private static final double PRICE = 99.2;
  private static final double PRICE2 = 99.5;

  public void test_of() {
    ResolvedFixedCouponBondSettlement test = sut();
    assertEquals(test.getSettlementDate(), SETTLE_DATE);
    assertEquals(test.getPrice(), PRICE);
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
  static ResolvedFixedCouponBondSettlement sut() {
    return ResolvedFixedCouponBondSettlement.of(SETTLE_DATE, PRICE);
  }

  static ResolvedFixedCouponBondSettlement sut2() {
    return ResolvedFixedCouponBondSettlement.of(SETTLE_DATE2, PRICE2);
  }

}
