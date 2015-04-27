/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate.swap;

import static com.opengamma.strata.basics.date.BusinessDayConventions.FOLLOWING;
import static com.opengamma.strata.basics.date.DayCounts.ACT_360;
import static com.opengamma.strata.basics.date.DayCounts.ACT_365F;
import static com.opengamma.strata.basics.date.HolidayCalendars.GBLO;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_1W;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_6M;
import static com.opengamma.strata.basics.schedule.Frequency.P1M;
import static com.opengamma.strata.basics.schedule.Frequency.P3M;
import static com.opengamma.strata.basics.schedule.RollConventions.DAY_5;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.finance.rate.swap.FixingRelativeTo.PERIOD_END;
import static com.opengamma.strata.finance.rate.swap.FixingRelativeTo.PERIOD_START;
import static com.opengamma.strata.finance.rate.swap.IborRateAveragingMethod.UNWEIGHTED;
import static com.opengamma.strata.finance.rate.swap.IborRateAveragingMethod.WEIGHTED;
import static com.opengamma.strata.finance.rate.swap.NegativeRateMethod.ALLOW_NEGATIVE;
import static com.opengamma.strata.finance.rate.swap.NegativeRateMethod.NOT_NEGATIVE;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalDouble;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.date.BusinessDayAdjustment;
import com.opengamma.strata.basics.date.DaysAdjustment;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;
import com.opengamma.strata.finance.rate.FixedRateObservation;
import com.opengamma.strata.finance.rate.IborAveragedFixing;
import com.opengamma.strata.finance.rate.IborAveragedRateObservation;
import com.opengamma.strata.finance.rate.IborInterpolatedRateObservation;
import com.opengamma.strata.finance.rate.IborRateObservation;

/**
 * Test.
 */
@Test
public class IborRateCalculationTest {

  private static final LocalDate DATE_01_02 = date(2014, 1, 2);
  private static final LocalDate DATE_01_05 = date(2014, 1, 5);
  private static final LocalDate DATE_01_06 = date(2014, 1, 6);
  private static final LocalDate DATE_01_08 = date(2014, 1, 8);
  private static final LocalDate DATE_01_31 = date(2014, 1, 31);
  private static final LocalDate DATE_02_03 = date(2014, 2, 3);
  private static final LocalDate DATE_02_05 = date(2014, 2, 5);
  private static final LocalDate DATE_02_28 = date(2014, 2, 28);
  private static final LocalDate DATE_03_03 = date(2014, 3, 3);
  private static final LocalDate DATE_03_05 = date(2014, 3, 5);
  private static final LocalDate DATE_04_02 = date(2014, 4, 2);
  private static final LocalDate DATE_04_03 = date(2014, 4, 3);
  private static final LocalDate DATE_04_04 = date(2014, 4, 4);
  private static final LocalDate DATE_04_05 = date(2014, 4, 5);
  private static final LocalDate DATE_04_07 = date(2014, 4, 7);
  private static final LocalDate DATE_05_01 = date(2014, 5, 1);
  private static final LocalDate DATE_05_06 = date(2014, 5, 6);
  private static final LocalDate DATE_06_03 = date(2014, 6, 3);
  private static final LocalDate DATE_06_05 = date(2014, 6, 5);
  private static final LocalDate DATE_07_05 = date(2014, 7, 5);
  private static final LocalDate DATE_07_07 = date(2014, 7, 7);
  private static final DaysAdjustment MINUS_TWO_DAYS = DaysAdjustment.ofBusinessDays(-2, GBLO);
  private static final DaysAdjustment MINUS_THREE_DAYS = DaysAdjustment.ofBusinessDays(-3, GBLO);

