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
    assertEquals(test.getReferenceStartMonth(), START_MONTH_FIRST);
    assertEquals(test.getReferenceStartInterpolationMonth(), START_MONTH_SECOND);
    assertEquals(test.getReferenceEndMonth(), END_MONTH_FIRST);
    assertEquals(test.getReferenceEndInterpolationMonth(), END_MONTH_SECOND);
    assertEquals(test.getWeight(), WEIGHT, 1.0e-14);
  }

  public void test_builder() {
    InflationInterpolatedRateObservation test = InflationInterpolatedRateObservation.builder()
        .index(CH_CPI)
        .referenceStartMonth(START_MONTH_FIRST)
        .referenceStartInterpolationMonth(START_MONTH_SECOND)
        .referenceEndMonth(END_MONTH_FIRST)
        .referenceEndInterpolationMonth(END_MONTH_SECOND)
        .weight(WEIGHT)
        .build();
    assertEquals(test.getIndex(), CH_CPI);
    assertEquals(test.getReferenceStartMonth(), START_MONTH_FIRST);
    assertEquals(test.getReferenceStartInterpolationMonth(), START_MONTH_SECOND);
    assertEquals(test.getReferenceEndMonth(), END_MONTH_FIRST);
    assertEquals(test.getReferenceEndInterpolationMonth(), END_MONTH_SECOND);
    assertEquals(test.getWeight(), WEIGHT, 1.0e-14);
  }

  public void test_wrongMonthOrder() {
    assertThrowsIllegalArg(() -> InflationInterpolatedRateObservation.of(
        GB_HICP, END_MONTH_FIRST, START_MONTH_FIRST, WEIGHT));
    assertThrowsIllegalArg(() -> InflationInterpolatedRateObservation.builder()
        .index(GB_HICP)
        .referenceStartMonth(YearMonth.of(2010, 1))
        .referenceStartInterpolationMonth(YearMonth.of(2010, 1))
        .referenceEndMonth(YearMonth.of(2010, 7))
        .referenceEndInterpolationMonth(YearMonth.of(2010, 8))
        .weight(WEIGHT)
        .build());
    assertThrowsIllegalArg(() -> InflationInterpolatedRateObservation.builder()
        .index(GB_HICP)
        .referenceStartMonth(YearMonth.of(2010, 1))
        .referenceStartInterpolationMonth(YearMonth.of(2010, 2))
        .referenceEndMonth(YearMonth.of(2010, 7))
        .referenceEndInterpolationMonth(YearMonth.of(2010, 7))
        .weight(WEIGHT)
        .build());
    assertThrowsIllegalArg(() -> InflationInterpolatedRateObservation.builder()
        .index(GB_HICP)
        .referenceStartMonth(YearMonth.of(2010, 8))
        .referenceStartInterpolationMonth(YearMonth.of(2010, 9))
        .referenceEndMonth(YearMonth.of(2010, 7))
        .referenceEndInterpolationMonth(YearMonth.of(2010, 8))
        .weight(WEIGHT)
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
    InflationInterpolatedRateObservation test2 = InflationInterpolatedRateObservation.builder()
        .index(CH_CPI)
        .referenceStartMonth(YearMonth.of(2010, 1))
        .referenceStartInterpolationMonth(YearMonth.of(2010, 2))
        .referenceEndMonth(YearMonth.of(2010, 7))
        .referenceEndInterpolationMonth(YearMonth.of(2010, 8))
        .weight(WEIGHT + 0.1d)
        .build();
    coverBeanEquals(test1, test2);
  }

  public void test_serialization() {
    InflationInterpolatedRateObservation test = InflationInterpolatedRateObservation.of(
        GB_HICP, START_MONTH_FIRST, END_MONTH_FIRST, WEIGHT);
    assertSerialization(test);
  }

}
