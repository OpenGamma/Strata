/**
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
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
 * Test {@link InflationEndInterpolatedRateObservation}.
 */
@Test
public class InflationEndInterpolatedRateObservationTest {

  private static final double START_INDEX = 135d;
  private static final YearMonth END_MONTH_FIRST = YearMonth.of(2015, 1);
  private static final YearMonth END_MONTH_SECOND = YearMonth.of(2015, 2);
  private static final double WEIGHT = 1.0 - 6.0 / 31.0;

  //-------------------------------------------------------------------------
  public void test_of() {
    InflationEndInterpolatedRateObservation test = InflationEndInterpolatedRateObservation.of(
        GB_HICP, START_INDEX, END_MONTH_FIRST, WEIGHT);
    assertEquals(test.getIndex(), GB_HICP);
    assertEquals(test.getEndObservation().getFixingMonth(), END_MONTH_FIRST);
    assertEquals(test.getEndSecondObservation().getFixingMonth(), END_MONTH_SECOND);
    assertEquals(test.getStartIndexValue(), START_INDEX);
    assertEquals(test.getWeight(), WEIGHT, 1.0e-14);
  }

  public void test_wrongMonthOrder() {
    assertThrowsIllegalArg(() -> InflationEndInterpolatedRateObservation.meta().builder()
        .set(InflationEndInterpolatedRateObservation.meta().startIndexValue(), START_INDEX)
        .set(InflationEndInterpolatedRateObservation.meta().endObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 7)))
        .set(InflationEndInterpolatedRateObservation.meta().endSecondObservation(),
            PriceIndexObservation.of(GB_HICP, YearMonth.of(2010, 7)))
        .set(InflationEndInterpolatedRateObservation.meta().weight(), WEIGHT)
        .build());
  }

  //-------------------------------------------------------------------------
  public void test_collectIndices() {
    InflationEndInterpolatedRateObservation test = InflationEndInterpolatedRateObservation.of(
        GB_HICP, START_INDEX, END_MONTH_FIRST, WEIGHT);
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(GB_HICP));
  }

  //-------------------------------------------------------------------------
  public void coverage() {
    InflationEndInterpolatedRateObservation test1 = InflationEndInterpolatedRateObservation.of(
        GB_HICP, START_INDEX, END_MONTH_FIRST, WEIGHT);
    coverImmutableBean(test1);
    InflationEndInterpolatedRateObservation test2 = InflationEndInterpolatedRateObservation.of(
        CH_CPI, 334d, YearMonth.of(2010, 7), WEIGHT + 1);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    InflationEndInterpolatedRateObservation test = InflationEndInterpolatedRateObservation.of(
        GB_HICP, START_INDEX, END_MONTH_FIRST, WEIGHT);
    assertSerialization(test);
  }

}
