/*
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.swap;

import static com.opengamma.strata.basics.index.PriceIndices.CH_CPI;
import static com.opengamma.strata.basics.index.PriceIndices.GB_HICP;
import static com.opengamma.strata.basics.index.PriceIndices.JP_CPI_EXF;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrows;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static com.opengamma.strata.product.swap.PriceIndexCalculationMethod.INTERPOLATED;
import static com.opengamma.strata.product.swap.PriceIndexCalculationMethod.INTERPOLATED_JAPAN;
import static com.opengamma.strata.product.swap.PriceIndexCalculationMethod.MONTHLY;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.Optional;
import java.util.OptionalDouble;

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
import com.opengamma.strata.product.rate.InflationEndInterpolatedRateComputation;
import com.opengamma.strata.product.rate.InflationEndMonthRateComputation;
import com.opengamma.strata.product.rate.InflationInterpolatedRateComputation;
import com.opengamma.strata.product.rate.InflationMonthlyRateComputation;

/**
 * Test {@link InflationRateCalculation}. 
 */
@Test
public class InflationRateCalculationTest {

  private static final ReferenceData REF_DATA = ReferenceData.standard();
  private static final LocalDate DATE_2015_01_05 = date(2015, 1, 5);
  private static final LocalDate DATE_2015_02_05 = date(2015, 2, 5);
  private static final LocalDate DATE_2015_03_05 = date(2015, 3, 5);
  private static final LocalDate DATE_2015_03_07 = date(2015, 3, 7);
  private static final LocalDate DATE_2015_04_05 = date(2015, 4, 5);

  private static final SchedulePeriod ACCRUAL1 =
      SchedulePeriod.of(DATE_2015_01_05, DATE_2015_02_05, DATE_2015_01_05, DATE_2015_02_05);
  private static final SchedulePeriod ACCRUAL2 =
      SchedulePeriod.of(DATE_2015_02_05, DATE_2015_03_07, DATE_2015_02_05, DATE_2015_03_05);
  private static final SchedulePeriod ACCRUAL3 =
      SchedulePeriod.of(DATE_2015_03_07, DATE_2015_04_05, DATE_2015_03_05, DATE_2015_04_05);
  private static final Schedule ACCRUAL_SCHEDULE = Schedule.builder()
      .periods(ACCRUAL1, ACCRUAL2, ACCRUAL3)
      .frequency(Frequency.P1M)
      .rollConvention(RollConventions.DAY_5)
      .build();
  private static final double START_INDEX = 325d;
  private static final ValueSchedule GEARING =
      ValueSchedule.of(1d, ValueStep.of(2, ValueAdjustment.ofReplace(2d)));

  //-------------------------------------------------------------------------
  public void test_of() {
    InflationRateCalculation test1 = InflationRateCalculation.of(CH_CPI, 3, MONTHLY);
    assertEquals(test1.getIndex(), CH_CPI);
    assertEquals(test1.getLag(), Period.ofMonths(3));
    assertEquals(test1.getIndexCalculationMethod(), MONTHLY);
    assertEquals(test1.getFirstIndexValue(), OptionalDouble.empty());
    assertEquals(test1.getGearing(), Optional.empty());
    assertEquals(test1.getType(), SwapLegType.INFLATION);
  }

  public void test_of_firstIndexValue() {
    InflationRateCalculation test1 = InflationRateCalculation.of(CH_CPI, 3, MONTHLY, 123d);
    assertEquals(test1.getIndex(), CH_CPI);
    assertEquals(test1.getLag(), Period.ofMonths(3));
    assertEquals(test1.getIndexCalculationMethod(), MONTHLY);
    assertEquals(test1.getFirstIndexValue(), OptionalDouble.of(123d));
    assertEquals(test1.getGearing(), Optional.empty());
    assertEquals(test1.getType(), SwapLegType.INFLATION);
  }

