/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.bond;

import static com.opengamma.strata.basics.currency.Currency.EUR;
import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.HolidayCalendars.SAT_SUN;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.currency.Payment;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.date.HolidayCalendar;
import com.opengamma.strata.basics.date.HolidayCalendars;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.collect.id.StandardId;

/**
 * Test {@link ExpandedFixedCouponBond}.
 */
@Test
public class ExpandedFixedCouponBondTest {

  private static final double FIXED_RATE = 0.0125;
  private static final double NOTIONAL = 1.0e7;
  private static final CurrencyAmount CURRENCY_NOTIONAL = CurrencyAmount.of(EUR, NOTIONAL);

  private static final FixedCouponBondPaymentPeriod PAYMENT_1 = FixedCouponBondPaymentPeriod.builder()
      .currency(EUR)
      .notional(NOTIONAL)
      .startDate(LocalDate.of(2015, 4, 13))
      .unadjustedStartDate(LocalDate.of(2015, 4, 12))
      .endDate(LocalDate.of(2015, 10, 12))
      .unadjustedEndDate(LocalDate.of(2015, 10, 12))
      .detachmentDate(LocalDate.of(2015, 10, 5))
      .fixedRate(FIXED_RATE)
      .build();
  private static final FixedCouponBondPaymentPeriod PAYMENT_2 = FixedCouponBondPaymentPeriod.builder()
      .currency(EUR)
      .notional(NOTIONAL)
      .startDate(LocalDate.of(2015, 10, 12))
      .unadjustedStartDate(LocalDate.of(2015, 10, 12))
      .endDate(LocalDate.of(2016, 4, 12))
      .unadjustedEndDate(LocalDate.of(2016, 4, 12))
      .detachmentDate(LocalDate.of(2016, 4, 5))
      .fixedRate(FIXED_RATE)
      .build();
  private static final FixedCouponBondPaymentPeriod PAYMENT_3 = FixedCouponBondPaymentPeriod.builder()
      .currency(EUR)
      .notional(NOTIONAL)
      .startDate(LocalDate.of(2016, 4, 12))
      .unadjustedStartDate(LocalDate.of(2016, 4, 12))
      .endDate(LocalDate.of(2016, 10, 12))
      .unadjustedEndDate(LocalDate.of(2016, 10, 12))
      .detachmentDate(LocalDate.of(2016, 10, 5))
      .fixedRate(FIXED_RATE)
      .build();
  private static final FixedCouponBondPaymentPeriod PAYMENT_4 = FixedCouponBondPaymentPeriod.builder()
      .currency(EUR)
      .notional(NOTIONAL)
      .startDate(LocalDate.of(2016, 10, 12))
      .unadjustedStartDate(LocalDate.of(2016, 10, 12))
      .endDate(LocalDate.of(2017, 4, 12))
      .unadjustedEndDate(LocalDate.of(2017, 4, 12))
      .detachmentDate(LocalDate.of(2017, 4, 5))
      .fixedRate(FIXED_RATE)
      .build();
  private static final ImmutableList<FixedCouponBondPaymentPeriod> PERIODIC_PAYMENTS =
      ImmutableList.<FixedCouponBondPaymentPeriod>of(PAYMENT_1, PAYMENT_2, PAYMENT_3, PAYMENT_4);

  private static final LocalDate START = PAYMENT_1.getStartDate();
  private static final LocalDate MATURITY = PAYMENT_4.getEndDate();
  private static final Payment PAYMENT = Payment.of(CURRENCY_NOTIONAL, MATURITY);

  private static final YieldConvention YIELD_CONVENTION = YieldConvention.GERMAN_BONDS;
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG-Ticker", "BUN EUR");
  private static final HolidayCalendar EUR_CALENDAR = HolidayCalendars.EUTA;
  private static final DaysAdjustment DATE_OFFSET = DaysAdjustment.ofBusinessDays(3, EUR_CALENDAR);
  private static final DayCount DAY_COUNT = DayCounts.ACT_365F;

