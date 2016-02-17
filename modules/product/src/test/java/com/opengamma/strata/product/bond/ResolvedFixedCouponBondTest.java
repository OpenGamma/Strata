/**
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

import com.opengamma.strata.basics.market.ReferenceData;

/**
 * Test {@link ResolvedFixedCouponBond}.
 */
@Test
public class ResolvedFixedCouponBondTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  public void test_getters() {
    ResolvedFixedCouponBond test = sut();
    assertEquals(test.getStartDate(), test.getPeriodicPayments().get(0).getStartDate());
    assertEquals(test.getEndDate(), test.getPeriodicPayments().get(test.getPeriodicPayments().size() - 1).getEndDate());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(sut());
    coverBeanEquals(sut(), sut2());
  }

  public void coverage_builder() {
    ResolvedFixedCouponBond test = sut();
    test.toBuilder().periodicPayments(test.getPeriodicPayments().toArray(new FixedCouponBondPaymentPeriod[0])).build();
  }

  public void test_serialization() {
    assertSerialization(sut());
  }

  //-------------------------------------------------------------------------
  static ResolvedFixedCouponBond sut() {
    return FixedCouponBondTest.sut().resolve(REF_DATA);
  }

  static ResolvedFixedCouponBond sut2() {
    return FixedCouponBondTest.sut2().resolve(REF_DATA);
  }

}