  //-------------------------------------------------------------------------
  public void test_builder() {
    InflationRateCalculation test1 = InflationRateCalculation.builder()
        .index(CH_CPI)
        .lag(Period.ofMonths(3))
        .indexCalculationMethod(MONTHLY)
        .firstIndexValue(123d)
        .build();
    assertEquals(test1.getIndex(), CH_CPI);
    assertEquals(test1.getLag(), Period.ofMonths(3));
    assertEquals(test1.getIndexCalculationMethod(), MONTHLY);
    assertEquals(test1.getGearing(), Optional.empty());
    assertEquals(test1.getFirstIndexValue(), OptionalDouble.of(123d));
    assertEquals(test1.getType(), SwapLegType.INFLATION);
    InflationRateCalculation test2 = InflationRateCalculation.builder()
        .index(GB_HICP)
        .lag(Period.ofMonths(4))
        .indexCalculationMethod(INTERPOLATED)
        .gearing(GEARING)
        .build();
    assertEquals(test2.getIndex(), GB_HICP);
    assertEquals(test2.getLag(), Period.ofMonths(4));
    assertEquals(test2.getIndexCalculationMethod(), INTERPOLATED);
    assertEquals(test2.getFirstIndexValue(), OptionalDouble.empty());
    assertEquals(test2.getGearing().get(), GEARING);
    assertEquals(test2.getType(), SwapLegType.INFLATION);
  }

  public void test_builder_missing_index() {
    assertThrowsIllegalArg(() -> InflationRateCalculation.builder().build());
  }