  public void test_of() {
    ExpandedFixedCouponBond testList = ExpandedFixedCouponBond.builder()
        .dayCount(DAY_COUNT)
        .legalEntityId(LEGAL_ENTITY)
        .nominalPayment(PAYMENT)
        .periodicPayments(PERIODIC_PAYMENTS)
        .yieldConvention(YIELD_CONVENTION)
        .settlementDateOffset(DATE_OFFSET)
        .build();
    assertEquals(testList.getCurrency(), EUR);
    assertEquals(testList.getDayCount(), DAY_COUNT);
    assertEquals(testList.getStartDate(), START);
    assertEquals(testList.getEndDate(), MATURITY);
    assertEquals(testList.getLegalEntityId(), LEGAL_ENTITY);
    assertEquals(testList.getNominalPayment(), PAYMENT);
    assertEquals(testList.getSettlementDateOffset(), DATE_OFFSET);
    assertEquals(testList.getYieldConvention(), YIELD_CONVENTION);
    assertEquals(testList.getPeriodicPayments(), PERIODIC_PAYMENTS);
    ExpandedFixedCouponBond testElms = ExpandedFixedCouponBond.builder()
        .dayCount(DAY_COUNT)
        .legalEntityId(LEGAL_ENTITY)
        .nominalPayment(PAYMENT)
        .periodicPayments(PAYMENT_1, PAYMENT_2, PAYMENT_3, PAYMENT_4)
        .yieldConvention(YIELD_CONVENTION)
        .settlementDateOffset(DATE_OFFSET)
        .build();
    assertEquals(testList, testElms);
  }

  public void test_expand() {
    ExpandedFixedCouponBond base = ExpandedFixedCouponBond.builder()
        .dayCount(DAY_COUNT)
        .legalEntityId(LEGAL_ENTITY)
        .nominalPayment(PAYMENT)
        .periodicPayments(PERIODIC_PAYMENTS)
        .yieldConvention(YIELD_CONVENTION)
        .settlementDateOffset(DATE_OFFSET)
        .build();
    ExpandedFixedCouponBond test = base.expand();
    assertEquals(test, base);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ExpandedFixedCouponBond test1 = ExpandedFixedCouponBond.builder()
        .dayCount(DAY_COUNT)
        .legalEntityId(LEGAL_ENTITY)
        .nominalPayment(PAYMENT)
        .periodicPayments(PERIODIC_PAYMENTS)
        .yieldConvention(YIELD_CONVENTION)
        .settlementDateOffset(DATE_OFFSET)
        .build();
    coverImmutableBean(test1);
    BusinessDayAdjustment adj = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, SAT_SUN);
    LocalDate start = LocalDate.of(2015, 4, 12);
    LocalDate end = LocalDate.of(2025, 4, 12);
    PeriodicSchedule sche = PeriodicSchedule.of(
        start, end, Frequency.P12M, adj, StubConvention.SHORT_INITIAL, true);
    ExpandedFixedCouponBond test2 = FixedCouponBond.builder()
        .dayCount(DayCounts.ACT_360)
        .fixedRate(0.005)
        .legalEntityId(StandardId.of("OG-Ticker", "BUN EUR 2"))
        .notional(1.0e6)
        .currency(GBP)
        .periodicSchedule(sche)
        .settlementDateOffset(DaysAdjustment.ofBusinessDays(2, SAT_SUN))
        .yieldConvention(YieldConvention.UK_BUMP_DMO)
        .exCouponPeriod(DaysAdjustment.NONE)
        .build()
        .expand();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ExpandedFixedCouponBond test = ExpandedFixedCouponBond.builder()
        .dayCount(DAY_COUNT)
        .legalEntityId(LEGAL_ENTITY)
        .nominalPayment(PAYMENT)
        .periodicPayments(PERIODIC_PAYMENTS)
        .yieldConvention(YIELD_CONVENTION)
        .settlementDateOffset(DATE_OFFSET)
        .build();
    assertSerialization(test);
  }

}
