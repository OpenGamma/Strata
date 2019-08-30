/*
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
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.schedule.PeriodicSchedule;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.StubConvention;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.product.swap.CompoundingMethod;
import com.opengamma.strata.product.swap.FixedAccrualMethod;
import com.opengamma.strata.product.swap.FixedRateCalculation;
import com.opengamma.strata.product.swap.FutureValueNotional;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;

/**
 * Test {@link FixedRateSwapLegConvention}.
 */
public class FixedRateSwapLegConventionTest {

  private static final double NOTIONAL_2M = 2_000_000d;
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, GBLO);
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, GBLO);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    FixedRateSwapLegConvention test = FixedRateSwapLegConvention.of(GBP, ACT_365F, P3M, BDA_MOD_FOLLOW);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getAccrualFrequency()).isEqualTo(P3M);
    assertThat(test.getAccrualBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getStartDateBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getEndDateBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getStubConvention()).isEqualTo(StubConvention.SMART_INITIAL);
    assertThat(test.getRollConvention()).isEqualTo(RollConventions.EOM);
    assertThat(test.getPaymentFrequency()).isEqualTo(P3M);
    assertThat(test.getPaymentDateOffset()).isEqualTo(DaysAdjustment.NONE);
    assertThat(test.getCompoundingMethod()).isEqualTo(CompoundingMethod.NONE);
    assertThat(test.getAccrualMethod()).isEqualTo(FixedAccrualMethod.DEFAULT);
  }

  @Test
  public void test_builder() {
    FixedRateSwapLegConvention test = FixedRateSwapLegConvention.builder()
        .currency(GBP)
        .dayCount(ACT_365F)
        .accrualFrequency(P3M)
        .accrualBusinessDayAdjustment(BDA_MOD_FOLLOW)
        .build();
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getAccrualFrequency()).isEqualTo(P3M);
    assertThat(test.getAccrualBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getStartDateBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getEndDateBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getStubConvention()).isEqualTo(StubConvention.SMART_INITIAL);
    assertThat(test.getRollConvention()).isEqualTo(RollConventions.EOM);
    assertThat(test.getPaymentFrequency()).isEqualTo(P3M);
    assertThat(test.getPaymentDateOffset()).isEqualTo(DaysAdjustment.NONE);
    assertThat(test.getCompoundingMethod()).isEqualTo(CompoundingMethod.NONE);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_notEnoughData() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> FixedRateSwapLegConvention.builder().build());
  }

  @Test
  public void test_builderAllSpecified() {
    FixedRateSwapLegConvention test = FixedRateSwapLegConvention.builder()
        .currency(USD)
        .dayCount(ACT_360)
        .accrualFrequency(P6M)
        .accrualBusinessDayAdjustment(BDA_FOLLOW)
        .startDateBusinessDayAdjustment(BDA_FOLLOW)
        .endDateBusinessDayAdjustment(BDA_FOLLOW)
        .stubConvention(LONG_INITIAL)
        .rollConvention(RollConventions.DAY_1)
        .paymentFrequency(P6M)
        .paymentDateOffset(PLUS_TWO_DAYS)
        .compoundingMethod(CompoundingMethod.FLAT)
        .accrualMethod(FixedAccrualMethod.OVERNIGHT_COMPOUNDED_ANNUAL_RATE)
        .build();
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.getAccrualFrequency()).isEqualTo(P6M);
    assertThat(test.getAccrualBusinessDayAdjustment()).isEqualTo(BDA_FOLLOW);
    assertThat(test.getStartDateBusinessDayAdjustment()).isEqualTo(BDA_FOLLOW);
    assertThat(test.getEndDateBusinessDayAdjustment()).isEqualTo(BDA_FOLLOW);
    assertThat(test.getStubConvention()).isEqualTo(StubConvention.LONG_INITIAL);
    assertThat(test.getRollConvention()).isEqualTo(RollConventions.DAY_1);
    assertThat(test.getPaymentFrequency()).isEqualTo(P6M);
    assertThat(test.getPaymentDateOffset()).isEqualTo(PLUS_TWO_DAYS);
    assertThat(test.getCompoundingMethod()).isEqualTo(CompoundingMethod.FLAT);
    assertThat(test.getAccrualMethod()).isEqualTo(FixedAccrualMethod.OVERNIGHT_COMPOUNDED_ANNUAL_RATE);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toLeg1() {
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
            .stubConvention(StubConvention.SMART_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, NOTIONAL_2M))
        .calculation(FixedRateCalculation.of(0.25d, ACT_365F))
        .build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_toLeg2() {
    FixedRateSwapLegConvention base = FixedRateSwapLegConvention.builder()
        .currency(GBP)
        .dayCount(ACT_365F)
        .accrualFrequency(P3M)
        .accrualBusinessDayAdjustment(BDA_MOD_FOLLOW)
        .accrualMethod(FixedAccrualMethod.OVERNIGHT_COMPOUNDED_ANNUAL_RATE)
        .stubConvention(StubConvention.SMART_INITIAL)
        .build();
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
            .stubConvention(StubConvention.SMART_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, NOTIONAL_2M))
        .calculation(FixedRateCalculation.builder()
            .rate(ValueSchedule.of(0.25d))
            .dayCount(ACT_365F)
            .futureValueNotional(FutureValueNotional.autoCalculate())
            .build())
        .build();
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
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

  @Test
  public void test_serialization() {
    FixedRateSwapLegConvention test = FixedRateSwapLegConvention.of(GBP, ACT_365F, P3M, BDA_MOD_FOLLOW);
    assertSerialization(test);
  }

}