  public void test_builder_badLag() {
    assertThrowsIllegalArg(() -> InflationRateCalculation.builder()
        .index(GB_HICP)
        .lag(Period.ZERO)
        .build());
    assertThrowsIllegalArg(() -> InflationRateCalculation.builder()
        .index(GB_HICP)
        .lag(Period.ofMonths(-1))
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    InflationRateCalculation test = InflationRateCalculation.builder()
        .index(GB_HICP)
        .lag(Period.ofMonths(3))
        .indexCalculationMethod(MONTHLY)
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GB_HICP));
  }

  //-------------------------------------------------------------------------
  public void test_createAccrualPeriods_Monthly() {
    InflationRateCalculation test = InflationRateCalculation.builder()
        .index(GB_HICP)
        .lag(Period.ofMonths(3))
        .indexCalculationMethod(MONTHLY)
        .build();
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(ACCRUAL1)
        .yearFraction(1.0)
        .rateComputation(
            InflationMonthlyRateComputation.of(
                GB_HICP,
                YearMonth.from(DATE_2015_01_05).minusMonths(3),
                YearMonth.from(DATE_2015_02_05).minusMonths(3)))
        .build();
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder(ACCRUAL2)
        .yearFraction(1.0)
        .rateComputation(
            InflationMonthlyRateComputation.of(
                GB_HICP,
                YearMonth.from(DATE_2015_02_05).minusMonths(3),
                YearMonth.from(DATE_2015_03_07).minusMonths(3)))
        .build();
    RateAccrualPeriod rap3 = RateAccrualPeriod.builder(ACCRUAL3)
        .yearFraction(1.0)
        .rateComputation(
            InflationMonthlyRateComputation.of(
                GB_HICP,
                YearMonth.from(DATE_2015_03_07).minusMonths(3),
                YearMonth.from(DATE_2015_04_05).minusMonths(3)))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.createAccrualPeriods(ACCRUAL_SCHEDULE, ACCRUAL_SCHEDULE, REF_DATA);
    assertEquals(periods, ImmutableList.of(rap1, rap2, rap3));
  }

  public void test_createAccrualPeriods_Interpolated() {
    InflationRateCalculation test = InflationRateCalculation.builder()
        .index(CH_CPI)
        .lag(Period.ofMonths(3))
        .indexCalculationMethod(INTERPOLATED)
        .build();
    double weight1 = 1.0 - 4.0 / 28.0;
    double weight2 = 1.0 - 6.0 / 31.0;
    double weight3 = 1.0 - 4.0 / 30.0;
    RateAccrualPeriod rap1 = RateAccrualPeriod
        .builder(ACCRUAL1)
        .yearFraction(1.0)
        .rateComputation(InflationInterpolatedRateComputation.of(
            CH_CPI,
            YearMonth.from(DATE_2015_01_05).minusMonths(3),
            YearMonth.from(DATE_2015_02_05).minusMonths(3),
            weight1))
        .build();
    RateAccrualPeriod rap2 = RateAccrualPeriod
        .builder(ACCRUAL2)
        .yearFraction(1.0)
        .rateComputation(InflationInterpolatedRateComputation.of(
            CH_CPI,
            YearMonth.from(DATE_2015_02_05).minusMonths(3),
            YearMonth.from(DATE_2015_03_07).minusMonths(3),
            weight2))
        .build();
    RateAccrualPeriod rap3 = RateAccrualPeriod
        .builder(ACCRUAL3)
        .yearFraction(1.0)
        .rateComputation(InflationInterpolatedRateComputation.of(
            CH_CPI,
            YearMonth.from(DATE_2015_03_07).minusMonths(3),
            YearMonth.from(DATE_2015_04_05).minusMonths(3),
            weight3))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.createAccrualPeriods(ACCRUAL_SCHEDULE, ACCRUAL_SCHEDULE, REF_DATA);
    assertEquals(periods, ImmutableList.of(rap1, rap2, rap3));
  }

  public void test_createRateComputation_InterpolatedJapan() {
    LocalDate date1 = LocalDate.of(2013, 3, 9);
    LocalDate date2 = LocalDate.of(2013, 3, 10);
    LocalDate date3 = LocalDate.of(2013, 3, 11);
    InflationRateCalculation test = InflationRateCalculation.builder()
        .index(JP_CPI_EXF)
        .lag(Period.ofMonths(3))
        .indexCalculationMethod(INTERPOLATED_JAPAN)
        .firstIndexValue(START_INDEX)
        .build();
    double weight1 = 1.0 - (9.0 + 28.0 - 10.0) / 28.0;
    double weight2 = 1.0;
    double weight3 = 1.0 - 1.0 / 31.0;
    InflationEndInterpolatedRateComputation obs1 = InflationEndInterpolatedRateComputation.of(
        JP_CPI_EXF, START_INDEX, YearMonth.from(date1).minusMonths(4), weight1);
    InflationEndInterpolatedRateComputation obs2 = InflationEndInterpolatedRateComputation.of(
        JP_CPI_EXF, START_INDEX, YearMonth.from(date2).minusMonths(3), weight2);
    InflationEndInterpolatedRateComputation obs3 = InflationEndInterpolatedRateComputation.of(
        JP_CPI_EXF, START_INDEX, YearMonth.from(date3).minusMonths(3), weight3);
    assertEquals(test.createRateComputation(date1), obs1);
    assertEquals(test.createRateComputation(date2), obs2);
    assertEquals(test.createRateComputation(date3), obs3);
  }

  public void test_createAccrualPeriods_Monthly_firstKnown() {
    InflationRateCalculation test = InflationRateCalculation.builder()
        .index(GB_HICP)
        .lag(Period.ofMonths(3))
        .indexCalculationMethod(MONTHLY)
        .firstIndexValue(123d)
        .build();
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(ACCRUAL1)
        .yearFraction(1.0)
        .rateComputation(
            InflationEndMonthRateComputation.of(
                GB_HICP,
                123d,
                YearMonth.from(DATE_2015_02_05).minusMonths(3)))
        .build();
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder(ACCRUAL2)
        .yearFraction(1.0)
        .rateComputation(
            InflationMonthlyRateComputation.of(
                GB_HICP,
                YearMonth.from(DATE_2015_02_05).minusMonths(3),
                YearMonth.from(DATE_2015_03_07).minusMonths(3)))
        .build();
    RateAccrualPeriod rap3 = RateAccrualPeriod.builder(ACCRUAL3)
        .yearFraction(1.0)
        .rateComputation(
            InflationMonthlyRateComputation.of(
                GB_HICP,
                YearMonth.from(DATE_2015_03_07).minusMonths(3),
                YearMonth.from(DATE_2015_04_05).minusMonths(3)))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.createAccrualPeriods(ACCRUAL_SCHEDULE, ACCRUAL_SCHEDULE, REF_DATA);
    assertEquals(periods, ImmutableList.of(rap1, rap2, rap3));
  }

  //-------------------------------------------------------------------------
  public void test_createRateComputation_Monthly() {
    InflationRateCalculation test = InflationRateCalculation.builder()
        .index(GB_HICP)
        .lag(Period.ofMonths(3))
        .indexCalculationMethod(MONTHLY)
        .firstIndexValue(START_INDEX)
        .build();
    InflationEndMonthRateComputation obs1 = InflationEndMonthRateComputation.of(
        GB_HICP, START_INDEX, YearMonth.from(DATE_2015_02_05).minusMonths(3));
    InflationEndMonthRateComputation obs2 = InflationEndMonthRateComputation.of(
        GB_HICP, START_INDEX, YearMonth.from(DATE_2015_03_07).minusMonths(3));
    InflationEndMonthRateComputation obs3 = InflationEndMonthRateComputation.of(
        GB_HICP, START_INDEX, YearMonth.from(DATE_2015_04_05).minusMonths(3));
    assertEquals(test.createRateComputation(DATE_2015_02_05), obs1);
    assertEquals(test.createRateComputation(DATE_2015_03_07), obs2);
    assertEquals(test.createRateComputation(DATE_2015_04_05), obs3);
  }

  public void test_createRateComputation_Interpolated() {
    InflationRateCalculation test = InflationRateCalculation.builder()
        .index(CH_CPI)
        .lag(Period.ofMonths(3))
        .indexCalculationMethod(INTERPOLATED)
        .firstIndexValue(START_INDEX)
        .build();
    double weight1 = 1.0 - 4.0 / 28.0;
    double weight2 = 1.0 - 6.0 / 31.0;
    double weight3 = 1.0 - 4.0 / 30.0;
    InflationEndInterpolatedRateComputation obs1 = InflationEndInterpolatedRateComputation.of(
        CH_CPI, START_INDEX, YearMonth.from(DATE_2015_02_05).minusMonths(3), weight1);
    InflationEndInterpolatedRateComputation obs2 = InflationEndInterpolatedRateComputation.of(
        CH_CPI, START_INDEX, YearMonth.from(DATE_2015_03_07).minusMonths(3), weight2);
    InflationEndInterpolatedRateComputation obs3 = InflationEndInterpolatedRateComputation.of(
        CH_CPI, START_INDEX, YearMonth.from(DATE_2015_04_05).minusMonths(3), weight3);
    assertEquals(test.createRateComputation(DATE_2015_02_05), obs1);
    assertEquals(test.createRateComputation(DATE_2015_03_07), obs2);
    assertEquals(test.createRateComputation(DATE_2015_04_05), obs3);
  }

  public void test_createRateComputation_noFirstIndexValue() {
    InflationRateCalculation test = InflationRateCalculation.builder()
        .index(CH_CPI)
        .lag(Period.ofMonths(3))
        .indexCalculationMethod(INTERPOLATED)
        .build();
    assertThrows(() -> test.createRateComputation(DATE_2015_04_05), IllegalStateException.class);
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    InflationRateCalculation test1 = InflationRateCalculation.builder()
        .index(CH_CPI)
        .lag(Period.ofMonths(3))
        .indexCalculationMethod(MONTHLY)
        .build();
    coverImmutableBean(test1);
    InflationRateCalculation test2 = InflationRateCalculation.builder()
        .index(GB_HICP)
        .lag(Period.ofMonths(4))
        .indexCalculationMethod(INTERPOLATED)
        .gearing(GEARING)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    InflationRateCalculation test1 = InflationRateCalculation.builder()
        .index(CH_CPI)
        .lag(Period.ofMonths(3))
        .indexCalculationMethod(MONTHLY)
        .build();
    assertSerialization(test1);
  }

}
