/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ICMA;
import static com.opengamma.strata.basics.date.DayCounts.NL_365;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendars.USNY;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.bond.YieldConvention.INDEX_LINKED_FLOAT;
import static com.opengamma.strata.product.bond.YieldConvention.US_IL_REAL;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

import java.time.LocalDate;
import java.time.Period;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.collect.id.StandardId;
import com.opengamma.strata.product.rate.RateObservation;
import com.opengamma.strata.product.swap.InflationRateCalculation;

/**
 * Test {@link ExpandedCapitalIndexedBond}. 
 */
@Test
public class ExpandedCapitalIndexedBondTest {

  private static final StandardId LEGAL_ENTITY = StandardId.of("OG-Ticker", "US-Govt");
  private static final double COUPON = 0.01;
  private static final InflationRateCalculation RATE_CALC = InflationRateCalculation.builder()
      .gearing(ValueSchedule.of(COUPON))
      .index(US_CPI_U)
      .lag(Period.ofMonths(3))
      .interpolated(true)
      .build();
  private static final double NOTIONAL = 10_000_000d;
  private static final double START_INDEX = 198.475;
  private static final BusinessDayAdjustment SCHEDULE_ADJ =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, USNY);
  private static final DaysAdjustment SETTLE_OFFSET = DaysAdjustment.ofBusinessDays(2, USNY);

  private static final CapitalIndexedBondPaymentPeriod[] PERIODIC = new CapitalIndexedBondPaymentPeriod[4];
  static {
    LocalDate[] unAdjDates = new LocalDate[] {LocalDate.of(2008, 1, 13), LocalDate.of(2008, 7, 13),
      LocalDate.of(2009, 1, 13), LocalDate.of(2009, 7, 13), LocalDate.of(2010, 1, 13) };
    for (int i = 0; i < 4; ++i) {
      LocalDate start = SCHEDULE_ADJ.adjust(unAdjDates[i]);
      LocalDate end = SCHEDULE_ADJ.adjust(unAdjDates[i + 1]);
      RateObservation obs = RATE_CALC.createRateObservation(end, START_INDEX);
      PERIODIC[i] = CapitalIndexedBondPaymentPeriod.builder()
          .currency(USD)
          .startDate(start)
          .endDate(end)
          .unadjustedStartDate(unAdjDates[i])
          .unadjustedEndDate(unAdjDates[i + 1])
          .detachmentDate(end)
          .realCoupon(COUPON)
          .rateObservation(obs)
          .notional(NOTIONAL)
          .build();
    }
  }
  private static final CapitalIndexedBondPaymentPeriod NOMINAL =
      PERIODIC[3].withUnitCoupon(PERIODIC[0].getStartDate(), PERIODIC[0].getUnadjustedStartDate());

  public void test_builder() {
    ExpandedCapitalIndexedBond test = ExpandedCapitalIndexedBond.builder()
        .dayCount(ACT_ACT_ICMA)
        .legalEntityId(LEGAL_ENTITY)
        .nominalPayment(NOMINAL)
        .periodicPayments(PERIODIC)
        .settlementDateOffset(SETTLE_OFFSET)
        .yieldConvention(US_IL_REAL)
        .build();
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getDayCount(), ACT_ACT_ICMA);
    assertEquals(test.getStartDate(), PERIODIC[0].getStartDate());
    assertEquals(test.getEndDate(), PERIODIC[3].getEndDate());
    assertEquals(test.getLegalEntityId(), LEGAL_ENTITY);
    assertEquals(test.getNominalPayment(), NOMINAL);
    assertEquals(test.getPeriodicPayments().toArray(), PERIODIC);
    assertEquals(test.getSettlementDateOffset(), SETTLE_OFFSET);
    assertEquals(test.getYieldConvention(), US_IL_REAL);
    assertSame(test.expand(), test);
  }
  
  public void test_builder_fail() {
    CapitalIndexedBondPaymentPeriod period = CapitalIndexedBondPaymentPeriod.builder()
        .startDate(PERIODIC[2].getStartDate())
        .endDate(PERIODIC[2].getEndDate())
        .currency(GBP)
        .notional(NOTIONAL)
        .rateObservation(PERIODIC[2].getRateObservation())
        .realCoupon(COUPON)
        .build();
    assertThrowsIllegalArg(() -> ExpandedCapitalIndexedBond.builder()
        .dayCount(ACT_ACT_ICMA)
        .legalEntityId(LEGAL_ENTITY)
        .nominalPayment(NOMINAL)
        .periodicPayments(PERIODIC[0], PERIODIC[1], period, PERIODIC[3])
        .settlementDateOffset(SETTLE_OFFSET)
        .yieldConvention(US_IL_REAL)
        .build());
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    ExpandedCapitalIndexedBond test1 = ExpandedCapitalIndexedBond.builder()
        .dayCount(ACT_ACT_ICMA)
        .legalEntityId(LEGAL_ENTITY)
        .nominalPayment(NOMINAL)
        .periodicPayments(PERIODIC)
        .settlementDateOffset(SETTLE_OFFSET)
        .yieldConvention(US_IL_REAL)
        .build();
    coverImmutableBean(test1);
    ExpandedCapitalIndexedBond test2 = ExpandedCapitalIndexedBond.builder()
        .dayCount(NL_365)
        .legalEntityId(StandardId.of("OG-Ticker", "US-Govt1"))
        .nominalPayment(PERIODIC[1].withUnitCoupon(PERIODIC[0].getStartDate(), PERIODIC[0].getUnadjustedStartDate()))
        .periodicPayments(PERIODIC[0], PERIODIC[1])
        .settlementDateOffset(DaysAdjustment.ofBusinessDays(3, GBLO))
        .yieldConvention(INDEX_LINKED_FLOAT)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    ExpandedCapitalIndexedBond test = ExpandedCapitalIndexedBond.builder()
        .dayCount(ACT_ACT_ICMA)
        .legalEntityId(LEGAL_ENTITY)
        .nominalPayment(NOMINAL)
        .periodicPayments(PERIODIC)
        .settlementDateOffset(SETTLE_OFFSET)
        .yieldConvention(US_IL_REAL)
        .build();
    assertSerialization(test);
  }
  
}
