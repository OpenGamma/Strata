/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.bond;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.DayCounts.ACT_ACT_ISDA;
import static com.opengamma.strata.basics.date.DayCounts.NL_365;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.USNY;
import static com.opengamma.strata.basics.index.PriceIndices.GB_RPI;
import static com.opengamma.strata.basics.index.PriceIndices.US_CPI_U;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.bond.CapitalIndexedBondYieldConvention.INDEX_LINKED_FLOAT;
import static com.opengamma.strata.product.bond.CapitalIndexedBondYieldConvention.US_IL_REAL;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.BusinessDayConventions;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.market.ReferenceData;
import com.opengamma.strata.basics.market.StandardId;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;
import com.opengamma.strata.product.SecurityId;
import com.opengamma.strata.product.rate.RateObservation;
import com.opengamma.strata.product.swap.InflationRateCalculation;

/**
 * Test {@link CapitalIndexedBond}.
 */
@Test
public class CapitalIndexedBondTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final SecurityId SECURITY_ID = SecurityId.of("OG-Test", "Bond");
  private static final SecurityId SECURITY_ID2 = SecurityId.of("OG-Test", "Bond2");
  private static final double NOTIONAL = 10_000_000d;
  private static final double START_INDEX = 198.475;
  private static final double[] COUPONS = new double[] {0.01, 0.015, 0.012, 0.09 };
  private static final ValueSchedule COUPON;
  static {
    List<ValueStep> steps = new ArrayList<ValueStep>();
    steps.add(ValueStep.of(1, ValueAdjustment.ofReplace(COUPONS[1])));
    steps.add(ValueStep.of(2, ValueAdjustment.ofReplace(COUPONS[2])));
    steps.add(ValueStep.of(3, ValueAdjustment.ofReplace(COUPONS[3])));
    COUPON = ValueSchedule.of(COUPONS[0], steps);
  }
  private static final InflationRateCalculation RATE_CALC = InflationRateCalculation.builder()
      .gearing(COUPON)
      .index(US_CPI_U)
      .lag(Period.ofMonths(3))
      .interpolated(true)
      .firstIndexValue(START_INDEX)
      .build();
  private static final BusinessDayAdjustment EX_COUPON_ADJ =
      BusinessDayAdjustment.of(BusinessDayConventions.PRECEDING, USNY);
  private static final DaysAdjustment EX_COUPON = DaysAdjustment.ofCalendarDays(-7, EX_COUPON_ADJ);
  private static final DaysAdjustment SETTLE_OFFSET = DaysAdjustment.ofBusinessDays(2, USNY);
  private static final StandardId LEGAL_ENTITY = StandardId.of("OG-Ticker", "US-Govt");
  private static final LocalDate START = LocalDate.of(2008, 1, 13);
  private static final LocalDate END = LocalDate.of(2010, 1, 13);
  private static final Frequency FREQUENCY = Frequency.P6M;
  private static final BusinessDayAdjustment SCHEDULE_ADJ =
      BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, USNY);
  private static final PeriodicSchedule SCHEDULE =
      PeriodicSchedule.of(START, END, FREQUENCY, SCHEDULE_ADJ, StubConvention.NONE, RollConventions.NONE);

  public void test_builder_full() {
    CapitalIndexedBond test = sut();
    assertEquals(test.getSecurityId(), SECURITY_ID);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getDayCount(), ACT_ACT_ISDA);
    assertEquals(test.getExCouponPeriod(), EX_COUPON);
    assertEquals(test.getLegalEntityId(), LEGAL_ENTITY);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getAccrualSchedule(), SCHEDULE);
    assertEquals(test.getRateCalculation(), RATE_CALC);
    assertEquals(test.getFirstIndexValue(), RATE_CALC.getFirstIndexValue().getAsDouble());
    assertEquals(test.getSettlementDateOffset(), SETTLE_OFFSET);
    assertEquals(test.getYieldConvention(), US_IL_REAL);
  }

  public void test_builder_min() {
    CapitalIndexedBond test = CapitalIndexedBond.builder()
        .securityId(SECURITY_ID)
        .notional(NOTIONAL)
        .currency(USD)
        .dayCount(ACT_ACT_ISDA)
        .rateCalculation(RATE_CALC)
        .legalEntityId(LEGAL_ENTITY)
        .yieldConvention(US_IL_REAL)
        .settlementDateOffset(SETTLE_OFFSET)
        .accrualSchedule(SCHEDULE)
        .build();
    assertEquals(test.getSecurityId(), SECURITY_ID);
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getDayCount(), ACT_ACT_ISDA);
    assertEquals(test.getExCouponPeriod(), DaysAdjustment.NONE);
    assertEquals(test.getLegalEntityId(), LEGAL_ENTITY);
    assertEquals(test.getNotional(), NOTIONAL);
    assertEquals(test.getAccrualSchedule(), SCHEDULE);
    assertEquals(test.getRateCalculation(), RATE_CALC);
    assertEquals(test.getSettlementDateOffset(), SETTLE_OFFSET);
    assertEquals(test.getYieldConvention(), US_IL_REAL);
  }

  public void test_builder_fail() {
    // negative settlement date offset
    assertThrowsIllegalArg(() -> CapitalIndexedBond.builder()
        .securityId(SECURITY_ID)
        .notional(NOTIONAL)
        .currency(USD)
        .dayCount(ACT_ACT_ISDA)
        .rateCalculation(RATE_CALC)
        .exCouponPeriod(EX_COUPON)
        .legalEntityId(LEGAL_ENTITY)
        .yieldConvention(US_IL_REAL)
        .settlementDateOffset(DaysAdjustment.ofBusinessDays(-2, USNY))
        .accrualSchedule(SCHEDULE)
        .build());
    // positive ex-coupon days
    assertThrowsIllegalArg(() -> CapitalIndexedBond.builder()
        .securityId(SECURITY_ID)
        .notional(NOTIONAL)
        .currency(USD)
        .dayCount(ACT_ACT_ISDA)
        .rateCalculation(RATE_CALC)
        .exCouponPeriod(
            DaysAdjustment.ofCalendarDays(7, BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, USNY)))
        .legalEntityId(LEGAL_ENTITY)
        .yieldConvention(US_IL_REAL)
        .settlementDateOffset(SETTLE_OFFSET)
        .accrualSchedule(SCHEDULE)
        .build());
  }

  public void test_resolve() {
    CapitalIndexedBond base = sut();
    LocalDate[] unAdjDates = new LocalDate[] {LocalDate.of(2008, 1, 13), LocalDate.of(2008, 7, 13),
      LocalDate.of(2009, 1, 13), LocalDate.of(2009, 7, 13), LocalDate.of(2010, 1, 13) };
    CapitalIndexedBondPaymentPeriod [] periodic =new CapitalIndexedBondPaymentPeriod[4];
    for (int i = 0; i < 4; ++i) {
      LocalDate start = SCHEDULE_ADJ.adjust(unAdjDates[i], REF_DATA);
      LocalDate end = SCHEDULE_ADJ.adjust(unAdjDates[i + 1], REF_DATA);
      LocalDate detachment = EX_COUPON.adjust(end, REF_DATA);
      RateObservation obs = RATE_CALC.createRateObservation(end);
      periodic[i] = CapitalIndexedBondPaymentPeriod.builder()
          .currency(USD)
          .startDate(start)
          .endDate(end)
          .unadjustedStartDate(unAdjDates[i])
          .unadjustedEndDate(unAdjDates[i + 1])
          .detachmentDate(detachment)
          .realCoupon(COUPONS[i])
          .rateObservation(obs)
          .notional(NOTIONAL)
          .build();
    }
    CapitalIndexedBondPaymentPeriod nominalExp =
        periodic[3].withUnitCoupon(periodic[0].getStartDate(), periodic[0].getUnadjustedStartDate());
    ResolvedCapitalIndexedBond expected = ResolvedCapitalIndexedBond.builder()
        .securityId(SECURITY_ID)
        .dayCount(ACT_ACT_ISDA)
        .legalEntityId(LEGAL_ENTITY)
        .nominalPayment(nominalExp)
        .periodicPayments(periodic)
        .frequency(SCHEDULE.getFrequency())
        .rollConvention(SCHEDULE.calculatedRollConvention())
        .settlementDateOffset(SETTLE_OFFSET)
        .yieldConvention(US_IL_REAL)
        .rateCalculation(base.getRateCalculation())
        .build();
    assertEquals(base.resolve(REF_DATA), expected);
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
  static CapitalIndexedBond sut() {
    return CapitalIndexedBond.builder()
        .securityId(SECURITY_ID)
        .notional(NOTIONAL)
        .currency(USD)
        .dayCount(ACT_ACT_ISDA)
        .rateCalculation(RATE_CALC)
        .exCouponPeriod(EX_COUPON)
        .legalEntityId(LEGAL_ENTITY)
        .yieldConvention(US_IL_REAL)
        .settlementDateOffset(SETTLE_OFFSET)
        .accrualSchedule(SCHEDULE)
        .build();
  }

  static CapitalIndexedBond sut1() {
    return CapitalIndexedBond.builder()
        .securityId(SECURITY_ID)
        .notional(NOTIONAL)
        .currency(USD)
        .dayCount(ACT_ACT_ISDA)
        .rateCalculation(RATE_CALC)
        .exCouponPeriod(EX_COUPON)
        .legalEntityId(LEGAL_ENTITY)
        .yieldConvention(INDEX_LINKED_FLOAT)
        .settlementDateOffset(SETTLE_OFFSET)
        .accrualSchedule(SCHEDULE)
        .build();
  }

  static CapitalIndexedBond sut2() {
    return CapitalIndexedBond.builder()
        .securityId(SECURITY_ID2)
        .notional(5.0e7)
        .currency(GBP)
        .dayCount(NL_365)
        .rateCalculation(
            InflationRateCalculation.builder()
                .index(GB_RPI)
                .lag(Period.ofMonths(2))
                .interpolated(true)
                .firstIndexValue(124.556)
                .build())
        .exCouponPeriod(EX_COUPON)
        .legalEntityId(StandardId.of("OG-Ticker", "US-Govt-1"))
        .yieldConvention(INDEX_LINKED_FLOAT)
        .settlementDateOffset(DaysAdjustment.ofBusinessDays(2, GBLO))
        .accrualSchedule(
            PeriodicSchedule.of(
                START, END, FREQUENCY,
                BusinessDayAdjustment.of(BusinessDayConventions.FOLLOWING, GBLO),
                StubConvention.NONE,
                RollConventions.NONE))
        .build();
  }

}
