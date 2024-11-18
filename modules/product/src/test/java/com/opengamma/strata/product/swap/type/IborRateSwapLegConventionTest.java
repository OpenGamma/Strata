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
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.Frequency.P6M;
import static com.opengamma.strata.basics.schedule.StubConvention.LONG_INITIAL;
import static com.opengamma.strata.basics.schedule.StubConvention.SMART_INITIAL;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.product.common.PayReceive.PAY;
import static com.opengamma.strata.product.swap.FixingRelativeTo.PERIOD_END;
import static com.opengamma.strata.product.swap.FixingRelativeTo.PERIOD_START;
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
import com.opengamma.strata.product.swap.IborRateCalculation;
import com.opengamma.strata.product.swap.NotionalSchedule;
import com.opengamma.strata.product.swap.PaymentSchedule;
import com.opengamma.strata.product.swap.RateCalculationSwapLeg;

/**
 * Test {@link IborRateSwapLegConvention}.
 */
public class IborRateSwapLegConventionTest {

  private static final double NOTIONAL_2M = 2_000_000d;
  private static final BusinessDayAdjustment BDA_FOLLOW = BusinessDayAdjustment.of(FOLLOWING, GBLO);
  private static final BusinessDayAdjustment BDA_MOD_FOLLOW = BusinessDayAdjustment.of(MODIFIED_FOLLOWING, GBLO);
  private static final DaysAdjustment PLUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(2, GBLO);
  private static final DaysAdjustment MINUS_FIVE_DAYS = DaysAdjustment.ofBusinessDays(-5, GBLO);

