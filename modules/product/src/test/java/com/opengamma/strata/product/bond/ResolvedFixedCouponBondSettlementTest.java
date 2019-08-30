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
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Test {@link ResolvedFixedCouponBondSettlement}. 
 */
public class ResolvedFixedCouponBondSettlementTest {

  private static final LocalDate SETTLE_DATE = date(2018, 6, 1);
  private static final LocalDate SETTLE_DATE2 = date(2018, 6, 2);
  private static final double PRICE = 99.2;
  private static final double PRICE2 = 99.5;

  @Test
  public void test_of() {
    ResolvedFixedCouponBondSettlement test = sut();
    assertThat(test.getSettlementDate()).isEqualTo(SETTLE_DATE);
    assertThat(test.getPrice()).isEqualTo(PRICE);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  @Test
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
