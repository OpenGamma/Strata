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

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.AdjustableDate;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
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
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG-Ticker", "US GOVT");
  private static final double NOTIONAL = 1.0e7;
  private static final DayCount DAY_COUNT = DayCounts.ACT_360;
  private static final LocalDate MATURITY_DATE = LocalDate.of(2019, 5, 23);
  private static final AdjustableDate MATURITY_DATE_ADJ = AdjustableDate.of(MATURITY_DATE, BUSINESS_ADJUST);
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "Bill2019-05-23");
  private static final Currency CCY = Currency.USD;
  private static final DaysAdjustment SETTLE = DaysAdjustment.ofBusinessDays(1, USNY, BUSINESS_ADJUST);
  public static final Bill US_BILL = Bill.builder()
      .currency(CCY)
      .dayCount(DAY_COUNT)
      .legalEntityId(LEGAL_ENTITY)
      .maturityDate(MATURITY_DATE_ADJ)
      .notional(NOTIONAL)
      .securityId(SECURITY_ID)
      .settlementDateOffset(SETTLE)
      .yieldConvention(YIELD_CONVENTION).build();
  public static final Bill BILL_2 = Bill.builder()
      .currency(Currency.EUR)
      .dayCount(DayCounts.ACT_365F)
      .legalEntityId(StandardId.of("OG-Ticker", "LE2"))
      .maturityDate(AdjustableDate.of(LocalDate.of(2019, 5, 24)))
      .notional(10)
      .securityId(SecurityId.of("OG-Test", "ID2"))
      .settlementDateOffset(DaysAdjustment.ofBusinessDays(2, EUTA, BUSINESS_ADJUST))
      .yieldConvention(BillYieldConvention.INTEREST_AT_MATURITY).build();


  //-------------------------------------------------------------------------
  public void test_builder() {
    assertEquals(US_BILL.getCurrency(), CCY);
    assertEquals(US_BILL.getDayCount(), DAY_COUNT);
    assertEquals(US_BILL.getLegalEntityId(), LEGAL_ENTITY);
    assertEquals(US_BILL.getMaturityDate(), MATURITY_DATE_ADJ);
    assertEquals(US_BILL.getNotional(), NOTIONAL);
    assertEquals(US_BILL.getSecurityId(), SECURITY_ID);
    assertEquals(US_BILL.getSettlementDateOffset(), SETTLE);
    assertEquals(US_BILL.getYieldConvention(), YIELD_CONVENTION);
  }
  
  //-------------------------------------------------------------------------
  public void test_resolve() {
    ResolvedBill resolved = US_BILL.resolve(REF_DATA);
    assertEquals(resolved.getCurrency(), CCY);
    assertEquals(resolved.getDayCount(), DAY_COUNT);
    assertEquals(resolved.getLegalEntityId(), LEGAL_ENTITY);
    assertEquals(resolved.getMaturityDate(), MATURITY_DATE);
    assertEquals(resolved.getNotional(), NOTIONAL);
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
