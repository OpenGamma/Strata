/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.ReferenceData;

/**
 * Test {@link ResolvedBill}.
 */
public class ResolvedBillTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  private static final double TOLERANCE_PRICE = 1.0E-8;

  //-------------------------------------------------------------------------
  @Test
  public void test_getters() {
    ResolvedBill test = sut();
    assertThat(test.getSecurityId()).isEqualTo(BillTest.US_BILL.getSecurityId());
    assertThat(test.getCurrency()).isEqualTo(BillTest.US_BILL.getCurrency());
    assertThat(test.getNotional()).isEqualTo(BillTest.US_BILL.getNotional().resolve(REF_DATA));
    assertThat(test.getDayCount()).isEqualTo(BillTest.US_BILL.getDayCount());
    assertThat(test.getYieldConvention()).isEqualTo(BillTest.US_BILL.getYieldConvention());
    assertThat(test.getLegalEntityId()).isEqualTo(BillTest.US_BILL.getLegalEntityId());
    assertThat(test.getSettlementDateOffset()).isEqualTo(BillTest.US_BILL.getSettlementDateOffset());
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
  @Test
  public void price_from_yield_discount() {
    ResolvedBill bill = sut();
    double yield = 0.01;
    LocalDate settlementDate = LocalDate.of(2018, 8, 17);
    double af = bill.getDayCount().relativeYearFraction(settlementDate, bill.getNotional().getDate());
    double priceExpected = 1.0d - yield * af;
    double priceComputed = bill.priceFromYield(yield, settlementDate);
    assertThat(priceExpected).isCloseTo(priceComputed, offset(TOLERANCE_PRICE));
  }

  @Test
  public void yield_from_price_discount() {
    ResolvedBill bill = sut();
    double price = 0.99;
    LocalDate settlementDate = LocalDate.of(2018, 8, 17);
    double af = bill.getDayCount().relativeYearFraction(settlementDate, bill.getNotional().getDate());
    double yieldExpected = (1.0d - price) / af;
    double yieldComputed = bill.yieldFromPrice(price, settlementDate);
    assertThat(yieldExpected).isCloseTo(yieldComputed, offset(TOLERANCE_PRICE));
  }

  @Test
  public void price_from_yield_intatmat() {
    ResolvedBill bill = BillTest.US_BILL
        .toBuilder().yieldConvention(BillYieldConvention.INTEREST_AT_MATURITY).build().resolve(REF_DATA);
    double yield = 0.01;
    LocalDate settlementDate = LocalDate.of(2018, 8, 17);
    double af = bill.getDayCount().relativeYearFraction(settlementDate, bill.getNotional().getDate());
    double priceExpected = 1.0d / (1 + yield * af);
    double priceComputed = bill.priceFromYield(yield, settlementDate);
    assertThat(priceExpected).isCloseTo(priceComputed, offset(TOLERANCE_PRICE));
  }

  @Test
  public void yield_from_price_intatmat() {
    ResolvedBill bill = BillTest.US_BILL
        .toBuilder().yieldConvention(BillYieldConvention.INTEREST_AT_MATURITY).build().resolve(REF_DATA);
    double price = 0.99;
    LocalDate settlementDate = LocalDate.of(2018, 8, 17);
    double af = bill.getDayCount().relativeYearFraction(settlementDate, bill.getNotional().getDate());
    double yieldExpected = (1.0d / price - 1.0d) / af;
    double yieldComputed = bill.yieldFromPrice(price, settlementDate);
    assertThat(yieldExpected).isCloseTo(yieldComputed, offset(TOLERANCE_PRICE));
  }

  //-------------------------------------------------------------------------
  static ResolvedBill sut() {
    return BillTest.US_BILL.resolve(REF_DATA);
  }

  static ResolvedBill sut2() {
    return BillTest.BILL_2.resolve(REF_DATA);
  }

}
