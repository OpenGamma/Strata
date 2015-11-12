/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate.swap;

import static com.opengamma.strata.basics.index.PriceIndices.CH_CPI;
import static com.opengamma.strata.basics.index.PriceIndices.GB_HICP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.testng.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.Optional;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.basics.schedule.RollConventions;
import com.opengamma.strata.basics.schedule.Schedule;
import com.opengamma.strata.basics.schedule.SchedulePeriod;
import com.opengamma.strata.basics.value.ValueAdjustment;
import com.opengamma.strata.basics.value.ValueSchedule;
import com.opengamma.strata.basics.value.ValueStep;
import com.opengamma.strata.product.rate.InflationInterpolatedRateObservation;
import com.opengamma.strata.product.rate.InflationMonthlyRateObservation;

/**
 * Test {@link InflationRateCalculation}. 
 */
@Test
public class InflationRateCalculationTest {

  private static final LocalDate DATE_2014_01_05 = date(2014, 1, 5);
  private static final LocalDate DATE_2015_01_06 = date(2015, 1, 6);
  private static final LocalDate DATE_2015_01_05 = date(2015, 1, 5);
  private static final LocalDate DATE_2016_01_05 = date(2016, 1, 5);
  private static final LocalDate DATE_2016_01_07 = date(2016, 1, 7);
  private static final LocalDate DATE_2017_01_05 = date(2017, 1, 5);

  private static final SchedulePeriod ACCRUAL1 =
      SchedulePeriod.of(DATE_2014_01_05, DATE_2015_01_06, DATE_2014_01_05, DATE_2015_01_05);
  private static final SchedulePeriod ACCRUAL2 =
      SchedulePeriod.of(DATE_2015_01_06, DATE_2016_01_07, DATE_2015_01_05, DATE_2016_01_05);
  private static final SchedulePeriod ACCRUAL3 =
      SchedulePeriod.of(DATE_2016_01_07, DATE_2017_01_05, DATE_2016_01_05, DATE_2017_01_05);
  private static final Schedule ACCRUAL_SCHEDULE = Schedule.builder()
      .periods(ACCRUAL1, ACCRUAL2, ACCRUAL3)
      .frequency(Frequency.P1M)
      .rollConvention(RollConventions.DAY_5)
      .build();

  private static final ValueSchedule GEARING =
      ValueSchedule.of(1d, ValueStep.of(2, ValueAdjustment.ofReplace(2d)));

  //-------------------------------------------------------------------------
  public void test_of() {
    InflationRateCalculation test1 = InflationRateCalculation.of(CH_CPI, 3, false);
    assertEquals(test1.getIndex(), CH_CPI);
    assertEquals(test1.getLag(), Period.ofMonths(3));
    assertEquals(test1.isInterpolated(), false);
    assertEquals(test1.getGearing(), Optional.empty());
    assertEquals(test1.getType(), SwapLegType.INFLATION);
  }

