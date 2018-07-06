/*
 * Copyright (C) 2018 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.date.HolidayCalendarIds.EUTA;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.currency.AdjustablePayment;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.product.LegalEntityId;
import com.opengamma.strata.product.SecurityId;

/**
 * Tests {@link Bill}.
 */
@Test
public class BillTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final BusinessDayAdjustment BUSINESS_ADJUST =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, USNY);
  private static final BillYieldConvention YIELD_CONVENTION = BillYieldConvention.DISCOUNT;
  private static final LegalEntityId LEGAL_ENTITY = LegalEntityId.of("OG-Ticker", "US GOVT");
  private static final Currency CCY = Currency.USD;
  private static final double NOTIONAL_AMOUNT = 1_000_000;
  private static final LocalDate MATURITY_DATE = LocalDate.of(2019, 5, 23);
  private static final AdjustableDate MATURITY_DATE_ADJ = AdjustableDate.of(MATURITY_DATE, BUSINESS_ADJUST);
  private static final AdjustablePayment NOTIONAL =
      AdjustablePayment.of(CurrencyAmount.of(CCY, NOTIONAL_AMOUNT), MATURITY_DATE_ADJ);
  private static final DayCount DAY_COUNT = DayCounts.ACT_360;
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "Bill2019-05-23");
  private static final DaysAdjustment SETTLE = DaysAdjustment.ofBusinessDays(1, USNY, BUSINESS_ADJUST);
  public static final Bill US_BILL = Bill.builder()
      .dayCount(DAY_COUNT)
      .legalEntityId(LEGAL_ENTITY)
      .notional(NOTIONAL)
      .securityId(SECURITY_ID)
      .settlementDateOffset(SETTLE)
      .yieldConvention(YIELD_CONVENTION).build();
  public static final Bill BILL_2 = Bill.builder()
      .dayCount(DayCounts.ACT_365F)
      .legalEntityId(LegalEntityId.of("OG-Ticker", "LE2"))
      .notional(AdjustablePayment.of(CurrencyAmount.of(CCY, 10), MATURITY_DATE_ADJ))
      .securityId(SecurityId.of("OG-Test", "ID2"))
      .settlementDateOffset(DaysAdjustment.ofBusinessDays(2, EUTA, BUSINESS_ADJUST))
      .yieldConvention(BillYieldConvention.INTEREST_AT_MATURITY).build();
  private static final double TOLERANCE_PRICE = 1.0E-8;

  //-------------------------------------------------------------------------
  public void test_builder() {
    assertEquals(US_BILL.getCurrency(), CCY);
    assertEquals(US_BILL.getDayCount(), DAY_COUNT);
    assertEquals(US_BILL.getLegalEntityId(), LEGAL_ENTITY);
    assertEquals(US_BILL.getNotional(), NOTIONAL);
    assertEquals(US_BILL.getSecurityId(), SECURITY_ID);
    assertEquals(US_BILL.getSettlementDateOffset(), SETTLE);
    assertEquals(US_BILL.getYieldConvention(), YIELD_CONVENTION);
  }

  //-------------------------------------------------------------------------
  public void price_from_yield_discount() {
    double yield = 0.01;
    LocalDate settlementDate = LocalDate.of(2018, 8, 17);
    double af = US_BILL.getDayCount().relativeYearFraction(settlementDate, MATURITY_DATE);
    double priceExpected = 1.0d - yield * af;
    double priceComputed = US_BILL.priceFromYield(yield, settlementDate);
    assertEquals(priceExpected, priceComputed, TOLERANCE_PRICE);
  }

  public void yield_from_price_discount() {
    double price = 0.99;
    LocalDate settlementDate = LocalDate.of(2018, 8, 17);
    double af = US_BILL.getDayCount().relativeYearFraction(settlementDate, MATURITY_DATE);
    double yieldExpected = (1.0d - price) / af;
    double yieldComputed = US_BILL.yieldFromPrice(price, settlementDate);
    assertEquals(yieldExpected, yieldComputed, TOLERANCE_PRICE);
  }

  public void price_from_yield_intatmat() {
    Bill bill = US_BILL.toBuilder().yieldConvention(BillYieldConvention.INTEREST_AT_MATURITY).build();
    double yield = 0.01;
    LocalDate settlementDate = LocalDate.of(2018, 8, 17);
    double af = bill.getDayCount().relativeYearFraction(settlementDate, MATURITY_DATE);
    double priceExpected = 1.0d / (1 + yield * af);
    double priceComputed = bill.priceFromYield(yield, settlementDate);
    assertEquals(priceExpected, priceComputed, TOLERANCE_PRICE);
  }

  public void yield_from_price_intatmat() {
    Bill bill = US_BILL.toBuilder().yieldConvention(BillYieldConvention.INTEREST_AT_MATURITY).build();
    double price = 0.99;
    LocalDate settlementDate = LocalDate.of(2018, 8, 17);
    double af = bill.getDayCount().relativeYearFraction(settlementDate, MATURITY_DATE);
    double yieldExpected = (1.0d / price - 1.0d) / af;
    double yieldComputed = bill.yieldFromPrice(price, settlementDate);
    assertEquals(yieldExpected, yieldComputed, TOLERANCE_PRICE);
  }

  public void test_positive_notional() {
    assertThrows(() -> Bill.builder()
        .dayCount(DAY_COUNT)
        .legalEntityId(LEGAL_ENTITY)
        .notional(AdjustablePayment.of(CurrencyAmount.of(CCY, -10), MATURITY_DATE_ADJ))
        .securityId(SECURITY_ID)
        .settlementDateOffset(SETTLE)
        .yieldConvention(YIELD_CONVENTION).build());
  }

  public void test_positive_offset() {
    assertThrows(() -> Bill.builder()
        .dayCount(DAY_COUNT)
        .legalEntityId(LEGAL_ENTITY)
        .notional(NOTIONAL)
        .securityId(SECURITY_ID)
        .settlementDateOffset(DaysAdjustment.ofBusinessDays(-11, USNY, BUSINESS_ADJUST))
        .yieldConvention(YIELD_CONVENTION).build());
  }

  //-------------------------------------------------------------------------
  public void test_resolve() {
    ResolvedBill resolved = US_BILL.resolve(REF_DATA);
    assertEquals(resolved.getDayCount(), DAY_COUNT);
    assertEquals(resolved.getLegalEntityId(), LEGAL_ENTITY);
    assertEquals(resolved.getNotional(), NOTIONAL.resolve(REF_DATA));
    assertEquals(resolved.getSecurityId(), SECURITY_ID);
    assertEquals(resolved.getSettlementDateOffset(), SETTLE);
    assertEquals(resolved.getYieldConvention(), YIELD_CONVENTION);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    coverImmutableBean(US_BILL);
    coverBeanEquals(US_BILL, BILL_2);
  }

  public void test_serialization() {
    assertSerialization(US_BILL);
  }

}