  private static final SchedulePeriod ACCRUAL1STUB = SchedulePeriod.of(DATE_01_08, DATE_02_05, DATE_01_08, DATE_02_05);
  private static final SchedulePeriod ACCRUAL1 = SchedulePeriod.of(DATE_01_06, DATE_02_05, DATE_01_05, DATE_02_05);
  private static final SchedulePeriod ACCRUAL2 = SchedulePeriod.of(DATE_02_05, DATE_03_05, DATE_02_05, DATE_03_05);
  private static final SchedulePeriod ACCRUAL3 = SchedulePeriod.of(DATE_03_05, DATE_04_07, DATE_03_05, DATE_04_05);
  private static final SchedulePeriod ACCRUAL3STUB = SchedulePeriod.of(DATE_03_05, DATE_04_04, DATE_03_05, DATE_04_04);
  private static final Schedule ACCRUAL_SCHEDULE = Schedule.builder()
      .periods(ACCRUAL1, ACCRUAL2, ACCRUAL3)
      .frequency(P1M)
      .rollConvention(DAY_5)
      .build();
  private static final Schedule ACCRUAL_SCHEDULE_STUBS = Schedule.builder()
      .periods(ACCRUAL1STUB, ACCRUAL2, ACCRUAL3STUB)
      .frequency(P1M)
      .rollConvention(DAY_5)
      .build();
  private static final Schedule ACCRUAL_SCHEDULE_INITIAL_STUB = Schedule.builder()
      .periods(ACCRUAL1STUB, ACCRUAL2, ACCRUAL3)
      .frequency(P1M)
      .rollConvention(DAY_5)
      .build();
  private static final Schedule ACCRUAL_SCHEDULE_FINAL_STUB = Schedule.builder()
      .periods(ACCRUAL1, ACCRUAL2, ACCRUAL3STUB)
      .frequency(P1M)
      .rollConvention(DAY_5)
      .build();

