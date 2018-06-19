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
 * Test {@link ResolvedBill}.
 */
@Test
public class ResolvedBillTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  //-------------------------------------------------------------------------
  public void test_getters() {
    ResolvedBill test = sut();
    assertEquals(test.getSecurityId(), BillTest.US_BILL.getSecurityId());
    assertEquals(test.getCurrency(), BillTest.US_BILL.getCurrency());
    assertEquals(test.getNotional(), BillTest.US_BILL.getNotional());
    assertEquals(test.getMaturityDate(), BillTest.US_BILL.getMaturityDate().adjusted(REF_DATA));
    assertEquals(test.getDayCount(), BillTest.US_BILL.getDayCount());
    assertEquals(test.getYieldConvention(), BillTest.US_BILL.getYieldConvention());
    assertEquals(test.getLegalEntityId(), BillTest.US_BILL.getLegalEntityId());
    assertEquals(test.getSettlementDateOffset(), BillTest.US_BILL.getSettlementDateOffset());
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
  static ResolvedBill sut() {
    return BillTest.US_BILL.resolve(REF_DATA);
  }

  static ResolvedBill sut2() {
    return BillTest.BILL_2.resolve(REF_DATA);
  }

}