  //-------------------------------------------------------------------------
  public void test_builder() {
    InflationRateCalculation test1 = InflationRateCalculation.builder()
        .index(CH_CPI)
        .lag(Period.ofMonths(3))
        .interpolated(false)
        .build();
    assertEquals(test1.getIndex(), CH_CPI);
    assertEquals(test1.getLag(), Period.ofMonths(3));
    assertEquals(test1.isInterpolated(), false);
    assertEquals(test1.getGearing(), Optional.empty());
    assertEquals(test1.getType(), SwapLegType.INFLATION);
    InflationRateCalculation test2 = InflationRateCalculation.builder()
        .index(GB_HICP)
        .lag(Period.ofMonths(4))
        .interpolated(true)
        .gearing(GEARING)
        .build();
    assertEquals(test2.getIndex(), GB_HICP);
    assertEquals(test2.getLag(), Period.ofMonths(4));
    assertEquals(test2.isInterpolated(), true);
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
        .interpolated(false)
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GB_HICP));
  }

  //-------------------------------------------------------------------------
  public void test_expand_Monthly() {
    InflationRateCalculation test = InflationRateCalculation.builder()
        .index(GB_HICP)
        .lag(Period.ofMonths(3))
        .interpolated(false)
        .build();
    RateAccrualPeriod rap1 = RateAccrualPeriod.builder(ACCRUAL1)
        .yearFraction(1.0)
        .rateObservation(
            InflationMonthlyRateObservation.of(
                GB_HICP,
                YearMonth.from(DATE_2014_01_05).minusMonths(3),
                YearMonth.from(DATE_2015_01_06).minusMonths(3)))
        .build();
    RateAccrualPeriod rap2 = RateAccrualPeriod.builder(ACCRUAL2)
        .yearFraction(1.0)
        .rateObservation(
            InflationMonthlyRateObservation.of(
                GB_HICP,
                YearMonth.from(DATE_2015_01_06).minusMonths(3),
                YearMonth.from(DATE_2016_01_07).minusMonths(3)))
        .build();
    RateAccrualPeriod rap3 = RateAccrualPeriod.builder(ACCRUAL3)
        .yearFraction(1.0)
        .rateObservation(
            InflationMonthlyRateObservation.of(
                GB_HICP,
                YearMonth.from(DATE_2016_01_07).minusMonths(3),
                YearMonth.from(DATE_2017_01_05).minusMonths(3)))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.expand(ACCRUAL_SCHEDULE, ACCRUAL_SCHEDULE);
    assertEquals(periods, ImmutableList.of(rap1, rap2, rap3));
  }

  @Test
  public void test_expand_Interpolated() {
    InflationRateCalculation test = InflationRateCalculation.builder()
        .index(CH_CPI)
        .lag(Period.ofMonths(3))
        .interpolated(true)
        .build();
    double weight1 = 1.0 - 5.0 / 31.0;
    double weight2 = 1.0 - 6.0 / 31.0;
    double weight3 = 1.0 - 4.0 / 31.0;
    RateAccrualPeriod rap1 = RateAccrualPeriod
        .builder(ACCRUAL1)
        .yearFraction(1.0)
        .rateObservation(InflationInterpolatedRateObservation.of(
            CH_CPI,
            YearMonth.from(DATE_2014_01_05).minusMonths(3),
            YearMonth.from(DATE_2015_01_06).minusMonths(3),
            weight1))
        .build();
    RateAccrualPeriod rap2 = RateAccrualPeriod
        .builder(ACCRUAL2)
        .yearFraction(1.0)
        .rateObservation(InflationInterpolatedRateObservation.of(
            CH_CPI,
            YearMonth.from(DATE_2015_01_06).minusMonths(3),
            YearMonth.from(DATE_2016_01_07).minusMonths(3),
            weight2))
        .build();
    RateAccrualPeriod rap3 = RateAccrualPeriod
        .builder(ACCRUAL3)
        .yearFraction(1.0)
        .rateObservation(InflationInterpolatedRateObservation.of(
            CH_CPI,
            YearMonth.from(DATE_2016_01_07).minusMonths(3),
            YearMonth.from(DATE_2017_01_05).minusMonths(3),
            weight3))
        .build();
    ImmutableList<RateAccrualPeriod> periods = test.expand(ACCRUAL_SCHEDULE, ACCRUAL_SCHEDULE);
    assertEquals(periods, ImmutableList.of(rap1, rap2, rap3));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    InflationRateCalculation test1 = InflationRateCalculation.builder()
        .index(CH_CPI)
        .lag(Period.ofMonths(3))
        .interpolated(false)
        .build();
    coverImmutableBean(test1);
    InflationRateCalculation test2 = InflationRateCalculation.builder()
        .index(GB_HICP)
        .lag(Period.ofMonths(4))
        .interpolated(true)
        .gearing(GEARING)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    InflationRateCalculation test1 = InflationRateCalculation.builder()
        .index(CH_CPI)
        .lag(Period.ofMonths(3))
        .interpolated(false)
        .build();
    assertSerialization(test1);
  }

}
