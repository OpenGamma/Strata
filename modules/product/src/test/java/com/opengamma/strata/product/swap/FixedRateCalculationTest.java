/**
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
import static org.testng.Assert.assertEquals;

import java.util.Optional;

import org.testng.annotations.Test;

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
import com.opengamma.strata.product.rate.FixedRateComputation;

/**
 * Test.
 */
@Test
public class FixedRateCalculationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();

  public void test_of() {
    FixedRateCalculation test = FixedRateCalculation.of(0.025d, ACT_365F);
    assertEquals(test.getType(), SwapLegType.FIXED);
    assertEquals(test.getRate(), ValueSchedule.of(0.025d));
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getInitialStub(), Optional.empty());
    assertEquals(test.getFinalStub(), Optional.empty());
  }

  public void test_builder() {
    FixedRateCalculation test = FixedRateCalculation.builder()
        .dayCount(ACT_365F)
        .rate(ValueSchedule.of(0.025d))
        .initialStub(FixedRateStubCalculation.ofFixedRate(0.1d))
        .finalStub(FixedRateStubCalculation.ofFixedRate(0.2d))
        .build();
    assertEquals(test.getRate(), ValueSchedule.of(0.025d));
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getInitialStub(), Optional.of(FixedRateStubCalculation.ofFixedRate(0.1d)));
    assertEquals(test.getFinalStub(), Optional.of(FixedRateStubCalculation.ofFixedRate(0.2d)));
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    FixedRateCalculation test = FixedRateCalculation.builder()
        .dayCount(ACT_365F)
        .rate(ValueSchedule.of(0.025d))
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of());
  }

  //-------------------------------------------------------------------------
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
    assertEquals(periods, ImmutableList.of(rap1, rap2, rap3));
  }

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
    assertEquals(periods, ImmutableList.of(rap1, rap2, rap3));
  }

  //-------------------------------------------------------------------------
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

  public void test_serialization() {
    FixedRateCalculation test = FixedRateCalculation.builder()
        .dayCount(ACT_365F)
        .rate(ValueSchedule.of(0.025d))
        .build();
    assertSerialization(test);
  }

}
