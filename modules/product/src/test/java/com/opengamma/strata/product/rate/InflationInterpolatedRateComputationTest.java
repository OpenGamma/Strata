/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.product.rate;

import static com.opengamma.strata.basics.index.PriceIndices.CH_CPI;
import static com.opengamma.strata.basics.index.PriceIndices.GB_HICP;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.assertThrowsIllegalArg;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static org.testng.Assert.assertEquals;

import java.time.YearMonth;

import org.testng.annotations.Test;

import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.Index;
import com.opengamma.strata.basics.index.PriceIndexObservation;

/**
 * Test {@link InflationInterpolatedRateComputation}.
 */
@Test
public class InflationInterpolatedRateComputationTest {

  private static final YearMonth START_MONTH_FIRST = YearMonth.of(2014, 1);
  private static final YearMonth START_MONTH_SECOND = YearMonth.of(2014, 2);
  private static final YearMonth END_MONTH_FIRST = YearMonth.of(2015, 1);
  private static final YearMonth END_MONTH_SECOND = YearMonth.of(2015, 2);
  private static final double WEIGHT = 1.0 - 6.0 / 31.0;

  //-------------------------------------------------------------------------
  public void test_of() {
    InflationInterpolatedRateComputation test = InflationInterpolatedRateComputation.of(
        GB_HICP, START_MONTH_FIRST, END_MONTH_FIRST, WEIGHT);
    assertEquals(test.getIndex(), GB_HICP);
    assertEquals(test.getStartObservation().getFixingMonth(), START_MONTH_FIRST);
    assertEquals(test.getStartSecondObservation().getFixingMonth(), START_MONTH_SECOND);
    assertEquals(test.getEndObservation().getFixingMonth(), END_MONTH_FIRST);
    assertEquals(test.getEndSecondObservation().getFixingMonth(), END_MONTH_SECOND);
    assertEquals(test.getWeight(), WEIGHT, 1.0e-14);
  }

  public void test_wrongMonthOrder() {
    assertThrowsIllegalArg(() -> InflationInterpolatedRateComputation.of(
        GB_HICP, END_MONTH_FIRST, START_MONTH_FIRST, WEIGHT));
    assertThrowsIllegalArg(() -> InflationInterpolatedRateComputation.meta().builder()
        .set(InflationInterpolatedRateComputation.meta().startObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 1)))
        .set(InflationInterpolatedRateComputation.meta().startSecondObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 1)))
        .set(InflationInterpolatedRateComputation.meta().endObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 7)))
        .set(InflationInterpolatedRateComputation.meta().endSecondObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 8)))
        .set(InflationInterpolatedRateComputation.meta().weight(), WEIGHT)
        .build());
    assertThrowsIllegalArg(() -> InflationInterpolatedRateComputation.meta().builder()
        .set(InflationInterpolatedRateComputation.meta().startObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 1)))
        .set(InflationInterpolatedRateComputation.meta().startSecondObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 2)))
        .set(InflationInterpolatedRateComputation.meta().endObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 7)))
        .set(InflationInterpolatedRateComputation.meta().endSecondObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 7)))
        .set(InflationInterpolatedRateComputation.meta().weight(), WEIGHT)
        .build());
    assertThrowsIllegalArg(() -> InflationInterpolatedRateComputation.meta().builder()
        .set(InflationInterpolatedRateComputation.meta().startObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 8)))
        .set(InflationInterpolatedRateComputation.meta().startSecondObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 9)))
        .set(InflationInterpolatedRateComputation.meta().endObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 7)))
        .set(InflationInterpolatedRateComputation.meta().endSecondObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 8)))
        .set(InflationInterpolatedRateComputation.meta().weight(), WEIGHT)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    InflationInterpolatedRateComputation test = InflationInterpolatedRateComputation.of(
        GB_HICP, START_MONTH_FIRST, END_MONTH_FIRST, WEIGHT);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GB_HICP));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    InflationInterpolatedRateComputation test1 = InflationInterpolatedRateComputation.of(
        GB_HICP, START_MONTH_FIRST, END_MONTH_FIRST, WEIGHT);
    coverImmutableBean(test1);
    InflationInterpolatedRateComputation test2 = InflationInterpolatedRateComputation.of(
        CH_CPI, YearMonth.of(2010, 1), YearMonth.of(2010, 7), WEIGHT + 0.1d);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    InflationInterpolatedRateComputation test = InflationInterpolatedRateComputation.of(
        GB_HICP, START_MONTH_FIRST, END_MONTH_FIRST, WEIGHT);
    assertSerialization(test);
  }

}
