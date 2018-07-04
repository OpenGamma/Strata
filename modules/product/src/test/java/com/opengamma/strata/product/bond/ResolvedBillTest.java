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

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link ResolvedBill}.
 */
@Test
public class ResolvedBillTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final double TOLERANCE_PRICE = 1.0E-8;

  //-------------------------------------------------------------------------
  public void test_getters() {
    ResolvedBill test = sut();
    assertEquals(test.getSecurityId(), BillTest.US_BILL.getSecurityId());
    assertEquals(test.getCurrency(), BillTest.US_BILL.getCurrency());
    assertEquals(test.getNotional(), BillTest.US_BILL.getNotional().resolve(REF_DATA));
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
  public void price_from_yield_discount() {
    ResolvedBill bill = sut();
    double yield = 0.01;
    LocalDate settlementDate = LocalDate.of(2018, 8, 17);
    double af = bill.getDayCount().relativeYearFraction(settlementDate, bill.getNotional().getDate());
    double priceExpected = 1.0d - yield * af;
    double priceComputed = bill.priceFromYield(yield, settlementDate);
    assertEquals(priceExpected, priceComputed, TOLERANCE_PRICE);
  }
  
  public void yield_from_price_discount() {
    ResolvedBill bill = sut();
    double price = 0.99;
    LocalDate settlementDate = LocalDate.of(2018, 8, 17);
    double af = bill.getDayCount().relativeYearFraction(settlementDate, bill.getNotional().getDate());
    double yieldExpected = (1.0d - price) / af;
    double yieldComputed = bill.yieldFromPrice(price, settlementDate);
    assertEquals(yieldExpected, yieldComputed, TOLERANCE_PRICE);
  }

  public void price_from_yield_intatmat() {
    ResolvedBill bill = BillTest.US_BILL
        .toBuilder().yieldConvention(BillYieldConvention.INTEREST_AT_MATURITY).build().resolve(REF_DATA);
    double yield = 0.01;
    LocalDate settlementDate = LocalDate.of(2018, 8, 17);
    double af = bill.getDayCount().relativeYearFraction(settlementDate, bill.getNotional().getDate());
    double priceExpected = 1.0d / (1 + yield * af);
    double priceComputed = bill.priceFromYield(yield, settlementDate);
    assertEquals(priceExpected, priceComputed, TOLERANCE_PRICE);
  }
  
  public void yield_from_price_intatmat() {
    ResolvedBill bill = BillTest.US_BILL
        .toBuilder().yieldConvention(BillYieldConvention.INTEREST_AT_MATURITY).build().resolve(REF_DATA);
    double price = 0.99;
    LocalDate settlementDate = LocalDate.of(2018, 8, 17);
    double af = bill.getDayCount().relativeYearFraction(settlementDate, bill.getNotional().getDate());
    double yieldExpected = (1.0d / price - 1.0d) / af;
    double yieldComputed = bill.yieldFromPrice(price, settlementDate);
    assertEquals(yieldExpected, yieldComputed, TOLERANCE_PRICE);
  }

  //-------------------------------------------------------------------------
  static ResolvedBill sut() {
    return BillTest.US_BILL.resolve(REF_DATA);
  }

  static ResolvedBill sut2() {
    return BillTest.BILL_2.resolve(REF_DATA);
  }

}
