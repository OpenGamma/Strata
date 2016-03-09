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
 * Test {@link InflationInterpolatedRateObservation}.
 */
@Test
public class InflationInterpolatedRateObservationTest {

  private static final YearMonth START_MONTH_FIRST = YearMonth.of(2014, 1);
  private static final YearMonth START_MONTH_SECOND = YearMonth.of(2014, 2);
  private static final YearMonth END_MONTH_FIRST = YearMonth.of(2015, 1);
  private static final YearMonth END_MONTH_SECOND = YearMonth.of(2015, 2);
  private static final double WEIGHT = 1.0 - 6.0 / 31.0;

  //-------------------------------------------------------------------------
  public void test_of() {
    InflationInterpolatedRateObservation test = InflationInterpolatedRateObservation.of(
        GB_HICP, START_MONTH_FIRST, END_MONTH_FIRST, WEIGHT);
    assertEquals(test.getIndex(), GB_HICP);
    assertEquals(test.getStartObservation().getFixingMonth(), START_MONTH_FIRST);
    assertEquals(test.getStartSecondObservation().getFixingMonth(), START_MONTH_SECOND);
    assertEquals(test.getEndObservation().getFixingMonth(), END_MONTH_FIRST);
    assertEquals(test.getEndSecondObservation().getFixingMonth(), END_MONTH_SECOND);
    assertEquals(test.getWeight(), WEIGHT, 1.0e-14);
  }

  public void test_wrongMonthOrder() {
    assertThrowsIllegalArg(() -> InflationInterpolatedRateObservation.of(
        GB_HICP, END_MONTH_FIRST, START_MONTH_FIRST, WEIGHT));
    assertThrowsIllegalArg(() -> InflationInterpolatedRateObservation.meta().builder()
        .set(InflationInterpolatedRateObservation.meta().startObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 1)))
        .set(InflationInterpolatedRateObservation.meta().startSecondObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 1)))
        .set(InflationInterpolatedRateObservation.meta().endObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 7)))
        .set(InflationInterpolatedRateObservation.meta().endSecondObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 8)))
        .set(InflationInterpolatedRateObservation.meta().weight(), WEIGHT)
        .build());
    assertThrowsIllegalArg(() -> InflationInterpolatedRateObservation.meta().builder()
        .set(InflationInterpolatedRateObservation.meta().startObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 1)))
        .set(InflationInterpolatedRateObservation.meta().startSecondObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 2)))
        .set(InflationInterpolatedRateObservation.meta().endObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 7)))
        .set(InflationInterpolatedRateObservation.meta().endSecondObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 7)))
        .set(InflationInterpolatedRateObservation.meta().weight(), WEIGHT)
        .build());
    assertThrowsIllegalArg(() -> InflationInterpolatedRateObservation.meta().builder()
        .set(InflationInterpolatedRateObservation.meta().startObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 8)))
        .set(InflationInterpolatedRateObservation.meta().startSecondObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 9)))
        .set(InflationInterpolatedRateObservation.meta().endObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 7)))
        .set(InflationInterpolatedRateObservation.meta().endSecondObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 8)))
        .set(InflationInterpolatedRateObservation.meta().weight(), WEIGHT)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    InflationInterpolatedRateObservation test = InflationInterpolatedRateObservation.of(
        GB_HICP, START_MONTH_FIRST, END_MONTH_FIRST, WEIGHT);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GB_HICP));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    InflationInterpolatedRateObservation test1 = InflationInterpolatedRateObservation.of(
        GB_HICP, START_MONTH_FIRST, END_MONTH_FIRST, WEIGHT);
    coverImmutableBean(test1);
    InflationInterpolatedRateObservation test2 = InflationInterpolatedRateObservation.of(
        CH_CPI, YearMonth.of(2010, 1), YearMonth.of(2010, 7), WEIGHT + 0.1d);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    InflationInterpolatedRateObservation test = InflationInterpolatedRateObservation.of(
        GB_HICP, START_MONTH_FIRST, END_MONTH_FIRST, WEIGHT);
    assertSerialization(test);
  }

}
