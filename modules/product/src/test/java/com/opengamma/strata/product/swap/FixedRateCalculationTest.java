/*
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.ReferenceData;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;
import com.opengamma.strata.product.rate.FixedOvernightCompoundedAnnualRateComputation;
import com.opengamma.strata.product.rate.FixedRateComputation;

/**
 * Test.
 */
public class FixedRateCalculationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  @Test
  public void test_of() {
    FixedRateCalculation test = FixedRateCalculation.of(0.025d, ACT_365F);
    assertThat(test.getType()).isEqualTo(SwapLegType.FIXED);
    assertThat(test.getRate()).isEqualTo(ValueSchedule.of(0.025d));
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getInitialStub()).isEqualTo(Optional.empty());
    assertThat(test.getFinalStub()).isEqualTo(Optional.empty());
    assertThat(test.getFutureValueNotional()).isEqualTo(Optional.empty());
  }

  @Test
  public void test_builder() {
    FixedRateCalculation test = FixedRateCalculation.builder()
        .dayCount(ACT_365F)
        .rate(ValueSchedule.of(0.025d))
        .initialStub(FixedRateStubCalculation.ofFixedRate(0.1d))
        .finalStub(FixedRateStubCalculation.ofFixedRate(0.2d))
        .futureValueNotional(FutureValueNotional.autoCalculate())
        .build();
    assertThat(test.getRate()).isEqualTo(ValueSchedule.of(0.025d));
    assertThat(test.getDayCount()).isEqualTo(ACT_365F);
    assertThat(test.getInitialStub()).isEqualTo(Optional.of(FixedRateStubCalculation.ofFixedRate(0.1d)));
    assertThat(test.getFinalStub()).isEqualTo(Optional.of(FixedRateStubCalculation.ofFixedRate(0.2d)));
    assertThat(test.getFutureValueNotional()).isEqualTo(Optional.of(FutureValueNotional.autoCalculate()));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_collectIndices() {
    FixedRateCalculation test = FixedRateCalculation.builder()
        .dayCount(ACT_365F)
        .rate(ValueSchedule.of(0.025d))
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertThat(builder.build()).isEmpty();
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_expand_oneValue() {
    FixedRateCalculation test = FixedRateCalculation.builder()
        .dayCount(ACT_365F)
        .rate(ValueSchedule.of(0.025d))
        .build();
    SchedulePeriod period1 = SchedulePeriod.of(date(2014, 1, 6), date(2014, 2, 5), date(2014, 1, 5), date(2014, 2, 5));
    SchedulePeriod period2 = SchedulePeriod.of(date(2014, 1, 5), date(2014, 2, 5), date(2014, 2, 5), date(2014, 3, 5));
    SchedulePeriod period3 = SchedulePeriod.of(date(2014, 3, 5), date(2014, 4, 7), date(2014, 3, 5), date(2014, 4, 5));
    Schedule schedule = Schedule.builder()
        .periods(period1, period2, period3)
        .frequency(Frequency.P1M)
        .rollConvention(RollConventions.DAY_5)
        .build();
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(period1)
        .yearFraction(period1.yearFraction(ACT_365F, schedule))
        .rateComputation(FixedRateComputation.of(0.025d))
        .build();
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder(period2)
        .yearFraction(period2.yearFraction(ACT_365F, schedule))
        .rateComputation(FixedRateComputation.of(0.025d))
        .build();
    RateAccrualPeriod rap3 = RateAccrualPeriod.builder(period3)
        .yearFraction(period3.yearFraction(ACT_365F, schedule))
        .rateComputation(FixedRateComputation.of(0.025d))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.createAccrualPeriods(schedule, schedule, REF_DATA);
    assertThat(periods).containsExactly(rap1, rap2, rap3);
  }

  @Test
  public void test_expand_distinctValues() {
    FixedRateCalculation test = FixedRateCalculation.builder()
        .dayCount(ACT_365F)
        .rate(ValueSchedule.of(
            0.025d,
            ValueStep.of(1, ValueAdjustment.ofReplace(0.020d)),
            ValueStep.of(2, ValueAdjustment.ofReplace(0.015d))))
        .build();
    SchedulePeriod period1 = SchedulePeriod.of(date(2014, 1, 6), date(2014, 2, 5), date(2014, 1, 5), date(2014, 2, 5));
    SchedulePeriod period2 = SchedulePeriod.of(date(2014, 1, 5), date(2014, 2, 5), date(2014, 2, 5), date(2014, 3, 5));
    SchedulePeriod period3 = SchedulePeriod.of(date(2014, 3, 5), date(2014, 4, 7), date(2014, 3, 5), date(2014, 4, 5));
    Schedule schedule = Schedule.builder()
        .periods(period1, period2, period3)
        .frequency(Frequency.P1M)
        .rollConvention(RollConventions.DAY_5)
        .build();
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(period1)
        .yearFraction(period1.yearFraction(ACT_365F, schedule))
        .rateComputation(FixedRateComputation.of(0.025d))
        .build();
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder(period2)
        .yearFraction(period2.yearFraction(ACT_365F, schedule))
        .rateComputation(FixedRateComputation.of(0.020d))
        .build();
    RateAccrualPeriod rap3 = RateAccrualPeriod.builder(period3)
        .yearFraction(period3.yearFraction(ACT_365F, schedule))
        .rateComputation(FixedRateComputation.of(0.015d))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.createAccrualPeriods(schedule, schedule, REF_DATA);
    assertThat(periods).containsExactly(rap1, rap2, rap3);
  }

  @Test
  public void test_expand_onlyInitialStub() {
    FixedRateCalculation test = FixedRateCalculation.builder()
        .dayCount(ACT_365F)
        .rate(ValueSchedule.of(0.025d))
        .initialStub(FixedRateStubCalculation.ofFixedRate(0.03d))
        .build();
    SchedulePeriod period1 = SchedulePeriod.of(date(2014, 1, 6), date(2014, 2, 9), date(2014, 1, 5), date(2014, 2, 9));
    Schedule schedule = Schedule.builder()
        .periods(period1)
        .frequency(Frequency.P1M)
        .rollConvention(RollConventions.DAY_5)
        .build();
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(period1)
        .yearFraction(period1.yearFraction(ACT_365F, schedule))
        .rateComputation(FixedRateComputation.of(0.03d))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.createAccrualPeriods(schedule, schedule, REF_DATA);
    assertThat(periods).containsExactly(rap1);
  }

  @Test
  public void test_expand_onlyFinalStub() {
    FixedRateCalculation test = FixedRateCalculation.builder()
        .dayCount(ACT_365F)
        .rate(ValueSchedule.of(0.025d))
        .finalStub(FixedRateStubCalculation.ofFixedRate(0.03d))
        .build();
    SchedulePeriod period1 = SchedulePeriod.of(date(2014, 1, 6), date(2014, 2, 9), date(2014, 1, 5), date(2014, 2, 9));
    Schedule schedule = Schedule.builder()
        .periods(period1)
        .frequency(Frequency.P1M)
        .rollConvention(RollConventions.DAY_5)
        .build();
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(period1)
        .yearFraction(period1.yearFraction(ACT_365F, schedule))
        .rateComputation(FixedRateComputation.of(0.03d))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.createAccrualPeriods(schedule, schedule, REF_DATA);
    assertThat(periods).containsExactly(rap1);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_expand_onePeriod_with_futureValueNotional() {
    FixedRateCalculation test = FixedRateCalculation.builder()
        .dayCount(ACT_365F)
        .rate(ValueSchedule.of(0.025d))
        .futureValueNotional(FutureValueNotional.of(1000d))
        .build();
    SchedulePeriod period = SchedulePeriod.of(date(2014, 1, 6), date(2014, 2, 5), date(2014, 1, 5), date(2014, 2, 5));
    Schedule schedule = Schedule.builder()
        .periods(period)
        .frequency(Frequency.TERM)
        .rollConvention(RollConventions.NONE)
        .build();
    double yearFraction = period.yearFraction(ACT_365F, schedule);
    RateAccrualPeriod rap = RateAccrualPeriod.builder(period)
        .yearFraction(period.yearFraction(ACT_365F, schedule))
        .rateComputation(FixedOvernightCompoundedAnnualRateComputation.of(0.025d, yearFraction))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.createAccrualPeriods(schedule, schedule, REF_DATA);
    assertThat(periods).containsExactly(rap);
  }

  @Test
  public void test_expand_multiplePeriod_with_futureValueNotional() {
    FixedRateCalculation test = FixedRateCalculation.builder()
        .dayCount(ACT_365F)
        .rate(ValueSchedule.of(0.025d))
        .futureValueNotional(FutureValueNotional.of(1000d))
        .build();
    SchedulePeriod period1 = SchedulePeriod.of(date(2014, 1, 6), date(2014, 2, 5), date(2014, 1, 5), date(2014, 2, 5));
    SchedulePeriod period2 = SchedulePeriod.of(date(2014, 1, 5), date(2014, 2, 5), date(2014, 2, 5), date(2014, 3, 5));
    SchedulePeriod period3 = SchedulePeriod.of(date(2014, 3, 5), date(2014, 4, 7), date(2014, 3, 5), date(2014, 4, 5));
    Schedule schedule = Schedule.builder()
        .periods(period1, period2, period3)
        .frequency(Frequency.P1M)
        .rollConvention(RollConventions.DAY_5)
        .build();
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.createAccrualPeriods(schedule, schedule, REF_DATA));
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    FixedRateCalculation test = FixedRateCalculation.builder()
        .dayCount(ACT_365F)
        .rate(ValueSchedule.of(0.025d))
        .build();
    coverImmutableBean(test);
    FixedRateCalculation test2 = FixedRateCalculation.builder()
        .dayCount(ACT_360)
        .rate(ValueSchedule.of(0.030d))
        .build();
    coverBeanEquals(test, test2);
  }

  @Test
  public void test_serialization() {
    FixedRateCalculation test = FixedRateCalculation.builder()
        .dayCount(ACT_365F)
        .rate(ValueSchedule.of(0.025d))
        .build();
    assertSerialization(test);
  }

}
