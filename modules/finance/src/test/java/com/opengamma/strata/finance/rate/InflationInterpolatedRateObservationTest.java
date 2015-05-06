/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.finance.rate;

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

/**
 * Test.
 */
@Test
public class InflationInterpolatedRateObservationTest {

  private static final YearMonth START_MONTH_FIRST = YearMonth.of(2014, 1);
  private static final YearMonth START_MONTH_SECOND = YearMonth.of(2014, 2);
  private static final YearMonth END_MONTH_FIRST = YearMonth.of(2015, 1);
  private static final YearMonth END_MONTH_SECOND = YearMonth.of(2015, 2);
  private static final double WEIGHT = 1.0 - 6.0 / 31.0;

  public void test_of() {
    InflationInterpolatedRateObservation test = InflationInterpolatedRateObservation.of(
        GB_HICP, START_MONTH_FIRST, START_MONTH_SECOND, END_MONTH_FIRST, END_MONTH_SECOND, WEIGHT);
    assertEquals(test.getIndex(), GB_HICP);
    assertEquals(test.getReferenceStartMonthFirst(), START_MONTH_FIRST);
    assertEquals(test.getReferenceStartMonthSecond(), START_MONTH_SECOND);
    assertEquals(test.getReferenceEndMonthFirst(), END_MONTH_FIRST);
    assertEquals(test.getReferenceEndMonthSecond(), END_MONTH_SECOND);
    assertEquals(test.getWeight(), WEIGHT, 1.0e-14);
  }

  public void test_builder() {
    InflationInterpolatedRateObservation test = InflationInterpolatedRateObservation.builder()
        .index(CH_CPI)
        .referenceStartMonthFirst(START_MONTH_FIRST)
        .referenceStartMonthSecond(START_MONTH_SECOND)
        .referenceEndMonthFirst(END_MONTH_FIRST)
        .referenceEndMonthSecond(END_MONTH_SECOND)
        .weight(WEIGHT)
        .build();
    assertEquals(test.getIndex(), CH_CPI);
    assertEquals(test.getReferenceStartMonthFirst(), START_MONTH_FIRST);
    assertEquals(test.getReferenceStartMonthSecond(), START_MONTH_SECOND);
    assertEquals(test.getReferenceEndMonthFirst(), END_MONTH_FIRST);
    assertEquals(test.getReferenceEndMonthSecond(), END_MONTH_SECOND);
    assertEquals(test.getWeight(), WEIGHT, 1.0e-14);
  }

  public void test_wrong_MONTHSOrder() {
    assertThrowsIllegalArg(() -> InflationInterpolatedRateObservation.of(
        GB_HICP, START_MONTH_FIRST, START_MONTH_SECOND, END_MONTH_FIRST, YearMonth.of(2014, 1), WEIGHT));
  }

  public void test_collectIndices() {
    InflationInterpolatedRateObservation test = InflationInterpolatedRateObservation.builder()
        .index(CH_CPI)
        .referenceStartMonthFirst(START_MONTH_FIRST)
        .referenceStartMonthSecond(YearMonth.of(2013, 11))
        .referenceEndMonthFirst(END_MONTH_FIRST)
        .referenceEndMonthSecond(END_MONTH_SECOND)
        .weight(WEIGHT)
        .build();
    ImmutableSet.Builder<Index> builder = ImmutableSet.builder();
    test.collectIndices(builder);
    assertEquals(builder.build(), ImmutableSet.of(CH_CPI));
  }

  public void coverage() {
    InflationInterpolatedRateObservation test1 = InflationInterpolatedRateObservation.of(
        GB_HICP, START_MONTH_FIRST, START_MONTH_SECOND, END_MONTH_FIRST, END_MONTH_SECOND, WEIGHT);
    coverImmutableBean(test1);
    InflationInterpolatedRateObservation test2 = InflationInterpolatedRateObservation.of(
        CH_CPI, START_MONTH_FIRST, START_MONTH_SECOND, END_MONTH_FIRST, END_MONTH_SECOND, WEIGHT);
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    InflationInterpolatedRateObservation test = InflationInterpolatedRateObservation.of(
        GB_HICP, START_MONTH_FIRST, START_MONTH_SECOND, END_MONTH_FIRST, END_MONTH_SECOND, WEIGHT);
    assertSerialization(test);
  }
}
