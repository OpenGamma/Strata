/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap.type;

import static com.opengamma.strata.basics.currency.Currency.GBP;
import static com.opengamma.strata.basics.currency.Currency.USD;
import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.BusinessDayConventions.MODIFIED_FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendarIds.GBLO;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.basics.schedule.StubConvention.LONG_INITIAL;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;

import org.testng.annotations.Test;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.product.swap.CompoundingMethod;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;

/**
 * Test {@link FixedRateSwapLegConvention}.
 */
@Test
public class FixedRateSwapLegConventionTest {

  private static final double NOTIONAL_2M = 2_000_000d;
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, GBLO);
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, GBLO);

  //-------------------------------------------------------------------------
  public void test_of() {
    FixedRateSwapLegConvention test = FixedRateSwapLegConvention.of(GBP, ACT_365F, P3M, BDA_MOD_FOLLOW);
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getAccrualFrequency(), P3M);
    assertEquals(test.getAccrualBusinessDayAdjustment(), BDA_MOD_FOLLOW);
    assertEquals(test.getStartDateBusinessDayAdjustment(), BDA_MOD_FOLLOW);
    assertEquals(test.getEndDateBusinessDayAdjustment(), BDA_MOD_FOLLOW);
    assertEquals(test.getStubConvention(), StubConvention.SHORT_INITIAL);
    assertEquals(test.getRollConvention(), RollConventions.NONE);
    assertEquals(test.getPaymentFrequency(), P3M);
    assertEquals(test.getPaymentDateOffset(), DaysAdjustment.NONE);
    assertEquals(test.getCompoundingMethod(), CompoundingMethod.NONE);
  }

  public void test_builder() {
    FixedRateSwapLegConvention test = FixedRateSwapLegConvention.builder()
        .currency(GBP)
        .dayCount(ACT_365F)
        .accrualFrequency(P3M)
        .accrualBusinessDayAdjustment(BDA_MOD_FOLLOW)
        .build();
    assertEquals(test.getCurrency(), GBP);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getAccrualFrequency(), P3M);
    assertEquals(test.getAccrualBusinessDayAdjustment(), BDA_MOD_FOLLOW);
    assertEquals(test.getStartDateBusinessDayAdjustment(), BDA_MOD_FOLLOW);
    assertEquals(test.getEndDateBusinessDayAdjustment(), BDA_MOD_FOLLOW);
    assertEquals(test.getStubConvention(), StubConvention.SHORT_INITIAL);
    assertEquals(test.getRollConvention(), RollConventions.NONE);
    assertEquals(test.getPaymentFrequency(), P3M);
    assertEquals(test.getPaymentDateOffset(), DaysAdjustment.NONE);
    assertEquals(test.getCompoundingMethod(), CompoundingMethod.NONE);
  }

  //-------------------------------------------------------------------------
  public void test_builder_notEnoughData() {
    assertThrowsIllegalArg(() -> FixedRateSwapLegConvention.builder().build());
  }

  public void test_builderAllSpecified() {
    FixedRateSwapLegConvention test = FixedRateSwapLegConvention.builder()
        .currency(USD)
        .dayCount(ACT_360)
        .accrualFrequency(P6M)
        .accrualBusinessDayAdjustment(BDA_FOLLOW)
        .startDateBusinessDayAdjustment(BDA_FOLLOW)
        .endDateBusinessDayAdjustment(BDA_FOLLOW)
        .stubConvention(LONG_INITIAL)
        .rollConvention(RollConventions.EOM)
        .paymentFrequency(P6M)
        .paymentDateOffset(PLUS_TWO_DAYS)
        .compoundingMethod(CompoundingMethod.FLAT)
        .build();
    assertEquals(test.getCurrency(), USD);
    assertEquals(test.getDayCount(), ACT_360);
    assertEquals(test.getAccrualFrequency(), P6M);
    assertEquals(test.getAccrualBusinessDayAdjustment(), BDA_FOLLOW);
    assertEquals(test.getStartDateBusinessDayAdjustment(), BDA_FOLLOW);
    assertEquals(test.getEndDateBusinessDayAdjustment(), BDA_FOLLOW);
    assertEquals(test.getStubConvention(), StubConvention.LONG_INITIAL);
    assertEquals(test.getRollConvention(), RollConventions.EOM);
    assertEquals(test.getPaymentFrequency(), P6M);
    assertEquals(test.getPaymentDateOffset(), PLUS_TWO_DAYS);
    assertEquals(test.getCompoundingMethod(), CompoundingMethod.FLAT);
  }

  //-------------------------------------------------------------------------
  public void test_toLeg() {
    FixedRateSwapLegConvention base = FixedRateSwapLegConvention.of(GBP, ACT_365F, P3M, BDA_MOD_FOLLOW);
    LocalDate startDate = LocalDate.of(2015, 5, 5);
    LocalDate endDate = LocalDate.of(2020, 5, 5);
    RateCalculationSwapLeg test = base.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d);
    RateCalculationSwapLeg expected = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .frequency(P3M)
            .startDate(startDate)
            .endDate(endDate)
            .businessDayAdjustment(BDA_MOD_FOLLOW)
            .stubConvention(StubConvention.SHORT_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, NOTIONAL_2M))
        .calculation(FixedRateCalculation.of(0.25d, ACT_365F))
        .build();
    assertEquals(test, expected);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    FixedRateSwapLegConvention test = FixedRateSwapLegConvention.of(GBP, ACT_365F, P3M, BDA_MOD_FOLLOW);
    coverImmutableBean(test);
    FixedRateSwapLegConvention test2 = FixedRateSwapLegConvention.builder()
        .currency(USD)
        .dayCount(ACT_360)
        .accrualFrequency(P6M)
        .accrualBusinessDayAdjustment(BDA_FOLLOW)
        .startDateBusinessDayAdjustment(BDA_FOLLOW)
        .endDateBusinessDayAdjustment(BDA_FOLLOW)
        .stubConvention(LONG_INITIAL)
        .rollConvention(RollConventions.EOM)
        .paymentFrequency(P6M)
        .paymentDateOffset(PLUS_TWO_DAYS)
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    FixedRateSwapLegConvention test = FixedRateSwapLegConvention.of(GBP, ACT_365F, P3M, BDA_MOD_FOLLOW);
    assertSerialization(test);
  }

}
