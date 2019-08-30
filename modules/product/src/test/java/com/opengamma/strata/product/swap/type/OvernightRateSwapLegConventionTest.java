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
import static com.opengamma.strata.basics.index.OvernightIndices.GBP_SONIA;
import static com.opengamma.strata.basics.index.OvernightIndices.USD_FED_FUND;
import static com.opengamma.strata.basics.schedule.Frequency.P12M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.basics.schedule.Frequency.TERM;
import static com.opengamma.strata.basics.schedule.StubConvention.LONG_INITIAL;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.swap.OvernightAccrualMethod.AVERAGED;
import static com.opengamma.strata.product.swap.OvernightAccrualMethod.COMPOUNDED;
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
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.OvernightRateCalculation;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;

/**
 * Test {@link OvernightRateSwapLegConvention}.
 */
public class OvernightRateSwapLegConventionTest {

  private static final double NOTIONAL_2M = 2_000_000d;
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, GBLO);
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, GBLO);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    OvernightRateSwapLegConvention test = OvernightRateSwapLegConvention.of(GBP_SONIA, P12M, 2);
    assertThat(test.getIndex()).isEqualTo(GBP_SONIA);
    assertThat(test.getAccrualMethod()).isEqualTo(COMPOUNDED);
    assertThat(test.getRateCutOffDays()).isEqualTo(0);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getAccrualFrequency()).isEqualTo(P12M);
    assertThat(test.getAccrualBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getStartDateBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getEndDateBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getStubConvention()).isEqualTo(StubConvention.SMART_INITIAL);
    assertThat(test.getRollConvention()).isEqualTo(RollConventions.EOM);
    assertThat(test.getPaymentFrequency()).isEqualTo(P12M);
    assertThat(test.getPaymentDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(2, GBP_SONIA.getFixingCalendar()));
    assertThat(test.getCompoundingMethod()).isEqualTo(CompoundingMethod.NONE);
  }

  @Test
  public void test_of_method() {
    OvernightRateSwapLegConvention test = OvernightRateSwapLegConvention.of(GBP_SONIA, P12M, 2, AVERAGED);
    assertThat(test.getIndex()).isEqualTo(GBP_SONIA);
    assertThat(test.getAccrualMethod()).isEqualTo(AVERAGED);
    assertThat(test.getRateCutOffDays()).isEqualTo(0);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getAccrualFrequency()).isEqualTo(P12M);
    assertThat(test.getAccrualBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getStartDateBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getEndDateBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getStubConvention()).isEqualTo(StubConvention.SMART_INITIAL);
    assertThat(test.getRollConvention()).isEqualTo(RollConventions.EOM);
    assertThat(test.getPaymentFrequency()).isEqualTo(P12M);
    assertThat(test.getPaymentDateOffset()).isEqualTo(DaysAdjustment.ofBusinessDays(2, GBP_SONIA.getFixingCalendar()));
    assertThat(test.getCompoundingMethod()).isEqualTo(CompoundingMethod.NONE);
  }

  @Test
  public void test_builder() {
    OvernightRateSwapLegConvention test = OvernightRateSwapLegConvention.builder()
        .index(GBP_SONIA)
        .build();
    assertThat(test.getIndex()).isEqualTo(GBP_SONIA);
    assertThat(test.getAccrualMethod()).isEqualTo(COMPOUNDED);
    assertThat(test.getRateCutOffDays()).isEqualTo(0);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getAccrualFrequency()).isEqualTo(TERM);
    assertThat(test.getAccrualBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getStartDateBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getEndDateBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getStubConvention()).isEqualTo(StubConvention.SMART_INITIAL);
    assertThat(test.getRollConvention()).isEqualTo(RollConventions.EOM);
    assertThat(test.getPaymentFrequency()).isEqualTo(TERM);
    assertThat(test.getPaymentDateOffset()).isEqualTo(DaysAdjustment.NONE);
    assertThat(test.getCompoundingMethod()).isEqualTo(CompoundingMethod.NONE);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_notEnoughData() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> OvernightRateSwapLegConvention.builder().build());
  }

  @Test
  public void test_builderAllSpecified() {
    OvernightRateSwapLegConvention test = OvernightRateSwapLegConvention.builder()
        .index(GBP_SONIA)
        .accrualMethod(COMPOUNDED)
        .rateCutOffDays(2)
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
        .build();
    assertThat(test.getIndex()).isEqualTo(GBP_SONIA);
    assertThat(test.getAccrualMethod()).isEqualTo(COMPOUNDED);
    assertThat(test.getRateCutOffDays()).isEqualTo(2);
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
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toLeg() {
    OvernightRateSwapLegConvention base = OvernightRateSwapLegConvention.of(GBP_SONIA, TERM, 2);
    LocalDate startDate = LocalDate.of(2015, 5, 5);
    LocalDate endDate = LocalDate.of(2020, 5, 5);
    RateCalculationSwapLeg test = base.toLeg(startDate, endDate, PAY, NOTIONAL_2M);
    RateCalculationSwapLeg expected = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .frequency(TERM)
            .startDate(startDate)
            .endDate(endDate)
            .businessDayAdjustment(BDA_MOD_FOLLOW)
            .stubConvention(StubConvention.SMART_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(TERM)
            .paymentDateOffset(DaysAdjustment.ofBusinessDays(2, GBP_SONIA.getFixingCalendar()))
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, NOTIONAL_2M))
        .calculation(OvernightRateCalculation.of(GBP_SONIA))
        .build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_toLeg_withSpread() {
    OvernightRateSwapLegConvention base = OvernightRateSwapLegConvention.builder()
        .index(GBP_SONIA)
        .accrualMethod(AVERAGED)
        .build();
    LocalDate startDate = LocalDate.of(2015, 5, 5);
    LocalDate endDate = LocalDate.of(2020, 5, 5);
    RateCalculationSwapLeg test = base.toLeg(startDate, endDate, PAY, NOTIONAL_2M, 0.25d);
    RateCalculationSwapLeg expected = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .frequency(TERM)
            .startDate(startDate)
            .endDate(endDate)
            .businessDayAdjustment(BDA_MOD_FOLLOW)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(TERM)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, NOTIONAL_2M))
        .calculation(OvernightRateCalculation.builder()
            .index(GBP_SONIA)
            .accrualMethod(AVERAGED)
            .spread(ValueSchedule.of(0.25d))
            .build())
        .build();
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    OvernightRateSwapLegConvention test = OvernightRateSwapLegConvention.builder()
        .index(GBP_SONIA)
        .accrualMethod(COMPOUNDED)
        .build();
    coverImmutableBean(test);
    OvernightRateSwapLegConvention test2 = OvernightRateSwapLegConvention.builder()
        .index(USD_FED_FUND)
        .accrualMethod(AVERAGED)
        .rateCutOffDays(2)
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
    OvernightRateSwapLegConvention test = OvernightRateSwapLegConvention.of(GBP_SONIA, P12M, 2);
    assertSerialization(test);
  }

}