  //-------------------------------------------------------------------------
  @Test
  public void test_of() {
    IborRateSwapLegConvention test = IborRateSwapLegConvention.of(GBP_LIBOR_3M);
    assertThat(test.getIndex()).isEqualTo(GBP_LIBOR_3M);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getAccrualFrequency()).isEqualTo(P3M);
    assertThat(test.getAccrualBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getStartDateBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getEndDateBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getStubConvention()).isEqualTo(StubConvention.SMART_INITIAL);
    assertThat(test.getRollConvention()).isEqualTo(RollConventions.EOM);
    assertThat(test.getFixingRelativeTo()).isEqualTo(PERIOD_START);
    assertThat(test.getFixingDateOffset()).isEqualTo(GBP_LIBOR_3M.getFixingDateOffset());
    assertThat(test.getPaymentFrequency()).isEqualTo(P3M);
    assertThat(test.getPaymentDateOffset()).isEqualTo(DaysAdjustment.NONE);
    assertThat(test.getCompoundingMethod()).isEqualTo(CompoundingMethod.NONE);
    assertThat(test.isNotionalExchange()).isFalse();
  }

  @Test
  public void test_builder() {
    IborRateSwapLegConvention test = IborRateSwapLegConvention.builder().index(GBP_LIBOR_3M).build();
    assertThat(test.getIndex()).isEqualTo(GBP_LIBOR_3M);
    assertThat(test.getCurrency()).isEqualTo(GBP);
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getAccrualFrequency()).isEqualTo(P3M);
    assertThat(test.getAccrualBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getStartDateBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getEndDateBusinessDayAdjustment()).isEqualTo(BDA_MOD_FOLLOW);
    assertThat(test.getStubConvention()).isEqualTo(StubConvention.SMART_INITIAL);
    assertThat(test.getRollConvention()).isEqualTo(RollConventions.EOM);
    assertThat(test.getFixingRelativeTo()).isEqualTo(PERIOD_START);
    assertThat(test.getFixingDateOffset()).isEqualTo(GBP_LIBOR_3M.getFixingDateOffset());
    assertThat(test.getPaymentFrequency()).isEqualTo(P3M);
    assertThat(test.getPaymentDateOffset()).isEqualTo(DaysAdjustment.NONE);
    assertThat(test.getCompoundingMethod()).isEqualTo(CompoundingMethod.NONE);
    assertThat(test.isNotionalExchange()).isFalse();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_builder_notEnoughData() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> IborRateSwapLegConvention.builder().build());
  }

  @Test
  public void test_builderAllSpecified() {
    IborRateSwapLegConvention test = IborRateSwapLegConvention.builder()
        .index(GBP_LIBOR_3M)
        .currency(USD)
        .dayCount(ACT_360)
        .accrualFrequency(P6M)
        .accrualBusinessDayAdjustment(BDA_FOLLOW)
        .startDateBusinessDayAdjustment(BDA_FOLLOW)
        .endDateBusinessDayAdjustment(BDA_FOLLOW)
        .stubConvention(LONG_INITIAL)
        .rollConvention(RollConventions.DAY_1)
        .fixingRelativeTo(PERIOD_END)
        .fixingDateOffset(MINUS_FIVE_DAYS)
        .paymentFrequency(P6M)
        .paymentDateOffset(PLUS_TWO_DAYS)
        .compoundingMethod(CompoundingMethod.FLAT)
        .notionalExchange(true)
        .build();
    assertThat(test.getIndex()).isEqualTo(GBP_LIBOR_3M);
    assertThat(test.getCurrency()).isEqualTo(USD);
    assertThat(test.getDayCount()).isEqualTo(ACT_360);
    assertThat(test.getAccrualFrequency()).isEqualTo(P6M);
    assertThat(test.getAccrualBusinessDayAdjustment()).isEqualTo(BDA_FOLLOW);
    assertThat(test.getStartDateBusinessDayAdjustment()).isEqualTo(BDA_FOLLOW);
    assertThat(test.getEndDateBusinessDayAdjustment()).isEqualTo(BDA_FOLLOW);
    assertThat(test.getStubConvention()).isEqualTo(StubConvention.LONG_INITIAL);
    assertThat(test.getRollConvention()).isEqualTo(RollConventions.DAY_1);
    assertThat(test.getFixingRelativeTo()).isEqualTo(PERIOD_END);
    assertThat(test.getFixingDateOffset()).isEqualTo(MINUS_FIVE_DAYS);
    assertThat(test.getPaymentFrequency()).isEqualTo(P6M);
    assertThat(test.getPaymentDateOffset()).isEqualTo(PLUS_TWO_DAYS);
    assertThat(test.getCompoundingMethod()).isEqualTo(CompoundingMethod.FLAT);
    assertThat(test.isNotionalExchange()).isTrue();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_toLeg() {
    IborRateSwapLegConvention base = IborRateSwapLegConvention.builder()
        .index(GBP_LIBOR_3M)
        .build();
    LocalDate startDate = LocalDate.of(2015, 5, 5);
    LocalDate endDate = LocalDate.of(2020, 5, 5);
    RateCalculationSwapLeg test = base.toLeg(startDate, endDate, PAY, NOTIONAL_2M);
    RateCalculationSwapLeg expected = RateCalculationSwapLeg.builder()
        .payReceive(PAY)
        .accrualSchedule(PeriodicSchedule.builder()
            .frequency(P3M)
            .startDate(startDate)
            .endDate(endDate)
            .businessDayAdjustment(BDA_MOD_FOLLOW)
            .stubConvention(SMART_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, NOTIONAL_2M))
        .calculation(IborRateCalculation.of(GBP_LIBOR_3M))
        .build();
    assertThat(test).isEqualTo(expected);
  }

  @Test
  public void test_toLeg_withSpread() {
    IborRateSwapLegConvention base = IborRateSwapLegConvention.builder()
        .index(GBP_LIBOR_3M)
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
            .stubConvention(SMART_INITIAL)
            .build())
        .paymentSchedule(PaymentSchedule.builder()
            .paymentFrequency(P3M)
            .paymentDateOffset(DaysAdjustment.NONE)
            .build())
        .notionalSchedule(NotionalSchedule.of(GBP, NOTIONAL_2M))
        .calculation(IborRateCalculation.builder()
            .index(GBP_LIBOR_3M)
            .spread(ValueSchedule.of(0.25d))
            .build())
        .build();
    assertThat(test).isEqualTo(expected);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    IborRateSwapLegConvention test = IborRateSwapLegConvention.builder()
        .index(GBP_LIBOR_3M)
        .build();
    coverImmutableBean(test);
    IborRateSwapLegConvention test2 = IborRateSwapLegConvention.builder()
        .index(GBP_LIBOR_3M)
        .currency(USD)
        .dayCount(ACT_360)
        .accrualFrequency(P6M)
        .accrualBusinessDayAdjustment(BDA_FOLLOW)
        .startDateBusinessDayAdjustment(BDA_FOLLOW)
        .endDateBusinessDayAdjustment(BDA_FOLLOW)
        .stubConvention(LONG_INITIAL)
        .rollConvention(RollConventions.EOM)
        .fixingRelativeTo(PERIOD_END)
        .fixingDateOffset(MINUS_FIVE_DAYS)
        .paymentFrequency(P6M)
        .paymentDateOffset(PLUS_TWO_DAYS)
        .notionalExchange(true)
        .build();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    IborRateSwapLegConvention test = IborRateSwapLegConvention.builder()
        .index(GBP_LIBOR_3M)
        .build();
    assertSerialization(test);
  }

}