  //-------------------------------------------------------------------------
  public void test_of() {
    IborRateCalculation test = IborRateCalculation.of(GBP_LIBOR_3M);
    assertEquals(test.getType(), SwapLegType.IBOR);
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getResetPeriods(), Optional.empty());
    assertEquals(test.getFixingRelativeTo(), PERIOD_START);
    assertEquals(test.getFixingOffset(), DaysAdjustment.NONE);
    assertEquals(test.getNegativeRateMethod(), ALLOW_NEGATIVE);
    assertEquals(test.getFirstRegularRate(), OptionalDouble.empty());
    assertEquals(test.getInitialStub(), Optional.empty());
    assertEquals(test.getFinalStub(), Optional.empty());
    assertEquals(test.getGearing(), Optional.empty());
    assertEquals(test.getSpread(), Optional.empty());
  }

  public void test_builder_ensureDefaults() {
    IborRateCalculation test = IborRateCalculation.builder()
        .index(GBP_LIBOR_3M)
        .build();
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getResetPeriods(), Optional.empty());
    assertEquals(test.getFixingRelativeTo(), PERIOD_START);
    assertEquals(test.getFixingOffset(), DaysAdjustment.NONE);
    assertEquals(test.getNegativeRateMethod(), ALLOW_NEGATIVE);
    assertEquals(test.getFirstRegularRate(), OptionalDouble.empty());
    assertEquals(test.getInitialStub(), Optional.empty());
    assertEquals(test.getFinalStub(), Optional.empty());
    assertEquals(test.getGearing(), Optional.empty());
    assertEquals(test.getSpread(), Optional.empty());
  }

  public void test_builder_ensureOptionalDouble() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_3M)
        .fixingOffset(MINUS_TWO_DAYS)
        .firstRegularRate(0.028d)
        .build();
    assertEquals(test.getDayCount(), ACT_365F);
    assertEquals(test.getIndex(), GBP_LIBOR_3M);
    assertEquals(test.getResetPeriods(), Optional.empty());
    assertEquals(test.getFixingRelativeTo(), PERIOD_START);
    assertEquals(test.getFixingOffset(), MINUS_TWO_DAYS);
    assertEquals(test.getNegativeRateMethod(), ALLOW_NEGATIVE);
    assertEquals(test.getFirstRegularRate(), OptionalDouble.of(0.028d));
    assertEquals(test.getInitialStub(), Optional.empty());
    assertEquals(test.getFinalStub(), Optional.empty());
    assertEquals(test.getGearing(), Optional.empty());
    assertEquals(test.getSpread(), Optional.empty());
  }

  public void test_builder_noIndex() {
    assertThrowsIllegalArg(() -> IborRateCalculation.builder().build());
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices_simple() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_1M)
        .fixingOffset(MINUS_TWO_DAYS)
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GBP_LIBOR_1M));
  }

  public void test_collectIndices_stubCalcsTwoStubs() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_1M)
        .fixingOffset(MINUS_TWO_DAYS)
        .initialStub(StubCalculation.ofIborRate(GBP_LIBOR_1W))
        .finalStub(StubCalculation.ofIborRate(GBP_LIBOR_3M))
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GBP_LIBOR_1M, GBP_LIBOR_1W, GBP_LIBOR_3M));
  }

  public void test_collectIndices_stubCalcsTwoStubs_interpolated() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_1M)
        .fixingOffset(MINUS_TWO_DAYS)
        .initialStub(StubCalculation.ofIborInterpolatedRate(GBP_LIBOR_1W, GBP_LIBOR_1M))
        .finalStub(StubCalculation.ofIborInterpolatedRate(GBP_LIBOR_3M, GBP_LIBOR_1M))
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GBP_LIBOR_1M, GBP_LIBOR_1W, GBP_LIBOR_3M));
  }

  //-------------------------------------------------------------------------
  public void test_expand_simple() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_1M)
        .fixingOffset(MINUS_TWO_DAYS)
        .build();
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(ACCRUAL1)
        .yearFraction(ACCRUAL1.yearFraction(ACT_365F, ACCRUAL_SCHEDULE))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_01_02))
        .build();
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder(ACCRUAL2)
        .yearFraction(ACCRUAL2.yearFraction(ACT_365F, ACCRUAL_SCHEDULE))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_02_03))
        .build();
    RateAccrualPeriod rap3 = RateAccrualPeriod.builder(ACCRUAL3)
        .yearFraction(ACCRUAL3.yearFraction(ACT_365F, ACCRUAL_SCHEDULE))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_03_03))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.expand(ACCRUAL_SCHEDULE, ACCRUAL_SCHEDULE);
    assertEquals(periods, ImmutableList.of(rap1, rap2, rap3));
  }

  public void test_expand_simpleFinalStub() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_1M)
        .fixingOffset(MINUS_TWO_DAYS)
        .build();
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(ACCRUAL1)
        .yearFraction(ACCRUAL1.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_FINAL_STUB))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_01_02))
        .build();
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder(ACCRUAL2)
        .yearFraction(ACCRUAL2.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_FINAL_STUB))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_02_03))
        .build();
    RateAccrualPeriod rap3 = RateAccrualPeriod.builder(ACCRUAL3STUB)
        .yearFraction(ACCRUAL3STUB.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_FINAL_STUB))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_03_03))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.expand(ACCRUAL_SCHEDULE_FINAL_STUB, ACCRUAL_SCHEDULE_FINAL_STUB);
    assertEquals(periods, ImmutableList.of(rap1, rap2, rap3));
  }

  public void test_expand_simpleInitialStub() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_1M)
        .fixingOffset(MINUS_TWO_DAYS)
        .build();
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(ACCRUAL1STUB)
        .yearFraction(ACCRUAL1STUB.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_INITIAL_STUB))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_01_06))
        .build();
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder(ACCRUAL2)
        .yearFraction(ACCRUAL2.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_INITIAL_STUB))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_02_03))
        .build();
    RateAccrualPeriod rap3 = RateAccrualPeriod.builder(ACCRUAL3)
        .yearFraction(ACCRUAL3.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_INITIAL_STUB))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_03_03))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.expand(ACCRUAL_SCHEDULE_INITIAL_STUB, ACCRUAL_SCHEDULE_INITIAL_STUB);
    assertEquals(periods, ImmutableList.of(rap1, rap2, rap3));
  }

  public void test_expand_simpleTwoStubs() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_1M)
        .fixingOffset(MINUS_TWO_DAYS)
        .build();
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(ACCRUAL1STUB)
        .yearFraction(ACCRUAL1STUB.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_STUBS))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_01_06))
        .build();
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder(ACCRUAL2)
        .yearFraction(ACCRUAL2.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_STUBS))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_02_03))
        .build();
    RateAccrualPeriod rap3 = RateAccrualPeriod.builder(ACCRUAL3STUB)
        .yearFraction(ACCRUAL3STUB.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_STUBS))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_03_03))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.expand(ACCRUAL_SCHEDULE_STUBS, ACCRUAL_SCHEDULE_STUBS);
    assertEquals(periods, ImmutableList.of(rap1, rap2, rap3));
  }

  //-------------------------------------------------------------------------
  public void test_expand_stubCalcsTwoStubs() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_1M)
        .fixingOffset(MINUS_TWO_DAYS)
        .initialStub(StubCalculation.ofIborRate(GBP_LIBOR_1W))
        .finalStub(StubCalculation.ofIborRate(GBP_LIBOR_3M))
        .build();
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(ACCRUAL1STUB)
        .yearFraction(ACCRUAL1STUB.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_STUBS))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1W, DATE_01_06))
        .build();
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder(ACCRUAL2)
        .yearFraction(ACCRUAL2.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_STUBS))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_02_03))
        .build();
    RateAccrualPeriod rap3 = RateAccrualPeriod.builder(ACCRUAL3STUB)
        .yearFraction(ACCRUAL3STUB.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_STUBS))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_3M, DATE_03_03))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.expand(ACCRUAL_SCHEDULE_STUBS, ACCRUAL_SCHEDULE_STUBS);
    assertEquals(periods, ImmutableList.of(rap1, rap2, rap3));
  }

  public void test_expand_stubCalcsTwoStubs_interpolated() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_1M)
        .fixingOffset(MINUS_TWO_DAYS)
        .initialStub(StubCalculation.ofIborInterpolatedRate(GBP_LIBOR_1W, GBP_LIBOR_1M))
        .finalStub(StubCalculation.ofIborInterpolatedRate(GBP_LIBOR_3M, GBP_LIBOR_1M))
        .build();
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(ACCRUAL1STUB)
        .yearFraction(ACCRUAL1STUB.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_STUBS))
        .rateObservation(IborInterpolatedRateObservation.of(GBP_LIBOR_1W, GBP_LIBOR_1M, DATE_01_06))
        .build();
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder(ACCRUAL2)
        .yearFraction(ACCRUAL2.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_STUBS))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_02_03))
        .build();
    RateAccrualPeriod rap3 = RateAccrualPeriod.builder(ACCRUAL3STUB)
        .yearFraction(ACCRUAL3STUB.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_STUBS))
        .rateObservation(IborInterpolatedRateObservation.of(GBP_LIBOR_1M, GBP_LIBOR_3M, DATE_03_03))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.expand(ACCRUAL_SCHEDULE_STUBS, ACCRUAL_SCHEDULE_STUBS);
    assertEquals(periods, ImmutableList.of(rap1, rap2, rap3));
  }

  //-------------------------------------------------------------------------
  public void test_expand_firstRegularRateFixed() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_1M)
        .fixingOffset(MINUS_TWO_DAYS)
        .firstRegularRate(0.028d)
        .build();
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(ACCRUAL1)
        .yearFraction(ACCRUAL1.yearFraction(ACT_365F, ACCRUAL_SCHEDULE))
        .rateObservation(FixedRateObservation.of(0.028d))
        .build();
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder(ACCRUAL2)
        .yearFraction(ACCRUAL2.yearFraction(ACT_365F, ACCRUAL_SCHEDULE))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_02_03))
        .build();
    RateAccrualPeriod rap3 = RateAccrualPeriod.builder(ACCRUAL3)
        .yearFraction(ACCRUAL3.yearFraction(ACT_365F, ACCRUAL_SCHEDULE))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_03_03))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.expand(ACCRUAL_SCHEDULE, ACCRUAL_SCHEDULE);
    assertEquals(periods, ImmutableList.of(rap1, rap2, rap3));
  }

  public void test_expand_firstRegularRateFixedInitialStub() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_1M)
        .fixingOffset(MINUS_TWO_DAYS)
        .firstRegularRate(0.028d)
        .build();
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(ACCRUAL1STUB)
        .yearFraction(ACCRUAL1STUB.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_INITIAL_STUB))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_01_06))
        .build();
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder(ACCRUAL2)
        .yearFraction(ACCRUAL2.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_INITIAL_STUB))
        .rateObservation(FixedRateObservation.of(0.028d))
        .build();
    RateAccrualPeriod rap3 = RateAccrualPeriod.builder(ACCRUAL3)
        .yearFraction(ACCRUAL3.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_INITIAL_STUB))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_03_03))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.expand(ACCRUAL_SCHEDULE_INITIAL_STUB, ACCRUAL_SCHEDULE_INITIAL_STUB);
    assertEquals(periods, ImmutableList.of(rap1, rap2, rap3));
  }

  public void test_expand_firstRegularRateFixedTwoStubs() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_1M)
        .fixingOffset(MINUS_TWO_DAYS)
        .firstRegularRate(0.028d)
        .build();
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(ACCRUAL1STUB)
        .yearFraction(ACCRUAL1STUB.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_STUBS))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_01_06))
        .build();
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder(ACCRUAL2)
        .yearFraction(ACCRUAL2.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_STUBS))
        .rateObservation(FixedRateObservation.of(0.028d))
        .build();
    RateAccrualPeriod rap3 = RateAccrualPeriod.builder(ACCRUAL3STUB)
        .yearFraction(ACCRUAL3STUB.yearFraction(ACT_365F, ACCRUAL_SCHEDULE_STUBS))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_1M, DATE_03_03))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.expand(ACCRUAL_SCHEDULE_STUBS, ACCRUAL_SCHEDULE_STUBS);
    assertEquals(periods, ImmutableList.of(rap1, rap2, rap3));
  }

  //-------------------------------------------------------------------------
  public void test_expand_resetPeriods_weighted() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_3M)
        .fixingOffset(MINUS_TWO_DAYS)
        .resetPeriods(ResetSchedule.builder()
            .resetFrequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .averagingMethod(WEIGHTED)
            .build())
        .build();

    SchedulePeriod accrual1 = SchedulePeriod.of(DATE_01_06, DATE_04_07, DATE_01_05, DATE_04_05);
    SchedulePeriod accrual2 = SchedulePeriod.of(DATE_04_07, DATE_07_07, DATE_04_05, DATE_07_05);
    Schedule schedule = Schedule.builder()
        .periods(accrual1, accrual2)
        .frequency(P3M)
        .rollConvention(DAY_5)
        .build();

    ImmutableList<IborAveragedFixing> fixings1 = ImmutableList.of(
        IborAveragedFixing.ofDaysInResetPeriod(DATE_01_02, DATE_01_06, DATE_02_05),
        IborAveragedFixing.ofDaysInResetPeriod(DATE_02_03, DATE_02_05, DATE_03_05),
        IborAveragedFixing.ofDaysInResetPeriod(DATE_03_03, DATE_03_05, DATE_04_07));
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(accrual1)
        .yearFraction(accrual1.yearFraction(ACT_365F, schedule))
        .rateObservation(IborAveragedRateObservation.of(GBP_LIBOR_3M, fixings1))
        .build();
    ImmutableList<IborAveragedFixing> fixings2 = ImmutableList.of(
        IborAveragedFixing.ofDaysInResetPeriod(DATE_04_03, DATE_04_07, DATE_05_06),
        IborAveragedFixing.ofDaysInResetPeriod(DATE_05_01, DATE_05_06, DATE_06_05),
        IborAveragedFixing.ofDaysInResetPeriod(DATE_06_03, DATE_06_05, DATE_07_07));
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder(accrual2)
        .yearFraction(accrual2.yearFraction(ACT_365F, schedule))
        .rateObservation(IborAveragedRateObservation.of(GBP_LIBOR_3M, fixings2))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.expand(schedule, schedule);
    assertEquals(periods, ImmutableList.of(rap1, rap2));
  }

  public void test_expand_resetPeriods_weighted_firstFixed() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_3M)
        .fixingOffset(MINUS_TWO_DAYS)
        .resetPeriods(ResetSchedule.builder()
            .resetFrequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .averagingMethod(WEIGHTED)
            .build())
        .firstRegularRate(0.028d)
        .build();

    SchedulePeriod accrual1 = SchedulePeriod.of(DATE_01_06, DATE_04_07, DATE_01_05, DATE_04_05);
    SchedulePeriod accrual2 = SchedulePeriod.of(DATE_04_07, DATE_07_07, DATE_04_05, DATE_07_05);
    Schedule schedule = Schedule.builder()
        .periods(accrual1, accrual2)
        .frequency(P3M)
        .rollConvention(DAY_5)
        .build();

    ImmutableList<IborAveragedFixing> fixings1 = ImmutableList.of(
        IborAveragedFixing.ofDaysInResetPeriod(DATE_01_02, DATE_01_06, DATE_02_05, 0.028d),
        IborAveragedFixing.ofDaysInResetPeriod(DATE_02_03, DATE_02_05, DATE_03_05),
        IborAveragedFixing.ofDaysInResetPeriod(DATE_03_03, DATE_03_05, DATE_04_07));
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(accrual1)
        .yearFraction(accrual1.yearFraction(ACT_365F, schedule))
        .rateObservation(IborAveragedRateObservation.of(GBP_LIBOR_3M, fixings1))
        .build();
    ImmutableList<IborAveragedFixing> fixings2 = ImmutableList.of(
        IborAveragedFixing.ofDaysInResetPeriod(DATE_04_03, DATE_04_07, DATE_05_06),
        IborAveragedFixing.ofDaysInResetPeriod(DATE_05_01, DATE_05_06, DATE_06_05),
        IborAveragedFixing.ofDaysInResetPeriod(DATE_06_03, DATE_06_05, DATE_07_07));
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder(accrual2)
        .yearFraction(accrual2.yearFraction(ACT_365F, schedule))
        .rateObservation(IborAveragedRateObservation.of(GBP_LIBOR_3M, fixings2))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.expand(schedule, schedule);
    assertEquals(periods, ImmutableList.of(rap1, rap2));
  }

  public void test_expand_resetPeriods_unweighted() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_3M)
        .fixingOffset(MINUS_TWO_DAYS)
        .resetPeriods(ResetSchedule.builder()
            .resetFrequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .averagingMethod(UNWEIGHTED)
            .build())
        .build();

    SchedulePeriod accrual1 = SchedulePeriod.of(DATE_01_06, DATE_04_07, DATE_01_05, DATE_04_05);
    SchedulePeriod accrual2 = SchedulePeriod.of(DATE_04_07, DATE_07_07, DATE_04_05, DATE_07_05);
    Schedule schedule = Schedule.builder()
        .periods(accrual1, accrual2)
        .frequency(P3M)
        .rollConvention(DAY_5)
        .build();

    ImmutableList<IborAveragedFixing> fixings1 = ImmutableList.of(
        IborAveragedFixing.of(DATE_01_02),
        IborAveragedFixing.of(DATE_02_03),
        IborAveragedFixing.of(DATE_03_03));
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(accrual1)
        .yearFraction(accrual1.yearFraction(ACT_365F, schedule))
        .rateObservation(IborAveragedRateObservation.of(GBP_LIBOR_3M, fixings1))
        .build();
    ImmutableList<IborAveragedFixing> fixings2 = ImmutableList.of(
        IborAveragedFixing.of(DATE_04_03),
        IborAveragedFixing.of(DATE_05_01),
        IborAveragedFixing.of(DATE_06_03));
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder(accrual2)
        .yearFraction(accrual2.yearFraction(ACT_365F, schedule))
        .rateObservation(IborAveragedRateObservation.of(GBP_LIBOR_3M, fixings2))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.expand(schedule, schedule);
    assertEquals(periods, ImmutableList.of(rap1, rap2));
  }

  public void test_expand_initialStubAndResetPeriods_weighted_firstFixed() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_360)
        .index(GBP_LIBOR_3M)
        .fixingOffset(MINUS_TWO_DAYS)
        .resetPeriods(ResetSchedule.builder()
            .resetFrequency(P1M)
            .businessDayAdjustment(BusinessDayAdjustment.of(FOLLOWING, GBLO))
            .averagingMethod(WEIGHTED)
            .build())
        .firstRegularRate(0.028d)
        .initialStub(StubCalculation.ofFixedRate(0.030d))
        .build();

    SchedulePeriod accrual1 = SchedulePeriod.of(DATE_02_05, DATE_04_07, DATE_02_05, DATE_04_05);
    SchedulePeriod accrual2 = SchedulePeriod.of(DATE_04_07, DATE_07_07, DATE_04_05, DATE_07_05);
    Schedule schedule = Schedule.builder()
        .periods(accrual1, accrual2)
        .frequency(P3M)
        .rollConvention(DAY_5)
        .build();

    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(accrual1)
        .yearFraction(accrual1.yearFraction(ACT_360, schedule))
        .rateObservation(FixedRateObservation.of(0.030d))
        .build();
    ImmutableList<IborAveragedFixing> fixings2 = ImmutableList.of(
        IborAveragedFixing.ofDaysInResetPeriod(DATE_04_03, DATE_04_07, DATE_05_06, 0.028d),
        IborAveragedFixing.ofDaysInResetPeriod(DATE_05_01, DATE_05_06, DATE_06_05),
        IborAveragedFixing.ofDaysInResetPeriod(DATE_06_03, DATE_06_05, DATE_07_07));
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder(accrual2)
        .yearFraction(accrual2.yearFraction(ACT_360, schedule))
        .rateObservation(IborAveragedRateObservation.of(GBP_LIBOR_3M, fixings2))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.expand(schedule, schedule);
    assertEquals(periods, ImmutableList.of(rap1, rap2));
  }

  //-------------------------------------------------------------------------
  public void test_expand_gearingSpreadEverythingElse() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_360)
        .index(GBP_LIBOR_3M)
        .fixingOffset(MINUS_THREE_DAYS)
        .fixingRelativeTo(PERIOD_END)
        .negativeRateMethod(NOT_NEGATIVE)
        .gearing(ValueSchedule.of(1d, ValueStep.of(2, ValueAdjustment.ofAbsoluteAmount(2d))))
        .spread(ValueSchedule.of(0d, ValueStep.of(1, ValueAdjustment.ofAbsoluteAmount(-0.025d))))
        .build();
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(ACCRUAL1)
        .yearFraction(ACCRUAL1.yearFraction(ACT_360, ACCRUAL_SCHEDULE))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_3M, DATE_01_31))
        .negativeRateMethod(NOT_NEGATIVE)
        .build();
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder(ACCRUAL2)
        .yearFraction(ACCRUAL2.yearFraction(ACT_360, ACCRUAL_SCHEDULE))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_3M, DATE_02_28))
        .negativeRateMethod(NOT_NEGATIVE)
        .spread(-0.025d)
        .build();
    RateAccrualPeriod rap3 = RateAccrualPeriod.builder(ACCRUAL3)
        .yearFraction(ACCRUAL3.yearFraction(ACT_360, ACCRUAL_SCHEDULE))
        .rateObservation(IborRateObservation.of(GBP_LIBOR_3M, DATE_04_02))
        .negativeRateMethod(NOT_NEGATIVE)
        .gearing(2d)
        .spread(-0.025d)
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.expand(ACCRUAL_SCHEDULE, ACCRUAL_SCHEDULE);
    assertEquals(periods, ImmutableList.of(rap1, rap2, rap3));
  }

  public void test_expand_null() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_3M)
        .fixingOffset(MINUS_TWO_DAYS)
        .build();
    Schedule schedule = Schedule.ofTerm(SchedulePeriod.of(DATE_01_05, DATE_02_05));
    assertThrowsIllegalArg(() -> test.expand(schedule, null));
    assertThrowsIllegalArg(() -> test.expand(null, schedule));
    assertThrowsIllegalArg(() -> test.expand(null, null));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_3M)
        .fixingOffset(MINUS_TWO_DAYS)
        .build();
    coverImmutableBean(test);
    IborRateCalculation test2 = IborRateCalculation.builder()
        .dayCount(ACT_360)
        .index(GBP_LIBOR_6M)
        .resetPeriods(ResetSchedule.builder()
            .resetFrequency(P3M)
            .averagingMethod(IborRateAveragingMethod.UNWEIGHTED)
            .businessDayAdjustment(BusinessDayAdjustment.NONE)
            .build())
        .fixingOffset(MINUS_THREE_DAYS)
        .fixingRelativeTo(PERIOD_END)
        .negativeRateMethod(NOT_NEGATIVE)
        .firstRegularRate(0.028d)
        .initialStub(StubCalculation.NONE)
        .finalStub(StubCalculation.NONE)
        .gearing(ValueSchedule.of(2d))
        .spread(ValueSchedule.of(-0.025d))
        .build();
    coverBeanEquals(test, test2);
  }

  public void test_serialization() {
    IborRateCalculation test = IborRateCalculation.builder()
        .dayCount(ACT_365F)
        .index(GBP_LIBOR_3M)
        .fixingOffset(MINUS_TWO_DAYS)
        .build();
    assertSerialization(test);
  }

}
